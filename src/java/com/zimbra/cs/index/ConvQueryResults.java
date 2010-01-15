/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author tim
 *
 * A set of UngroupedQueryResults which groups by Conversation
 */
class ConvQueryResults extends ZimbraQueryResultsImpl {
    ZimbraQueryResults mResults;

    ConvQueryResults(ZimbraQueryResults results, byte[] types,
            SortBy searchOrder, Mailbox.SearchResultMode mode) {
        super(types, searchOrder, mode);
        mResults = results;
    }

    private ZimbraHit mNextHit = null;
    
    private int mNextHitNo = 0;
    private ArrayList<ZimbraHit> mCachedResults = new ArrayList<ZimbraHit>();
    
    HashMap<Integer, ConversationHit> mSeenConversations = new HashMap<Integer, ConversationHit>();

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
            
            if ((!(opNext instanceof MessageHit)) && (!(opNext instanceof MessagePartHit)) && (!(opNext instanceof ConversationHit))) {
                return opNext;
            } else {
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
                        if ((!(nextHit instanceof MessageHit)) && (!(nextHit instanceof MessagePartHit)) && (!(nextHit instanceof ConversationHit))) {
                            return curHit;
                        } else {
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
                    }
                    return curHit;
                }
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
    
    public ZimbraHit getNext() throws ServiceException {
        ZimbraHit retVal = null;
        if (mCachedResults.size() > mNextHitNo) {
            retVal = (ZimbraHit)mCachedResults.get(mNextHitNo);
        } else {
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
    
    public List<QueryInfo> getResultInfo() { return mResults.getResultInfo(); }
    
    public int estimateResultSize() throws ServiceException {
        // guess 1.5 msgs/conv
        return (int)((double)mResults.estimateResultSize() / 1.5);
    }
}

