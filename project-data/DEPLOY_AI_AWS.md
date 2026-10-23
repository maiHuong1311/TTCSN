# Bổ sung Gemini AI vào AWS đang chạy

> Cập nhật 24/10/2026: bản Java mới gọi Gemini trực tiếp — xem [hướng dẫn mới, mục AWS](HUONG_DAN_GEMINI_TRUC_TIEP.md). Không cần deploy Python theo tài liệu dưới khi dùng GEMINI_API_KEY trong backend; phần dưới dành cho adapter FastAPI tùy chọn.

Hướng dẫn theo cấu hình trong repo ngày 24/10/2026: EC2 Ubuntu x86_64, backend Docker Compose ở `~/ttcsn/TTCSN/backend`, cổng 8081 và `network_mode: host`. Chưa SSH hoặc xác minh tài nguyên AWS thực tế trong phiên này. Nếu server khác cấu hình này, cần điều chỉnh trước khi chạy.

Frontend → backend hiện tại → AI nội bộ `127.0.0.1:8001` → Gemini API. Dịch vụ AI dùng mã Python có sẵn, chạy container Python 3.12 riêng, không cần cài Python trên host. Dữ liệu vector nằm trên Docker volume; frontend tiếp tục gọi `/api/ai/*` qua backend.

## 1. Kiểm tra server trước

SSH bằng key và IP hiện tại của bạn; không dùng IP trong tài liệu cũ nếu EC2 đã đổi IP.

```bash
cd ~/ttcsn/TTCSN/backend
sudo docker compose version
sudo docker ps --filter label=com.docker.compose.project=forme
curl --fail http://127.0.0.1:8081/api/health
free -h
df -h /
sudo ss -ltnp | grep ':8001 ' || true
```

Cần Compose >= 2.30, backend health UP và cổng 8001 chưa bị dịch vụ khác chiếm. Nếu đã có AI trên cổng này, kiểm tra dịch vụ đó trước, không chạy thêm bản trùng cổng. Server đang chạy cả Java/MySQL/Redis cần có RAM/đĩa trống cho Chroma và image Python; kiểm tra lại `docker stats --no-stream` sau khi bật AI. Không tải model LLM về EC2.

## 2. Chuyển đúng nguồn AI và file Docker lên server

Đã chuẩn bị ba file mới trong `backend/`: `Dockerfile.ai`, `Dockerfile.ai.dockerignore`, `docker-compose.ai.yml`. Đây là cấu hình riêng; Compose backend và workflow hiện tại không tự deploy AI.

Từ PowerShell tại thư mục gốc dự án trên máy cá nhân:

```powershell
New-Item -ItemType Directory -Force project-data/.local | Out-Null
tar -czf project-data/.local/forme-ai-deploy.tar.gz -C backend Dockerfile.ai Dockerfile.ai.dockerignore docker-compose.ai.yml ai-service/requirements.lock.txt ai-service/forme_ai src/main/resources/fashion
scp -i "C:\DUONG_DAN\key.pem" project-data/.local/forme-ai-deploy.tar.gz ubuntu@EC2_IP:~/forme-ai-deploy.tar.gz
```

Thay key và EC2_IP bằng thông tin đang dùng. Gói này không lấy `.env`, database, thư mục upload hoặc build Java.

Trên EC2:

```bash
mkdir -p ~/forme-ai
tar -xzf ~/forme-ai-deploy.tar.gz -C ~/forme-ai
cd ~/forme-ai
```

Đây là thư mục deploy AI độc lập. Khi cập nhật AI lần sau, dùng gói nguồn cùng phiên bản đã kiểm thử; lưu bản trước để quay lại khi cần. Không cần git pull vào thư mục backend đang deploy bằng image.

## 3. Tạo cấu hình bí mật của AI

Khóa Gemini tạo/quản lý tại Google AI Studio. Để khóa trên server, không đưa vào React, file application.yml hay Git.

Sinh token nội bộ một lần nếu chưa có token đang dùng:

```bash
openssl rand -hex 32
```

Ghi lại token này để dùng cùng một giá trị ở cả AI và backend. Token này không phải khóa Gemini.

```bash
cd ~/forme-ai
umask 077
touch .env.ai
chmod 600 .env.ai
nano .env.ai
```

Nội dung, thay các placeholder trước khi khởi động:

```dotenv
AI_API_KEY=THAY_TOKEN_NOI_BO_VUA_SINH
GEMINI_API_KEY=THAY_KHOA_GEMINI
GEMINI_MODEL=THAY_MODEL_HO_TRO_STRUCTURED_OUTPUT
GEMINI_EMBEDDING_MODEL=gemini-embedding-001
AI_EMBEDDING_MODE=gemini
```

