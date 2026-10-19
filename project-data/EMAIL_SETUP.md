# Gửi email bằng Brevo API

Ứng dụng hỗ trợ Brevo HTTP API cho mã đăng ký và mã OTP quên mật khẩu. Khi có đủ API key và email người gửi, backend ưu tiên API; không cần SMTP login hoặc SMTP key.

Cấu hình cá nhân nằm trong `project-data/.local/mail.properties` (đã được gitignore). Các giá trị Brevo được cung cấp đã được lưu vào file local này. Không cần chép đè file bằng mẫu SMTP.

```properties
application.mail.brevo-api-key=API_KEY_CUA_BAN
application.mail.brevo-sender-email=EMAIL_GUI_DA_XAC_MINH
```

Cũng có thể dùng biến môi trường `BREVO_API_KEY`, `BREVO_SENDER_EMAIL` khi không dùng file local. Backend gọi `https://api.brevo.com/v3/smtp/email` qua HTTPS, đặt timeout kết nối/đọc 5 giây. Khóa không được gửi xuống React hoặc trả trong lỗi API.

Khởi động lại `project-data/scripts/Start-Backend.ps1` sau khi đổi cấu hình. Đăng ký: nhập email → **Gửi mã xác minh email** → mở hộp thư → nhập mã → tạo tài khoản.

**Quên mật khẩu dùng OTP, không dùng liên kết trong email.** Chọn **Quên mật khẩu?** → nhập email đã đăng ký → nhận mã 6 số qua email → nhập và xác minh OTP → nhập/xác nhận mật khẩu mới. OTP có hiệu lực 10 phút, chỉ dùng một lần và bị hủy sau 5 lần nhập sai. Xác minh đúng mở phiên đổi mật khẩu trong 5 phút. Sau khi đổi mật khẩu, mọi phiên đăng nhập trước đó bị vô hiệu hóa. Giới hạn gửi: mỗi 60 giây, tối đa 10 lần/24 giờ/email; yêu cầu mới vô hiệu mã và phiên xác minh cũ. Thông báo chung không tiết lộ email có tài khoản hay không.

Redis local lưu trạng thái OTP, số lần thử, hạn gửi lại và phiên đổi mật khẩu bằng TTL; không lưu OTP nguyên văn. Cấu hình theo lựa chọn mới nhất được lưu cùng file private `project-data/.local/mail.properties`:

```properties
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
spring.data.redis.username=
spring.data.redis.password=
spring.data.redis.ssl.enabled=false
```

Chạy `.\project-data\scripts\Start-Redis.ps1` trước backend. Script khởi động Redis chỉ tại `127.0.0.1:6379`, không cài dịch vụ Windows. Dữ liệu/log ở `project-data/.local/redis-data`, `project-data/.local/redis-local.log`. Lần đầu tải bản Windows portable 8.10.2 từ [redis-windows](https://github.com/redis-windows/redis-windows), kiểm tra SHA-256 đã ghim rồi chạy ẩn. Đây là bản dựng cộng đồng dành cho local. Lần sau dùng lại bản đã tải, hoặc dùng Redis đang chạy nếu PING thành công.

Nếu không dùng file local, có thể đặt `REDIS_HOST`, `REDIS_PORT`, `REDIS_USERNAME`, `REDIS_PASSWORD`, `REDIS_SSL_ENABLED`. Biến `REDIS_SSL` cũng được hỗ trợ để tương thích cấu hình cũ. Nếu chuyển lại Upstash, cần hostname đang hoạt động và bật TLS. Khi Redis không kết nối được, luồng quên mật khẩu báo dịch vụ tạm gián đoạn; không bỏ qua xác minh hoặc lưu OTP tạm trong RAM. Đăng ký mới vẫn dùng cơ chế xác minh email riêng trong MySQL.

Kiểm thử Redis thật (chỉ tạo/xóa key thử nghiệm có prefix riêng, không gửi email): đặt `RUN_REDIS_INTEGRATION=true` trước khi chạy bộ kiểm thử Maven. Cấu hình Redis local là mặc định; database kiểm thử vẫn dùng `fashion_store_test` theo `project-data/scripts/Verify.ps1 -MySql`.

Các liên kết đổi mật khẩu cũ không còn được chấp nhận. Bảng `password_reset` từ migration V8 giữ lịch sử schema, không còn được dùng cho việc đặt lại mật khẩu. Không sửa migration đã áp dụng.

Đăng ký mới chỉ hoàn tất sau khi nhập đúng mã 6 số gửi đến email. Tài khoản cũ được giữ nguyên. Chưa cấu hình email hoặc gửi mail thất bại thì không tạo tài khoản. Không có mã mặc định và không in mã ra log.

Nếu muốn chuyển sang SMTP, xóa cấu hình API key/email API rồi điền ba giá trị SMTP trong file local:

```properties
spring.mail.username=SMTP_LOGIN_CUA_BAN
spring.mail.password=SMTP_KEY_CUA_BAN
application.mail.from=EMAIL_GUI_DA_XAC_MINH
```

Giữ host `smtp-relay.brevo.com`, port `587`. Ứng dụng bật xác thực và bắt buộc STARTTLS, đặt timeout 5 giây. File `project-data/.local/mail.properties` nằm trong thư mục đã được gitignore. Không đưa khóa vào source hoặc frontend. Nếu giá trị chứa dấu gạch chéo ngược, escape theo định dạng Java properties (`\\`).

Cũng có thể dùng biến môi trường `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` trước khi chạy backend, khi không dùng file local trên.

Mã hết hạn sau 10 phút, chỉ dùng một lần. Gửi lại sau 60 giây, tối đa 10 mã/24 giờ/email. Nhập sai 5 lần thì cần gửi lại mã. Mã mới vô hiệu mã cũ. `EMAIL_BLOCKED_DOMAINS` là danh sách miền email tạm thời phân cách bằng dấu phẩy, gồm một số miền phổ biến mặc định. Xác minh chứng minh quyền truy cập hộp thư; không thể bảo đảm chặn mọi dịch vụ email tạm thời chỉ bằng danh sách miền.

Tài liệu chính thức: [Brevo Transactional Email API](https://developers.brevo.com/reference/send-transac-email), [Brevo SMTP](https://help.brevo.com/hc/en-us/articles/7924908994450-Send-transactional-emails-using-Brevo-SMTP), [SMTP keys](https://help.brevo.com/hc/en-us/articles/7959631848850-Create-and-manage-your-SMTP-keys).

Kết nối Redis: [Upstash Redis clients và TLS](https://upstash.com/docs/redis/howto/connect-client).
