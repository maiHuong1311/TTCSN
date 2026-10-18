# Ảnh chụp và tham khảo thiết kế

Giao diện FORME được thiết kế lại theo bố cục cửa hàng [Cardina](https://cardina.vn/): nền trắng, điểm nhấn đỏ, thanh tìm kiếm, menu danh mục, banner lớn, ô ưu đãi và lưới sản phẩm. Thương hiệu hiển thị của dự án vẫn là FORME.

Ảnh được tải về ngày 18/10/2026 theo yêu cầu của người dùng, lưu tại `frontend/public/assets/photos/`. Website chạy local sử dụng bản ảnh trong dự án, không cần gọi CDN bên ngoài khi xem sản phẩm.

Cập nhật ảnh theo màu: `colors.json` trong thư mục ảnh là danh sách hiện tại của **8 mẫu / 19 màu**, kèm URL nguồn ảnh và trang sản phẩm gốc. Các mẫu áo, đầm, quần có ảnh màu từ Cardina; balo cuộn miệng 20L có ảnh navy/đen/xanh rêu từ [Stubble & Co](https://www.stubbleandco.com/products/the-roll-top-20l). Một số mẫu một màu được thay bằng mẫu cùng loại có nhiều màu thật. Ảnh màu không bị tô lại hay áp bộ lọc đổi màu. File `sources.json` giữ nguồn ảnh của lần triển khai đầu tiên.

Migration V9 thêm `image_url` cho biến thể. V10 bổ sung các lựa chọn màu vào sản phẩm mẫu còn đúng tên/ảnh mặc định; giữ mã biến thể và tồn kho cũ, thêm tồn kho demo 15 cho mỗi biến thể mới. Admin có thể sửa ảnh từng biến thể trong trang Quản trị. Danh sách, chi tiết và giỏ hàng dùng ảnh của biến thể được chọn. Giá và tồn kho của website nguồn không được sao chép vào ứng dụng.

| Tệp | Nội dung | Nguồn |
|---|---|---|
| product-1.jpg | Áo kiểu cổ nhún | Cardina |
| product-2.jpg | Quần dạ công sở | Cardina |
| product-3.jpg | Đầm sơ mi kẻ sọc | Cardina |
| product-4.jpg | Áo thun tăm dài tay | Cardina |
| product-5.jpg | Quần jeans ống rộng | Cardina |
| product-6.png, product-6-color-2.png, product-6-color-3.png | Balo cuộn miệng 20L, navy/đen/xanh rêu | Stubble & Co |
| product-7.jpg | Áo nỉ cổ cao | Cardina |
| product-8.jpg | Quần short dạ | Cardina |
| category-set.jpg | Ảnh bộ trang phục nữ | Cardina |
| category-knit.jpg | Ảnh áo dệt nữ | Cardina |

URL gốc của từng ảnh được ghi trong `frontend/public/assets/photos/sources.json`. Hai ảnh `reference-banner.jpg` và `reference-knitwear.jpg` dùng để đối chiếu thiết kế; không hiển thị các chương trình giảm giá hoặc nhận diện thương hiệu Cardina trên giao diện FORME. Banner FORME kết hợp ảnh chụp trang phục với nội dung riêng bằng HTML/CSS.

Migration V6 thay ảnh minh họa mẫu bằng ảnh chụp và điều chỉnh tên/chất liệu/màu mẫu cho phù hợp. Không sửa migration đã chạy và không ghi đè đường dẫn ảnh tùy chỉnh của quản trị viên. Giá, số lượng tồn kho và các luồng đặt hàng giữ nguyên.

Khách chưa đăng nhập xem ảnh lookbook và được mời đăng nhập để xem giá hiện tại. Khi đăng nhập, trang chủ lấy sản phẩm, giá và tồn kho thật qua React Query, tuân thủ yêu cầu JWT cho API catalog trong AGENTS.

## Bổ sung 10 sản phẩm

Ngày 18/10/2026 thêm 10 mẫu (4 áo/áo khoác, 3 quần, 2 đầm và 1 chân váy), mỗi mẫu 2 màu với ảnh chụp riêng, lưu tại `collection-9-color-1.jpg` đến `collection-18-color-2.jpg`. Ảnh lấy từ các trang sản phẩm công khai của Cardina, tải bản grande về local để trang chi tiết hiển thị rõ. Danh sách tên mẫu, màu, URL trang gốc, URL ảnh gốc và URL tải được ghi trong `frontend/public/assets/photos/autumn-collection.json`. Đã đối chiếu ảnh theo image_id của từng màu và xem bảng ảnh tổng hợp.

V12 thêm dữ liệu bằng ID tự tăng để giữ các sản phẩm do admin tạo. Giá, mô tả và tồn kho là dữ liệu mẫu của FORME; mỗi biến thể mới có 15 sản phẩm. Có 78 biến thể mới: đa số S/M/L/XL, riêng đầm gấm M/L/XL theo lựa chọn mẫu. Mỗi sản phẩm có hai ảnh trong thư viện, ảnh chính và ảnh riêng cho từng biến thể màu. Không sửa migration cũ hoặc sản phẩm hiện có.

Bổ sung tiếp 2 mẫu bằng V13: áo cotton cổ thuyền xếp ly eo và chân váy kaki dáng suông xẻ sau, 4 ảnh thật local `collection-19-color-1.jpg` đến `collection-20-color-2.jpg`. Thông tin nguồn ảnh và màu được bổ sung vào `autumn-collection.json`. Hai mẫu có tổng 16 biến thể, giá/tồn kho là dữ liệu demo. Đã xem ảnh để đối chiếu kiểu dáng.

V14 thêm 4 mẫu để lấp đầy lưới hai trang: áo sơ mi nữ phom rộng tay dơi (Tím/Vàng), áo khoác đũi mỏng (Nâu/Trắng gạo), quần short tweed (Họa tiết nâu/Họa tiết rêu), chân váy linen chấm bi phối ren (Navy/Be kaki). 8 ảnh theo màu lưu local tại `collection-21-color-1.jpg` đến `collection-24-color-2.jpg`, nguồn gốc trong `autumn-collection.json`. Ảnh được chọn bằng image_id của từng màu tại Cardina; giá/tồn kho và mô tả là dữ liệu mẫu FORME.
