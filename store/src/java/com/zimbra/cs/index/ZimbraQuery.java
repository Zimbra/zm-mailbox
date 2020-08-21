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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import com.google.common.base.Joiner;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.query.ConjQuery;
import com.zimbra.cs.index.query.InQuery;
import com.zimbra.cs.index.query.Query;
import com.zimbra.cs.index.query.Query.Modifier;
import com.zimbra.cs.index.query.SubQuery;
import com.zimbra.cs.index.query.parser.QueryParser;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.util.IOUtil;

/**
 * Executes a search query.
 * <p>
 * Flow is simple:
 * <ol>
 *  <li>Constructor
 *  <ol>
 *   <li>Parse the query string, turn it into a list of {@link Query}'s. This is done by the JavaCC-generated
 *   {@link QueryParser}.
 *   <li>Push "not's" down to the leaves, so that we never have to invert result sets. See the internal
 *   {@link ParseTree} class.
 *   <li>Generate a {@link QueryOperation} (which is usually a tree of {@link QueryOperation} objects) from the
 *   {@link ParseTree}, then optimize them {@link QueryOperation}s in preparation to run the query.
 *  </ol>
 *  <li>{@link #execute()} - Begin the search, get the {@link ZimbraQueryResults} iterator.
 * </ol>
 * TODO: move ParseTree classes out of this class
 *
 * @author tim
 * @author ysasaki
 */
public final class ZimbraQuery {

    private final List<Query> clauses;
    private QueryOperation operation;
    private final OperationContext octxt;
    private final SoapProtocol protocol;
    private final Mailbox mailbox;
    private final SearchParams params;
    private final ParseTree.Node parseTree;

    /**
     * ParseTree's job is to take the LIST of query terms (BaseQuery's) and build them
     * into a Tree structure of Things (return results) and Operators (AND and OR)
     *
     * Once a simple tree is built, then ParseTree "distributes the NOTs" down to the leaf
     * nodes: this is so we never have to do result-set inversions, which are prohibitively
     * expensive for nontrivial cases.
     */
    private static class ParseTree {
        enum Conjunction {
            AND, OR;
        }

        static abstract class Node {
            boolean bool = true;

            Node() {
            }

            void setBool(boolean value) {
                bool = value;
            };

            void invert() {
                bool = !bool;
            }

            abstract void pushNotsDown();
            abstract Node simplify();
            abstract QueryOperation compile(Mailbox mbox) throws ServiceException;
        }

        static class OperatorNode extends Node {
            private Conjunction conjunction;
            private List<Node> nodes = new ArrayList<Node>();

            OperatorNode(Conjunction conj) {
                conjunction = conj;
            }

            @Override
            void pushNotsDown() {
                if (!bool) { // ONLY push down if this is a "not"
                    bool = true;
                    switch (conjunction) {
                        case AND:
                            conjunction = Conjunction.OR;
                            break;
                        case OR:
                            conjunction = Conjunction.AND;
                            break;
                    }
                    for (Node node : nodes) {
                        node.invert();
                    }
                }
                for (Node node : nodes) {
                    node.pushNotsDown();
                }
            }

            @Override
            Node simplify() {
                boolean simplifyAgain;
                do {
                    simplifyAgain = false;
                    // first, simplify our sub-ops...
                    List<Node> simplified = new ArrayList<Node>();
                    for (Node node : nodes) {
                        simplified.add(node.simplify());
                    }
                    nodes = simplified;

                    // now, see if any of our subops can be trivially combined with us
                    List<Node> combined = new ArrayList<Node>();
                    for (Node node : nodes) {
                        if (node instanceof OperatorNode) {
                            OperatorNode opnode = (OperatorNode) node;
                            if (opnode.conjunction == conjunction && opnode.bool) {
                                simplifyAgain = true;
                                for (Node child : opnode.nodes) {
                                    combined.add(child);
                                }
                                continue;
                            }
                        }
                        combined.add(node);
                    }
                    nodes = combined;
                } while (simplifyAgain);

                if (nodes.isEmpty()) {
                    return null;
                } else if (nodes.size() == 1) {
                    Node node = nodes.get(0);
                    if (!bool) {
                        node.invert();
                    }
                    return node;
                }
                return this;
            }

