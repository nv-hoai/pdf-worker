package edu.dut.distributed.protocol;

/**
 * Message types - MUST match server's MessageType enum
 */
public enum MessageType {
    // Worker -> Master
    WORKER_REGISTER,
    WORKER_HEARTBEAT,
    JOB_RESULT_SUCCESS,
    JOB_RESULT_FAILED,
    
    // Master -> Worker
    WORKER_REGISTERED,
    JOB_ASSIGN,
    WORKER_SHUTDOWN,
    HEARTBEAT_ACK
}
