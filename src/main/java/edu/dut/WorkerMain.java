package edu.dut;

import edu.dut.distributed.protocol.MessageType;
import edu.dut.distributed.protocol.WorkerMessage;
import edu.dut.processor.PDFConverter;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Worker client that connects to Master server
 */
public class WorkerMain {
    
    private String serverHost;
    private int serverPort;
    private String workerId;
    
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private BlockingQueue<WorkerMessage> sendQueue;
    
    private PDFConverter converter;
    private volatile boolean running = false;
    
    public WorkerMain(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
        this.sendQueue = new LinkedBlockingQueue<>();
        this.converter = new PDFConverter();
    }
    
    public void start() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔧 PDF Conversion Worker Starting...");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Worker ID: " + workerId);
        System.out.println("Server: " + serverHost + ":" + serverPort);
        System.out.println("Mode: TCP File Transfer (No shared storage needed)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            // Connect to server
            System.out.println("Connecting to master server...");
            socket = new Socket(serverHost, serverPort);
            
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            
            System.out.println("✓ Connected to master server");
            
            running = true;
            
            // Start sender thread
            Thread senderThread = new Thread(this::messageSender);
            senderThread.setDaemon(true);
            senderThread.start();
            
            // Start heartbeat thread
            Thread heartbeatThread = new Thread(this::heartbeat);
            heartbeatThread.setDaemon(true);
            heartbeatThread.start();
            
            // Register with server
            register();
            
            // Main message loop
            while (running) {
                try {
                    WorkerMessage message = (WorkerMessage) in.readObject();
                    handleMessage(message);
                } catch (EOFException | ClassNotFoundException e) {
                    System.err.println("Connection lost: " + e.getMessage());
                    break;
                }
            }
            
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    private void register() {
        try {
            WorkerMessage msg = new WorkerMessage(MessageType.WORKER_REGISTER, workerId);
            msg.put("hostname", InetAddress.getLocalHost().getHostName());
            msg.put("cores", Runtime.getRuntime().availableProcessors());
            msg.put("memoryMB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
            
            sendMessage(msg);
            System.out.println("→ Registration request sent");
            
        } catch (Exception e) {
            System.err.println("Failed to register: " + e.getMessage());
        }
    }
    
    private void heartbeat() {
        while (running) {
            try {
                Thread.sleep(10000); // Every 10 seconds
                
                WorkerMessage msg = new WorkerMessage(MessageType.WORKER_HEARTBEAT, workerId);
                sendMessage(msg);
                
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void handleMessage(WorkerMessage message) {
        System.out.println("← Received: " + message.getType());
        
        switch (message.getType()) {
            case WORKER_REGISTERED:
                System.out.println("✓ Successfully registered with master");
                System.out.println("✓ Ready to receive jobs");
                break;
                
            case JOB_ASSIGN:
                handleJobAssignment(message);
                break;
                
            case WORKER_SHUTDOWN:
                System.out.println("⚠ Shutdown request from master");
                running = false;
                break;
                
            case HEARTBEAT_ACK:
                // Silent ACK
                break;
                
            default:
                System.err.println("Unknown message type: " + message.getType());
        }
    }
    
    private void handleJobAssignment(WorkerMessage message) {
        Integer requestId = message.getInt("requestId");
        String savedFilename = message.getString("savedFilename");
        String originalFilename = message.getString("originalFilename");
        byte[] fileData = message.getFileData();
        long fileSize = message.getFileSize();
        
        if (requestId == null || savedFilename == null || fileData == null) {
            System.err.println("Invalid job assignment - missing data");
            return;
        }
        
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📄 Processing Job #" + requestId);
        System.out.println("   File: " + originalFilename + " (" + (fileSize / 1024) + " KB)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Process in new thread
        new Thread(() -> processJob(requestId, savedFilename, originalFilename, fileData)).start();
    }
    
    private void processJob(int requestId, String savedFilename, String originalFilename, byte[] wordFileData) {
        long startTime = System.currentTimeMillis();
        
        File tempInputFile = null;
        File tempOutputFile = null;
        
        try {
            // Create temp files
            tempInputFile = File.createTempFile("word_" + requestId + "_", ".docx");
            tempOutputFile = File.createTempFile("pdf_" + requestId + "_", ".pdf");
            
            // Write received Word file data to temp file
            java.nio.file.Files.write(tempInputFile.toPath(), wordFileData);
            System.out.println("✓ Saved temp Word file: " + tempInputFile.getName());
            
            // Convert to PDF
            converter.convertToPDF(tempInputFile, tempOutputFile);
            System.out.println("✓ Conversion completed");
            
            // Read PDF file as byte array
            byte[] pdfFileData = java.nio.file.Files.readAllBytes(tempOutputFile.toPath());
            
            String pdfFilename = savedFilename.substring(0, savedFilename.lastIndexOf('.')) + ".pdf";
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Send success result with PDF data
            WorkerMessage result = new WorkerMessage(MessageType.JOB_RESULT_SUCCESS, workerId);
            result.put("requestId", requestId);
            result.put("pdfFilename", pdfFilename);
            result.put("success", true);
            result.setFileData(pdfFileData, pdfFileData.length);
            
            sendMessage(result);
            
            System.out.println("✓ Job #" + requestId + " completed in " + duration + "ms");
            System.out.println("✓ PDF size: " + (pdfFileData.length / 1024) + " KB");
            
        } catch (Exception e) {
            System.err.println("✗ Job #" + requestId + " failed: " + e.getMessage());
            
            // Send failure result
            WorkerMessage result = new WorkerMessage(MessageType.JOB_RESULT_FAILED, workerId);
            result.put("requestId", requestId);
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
            
            sendMessage(result);
            
        } finally {
            // Clean up temp files
            if (tempInputFile != null && tempInputFile.exists()) {
                tempInputFile.delete();
                System.out.println("✓ Deleted temp input file");
            }
            if (tempOutputFile != null && tempOutputFile.exists()) {
                tempOutputFile.delete();
                System.out.println("✓ Deleted temp output file");
            }
        }
    }
    
    private void sendMessage(WorkerMessage message) {
        try {
            sendQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void messageSender() {
        while (running) {
            try {
                WorkerMessage message = sendQueue.take();
                out.writeObject(message);
                out.flush();
            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                System.err.println("Failed to send message: " + e.getMessage());
                running = false;
                break;
            }
        }
    }
    
    private void cleanup() {
        System.out.println("Shutting down worker...");
        
        running = false;
        
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // Ignore
        }
        
        System.out.println("Worker stopped");
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java -jar pdf-worker.jar <server-host> <server-port>");
            System.err.println("Example: java -jar pdf-worker.jar 192.168.1.100 7777");
            System.err.println("         java -jar pdf-worker.jar localhost 7777");
            System.err.println("");
            System.err.println("Note: Files are transferred via TCP - no shared storage needed!");
            System.exit(1);
        }
        
        String serverHost = args[0];
        int serverPort = Integer.parseInt(args[1]);
        
        WorkerMain worker = new WorkerMain(serverHost, serverPort);
        worker.start();
    }
}