            void add(Node subNode) {
                nodes.add(subNode);
            }

            public List<Node> getNodes() {
                return nodes;
            }

            @Override
            public String toString() {
                StringBuilder buff = bool ? new StringBuilder() : new StringBuilder(" NOT ");
                buff.append(conjunction).append('[');
                Joiner.on(',').appendTo(buff, nodes);
                buff.append("] ");
                return buff.toString();
            }

            @Override
            QueryOperation compile(Mailbox mbox) throws ServiceException {
                assert(bool); // we should have pushed the NOT's down the tree already
                switch (conjunction) {
                    case AND:
                        IntersectionQueryOperation intersect = new IntersectionQueryOperation();
                        for (Node node : nodes) {
                            QueryOperation op = node.compile(mbox);
                            assert(op != null);
                            intersect.addQueryOp(op);
                        }
                        return intersect;
                    case OR:
                        UnionQueryOperation union = new UnionQueryOperation();
                        for (Node node : nodes) {
                            QueryOperation op = node.compile(mbox);
                            assert(op != null);
                            union.add(op);
                        }
                        return union;
                    default:
                        throw new IllegalStateException(conjunction.name());
                }
            }

        }

        static class ThingNode extends Node {
            private final Query query;

            ThingNode(Query query) {
                this.query = query;
                this.bool = query.getBool();
            }

            @Override
            void pushNotsDown() {
            }

            @Override
            Node simplify() {
                return this;
            }

            public Query getQuery() {
                return query;
            }

            @Override
            public void invert() {
                if (query instanceof InQuery) {
                    if (query.getModifier() == Modifier.MINUS) {
                        query.setModifier(Modifier.NONE);
                    } else {
                        query.setModifier(Modifier.MINUS);
                    }
                } else {
                    super.invert();
                }
            }

            @Override
            public String toString() {
                StringBuilder buff = bool ? new StringBuilder() : new StringBuilder(" NOT ");
                buff.append(query);
                return buff.toString();
            }

            @Override
            QueryOperation compile(Mailbox mbox) throws ServiceException {
                return query.compile(mbox, bool);
            }
        }

        static Node build(List<Query> clauses) {
            OperatorNode top = new OperatorNode(Conjunction.OR);
            OperatorNode cur = new OperatorNode(Conjunction.AND);
            top.add(cur);

            for (Query query : clauses) {
                if (query instanceof ConjQuery) {
                    if (((ConjQuery) query).getConjunction() == ConjQuery.Conjunction.OR) {
                        cur = new OperatorNode(Conjunction.AND);
                        top.add(cur);
                    }
                } else if (query instanceof SubQuery) {
                    SubQuery sq = (SubQuery) query;
                    Node subTree = build(sq.getSubClauses());
                    subTree.setBool(sq.getModifier() != Query.Modifier.MINUS);
                    cur.add(subTree);
                } else {
                    cur.add(new ThingNode(query));
                }
            }

            return top;
        }
    }

    private static final class CountTextOperations implements QueryOperation.RecurseCallback {
        int num = 0;

        @Override
        public void recurseCallback(QueryOperation op) {
            if (op instanceof LuceneQueryOperation) {
                num++;
            }
        }
    }

    private static final class CountCombiningOperations implements QueryOperation.RecurseCallback {
        int num = 0;

        @Override
        public void recurseCallback(QueryOperation op) {
            if (op instanceof CombiningQueryOperation) {
                if (((CombiningQueryOperation)op).getNumSubOps() > 1) {
                    num++;
                }
            }
        }
    }

