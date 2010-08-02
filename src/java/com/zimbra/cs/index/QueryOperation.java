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

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.ResultValidator.QueryResult;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.SearchResultMode;

/**
 * A {@link QueryOperation} is a part of a Search request -- there are
 * potentially multiple query operations in a search. {@link QueryOperation}
 * return {@link ZimbraQueryResultsImpl} sets -- which can be iterated over to
 * get {@link ZimbraHit} objects.
 * <p
 * The difference between a {@link QueryOperation} and a simple
 * {@link ZimbraQueryResultsImpl} set is that a {@link QueryOperation} knows how
 * to Optimize and Execute itself -- whereas a {@link QueryResult} set is just
 * a set of results and can only be iterated.
 *
 * @since Oct 15, 2004
 */
abstract class QueryOperation implements Cloneable, ZimbraQueryResults {
    static final int MIN_CHUNK_SIZE = 26;
    static final int MAX_CHUNK_SIZE = 5000;

    protected SearchParams mParams;

    /**
     * @return A representation of this operation as a parsable query string
     */
    abstract String toQueryString();

    public SortBy getSortBy() {
        return mParams.getSortBy();
    }

    // based on data from our internal mail server:
    //
    // HOWTO Calculate avg msgs/conv from SQL:
    //
    // TOT_MSGS = select count(*) from mail_item mi where mi.type=5 and mi.mailbox_id=?;
    // TOT_CONVS = select count(*) from mail_item mi where mi.type=4 and mi.mailbox_id=?;
    // TOT_VIRTUAL_CONVS = select count(*) from mail_item mi where mi.type=5 and mi.parent_id is NULL and mi.mailbox_id=?;
    //
    // MSGS_PER_CONV = TOT_MSGS / (TOT_CONVS + TOT_VIRT_CONVS);
    //
    private static final float MESSAGES_PER_CONV_ESTIMATE = 2.25f;

    // What level of result grouping do we want?  ConversationResult, MessageResult, or DocumentResult?
    private static enum Grouping {
        CONVERSATION, MESSAGE, ITEM;
    };

    ////////////////////
    // Top-Level Execution
    final ZimbraQueryResults run(Mailbox mbox, MailboxIndex mbidx,
            SearchParams params, int chunkSize) throws IOException, ServiceException {
        mParams = params;
        mIsToplevelQueryOp = true;

        chunkSize++; // one extra for checking the "more" flag at the end of the results

        if (chunkSize < MIN_CHUNK_SIZE) {
            chunkSize = MIN_CHUNK_SIZE;
        } else if (chunkSize > MAX_CHUNK_SIZE) {
            chunkSize = MAX_CHUNK_SIZE;
        }

        Grouping retType = Grouping.ITEM; //MailboxIndex.SEARCH_RETURN_DOCUMENTS;
        byte[] types = mParams.getTypes();
        for (int i = 0; i < types.length; i++) {
            if (types[i] == MailItem.TYPE_CONVERSATION) {
                retType = Grouping.CONVERSATION; //MailboxIndex.SEARCH_RETURN_CONVERSATIONS;
                break;
            }
            if (types[i] == MailItem.TYPE_MESSAGE) {
                retType = Grouping.MESSAGE; // MailboxIndex.SEARCH_RETURN_MESSAGES;
                break;
            }
        }

        // set me to TRUE if you're returning Conversations or something which could benefit from preloading
        boolean preloadOuterResults = false;

        int outerChunkSize = chunkSize;

        boolean usePreloadingGrouper = true;

        // don't preload if all we want is IDs!
        if (mParams.getMode() == SearchResultMode.IDS) {
            usePreloadingGrouper = false;
        }

        switch (retType) {
            case CONVERSATION: // MailboxIndex.SEARCH_RETURN_CONVERSATIONS:
                if (mParams.getPrefetch() && usePreloadingGrouper) {
                    chunkSize+= 2; // one for the ConvQueryResults, one for the Grouper
                    setupResults(mbox, new ConvQueryResults(
                            new ItemPreloadingGrouper(this, chunkSize, mbox),
                            types, mParams.getSortBy(), mParams.getMode()));
                    chunkSize *= MESSAGES_PER_CONV_ESTIMATE; // guess 2 msgs per conv
                } else {
                    chunkSize++; // one for the ConvQueryResults
                    setupResults(mbox, new ConvQueryResults(this,
                            types, mParams.getSortBy(), mParams.getMode()));
                    chunkSize *= MESSAGES_PER_CONV_ESTIMATE;
                }
                preloadOuterResults = true;
                break;
            case MESSAGE: //MailboxIndex.SEARCH_RETURN_MESSAGES:
                if (mParams.getPrefetch()  && usePreloadingGrouper) {
                    chunkSize += 2; // one for the MsgQueryResults, one for the Grouper
                    setupResults(mbox, new MsgQueryResults(
                            new ItemPreloadingGrouper(this, chunkSize, mbox),
                            types, mParams.getSortBy(), mParams.getMode()));
                } else {
                    chunkSize++; // one for the MsgQueryResults
                    setupResults(mbox, new MsgQueryResults(this,
                            types, mParams.getSortBy(), mParams.getMode()));
                }
                break;
            case ITEM: //MailboxIndex.SEARCH_RETURN_DOCUMENTS:
                if (mParams.getPrefetch() && usePreloadingGrouper) {
                    chunkSize++; // one for the grouper
                    setupResults(mbox, new UngroupedQueryResults(
                            new ItemPreloadingGrouper(this, chunkSize, mbox),
                            types, mParams.getSortBy(), mParams.getMode()));
                } else {
                    setupResults(mbox, new UngroupedQueryResults(this,
                            types, mParams.getSortBy(), mParams.getMode()));
                }
                break;
        }

        prepare(mMailbox, mResults, mbidx, mParams, chunkSize);

        if (usePreloadingGrouper && preloadOuterResults && mParams.getPrefetch()) {
            return new ItemPreloadingGrouper(mResults, outerChunkSize, mbox);
        } else {
            return mResults;
        }
    }


