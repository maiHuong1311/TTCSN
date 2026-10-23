# Kết quả kiểm tra — 18/10/2026

## Cấu hình deploy EC2 — 19/10/2026

- Thêm Docker Compose chạy backend cổng 8081, dùng MySQL/Redis trên host; `.env` nằm cạnh Compose tại `~/ttcsn/TTCSN/backend`, được đọc raw và không bị source/ghi đè. Thư mục ảnh bind mount `backend/uploads/product-images`.
- `bash -n backend/deploy.sh` đạt. Script test bằng Docker/AWS giả: **8 ca đạt**, gồm deploy thành công, rollback thành công/thất bại, lỗi bản đầu, lỗi pull không thay container, từ chối env/tag sai và thiếu Compose có thông báo cài plugin. Các kiểm thử mô phỏng không kết nối máy chủ hoặc thay đổi database.
- Docker Compose chính thức v5.6.0 (đã xác minh SHA-256) chạy `config --quiet` thành công với fixture giả; kiểm tra host networking, port 8081, dấu `$`/`#` và dấu `&` trong JDBC URL. `config` escape dollar khi kết xuất cấu hình; fixture đã kiểm tra có xét quy tắc này.
- Workflow đã được `actionlint v1.7.12` (xác minh SHA-256) kiểm tra thành công. Các action được ghim bằng commit SHA. Workflow chỉ deploy main, test với MySQL/Redis riêng, push ECR rồi SSH gọi script theo SHA.
- Chưa build Docker image thực vì máy phiên này không có Docker Engine; chưa chạy workflow hoặc deploy EC2. Cần khai báo quyền push ECR cho GitHub, các SSH secrets và cài Compose >=2.30 trên EC2 trước khi chạy. Hướng dẫn: `DEPLOY_EC2.md`.

## Tải ảnh sản phẩm từ máy — 18/10/2026

- Thay ô nhập liên kết ở form sản phẩm và biến thể bằng nút chọn ảnh JPG/PNG từ máy, xem trước và xóa ảnh khỏi danh sách. Chờ tải ảnh hoàn tất trước khi lưu; ảnh theo màu được chia sẻ cho các kích cỡ cùng màu.
- API multipart `/api/admin/product-images` chỉ dành cho Admin; giới hạn 5 MB, kiểm tra nội dung ảnh thực, kích thước và ghi lại ảnh bằng ImageIO với tên UUID. GET `/api/images/{filename}` trả ảnh công khai để thẻ ảnh có thể hiển thị, kiểm tra tên tệp và không chấp nhận đường dẫn tùy ý. Không thay đổi schema MySQL.
- Maven verify trên MySQL test + Redis local: **49 tests thành công, không lỗi, không bỏ qua**. Có kiểm tra phân quyền upload, tải lại ảnh không cần JWT, lưu bền sau khi tạo lại service, tệp giả ảnh, quá dung lượng và đường dẫn không hợp lệ. Log: `.local/image-upload-backend-verify.log`.
- Kiểm thử frontend bổ sung multipart boundary, ảnh xem trước, giữ ảnh cũ khi upload lỗi, xóa ảnh và lưu URL trả về vào gallery/biến thể; kích cỡ vẫn được để trống.
- H2 mặc định không hỗ trợ cú pháp MySQL trong migration V11 hiện có; suite backend được chạy trên `fashion_store_test`, không sửa migration đã áp dụng.

## OTP quên mật khẩu với Redis local

