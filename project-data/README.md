# FORME — Fashion E-Commerce

Máy mới bắt đầu tại [Hướng dẫn chạy dự án](HUONG_DAN_CHAY_DU_AN.md).

Dự án chạy local: **Java Spring Boot + MySQL + React.js**, phát triển tiếp từ `backend`. Giao diện tiếng Việt, viết bằng React/TypeScript với HTML và CSS thuần, React Query quản lý dữ liệu API, Zustand giữ phiên đăng nhập và tùy chọn UI. Quyết định điều chỉnh từ quy ước LearnFlow được ghi ở [DECISIONS.md](DECISIONS.md).

## Cấu trúc dự án

Thư mục gốc có ba thư mục chính:

```text
TTCSN/
├── backend/        # Java Spring Boot, Maven Wrapper, migrations và tests
├── frontend/       # React, CSS và ảnh sản phẩm local
└── project-data/   # Tài liệu, cấu hình mẫu, script và dữ liệu chạy local
    ├── README.md
    ├── EMAIL_SETUP.md
    ├── .env.example
    ├── config/
    ├── scripts/
    ├── tools/
    ├── .local/     # Dữ liệu MySQL/Redis, cấu hình mail riêng, log (không chia sẻ)
    ├── .m2/        # Cache Maven tải tự động (không chia sẻ)
    └── archives/   # Các bản ZIP (không đưa lồng vào ZIP mới)
```

Các lệnh bên dưới chạy từ **thư mục gốc đã giải nén** (ví dụ `C:\TTCSN`), không chạy từ bên trong `project-data`. File `.gitignore` ở thư mục gốc ngăn đưa dữ liệu riêng lên Git. Script tự xác định đường dẫn nên có thể giải nén dự án ở vị trí khác.

## Chạy nhanh trên Windows

Cần Java 17+ (đã kiểm tra với Java 23), Node.js 20.19+ và MySQL Server 8.0+. Maven Wrapper có sẵn; lần chạy đầu cần Internet để tải dependency. Không cần Docker để chạy local.

Trong PowerShell tại `C:\TTCSN`:

```powershell
# 1. Khởi động MySQL dành riêng cho dự án ở cổng 3307
.\project-data\scripts\Start-MySql.ps1

# Khởi động Redis local cho OTP ở cổng 6379
.\project-data\scripts\Start-Redis.ps1

# 2. Trong một terminal riêng, chạy backend
.\project-data\scripts\Start-Backend.ps1

# 3. Trong terminal khác, chạy React
.\project-data\scripts\Start-Frontend.ps1
```

Mở **http://127.0.0.1:5173**. Backend: **http://127.0.0.1:8080/api/health**.

Để đăng ký khách hàng mới, cấu hình **Brevo API** theo [EMAIL_SETUP.md](EMAIL_SETUP.md); vẫn hỗ trợ SMTP dự phòng. Người dùng phải nhận và nhập mã xác minh email trước khi tài khoản được tạo. Khi chưa cấu hình gửi mail, admin/tài khoản có sẵn vẫn đăng nhập được.

Trang đăng nhập có **Quên mật khẩu?**: nhập email → nhận/xác minh OTP → đặt mật khẩu mới. Brevo gửi mã; Redis local `127.0.0.1:6379` lưu OTP đã băm và TTL, không mật khẩu/TLS theo cấu hình local. OTP hết hạn 10 phút, tối đa 5 lần nhập sai; phiên sau xác minh hết hạn 5 phút. Chi tiết ở [EMAIL_SETUP.md](EMAIL_SETUP.md). Sau khi đổi mật khẩu, các phiên đăng nhập cũ bị vô hiệu hóa. Nút **Đăng xuất** nằm trên header và cuối trang khi đã đăng nhập.

Script MySQL dùng bản MySQL đã cài, tạo data directory tại `project-data/.local/mysql-data`, bind `127.0.0.1`, không sửa dịch vụ MySQL Windows đang có. Dữ liệu vẫn còn sau khi tắt máy. Tài khoản DB local là `fashion` / `fashion`; root của instance riêng là `local-root-change-me-2026`. Đây là thông tin mẫu cho môi trường local. Script backend tạo admin khi email chưa tồn tại:

| Vai trò | Email | Mật khẩu local |
|---|---|---|
| Admin | `admin@forme.local` | `FormeLocalAdmin2026!` |
| Customer | Đăng ký trên giao diện | Tự đặt |

Sau khi đăng nhập admin, hệ thống chuyển thẳng đến **Quản trị** với giao diện riêng. Tài khoản bị khóa sẽ mất quyền truy cập API kể cả token chưa hết hạn. Đổi admin mẫu khi khởi động bằng `-AdminEmail` và `-AdminPassword`; script chỉ tạo tài khoản mới, không ghi đè mật khẩu của tài khoản có sẵn.

