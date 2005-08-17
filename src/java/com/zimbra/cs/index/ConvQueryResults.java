package com.liquidsys.coco.index;

import java.util.ArrayList;
import java.util.HashMap;

import com.liquidsys.coco.service.ServiceException;

/**
 * @author tim
 *
 * A set of UngroupedQueryResults which groups by Conversation
 */
class ConvQueryResults extends LiquidQueryResultsImpl {
    LiquidQueryResults mResults;

    public ConvQueryResults(LiquidQueryResults results, byte[] types,
            int searchOrder) {
        super(types, searchOrder);
        mResults = results;
    }

    private LiquidHit mNextHit = null;
    
    private int mNextHitNo = 0;
    private ArrayList mCachedResults = new ArrayList();
    
    HashMap mSeenConversations = new HashMap();

    /**
     * Get the next Conversation Hit from our subOp 
     * 
     * Side-Effect: always advances the SubOp pointer!
     * 
     * @return
     * @throws ServiceException
     */
    private LiquidHit internalGetNextHit() throws ServiceException {
        while (mResults.hasNext()) {
            LiquidHit opNext = mResults.getNext();
            
            ConversationHit curHit = null;
            Integer convId = new Integer(opNext.getConversationId());
            
            curHit = (ConversationHit)mSeenConversations.get(convId);
            if (curHit != null) {
                // we've seen this conv before (and therefore reported it) -- so just do nothing right now... 
            } else {
                if (opNext instanceof ConversationHit) {
                    curHit = (ConversationHit)opNext;
                } else if (opNext instanceof MessageHit) {
                    curHit = ((MessageHit)opNext).getConversationResult();
                } else if (opNext instanceof MessagePartHit) {
                    curHit = ((MessagePartHit)opNext).getMessageResult().getConversationResult();
                } else {
                    // wasn't a Conversation, Message or part hit...so just return it...
                    return opNext;
                }
                mSeenConversations.put(convId, curHit);
                
                /* iterate further: try to get all the hits for this result 
                 * Conversation if they happen to be right here with the first one */
                while (mResults.hasNext()) {
                    LiquidHit nextHit = mResults.peekNext();
                    if (nextHit.getConversationId() != convId.intValue()) {
                        return curHit; // no more, all done!
                    } else {
                        // same conv.  Consume this hit!
                        mResults.getNext();
                        
                        //  Now add this hit to the conv we're gonna return
                        if (nextHit instanceof MessageHit) {
                            curHit.addMessageHit((MessageHit) nextHit);
                        } else if (nextHit instanceof MessagePartHit) {
                            curHit.addMessageHit(((MessagePartHit) nextHit).getMessageResult());
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
        }
        return (mNextHit != null);
    }
    
    public LiquidHit peekNext() throws ServiceException {
        bufferNextHit();
        return mNextHit;
    }
    
    public void resetIterator() throws ServiceException {
        mSeenConversations.clear();
        mResults.resetIterator();
        mNextHitNo = 0;
        mCachedResults.clear(); // must clear since we clear mSeenConversations...
        mNextHit = null;
    }
    
//    public LiquidHit getFirstHit() throws ServiceException {
//        mSeenConversations.clear();
//        mResults.resetIterator();
//        mNextHitNo = 0;
//        mCachedResults.clear(); // must clear since we clear mSeenConversations...
//        LiquidHit retVal = null;
//        retVal = internalGetNextHit();
//        if (retVal != null) {
//            mCachedResults.add(mNextHitNo, retVal);
//        }
//        if (retVal != null) {
//            mNextHitNo++;
//        }
//        return retVal;
//    }

    public LiquidHit getNext() throws ServiceException {
        LiquidHit retVal = null;
        if (mCachedResults.size() > mNextHitNo) {
            retVal = (LiquidHit)mCachedResults.get(mNextHitNo);
        } else {
//            retVal = internalGetNextHit();
//            if (retVal != null) {
//                mCachedResults.add(mNextHitNo, retVal);
//            }
            if (bufferNextHit()) {
                retVal = mNextHit;
                mCachedResults.add(mNextHitNo, mNextHit);
                mNextHit = null;
            }
        }
        if (retVal != null) {
            mNextHitNo++;
        }
        return retVal;
    }

    public void doneWithSearchResults() throws ServiceException {
        mResults.doneWithSearchResults();
    }

    public LiquidHit skipToHit(int hitNo) throws ServiceException {
        if (hitNo == 0) {
            resetIterator();
            return getNext();
        } else {
            LiquidHit retVal = null;
            if (hitNo < mCachedResults.size()) {
                mNextHitNo = hitNo;
            } else {
                mNextHitNo = mCachedResults.size();
            }
            while(mNextHitNo <= hitNo) {
                retVal = getNext();
                if (retVal == null) {
                    break;
                }
            } 
            return retVal;
        }
    }
}

