# Chạy trợ lý thời trang FORME trên máy local

> Cập nhật 24/10/2026: dùng **Spring Boot gọi Gemini trực tiếp** không cần dịch vụ Python — xem [hướng dẫn mới](HUONG_DAN_GEMINI_TRUC_TIEP.md). Phần dưới mô tả FastAPI/Chroma tùy chọn; khi GEMINI_API_KEY đặt cho backend, chế độ trực tiếp được ưu tiên.

## 1. Phạm vi đã triển khai

Spring Boot tiếp tục xử lý tài khoản, đơn hàng, giá và tồn kho. Dịch vụ Python FastAPI dùng LangChain và Chroma để truy xuất sản phẩm/chính sách, sau đó gọi Gemini để chọn sản phẩm phù hợp câu hỏi. Frontend hiển thị kết quả từ backend. Không cần tải model về máy, không cần GPU và chưa triển khai dịch vụ AI lên AWS.

- Gợi ý cá nhân hóa từ lịch sử xem, tìm kiếm, thêm giỏ và mua hàng khi khách bật đồng ý ghi nhận. Lịch sử được giữ tối đa 90 ngày; có thể xóa tại trang tài khoản. Tài khoản mới dùng sản phẩm phổ biến/còn hàng. Sản phẩm tương tự dùng nội dung; phối đồ dùng nhóm sản phẩm bổ trợ và dữ liệu mua cùng khi có.
- Tư vấn size dùng bốn bảng chính xác do cửa hàng cung cấp: áo nữ, quần nữ, chân váy, đầm. Trang `/size-guide` hiển thị bảng; quản trị viên chọn bảng áp dụng khi sửa sản phẩm. Giao diện dùng tên **Vòng ba**.
- Size được tính từ các số đo bắt buộc của bảng, không do Gemini tự đoán. Tại mốc chung chọn size lớn hơn; các số đo thuộc nhiều size thì chọn size lớn nhất và giải thích. Thiếu số đo, ngoài bảng hoặc hết size thì không tự thay bằng size khác. Chiều cao, cân nặng và dáng người chỉ là thông tin tham khảo, chưa phải mô hình suy luận size từ ảnh/cơ thể.
- Trang `/policies` chứa chính sách và liên hệ: 0982655491, maihunw@gmail.com, Nhổn, Hà Nội, 08:00–23:00. Phí giao hàng vẫn lấy từ trang thanh toán thực tế. QR thanh toán thử nghiệm không được coi là cổng thanh toán thật.
- Tra cứu đơn hàng yêu cầu đăng nhập và chỉ cho phép đọc đơn của chính tài khoản.

## 2. Chuẩn bị và cấu hình

Cần Java 17+, Node.js phù hợp dự án và Python **3.12** (script cũng hỗ trợ 3.11/3.13). Máy RAM 16 GB đáp ứng việc chạy API local; Gemini được xử lý trên dịch vụ Google.

Mở PowerShell trên Windows, không chạy những lệnh này trong phiên SSH Ubuntu:

```powershell
cd C:\TTCSN
powershell -ExecutionPolicy Bypass -File .\project-data\scripts\Setup-AI.ps1
```

Nếu `python` không phải bản phù hợp, truyền đường dẫn Python 3.12:

```powershell
.\project-data\scripts\Setup-AI.ps1 -Python 'C:\duong-dan-python\python.exe'
```

Script cài thư viện theo `backend/ai-service/requirements.lock.txt`, tạo môi trường Python trong `project-data/.local/fashion-ai-venv` và cập nhật phần AI trong **`backend/.env`**. Giữ nguyên các cấu hình khác của bạn.

