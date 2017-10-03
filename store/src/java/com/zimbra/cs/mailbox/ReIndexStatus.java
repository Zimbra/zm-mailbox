package com.zimbra.cs.mailbox;

/**
 * Re-index progress information. The counters are thread safe.
 * 
 * @author Greg Solovyev
 */
public final class ReIndexStatus {
    private volatile int total = 0;
    private volatile int succeeded = 0;
    private volatile int failed = 0;
    private volatile int status = STATUS_IDLE;

    public static final int STATUS_FAILED = -3;
    public static final int STATUS_QUEUE_FULL = -2;
    public static final int STATUS_ABORTED = -1;
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_DONE = 2;

    public ReIndexStatus() {
        this(0, 0, 0, STATUS_IDLE);
    }

    public ReIndexStatus(int total, int succeeded) {
        this(total, succeeded, 0, STATUS_IDLE);
    }

    public ReIndexStatus(int total, int succeeded, int failed) {
        this(total, succeeded, failed, STATUS_IDLE);
    }

    public ReIndexStatus(int total, int succeeded, int failed, int status) {
        this.total = total;
        this.succeeded = succeeded;
        this.failed = failed;
        this.status = status;
    }

    void setTotal(int value) {
        total = value;
    }

    public int getTotal() {
        return total;
    }

    public int getSucceeded() {
        return succeeded;
    }

    public int getProcessed() {
        return succeeded + failed;
    }

    public int getStatus() {
        return status;
    }

    public int getFailed() {
        return failed;
    }

    public boolean isRunning() {
        return total > 0 && total > (succeeded + failed);
    }
}