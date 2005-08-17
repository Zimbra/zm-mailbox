package com.zimbra.cs.index;

import java.util.HashMap;
import com.zimbra.cs.service.ServiceException;

/**
 * @author tim
 *
 * A set of UngroupedQueryResults which groups by Message
 */
class MsgQueryResults extends LiquidQueryResultsImpl 
{
//    QueryOperation mResults;
    LiquidQueryResults mResults;

    public MsgQueryResults(LiquidQueryResults topLevelQueryOperation, byte[] types, int searchOrder) {
        super(types, searchOrder);
        mResults = topLevelQueryOperation;
    }

    LiquidHit mNextHit = null;
    
    HashMap mSeenMsgs = new HashMap();
    
    /**
     * Gets the next hit from the QueryOp.  
     * 
     * Side effect: will Op's iterator one or more entries forward
     *  
     * @return
     * @throws ServiceException
     */
    private LiquidHit internalGetNextHit() throws ServiceException {
        while (mResults.hasNext()) {
            LiquidHit opNext = mResults.getNext();
            
            MessageHit curHit = null;
            Integer msgId = new Integer(opNext.getItemId());
            
            curHit = (MessageHit)mSeenMsgs.get(msgId);
            if (curHit != null) {
                // we've seen this Message before...skip this hit
            } else {
                if (opNext instanceof ConversationHit) {
                    assert(false); // not written yet.  If we hit this, need to iterate conv and add ALL of its messages to the hit list here...
                } else if (opNext instanceof MessageHit) {
                    curHit = ((MessageHit)opNext);
                } else if (opNext instanceof MessagePartHit) {
                    curHit = ((MessagePartHit)opNext).getMessageResult();
                } else {
                    return curHit; // wasn't a Conv/Message/Part, so just return it as-is
                }
                
                mSeenMsgs.put(msgId, curHit);
                
                /* Iterate fwd a bit to see if we can pick up more message parts... */
                while (mResults.hasNext()) {
                    LiquidHit nextHit = mResults.peekNext();
                    
                    int newMsgId = nextHit.getItemId();
                    
                    if (newMsgId != msgId.intValue()) {
                        return curHit;
                    } else {
                        mResults.getNext(); // same msg id -- so move iterator fwd
                        if (nextHit instanceof MessagePartHit) {
                            curHit.addPart((MessagePartHit) nextHit);
                        }
                    }
                }
                return curHit;
            }
        }
        return null;
    }
    
    private boolean bufferNextHit() throws ServiceException {
        if (mNextHit == null) {
            mNextHit = internalGetNextHit();
            assert(mNextHit == null || mNextHit instanceof MessageHit);
        }
        return (mNextHit != null);
    }
    
    public void resetIterator() throws ServiceException {
        mSeenMsgs.clear();
        mResults.resetIterator();
    }
    
    public LiquidHit getNext() throws ServiceException {
        bufferNextHit();
        LiquidHit toRet = mNextHit;
        assert(mNextHit == null || mNextHit instanceof MessageHit);
        mNextHit = null;
        return toRet;
    }
    
    public LiquidHit peekNext() throws ServiceException {
        bufferNextHit();
        assert(mNextHit == null || mNextHit instanceof MessageHit);
        return mNextHit;
    }
    
//    public LiquidHit getFirstHit() throws ServiceException {
//        mSeenMsgs.clear();
//        mResults.resetIterator();
//        return internalGetNextHit();
//    }
//
//    public LiquidHit getNextHit() throws ServiceException {
//        return internalGetNextHit();
//    }

    public void doneWithSearchResults() throws ServiceException {
        mResults.doneWithSearchResults();
    }

    public LiquidHit skipToHit(int hitNo) throws ServiceException {
        if (hitNo > 0) {
            mResults.skipToHit(hitNo-1);
        } else {
            resetIterator();
        }
        return getNext();
    }

}