Nếu PowerShell chặn script, có thể mở terminal cho phiên hiện tại bằng `powershell -ExecutionPolicy Bypass` rồi chạy lại. Không cần thay đổi execution policy toàn máy.

## Dùng MySQL của bạn

Tạo database và tài khoản có quyền tạo bảng/index/migration trong database đó:

```sql
CREATE DATABASE fashion_store CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'fashion'@'localhost' IDENTIFIED BY 'fashion';
GRANT ALL ON fashion_store.* TO 'fashion'@'localhost';
```

```powershell
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/fashion_store?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:DB_USERNAME='fashion'
$env:DB_PASSWORD='fashion'
.\project-data\scripts\Start-Backend.ps1
```

Flyway tự chạy migration V1–V14 và seed 24 sản phẩm, 202 biến thể, 4 danh mục và mã `WELCOME10`. Không import SQL bằng tay và không đổi `ddl-auto=validate`. Giữ nguyên migration đã áp dụng; sửa schema bằng migration mới. Ảnh chụp thật ở `frontend/public/assets/photos` được lưu local. Giao diện tham khảo Cardina; xem [ASSETS.md](ASSETS.md) để biết nguồn ảnh và thay đổi trong migration V6. Quản trị viên có thể thay ảnh bằng URL riêng.

## Các luồng đã triển khai

- Đăng ký với mã xác minh email Brevo, kiểm tra số di động Việt Nam; đăng nhập bằng email hoặc username với JWT, BCrypt, phân quyền Customer/Admin, khóa/mở người dùng.
- Danh mục phân cấp; tìm kiếm và lọc theo danh mục, giới tính; phân trang; chi tiết, ảnh, màu, size, tồn kho và SKU.
- 24 sản phẩm mẫu có 51 màu với ảnh chụp tương ứng lưu local; danh sách cửa hàng có 2 trang, mỗi trang 12 sản phẩm. Bấm ảnh màu trên thẻ hoặc trang chi tiết để đổi ảnh; kích cỡ/tồn kho và ảnh giỏ hàng theo đúng biến thể. Admin sửa được ảnh của từng biến thể; nguồn ảnh ở `frontend/public/assets/photos/colors.json` và `frontend/public/assets/photos/autumn-collection.json`.
- Giỏ hàng lưu MySQL, tăng/giảm/xóa sản phẩm; địa chỉ cá nhân và địa chỉ mặc định; hồ sơ gồm chiều cao, cân nặng, phong cách.
- Checkout tính tiền trên server, kiểm tra hạn mức/thời gian/đơn tối thiểu của mã giảm giá; vận chuyển 30.000đ, miễn từ 1.000.000đ trước giảm giá.
- Đặt hàng trong transaction, khóa các biến thể theo thứ tự để tránh oversell; coupon và giỏ được khóa để tránh dùng quá hạn mức/đặt đơn hai lần. Đơn Pending giữ tồn kho; hủy đơn hoàn lại tồn đúng một lần. Lượt coupon đã đặt vẫn được tính khi hủy để giữ giới hạn sử dụng nhất quán.
- Customer hủy khi Pending; admin chuyển Pending → Confirmed → Shipping → Delivered, hoặc hủy trước Shipping. Không cho nhảy trạng thái.
- COD chuyển Paid khi giao thành công. VietQR/EWallet tạo Pending, admin nhập mã giao dịch sau khi tự đối soát; đơn online phải Paid trước Shipping. **Chưa có gateway, QR ngân hàng hoặc chuyển tiền thật.** Refunded là trạng thái sổ theo dõi local, việc hoàn tiền ngoài hệ thống phải do cửa hàng xử lý.
- Lịch sử/chi tiết đơn hàng, in hóa đơn qua trình duyệt; review 1–5 sao và URL ảnh sau khi mua hàng đã Delivered, một review/người/sản phẩm.
- Admin quản lý sản phẩm, ảnh, biến thể, tồn kho, danh mục, coupon, người dùng, đơn hàng và báo cáo. Xóa sản phẩm là chuyển Inactive để giữ lịch sử đơn.
- Doanh thu từ đơn Delivered, sản phẩm bán chạy theo số lượng đã giao; thống kê views tổng hợp có sự đồng ý. Bật/tắt và xóa lịch sử xem trong My account.

## Phần AI để kết nối sau

