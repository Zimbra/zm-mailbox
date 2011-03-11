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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.index.query.ConjQuery;
import com.zimbra.cs.index.query.Query;
import com.zimbra.cs.index.query.SubQuery;
import com.zimbra.cs.index.query.parser.QueryParser;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox.SearchResultMode;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.SoapProtocol;

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

    private List<Query> clauses;
    private QueryOperation operation;
    private final Mailbox mailbox;
    private final SearchParams params;
    private int chunkSize;

    /**
     * ParseTree's job is to take the LIST of query terms (BaseQuery's) and build them
     * into a Tree structure of Things (return results) and Operators (AND and OR)
     *
     * Once a simple tree is built, then ParseTree "distributes the NOTs" down to the leaf
     * nodes: this is so we never have to do result-set inversions, which are prohibitively
     * expensive for nontrivial cases.
     */
    private static class ParseTree {
        private static final int STATE_AND = 1;
        private static final int STATE_OR = 2;

        static abstract class Node {
            boolean mTruthFlag = true;

            Node() {
            }

            void setTruth(boolean truth) {
                mTruthFlag = truth;
            };

            void invertTruth() {
                mTruthFlag = !mTruthFlag;
            }

            abstract void pushNotsDown();
            abstract Node simplify();
            abstract QueryOperation getQueryOperation();
        }

        static class OperatorNode extends Node {
            private int mKind;
            private boolean mTruthFlag = true;
            private List<Node> mNodes = new ArrayList<Node>();

            OperatorNode(int kind) {
                mKind = kind;
            }

            @Override
            void setTruth(boolean truth) {
                mTruthFlag = truth;
            };

            @Override
            void invertTruth() {
                mTruthFlag = !mTruthFlag;
            }

            @Override
            void pushNotsDown() {
                if (!mTruthFlag) { // ONLY push down if this is a "not"
                    mTruthFlag = !mTruthFlag;

                    if (mKind == STATE_AND) {
                        mKind = STATE_OR;
                    } else {
                        mKind = STATE_AND;
                    }
                    for (Node n : mNodes) {
                        n.invertTruth();
                    }
                }
                assert(mTruthFlag);
                for (Node n : mNodes) {
                    n.pushNotsDown();
                }
            }

            @Override
            Node simplify() {
                boolean simplifyAgain;
                do {
                    simplifyAgain = false;
                    // first, simplify our sub-ops...
                    List<Node> newNodes = new ArrayList<Node>();
                    for (Node n : mNodes) {
                        newNodes.add(n.simplify());
                    }
                    mNodes = newNodes;

                    // now, see if any of our subops can be trivially combined with us
                    newNodes = new ArrayList<Node>();
                    for (Node n : mNodes) {
                        boolean addIt = true;

                        if (n instanceof OperatorNode) {
                            OperatorNode opn = (OperatorNode)n;
                            if (opn.mKind == mKind && opn.mTruthFlag == true) {
                                addIt = false;
                                simplifyAgain = true;
                                for (Node opNode: opn.mNodes) {
                                    newNodes.add(opNode);
                                }
                            }
                        }
                        if (addIt) {
                            newNodes.add(n);
                        }
                    }
                    mNodes = newNodes;
                } while (simplifyAgain);

                if (mNodes.size() == 0) {
                    return null;
                }
                if (mNodes.size() == 1) {
                    Node n = mNodes.get(0);
                    if (!mTruthFlag) {
                        n.invertTruth();
                    }
                    return n;
                }
                return this;
            }

            void add(Node subNode) {
                mNodes.add(subNode);
            }

            @Override
            public String toString() {
                StringBuilder buff = mTruthFlag ?
                        new StringBuilder() : new StringBuilder(" NOT ");

                buff.append(mKind == STATE_AND ? " AND[" : " OR(");

                for (Node node : mNodes) {
                    buff.append(node.toString());
                    buff.append(", ");
                }
                buff.append(mKind == STATE_AND ? "] " : ") ");
                return buff.toString();
            }

            @Override
            QueryOperation getQueryOperation() {
                assert(mTruthFlag == true); // we should have pushed the NOT's down the tree already
                if (mKind == STATE_AND) {
                    IntersectionQueryOperation intersect = new IntersectionQueryOperation();

                    for (Node n : mNodes) {
                        QueryOperation op = n.getQueryOperation();
                        assert(op!=null);
                        intersect.addQueryOp(op);
                    }

                    return intersect;
                } else {

                    UnionQueryOperation union = new UnionQueryOperation();

                    for (Node n : mNodes) {
                        QueryOperation op = n.getQueryOperation();
                        assert(op != null);
                        union.add(op);
                    }
                    return union;
                }
            }

        }

        static class ThingNode extends Node {
            private Query mThing;

            ThingNode(Query thing) {
                mThing = thing;
                mTruthFlag = thing.getBool();
            }

            @Override
            void invertTruth() {
                mTruthFlag = !mTruthFlag;
            }

            @Override
            void pushNotsDown() {
            }

            @Override
            Node simplify() {
                return this;
            }

            @Override
            public String toString() {
                StringBuilder buff = mTruthFlag ?
                        new StringBuilder() : new StringBuilder(" NOT ");
                buff.append(mThing.toString());
                return buff.toString();
            }

            @Override
            QueryOperation getQueryOperation() {
                return mThing.getQueryOperation(mTruthFlag);
            }
        }

        static Node build(List<Query> clauses) {
            OperatorNode top = new OperatorNode(STATE_OR);
            OperatorNode cur = new OperatorNode(STATE_AND);
            top.add(cur);

            for (Query q : clauses) {
                if (q instanceof ConjQuery) {
                    if (((ConjQuery) q).getConjunction() == ConjQuery.Conjunction.OR) {
                        cur = new OperatorNode(STATE_AND);
                        top.add(cur);
                    }
                } else {
                    if (q instanceof SubQuery) {
                        SubQuery sq = (SubQuery) q;
                        Node subTree = build(sq.getSubClauses());
                        subTree.setTruth(sq.getModifier() != Query.Modifier.MINUS);
                        cur.add(subTree);
                    } else {
                        cur.add(new ThingNode(q));
                    }
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
     * Returns the number of text parts of this query.
     */
    public int countTextOperations() {
        if (operation == null) {
            return 0;
        }
        CountTextOperations count = new CountTextOperations();
        operation.depthFirstRecurse(count);
        return count.num;
    }

    /**
     * Returns number of text parts of this query.
     */
    private static int countTextOperations(QueryOperation op) {
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
     * Take the specified query string and build an optimized query. Do not
     * execute the query, however.
     *
     * @param mbox
     * @param params
     * @throws ServiceException
     */
    public ZimbraQuery(OperationContext octxt, SoapProtocol proto, Mailbox mbox, SearchParams params)
            throws ServiceException {

        this.params = params;
        this.mailbox = mbox;
        long chunkSize = (long) params.getOffset() + (long) params.getLimit();
        if (chunkSize > 1000) {
            chunkSize = 1000;
        } else {
            chunkSize = (int) chunkSize;
        }

        Analyzer analyzer = null;
        MailboxIndex index = mbox.index.getMailboxIndex();

        // Step 1: parse the text using the JavaCC parser
        try {
            if (index != null) {
                index.initAnalyzer(mbox);
                analyzer = index.getAnalyzer();
            } else {
                analyzer = ZimbraAnalyzer.getInstance();
            }
            QueryParser parser = new QueryParser(mbox, analyzer);
            parser.setDefaultField(params.getDefaultField());
            parser.setTypes(params.getTypes());
            parser.setTimeZone(params.getTimeZone());
            parser.setLocale(params.getLocale());
            clauses = parser.parse(params.getQueryStr());

            String sortByStr = parser.getSortBy();
            if (sortByStr != null) {
                SortBy sortBy = SortBy.lookup(sortByStr);
                if (sortBy == null) {
                    throw ServiceException.PARSE_ERROR("INVALID_SORTBY: " + sortByStr, null);
                }
                params.setSortBy(sortBy);
            }
        } catch (Error e) {
            throw ServiceException.PARSE_ERROR("PARSER_ERROR", e);
        }

        ZimbraLog.search.debug("%s,types=%s,sort=%s", this, params.getTypes(), params.getSortBy());

        // Step 2: build a parse tree and push all the "NOT's" down to the bottom level.
        // This is because we cannot invert result sets
        ParseTree.Node parseTree = ParseTree.build(clauses);
        parseTree = parseTree.simplify();
        parseTree.pushNotsDown();

        // Step 3: Convert list of BaseQueries into list of QueryOperations, then Optimize the Ops
        if (clauses.size() > 0) {
            // this generates all of the query operations
            operation = parseTree.getQueryOperation();

            ZimbraLog.search.debug("OP=%s", operation);

            // expand the is:local and is:remote parts into in:(LIST)'s
            operation = operation.expandLocalRemotePart(mbox);
            ZimbraLog.search.debug("AFTEREXP=%s", operation);

            // optimize the query down
            operation = operation.optimize(mailbox);
            if (operation == null) {
                operation = new NoResultsQueryOperation();
            }
            ZimbraLog.search.debug("OPTIMIZED=%s", operation);
        }

        // STEP 4: use the OperationContext to update the set of visible referenced folders, local AND remote
        if (operation != null) {
            QueryTargetSet queryTargets = operation.getQueryTargets();
            assert(operation instanceof UnionQueryOperation || queryTargets.countExplicitTargets() <= 1);

            // easiest to treat the query two unions: one the LOCAL and one REMOTE parts
            UnionQueryOperation remoteOps = new UnionQueryOperation();
            UnionQueryOperation localOps = new UnionQueryOperation();

            if (operation instanceof UnionQueryOperation) {
                UnionQueryOperation union = (UnionQueryOperation) operation;
                // separate out the LOCAL vs REMOTE parts...
                for (QueryOperation op : union.mQueryOperations) {
                    QueryTargetSet targets = op.getQueryTargets();

                    // this assertion OK because we have already distributed multi-target query ops
                    // during the optimize() step
                    assert(targets.countExplicitTargets() <= 1);

                    // the assertion above is critical: the code below all assumes
                    // that we only have ONE target (ie we've already distributed if necessary)

                    if (targets.hasExternalTargets()) {
                        remoteOps.add(op);
                    } else {
                        localOps.add(op);
                    }
                }
            } else {
                // single target: might be local, might be remote

                QueryTargetSet targets = operation.getQueryTargets();
                // this assertion OK because we have already distributed multi-target query ops
                // during the optimize() step
                assert(targets.countExplicitTargets() <= 1);

                if (targets.hasExternalTargets()) {
                    remoteOps.add(operation);
                } else {
                    localOps.add(operation);
                }
            }

            // Handle the REMOTE side:
            if (!remoteOps.mQueryOperations.isEmpty()) {
                // Since optimize() has already been run, we know that each of our ops
                // only has one target (or none).  Find those operations which have
                // an external target and wrap them in RemoteQueryOperations

                // iterate backwards so we can remove/add w/o screwing iteration
                for (int i = remoteOps.mQueryOperations.size()-1; i >= 0; i--) {
                    QueryOperation op = remoteOps.mQueryOperations.get(i);

                    QueryTargetSet targets = op.getQueryTargets();

                    // this assertion OK because we have already distributed multi-target query ops
                    // during the optimize() step
                    assert(targets.countExplicitTargets() <= 1);

                    // the assertion above is critical: the code below all assumes
                    // that we only have ONE target (ie we've already distributed if necessary)

                    if (targets.hasExternalTargets()) {
                        remoteOps.mQueryOperations.remove(i);
                        boolean foundOne = false;
                        // find a remoteOp to add this one to
                        for (QueryOperation remoteOp : remoteOps.mQueryOperations) {
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
                            remoteOps.mQueryOperations.add(i, remoteOp);
                        }
                    }
                }

                // ...we need to call setup on every RemoteQueryOperation we end up with...
                for (QueryOperation remoteOp : remoteOps.mQueryOperations) {
                    assert(remoteOp instanceof RemoteQueryOperation);
                    try {
                        ((RemoteQueryOperation) remoteOp).setup(proto, octxt.getAuthToken(), params);
                    } catch (Exception e) {
                        ZimbraLog.search.info("Ignoring " + e + " during RemoteQuery generation for " + remoteOps);
                    }
                }
            }

            // For the LOCAL parts of the query, do permission checks, do trash/spam exclusion
            if (!localOps.mQueryOperations.isEmpty()) {
                ZimbraLog.search.debug("LOCAL_IN=%s", localOps);

                Account authAcct = null;
                if (octxt != null) {
                    authAcct = octxt.getAuthenticatedUser();
                } else {
                    authAcct = mbox.getAccount();
                }

                // Now, for all the LOCAL PARTS of the query, add the trash/spam exclusion part
                boolean includeTrash = false;
                boolean includeSpam = false;
                if (authAcct != null) {
                    includeTrash = authAcct.getBooleanAttr(Provisioning.A_zimbraPrefIncludeTrashInSearch, false);
                    includeSpam = authAcct.getBooleanAttr(Provisioning.A_zimbraPrefIncludeSpamInSearch, false);
                }
                if (!includeTrash || !includeSpam) {
                    List<QueryOperation> toAdd = new ArrayList<QueryOperation>();
                    for (Iterator<QueryOperation> iter = localOps.mQueryOperations.iterator(); iter.hasNext();) {
                        QueryOperation cur = iter.next();
                        if (!cur.hasSpamTrashSetting()) {
                            QueryOperation newOp = cur.ensureSpamTrashSetting(mbox, includeTrash, includeSpam);
                            if (newOp != cur) {
                                iter.remove();
                                toAdd.add(newOp);
                            }
                        }
                    }
                    localOps.mQueryOperations.addAll(toAdd);
                }

                ZimbraLog.search.debug("LOCAL_AFTERTS=%s", localOps);

                // Check to see if we need to filter out private appointment data
                boolean allowPrivateAccess = true;
                if (octxt != null) {
                    allowPrivateAccess = AccessManager.getInstance().allowPrivateAccess(octxt.getAuthenticatedUser(),
                            mbox.getAccount(), octxt.isUsingAdminPrivileges());
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
                if (hasCalendarType && !allowPrivateAccess && countTextOperations(localOps) > 0) {
                    // the searcher is NOT allowed to see private items globally....lets check
                    // to see if there are any individual folders that they DO have rights to...
                    // if there are any, then we'll need to run special searches in those
                    // folders
                    Set<Folder> allVisibleFolders = mbox.getVisibleFolders(octxt);
                    if (allVisibleFolders == null) {
                        allVisibleFolders = new HashSet<Folder>();
                        allVisibleFolders.addAll(mbox.getFolderList(octxt, SortBy.NONE));
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

                Set<Folder> visibleFolders = mbox.getVisibleFolders(octxt);

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
                    assert(clonedLocal.mQueryOperations.size() == 1);

                    QueryOperation optimizedClonedLocal = clonedLocal.optimize(mbox);
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

            }

            UnionQueryOperation union = new UnionQueryOperation();
            union.add(localOps);
            union.add(remoteOps);
            ZimbraLog.search.debug("BEFORE_FINAL_OPT=%s", union);
            operation = union.optimize(mbox);
        }
        ZimbraLog.search.debug("END_ZIMBRAQUERY_CONSTRUCTOR=%s", operation);
    }

    SearchParams getParams() {
        return params;
    }

    /**
     * Runs the search and gets an open result set.
     *
     * WARNING: You **MUST** call ZimbraQueryResults.doneWithSearchResults() when you are done with them!
     *
     * @param octxt The operation context
     * @param proto The soap protocol the response should be returned with
     * @return Open ZimbraQueryResults -- YOU MUST CALL doneWithSearchResults() to release the results set!
     * @throws ServiceException
     */
    public ZimbraQueryResults execute() throws ServiceException {
        if (operation != null) {
            QueryTargetSet targets = operation.getQueryTargets();
            assert(operation instanceof UnionQueryOperation || targets.countExplicitTargets() <= 1);
            assert(targets.size() >1 || !targets.hasExternalTargets() || operation instanceof RemoteQueryOperation);

            ZimbraLog.search.debug("OPERATION: %s", operation);

            ZimbraQueryResults results = null;
            try {
                results = HitIdGrouper.create(operation.run(mailbox, params, chunkSize), params.getSortBy());
                if ((!params.getIncludeTagDeleted() && params.getMode() != SearchResultMode.IDS)
                        || params.getAllowableTaskStatuses() != null) {
                    // we have to do some filtering of the result set
                    FilteredQueryResults filtered = new FilteredQueryResults(results);

                    if (!params.getIncludeTagDeleted()) {
                        filtered.setFilterTagDeleted(true);
                    }
                    if (params.getAllowableTaskStatuses() != null) {
                        filtered.setAllowedTaskStatuses(params.getAllowableTaskStatuses());
                    }
                    results = filtered;
                }
                return results;
            } catch (RuntimeException e) {
                if (results != null) {
                    results.doneWithSearchResults();
                }
                operation.doneWithSearchResults();
                throw e;
            }
        } else {
            ZimbraLog.search.debug("Operation optimized to nothing.  Returning no results");
            return new EmptyQueryResults(params.getTypes(), params.getSortBy(), params.getMode());
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
     * For the local targets:
     *   - exclude all the not-visible folders from the query
     *   - look at all the text-operations and figure out if private appointments need to be excluded
     */
    private static UnionQueryOperation handleLocalPermissionChecks(UnionQueryOperation union,
            Set<Folder> visibleFolders, boolean allowPrivateAccess) {

        // Since optimize() has already been run, we know that each of our ops
        // only has one target (or none).  Find those operations which have
        // an external target and wrap them in RemoteQueryOperations
        for (int i = union.mQueryOperations.size()-1; i >= 0; i--) { // iterate backwards so we can remove/add w/o screwing iteration
            QueryOperation op = union.mQueryOperations.get(i);
            QueryTargetSet targets = op.getQueryTargets();

            // this assertion is OK because we have already distributed multi-target query ops
            // during the optimize() step
            assert(targets.countExplicitTargets() <= 1);
            // the assertion above is critical: the code below all assumes
            // that we only have ONE target (ie we've already distributed if necessary)

            assert(!targets.hasExternalTargets());

            if (!targets.hasExternalTargets()) {
                // local target
                if (!allowPrivateAccess)
                    op.depthFirstRecurse(new excludePrivateCalendarItems());

                if (visibleFolders != null) {
                    if (visibleFolders.isEmpty()) {
                        union.mQueryOperations.remove(i);
                        ZimbraLog.search.debug("Query changed to NULL_QUERY_OPERATION, no visible folders");
                        union.mQueryOperations.add(i, new NoResultsQueryOperation());
                    } else {
                        union.mQueryOperations.remove(i);

                        // build a "and (in:visible1 or in:visible2 or in:visible3...)" query tree here!
                        IntersectionQueryOperation intersect = new IntersectionQueryOperation();
                        intersect.addQueryOp(op);

                        UnionQueryOperation newUnion = new UnionQueryOperation();
                        intersect.addQueryOp(newUnion);

                        for (Folder folder : visibleFolders) {
                            // exclude remote folders
                            if (!(folder instanceof Mountpoint) || ((Mountpoint) folder).isLocal()) {
                                DBQueryOperation newOp = new DBQueryOperation();
                                newUnion.add(newOp);
                                newOp.addInClause(folder, true);
                            }
                        }

                        union.mQueryOperations.add(i, intersect);
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

    public String toQueryString() {
        if (operation == null) {
            return "";
        } else {
            return operation.toQueryString();
        }
    }

}
