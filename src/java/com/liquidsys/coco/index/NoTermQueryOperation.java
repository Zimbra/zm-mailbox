package com.liquidsys.coco.index;

import java.io.IOException;

import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.ServiceException;

/**
 * @author tim
 * 
 * A QueryOperation which is generated when a query term evaluates to "nothing".  
 * 
 * This is not the same as a NullQueryOperation because:
 *     RESULTS(Op AND NoTermQuery) = RESULTS(Op)
 *     
 * It is also not the same as an AllQueryOperation because:
 *     RESULTS(NoTemQuery) = NONE
 *
 * Basically, this pseudo-Operation is here to handle the situation when a Lucene term 
 * evaluates to the empty string -- by generating a special-purpose Pseudo-Operation for 
 * this case we can hand-tune the Optimizer behavior and make it do the right thing in all 
 * cases.
 *
 */
public class NoTermQueryOperation extends QueryOperation {

    public NoTermQueryOperation() {
        super();
    }
    
    int getOpType() {
        return OP_TYPE_NO_TERM;
    }

    protected void prepare(Mailbox mbx, LiquidQueryResultsImpl res,
            MailboxIndex mbidx) throws IOException, ServiceException {
    }

    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash,
            boolean includeSpam) throws ServiceException {
        return this;
    }

    boolean hasSpamTrashSetting() {
        return false;
    }

    void forceHasSpamTrashSetting() {
        assert(false);
    }

    boolean hasNoResults() {
        // TODO Auto-generated method stub
        return false;
    }

    boolean hasAllResults() {
        // TODO Auto-generated method stub
        return false;
    }

    QueryOperation optimize(Mailbox mbox) throws ServiceException {
        return null;
    }

    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        return other;
    }

    protected int inheritedGetExecutionCost() {
        return 10000;
    }

    public void resetIterator() throws ServiceException {
        }

    public LiquidHit getNext() throws ServiceException {
        return null;
    }

    public LiquidHit peekNext() throws ServiceException {
        return null;
    }

    public void doneWithSearchResults() throws ServiceException {
    }

}
