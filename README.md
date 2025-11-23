# PDF Conversion Worker

Worker node độc lập cho hệ thống chuyển đổi PDF phân tán.

## Tổng quan

Worker kết nối tới Master Server qua TCP, nhận file Word qua mạng, chuyển đổi sang PDF bằng Docx4j + Apache FOP, và gửi kết quả trả về.

**Đặc điểm:**
- Không cần shared storage - file truyền qua TCP
- Plug & Play - chỉ cần kết nối và chạy
- Đa nền tảng - Windows, Linux, macOS
- Tự động kết nối lại khi mất kết nối
- Tự động dọn dẹp file tạm

## Yêu cầu hệ thống

- Java 8 trở lên (JRE hoặc JDK)
- Kết nối mạng tới Master Server port 7777
- Không cần LibreOffice

## Stack công nghệ

- Docx4j 11.4.9 - Xử lý tài liệu Word
- Apache FOP 2.9 - Render PDF
- Java Sockets - TCP communication
- ObjectInputStream/OutputStream - Binary protocol

## Build project

```bash
mvn clean package
```

Output:
- `target/pdf-worker-1.0.0.jar` - JAR thường
- `target/pdf-worker-1.0.0-jar-with-dependencies.jar` - Fat JAR (sử dụng file này)

## Chạy worker

### Cú pháp

```bash
java -jar pdf-worker-1.0.0-jar-with-dependencies.jar <server-ip> <server-port>
```

### Ví dụ

**Local:**
```bash
java -jar pdf-worker-1.0.0-jar-with-dependencies.jar localhost 7777
```

**Remote:**
```bash
java -jar pdf-worker-1.0.0-jar-with-dependencies.jar 192.168.1.100 7777
```

**Script:**
```bash
# Windows
start-worker.bat 192.168.1.100 7777

# Linux/Mac
./start-worker.sh 192.168.1.100 7777
```

## Cách hoạt động

### Kiến trúc truyền file qua TCP

```
Master Server                    Worker
     │                              │
     ├─ User upload file Word       │
     ├─ Đọc file → byte[]           │
     │                              │
     ├─ TCP Send: {                 │
     │    requestId: 123,            │
     │    fileData: [bytes],         │
     │    fileSize: 524288           │
     │  } ─────────────────────────► │
     │                              │
     │                              ├─ Nhận file bytes
     │                              ├─ Lưu temp: word_123.docx
     │                              ├─ Docx4j + FOP → pdf_123.pdf
     │                              ├─ Đọc PDF → byte[]
     │                              ├─ Xóa file temp
     │                              │
     │ ◄─────────────────────────── │ TCP Send: {
     │                              │   requestId: 123,
     │                              │   pdfData: [bytes],
     │                              │   fileSize: 128000
     │                              │ }
     │                              │
     ├─ Nhận PDF bytes              │
     ├─ Lưu vào outputs/            │
     └─ User download PDF           │
```

### Ưu điểm

- Không cần cấu hình NFS/SMB
- Workers có thể ở bất kỳ đâu (khác mạng, cloud)
- Dễ triển khai Docker/Kubernetes
- Tự động dọn dẹp file tạm

## Deployment scenarios

### Single machine (Development)

```bash
# 1. Deploy master server (pdfconverterv9.war) vào Tomcat
# 2. Khởi động worker
java -jar pdf-worker.jar localhost 7777
```

### Multiple workers (Production)

**Server (192.168.1.100):**
- Deploy webapp vào Tomcat
- Master TCP server listen port 7777

**Worker 1:**
```bash
java -jar pdf-worker.jar 192.168.1.100 7777
```

**Worker 2:**
```bash
java -jar pdf-worker.jar 192.168.1.100 7777
```

**Worker 3:**
```bash
java -jar pdf-worker.jar 192.168.1.100 7777
```

## Monitoring

### Console logs