Tạo khóa tại [Google AI Studio](https://aistudio.google.com/api-keys), tự điền vào file riêng:

```dotenv
AI_API_URL=http://127.0.0.1:8001
AI_API_KEY=<token-noi-bo-do-script-tu-tao>
GEMINI_API_KEY=<khoa-Google-cua-ban>
GEMINI_MODEL=gemini-3.5-flash-lite
GEMINI_EMBEDDING_MODEL=gemini-embedding-001
AI_EMBEDDING_MODE=gemini
AI_CONNECT_TIMEOUT_SECONDS=2
AI_READ_TIMEOUT_SECONDS=30
```

`AI_API_KEY` xác thực Spring Boot với FastAPI, **khác** `GEMINI_API_KEY` của Google. Không đưa khóa lên Git, không gửi vào chat và không điền khóa thật trong `.env.example`. Nếu chưa có khóa Google, để trống `GEMINI_API_KEY`; bảng size, FAQ và gợi ý local vẫn hoạt động. Hạn mức miễn phí phụ thuộc tài khoản, khu vực và chính sách Google; không phải miễn phí không giới hạn.

## 3. Khởi động

Mở các cửa sổ PowerShell riêng tại `C:\TTCSN`. Khởi động dữ liệu trước:

```powershell
.\project-data\scripts\Start-MySql.ps1
.\project-data\scripts\Start-Redis.ps1
```

Nếu MySQL/Redis của bạn đã chạy thì dùng cấu hình hiện có trong `backend/.env`, không chạy thêm một dịch vụ trùng cổng. MySQL local của các script mặc định dùng cổng 3307; Redis dùng 6379. Xem thêm [hướng dẫn dự án](HUONG_DAN_CHAY_DU_AN.md) về tài khoản database.

Cửa sổ dịch vụ AI:

```powershell
.\project-data\scripts\Start-AI.ps1
```

Cửa sổ backend:

```powershell
.\project-data\scripts\Start-Backend.ps1
```

Cửa sổ frontend:

```powershell
.\project-data\scripts\Start-Frontend.ps1
```

Mở địa chỉ Vite in trong terminal (mặc định `http://localhost:5173`). Vào **Trợ lý**, đăng nhập tài khoản khách hàng. Backend mặc định `http://localhost:8080`, AI chỉ lắng nghe `127.0.0.1:8001`. Sau khi đổi khóa/cấu hình, khởi động lại dịch vụ AI và backend.

## 4. Thử chức năng

1. Mở hướng dẫn size và chính sách; kiểm tra đủ bảng S–XXL và thông tin liên hệ.
2. Trong quản trị sản phẩm, chọn bảng size đúng. Mở trang chi tiết, bấm tư vấn để mang sản phẩm sang trợ lý.
3. Áo nữ: ngực 82, eo 64, vai 35,5 cm → S nếu còn hàng. Quần: eo 67, vòng ba 92 cm → M nếu còn hàng. Thử số đo ngoài bảng để kiểm tra thông báo thay vì kết quả bịa.
4. Hỏi “Đổi trả trong bao nhiêu ngày?”, “Liên hệ cửa hàng thế nào?”. Câu trả lời lấy chính sách cửa hàng, không cần Google.
5. Với khóa Google hợp lệ, bật ô đồng ý sử dụng Gemini, hỏi “Gợi ý trang phục đi làm dưới 500 nghìn”. Kết quả chỉ tham chiếu sản phẩm thực tế còn hàng; nếu API lỗi/hết hạn mức, giao diện ghi rõ gợi ý local.
6. Hỏi “Đơn hàng #123” bằng tài khoản sở hữu đơn; tài khoản khác không được xem.
7. Bật ghi nhận tương tác trong tài khoản, xem/tìm kiếm/thêm giỏ để thử cá nhân hóa. Tắt đồng ý thì không ghi nhận mới; nút xóa lịch sử xóa dữ liệu đã lưu.

## 5. Dữ liệu và giới hạn

Bảng size/chính sách nằm trong `backend/src/main/resources/fashion/`. Liên kết sản phẩm với bảng được lưu bằng migration Flyway V15; không sửa migration đã chạy. Chroma lưu chỉ mục công khai tại `project-data/.local/ai-chroma`, không commit thư mục này.

Gemini embeddings tạo vector ngữ nghĩa khi có khóa và API hoạt động. Khi không có khóa hoặc lỗi dịch vụ, hệ thống chuyển sang truy xuất từ khóa bằng vector băm local; đây không phải model embedding ngữ nghĩa tự huấn luyện. Lựa chọn sản phẩm được kiểm tra lại giá, tồn kho và ID ở Spring Boot.

Số đo nhập trong các ô tư vấn, lịch sử tài khoản và dữ liệu đơn hàng không được gửi sang Gemini. Câu hỏi tự do có thể được gửi nếu khách đồng ý; dịch vụ che một số mẫu email/số điện thoại nhưng không bảo đảm nhận diện mọi dữ liệu cá nhân. Không nhập mật khẩu, số thẻ hoặc thông tin nhạy cảm trong câu hỏi. Việc xử lý dữ liệu của Google phụ thuộc loại dịch vụ/tài khoản sử dụng.

SSE hiện truyền trạng thái xử lý rồi kết quả có cấu trúc, chưa truyền từng token câu trả lời của LLM. Chưa xác nhận mục tiêu phản hồi 2–3 giây hoặc đánh giá chất lượng trên bộ dữ liệu lớn. Ngày 23/10/2026 đã kiểm tra thành công khóa Gemini và luồng Spring Boot → FastAPI → Gemini + RAG với câu hỏi gợi ý áo đi làm dưới 500 nghìn; kết quả trả hai sản phẩm có thật. Khóa chỉ nằm trong file .env riêng.

## 6. Kiểm thử

Bộ kiểm thử tự động: backend 61, frontend 43, Python 12 ca đạt; lint/build đạt. Provider và HTTP được giả lập, không gọi Google trong các bài kiểm thử tự động. Lượt kiểm tra thủ công với khóa do người dùng cấp đã xác nhận Gemini hoạt động:

```powershell
cd C:\TTCSN\backend
.\mvnw.cmd verify
cd ..\frontend
npm.cmd run lint
npm.cmd run build
npm.cmd run test
cd ..\backend\ai-service
..\..\project-data\.local\fashion-ai-venv\Scripts\python.exe -m pytest -q
```

Backend integration test cần database test theo [VALIDATION.md](VALIDATION.md). Không trỏ kiểm thử vào database khách hàng thật.

Tham khảo API: [Gemini model](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite), [structured output](https://ai.google.dev/gemini-api/docs/structured-output), [embeddings](https://ai.google.dev/gemini-api/docs/embeddings).