- Cấu hình cuối theo người dùng: Redis `127.0.0.1:6379`, không mật khẩu, TLS tắt qua `REDIS_SSL_ENABLED=false`. Đã chạy Redis Windows portable trong `.local`, bind loopback, PING/PONG thành công. Start-Redis.ps1 chạy lại dùng instance đang có; cú pháp PowerShell hợp lệ.
- Backend `mvnw verify` với MySQL và `RUN_REDIS_INTEGRATION=true`: **41 test thành công, không lỗi và không bỏ qua test nào**. Redis thật kiểm tra TTL, cooldown, giới hạn ngày, hủy OTP sau 5 lần sai, mã mới vô hiệu mã cũ, hết hạn và chỉ một request tiêu thụ phiên khi chạy đồng thời. Chỉ tạo/xóa key test có namespace riêng; không dùng FLUSHDB hoặc quét/xóa key khác.
- Frontend: ESLint, TypeScript/Vite build và **19 test thành công**. Kiểm tra email → OTP → mật khẩu, không hiển thị ô mật khẩu trước khi xác minh, nhập sai vẫn ở bước OTP, xác nhận mật khẩu và xóa phiên cũ sau đổi mật khẩu. Liên kết kiểu cũ không mở bước đặt mật khẩu.
- HTTP bản local: quên mật khẩu với email ngẫu nhiên chưa đăng ký trả 200 và thông báo OTP; mã sai/phiên thiếu trả 400 bằng tiếng Việt; admin cũ đăng nhập 200; catalog chưa đăng nhập vẫn 401. Đã xóa đúng 4 key của ca smoke. Không gửi email thật trong kiểm tra tự động.
- Endpoint Upstash ban đầu NXDOMAIN nên đã được thay bằng Redis local theo phản hồi người dùng; không còn là điều kiện chặn. Không có migration mới: bảng/token liên kết cũ không còn được dùng, lịch sử Flyway giữ nguyên.

Log: `project-data/.local/otp-redis-real-verify.log`, `project-data/.local/otp-redis-frontend-tests.log`, `project-data/.local/backend-otp-local.log`, `project-data/.local/redis-local.log`. Chưa có browser session để kiểm tra trực quan toàn trang.

## Brevo API và ảnh theo màu sản phẩm

- Brevo API được kiểm tra read-only qua `/v3/account` và `/v3/senders`: HTTP 200, người gửi được cung cấp tồn tại và active. Không gửi thư thật tự động. Khóa lưu riêng trong `project-data/.local/mail.properties`; đã kiểm tra không có khóa thật trong source frontend/backend hoặc tài liệu chia sẻ.
- Backend `mvnw verify` trên MySQL: **34 test thành công**, không lỗi. Test mới kiểm tra payload/header gửi Brevo bằng mock HTTP, lỗi nhà cung cấp không lộ nội dung/key, chuyển từ SMTP sang API và ảnh giỏ hàng theo màu. Kiểm thử mock không gọi Brevo thật.
- Frontend: ESLint, TypeScript/Vite build và **18 test thành công**. Kiểm tra đổi ảnh khi chọn màu trên card, lookbook chưa đăng nhập và trang chi tiết; mở trang theo màu từ card; giữ size còn hàng; chặn mua màu hết hàng; thêm đúng variant ID vào giỏ.
- Flyway V9/V10 áp dụng thành công cho MySQL ứng dụng. Kiểm tra HTTP bản local: **8 sản phẩm, 19 màu, 19 ảnh local** đều trả 200 với MIME JPEG/PNG; mỗi màu có ảnh tương ứng. Catalog chưa đăng nhập vẫn 401. Yêu cầu quên mật khẩu với email ngẫu nhiên chưa đăng ký trả 200, xác nhận backend đã nạp cấu hình gửi thư mà không gửi email.
- Đã kiểm tra trực tiếp ảnh các mẫu thay thế và màu bổ sung; chưa kiểm tra trực quan toàn trang vì không có browser session. Một số mẫu một màu được thay bằng mẫu cùng loại có nhiều màu thật, ghi nguồn trong `ASSETS.md` và `assets/photos/colors.json`.

Log: `project-data/.local/brevo-colors-verify.log`, `project-data/.local/brevo-colors-frontend-tests.log`, `project-data/.local/backend-brevo-colors-local.log`.

## Đăng xuất, quên mật khẩu và font tiếng Việt

- Backend `mvnw verify` trên MySQL: **30 test thành công**, không lỗi. Các ca mới kiểm tra token ngẫu nhiên/đã dùng/hết hạn, thay liên kết cũ khi gửi lại, giới hạn gửi, email không tồn tại/bị khóa có cùng phản hồi, lỗi gửi thư giữ nguyên mật khẩu, đổi mật khẩu vô hiệu JWT cũ và hai request đồng thời chỉ một request thành công.
- Frontend: ESLint, TypeScript/Vite build và **14 test thành công**. Kiểm tra nút Đăng xuất ở header xóa phiên/cache riêng; gửi yêu cầu quên mật khẩu; xác nhận mật khẩu không trùng thì không gọi API; đặt lại thành công yêu cầu đăng nhập mới; thiếu token có đường dẫn yêu cầu lại.
- Font tiêu đề editorial, banner và khối tư vấn đổi sang Arial; editorial bỏ giãn chữ và giữ từ nguyên vẹn khi xuống dòng. Chưa kiểm tra trực quan toàn trang vì không có browser session.
- Brevo chưa có thông tin SMTP cá nhân, nên test gửi thư dùng mock, chưa xác nhận giao thư thật. Backend áp dụng Flyway V8 khi khởi động local; không chỉnh sửa migration cũ.

