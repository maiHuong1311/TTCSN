# Hướng dẫn triển khai backend FORME lên AWS

Tài liệu đối chiếu với mã nguồn FORME và `CHIIKAIWA-BE-AWS-DEPLOYMENT-GUIDE.md`, ngày 18/10/2026. Các lệnh bên dưới là hướng dẫn để người vận hành thực hiện; chưa có tài nguyên AWS nào được tạo và chưa kích hoạt pipeline triển khai.

## 1. Mô hình triển khai

```text
Pull Request → dev → main
                      ↓
GitHub Actions: kiểm thử → build JAR → build Docker image
                      ↓
Amazon ECR: image có tag bằng commit SHA
                      ↓
SSH vào EC2: pull đúng SHA → thay container → kiểm tra health
                      ↓
Spring Boot trên EC2 → RDS MySQL
                    → Redis trên cùng EC2
                    → Brevo API và API AI nếu được cấu hình
                    → thư mục ảnh nằm trên ổ đĩa EC2
```

RDS của FORME phải là **MySQL**, không phải PostgreSQL như dự án mẫu. Build context là thư mục gốc repo, còn Maven project nằm trong `backend/`. Backend hiện chưa có Dockerfile AWS, workflow deploy hay profile AWS riêng. Các ví dụ dưới đây cần được thêm và đi qua Pull Request trước khi bật CI/CD.

## 2. Những điểm riêng của FORME

| Nội dung | Giá trị/cách xử lý |
|---|---|
| Java | 17 |
| Maven project | `backend/pom.xml` |
| JAR | `backend/target/fashion-store-backend-0.0.1-SNAPSHOT.jar` |
| Cổng | 8080 |
| Health | `/api/health`, hiện chỉ kiểm tra ứng dụng đáp ứng HTTP, không kiểm tra MySQL/Redis |
| Swagger | `/swagger-ui.html`, tài liệu JSON `/v3/api-docs` |
| Database | RDS MySQL 8.x, cổng 3306 |
| Redis | Cổng 6379, chỉ bind loopback của EC2 |
| JWT | Phải đặt `JWT_SECRET` mới, không dùng default local |
| Ảnh upload | Gắn thư mục EC2 vào container; không lưu chỉ trong lớp ghi của container |
| Admin | `LocalAdminBootstrap` chỉ hoạt động ở profile `local`; không tự tạo admin khi chạy profile `aws` |
| Database schema | Flyway tự chạy V1–V14 khi ứng dụng khởi động; giữ `ddl-auto=validate` |
| Seed data | Các migration hiện có tạo cả sản phẩm mẫu; DB AWS mới cũng có dữ liệu mẫu |

## 3. Tạo tài nguyên trong AWS Console

Chọn một region thống nhất, ví dụ `ap-southeast-1`. EC2, ECR và RDS trong hướng dẫn đều dùng region này. Các tài nguyên này có thể phát sinh phí; xem phần ước tính chi phí trong Console trước khi xác nhận tạo.

### 3.1. EC2

1. Vào **EC2 → Launch instance**, đặt tên `forme-backend`.
2. Chọn **Amazon Linux 2023, x86_64**. Chọn instance có khoảng 2 GB RAM trở lên để backend và Redis có dư bộ nhớ; đây là mức đề xuất cho demo, không phải cam kết về hiệu năng hay miễn phí.
3. Tạo/chọn SSH key pair và giữ file `.pem` trên máy cá nhân, không đưa vào GitHub.
4. Chọn VPC sẽ dùng chung với RDS. Để theo hướng dẫn SSH này, EC2 cần public IPv4 và subnet có đường ra Internet; có thể dùng Elastic IP nếu cần địa chỉ ổn định.
5. Security Group `forme-ec2-sg`: mở TCP 22 từ IP quản trị của bạn. Chưa cần mở 8080 công khai. Không mở 3306/6379 ra Internet.
6. Tạo IAM Role cho EC2 có quyền pull ECR và gắn bằng **Actions → Security → Modify IAM role**. Quyền cần gồm `ecr:GetAuthorizationToken` và các quyền đọc layer/image của repository `forme-backend`.

