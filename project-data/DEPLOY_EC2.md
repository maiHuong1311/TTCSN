# Deploy FORME bằng GitHub Actions và Docker Compose

Cấu hình này dùng đúng máy Ubuntu x86_64 đang có MySQL/Redis và ECR:

- Repo trên EC2: `~/ttcsn/TTCSN/backend`.
- File bí mật: `~/ttcsn/TTCSN/backend/.env`.
- ECR: `331545064056.dkr.ecr.ap-southeast-1.amazonaws.com/forme-backend`.
- Cổng ứng dụng: `8081`.
- MySQL: `127.0.0.1:3306`, database `forme_store`, user `forme_app`.
- Redis: `127.0.0.1:6379`.

## Các file

| File | Mục đích |
|---|---|
| `backend/docker-compose.yml` | Chạy một container backend, dùng MySQL/Redis trên host |
| `backend/deploy.sh` | Đăng nhập ECR, pull image, triển khai và kiểm tra health; khôi phục image cũ nếu lỗi |
| `backend/Dockerfile` | Đóng gói JAR đã build trên GitHub Actions, chạy Java 17 bằng UID 10001 |
| `backend/.dockerignore` | Chỉ đưa Dockerfile/JAR vào Docker build context, loại `.env` và dữ liệu |
| `.github/workflows/deploy.yml` | Test → build → push ECR theo SHA → SSH chuyển Compose/script → deploy |

EC2 không build Java và không cần cài Maven/JDK. File `.env` trên EC2 không bị workflow ghi đè hoặc chuyển lên runner. Workflow không cần git pull trên EC2: image và hai file deploy được lấy từ đúng commit của lượt chạy.

## 1. Chuẩn bị EC2 một lần

Chạy trong SSH Ubuntu:

```bash
sudo apt update
sudo apt install -y docker-compose-v2 python3 util-linux
sudo docker compose version
```