Log: `project-data/.local/password-reset-verify.log`, `project-data/.local/password-reset-frontend-tests.log`, `project-data/.local/backend-password-reset-local.log`.

## Xác minh email, số điện thoại và thông báo tiếng Việt

- Backend `mvnw verify` trên MySQL local: **23 test thành công**, không lỗi. Bao gồm mã đúng, mã hết hạn, mã đã dùng, lưu đủ 5 lần nhập sai qua các HTTP request thất bại, hạn gửi lại, giới hạn ngày, miền email tạm thời, định dạng điện thoại và rollback khi gửi mail thất bại.
- Mail adapter được kiểm tra người nhận/nội dung bằng mock; không gửi thư thật qua Brevo vì chưa có SMTP login/key/email người gửi.
- Frontend: ESLint, TypeScript/Vite build và **10 test thành công**. Kiểm tra thông báo native bằng tiếng Việt, xóa lỗi khi nhập lại, định dạng số Việt Nam, gửi/nhập mã trên form đăng ký và hàng ưu đãi có 3 sản phẩm.
- HTTP qua Vite proxy thực tế: trang React 200; số điện thoại nước ngoài và thiếu mã trả 400 với lỗi tiếng Việt; chưa cấu hình SMTP trả 503 và không tạo tài khoản; admin cũ đăng nhập 200; danh mục chưa đăng nhập vẫn 401.
- Flyway V7 áp dụng thành công cho database ứng dụng và database test; backend đã khởi động lại ở 8080. File `project-data/.local/mail.properties` được chuẩn bị với các giá trị SMTP cá nhân để trống.
- Script Start-Backend đã kiểm tra cú pháp PowerShell; smoke.mjs đã kiểm tra cú pháp Node và yêu cầu token của hai khách hàng đã xác minh, không bỏ qua email.

Log backend: `project-data/.local/email-backend-verify.log`, `project-data/.local/backend-email-local.log`. Chưa kiểm tra trực quan toàn trang trên trình duyệt vì không có browser session khả dụng.

## Giao diện tham khảo Cardina

- ESLint, TypeScript/Vite build và 7 test frontend đều thành công, bao gồm chuyển banner và trang giới thiệu khi chưa đăng nhập.
- Backend `mvnw verify`: 13 test thành công trên MySQL thật; Flyway V6 áp dụng thành công cho dữ liệu mẫu.
- Kiểm tra HTTP qua Vite: trang React 200, đăng nhập 200, 8 sản phẩm từ MySQL dùng ảnh JPEG local, toàn bộ 8 ảnh sản phẩm và 2 ảnh bộ sưu tập trả 200. API danh mục khi chưa đăng nhập vẫn trả 401.
- Banner, danh mục và thẻ sản phẩm dùng ảnh thật đã kiểm tra trực tiếp từng ảnh; nguồn ghi trong `ASSETS.md`.
- Chưa kiểm tra trực quan toàn trang trên trình duyệt vì không có browser session khả dụng.

Log: `project-data/.local/redesign-frontend-tests.log`, `project-data/.local/redesign-backend-verify.log`, `project-data/.local/backend-local.log`.

## Kiểm tra chức năng nền — 17/10/2026

- Backend: `mvnw verify` thành công, 13 test; chạy cả H2 smoke và MySQL 9.5 thật trên database riêng `fashion_store_test`.
- Frontend: ESLint, TypeScript/Vite build và 5 test Vitest/Testing Library đều thành công.
- Smoke HTTP trên MySQL: tranh mua sản phẩm cuối, hủy hoàn tồn, tranh lượt coupon cuối, SSE có JWT đều thành công. Fixture được giữ riêng trong database test.
- Kiểm tra qua Vite proxy: trang React 200, SVG 200, health API 200, admin login 200, danh mục seed có 8 sản phẩm, AI báo Not connected.
- Các script PowerShell đã kiểm tra cú pháp; Start-MySql chạy idempotent và Start-Backend khởi động ứng dụng thành công trên 127.0.0.1:8080.
- Dịch vụ đang chạy lúc bàn giao: React 127.0.0.1:5173, Spring Boot 127.0.0.1:8080, MySQL riêng 127.0.0.1:3307.

