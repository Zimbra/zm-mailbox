/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.index;

import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * A {@link QueryOperation} is a part of a Search request -- there are
 * potentially multiple query operations in a search. {@link QueryOperation}
 * return {@link ZimbraQueryResultsImpl} sets -- which can be iterated over to
 * get {@link ZimbraHit} objects.
 * <p>
 * The difference between a {@link QueryOperation} and a simple
 * {@link ZimbraQueryResultsImpl} set is that a {@link QueryOperation} knows how
 * to Optimize and Execute itself -- whereas a {@link QueryResult} set is just
 * a set of results and can only be iterated.
 *
 * @since Oct 15, 2004
 */
public abstract class QueryOperation implements Cloneable, ZimbraQueryResults {
    static final int MIN_CHUNK_SIZE = 26;
    static final int MAX_CHUNK_SIZE = 5000;

    protected QueryContext context;
    protected Mailbox authMailbox;

    @Override
    public SortBy getSortBy() {
        return context.getParams().getSortBy();
    }

    @Override
    public boolean isPreSorted() {
        return false;
    }

    /**
     * @return A representation of this operation as a parsable query string
     */
    abstract String toQueryString();

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

    /**
     * Executes the query.
     *
     * @param mbox mailbox to search
     * @param params search parameters
     * @param chunkSize A hint to the query operation telling it what size to
     *  chunk data in. Higher numbers can be more efficient if you are using a
     *  lot of results, but have more overhead.
     * @return search results
     * @throws ServiceException if an error occurred
     */
    final ZimbraQueryResults run(Mailbox mbox, SearchParams params, int chunkSize) throws ServiceException {
        mIsToplevelQueryOp = true;

        chunkSize++; // one extra for checking the "more" flag at the end of the results

        if (chunkSize < MIN_CHUNK_SIZE) {
            chunkSize = MIN_CHUNK_SIZE;
        } else if (chunkSize > MAX_CHUNK_SIZE) {
            chunkSize = MAX_CHUNK_SIZE;
        }

        Grouping retType = Grouping.ITEM; //MailboxIndex.SEARCH_RETURN_DOCUMENTS;
        Set<MailItem.Type> types = params.getTypes();
        if (types.contains(MailItem.Type.CONVERSATION)) {
            retType = Grouping.CONVERSATION; //MailboxIndex.SEARCH_RETURN_CONVERSATIONS;
        } else if (types.contains(MailItem.Type.MESSAGE)) {
            retType = Grouping.MESSAGE; // MailboxIndex.SEARCH_RETURN_MESSAGES;
        }

        // set me to TRUE if you're returning Conversations or something which could benefit from preloading
        boolean preloadOuterResults = false;

        int outerChunkSize = chunkSize;

        boolean usePreloadingGrouper = true;

        // don't preload if all we want is IDs!
        if (params.getFetchMode() == SearchParams.Fetch.IDS) {
            usePreloadingGrouper = false;
        }

        ZimbraQueryResultsImpl results = null;
        switch (retType) {
            case CONVERSATION:
                if (params.getPrefetch() && usePreloadingGrouper) {
                    chunkSize+= 2; // one for the ConvQueryResults, one for the Grouper
                    results = new ConvQueryResults(new ItemPreloadingGrouper(this, chunkSize, mbox,
                            params.inDumpster()), types, params.getSortBy(), params.getFetchMode());
                    chunkSize *= MESSAGES_PER_CONV_ESTIMATE; // guess 2 msgs per conv
                } else {
                    chunkSize++; // one for the ConvQueryResults
                    results = new ConvQueryResults(this, types, params.getSortBy(), params.getFetchMode());
                    chunkSize *= MESSAGES_PER_CONV_ESTIMATE;
                }
                preloadOuterResults = true;
                break;
            case MESSAGE:
                if (params.getPrefetch()  && usePreloadingGrouper) {
                    chunkSize += 2; // one for the MsgQueryResults, one for the Grouper
                    results = new MsgQueryResults(new ItemPreloadingGrouper(this, chunkSize, mbox, params.inDumpster()),
                            types, params.getSortBy(), params.getFetchMode());
                } else {
                    chunkSize++; // one for the MsgQueryResults
                    results = new MsgQueryResults(this, types, params.getSortBy(), params.getFetchMode());
                }
                break;
            case ITEM:
                if (params.getPrefetch() && usePreloadingGrouper) {
                    chunkSize++; // one for the grouper
                    results = new UngroupedQueryResults(new ItemPreloadingGrouper(this, chunkSize, mbox,
                            params.inDumpster()), types, params.getSortBy(), params.getFetchMode());
                } else {
                    results = new UngroupedQueryResults(this, types, params.getSortBy(), params.getFetchMode());
                }
                break;
            default:
                assert(false);
        }

        begin(new QueryContext(mbox, results, params, chunkSize));

        if (usePreloadingGrouper && preloadOuterResults && params.getPrefetch()) {
            return new ItemPreloadingGrouper(results, outerChunkSize, mbox, params.inDumpster());
        } else {
            return results;
        }
    }

    @Override
    public boolean hasNext() throws ServiceException {
        return peekNext() != null;
    }

    /**
     * Begins query execution. It is allowed to grab and hold resources, which
     * are then released via {@link #doneWithSearchResults()}.
     * <p>
     * IMPORTANT: {@link #begin(QueryContext)} and {@link #doneWithSearchResults()}
     * must always be called in a pair. That is, if you call {@link #begin(QueryContext)},
     * you MUST call {@link #doneWithSearchResults()}.
     *
     * @param ctx various context parameters
     * @throws ServiceException if an error occurred
     */
    protected abstract void begin(QueryContext ctx) throws ServiceException;

    @Override
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

    abstract Set<QueryTarget> getQueryTargets();

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

    protected static final class QueryContext {
        private final Mailbox mailbox;
        private final ZimbraQueryResultsImpl results;
        private final SearchParams params;
        private final int chunkSize;

        QueryContext(Mailbox mbox, ZimbraQueryResultsImpl results,
                SearchParams params, int chunkSize) {
            this.mailbox = mbox;
            this.results = results;
            this.params = params;
            this.chunkSize = chunkSize;
        }

        Mailbox getMailbox() {
            return mailbox;
        }

        ZimbraQueryResultsImpl getResults() {
            return results;
        }

        SearchParams getParams() {
            return params;
        }

        int getChunkSize() {
            return chunkSize;
        }
    }

}
