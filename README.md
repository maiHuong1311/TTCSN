# FORME — Website bán hàng thời trang

Ứng dụng thương mại điện tử chạy local, gồm giao diện khách hàng và trang quản trị riêng.

## Công nghệ

- Backend: Java 17, Spring Boot, Spring Security/JWT, Spring Data JPA và Flyway.
- Frontend: React, TypeScript và Vite.
- Dữ liệu: MySQL; Redis phục vụ OTP khôi phục mật khẩu.
- Email: Brevo; thanh toán QR giả lập; adapter tích hợp API AI.

## Cấu trúc

- `backend/`: mã nguồn API, migration và kiểm thử.
- `frontend/`: giao diện React và ảnh sản phẩm mẫu.
- `project-data/`: cấu hình mẫu, script và hướng dẫn.

## Chạy dự án

Xem [Hướng dẫn chạy dự án](project-data/HUONG_DAN_CHAY_DU_AN.md) để cài đặt MySQL, Redis, chạy backend và frontend. Cấu hình mẫu nằm tại [project-data/.env.example](project-data/.env.example); hướng dẫn gửi email tại [EMAIL_SETUP.md](project-data/EMAIL_SETUP.md).

Swagger khi backend hoạt động: <http://localhost:8080/swagger-ui.html>.

Kho mã không chứa cấu hình bí mật, dữ liệu cá nhân local, cache hoặc bản build. Ảnh sản phẩm do người dùng tải lên cần được sao lưu riêng cùng dữ liệu MySQL.
