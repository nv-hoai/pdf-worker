# 🖥️ Worker Chuyển Đổi PDF

Worker độc lập cho hệ thống chuyển đổi PDF phân tán sử dụng **Docx4j + Apache FOP**.

**✨ Tính năng:**
- 🚀 **Không Cần Cấu Hình** - Không cần shared storage
- 📦 **Truyền File qua TCP** - Tất cả file được truyền qua mạng
- 🔧 **Plug & Play** - Chỉ cần kết nối tới master server và chạy
- 💻 **Đa Nền Tảng** - Windows, Linux, macOS
- 📚 **Chất Lượng Cao** - Docx4j + Apache FOP hỗ trợ Unicode tốt
- 🌏 **Hỗ Trợ Tiếng Việt** - Render hoàn hảo font tiếng Việt
- ⚡ **Tự Động Kết Nối Lại** - Tự động kết nối lại nếu mất kết nối
- 🧹 **Tự Động Dọn Dẹp** - File tạm được xóa tự động

## 📋 Yêu Cầu

- **Java 8+** (JRE hoặc JDK)
- **Mạng** kết nối tới Master server port 7777
- **Không cần LibreOffice!** (Sử dụng thư viện Java Docx4j thuần túy)

## 🛠️ Công Nghệ Sử Dụng

- **Docx4j 11.4.9** - Xử lý tài liệu Word
- **Apache FOP 2.9** - Engine render PDF
- **Java Sockets** - Giao tiếp TCP
- **ObjectInputStream/OutputStream** - Giao thức binary

## 🚀 Build Project

```bash
mvn clean package
```

Lệnh này sẽ tạo ra:
- `target/pdf-worker-1.0.0.jar` - JAR thường
- `target/pdf-worker-1.0.0-jar-with-dependencies.jar` - **Fat JAR (dùng file này)**

## 📦 Chạy Worker

### Cách Dùng Cơ Bản

```bash
java -jar target/pdf-worker-1.0.0-jar-with-dependencies.jar <server-ip> <server-port>
```

### Ví Dụ

**Kết nối tới server local:**
```bash
java -jar pdf-worker-1.0.0-jar-with-dependencies.jar localhost 7777
```

**Kết nối tới server từ xa:**
```bash
java -jar pdf-worker-1.0.0-jar-with-dependencies.jar 192.168.1.100 7777
```

**Sử dụng script khởi động:**
```bash
# Windows
start-worker.bat 192.168.1.100 7777

# Linux/Mac
./start-worker.sh 192.168.1.100 7777
```

## 🔧 Cách Hoạt Động

### Kiến Trúc Truyền File qua TCP

```
Master Server                    Worker
     │                              │
     ├─ User upload file Word       │
     │                              │
     ├─ Đọc file → byte[]           │
     │                              │
     ├─ TCP Gửi: {                  │
     │    requestId: 123,            │
     │    fileData: [bytes],         │
     │    fileSize: 524288           │
     │  } ─────────────────────────► │
     │                              │
     │                              ├─ Nhận file bytes
     │                              ├─ Lưu temp: /tmp/word_123.docx
     │                              ├─ Chuyển đổi → /tmp/pdf_123.pdf
     │                              ├─ Đọc PDF → byte[]
     │                              ├─ Xóa file temp
     │                              │
     │ ◄─────────────────────────── │ TCP Gửi: {
     │                              │   requestId: 123,
     │                              │   pdfData: [bytes],
     │                              │   fileSize: 128000
     │                              │ }
     │                              │
     ├─ Nhận PDF bytes              │
     ├─ Lưu vào outputs/            │
     ├─ User tải PDF xuống          │
```

**✅ Lợi Ích:**
- Không cần cấu hình NFS/SMB
- Workers có thể ở bất kỳ đâu (mạng khác nhau, cloud, v.v.)
- Dễ dàng triển khai Docker/Kubernetes
- Tự động dọn dẹp file tạm

## 🖥️ Kịch Bản Triển Khai

### Máy Đơn (Development)

```bash
# Khởi động master server (webapp)
# Deploy pdfconverterv9.war vào Tomcat port 8080

# Khởi động worker
java -jar pdf-worker.jar localhost 7777
```

### Nhiều Workers (Production)

**Máy Server (192.168.1.100):**
```bash
# Deploy webapp vào Tomcat
# Master TCP server sẽ lắng nghe trên port 7777
```

**Máy Worker 1:**
```bash
java -jar pdf-worker.jar 192.168.1.100 7777
```

**Máy Worker 2:**
```bash
java -jar pdf-worker.jar 192.168.1.100 7777
```

**Máy Worker 3:**
```bash
java -jar pdf-worker.jar 192.168.1.100 7777
```

## 🔍 Giám Sát