Khi chưa có `AI_API_URL`, màn hình advisor ghi rõ **AI chưa được kết nối**. Gợi ý từ danh mục/lịch sử xem là logic local, không giả lập kết quả AI. Người mới nhận các sản phẩm mới; sản phẩm tương tự dùng cùng danh mục; phối đồ ưu tiên danh mục khác. Chưa có mô hình học máy được huấn luyện.

Adapter nằm ở `ai/FashionAIProvider` và `HttpFashionAIProvider`. Đặt biến môi trường trước khi chạy backend:

```powershell
$env:AI_API_URL='http://127.0.0.1:9000'
$env:AI_API_KEY='your-server-side-key'
```

Backend gọi `POST {AI_API_URL}/fashion/advise`, gửi:

```json
{"message":"An outfit for an outdoor wedding","heightCm":170,"weightKg":60,"catalog":["ProductResponse objects from this store"]}
```

Service AI cần trả JSON theo hợp đồng:

```json
{"productIds":[1,3],"suggestedSize":"M"}
```

`catalog` thực tế là danh sách object đầy đủ (tối đa 100 sản phẩm active), không phải chuỗi như phần minh họa. Backend loại ID ngoài danh mục, chỉ chấp nhận size XS/S/M/L/XL/XXL. Tên, giá, ảnh và đường dẫn hiển thị đều lấy từ DB; không render HTML/URL/tên sản phẩm do model tự tạo. Muốn dùng OpenAI/Claude hoặc API khác, triển khai adapter của bạn theo hợp đồng này. Key chỉ ở backend.

`/api/ai/chat/stream` dùng SSE để gửi trạng thái rồi kết quả. Đây chưa phải streaming từng token của LLM. FAQ vận chuyển/đổi trả và tra cứu **đơn của chính người dùng** hoạt động local. Khi API lỗi/timeout, UI báo rõ, không tạo tư vấn giả. Mục tiêu latency AI trong đề bài cần được đo lại khi có provider thật.

## Cấu trúc và API

```text
backend/src/main/java/com/learnflow/backend/
  auth, catalog, address, cart, coupon, order, review, admin, ai, common
backend/src/main/resources/db/migration/   # V1 base + các migration mới
frontend/src/
  api/ hooks/ pages/ components/ stores/ types/ lib/
project-data/scripts/                    # chạy local và kiểm tra
```

Các endpoint cần `Authorization: Bearer <jwt>` trừ register, login và health; catalog cũng cần đăng nhập theo AGENTS. UI Discover vẫn xem được trước đăng nhập. DTO dùng camelCase, thời gian ISO-8601 UTC, lỗi ProblemDetail và lỗi field trong `errors`.

| Nhóm | Endpoint chính |
|---|---|
| Auth | POST `/api/auth/register`, POST `/api/auth/login`, GET/PUT `/api/auth/me` |
| Catalog | GET `/api/categories`, GET `/api/products?page=0&size=12&q=...&categoryId=1&gender=Unisex`, GET `/api/products/{id}` |
| Gợi ý/views | GET `/api/recommendations?productId=1&match=true`, POST `/api/products/{id}/views`, DELETE `/api/interactions` |
| Địa chỉ | GET/POST `/api/addresses`, PUT/DELETE `/api/addresses/{id}` |
| Giỏ | GET `/api/carts/current`, POST/PUT `/api/carts/current/items`, DELETE `/api/carts/current/items/{id}` |
| Đơn hàng | POST `/api/orders/quote`, GET/POST `/api/orders`, GET `/api/orders/{id}`, POST `/api/orders/{id}/cancel` |
| Reviews | GET/POST `/api/products/{id}/reviews` |
| AI | GET `/api/ai/status`, POST `/api/ai/chat`, POST `/api/ai/chat/stream` |
| Admin | `/api/admin/products`, `/categories`, `/orders`, `/coupons`, `/users`, `/reports`, `/behavior` |
| Trạng thái/thanh toán | PATCH `/api/admin/orders/{id}/status`, POST `/api/admin/orders/{id}/payment`, PATCH `/api/admin/users/{id}/status` |

## Kiểm tra

```powershell
.\project-data\scripts\Verify.ps1           # backend verify, frontend lint/build/test
.\project-data\scripts\Verify.ps1 -MySql    # cùng bộ test chạy trên fashion_store_test ở MySQL local
# Khi có Docker:
.\project-data\scripts\Verify.ps1 -Container
```

Test đơn hàng rollback sau mỗi ca, sử dụng database test riêng. Test smoke mặc định kế thừa H2 từ base; kiểm tra MySQL thật quan trọng vì H2 không kiểm tra được toàn bộ khác biệt dialect/locking. Testcontainers profile dùng MySQL khi có Docker. Không test nào gọi AI thật. Có ArchUnit kiểm tra ranh giới Controller/Repository và repository theo domain; test coupon dùng Clock cố định.

