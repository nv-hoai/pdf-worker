package edu.dut.distributed.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Protocol message - MUST match server's WorkerMessage class
 */
public class WorkerMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private MessageType type;
    private String workerId;
    private long timestamp;
    private Map<String, Object> data;
    
    // File transfer fields
    private byte[] fileData;
    private long fileSize;
    
    public WorkerMessage() {
        this.timestamp = System.currentTimeMillis();
        this.data = new HashMap<>();
    }
    
    public WorkerMessage(MessageType type, String workerId) {
        this();
        this.type = type;
        this.workerId = workerId;
    }
    
    public void put(String key, Object value) {
        data.put(key, value);
    }
    
    public Object get(String key) {
        return data.get(key);
    }
    
    public Integer getInt(String key) {
        Object value = data.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return null;
    }
    
    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
    
    // File transfer methods
    public void setFileData(byte[] fileData, long fileSize) {
        this.fileData = fileData;
        this.fileSize = fileSize;
    }
    
    public byte[] getFileData() {
        return fileData;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    // Getters and Setters
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public String getWorkerId() {
        return workerId;
    }
    
    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    @Override
    public String toString() {
        return "WorkerMessage{type=" + type + ", workerId='" + workerId + '\'' + 
               ", data=" + data + '}';
    }
}
