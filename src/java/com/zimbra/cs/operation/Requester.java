package com.zimbra.cs.operation;

import com.zimbra.cs.operation.Scheduler.Priority;

/**
 * The Requester is used to denote what subsystem is calling a particular 
 * Operation.  Each Requester has a default Priority level, which can be used
 * or ignored by a particular Operation implementation.
 */
public enum Requester {
    ADMIN(Priority.ADMIN),
    SOAP(Priority.INTERACTIVE_HIGH),
    REST(Priority.INTERACTIVE_LOW),
    IMAP(Priority.BATCH),
    SYNC(Priority.BATCH),
    POP(Priority.BATCH);

    private Priority mDefaultPrio;

    private Requester(Priority defaultPrio) { mDefaultPrio = defaultPrio; }
    public Priority getPriority() { return mDefaultPrio; }
}