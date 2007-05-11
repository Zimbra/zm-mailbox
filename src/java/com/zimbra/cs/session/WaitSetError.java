package com.zimbra.cs.session;

/**
 * Simple struct used to communicate error codes for individual accounts during a wait 
 */
public class WaitSetError {
    public static enum Type {
        ALREADY_IN_SET_DURING_ADD,
        ERROR_LOADING_MAILBOX,
        MAINTENANCE_MODE,
        NO_SUCH_ACCOUNT,
        WRONG_HOST_FOR_ACCOUNT,
        NOT_IN_SET_DURING_REMOVE,
        NOT_IN_SET_DURING_UPDATE,
        ;
    }

    public WaitSetError(String accountId, WaitSetError.Type error) {
        this.accountId = accountId;
        this.error = error;
    }
    public final String accountId;

    public final WaitSetError.Type error;
}