# Hướng dẫn chạy dự án FORME trên máy mới

## Chọn ảnh sản phẩm từ máy

Trong **Quản trị → Sản phẩm → Thêm sản phẩm/Chỉnh sửa**, bấm **Chọn tệp** ở mục ảnh sản phẩm hoặc ảnh biến thể. Hỗ trợ JPG/PNG, tối đa 5 MB mỗi ảnh và 20 ảnh sản phẩm. Ảnh đầu tiên là ảnh đại diện. Ảnh biến thể được dùng chung cho các kích cỡ cùng màu; chọn màu trên trang mua hàng sẽ hiển thị ảnh tương ứng. Chờ tải ảnh xong rồi bấm **Lưu sản phẩm**.

Ảnh tải lên được lưu tại `backend/uploads/product-images/` khi chạy backend từ thư mục `backend`. Cần giữ và sao lưu thư mục này cùng MySQL; khởi động lại backend không làm mất ảnh. Khi gửi dự án có sản phẩm đã tải ảnh cho máy khác, gửi kèm thư mục này và dữ liệu MySQL tương ứng. Có thể đổi nơi lưu bằng biến `PRODUCT_IMAGES_DIRECTORY` (nên dùng đường dẫn tuyệt đối). Ảnh cũ có sẵn vẫn hiển thị bình thường.

Dự án chạy local bằng **Java Spring Boot, React.js, MySQL và Redis**. Hướng dẫn này dành cho Windows và PowerShell.

## 1. Chuẩn bị phần mềm

| Phần mềm | Yêu cầu |
|---|---|
| Java JDK | Phiên bản 17 trở lên |
| Node.js | Phiên bản 20.19 trở lên, có npm |
| MySQL Server | Phiên bản 8.0 trở lên; chỉ cài Workbench thì chưa đủ |

Không cần cài Maven riêng vì dự án có Maven Wrapper. Script Redis tự tải bản portable trong lần chạy đầu. Cần Internet để tải Redis và thư viện Java/React.

Sau khi cài, mở PowerShell mới và kiểm tra:

```powershell
java -version
node -v
npm -v
```

Nếu không nhận diện lệnh, kiểm tra PATH/JAVA_HOME và mở lại terminal.

## 2. Giải nén và mở đúng thư mục

Ví dụ giải nén tại `C:\TTCSN`, giữ nguyên cấu trúc:

```text
TTCSN/
├── backend/                  # Spring Boot, Maven Wrapper và migration SQL
├── frontend/                 # React và ảnh sản phẩm
└── project-data/
    ├── HUONG_DAN_CHAY_DU_AN.md
    ├── README.md
    ├── EMAIL_SETUP.md
    ├── .env.example
    ├── config/
    ├── scripts/
    └── tools/
```

**Mỗi terminal chạy lệnh bên dưới phải đứng tại thư mục gốc**, nơi có cả ba thư mục `backend`, `frontend`, `project-data`:

```powershell
cd C:\TTCSN
```

Nếu giải nén ở chỗ khác, thay đường dẫn trên bằng đường dẫn thực tế. Không cần đổi tên thư mục hoặc sửa đường dẫn trong source.

Nếu gặp lỗi PowerShell không cho chạy script, chạy lệnh sau trong terminal đó rồi thử lại:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

Lệnh chỉ áp dụng cho terminal hiện tại.

## 3. Khởi động MySQL và Redis

Trong terminal thứ nhất:

```powershell
cd C:\TTCSN
.\project-data\scripts\Start-MySql.ps1
.\project-data\scripts\Start-Redis.ps1
```

Chờ thông báo MySQL đã sẵn sàng và Redis đã khởi động hoặc đang chạy.

- MySQL riêng của dự án: `127.0.0.1:3307`.
- Database ứng dụng: `fashion_store`.
- Database kiểm thử: `fashion_store_test`.
- Tài khoản database mẫu: `fashion` / `fashion`.
- Redis local: `127.0.0.1:6379`, không mật khẩu, không TLS.

Script MySQL dùng MySQL Server đã cài trên máy, tạo instance và thư mục dữ liệu riêng; không cần dừng dịch vụ MySQL ở cổng 3306. Script không thay đổi cấu hình của dịch vụ đó.

Dữ liệu MySQL/Redis, cấu hình mail riêng và log được lưu trong `project-data/.local/`. Giữ thư mục này nếu muốn giữ dữ liệu sau khi tắt và chạy lại dự án.

