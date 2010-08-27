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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * A set of UngroupedQueryResults which groups by Conversation.
 *
 * @author tim
 */
final class ConvQueryResults extends ZimbraQueryResultsImpl {

    private ZimbraQueryResults mResults;
    private ZimbraHit mNextHit;
    private int mNextHitNo = 0;
    private List<ZimbraHit> mCachedResults = new ArrayList<ZimbraHit>();
    private Set<Integer> mSeenConvIDs = new HashSet<Integer>();

    ConvQueryResults(ZimbraQueryResults results, byte[] types,
            SortBy searchOrder, Mailbox.SearchResultMode mode) {
        super(types, searchOrder, mode);
        mResults = results;
    }

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

            if (!isConversation(opNext)) {
                return opNext;
            }

            int convId = opNext.getConversationId();
            if (mSeenConvIDs.contains(convId)) {
                // we've seen this conv before (and therefore reported it),
                // so just do nothing right now...
            } else {
                ConversationHit curHit = null;
                if (opNext instanceof ConversationHit) {
                    curHit = (ConversationHit) opNext;
                } else if (opNext instanceof MessageHit) {
                    curHit = ((MessageHit) opNext).getConversationResult();
                } else if (opNext instanceof MessagePartHit) {
                    curHit = ((MessagePartHit) opNext).getMessageResult().getConversationResult();
                } else {
                    assert(false);
                }
                mSeenConvIDs.add(convId);

                // iterate further: try to get all the hits for this result
                // Conversation if they happen to be right here with the first one
                while (mResults.hasNext()) {
                    ZimbraHit nextHit = mResults.peekNext();
                    if (!isConversation(nextHit)) {
                        return curHit;
                    } else {
                        if (nextHit.getConversationId() != convId) {
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
        return null;
    }

    private boolean isConversation(ZimbraHit hit) {
        return (hit instanceof MessageHit || hit instanceof MessagePartHit ||
                hit instanceof ConversationHit);
    }

    private boolean bufferNextHit() throws ServiceException {
        if (mNextHit == null) {
            mNextHit = internalGetNextHit();
        }
        return (mNextHit != null);
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        bufferNextHit();
        return mNextHit;
    }

    @Override
    public void resetIterator() throws ServiceException {
        mSeenConvIDs.clear();
        mResults.resetIterator();
        mNextHitNo = 0;
        mCachedResults.clear(); // must clear since we clear mSeenConvIDs
        mNextHit = null;
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        ZimbraHit hit = null;
        if (mCachedResults.size() > mNextHitNo) {
            hit = mCachedResults.get(mNextHitNo);
        } else if (bufferNextHit()) {
            hit = mNextHit;
            mCachedResults.add(mNextHitNo, mNextHit);
            mNextHit = null;
        }
        if (hit != null) {
            mNextHitNo++;
        }
        return hit;
    }

    @Override
    public void doneWithSearchResults() throws ServiceException {
        mResults.doneWithSearchResults();
    }

    @Override
    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        if (hitNo == 0) {
            resetIterator();
            return getNext();
        } else {
            ZimbraHit hit = null;
            if (hitNo < mCachedResults.size()) {
                mNextHitNo = hitNo;
            } else {
                mNextHitNo = mCachedResults.size();
            }
            while(mNextHitNo <= hitNo) {
                hit = getNext();
                if (hit == null) {
                    break;
                }
            }
            return hit;
        }
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return mResults.getResultInfo();
    }

    @Override
    public int estimateResultSize() throws ServiceException {
        // guess 1.5 msgs/conv
        return (int) (mResults.estimateResultSize() / 1.5);
    }
}