### 3.2. RDS MySQL

1. Vào **RDS → Create database**, chọn MySQL và phiên bản 8.x phù hợp.
2. Chọn cùng VPC với EC2; **Public access = No**.
3. Đặt initial database name là `fashion_store`. Ghi lại endpoint, username, password và cổng 3306.
4. Security Group `forme-rds-sg`: inbound TCP 3306, source là **Security Group `forme-ec2-sg`**, không phải `0.0.0.0/0`.
5. Bật backup tự động với thời gian lưu phù hợp. Có thể tạo user ứng dụng riêng có quyền trên `fashion_store.*`; tài khoản chạy Flyway cần cả quyền thay đổi schema, không chỉ SELECT/INSERT.

Không dùng hostname `localhost` hoặc cổng MySQL local 3307 làm địa chỉ RDS. FORME đọc **`DB_URL` đầy đủ**, không tự ghép từ `DB_HOST`/`DB_NAME` của tài liệu mẫu.

### 3.3. ECR

Vào **Amazon ECR → Private repositories → Create repository**, tạo repository `forme-backend`. Bật quét image và đặt lifecycle policy giữ đủ bản để rollback. Ghi lại URI dạng:

```text
ACCOUNT_ID.dkr.ecr.ap-southeast-1.amazonaws.com/forme-backend
```