Giá trị không bọc dấu nháy, không thêm `export`. Có thể đặt `AI_EMBEDDING_MODE=lexical` nếu muốn dùng truy xuất local và chỉ gọi Gemini ở bước chọn sản phẩm; đây không phải embedding ngữ nghĩa. Vẫn cần thư viện Chroma theo mã hiện tại.

Tên model phải phù hợp tài khoản/API hiện có. Không mặc định rằng model được ghi trong tài liệu cũ vẫn khả dụng. Bước kế tiếp có lệnh liệt kê model trước khi chạy.

## 4. Build, kiểm tra model và bật AI

```bash
cd ~/forme-ai
sudo docker compose --env-file /dev/null -p forme-ai -f docker-compose.ai.yml build ai
```

Dockerfile dùng lock file hiện có; nếu pip báo không có phiên bản/wheel, dừng ở bước build, giữ backend đang hoạt động và kiểm tra log gói lỗi. Không tự bỏ phiên bản pin hoặc sửa backend để che lỗi.

Liệt kê model khả dụng với khóa vừa đặt; lệnh chỉ in tên model và phương thức hỗ trợ:

```bash
sudo docker compose --env-file /dev/null -p forme-ai -f docker-compose.ai.yml run --rm --no-deps -T --entrypoint python ai - <<'PY'
import json, os, urllib.request
url = 'https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000'
req = urllib.request.Request(url, headers={'x-goog-api-key': os.environ['GEMINI_API_KEY']})
with urllib.request.urlopen(req, timeout=20) as response:
    data = json.load(response)
for model in data.get('models', []):
    print(model['name'].removeprefix('models/'), ','.join(model.get('supportedGenerationMethods', [])))
PY
```

Chọn model có `generateContent` và hỗ trợ JSON structured output theo tài liệu Google; đặt mã không có tiền tố `models/` vào `GEMINI_MODEL`. Xác minh model embedding trong danh sách hoặc chọn lexical. Sau đó:

```bash
nano .env.ai
sudo docker compose --env-file /dev/null -p forme-ai -f docker-compose.ai.yml up -d --no-build --wait --wait-timeout 180 ai
sudo docker compose --env-file /dev/null -p forme-ai -f docker-compose.ai.yml ps
```

Không thêm inbound Security Group cho 8001. Container AI chỉ bind loopback và backend host-network truy cập được cổng này. Chỉ cần EC2 có outbound HTTPS/DNS tới Google và registry/package repositories khi build.

Kiểm tra health có xác thực, không in token:

```bash
sudo docker compose --env-file /dev/null -p forme-ai -f docker-compose.ai.yml exec -T ai python - <<'PY'
import os, urllib.request
req = urllib.request.Request('http://127.0.0.1:8001/health', headers={'Authorization':'Bearer '+os.environ['AI_API_KEY']})
with urllib.request.urlopen(req, timeout=5) as response:
    print(response.read().decode())
PY
```

Kỳ vọng `status=UP`, `geminiConfigured=true`. Trường `geminiConfigured` chỉ xác nhận đã điền khóa, chưa chứng minh Google chấp nhận khóa hoặc model hoạt động.

## 5. Nối backend đang chạy với AI

File `application.yml` đã đọc biến môi trường; không cần hardcode khóa vào đó.

```bash
cd ~/ttcsn/TTCSN/backend
cp -p .env ".env.before-ai.$(date +%Y%m%d-%H%M%S)"
nano .env
```

Chỉnh đúng bốn biến, mỗi biến chỉ một dòng:

```dotenv
AI_API_URL=http://127.0.0.1:8001
AI_API_KEY=THAY_CUNG_TOKEN_NOI_BO_TRONG_ENV_AI
AI_CONNECT_TIMEOUT_SECONDS=2
AI_READ_TIMEOUT_SECONDS=30
```

Giữ nguyên DB/JWT/Redis/Brevo/FRONTEND_ORIGIN đang hoạt động. Khóa Gemini chỉ nằm trong `~/forme-ai/.env.ai`; backend cần token nội bộ. Không đặt Google URL vào AI_API_URL vì adapter Spring hiện gọi `/health` và `/fashion/advise` của dịch vụ Python.

Sau khi AI healthy, tạo lại container backend bằng đúng image hiện tại để nhận env mới:

```bash
cd ~/ttcsn/TTCSN/backend
chmod 600 .env
FORME_IMAGE=$(cat .deploy/current-image)
test -n "$FORME_IMAGE"
sudo env "FORME_IMAGE=$FORME_IMAGE" docker compose --env-file /dev/null -p forme -f docker-compose.yml up -d --no-build --pull never --force-recreate --wait --wait-timeout 240 backend
curl --fail http://127.0.0.1:8081/api/health
```

