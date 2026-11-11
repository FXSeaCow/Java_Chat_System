# JC-5 – Cấu trúc project Maven multi-module

Subtask: JC-6 (common), JC-7 (server), JC-8 (user-client), JC-9 (admin-client),
JC-10 (config.properties mẫu).

> Log này được viết bổ sung ngày 11/11/2026, dựa trên code hiện có, khi commit
> phần tái cấu trúc. Code được tạo trong các phiên làm việc với AI trước đó.

## Yêu cầu

Chuyển khung thư mục ban đầu (`shared/`, `backend/`, `frontend/user-app`,
`frontend/admin-app`) thành một project Maven nhiều module. Module nào cũng build
được từ thư mục gốc.

## Kế hoạch của AI (dựng lại từ kết quả)

1. Tạo `pom.xml` gốc dạng `pom`, khai báo 4 module, dùng Java 21 và mã hoá UTF-8.
2. Đổi `shared/` thành module `common`. Giữ nguyên package `com.javachat.shared`
   để không phải sửa các import đã có.
3. Đổi `backend/` thành `server`, `frontend/user-app` thành `user-client`,
   `frontend/admin-app` thành `admin-client`. Module `server` và `user-client`
   phụ thuộc vào `common`.
4. Thêm `config.properties.example` (không chứa mật khẩu hay key) và bỏ qua
   `config.properties` thật trong `.gitignore`.
5. Cập nhật tài liệu protocol, đổi tên module `shared` thành `common`.

## AI đã làm gì

- Tạo `pom.xml` gốc và `pom.xml` cho 4 module.
- Chuyển toàn bộ DTO, model, protocol và smoke test từ `shared/` sang `common/`.
  Nội dung không đổi, chỉ đổi vị trí.
- Chuyển các thư mục giữ chỗ (`.gitkeep`) của backend và frontend sang module mới.
- Thêm `config.properties.example` gồm các khoá `server.*`, `database.*`, `ai.*`.
- Thêm `.gitignore` cho `target/`, `out/` và `config.properties`.
- Viết README giới thiệu cấu trúc module.