    /**
     * Returns true if this query has at least one text query, false if it's entirely DB query.
     */
    public boolean hasTextOperation() {
        for (Query query : clauses) {
            if (query.hasTextOperation()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if all clauses have a text component, false if any of the clauses is DB-only
     */
    private boolean allClausesHaveTextOperations() {
        for (Query query: clauses) {
            if (!(query instanceof ConjQuery) && !query.hasTextOperation()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns number of text parts of this query.
     */
    private int getTextOperationCount(QueryOperation op) {
        if (op == null) {
            return 0;
        }
        CountTextOperations count = new CountTextOperations();
        op.depthFirstRecurse(count);
        return count.num;
    }

    /**
     * @return number of non-trivial (num sub-ops > 1) Combining operations (joins/unions)
     */
    int countNontrivialCombiningOperations() {
        if (operation == null) {
            return 0;
        }
        CountCombiningOperations count =  new CountCombiningOperations();
        operation.depthFirstRecurse(count);
        return count.num;
    }

    /**
     * Parse the query string.
     */
    public ZimbraQuery(OperationContext octxt, SoapProtocol proto, Mailbox mbox, SearchParams params)
            throws ServiceException {
        this.octxt = octxt;
        this.protocol = proto;
        this.params = params;
        this.mailbox = mbox;

        // Parse the text using the JavaCC parser.
        try {
            QueryParser parser = new QueryParser(mbox);
            parser.setDefaultField(params.getDefaultField());
            parser.setTypes(params.getTypes());
            parser.setTimeZone(params.getTimeZone());
            parser.setLocale(params.getLocale());
            parser.setQuick(params.isQuick());
            clauses = parser.parse(params.getQueryString());

            if (parser.getSortBy() != null) {
                SortBy sort = SortBy.of(parser.getSortBy());
                if (sort == null) {
                    throw ServiceException.PARSE_ERROR("INVALID_SORTBY: " + sort, null);
                }
                params.setSortBy(sort);
            }
        } catch (Error e) {
            throw ServiceException.PARSE_ERROR("PARSER_ERROR", e);
        }

        ZimbraLog.search.debug("%s,types=%s,sort=%s", this, params.getTypes(), params.getSortBy());

        // Build a parse tree and push all the "NOT's" down to the bottom level.
        // This is because we cannot invert result sets.
        parseTree = ParseTree.build(clauses).simplify();
        parseTree.pushNotsDown();

        // Check sort compatibility.
        switch (params.getSortBy().getKey()) {
            case RCPT:
                // We don't store these in the index.
                if (hasTextOperation()) {
                    throw ServiceException.INVALID_REQUEST(
                            "Sort '" + params.getSortBy().name() + "' can't be used with text query.", null);
                }
                break;
            case RELEVANCE:
                //relevance sort can only be used if an index query is involved
                if (!allClausesHaveTextOperations()) {
                    SortBy sortBy = SortBy.DATE_DESC;
                    ZimbraLog.search.debug("relevance search not supported; switching to %s", sortBy.name());
                    params.setSortBy(sortBy);
                }
                break;
            default:
                break;
        }

        SearchParams.Cursor cursor = params.getCursor();
        if (cursor != null) {
            // Check cursor compatibility
            if (params.getCursor().isIncludeOffset() && hasTextOperation()) {
                throw ServiceException.INVALID_REQUEST("cursor.includeOffset can't be used with text query.", null);
            }
            // Supplement sortValue
            if (cursor.getSortValue() == null) {
                ZimbraLog.search.debug("Supplementing sortValue sort=%s,id=%s", params.getSortBy(), cursor.getItemId());
                try {
                    MailItem item = mailbox.getItemById(null, cursor.getItemId().getId(), MailItem.Type.UNKNOWN);
                    switch (params.getSortBy().getKey()) {
                        case NAME:
                            cursor.setSortValue(item.getName());
                            break;
                        case RCPT:
                            cursor.setSortValue(item.getSortRecipients());
                            break;
                        case SENDER:
                            cursor.setSortValue(item.getSortSender());
                            break;
                        case SIZE:
                            cursor.setSortValue(String.valueOf(item.getSize()));
                            break;
                        case SUBJECT:
                            cursor.setSortValue(item.getSortSubject());
                            break;
                        case PRIORITY:
                            cursor.setSortValue(LuceneFields.valueForPriority(item.getFlagBitmask()));
                            break;
                        case FLAG:
                            cursor.setSortValue(LuceneFields.valueForBooleanField(item.isFlagged()));
                            break;
                        case ATTACHMENT:
                            cursor.setSortValue(LuceneFields.valueForBooleanField(item.hasAttachment()));
                            break;
                        case ID:
                            cursor.setSortValue(String.valueOf(item.getId()));
                            break;
                        case UNREAD:
                            cursor.setSortValue(LuceneFields.valueForBooleanField(item.isUnread()));
                            break;
                        case DATE:
                        default:
                            cursor.setSortValue(String.valueOf(item.getDate()));
                            break;
                    }
                } catch (NoSuchItemException e) {
                    params.setCursor(null); // clear cursor
                }
            }
        }
    }

    private void compile() throws ServiceException {
        assert clauses != null;
        if (clauses.isEmpty()) {
            return;
        }

        // Convert list of Queries into list of QueryOperations, then optimize them.
        // this generates all of the query operations
        operation = parseTree.compile(mailbox);
        ZimbraLog.search.debug("OP=%s", operation);

        // expand the is:local and is:remote parts into in:(LIST)'s
        operation = operation.expandLocalRemotePart(mailbox);
        ZimbraLog.search.debug("AFTEREXP=%s", operation);

        // optimize the query down
        operation = operation.optimize(mailbox);
        ZimbraLog.search.debug("OPTIMIZED=%s", operation);
        if (operation == null || operation instanceof NoTermQueryOperation) {
            operation = new NoResultsQueryOperation();
            return;
        }

        // Use the OperationContext to update the set of visible referenced folders, local AND remote.
        Set<QueryTarget> targets = operation.getQueryTargets();
        assert(operation instanceof UnionQueryOperation || QueryTarget.getExplicitTargetCount(targets) <= 1);

        // easiest to treat the query two unions: one the LOCAL and one REMOTE parts
        UnionQueryOperation remoteOps = new UnionQueryOperation();
        UnionQueryOperation localOps = new UnionQueryOperation();

        if (operation instanceof UnionQueryOperation) {
            UnionQueryOperation union = (UnionQueryOperation) operation;
            // separate out the LOCAL vs REMOTE parts...
            for (QueryOperation op : union.operations) {
                Set<QueryTarget> set = op.getQueryTargets();

                // this assertion OK because we have already distributed multi-target query ops
                // during the optimize() step
                assert(QueryTarget.getExplicitTargetCount(set) <= 1);

                // the assertion above is critical: the code below all assumes
                // that we only have ONE target (ie we've already distributed if necessary)

                if (QueryTarget.hasExternalTarget(set)) {
                    remoteOps.add(op);
                } else {
                    localOps.add(op);
                }
            }
        } else {
            // single target: might be local, might be remote
            Set<QueryTarget> set = operation.getQueryTargets();
            // this assertion OK because we have already distributed multi-target query ops
            // during the optimize() step
            assert(QueryTarget.getExplicitTargetCount(set) <= 1);

            if (QueryTarget.hasExternalTarget(set)) {
                remoteOps.add(operation);
            } else {
                localOps.add(operation);
            }
        }

        // Handle the REMOTE side:
        if (!remoteOps.operations.isEmpty()) {
            // Since optimize() has already been run, we know that each of our ops
            // only has one target (or none).  Find those operations which have
            // an external target and wrap them in RemoteQueryOperations

            // iterate backwards so we can remove/add w/o screwing iteration
            for (int i = remoteOps.operations.size() - 1; i >= 0; i--) {
                QueryOperation op = remoteOps.operations.get(i);
                Set<QueryTarget> set = op.getQueryTargets();

                // this assertion OK because we have already distributed multi-target query ops
                // during the optimize() step
                assert(QueryTarget.getExplicitTargetCount(set) <= 1);

                // the assertion above is critical: the code below all assumes
                // that we only have ONE target (ie we've already distributed if necessary)

                if (QueryTarget.hasExternalTarget(set)) {
                    remoteOps.operations.remove(i);
                    boolean foundOne = false;
                    // find a remoteOp to add this one to
                    for (QueryOperation remoteOp : remoteOps.operations) {
                        if (remoteOp instanceof RemoteQueryOperation) {
                            if (((RemoteQueryOperation) remoteOp).tryAddOredOperation(op)) {
                                foundOne = true;
                                break;
                            }
                        }
                    }
                    if (!foundOne) {
                        RemoteQueryOperation remoteOp = new RemoteQueryOperation();
                        remoteOp.tryAddOredOperation(op);
                        remoteOps.operations.add(i, remoteOp);
                    }
                }
            }

            // ...we need to call setup on every RemoteQueryOperation we end up with...
            for (QueryOperation remoteOp : remoteOps.operations) {
                assert(remoteOp instanceof RemoteQueryOperation);
                try {
                    ((RemoteQueryOperation) remoteOp).setup(protocol, octxt.getAuthToken(), params);
                } catch (Exception e) {
                    ZimbraLog.search.info("Ignoring %s during RemoteQuery generation for %s", e, remoteOps);
                }
            }
        }

        // For the LOCAL parts of the query, do permission checks, do trash/spam exclusion
        if (!localOps.operations.isEmpty()) {
            ZimbraLog.search.debug("LOCAL_IN=%s", localOps);

            Account authAcct = octxt != null ? octxt.getAuthenticatedUser() : mailbox.getAccount();

            // Now, for all the LOCAL PARTS of the query, add the trash/spam exclusion part
            boolean includeTrash = false;
            boolean includeSpam = false;
            if (params.inDumpster()) {
                // Always include trash and spam for dumpster searches.  Excluding spam is a client side choice.
                includeTrash = true;
                includeSpam = true;
                if (!mailbox.hasFullAdminAccess(octxt)) { // If the requester is not an admin, limit to recent date range.
                    long now = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();
                    long mdate = now - authAcct.getDumpsterUserVisibleAge();
                    IntersectionQueryOperation and = new IntersectionQueryOperation();
                    DBQueryOperation db = new DBQueryOperation();
                    db.addMDateRange(mdate, false, -1L, false, true);
                    and.addQueryOp((QueryOperation) localOps.clone());
                    and.addQueryOp(db);
                    localOps.operations.clear();
                    localOps.operations.add(and);
                }
            } else {
                includeTrash = authAcct.isPrefIncludeTrashInSearch();;
                includeSpam = authAcct.isPrefIncludeSpamInSearch();
            }
            if (!includeTrash || !includeSpam) {
                // First check that we aren't specifically looking for items in one of these.
                // For instance, in ZWC, if "Include Shared Items" is selected, the Trash list may currently use a
                // search similar to : 'in:"trash" (inid:565 OR is:local)'
                // where 565 is the folder ID for a shared folder.  We don't want to end up doing a search for items
                // that are both in "trash" and NOT in "trash"...
                    if (parseTreeIncludesFolder(parseTree, Mailbox.ID_FOLDER_SPAM)) {
                        includeSpam = true;
                    }
                    if (parseTreeIncludesFolder(parseTree, Mailbox.ID_FOLDER_TRASH)) {
                        includeTrash = true;
                    }
            }
            if (!includeTrash || !includeSpam) {
                List<QueryOperation> toAdd = new ArrayList<QueryOperation>();
                for (Iterator<QueryOperation> iter = localOps.operations.iterator(); iter.hasNext();) {
                    QueryOperation cur = iter.next();
                    if (!cur.hasSpamTrashSetting()) {
                        QueryOperation newOp = cur.ensureSpamTrashSetting(mailbox, includeTrash, includeSpam);
                        if (newOp != cur) {
                            iter.remove();
                            toAdd.add(newOp);
                        }
                    }
                }
                localOps.operations.addAll(toAdd);
                ZimbraLog.search.debug("LOCAL_AFTER_TRASH/SPAM/DUMPSTER=%s", localOps);
            }

            // Check to see if we need to filter out private appointment data
            boolean allowPrivateAccess = true;
            if (octxt != null) {
                allowPrivateAccess = AccessManager.getInstance().allowPrivateAccess(octxt.getAuthenticatedUser(),
                        mailbox.getAccount(), octxt.isUsingAdminPrivileges());
            }

            //
            // bug 28892 - ACL.RIGHT_PRIVATE support:
            //
            // Basically, if ACL.RIGHT_PRIVATE is set somewhere, and if we're excluding private items from
            // search, then we need to run the query twice -- once over the whole mailbox with
            // private items excluded and then UNION it with a second run, this time only in the
            // RIGHT_PRIVATE enabled folders, with private items enabled.
            //
            UnionQueryOperation clonedLocal = null;
            Set<Folder> hasFolderRightPrivateSet = new HashSet<Folder>();

            // ...don't do any of this if they aren't asking for a calendar type...
            Set<MailItem.Type> types = params.getTypes();
            boolean hasCalendarType =
                types.contains(MailItem.Type.APPOINTMENT) || types.contains(MailItem.Type.TASK);
            if (hasCalendarType && !allowPrivateAccess && getTextOperationCount(localOps) > 0) {
                // the searcher is NOT allowed to see private items globally....lets check
                // to see if there are any individual folders that they DO have rights to...
                // if there are any, then we'll need to run special searches in those
                // folders
                Set<Folder> allVisibleFolders = mailbox.getVisibleFolders(octxt);
                if (allVisibleFolders == null) {
                    allVisibleFolders = new HashSet<Folder>();
                    allVisibleFolders.addAll(mailbox.getFolderList(octxt, SortBy.NONE));
                }
                for (Folder f : allVisibleFolders) {
                    if (f.getType() == MailItem.Type.FOLDER &&
                            CalendarItem.allowPrivateAccess(f, authAcct, false)) {
                        hasFolderRightPrivateSet.add(f);
                    }
                }
                if (!hasFolderRightPrivateSet.isEmpty()) {
                    clonedLocal = (UnionQueryOperation)localOps.clone();
                }
            }

            Set<Folder> visibleFolders = mailbox.getVisibleFolders(octxt);

            localOps = handleLocalPermissionChecks(localOps, visibleFolders, allowPrivateAccess);

            ZimbraLog.search.debug("LOCAL_AFTER_PERM_CHECKS=%s", localOps);

            if (!hasFolderRightPrivateSet.isEmpty()) {
                ZimbraLog.search.debug("CLONED_LOCAL_BEFORE_PERM=%s", clonedLocal);

                // now we're going to setup the clonedLocal tree
                // to run with private access ALLOWED, over the set of folders
                // that have RIGHT_PRIVATE (note that we build this list from the visible
                // folder list, so we are
                clonedLocal = handleLocalPermissionChecks(clonedLocal, hasFolderRightPrivateSet, true);

                ZimbraLog.search.debug("CLONED_LOCAL_AFTER_PERM=%s", clonedLocal);

                // clonedLocal should only have the single INTERSECT in it
                assert(clonedLocal.operations.size() == 1);

                QueryOperation optimizedClonedLocal = clonedLocal.optimize(mailbox);
                ZimbraLog.search.debug("CLONED_LOCAL_AFTER_OPTIMIZE=%s", optimizedClonedLocal);

                UnionQueryOperation withPrivateExcluded = localOps;
                localOps = new UnionQueryOperation();
                localOps.add(withPrivateExcluded);
                localOps.add(optimizedClonedLocal);

                ZimbraLog.search.debug("LOCAL_WITH_CLONED=%s", localOps);

                //
                // we should end up with:
                //
                // localOps =
                //    UNION(withPrivateExcluded,
                //          UNION(INTERSECT(clonedLocal,
                //                          UNION(hasFolderRightPrivateList)
                //                         )
                //                )
                //          )
                //
            }
            localOps = removeLocalQueriesForPseudoTags(localOps);
        }

        UnionQueryOperation union = new UnionQueryOperation();
        union.add(localOps);
        union.add(remoteOps);
        ZimbraLog.search.debug("BEFORE_FINAL_OPT=%s", union);
        operation = union.optimize(mailbox);
        operation.authMailbox = MailboxManager.getInstance().getMailboxByAccountId(octxt.getAuthenticatedUser().getId());

        ZimbraLog.search.debug("COMPILED=%s", operation);
    }

    private boolean parseTreeIncludesFolder(ZimbraQuery.ParseTree.Node node, int folderId) {
        if (node instanceof ParseTree.OperatorNode) {
            for (ParseTree.Node subNode: ((ParseTree.OperatorNode) node).getNodes()) {
                if (parseTreeIncludesFolder(subNode, folderId)) {
                    return true;
                }
            }
        } else if (node instanceof ParseTree.ThingNode) {
            Query query = ((ParseTree.ThingNode) node).getQuery();
            if (query instanceof InQuery) {
                Folder folder = ((InQuery) query).getFolder();
                return folder != null? folder.getId() == folderId && query.getModifier() != Modifier.MINUS: false;
            } else {
                return false;
            }
        }
        return false;
    }

    public SearchParams getParams() {
        return params;
    }

    /**
     * Runs the search and gets an open result set.
     *
     * WARNING: You **MUST** call {@link ZimbraQueryResults#close()} when you are done with them!
     */
    public ZimbraQueryResults execute() throws ServiceException {
        compile();

        Set<QueryTarget> targets = operation.getQueryTargets();
        assert(operation instanceof UnionQueryOperation || QueryTarget.getExplicitTargetCount(targets) <= 1);
        assert(targets.size() >1 || !QueryTarget.hasExternalTarget(targets) || operation instanceof RemoteQueryOperation);

        ZimbraLog.search.debug("OPERATION: %s", operation);

        int chunkSize = (int) Math.min((long) params.getOffset() + (long) params.getLimit(), 1000L);
        ZimbraQueryResults results = null;
        try {
            results = operation.run(mailbox, params, chunkSize);
            if (((!params.getIncludeTagDeleted() || !params.getIncludeTagMuted()) && params.getFetchMode() != SearchParams.Fetch.IDS)
                    || params.getAllowableTaskStatuses() != null) {
                // we have to do some filtering of the result set
                FilteredQueryResults filtered = new FilteredQueryResults(results, params);

                if (!params.getIncludeTagDeleted()) {
                    filtered.setFilterTagDeleted(true);
                }
                if (!params.getIncludeTagMuted()) {
                    filtered.setFilterTagMuted(true);
                }
                if (params.getAllowableTaskStatuses() != null) {
                    filtered.setAllowedTaskStatuses(params.getAllowableTaskStatuses());
                }
                results = filtered;
            }
            return results;
        } catch (RuntimeException e) {
            IOUtil.closeQuietly(results);
            IOUtil.closeQuietly(operation);
            throw e;
        }
    }

    /**
     * Callback -- adds a "-l.field:_calendaritemclass:private" term to all Lucene search parts: to exclude
     *             text data from searches in private appointments
     */
    private static final class excludePrivateCalendarItems implements QueryOperation.RecurseCallback {
        @Override
        public void recurseCallback(QueryOperation op) {
            if (op instanceof LuceneQueryOperation) {
                ((LuceneQueryOperation) op).addAndedClause(new TermQuery(new Term(
                        LuceneFields.L_FIELD, CalendarItem.INDEX_FIELD_ITEM_CLASS + ":private")), false);
            }
        }
    }

    /**
     * For the local targets, prune out intersection clauses where one of the ANDed operations is a search
     * for a non-existent tag.
     * Scenario:
     *  1. ZWC user clicks on a tag configured for their mailbox which is NOT configured for the mailbox associated
     *     with one of their shared.
     *  2. SOAP query string is "tag:\"Orange\" (inid:257 OR inid:259 OR is:local)"
     *  3. As part of processing that query, another SOAP query is fired at the remote mailbox similar to:
     *     "(((((INID:\"7b4c7d75-68e4-4c19-a4c6-e6e891b1d065:257\" ) OR
     *      (INID:\"7b4c7d75-68e4-4c19-a4c6-e6e891b1d065:259\" ))) AND (TAG:(Orange) )))"
     *  4. If the TAG "Orange" is not defined, that query should return no results.  Prior to this fix for Bug 79576,
     *     this query got expanded into a fairly complex query which actually returned some items.  By applying
     *     this optimization, that no longer happens.
     * WARNING:  Only use for local targets.
     */
    private UnionQueryOperation removeLocalQueriesForPseudoTags(UnionQueryOperation union) {
        boolean changesMade = false;
        for (int i = union.operations.size() - 1; i >= 0; i--) {
            QueryOperation op = union.operations.get(i);
            boolean replaceOp = false;
            if (op instanceof IntersectionQueryOperation) {
                IntersectionQueryOperation intersection = (IntersectionQueryOperation) op;
                for (QueryOperation andedOp : intersection.operations) {
                    if (andedOp instanceof DBQueryOperation) {
                        DBQueryOperation dbqOp = (DBQueryOperation) andedOp;
                        replaceOp = dbqOp.isSearchForNonexistentLocalTag(mailbox);
                        if (replaceOp) {
                            changesMade = true;
                            union.operations.remove(i);
                            union.operations.add(i, new NoResultsQueryOperation());
                        }
                    }
                }
            }
        }
        if (changesMade) {
            ZimbraLog.search.debug("LOCAL_AFTER_PRUNE_NONEXISTENT_TAG_SEARCH:%s", union);
        }
        return union;
    }
    /**
     * For the local targets:
     *   - exclude all the not-visible folders from the query
     *   - look at all the text-operations and figure out if private appointments need to be excluded
     */
    private static UnionQueryOperation handleLocalPermissionChecks(UnionQueryOperation union,
            Set<Folder> visibleFolders, boolean allowPrivateAccess) {

        // Since optimize() has already been run, we know that each of our ops only has one target (or none). Find those
        // operations which have an external target and wrap them in RemoteQueryOperations.
        // iterate backwards so we can remove/add w/o screwing iteration
        for (int i = union.operations.size() - 1; i >= 0; i--) {
            QueryOperation op = union.operations.get(i);
            Set<QueryTarget> targets = op.getQueryTargets();

            // this assertion is OK because we have already distributed multi-target query ops
            // during the optimize() step
            assert(QueryTarget.getExplicitTargetCount(targets) <= 1);
            // the assertion above is critical: the code below all assumes
            // that we only have ONE target (ie we've already distributed if necessary)

            if (!QueryTarget.hasExternalTarget(targets)) {
                // local target
                if (!allowPrivateAccess)
                    op.depthFirstRecurse(new excludePrivateCalendarItems());

                if (visibleFolders != null) {
                    if (visibleFolders.isEmpty()) {
                        union.operations.remove(i);
                        ZimbraLog.search.debug("Query changed to NULL_QUERY_OPERATION, no visible folders");
                        union.operations.add(i, new NoResultsQueryOperation());
                    } else {
                        union.operations.remove(i);

                        // build a "and (in:visible1 or in:visible2 or in:visible3...)" query tree here!
                        IntersectionQueryOperation intersect = new IntersectionQueryOperation();
                        intersect.addQueryOp(op);

                        UnionQueryOperation newUnion = new UnionQueryOperation();
                        intersect.addQueryOp(newUnion);

                        //if one or more target folders are specified, use those which are visible.
                        Set<Folder> targetFolders = null;
                        if (op instanceof DBQueryOperation) {
                            DBQueryOperation dbOp = (DBQueryOperation) op;
                            targetFolders = dbOp.getTargetFolders();
                        }

                        for (Folder folder : visibleFolders) {
                            // exclude remote folders
                            if (!(folder instanceof Mountpoint) || ((Mountpoint) folder).isLocal()) {
                                if (targetFolders != null && targetFolders.size() > 0 && !targetFolders.contains(folder)) {
                                    continue; //don't bother searching other visible folders if the query asked for specific folders...
                                }
                                DBQueryOperation newOp = new DBQueryOperation();
                                newUnion.add(newOp);
                                newOp.addInFolder(folder, true);
                            }
                        }

                        union.operations.add(i, intersect);
                    }
                }
            }
        }

        return union;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("ZQ: ");
        for (Query clause : clauses) {
            clause.toString(out);
        }
        return out.toString();
    }

    public String toQueryString() throws ServiceException {
        if (operation == null) {
            compile();
        }
        return operation.toQueryString();
    }

    public String toSanitizedtring() throws ServiceException {
        StringBuilder out = new StringBuilder();
        for (Query clause : clauses) {
            clause.toSanitizedString(out);
        }
        return out.toString();
    }

}