Xem [quy trình push/pull ECR chính thức](https://docs.aws.amazon.com/AmazonECR/latest/userguide/getting-started-cli.html).

## 4. Chuẩn bị EC2

Từ PowerShell trên máy cá nhân:

```powershell
ssh -i "C:\duong-dan\forme-key.pem" ec2-user@EC2_PUBLIC_IP
```

Các khối Bash tiếp theo chạy **trong EC2**, không chạy trực tiếp bằng PowerShell:

```bash
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
```

Thoát SSH rồi đăng nhập lại để cập nhật group. Kiểm tra:

```bash
docker version
aws --version
aws sts get-caller-identity
```

Nếu `aws` chưa có, cài AWS CLI v2 theo [hướng dẫn AWS](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html). EC2 dùng IAM Role, không cần `aws configure` với access key cá nhân.

Tạo thư mục bền vững và chạy Redis:

```bash
sudo mkdir -p /srv/forme/uploads
sudo chown -R ec2-user:ec2-user /srv/forme
sudo chown 10001:10001 /srv/forme/uploads
docker volume create forme-redis-data
docker run -d --name forme-redis --restart unless-stopped \
  -p 127.0.0.1:6379:6379 \
  -v forme-redis-data:/data \
  --log-opt max-size=10m --log-opt max-file=3 \
  redis:7.4-alpine redis-server --appendonly yes \
  --maxmemory 128mb --maxmemory-policy noeviction
docker exec forme-redis redis-cli ping
```

Kết quả cần có `PONG`. Đây là cấu hình một EC2, Redis chỉ công bố cổng trên loopback. Container backend ở bước sau dùng host network nên truy cập được `127.0.0.1:6379`. Với môi trường lâu dài, ghim Redis và Java image bằng digest đã kiểm tra, thay vì dựa hoàn toàn vào tag có thể thay đổi.

## 5. Tạo cấu hình môi trường trên EC2

```bash
umask 077
nano /srv/forme/.env
```

Nhập cấu hình dưới đây và thay toàn bộ giá trị `THAY_...`:

```dotenv
SPRING_PROFILES_ACTIVE=aws
SERVER_ADDRESS=0.0.0.0
SERVER_PORT=8080
DB_URL=jdbc:mysql://THAY_RDS_ENDPOINT:3306/fashion_store?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&sslMode=REQUIRED
DB_USERNAME=THAY_DATABASE_USERNAME
DB_PASSWORD=THAY_DATABASE_PASSWORD
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_SSL_ENABLED=false
JWT_SECRET=THAY_CHUOI_NGAU_NHIEN_DAI_IT_NHAT_32_BYTE
JWT_ACCESS_MINUTES=60
FRONTEND_ORIGIN=http://localhost:5173
PRODUCT_IMAGES_DIRECTORY=/app/uploads
BREVO_API_KEY=THAY_BREVO_API_KEY
BREVO_SENDER_EMAIL=THAY_EMAIL_GUI_DA_XAC_MINH
AI_API_URL=
AI_API_KEY=
```

Sinh JWT secret bằng `openssl rand -hex 32`, sau đó điền kết quả vào file. Không đặt dấu nháy bao quanh giá trị trong file dùng với `docker --env-file`. Không đưa file này vào Git. Đặt quyền:

```bash
chmod 600 /srv/forme/.env
```

`sslMode=REQUIRED` yêu cầu mã hóa TLS nhưng chưa xác minh đầy đủ danh tính RDS. Khi triển khai sử dụng thực tế, tải CA bundle RDS, đưa CA vào truststore Java rồi dùng `sslMode=VERIFY_IDENTITY` với endpoint RDS gốc; xem [hướng dẫn TLS RDS MySQL](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/ssl-certificate-rotation-mysql.html).

Không dùng `SPRING_PROFILES_ACTIVE=local` trên AWS chỉ để tạo admin. Profile `aws` hiện chưa có file cấu hình riêng vẫn chạy được: `application.yml` chung đọc các biến ở trên. Khi hoàn thiện CI/CD nên thêm `application-aws.yml` để bắt buộc secret/DB production được cung cấp, tránh rơi về default local.

## 6. Dockerfile cho FORME

Tạo `backend/aws.Dockerfile` trong nhánh triển khai, nội dung:

```dockerfile
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --gid 10001 spring \
    && useradd --uid 10001 --gid spring --no-create-home spring \
    && mkdir -p /app/uploads \
    && chown spring:spring /app/uploads
COPY --chown=spring:spring backend/target/fashion-store-backend-0.0.1-SNAPSHOT.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60.0", "-jar", "/app/app.jar"]
```

Biến `SPRING_PROFILES_ACTIVE` được Spring Boot đọc từ môi trường. Không sao chép ENTRYPOINT dạng `${SPRING_PROFILES_ACTIVE:aws}` của tài liệu gốc vào JSON exec form: Docker không tự nội suy biến theo cú pháp Spring ở đó.

Tạo `.dockerignore` ở root repo theo cách allowlist để Docker chỉ nhận đúng JAR và Dockerfile:

```dockerignore
**
!backend/
backend/**
!backend/aws.Dockerfile
!backend/target/
backend/target/**
!backend/target/fashion-store-backend-0.0.1-SNAPSHOT.jar
```

## 7. Build và push image lên ECR

Trong luồng chính, phần này chạy trên **GitHub Actions**, không build trên EC2. Để thử các lệnh tương đương, cần máy Linux/runner có Java 17, Docker và AWS CLI đã xác thực với quyền push ECR.

```bash
set -euo pipefail
AWS_REGION=ap-southeast-1
AWS_ACCOUNT_ID=THAY_ACCOUNT_ID
REGISTRY="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
IMAGE="$REGISTRY/forme-backend"
TAG=$(git rev-parse HEAD)

chmod +x backend/mvnw
./backend/mvnw -f backend/pom.xml -B -ntp clean package -DskipTests
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY"
docker build --platform linux/amd64 -f backend/aws.Dockerfile -t "$IMAGE:$TAG" .
docker push "$IMAGE:$TAG"
```

Lệnh package bỏ qua test chỉ dùng sau một job kiểm thử đã thành công. Không chạy kiểm thử ghi dữ liệu lên RDS ứng dụng. Runner cần MySQL test riêng và Redis; H2 mặc định không hỗ trợ một số migration MySQL hiện có.

## 8. Pull và chạy backend trên EC2

Trong SSH EC2, thay account ID và commit SHA bằng image thực sự đã push:

```bash
set -euo pipefail
AWS_REGION=ap-southeast-1
REGISTRY=THAY_ACCOUNT_ID.dkr.ecr.ap-southeast-1.amazonaws.com
IMAGE="$REGISTRY/forme-backend:THAY_COMMIT_SHA"
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY"
docker pull "$IMAGE"

# Ghi lại image cũ trước khi thay container để có thể rollback.
docker inspect --format '{{.Config.Image}}' forme-backend \
  > /srv/forme/previous-image.txt 2>/dev/null || true
docker rm -f forme-backend 2>/dev/null || true

docker run -d --name forme-backend --restart unless-stopped \
  --network host \
  --env-file /srv/forme/.env \
  --mount type=bind,source=/srv/forme/uploads,target=/app/uploads \
  --memory=1g --cpus=1 \
  --log-opt max-size=10m --log-opt max-file=3 \
  "$IMAGE"

curl --fail --retry 20 --retry-all-errors --retry-delay 3 \
  http://127.0.0.1:8080/api/health
```

Nếu ứng dụng đã sẵn sàng, API health trả JSON có `status: UP`. Kiểm tra thêm log và Redis vì health hiện chưa đo các phụ thuộc:

```bash
docker logs --tail 100 forme-backend
docker exec forme-redis redis-cli ping
docker ps --filter name=forme-backend
```

Log phải không có lỗi kết nối RDS, Flyway hoặc schema validation. Kiểm thử một API đọc dữ liệu sau khi đăng nhập để xác nhận truy vấn MySQL; thử luồng OTP bằng email kiểm thử của bạn khi muốn kiểm tra cả Redis/Brevo. Flyway đã tự tạo bảng và seed, không import thêm SQL trùng vào DB mới.

## 9. Mở Swagger từ máy của bạn

Giữ cổng 8080 riêng, tạo SSH tunnel từ PowerShell:

```powershell
ssh -i "C:\duong-dan\forme-key.pem" -L 8081:127.0.0.1:8080 ec2-user@EC2_PUBLIC_IP
```

Giữ terminal đó mở, truy cập:

- Health: `http://localhost:8081/api/health`.
- Swagger: `http://localhost:8081/swagger-ui.html`.

Gọi login, sao chép JWT vào Authorize. Swagger hiện công khai tài liệu trong ứng dụng, nhưng API nghiệp vụ vẫn kiểm tra JWT/quyền. Khi có domain public, có thể hạn chế đường dẫn tài liệu ở reverse proxy hoặc cấu hình lại quyền tùy nhu cầu vận hành.

Nếu cần API truy cập qua Internet, đặt **ALB HTTPS + chứng chỉ ACM** trước EC2: listener 443 → target group HTTP 8080, health path `/api/health`. EC2 chỉ cho phép 8080 từ Security Group của ALB. Thêm `SERVER_FORWARD_HEADERS_STRATEGY=framework` khi ứng dụng chỉ nhận traffic từ proxy tin cậy. Đổi `FRONTEND_ORIGIN` thành origin chính xác của frontend, ví dụ `https://forme.example.com`, không kèm đường dẫn hoặc dấu `/` cuối.

## 10. Tạo admin trên database AWS

Profile AWS không chạy LocalAdminBootstrap. Cách không sửa code:

1. Cấu hình Brevo hoạt động, đăng ký/xác minh email của chính người quản trị bằng Swagger.
2. Từ công cụ SQL có kết nối riêng tới RDS, kiểm tra đúng email và thực hiện:

```sql
UPDATE app_user
SET role = 'Admin', auth_version = auth_version + 1
WHERE email = 'THAY_EMAIL_QUAN_TRI_DA_DANG_KY';
```

3. Kiểm tra đúng một tài khoản được cập nhật, đăng nhập lại để nhận JWT có quyền Admin.

Nếu dùng MySQL Workbench trên máy cá nhân, cấu hình TCP/IP over SSH qua EC2 tới endpoint RDS, không bật public RDS. Không đưa mật khẩu mẫu local vào tài khoản AWS.

## 11. Hoàn thiện GitHub Actions theo luồng gốc

Trước tiên merge PR mã nguồn vào `dev`, rồi phát hành qua PR từ `dev` sang `main`. Theo quy ước dự án, không commit/push trực tiếp main hoặc dev. Tạo workflow `.github/workflows/deploy-aws.yml` trên một nhánh riêng.

Workflow cần hai job tuần tự:

### Job test

- Checkout source và cài Java 17.
- Khởi động MySQL service, database `fashion_store_test`, và Redis service. Chờ chúng healthy.
- Chạy Maven verify với datasource test; bật kiểm thử Redis thật:

```bash
RUN_REDIS_INTEGRATION=true ./backend/mvnw -f backend/pom.xml -B -ntp verify \
  -Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/fashion_store_test \
  -Dspring.datasource.username=THAY_CI_DATABASE_USER \
  -Dspring.datasource.password=THAY_CI_DATABASE_PASSWORD \
  -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

User/password này là cấu hình database test của runner, không phải tài khoản RDS. Không gọi Brevo/API AI thật trong test.

### Job deploy, chỉ chạy khi test đạt

1. Checkout, Java 17 và build JAR theo bước 7.
2. Xác thực AWS bằng GitHub OIDC, role chỉ cho phép repo/nhánh phát hành.
3. Đăng nhập ECR, build/push image với `${{ github.sha }}`.
4. SSH vào EC2 bằng SSH key, kiểm tra host key đã xác minh trước.
5. Chạy các lệnh bước 8 với đúng SHA. Nếu health lỗi, giữ log và chạy lại image trong `previous-image.txt` bằng cùng env/volume/cấu hình run.

Khung trigger và chống chạy song song:

```yaml
on:
  push:
    branches: [main]
  workflow_dispatch:
concurrency:
  group: forme-aws-production
  cancel-in-progress: false
permissions:
  contents: read
  id-token: write
```

Dùng `needs: test` cho job deploy. Với workflow_dispatch, chỉ cho deploy khi ref là main. Pin các action bằng full commit SHA đã kiểm tra. Không chạy hai lần deploy cùng lúc hoặc hủy giữa lúc đang thay container.

### IAM và biến GitHub

Tạo IAM OIDC provider `https://token.actions.githubusercontent.com`, audience `sts.amazonaws.com`; role có quyền push repository ECR `forme-backend`. Role EC2 chỉ cần pull. Không truyền access key GitHub xuống EC2.

**Lưu ý GitHub hiện hành:** repo được tạo sau 15/07/2026 có thể dùng subject OIDC chứa ID bất biến, dạng `repo:OWNER@OWNER_ID/REPO@REPO_ID:ref:refs/heads/main`, thay vì chỉ tên repo. Trust policy phải khớp dạng subject repo thực sự dùng. Nếu dùng GitHub Environment, subject đổi theo environment. Xem [tài liệu OIDC GitHub/AWS](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws).

| Loại | Tên | Giá trị |
|---|---|---|
| Variable | `AWS_REGION` | `ap-southeast-1` |
| Variable | `AWS_ROLE_ARN` | Role OIDC được phép push ECR |
| Variable | `ECR_REPOSITORY` | `forme-backend` |
| Variable | `EC2_HOST` | DNS/IP public ổn định của EC2 |
| Variable | `EC2_USER` | `ec2-user` |
| Secret | `EC2_SSH_KEY` | Private key SSH cho deploy |
| Secret/Variable | `EC2_HOST_KEY` | Public host key đã xác minh của EC2 |

Cấu hình ứng dụng được giữ tại `/srv/forme/.env` như bước 5; workflow không cần nắm DB password/Brevo key. Đây là biến thể đơn giản hơn việc ghi lại toàn bộ env mỗi lần deploy trong tài liệu gốc. Nếu muốn pipeline quản lý env, chuyển các giá trị đó sang GitHub Environment Secrets/SSM Parameter Store và sinh file quyền 600 mà không in nội dung ra log.

**SSH từ runner:** rule 22 chỉ cho IP cá nhân sẽ không cho GitHub-hosted runner vào. Cần chọn một phương án trước khi bật deploy: runner có egress IP cố định; hoặc pipeline được IAM cho phép mở tạm TCP 22 từ đúng IPv4 `/32` của lượt chạy rồi đóng lại ở bước `always()`. Chỉ cấp quyền sửa đúng Security Group cần thiết. Không giải quyết bằng mở SSH cho toàn Internet. Có thể thay riêng bước SSH bằng AWS Systems Manager khi muốn loại bỏ quản lý inbound SSH.

## 12. Ảnh, frontend và dữ liệu sau deploy

- Ảnh upload mới có URL `/api/images/...`, được backend phục vụ từ volume EC2. Giữ `/srv/forme/uploads` khi thay container và sao lưu cùng RDS.
- Ảnh mẫu `/assets/photos/...` hiện nằm trong `frontend/public`, không nằm trong JAR backend. Khi triển khai frontend, cần giữ các assets đó. Deploy backend không đồng nghĩa các ảnh mẫu cũng được phục vụ tại domain backend.
- Frontend hiện gọi đường dẫn tương đối `/api`. `FRONTEND_ORIGIN` chỉ điều khiển CORS, không đổi API URL trong React. Nếu frontend và backend khác domain, phải cấu hình base URL trong frontend và xử lý URL ảnh hoặc reverse proxy `/api` về backend. Nếu vẫn test frontend local, có thể tạm đổi target `/api` trong Vite proxy sang backend AWS/SSH tunnel.
- Chưa có API AI thực thì để `AI_API_URL`/`AI_API_KEY` trống; không điền URL minh họa làm endpoint thật.
- Thanh toán hiện là demo, triển khai AWS không biến nó thành thanh toán tiền thật.

## 13. Xử lý lỗi thường gặp

| Hiện tượng | Kiểm tra |
|---|---|
| Không vào được Swagger | Tunnel còn mở; container chạy; `/api/health` trả 200; SG SSH đúng IP |
| RDS timeout | VPC/subnet/route và RDS SG cho phép SG EC2 trên 3306; endpoint đúng |
| Access denied MySQL | DB user/password/quyền schema; không dùng password local |
| Flyway/schema validation lỗi | Đọc log; không bật ddl-auto=update hoặc sửa migration đã apply |
| Redis connection refused | Redis container/PONG; host network; không dùng localhost nếu đổi sang bridge network |
| Không có quyền Admin | Tài khoản AWS đã được nâng role; đăng nhập lại sau thay role |
| Upload trả lỗi 500 | Quyền UID 10001 trên `/srv/forme/uploads`, mount và dung lượng ổ đĩa |
| Ảnh mất sau cập nhật | Container mới thiếu bind mount hoặc dùng sai thư mục |
| Deploy CI SSH timeout | Rule 22 không cho IP runner hoặc host/IP đã đổi |
| OIDC bị từ chối | Subject có/không ID bất biến, ref main, environment và trust policy |
| ALB báo unhealthy | Health path `/api/health`, app bind 0.0.0.0, target port và SG |

## 14. Rollback

Đọc image cũ từ `/srv/forme/previous-image.txt`, xác minh đúng image mong muốn, pull lại rồi tạo `forme-backend` bằng **toàn bộ các cờ docker run ở bước 8**, chỉ đổi biến `IMAGE`. Giữ nguyên file env và volume ảnh. Không prune/xóa image rollback ngay sau deploy.

Nếu Flyway đã áp dụng migration, quay lại image không tự quay lại schema. Cần kiểm tra migration tương thích ngược và có backup trước khi đưa bản thay đổi database lên server.

## 15. Thứ tự nên thực hiện

1. Chuẩn bị EC2/RDS/ECR và IAM, ghi lại endpoint/ARN không bí mật.
2. Thêm Dockerfile, build/push image đầu tiên và thử chạy qua SSH tunnel.
3. Kiểm tra đăng ký, admin, MySQL/Redis, Brevo và upload ảnh.
4. Đưa quy trình build/push/SSH/health vào workflow sau khi đã xác minh kết nối và quyền.
5. Cấu hình domain/HTTPS và frontend origin trước khi dùng tài khoản/dữ liệu thực qua Internet.