Worker xuất log ra console:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PDF Conversion Worker Starting...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Worker ID: worker-a1b2c3d4
Server: 192.168.1.100:7777
Mode: TCP File Transfer (No shared storage)
Converter: Docx4j + Apache FOP
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ PDF Converter initialized
➤ Connecting to Master at 192.168.1.100:7777...
✓ Connected to master server
→ Sending registration...
✓ Registration successful
✓ Ready to receive jobs
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Processing Job #123
File: report.docx (512 KB)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ Saved temp Word file: word_123_abc.docx
✓ Converted: word_123_abc.docx → pdf_123_xyz.pdf
✓ Conversion completed
✓ Job #123 completed in 3421ms
✓ PDF size: 128 KB
✓ Cleaned up temp files
```

### Web dashboard

Truy cập Master Server:
```
http://server-ip:8080/pdfconverterv9/admin/workers
```

Dashboard hiển thị:
- Worker ID và hostname
- Status (IDLE/BUSY/OFFLINE)
- Jobs completed/failed
- CPU cores và memory
- Last heartbeat
- Real-time updates

## Troubleshooting

### Connection refused

**Error:** `Connection error: Connection refused`

**Solutions:**

1. Kiểm tra master server đang chạy
2. Mở port 7777 trên firewall:
   ```bash
   # Windows
   netsh advfirewall firewall add rule name="PDF Worker" dir=in action=allow protocol=TCP localport=7777
   
   # Linux
   sudo ufw allow 7777/tcp
   ```
3. Xác minh IP và port đúng
4. Kiểm tra Tomcat logs (Master TCP Server đã start chưa)

### Out of memory

**Error:** `OutOfMemoryError` hoặc `Java heap space`

**Solutions:**

```bash
# Tăng heap 2GB
java -Xmx2048m -jar pdf-worker.jar localhost 7777

# File lớn (>10MB) cần 4GB
java -Xmx4096m -jar pdf-worker.jar localhost 7777
```

### Conversion failed

**Error:** `Conversion failed` hoặc PDF bị lỗi layout

**Nguyên nhân:**
- Tài liệu Word phức tạp (Docx4j không hỗ trợ hết)
- Thiếu font (cài Arial, Times New Roman cho tiếng Việt)
- File bị corrupt

**Solutions:**
- Cài đặt font cần thiết
- Kiểm tra file Word mở được không
- Xem worker logs để biết chi tiết

### Worker không hiển thị dashboard

**Triệu chứng:** Worker kết nối nhưng không thấy trên `/admin/workers`

**Solutions:**
1. Kiểm tra worker logs có "Registration successful"
2. Refresh trang (auto-update mỗi 5 giây)
3. Kiểm tra Master server logs
4. Xác minh kết nối mạng

## Security notes

- Worker kết nối TỚI master (không phải ngược lại)
- Không authentication (giả định internal network)
- File transfer qua TCP plaintext (không mã hóa)
- Production: dùng VPN hoặc SSH tunnel
  ```bash
  ssh -L 7777:localhost:7777 user@server-ip
  java -jar pdf-worker.jar localhost 7777
  ```

## Performance

**Xử lý:**
- Mỗi worker: 1 job tại một thời điểm (sequential)
- Tốc độ tùy bandwidth
- Recommended file size: < 50 MB
- Large files (> 100 MB): có thể gây OOM

**Throughput estimate:**
- Local network (1 Gbps): ~2-5s/document
- Remote network (100 Mbps): ~5-15s/document
- Multiple workers: linear scaling

## Configuration

### Increase memory

```bash
java -Xmx4096m -jar pdf-worker.jar server-ip 7777
```

### Background process

**Windows:**
```bash
start /B java -jar pdf-worker.jar server-ip 7777 > worker.log 2>&1
```

**Linux:**
```bash
nohup java -jar pdf-worker.jar server-ip 7777 > worker.log 2>&1 &
```

## Shutdown

- `Ctrl+C` để tắt gracefully
- Worker thông báo master trước khi disconnect
- Job đang chạy sẽ complete trước
- File tạm tự động cleanup