Cần Compose **2.30 trở lên** vì cấu hình dùng `env_file.format: raw` để giữ nguyên ký tự `$` trong mật khẩu. Ubuntu 26.04 cung cấp gói `docker-compose-v2`: [thông tin gói chính thức](https://packages.ubuntu.com/eu/resolute/admin/docker-compose-v2).

Đảm bảo thư mục và file có sẵn:

```bash
cd ~/ttcsn/TTCSN/backend
test -f .env && echo 'Da co file .env'
chmod 600 .env
sudo docker info >/dev/null
sudo -n true
```

Script tự dùng `sudo -n docker` nếu user `ubuntu` chưa có quyền Docker. Với EC2 Ubuntu mặc định có sudo không hỏi mật khẩu, cách này cũng dùng đúng Docker credential đã login bằng sudo. Không chạy cả script bằng sudo vì khi đó AWS CLI có thể mất ngữ cảnh IAM/config của user.

MySQL/Redis đang hoạt động sẵn nên Compose không tạo thêm hai dịch vụ này và không thay đổi dữ liệu của chúng. Security Group phải cho nguồn kiểm thử truy cập TCP 8081; SSH của GitHub runner cần được phép theo cấu hình SSH bạn đã thiết lập.

## 2. Nội dung file .env trên EC2

Những dòng tối thiểu:

```dotenv
DB_URL=jdbc:mysql://127.0.0.1:3306/forme_store?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true
DB_USERNAME=forme_app
DB_PASSWORD=THAY_MAT_KHAU_THUC
JWT_SECRET=THAY_CHUOI_NGAU_NHIEN_IT_NHAT_32_BYTE
JWT_ACCESS_MINUTES=60
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_SSL_ENABLED=false
FRONTEND_ORIGIN=http://localhost:5173
BREVO_API_KEY=
BREVO_SENDER_EMAIL=
AI_API_URL=
AI_API_KEY=
```

Không giữ placeholder `THAY_...`, không bọc giá trị bằng dấu nháy và không dùng `export KEY=...`. Đây là định dạng env raw: các dấu `$`, `&`, `#` trong giá trị được giữ nguyên. Không chạy `source .env`. Script chỉ kiểm tra giá trị bắt buộc, không in chúng ra log.

Compose chủ động đặt profile `aws`, bind `0.0.0.0`, port `8081` và đường dẫn ảnh `/app/uploads`. Vì vậy các dòng tương ứng trong `.env` không thay đổi bốn giá trị này.

Ảnh upload được bind mount từ `backend/uploads/product-images/`. Script tạo thư mục và đặt owner UID/GID 10001; không xóa ảnh khi deploy. Nếu ảnh cũ nằm ở `/srv/forme/uploads`, hãy sao lưu và chuyển chúng sang thư mục mới trước khi sử dụng; script không tự di chuyển ảnh cũ. Sao lưu thư mục ảnh cùng database.

## 3. Khai báo GitHub Secrets

Vào repo → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Giá trị |
|---|---|
| `EC2_HOST` | `56.10.115.197` hoặc IP hiện tại của EC2 |
| `EC2_USERNAME` | `ubuntu` (có thể bỏ vì workflow mặc định ubuntu) |
| `EC2_SSH_KEY` | Toàn bộ private key `.pem` dùng SSH, giữ trong GitHub Secrets |
| `EC2_HOST_FINGERPRINT` | Fingerprint host key đã xác minh của EC2 |

Lấy host fingerprint **từ terminal EC2 mà bạn đã SSH tin cậy**:

```bash
sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub -E sha256
```

Sao chép phần `SHA256:...` vào secret. Workflow dùng `appleboy/ssh-action` với fingerprint để xác minh server, xem [tài liệu action chính thức](https://github.com/appleboy/ssh-action).

Nếu SSH workflow trước của bạn dùng tên secret khác, đổi các tham chiếu `secrets.EC2_...` trong workflow cho khớp hoặc tạo tên secret như bảng.

### Quyền push ECR của GitHub Actions

SSH đã kết nối chưa đồng nghĩa runner có quyền push ECR. Chọn một cách:

1. **OIDC:** đặt secret `AWS_ROLE_ARN` trỏ tới role GitHub Actions có quyền push ECR. Workflow tự dùng OIDC khi secret này có giá trị. Trust policy phải khớp repo, ref main và định dạng subject thực tế của GitHub.
2. **Access key đã cấu hình:** nếu không có `AWS_ROLE_ARN`, workflow dùng hai secret `AWS_ACCESS_KEY_ID` và `AWS_SECRET_ACCESS_KEY`. Không đưa hai giá trị này vào source hoặc `.env` backend.

Role/user của runner cần:

- `ecr:GetAuthorizationToken` trên `*`.
- `ecr:BatchCheckLayerAvailability`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`, `ecr:PutImage`, `ecr:BatchGetImage` trên `arn:aws:ecr:ap-southeast-1:331545064056:repository/forme-backend`.

EC2 tiếp tục dùng instance role ECR ReadOnly đã cấu hình, không dùng access key của runner.

## 4. Chạy workflow

Đưa các file vào nhánh riêng, mở PR vào dev rồi phát hành qua PR vào main theo quy ước dự án. Không đẩy commit trực tiếp lên main/dev.

Workflow tự chạy khi main thay đổi backend hoặc workflow. Có thể vào **Actions → Deploy FORME backend to EC2 → Run workflow → main**. Workflow cố ý không deploy các nhánh khác main.

Các bước tự động:

1. Test bằng MySQL/Redis riêng trên runner, không dùng database EC2.
2. Build JAR bằng Java 17, tạo image Linux AMD64 và push tag SHA.
3. SSH vào EC2, giữ nguyên `.env`, chuyển hai file Compose/script của đúng commit.
4. Gọi `bash deploy.sh <SHA>`, kéo image trước khi thay container.
5. Chờ health tối đa 240 giây. Nếu thất bại, script thử khôi phục image trước và vẫn báo workflow thất bại.

Ảnh Docker cũ không bị prune. Script dùng lock để chặn hai lần triển khai trực tiếp chạy cùng lúc; workflow không tự hủy lượt deploy đang chạy.

## 5. Chạy thủ công hoặc kiểm tra lỗi

Sau khi đã có image trên ECR và hai file deploy trên EC2:

```bash
cd ~/ttcsn/TTCSN/backend
bash deploy.sh THAY_TAG_SHA_DA_CO_TREN_ECR
```

Không dùng lệnh này trước khi tag được push. Script chạy tương đối theo vị trí file nên vẫn dùng được khi đổi thư mục repo, miễn Compose và `.env` ở cạnh script.

Kiểm tra:

```bash
curl --fail http://127.0.0.1:8081/api/health
sudo docker ps --filter label=com.docker.compose.project=forme
```

Xem log bằng container ID, không cần đọc/in `.env`:

```bash
container_id=$(sudo docker ps -aq --filter label=com.docker.compose.project=forme --filter label=com.docker.compose.service=backend)
sudo docker logs --tail 100 "$container_id"
```

Mở Swagger: `http://56.10.115.197:8081/swagger-ui.html` (thay IP nếu EC2 đổi địa chỉ).

Các lệnh Compose trực tiếp cần cung cấp image và tắt tự đọc dotenv, ví dụ:

```bash
export FORME_IMAGE=$(cat .deploy/current-image)
sudo --preserve-env=FORME_IMAGE docker compose --env-file /dev/null -p forme ps
```

Nếu sudo không cho preserve-env, dùng `sudo env FORME_IMAGE="$FORME_IMAGE" docker compose --env-file /dev/null -p forme ps`.

## Giới hạn

- Có khoảng gián đoạn khi thay container. Health `/api/health` hiện là kiểm tra HTTP ứng dụng, không phải readiness MySQL/Redis đầy đủ.
- Rollback chỉ đổi image, không quay lui Flyway hoặc khôi phục nội dung `.env` cũ. Sao lưu DB trước khi phát hành migration.
- Container giới hạn RAM 640 MB, heap Java 256 MB vì máy hiện có khoảng 2 GB RAM và đang chạy MySQL/Redis. Theo dõi khi chạy; nếu thiếu bộ nhớ phải điều chỉnh tài nguyên, không chỉ tăng heap.
- Profile AWS không tự tạo admin. Đăng ký/xác minh email và cấp role cho tài khoản quản trị theo hướng dẫn AWS hiện có.
- Những file này chưa được push, workflow chưa chạy trên GitHub, image chưa được build/push và server chưa được thay đổi từ phiên làm việc này.
