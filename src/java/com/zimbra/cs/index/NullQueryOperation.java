/*
 * Created on Nov 12, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.index;

import java.io.IOException;

import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.ServiceException;

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
     * @see com.liquidsys.coco.index.QueryOperation#resetIterator()
     */
    public void resetIterator() throws ServiceException {
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.index.QueryOperation#getNext()
     */
    public LiquidHit getNext() throws ServiceException {
        return null;
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.index.QueryOperation#hasNext()
     */
//    boolean hasNext() throws ServiceException {
//        return false;
//    }
    
    public LiquidHit peekNext() throws ServiceException
    {
        return null;
    }
    

    /* (non-Javadoc)
     * @see com.liquidsys.coco.index.QueryOperation#prepare(com.liquidsys.coco.mailbox.Mailbox, com.liquidsys.coco.index.LiquidQueryResultsImpl, com.liquidsys.coco.index.MailboxIndex, int, int)
     */
    protected void prepare(Mailbox mbx, LiquidQueryResultsImpl res, MailboxIndex mbidx)
            throws IOException, ServiceException {
        // empty
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.index.QueryOperation#doneWithSearchResults()
     */
    public void doneWithSearchResults() throws ServiceException {
        //empty 
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.index.QueryOperation#optimize()
     */
    public QueryOperation optimize(Mailbox mbox) throws ServiceException {
        return this;
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.index.QueryOperation#combineOps(com.liquidsys.coco.index.QueryOperation, boolean)
     */
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        return null;
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.index.QueryOperation#inheritedGetExecutionCost()
     */
    protected int inheritedGetExecutionCost() {
        return 0;
    }

}