## 4. Chạy backend

Mở terminal thứ hai:

```powershell
cd C:\TTCSN
.\project-data\scripts\Start-Backend.ps1
```

Lần đầu Maven tải thư viện nên có thể mất vài phút. Chờ dòng thông báo `Started FashionStoreApplication`.

Backend: **http://127.0.0.1:8080**.

Kiểm tra bằng trình duyệt: **http://127.0.0.1:8080/api/health**. Khi hoạt động, kết quả có `"status":"UP"`.

Flyway tự tạo bảng và nạp **24 sản phẩm mẫu** trên database mới. Không cần nhập file SQL thủ công và không sửa migration đã áp dụng.

Giữ terminal backend đang chạy.

## 5. Chạy frontend

Mở terminal thứ ba:

```powershell
cd C:\TTCSN
.\project-data\scripts\Start-Frontend.ps1
```

Script tự chạy `npm ci` nếu chưa có `frontend/node_modules`. Khi Vite thông báo đã sẵn sàng, mở:

**http://127.0.0.1:5173**

Giữ terminal frontend đang chạy. Frontend chuyển tiếp các yêu cầu `/api` tới backend local.

## 6. Đăng nhập và sử dụng

Tài khoản quản trị mẫu được tạo khi khởi động backend nếu email chưa tồn tại:

```text
Email: admin@forme.local
Mật khẩu: FormeLocalAdmin2026!
```

Admin đăng nhập vào khu vực quản trị riêng để quản lý sản phẩm, biến thể, danh mục, đơn hàng, mã giảm giá và khách hàng.

Muốn dùng giao diện mua sắm bằng tài khoản khách hàng, cấu hình gửi email ở bước tiếp theo rồi đăng ký, nhận và nhập mã xác minh.

Để tạo admin với thông tin khác:

```powershell
.\project-data\scripts\Start-Backend.ps1 -AdminEmail 'admin@example.com' -AdminPassword 'MatKhauLocalMoi2026!'
```

Tham số này chỉ tạo tài khoản mới nếu email chưa tồn tại, không đổi mật khẩu tài khoản cũ.

## 7. Cấu hình Brevo để đăng ký và quên mật khẩu

Nếu chưa cấu hình Brevo, admin mẫu vẫn đăng nhập được; chức năng cần gửi mã email sẽ chưa sử dụng được.

Chuẩn bị API key Brevo và địa chỉ người gửi đã được xác minh. Từ thư mục gốc, tạo/mở file cấu hình riêng:

```powershell
New-Item -ItemType Directory -Force -Path .\project-data\.local | Out-Null
notepad .\project-data\.local\mail.properties
```

Trên máy mới chưa có file này, lưu nội dung sau và thay hai giá trị mẫu bằng cấu hình của bạn:

```properties
application.mail.brevo-api-key=API_KEY_CUA_BAN
application.mail.brevo-sender-email=EMAIL_GUI_DA_XAC_MINH
```

Nếu file đã tồn tại, chỉnh các mục tương ứng và giữ các cấu hình riêng khác. Dừng backend bằng `Ctrl+C`, sau đó chạy lại script backend để nhận cấu hình.

- Đăng ký: nhập thông tin → gửi mã → nhập mã trong email → tạo tài khoản.
- Quên mật khẩu: nhập email đã đăng ký → xác minh OTP → đặt mật khẩu mới. Redis phải đang chạy.

**Hiện script không tự đọc `backend/.env`, `project-data/.env` hoặc `.env.example`.** File `project-data/.env.example` chỉ dùng tham khảo. Muốn cấu hình, dùng file `mail.properties` như trên hoặc đặt biến môi trường trong terminal chạy backend. Xem thêm [EMAIL_SETUP.md](EMAIL_SETUP.md).

## 8. Ảnh, thanh toán và AI

- Ảnh sản phẩm nằm trong `frontend/public/assets/photos/`. Đường dẫn nhập trong quản trị có dạng `/assets/photos/ten-anh.jpg`.
- Thanh toán **QR giả lập** không chuyển tiền thật: đặt hàng → tạo QR trong đơn → mở trang demo → chọn thành công/thất bại.
- Quét QR bằng điện thoại: điện thoại và máy tính cùng Wi-Fi; dừng frontend rồi chạy `Start-Frontend.ps1 -Lan`. Mở website bằng địa chỉ IP mạng nội bộ của máy tính rồi tạo QR. Xem chi tiết trong [README.md](README.md).
- Trợ lý AI cần kết nối API riêng để tư vấn bằng mô hình AI. Chưa cấu hình API thì FAQ và tra cứu đơn của chính tài khoản vẫn hoạt động.
- Chỉ tài khoản đã mua đúng sản phẩm trong đơn **Đã giao hàng** mới được đánh giá sản phẩm.