    /******************
     *
     * Hits iteration
     *
     *******************/
    public boolean hasNext() throws ServiceException {
        return peekNext() != null;
    }

    /**
     *
     * prepare() is the API which begins query execution.  It is allowed to grab and hold resources, which are then
     * released via doneWithSearchResults().
     *
     *
     * IMPORTANT IMPORTANT: prepare() and doneWithSearchResults must always be called in a pair.  That is,
     * if you call prepare, you MUST call doneWithSearchResults.
     *
     * @param mbx
     * @param res
     * @param mbidx
     * @param chunkSize A hint to the query operation telling it what size to chunk data in.  Higher numbers
     *                   can be more efficient if you are using a lot of results, but have more overhead
     * @throws IOException
     * @throws ServiceException
     */
    protected abstract void prepare(Mailbox mbx, ZimbraQueryResultsImpl res,
            MailboxIndex mbidx, SearchParams params, int chunkSize)
        throws IOException, ServiceException;

    public ZimbraHit getFirstHit() throws ServiceException {
        resetIterator();
        return getNext();
    }

    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        resetIterator();
        for (int i = 0; i < hitNo; i++) {
            if (!hasNext()) {
                return null;
            }
            getNext();
        }
        return getNext();
    }

    /******************
     *
     * Internals
     *
     *******************/

    abstract QueryTargetSet getQueryTargets();

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            assert(false); // better not happen
        }
        return null;
    }

    private boolean mIsToplevelQueryOp = false;
    protected boolean isTopLevelQueryOp() {
        return mIsToplevelQueryOp;
    }

    private ZimbraQueryResultsImpl mResults;
    private Mailbox mMailbox;

    final protected Mailbox getMailbox() {
        return mMailbox;
    }

    final protected ZimbraQueryResultsImpl getResultsSet() {
        return mResults;
    }

    final protected void setupResults(Mailbox mbx, ZimbraQueryResultsImpl res) {
        mMailbox = mbx;
        mResults = res;
    }

    /**
     * We use this code to recursively descend the operation tree and set the "-in:junk -in:trash"
     * setting as necessary.  Descend down the tree, when you hit an AND, then only one of the
     * subtrees must have it, when you hit an OR, then every subtree must have a setting, or else one must
     * be added.
     * @param includeTrash TODO
     * @param includeSpam TODO
     *
     */
    abstract QueryOperation ensureSpamTrashSetting(Mailbox mbox,
            boolean includeTrash, boolean includeSpam) throws ServiceException;

    /**
     * @return TRUE if this subtree has a trash/spam paramater, FALSE if one is needed.
     */
    abstract boolean hasSpamTrashSetting();

    /**
     * A bit of a hack -- when we combine a "all items including trash/spam" term with another query, this
     * API lets us force the "include trash/spam" part in the other query and thereby drop the 1st one.
     */
    abstract void forceHasSpamTrashSetting();

    /**
     * @return TRUE if this QueryOperation definitely has no results.  Note that this API might
     * return FALSE in some cases where there really aren't any results available -- it is only
     * guaranteed to catch "trivial-reject" cases useful during the query optimization process
     */
    abstract boolean hasNoResults();

    /**
     * @return TRUE if this QueryOperation returns *all* hits.  Note that this API might return
     * FALSE in cases where this operation returns all results -- this API is only intended
     * to catch trivial cases and is useful during the query optimization process
     */
    abstract boolean hasAllResults();

    /**
     * Expand "is:local" and "is:remote" queries into in:(folder OR folder OR folder) as appropriate
     *
     * @param mbox
     * @return
     * @throws ServiceException
     */
    abstract QueryOperation expandLocalRemotePart(Mailbox mbox) throws ServiceException;

    /**
     * @param mbox
     * @return An Optimzed version of this query, or NULL if this query "should have no effect".
     * If NULL is returned, it is up to the caller to decide what to do (usually replace with a
     * NullQueryOperation -- however sometimes like within an Intersection you might just want
     * to remove the term)
     *
     * @throws ServiceException
     */
    abstract QueryOperation optimize(Mailbox mbox) throws ServiceException;

    /**
     * Called when optimize()ing a UNION or INTERSECTION -- see if two Ops can be expressed as one (e.g.
     * is:unread and in:inbox can both be expressed via one DBQueryOperation)
     *
     * @param other
     * @param union
     * @return the new operation that handles both us and the other constraint, or NULL if the ops could
     * not be combined.
     */
    protected abstract QueryOperation combineOps(QueryOperation other, boolean union);

    /**
     * Callback for {@link com.zimbra.cs.index.QueryOperation#depthFirstRecurse(RecurseCallback)}
     */
    interface RecurseCallback {
        void recurseCallback(QueryOperation op);
    }

    /**
     *
     * Walk the tree of QueryOperations in a depth-first manner calling {@link RecurseCallback}
     *
     * @param cb - The callback
     */
    protected abstract void depthFirstRecurse(RecurseCallback cb);

}