Worker xuất log ra console:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔧 PDF Conversion Worker Đang Khởi Động...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Worker ID: worker-a1b2c3d4
Server: 192.168.1.100:7777
Chế độ: Truyền File qua TCP (Không cần shared storage)
Converter: Docx4j + Apache FOP
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ PDF Converter khởi tạo bằng Docx4j + Apache FOP
➤ Đang kết nối tới Master tại 192.168.1.100:7777...
✓ Đã kết nối tới master server
→ Đang gửi đăng ký...
✓ Đăng ký thành công với master
✓ Sẵn sàng nhận công việc
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📄 Đang Xử Lý Job #123
   File: report.docx (512 KB)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ Đã lưu file Word tạm: word_123_abc.docx
✓ Đã chuyển đổi: word_123_abc.docx → pdf_123_xyz.pdf
✓ Chuyển đổi hoàn tất
✓ Job #123 hoàn thành trong 3421ms
✓ Kích thước PDF: 128 KB
✓ Đã xóa file input tạm
✓ Đã xóa file output tạm
```

### Dashboard Master Server

Xem tất cả workers và trạng thái tại:
```
http://server-ip:8080/pdfconverterv9/workers.jsp
```

**Dashboard hiển thị:**
- Worker ID và hostname
- Trạng thái (IDLE/BUSY/OFFLINE)
- Số jobs hoàn thành/thất bại
- CPU cores và bộ nhớ
- Thời gian heartbeat cuối
- Cập nhật real-time

## 🐛 Xử Lý Sự Cố

### Lỗi Connection Refused

**Lỗi:** `Connection error: Connection refused`

**Giải pháp:**
1. Kiểm tra master server đang chạy
2. Kiểm tra firewall cho phép port 7777:
   ```bash
   # Windows
   netsh advfirewall firewall add rule name="PDF Worker" dir=in action=allow protocol=TCP localport=7777
   
   # Linux
   sudo ufw allow 7777/tcp
   ```
3. Xác minh địa chỉ IP server đúng
4. Kiểm tra Tomcat logs xem Master TCP Server đã khởi động chưa

### Lỗi Hết Bộ Nhớ

**Lỗi:** `OutOfMemoryError` hoặc `Java heap space`

**Giải pháp:**
```bash
# Tăng heap size lên 2GB
java -Xmx2048m -jar pdf-worker.jar localhost 7777

# Với file lớn (>10MB)
java -Xmx4096m -jar pdf-worker.jar localhost 7777
```

### Lỗi Chuyển Đổi

**Lỗi:** `Conversion failed` hoặc PDF bị lỗi layout

**Nguyên nhân có thể:**
1. **Tài liệu Word phức tạp** - Docx4j có thể không hỗ trợ tất cả tính năng
2. **Thiếu font** - Cài đặt font cần thiết trên máy worker
3. **File bị hỏng** - Thử mở bằng Word để kiểm tra

**Giải pháp:**
- Với tiếng Việt: Cài đặt font Arial, Times New Roman
- Với tài liệu phức tạp: Cân nhắc dùng Aspose.Words (thương mại)
- Xem worker logs để biết chi tiết lỗi

### Worker Không Hiển Thị Trong Dashboard

**Triệu chứng:** Worker đã kết nối nhưng không thấy ở `/workers.jsp`

**Giải pháp:**
1. Kiểm tra worker logs có dòng "✓ Đăng ký thành công"
2. Refresh trang dashboard (tự động cập nhật mỗi 5 giây)
3. Kiểm tra Master server logs xem có thông báo đăng ký không
4. Xác minh kết nối mạng giữa worker và master

## 🔐 Lưu Ý Bảo Mật

- Worker kết nối **TỚI** master, không phải ngược lại
- Không yêu cầu xác thực (giả định mạng nội bộ)
- File được truyền qua TCP (mặc định không mã hóa)
- Với production, dùng VPN hoặc SSH tunnel:
  ```bash
  ssh -L 7777:localhost:7777 user@server-ip
  java -jar pdf-worker.jar localhost 7777
  ```

## 📊 Hiệu Suất

- Mỗi worker xử lý **1 job tại một thời điểm** (tuần tự)
- Tốc độ truyền file phụ thuộc vào băng thông mạng
- Kích thước file đề xuất: **< 50 MB** để hiệu suất tối ưu
- File lớn (> 100 MB) có thể gây vấn đề về bộ nhớ

**Ước Tính Throughput:**
- Mạng local (1 Gbps): ~2-5 giây/tài liệu
- Mạng từ xa (100 Mbps): ~5-15 giây/tài liệu
- Nhiều workers mở rộng tuyến tính

## 🛑 Tắt Worker

- Nhấn `Ctrl+C` để tắt an toàn
- Worker sẽ thông báo master trước khi ngắt kết nối
- Job đang chạy sẽ hoàn thành trước khi tắt
- File tạm tự động được dọn dẹp
