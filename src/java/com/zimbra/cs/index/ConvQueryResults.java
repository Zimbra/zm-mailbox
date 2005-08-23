/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.HashMap;

import com.zimbra.cs.service.ServiceException;

/**
 * @author tim
 *
 * A set of UngroupedQueryResults which groups by Conversation
 */
class ConvQueryResults extends ZimbraQueryResultsImpl {
    ZimbraQueryResults mResults;

    public ConvQueryResults(ZimbraQueryResults results, byte[] types,
            int searchOrder) {
        super(types, searchOrder);
        mResults = results;
    }

    private ZimbraHit mNextHit = null;
    
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
    private ZimbraHit internalGetNextHit() throws ServiceException {
        while (mResults.hasNext()) {
            ZimbraHit opNext = mResults.getNext();
            
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
                    ZimbraHit nextHit = mResults.peekNext();
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
    
    public ZimbraHit peekNext() throws ServiceException {
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
    
//    public ZimbraHit getFirstHit() throws ServiceException {
//        mSeenConversations.clear();
//        mResults.resetIterator();
//        mNextHitNo = 0;
//        mCachedResults.clear(); // must clear since we clear mSeenConversations...
//        ZimbraHit retVal = null;
//        retVal = internalGetNextHit();
//        if (retVal != null) {
//            mCachedResults.add(mNextHitNo, retVal);
//        }
//        if (retVal != null) {
//            mNextHitNo++;
//        }
//        return retVal;
//    }

    public ZimbraHit getNext() throws ServiceException {
        ZimbraHit retVal = null;
        if (mCachedResults.size() > mNextHitNo) {
            retVal = (ZimbraHit)mCachedResults.get(mNextHitNo);
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

    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        if (hitNo == 0) {
            resetIterator();
            return getNext();
        } else {
            ZimbraHit retVal = null;
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