## 9. Tắt và chạy lại dự án

Backend/frontend: nhấn `Ctrl+C` trong terminal tương ứng.

Để dừng MySQL của dự án, nếu `mysqladmin` có trong PATH:

```powershell
mysqladmin --host=127.0.0.1 --port=3307 --user=root -p shutdown
```

Nhập mật khẩu root mẫu `local-root-change-me-2026` nếu chưa đổi bằng tham số `-RootPassword` khi tạo instance. Nếu không nhận diện lệnh, gọi `mysqladmin.exe` trong thư mục `bin` của MySQL Server đã cài.

Để dừng Redis portable của dự án từ thư mục gốc:

```powershell
$redisCli = Get-ChildItem .\project-data\.local\redis-8.10.2 -Filter redis-cli.exe -Recurse | Select-Object -First 1
& $redisCli.FullName -h 127.0.0.1 -p 6379 shutdown save
```

Lần sau chạy lại các bước 3–5. Các script MySQL/Redis nhận diện dịch vụ đã chạy; không xóa dữ liệu để chạy lại.

## 10. Lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Không tìm thấy script | Kiểm tra terminal đang ở thư mục gốc có ba thư mục chính, không phải bên trong `backend` hoặc `project-data`. |
| `java`, `node` hoặc `npm` không được nhận diện | Cài phần mềm cần thiết, kiểm tra PATH/JAVA_HOME rồi mở terminal mới. |
| Script bị chặn | Dùng `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` trong terminal đang chạy. |
| Không tìm thấy MySQL Server | Cài MySQL Server, hoặc thêm thư mục `bin` của MySQL Server vào PATH; Workbench không thay thế Server. |
| Backend không kết nối được database | Chạy script MySQL trước, kiểm tra cổng 3307 và các biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` đang dùng. |
| Cổng 3307, 6379, 8080 hoặc 5173 đang bận | Kiểm tra có bản dự án khác đang chạy; dừng đúng tiến trình đó rồi thử lại. |
| Frontend không gọi được API | Mở `/api/health` ở cổng 8080; đảm bảo backend vẫn chạy, sau đó tải lại trang frontend. |
| Không gửi được mã email | Kiểm tra Brevo API key, người gửi đã xác minh và file `mail.properties`; khởi động lại backend. |
| Quên mật khẩu báo dịch vụ tạm gián đoạn | Chạy script Redis rồi thử lại; kiểm tra kết nối Redis local. |
| Ảnh không hiện | Kiểm tra file trong `frontend/public/assets/photos`, dùng đúng tên/đuôi file và đường dẫn `/assets/photos/...`. |

Nếu dùng MySQL ở cổng khác hoặc database riêng, xem mục **Dùng MySQL của bạn** trong [README.md](README.md).

## 11. Khi gửi dự án cho người khác

Gửi bản mã nguồn mới nhất với đủ ba thư mục. Không cần gửi `node_modules`, `target`, `dist`, cache Maven hoặc bản Redis portable; các thư viện được tải lại khi chạy.

Không đưa khóa API, file `.env` riêng hoặc `project-data/.local/mail.properties` vào gói mã nguồn chia sẻ.

Database trên máy mới chỉ có dữ liệu mẫu do migration tạo. Sản phẩm thêm thủ công, tài khoản và đơn hàng trên máy người gửi **không tự chuyển sang**; muốn giữ chúng cần xuất/nhập MySQL riêng. Không chép thư mục dữ liệu MySQL đang chạy sang máy khác.

## 12. Kiểm tra mã nguồn (không bắt buộc để mở web)

Khi MySQL và Redis đã chạy, mở terminal tại thư mục gốc:

```powershell
$env:RUN_REDIS_INTEGRATION = 'true'
.\project-data\scripts\Verify.ps1 -MySql
```

Script chạy kiểm thử backend bằng database riêng `fashion_store_test`, rồi kiểm tra lint, build và test frontend. Không dùng database ứng dụng cho các ca kiểm thử.
