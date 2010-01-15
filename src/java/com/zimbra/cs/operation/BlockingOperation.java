/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.operation;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Scheduler.Priority;
import com.zimbra.cs.session.Session;

/**
 * 
 */
public class BlockingOperation extends Operation {
    
    public static final BlockingOperation schedule(String opName, Session session, OperationContext oc, Mailbox mbox, Requester req,
        Priority basePriority, int load) throws ServiceException {
        BlockingOperation toRet = new BlockingOperation(opName, session, oc, mbox, req, basePriority, load);
        toRet.start();
        return toRet;
    }

    /**
     * @param session
     * @param oc
     * @param mbox
     * @param req
     * @param basePriority
     * @param load
     */
    private BlockingOperation(String opName, Session session, OperationContext oc, Mailbox mbox, Requester req,
        Priority basePriority, int load) {
        super(session, oc, mbox, req, basePriority, load);
        mOperationName = opName;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.operation.Operation#callback()
     */
    @Override
    protected void callback() throws ServiceException {
    }
    
    
    public void schedule() throws ServiceException {
        throw ServiceException.FAILURE("Cannot schedule() a BlockingOperation", null);
    }
    
    protected String getClassName() {
        return mOperationName;
    }
    
    public void start() throws ServiceException {
        assert(mMailbox == null || !Thread.holdsLock(mMailbox));
        if (ZimbraLog.op.isDebugEnabled())
            ZimbraLog.op.debug("Scheduling "+toSchedulingString());

        // 
        // Schedule the operation.  This part
        // may block the current thread
        //
        mSched = Scheduler.get(mMailbox);
        mSched.schedule(this);
        if (mSession != null)
            mSession.logOperation(this);

        if (ZimbraLog.op.isInfoEnabled())
            mStart = System.currentTimeMillis();
        if (ZimbraLog.op.isDebugEnabled()) 
            ZimbraLog.op.debug("Executing:  "+toExecutingString());
    }
    
    public void finish() {
        //
        // We must not miss this part, or else the Scheduler's
        // bookkeeping will be messed up
        mSched.runCompleted(this);
        if (ZimbraLog.op.isInfoEnabled()) {
            long end = System.currentTimeMillis();
            ZimbraLog.op.info("Completed("+(end-mStart)+"ms): "+toCompletedString());
        }
    }
    
    private long mStart = 0;
    private String mOperationName;
}
