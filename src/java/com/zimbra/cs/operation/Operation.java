package com.zimbra.cs.operation;

import java.util.List;

import org.apache.commons.logging.Log;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Scheduler.IOperation;
import com.zimbra.cs.operation.Scheduler.Priority;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.Session.RecentOperation;
import com.zimbra.cs.util.ZimbraLog;

public abstract class Operation implements IOperation 
{
	/**
	 * The Requester is used to denote what subsystem is calling a particular 
	 * Operation.  Each Requester has a default Priority level, which can be used
	 * or ignored by a particular Operation implementation.
	 */
	public static enum Requester {
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

	/**
	 * @param session {@link com.zimbra.cs.Session} object or NULL if none
	 * @param oc        OperationContext for the current request
	 * @param mbox     Mailbox this request is targeted to, or NULL if none
	 * @param req      {@link Requester} gives information about what subsystem is making this request
	 * @param basePriority Base priority of this operation, which may be updated by the overloadable {@link calcPriority} API 
	 * @param load     The load (see {@link Scheduler}) of this operation
	 */
	protected Operation(Session session, OperationContext oc, Mailbox mbox, Requester req, Priority basePriority, int load) {
		mSession = session;
		mOpCtxt = oc;
		mMailbox = mbox;
		mPriority = calcPriority(basePriority);
		mLoad = load;
		mReq = req;
	}
	
	/**
	 * This version uses the default priority of the Requestor
	 * 
	 * @param session {@link com.zimbra.cs.Session} object or NULL if none
	 * @param oc        OperationContext for the current request
	 * @param mbox     Mailbox this request is targeted to, or NULL if none
	 * @param req      {@link Requester} gives information about what subsystem is making this request
	 * @param load     The load (see {@link Scheduler}) of this operation
	 */
	protected Operation(Session session, OperationContext oc, Mailbox mbox, Requester req, int load) {
		this(session, oc, mbox, req, req.getPriority(), load);
	}
	
	
	/**
	 * This abstract method is called in the subclass when the operation is ready to execute.
	 * This call should do the actual "work" of the operation
	 * 
	 * @throws ServiceException
	 */
	abstract protected void callback() throws ServiceException;
	
	/**
	 * This call handles the scheduling, logging, and cleanup of the operation.
	 * This call will block the calling thread until the operation can execute, and
	 * then the abstract callback() method will be called on this operation.
	 * 
	 * run() will automatically log Operation state at the Scheduling, Executing,
	 * and Completed phases, according to logging properties.
	 * 
	 * @throws ServiceException
	 */
	protected void run() throws ServiceException {
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

		//
		// At this point, we've gotten through the scheduler
		// and we know it is time to run.  Run the operation
		//
		long start=0;
		try {
			if (ZimbraLog.op.isInfoEnabled())
				start = System.currentTimeMillis();
			if (ZimbraLog.op.isDebugEnabled()) 
				ZimbraLog.op.debug("Executing:  "+toExecutingString());
			callback();
		} finally {
			//
			// We must not miss this part, or else the Scheduler's
			// bookkeeping will be messed up
			//
			mSched.runCompleted(this);
			if (ZimbraLog.op.isInfoEnabled()) {
				long end = System.currentTimeMillis();
				ZimbraLog.op.info("Completed("+(end-start)+"ms): "+toCompletedString());
			}
		}
	}

	/**
	 * This call is used to generate logging information which is logged to the 
	 * ZimbraLog.op logger at the DEBUG level when an operation being scheduled.
	 * 
	 * Overload this call if you want to print something specific for the operation while 
	 * it is being scheduled.
	 * 
	 * @return
	 */
	protected String toSchedulingString() { return toString(); }
	
	
	/**
	 * This call is used to generate logging information which is logged to the 
	 * ZimbraLog.op logger at the INFO level immediately before the operation
	 * is executed.
	 * 
	 * Overload this call if you want to print something specific for this operation
	 * after it has been scheduled and immediately before the callback() function
	 * is called
	 * 
	 * @return
	 */
	protected String toExecutingString() { return toString(); }
	
	
	/**
	 * This call is used to generate logging information which is logged to the 
	 * ZimbraLog.op logger at the INFO level after the operation.
	 * 
	 * Overload this call if you want to print something specific for this operation
	 * after it has been scheduled and immediately before the callback() function
	 * is called
	 * 
	 * @return
	 */
	protected String toCompletedString() { return toString(); }
	
	public String toString() {
		StringBuilder toRet = new StringBuilder(this.getClass().getSimpleName());
		toRet.append(" Req=").append(mReq.toString());
		if (mMailbox != null)
			toRet.append(" Mbox=").append(mMailbox.getId());
		if (mSession != null) 
			toRet.append(" Session=").append(mSession.getSessionId());
		toRet.append(" Pri=").append(mPriority.toString());
		toRet.append(" Load=").append(mLoad);
		return toRet.toString();
	}

	
	/**
	 * Calculate the priority of this operation given the passed-in base priority.
	 * The default implementation will lower the priority by one step if the session
	 * object tells us this is a "repeated" operation. @see isRepeatedOperation below
	 * 
	 * @param basePriority
	 * @return
	 */
	protected Priority calcPriority(Priority basePriority) {
		Priority toRet = basePriority;

		if (mSession != null) {
			List<Session.RecentOperation> recentOps = mSession.getRecentOperations();
			
			if (isRepeatedOperation(recentOps)) {
				toRet = toRet.decrement();
			}
		}
		
		return toRet;
	}

	protected static final int OPERATION_REPEAT_THRESHOLD  = Session.OPERATION_HISTORY_LENGTH-2;
	
	/**
	 * Given a list of the recent operations on the current session, this function returns
	 * TRUE if the current operation can be considered a "repeated" operation.  
	 * 
	 * @param recentOps
	 * @return
	 */
	protected boolean isRepeatedOperation(List<Session.RecentOperation> recentOps) {
		synchronized(recentOps) {
			int counted = 0;
			for (RecentOperation rop : recentOps) {
				if (rop.mOperationClass == this.getClass())
					counted++;
			}
			return (counted >= OPERATION_REPEAT_THRESHOLD);
		}
	}
	
	protected Log getLog() { return ZimbraLog.op; }
	
	public String getName() {
		return this.getClass().getSimpleName();
	}
	
	public int getLoad() {
		return mLoad;
	}

	public Mailbox getMailbox() {
		return mMailbox;
	}

	public OperationContext getOpCtxt() {
		return mOpCtxt;
	}

	public Priority getPriority() {
		return mPriority;
	}

	public Requester getReq() {
		return mReq;
	}

	public Scheduler getSched() {
		return mSched;
	}

	public Session getSession() {
		return mSession;
	}
	
	protected Priority mPriority;
	protected int mLoad;
	protected Session mSession;
	protected OperationContext mOpCtxt;
	protected Mailbox mMailbox;
	protected Scheduler mSched;
	protected Requester mReq;
}
