package com.zimbra.cs.mailbox;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.lock.ZLock;

public class LockFailedException extends RuntimeException {
    private static final long serialVersionUID = -6899718561860023270L;

    public LockFailedException(final String message) {
        super(message);
    }

    public LockFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
