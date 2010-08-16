/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * A set of {@link UngroupedQueryResults} which groups by Message.
 *
 * @author tim
 * @author ysasaki
 */
class MsgQueryResults extends ZimbraQueryResultsImpl {
    private ZimbraQueryResults mResults;
    private ZimbraHit mNextHit = null;

    /**
     * Cache of local Message IDs we've seen this iteration -- used so
     * that in situations where we have multiple {@link MessagePartHit}s we
     * still only return a single Message.
     */
    private final Set<Integer> mSeenMsgs = new HashSet<Integer>();

    MsgQueryResults(ZimbraQueryResults topLevelQueryOperation, byte[] types,
            SortBy searchOrder, Mailbox.SearchResultMode mode) {
        super(types, searchOrder, mode);
        mResults = topLevelQueryOperation;
    }

    /**
     * Gets the next hit from the QueryOp.
     *
     * Side effect: will Op's iterator one or more entries forward
     *
     * @return next hit
     * @throws ServiceException
     */
    private ZimbraHit internalGetNextHit() throws ServiceException {
        while (mResults.hasNext()) {
            ZimbraHit hit = mResults.getNext();

            MessageHit msgHit;
            if (hit instanceof MessageHit) {
                msgHit = (MessageHit) hit;
            } else if (hit instanceof MessagePartHit) {
                msgHit = ((MessagePartHit) hit).getMessageResult();
            } else if (hit instanceof ConversationHit) { // TODO not written yet
                throw new UnsupportedOperationException();
                // If we hit this, need to iterate conv and add ALL of its
                // messages to the hit list here...
            } else { // wasn't a Conv/Message/Part, so just return it as-is
                return hit;
            }

            int iid = msgHit.getItemId();
            if (mSeenMsgs.add(iid)) { // skip if we've seen this Message before
                // Iterate fwd a bit to see if we can pick up more message parts
                while (mResults.hasNext()) {
                    ZimbraHit next = mResults.peekNext();
                    if (iid == next.getItemId()) { // same msg id
                        mResults.getNext(); // move iterator fwd
                        if (next instanceof MessagePartHit) {
                            msgHit.addPart((MessagePartHit) next);
                        }
                    } else {
                        break;
                    }
                }
                return msgHit;
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

    @Override
    public void resetIterator() throws ServiceException {
        mSeenMsgs.clear();
        mResults.resetIterator();
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        bufferNextHit();
        ZimbraHit toRet = mNextHit;
        assert(mNextHit == null || (!(mNextHit instanceof MessagePartHit) &&
                !(mNextHit instanceof ConversationHit)));
        mNextHit = null;
        return toRet;
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        bufferNextHit();
        assert(mNextHit == null || (!(mNextHit instanceof MessagePartHit) &&
                !(mNextHit instanceof ConversationHit)));
        return mNextHit;
    }

    @Override
    public void doneWithSearchResults() throws ServiceException {
        mResults.doneWithSearchResults();
    }

    @Override
    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        if (hitNo > 0) {
            mResults.skipToHit(hitNo-1);
        } else {
            resetIterator();
        }
        return getNext();
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return mResults.getResultInfo();
    }

    @Override
    public int estimateResultSize() throws ServiceException {
        return mResults.estimateResultSize();
    }

}