Giới hạn xác minh: không có browser session nên chưa kiểm tra trực quan desktop/mobile. Không có Docker nên chưa chạy profile Testcontainers; database được kiểm tra trực tiếp bằng MySQL local. Không gọi AI thật, không thu tiền qua gateway thật. Flyway từ base cảnh báo MySQL 9.5 mới hơn dải phiên bản được vendor xác nhận, nhưng migration, schema validation và suite MySQL thực tế đều đã qua.

Log chi tiết ở `project-data/.local/final-verify.log`, `project-data/.local/final-mysql-verify.log` và `project-data/.local/backend-local.log` (không đưa log vào Git).

## QR thanh toán giả lập — 18/10/2026

- Maven verify trên MySQL `fashion_store_test` và Redis local thật: **44 tests, 0 failures, 0 errors, 0 skipped**; `project-data/.local/demo-backend-verify.log`.
- Frontend: lint và production build đạt; **22 tests đạt**, gồm trang demo không cần đăng nhập, kết quả thành công, liên kết thiếu token, QR PNG tạo local và liên kết cùng origin. Log `project-data/.local/demo-frontend-tests.log`.
- Test backend mới: chủ đơn mới được cấp phiên, API cấp phiên cần JWT; token chỉ lưu hash; response public không lộ địa chỉ/số điện thoại; số tiền client không ghi đè được; gọi lặp không tạo mã giao dịch khác; từ chối kết quả đối nghịch, mã hết hạn/mã bị thay, đơn đã hủy và thanh toán thủ công; thất bại có thể tạo phiên mới; chưa trả tiền không được giao hàng.
- Backend local đã áp dụng V11 và khởi động trên 8080. HTTP smoke: health UP; catalog không JWT vẫn 401; phiên không tồn tại 404; token sai định dạng 400 với thông báo tiếng Việt. Frontend 5173 trả HTTP 200. Không gửi email hoặc thu tiền thật khi kiểm thử.
- Chưa kiểm tra quét QR bằng điện thoại vật lý; hướng dẫn cùng Wi-Fi và tùy chọn `Start-Frontend.ps1 -Lan` có trong README.

## Giao diện admin riêng — 18/10/2026

- Frontend lint/build đạt; 25 tests đạt (`project-data/.local/admin-layout-frontend-tests.log`). Ba test điều hướng mới kiểm tra admin vào `/cart` được chuyển đến quản trị không có UI mua sắm, đăng xuất xóa phiên/cache, customer không vào admin và admin đăng nhập bỏ qua đường dẫn trở lại checkout.
- Maven verify đạt 44 tests, không lỗi/không bỏ qua, trên MySQL test và Redis local thật (`project-data/.local/admin-layout-backend-verify.log`). Không đổi quyền API hoặc schema trong thay đổi này.

## Bổ sung 10 sản phẩm — 18/10/2026

- V12 đã áp dụng thành công trên MySQL ứng dụng và MySQL test. API ứng dụng hiện trả 18 sản phẩm; xác nhận đúng 10 sản phẩm mới qua SKU FRM-A26, 78 biến thể có tồn kho mẫu 15, mỗi sản phẩm 2 màu và đúng một ảnh chính. Cả 20 ảnh được frontend phục vụ HTTP 200 với Content-Type image; đã mở bảng ảnh tổng hợp để đối chiếu hình và màu.
- Frontend lint/build đạt, 25 tests đạt (`project-data/.local/catalog-ten-frontend-tests.log`). Maven verify trên MySQL test + Redis thật: 44 tests đạt, không lỗi/không bỏ qua (`project-data/.local/catalog-ten-backend-verify.log`). Không gửi email hoặc gọi thanh toán/AI thật.

## Đủ 20 sản phẩm / hai trang — 18/10/2026