Có gián đoạn ngắn khi thay container. `docker restart` không nạp lại nội dung env_file; cần recreate. Nếu không có `.deploy/current-image`, lấy tên image từ container backend đang chạy bằng `docker inspect` trước; không dùng tag latest hoặc tự đoán image. Image Java hiện tại cũng phải có endpoint `/api/ai/status` và adapter mới; nếu endpoint 404 hoặc phản hồi kiểu cũ, cần phát hành bản Java tương ứng qua pipeline hiện có.

## 6. Kiểm tra toàn luồng từ website

Frontend hiện đã lên server: giữ cấu hình URL API đang dùng. Không thêm Gemini key hay URL 8001 vào frontend. Trên website đang chạy, đăng nhập rồi vào Trợ lý:

1. Mở DevTools → Network, kiểm tra `/api/ai/status` trả `configured:true`, mode `Gemini + RAG`.
2. Bật ô cho phép Gemini, gửi “Gợi ý áo đi làm dưới 500 nghìn”. Kết quả cuối phải có mode **Gemini + RAG**, sản phẩm có thật/còn hàng/đúng ngân sách.
3. Thử “Tôi đi tiệc cưới ngoài trời mùa hè, không muốn mặc quần”. Đánh giá sản phẩm phù hợp, không chỉ kiểm tra HTTP 200.
4. Thử số đo/đổi trả/đơn của mình và đơn tài khoản khác. Những nhánh này xử lý nội bộ, không dùng để chứng minh Gemini đã hoạt động.
5. Nếu giao diện trả `Gợi ý local`, chưa coi là kiểm thử Gemini thành công. Kiểm tra khóa, model, hạn mức, kết nối và timeout; mã hiện tại có fallback nên HTTP 200 vẫn có thể là provider lỗi.

Nếu có Nginx/ALB phía trước, kiểm tra request POST `/api/ai/chat/stream` được chuyển tiếp nguyên JWT và SSE. Riêng location SSE trong Nginx cần `proxy_buffering off;` và timeout proxy đủ cho request (ví dụ 60s). Không thay toàn bộ cấu hình Nginx đang dùng; nếu chỉnh, chạy `sudo nginx -t` trước khi reload. Bản hiện tại gửi status rồi result, không stream từng token LLM.

## 7. Log, cập nhật và khôi phục

```bash
cd ~/forme-ai
sudo docker compose --env-file /dev/null -p forme-ai -f docker-compose.ai.yml logs --tail 100 ai
sudo docker stats --no-stream
```

Backend log:

```bash
cd ~/ttcsn/TTCSN/backend
container_id=$(sudo docker ps -aq --filter label=com.docker.compose.project=forme --filter label=com.docker.compose.service=backend)
sudo docker logs --tail 100 "$container_id"
```

Không in toàn bộ `.env` hoặc `docker inspect` phần environment để chia sẻ log. Provider hiện chuyển lỗi Gemini sang local mà không phân loại nguyên nhân, nên không thấy stack trace không đồng nghĩa đã gọi Gemini thành công.

Đổi khóa/model: chỉnh `.env.ai`, rồi chạy lại `up -d --no-build --force-recreate --wait ai` với cùng các cờ Compose ở trên. Backend chỉ cần recreate nếu đổi token nội bộ, URL hoặc timeout của backend.

Nếu cần tạm ngắt AI, đặt `AI_API_URL=` trong backend `.env` và recreate backend bằng đúng image đang chạy; ứng dụng quay về gợi ý local, FAQ/size/đơn hàng. Sau đó có thể `stop ai` bằng Compose riêng. Giữ volume `ai-data`; không chạy `down -v`.

Workflow hiện tại chỉ build/push/deploy backend Java. Thay đổi Python/Dockerfile.ai chưa tự cập nhật AI server. Sau khi deploy thủ công thành công, có thể bổ sung pipeline ECR riêng cho image AI. Không merge lẫn hai Compose project bằng một lệnh up không rõ phạm vi.

## Tài liệu chính thức đã đối chiếu

- [Docker host networking](https://docs.docker.com/engine/network/drivers/host/): container host-mode dùng chung network với EC2.
- [Dockerfile-specific dockerignore](https://docs.docker.com/build/concepts/context/#filename-and-location): Dockerfile.ai.dockerignore ghi đè .dockerignore chỉ đóng gói Java có sẵn.
- [Gemini API keys](https://ai.google.dev/gemini-api/docs/api-key).
- [Gemini models.list](https://ai.google.dev/api/models).
- [Gemini structured output](https://ai.google.dev/gemini-api/docs/structured-output).

Các file deploy được chuẩn bị từ mã nguồn. Chưa build image trên Linux hoặc triển khai lên AWS trong phiên này; chỉ kiểm tra cấu hình cục bộ trong phạm vi công cụ có sẵn.
