/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;

/**
 * A set of {@link UngroupedQueryResults} which groups by Message.
 *
 * @author tim
 * @author ysasaki
 */
final class MsgQueryResults extends ZimbraQueryResultsImpl {
    private final ZimbraQueryResults results;
    private ZimbraHit nextHit = null;

    /**
     * Cache of local Message IDs we've seen this iteration -- used so
     * that in situations where we have multiple {@link MessagePartHit}s we
     * still only return a single Message.
     */
    private final Set<Integer> mSeenMsgs = new HashSet<Integer>();

    MsgQueryResults(ZimbraQueryResults topLevelQueryOperation, Set<MailItem.Type> types,
            SortBy sort, SearchParams.Fetch fetch) {
        super(types, sort, fetch);
        results = topLevelQueryOperation;
    }

    @Override
    public long getCursorOffset() {
        return results.getCursorOffset();
    }

    /**
     * Gets the next hit from the QueryOp.
     *
     * Side effect: will Op's iterator one or more entries forward
     *
     * @return next hit
     */
    private ZimbraHit internalGetNextHit() throws ServiceException {
        while (results.hasNext()) {
            ZimbraHit hit = results.getNext();

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
                while (results.hasNext()) {
                    ZimbraHit next = results.peekNext();
                    if (next.isLocal() && iid == next.getItemId()) {
                        results.getNext(); // move iterator fwd
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
        if (nextHit == null) {
            nextHit = internalGetNextHit();
        }
        return (nextHit != null);
    }

    @Override
    public void resetIterator() throws ServiceException {
        mSeenMsgs.clear();
        results.resetIterator();
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        bufferNextHit();
        ZimbraHit toRet = nextHit;
        assert(nextHit == null || (!(nextHit instanceof MessagePartHit) && !(nextHit instanceof ConversationHit)));
        nextHit = null;
        return toRet;
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        bufferNextHit();
        assert(nextHit == null || (!(nextHit instanceof MessagePartHit) && !(nextHit instanceof ConversationHit)));
        return nextHit;
    }

    @Override
    public void close() throws IOException {
        results.close();
    }

    @Override
    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        if (hitNo > 0) {
            results.skipToHit(hitNo-1);
        } else {
            resetIterator();
        }
        return getNext();
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return results.getResultInfo();
    }

}
