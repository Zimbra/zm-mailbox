package com.zimbra.cs.mailbox;


/**
 * Re-index progress information. The counters are thread safe.
 * @author Greg Solovyev
 */
public final class ReIndexStatus  {
    private volatile int total = 0;
    private volatile int succeeded = 0;
    private volatile int failed = 0;

    public ReIndexStatus() {
    }
    
    public ReIndexStatus(int total, int succeeded) {
        this.total = total;
        this.succeeded = succeeded;
        this.failed = 0;
    }

    public ReIndexStatus(int total, int succeeded, int failed) {
        this.total = total;
        this.succeeded = succeeded;
        this.failed = failed;
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

    public boolean isCancelled() {
        return total < 0;
    }
    
    public int getFailed() {
        return failed;
    }
    
    public boolean isRunning() {
        return total > 0 && total > (succeeded + failed);
    }
}