- V13 đã áp dụng trên MySQL local: thêm áo cotton cổ thuyền xếp ly eo và chân váy kaki dáng suông xẻ sau, tổng 16 biến thể, 4 ảnh màu thật.
- Smoke API xác nhận tổng 20 sản phẩm, 2 trang, mỗi trang 10; trang 1 chứa ID 20–11, trang 2 chứa ID 10–1, không trùng. Thứ tự có ID làm khóa phụ khi ngày tạo bằng nhau. Hai mẫu mới có đủ 2 màu, 8 biến thể/mẫu; 4 ảnh frontend trả HTTP 200.
- Frontend lint/build đạt, 25 tests đạt (`project-data/.local/catalog-twenty-frontend-tests.log`).
- Maven verify đạt 44 tests, không lỗi/không bỏ qua trên MySQL test và Redis local thật (`project-data/.local/catalog-twenty-backend-verify.log`).

## Lấp đầy hai trang catalog — 18/10/2026

- V14 đã áp dụng vào MySQL local/test, thêm 4 sản phẩm, 32 biến thể, 8 ảnh thật. API local xác nhận tổng 24 sản phẩm, 2 trang × 12 sản phẩm; trang 1 ID 24–13, trang 2 ID 12–1, không trùng. Với lưới 4 cột trong ảnh người dùng, mỗi trang có đúng 3 hàng đầy đủ; 12 cũng chia hết cho lưới 2/3 cột ở kích thước nhỏ hơn.
- Cả 4 mẫu mới đều có 2 màu và 8 biến thể; 8 ảnh local trả HTTP 200 với Content-Type image. Đã mở ảnh để đối chiếu kiểu dáng. Không sửa giá/tồn kho hoặc sản phẩm cũ.
- Frontend lint/build đạt, 25 tests đạt (`project-data/.local/catalog-full-pages-frontend.log`). Backend Maven verify đạt 44 tests, không lỗi/không bỏ qua trên MySQL test + Redis local thật (`project-data/.local/catalog-full-pages-backend.log`).

## Sắp xếp ba thư mục — 18/10/2026

- Thư mục gốc chỉ có ba thư mục `backend`, `frontend`, `project-data` và file kỹ thuật `.gitignore`. Backend giữ nguyên package, Maven Wrapper, migration và source; frontend giữ nguyên ảnh và source. Tài liệu/config/scripts/tools và dữ liệu local/cache nằm trong project-data.
- Đã sao lưu MySQL vào `project-data/.local/backups/before-folder-reorganization.sql`, dừng MySQL/Redis trước khi chuyển, khởi động bằng script mới. So sánh trước/sau: 4 tài khoản, 24 sản phẩm, 2 đơn hàng. MySQL @@datadir trỏ `project-data/.local/mysql-data`; Redis PING trả PONG. Cấu hình Brevo riêng giữ ở `project-data/.local/mail.properties`, không xuất khóa.
- Backend health UP, frontend và proxy `/api/health` UP, đăng nhập admin thành công, catalog đủ 24 sản phẩm, ảnh local HTTP 200. Các script PowerShell và helper Node kiểm tra cú pháp đạt.
- Chạy `project-data/scripts/Verify.ps1 -MySql` với RUN_REDIS_INTEGRATION=true từ cấu trúc mới: Maven verify 44 tests đạt, không bỏ qua; frontend lint/build và 25 tests đạt. Log: `project-data/.local/reorganized-verify.log`. Không gửi mail/thu tiền/gọi AI thật khi kiểm tra.

## Kích cỡ không bắt buộc — 18/10/2026

- Form quản trị cho phép để trống kích cỡ; DTO chấp nhận thiếu/null/trống, service lưu Freesize để giữ schema và dữ liệu hiển thị nhất quán. Không cần migration.
- Integration test mới xác nhận cả ba trường hợp thiếu/null/khoảng trắng lưu thành Freesize; biến thể thêm được vào giỏ và đặt đơn COD, kích cỡ trong đơn đúng.
- Verify.ps1 -MySql với Redis integration: 45 backend tests đạt, 25 frontend tests đạt, lint/build đạt. Log `project-data/.local/optional-size-verify.log`. Backend đã khởi động lại và health UP. Không gửi email/thu tiền thật trong test.

## Đánh giá sau khi nhận hàng — 18/10/2026

