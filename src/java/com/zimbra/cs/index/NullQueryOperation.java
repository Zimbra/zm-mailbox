/*
 * Created on Nov 12, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;

/**
 * @author tim
 * 
 * A query operation which returns no elements at all.
 *
 */
class NullQueryOperation extends QueryOperation {

    public NullQueryOperation() {
        
    }
    
    int getOpType() {
        return OP_TYPE_NULL;
    }
    
    public QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) throws ServiceException
    {
        return this;
    }
    
    public boolean hasSpamTrashSetting() {
        // if someone ANDS with us, then there's no need to set the spam-trash b/c
        // we match nothing.  On the other hand, if someone OR's with us, this func's 
        // return won't matter 
        return true;
    }
    void forceHasSpamTrashSetting() {
    }
    
    boolean hasNoResults() {
        return true;
    }
    boolean hasAllResults() {
        return false;
    }
    
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#resetIterator()
     */
    public void resetIterator() throws ServiceException {
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#getNext()
     */
    public ZimbraHit getNext() throws ServiceException {
        return null;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#hasNext()
     */
//    boolean hasNext() throws ServiceException {
//        return false;
//    }
    
    public ZimbraHit peekNext() throws ServiceException
    {
        return null;
    }
    

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#prepare(com.zimbra.cs.mailbox.Mailbox, com.zimbra.cs.index.ZimbraQueryResultsImpl, com.zimbra.cs.index.MailboxIndex, int, int)
     */
    protected void prepare(Mailbox mbx, ZimbraQueryResultsImpl res, MailboxIndex mbidx)
            throws IOException, ServiceException {
        // empty
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#doneWithSearchResults()
     */
    public void doneWithSearchResults() throws ServiceException {
        //empty 
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#optimize()
     */
    public QueryOperation optimize(Mailbox mbox) throws ServiceException {
        return this;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#combineOps(com.zimbra.cs.index.QueryOperation, boolean)
     */
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        return null;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#inheritedGetExecutionCost()
     */
    protected int inheritedGetExecutionCost() {
        return 0;
    }

}