Chạy từng phần:

```powershell
cd backend
.\mvnw.cmd '-Dmaven.repo.local=..\project-data\.m2\repository' verify
cd ..\frontend
npm ci
npm run lint
npm run build
npm run test
```

`npm run build` tạo `frontend/dist`; `npm run preview` xem bản build. Để gọi API khi preview, dùng `npm run dev` hoặc cấu hình reverse proxy `/api` tương tự Vite dev. Dừng backend/frontend bằng Ctrl+C. Instance MySQL của dự án có thể dừng bằng `mysqladmin --host=127.0.0.1 --port=3307 --user=root -p shutdown` và nhập root password local.

Smoke test đồng thời: chạy backend bằng database `fashion_store_test`, `SERVER_PORT=8081`, profile local và admin mẫu. Chuẩn bị hai tài khoản khách hàng đã xác minh trên server test, đặt `TEST_CUSTOMER_TOKENS` bằng hai JWT phân cách dấu phẩy rồi từ root chạy `node project-data/tools/smoke.mjs`. Script không bỏ qua xác minh email; test tự động trong Maven dùng mock gửi mail riêng để không gửi thư thật. Script tạo fixture test, kiểm tra tranh tồn kho/coupon, hoàn tồn và SSE; sản phẩm fixture được chuyển Inactive sau kiểm tra. Không dùng script này với dữ liệu thật. Có thể đặt `TEST_API_URL`, `TEST_ADMIN_EMAIL`, `TEST_ADMIN_PASSWORD`. Các fixture chỉ nằm trong database test.

Trong môi trường triển khai này không có browser session khả dụng cho kiểm tra trực quan. Responsive CSS, logic UI, build và các test tương tác được kiểm tra; bố cục thực tế cần mở trên trình duyệt của bạn. Profile Testcontainers đã chuẩn bị nhưng chưa chạy vì máy không có Docker; kiểm tra database được chạy trực tiếp trên MySQL 9.5 riêng của dự án.

Chưa có `.git`/branch `dev` trong workspace, nên chưa tạo commit hay PR. Khi đưa lên Git, làm trên branch `phase/1-fashion-store` từ `dev` theo AGENTS. Không đưa `.local`, `.m2`, `.env`, `node_modules` hoặc `target` lên Git.



### Thanh toán QR giả lập

- Tại bước đặt hàng, chọn **QR giả lập**. Mở đơn hàng, nhấn **Tạo mã QR thanh toán**.
- Quét QR hoặc nhấn **Mở trang thanh toán demo**, sau đó chọn **Giả lập thành công** hoặc **Giả lập thất bại**. Trang đơn hàng tự kiểm tra kết quả mỗi 3 giây khi đang chờ.
- Mã có hạn 15 phút. Nếu thất bại hoặc hết hạn, tạo mã mới từ đơn hàng; mã cũ không còn sử dụng được. Đơn đã hủy không thể thanh toán. Số tiền luôn lấy từ cơ sở dữ liệu.
- Đây là thanh toán demo, **không chuyển tiền thật**, không phải mã chuyển khoản ngân hàng VietQR. COD, VietQR và ví điện tử hiện có vẫn hoạt động như trước.

Để quét bằng điện thoại trong cùng Wi-Fi: dừng tiến trình frontend đang chạy, chạy `powershell -ExecutionPolicy Bypass -File .\project-data\scripts\Start-Frontend.ps1 -Lan`, mở địa chỉ Network mà Vite in ra (ví dụ `http://192.168.1.10:5173`) trên máy tính rồi tạo QR. Điện thoại phải truy cập được cùng địa chỉ này; nếu Windows chặn, cho phép Node.js trên mạng riêng. Backend và MySQL vẫn chỉ cần chạy local; frontend proxy `/api` sang backend. Không cần mở cổng router. Nếu chỉ chạy mặc định `127.0.0.1`, dùng nút mở trang demo trên cùng máy tính.


Kích cỡ biến thể trong quản trị có thể để trống; backend tự lưu thành `Freesize` để hiển thị thống nhất trong catalog, giỏ hàng và đơn hàng. Kích cỡ có nhập vẫn giới hạn 10 ký tự.

Đánh giá chỉ dành cho chính tài khoản đã mua sản phẩm trong đơn ở trạng thái **Đã giao hàng**. Đơn chờ xác nhận/đang giao/đã hủy không cấp quyền đánh giá. Giao diện chỉ hiện form khi đủ điều kiện; mỗi tài khoản đánh giá một lần cho mỗi sản phẩm. Backend kiểm tra lại quyền khi gửi, không dựa vào trạng thái từ trình duyệt.
