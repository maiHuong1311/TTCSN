# Hỏi đáp Gemini trực tiếp — FORME

Từ bản cập nhật 24/10/2026, Spring Boot có thể gọi Gemini REST trực tiếp. Không cần Python, LangChain, Chroma, cổng 8001 hay AI_API_KEY khi dùng chế độ này. Frontend vẫn gọi API backend; khóa Google không được gửi tới trình duyệt.

## Cấu hình local

Điền trong file riêng `backend/.env`, giữ nguyên các cấu hình DB/Redis/Brevo nếu có:

```dotenv
GEMINI_API_KEY=KHOA_GOOGLE_CUA_BAN
GEMINI_MODEL=gemini-3.5-flash-lite
AI_CONNECT_TIMEOUT_SECONDS=3
AI_READ_TIMEOUT_SECONDS=30
```

Không ghi khóa vào application.yml, Git hoặc chat. File .env đã nằm trong .gitignore. Từ thư mục gốc, khởi động backend và frontend bằng hai terminal:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File project-data/scripts/Start-Backend.ps1
```

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File project-data/scripts/Start-Frontend.ps1
```

Script backend đọc .env cho tiến trình mới, dùng JDK đã cài trong Program Files/Java nếu JAVA_HOME đang trỏ tới thư mục không tồn tại và đặt cache Maven trong project-data/.m2. Không đổi Java toàn hệ thống. Biến môi trường đã có ở terminal được ưu tiên hơn .env; sau khi đổi khóa hãy khởi động tiến trình backend mới với đúng môi trường.

Mở `http://127.0.0.1:5173/advisor`, đăng nhập và bật **Cho phép dùng Gemini để trả lời câu hỏi và gợi ý trang phục**.

## Cách sử dụng và phạm vi

- “Linen và cotton khác nhau thế nào? Nên chọn chất liệu nào khi trời nóng?”
- “Tôi chỉ muốn mua áo nữ đi làm dưới 500 nghìn, không gợi ý quần hoặc váy.”
- “Áo oversize nên phối thế nào cho gọn gàng?”
- “Tôi đi tiệc cưới ngoài trời mùa hè thì nên mặc gì?”

Giao diện chỉ hiển thị nội dung tư vấn, không có nhãn kỹ thuật. Khi cần xác minh, phản hồi API/SSE có trường `mode` là **Gemini API** nếu Google trả lời thành công. Nội dung tư vấn do Gemini tạo, không chỉ ghép tên sản phẩm bằng mẫu cố định. Backend lọc ngân sách trước khi gửi danh mục, sau đó kiểm tra lại ID, trạng thái, giá và tồn kho. ID không hợp lệ làm phản hồi provider bị từ chối; nếu giá/tồn kho thay đổi sau khi gọi model, nội dung cũ không được dùng để giới thiệu sản phẩm bị loại.

Đây là hỏi đáp từng câu, chưa có bộ nhớ hội thoại nhiều lượt. Khi hỏi tiếp, nhắc lại sản phẩm/bối cảnh cần tư vấn. Prompt yêu cầu model chỉ dùng chính sách/danh mục được cung cấp nhưng không bảo đảm tuyệt đối mọi nội dung tự do đều chính xác. Giá, màu và tồn kho hiện tại hiển thị trên thẻ sản phẩm.

Size vẫn đối chiếu bảng khi chọn sản phẩm/bảng và nhập số đo. FAQ dùng chính sách, đơn hàng chỉ truy cập sau kiểm tra chủ đơn. Các nhánh này không cần Gemini. Không suy ra size chỉ từ chiều cao/cân nặng.

Chỉ câu hỏi được khách cho phép và dữ liệu công khai của cửa hàng gửi sang Google. Các trường số đo/hồ sơ có cấu trúc và thông tin đơn hàng không gửi. Một số mẫu email/số điện thoại trong câu hỏi được che, nhưng không nhận diện mọi dữ liệu nhạy cảm. Không nhập mật khẩu hoặc thông tin thanh toán vào câu hỏi.

## Kiểm tra

Các lệnh đọc khóa nội bộ và không in khóa:

```powershell
# Kiểm tra quyền truy cập model, chưa tạo câu trả lời.
node project-data/tools/check-gemini.mjs

# Tạo một câu chào thật và kiểm tra JSON schema.
node project-data/tools/check-gemini.mjs --generate

# Liệt kê model tài khoản được dùng.
node project-data/tools/check-gemini.mjs --list

# Test toàn luồng Vite -> Spring Boot -> Gemini, sử dụng hạn mức Google.
node project-data/tools/test-gemini-live.mjs
```

Script test HTTP giới hạn host local, mặc định dùng admin mẫu; có thể đặt TEST_ADMIN_EMAIL/TEST_ADMIN_PASSWORD cho tài khoản test khác. Nó không tạo đơn, không gửi email, không ghi lịch sử tương tác. Kết quả nằm tại `project-data/.local/gemini-live-results.json`, không chứa khóa hoặc JWT.

Test Java dùng HTTP giả lập, không gọi Google: GeminiFashionAIClientTest, HttpFashionAIProviderTest, FashionAIServiceTest. Có kiểm tra quota, timeout, JSON sai, ID giả, dữ liệu private không vào request, consent và thay đổi giá/tồn kho. Test frontend hiện có kiểm tra form consent và parser SSE.

`GET /api/ai/status` chỉ xác nhận có cấu hình, không gọi model tính phí mỗi lần mở trang. Cần phản hồi cuối có `mode=Gemini API` trong Network của DevTools hoặc lệnh --generate thành công để xác nhận kết nối thật. Khi Google lỗi/hết quota/timeout, phản hồi có mode **Gợi ý local** cùng thông báo phù hợp trong nội dung; nhãn mode không hiện trên giao diện.

## AWS với backend/frontend đã deploy

Build/deploy lại image Java từ bản có GeminiFashionAIClient qua pipeline hiện tại. Thêm bốn biến GEMINI_API_KEY, GEMINI_MODEL, AI_CONNECT_TIMEOUT_SECONDS, AI_READ_TIMEOUT_SECONDS vào .env backend trên EC2; giữ nguyên DB/JWT/Redis/Brevo. Compose hiện tại đã đọc file này nên không cần thêm container AI hoặc mở cổng mới.

Nếu chỉ đổi biến môi trường trên image đã hỗ trợ Gemini trực tiếp, recreate container backend để nạp env mới; docker restart không nạp lại env_file. Frontend cần build lại để cập nhật nhãn checkbox, nhưng cấu trúc API hỏi đáp/SSE giữ nguyên. Không đưa khóa vào React hoặc GitHub; chỉ push mã nguồn, khóa ở .env server.

Khi GEMINI_API_KEY được đặt, chế độ trực tiếp được ưu tiên dù còn AI_API_URL cũ. Nếu không đặt khóa Google nhưng có AI_API_URL, adapter Python cũ vẫn tương thích; các tài liệu FastAPI/Chroma mô tả phương án tùy chọn này.

SSE gửi trạng thái và kết quả cuối, chưa stream từng token. Timeout Google là 30 giây theo cấu hình trên; Nginx/ALB cần timeout đủ và SSE không bị buffering. Không đảm bảo mọi request đạt 2–3 giây.

Tài liệu chính thức: [GenerateContent](https://ai.google.dev/api/generate-content), [models](https://ai.google.dev/api/models), [structured output](https://ai.google.dev/gemini-api/docs/structured-output).