- API tạo đánh giá bắt buộc đúng tài khoản mua, đúng sản phẩm trong đơn Delivered; giữ quy tắc mỗi tài khoản đánh giá sản phẩm một lần. GET /api/products/{id}/reviews/eligibility cần JWT, chỉ cung cấp quyền của tài khoản hiện tại. Frontend ẩn form khi chưa đủ điều kiện/đã đánh giá, cập nhật quyền sau gửi thành công.
- Integration tests phủ Pending, Confirmed, Shipping, Cancelled, Delivered; người khác và sản phẩm không mua bị từ chối; Delivered được gửi một lần và lần sau 409. Frontend tests xác nhận ẩn form, hiện và gửi với người đủ điều kiện, không hiện lại khi đã đánh giá.
- Verify.ps1 -MySql + Redis: 46 backend tests, 28 frontend tests đều đạt, lint/build đạt. Log project-data/.local/delivered-reviews-verify.log. Backend local đã khởi động; smoke quyền với tài khoản không mua trả canReview=false, gọi không JWT trả 401. Không gửi thư/thu tiền thật khi kiểm thử.

## AI local — 23/10/2026
Backend verify: 61 kiểm thử đạt, gồm MySQL migration V15 và Redis integration. Frontend: lint/build đạt, 43 kiểm thử đạt. Python FastAPI/Chroma: 12 kiểm thử đạt với HTTP Gemini giả lập, không gọi Google thật. Xem HUONG_DAN_AI_LOCAL.md để cấu hình khóa và chạy thủ công. Đã kiểm tra thủ công local: FAQ, size S và Gemini + RAG trả hai sản phẩm thực tế; API health và frontend HTTP 200. Khóa riêng không được commit. Chưa triển khai dịch vụ AI lên AWS.

## Gemini API trực tiếp — 24/10/2026

- Người dùng tự điền khóa trong backend/.env được Git bỏ qua. Đã kiểm tra khóa truy cập model gemini-3.5-flash-lite và tạo câu trả lời thật; không in hoặc đưa khóa vào mã nguồn.
- Spring Boot gọi Google trực tiếp, không cần dịch vụ Python. Kết quả có nhãn Gemini API, giữ nguyên API/SSE cho frontend. Tư vấn là nội dung của model; thẻ sản phẩm kiểm tra ID/trạng thái/giá/tồn kho tại backend. Model lỗi/quota/timeout có thông báo an toàn và nhãn gợi ý local.
- Backend: 33 test đạt, không lỗi hoặc bỏ qua: 29 unit về Gemini/provider/service/recommendation/size và 4 integration về size/FAQ/consent tìm kiếm/quyền xem đơn trên fashion_store_test. Test tự động dùng HTTP mock, không gọi Google.
- Frontend: 4 test AdvisorPage/ai API đạt, ESLint và TypeScript/Vite build đạt. Chưa thực hiện kiểm tra trình duyệt trực quan trong đợt này.
- Live qua Vite 5173 -> Spring Boot 8080 -> Google: 8/8 kiểm tra đạt. Năm lượt thực sự dùng Gemini gồm hỏi đáp chất liệu, áo nữ dưới 500 nghìn không lấy quần/váy, phối áo oversize, không bịa giày Nike ngoài danh mục và trả lời qua SSE. Ba kiểm tra nội bộ: tắt consent Gemini, ngân sách 0 và bảng size M.
- Lượt cuối các câu Gemini mất khoảng 1,3–3,3 giây; không phải cam kết độ trễ hoặc đánh giá tải lớn. Chi tiết: project-data/.local/gemini-live-results.json, không có khóa/JWT.
- Backend và frontend local đã được khởi động với bản cuối. Không tạo đơn hàng, không gửi email, không ghi lịch sử tương tác trong script live. Test integration dùng database riêng.
- Giới hạn: hỏi đáp từng câu, chưa có bộ nhớ hội thoại nhiều lượt hoặc token streaming; chưa deploy AWS. Hướng dẫn sử dụng/local/AWS: HUONG_DAN_GEMINI_TRUC_TIEP.md. Các báo cáo audit trước khi cấu hình Gemini là dữ liệu lịch sử, không còn phản ánh trạng thái kết nối hiện tại.

## Kiểm tra trước khi merge main — 24/10/2026

- Maven verify hoàn tất trên fashion_store_test và Redis local: 82 test đạt, 0 failures, 0 errors, 0 skipped; JAR Spring Boot đã đóng gói thành công. Log riêng: project-data/.local/pre-merge-backend-verify.log.
- Frontend ESLint, production build và toàn bộ 43 test đạt; bundle đã bỏ nhãn Gemini API trên giao diện.
- Kiểm tra 473 đường dẫn tracked/new không phát hiện khóa Gemini thực tế, private key hoặc API token. backend/.env và project-data/.local tiếp tục được Git bỏ qua.
