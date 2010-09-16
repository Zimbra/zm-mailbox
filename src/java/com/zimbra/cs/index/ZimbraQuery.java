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
 *   <li>Parse the query string, turn it into a list of {@link Query}'s. This is
 *   done by the JavaCC-generated {@link QueryParser}.
 *   <li>Push "not's" down to the leaves, so that we never have to invert result
 *   sets. See the internal {@link ParseTree} class.
 *   <li>Generate a {@link QueryOperation} (which is usually a tree of
 *   {@link QueryOperation} objects) from the {@link ParseTree}, then optimize
 *   them {@link QueryOperation}s in preparation to run the query.
 *  </ol>
 *  <li>{@link #execute()} - Begin the search, get the {@link ZimbraQueryResults}
 *  iterator.
 * </ol>
 * long-standing TODO is to move ParseTree classes out of this class.
 *
 * @author tim
 * @author ysasaki
 */
public final class ZimbraQuery {

    private List<Query> mClauses;
    private ParseTree.Node mParseTree = null;
    private QueryOperation mOp;
    private Mailbox mMbox;
    private ZimbraQueryResults mResults;
    private SearchParams mParams;
    private int mChunkSize;

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

        private static final boolean SPEW = false;

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
                    if (ParseTree.SPEW) System.out.print(" AND(");

                    IntersectionQueryOperation intersect = new IntersectionQueryOperation();

                    for (Node n : mNodes) {
                        QueryOperation op = n.getQueryOperation();
                        assert(op!=null);
                        intersect.addQueryOp(op);
                    }

                    if (ParseTree.SPEW) {
                        System.out.print(") ");
                    }
                    return intersect;
                } else {
                    if (ParseTree.SPEW) {
                        System.out.print(" OR(");
                    }

                    UnionQueryOperation union = new UnionQueryOperation();

                    for (Node n : mNodes) {
                        QueryOperation op = n.getQueryOperation();
                        assert(op != null);
                        union.add(op);
                    }
                    if (ParseTree.SPEW) {
                        System.out.print(") ");
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

    /**
     * the query string can OPTIONALLY have a "sortby:" element which will override
     * the sortBy specified in the <SearchRequest> xml...this is basically to allow
     * people to do more with cut-and-pasted search strings
     */
    private SortBy mSortByOverride = null;

    private void handleSortByOverride(String str) throws ServiceException {
        SortBy sortBy = SortBy.lookup(str);
        if (sortBy == null) {
            throw ServiceException.FAILURE(
                    "Unkown sortBy: specified in search string: " + str, null);
        }

        mSortByOverride = sortBy;
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
     * @return number of Text parts of this query
     */
    int countSearchTextOperations() {
        if (mOp == null) {
            return 0;
        }
        CountTextOperations count = new CountTextOperations();
        mOp.depthFirstRecurse(count);
        return count.num;
    }

    /**
     * @return number of Text parts of this query
     */
    private static int countSearchTextOperations(QueryOperation op) {
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
        if (mOp == null) {
            return 0;
        }
        CountCombiningOperations count =  new CountCombiningOperations();
        mOp.depthFirstRecurse(count);
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
    public ZimbraQuery(OperationContext octxt, SoapProtocol proto,
            Mailbox mbox, SearchParams params) throws ServiceException {

        mParams = params;
        mMbox = mbox;
        long chunkSize = (long) mParams.getOffset() + (long) mParams.getLimit();
        if (chunkSize > 1000) {
            mChunkSize = 1000;
        } else {
            mChunkSize = (int)chunkSize;
        }

        Analyzer analyzer = null;
        MailboxIndex index = mbox.getMailboxIndex();

        // Step 1: parse the text using the JavaCC parser
        try {
            if (index != null) {
                index.initAnalyzer(mbox);
                analyzer = index.getAnalyzer();
            } else {
                analyzer = ZimbraAnalyzer.getDefaultAnalyzer();
            }
            QueryParser parser = new QueryParser(mbox, analyzer);
            parser.setDefaultField(params.getDefaultField());
            parser.setTimeZone(params.getTimeZone());
            parser.setLocale(params.getLocale());
            mClauses = parser.parse(params.getQueryStr());

            String sortBy = parser.getSortBy();
            if (sortBy != null) {
                handleSortByOverride(sortBy);
            }
        } catch (Error e) {
            throw ServiceException.FAILURE(
                    "ZimbraQueryParser threw Error: " + e, e);
        }

        if (ZimbraLog.index_search.isDebugEnabled()) {
            StringBuilder buf = new StringBuilder(toString());
            buf.append(" search([");
            buf.append(mParams.getTypesStr());
            buf.append("],");
            buf.append(mParams.getSortBy());
            buf.append(')');
            ZimbraLog.index_search.debug(buf.toString());
        }

        // Step 2: build a parse tree and push all the "NOT's" down to the
        // bottom level -- this is because we cannot invert result sets
        if (ParseTree.SPEW) {
            System.out.println("QueryString: " + mParams.getQueryStr());
        }
        ParseTree.Node pt = ParseTree.build(mClauses);
        if (ParseTree.SPEW) {
            System.out.println("PT: " + pt.toString());
        }
        if (ParseTree.SPEW) {
            System.out.println("Simplified:");
        }
        pt = pt.simplify();
        if (ParseTree.SPEW) {
            System.out.println("PT: " + pt.toString());
        }
        if (ParseTree.SPEW) {
            System.out.println("Pushing nots down:");
        }
        pt.pushNotsDown();
        if (ParseTree.SPEW) {
            System.out.println("PT: " + pt.toString());
        }

        // Store some variables that we'll need later
        mParseTree = pt;
        mOp = null;

        // handle the special "sort:" tag in the search string
        if (mSortByOverride != null) {
            if (ZimbraLog.index_search.isDebugEnabled())
                ZimbraLog.index_search.debug(
                        "Overriding SortBy parameter to execute (" +
                        params.getSortBy().toString() +
                        ") w/ specification from QueryString: " +
                        mSortByOverride.toString());

            params.setSortBy(mSortByOverride);
        }

        // Step 3: Convert list of BaseQueries into list of QueryOperations, then Optimize the Ops
        if (mClauses.size() > 0) {
            // this generates all of the query operations
            mOp = mParseTree.getQueryOperation();

            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("OP=%s", mOp);
            }

            // expand the is:local and is:remote parts into in:(LIST)'s
            mOp = mOp.expandLocalRemotePart(mbox);
            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("AFTEREXP=%s", mOp);
            }

            // optimize the query down
            mOp = mOp.optimize(mMbox);
            if (mOp == null)
                mOp = new NoResultsQueryOperation();
            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("OPTIMIZED=%s", mOp);
            }
        }

        // STEP 4: use the OperationContext to update the set of visible referenced folders, local AND remote
        if (mOp != null) {
            QueryTargetSet queryTargets = mOp.getQueryTargets();
            assert(mOp instanceof UnionQueryOperation ||
                    queryTargets.countExplicitTargets() <= 1);

            // easiest to treat the query two unions: one the LOCAL and one REMOTE parts
            UnionQueryOperation remoteOps = new UnionQueryOperation();
            UnionQueryOperation localOps = new UnionQueryOperation();

            if (mOp instanceof UnionQueryOperation) {
                UnionQueryOperation union = (UnionQueryOperation) mOp;
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

                QueryTargetSet targets = mOp.getQueryTargets();
                // this assertion OK because we have already distributed multi-target query ops
                // during the optimize() step
                assert(targets.countExplicitTargets() <= 1);

                if (targets.hasExternalTargets()) {
                    remoteOps.add(mOp);
                } else {
                    localOps.add(mOp);
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
                        ZimbraLog.index_search.info("Ignoring " + e +
                                " during RemoteQuery generation for " + remoteOps);
                    }
                }
            }

            //
            // For the LOCAL parts of the query, do permission checks, do trash/spam exclusion
            //
            if (!localOps.mQueryOperations.isEmpty()) {
                if (ZimbraLog.index_search.isDebugEnabled()) {
                    ZimbraLog.index_search.debug("LOCAL_IN=" + localOps.toString());
                }

                Account authAcct = null;
                if (octxt != null) {
                    authAcct = octxt.getAuthenticatedUser();
                } else {
                    authAcct = mbox.getAccount();
                }

                //
                // Now, for all the LOCAL PARTS of the query, add the trash/spam exclusion part
                //
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

                if (ZimbraLog.index_search.isDebugEnabled()) {
                    ZimbraLog.index_search.debug(
                            "LOCAL_AFTERTS=" + localOps.toString());
                }

                //
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
                boolean hasCalendarType = false;
                if (params.getTypes() != null) {
                    for (byte b : params.getTypes()) {
                        if (b == MailItem.TYPE_APPOINTMENT || b == MailItem.TYPE_TASK) {
                            hasCalendarType = true;
                            break;
                        }
                    }
                }
                if (hasCalendarType && !allowPrivateAccess && countSearchTextOperations(localOps)>0) {
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
                        if (f.getType() == MailItem.TYPE_FOLDER &&
                                CalendarItem.allowPrivateAccess(f, authAcct, false)) {
                            hasFolderRightPrivateSet.add(f);
                        }
                    }
                    if (!hasFolderRightPrivateSet.isEmpty()) {
                        clonedLocal = (UnionQueryOperation)localOps.clone();
                    }
                }

                Set<Folder> visibleFolders = mbox.getVisibleFolders(octxt);

                localOps = handleLocalPermissionChecks(localOps, visibleFolders,
                        allowPrivateAccess);

                if (ZimbraLog.index_search.isDebugEnabled()) {
                    ZimbraLog.index_search.debug("LOCAL_AFTER_PERM_CHECKS=%s", localOps);
                }

                if (!hasFolderRightPrivateSet.isEmpty()) {
                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("CLONED_LOCAL_BEFORE_PERM=%s", clonedLocal);
                    }

                    //
                    // now we're going to setup the clonedLocal tree
                    // to run with private access ALLOWED, over the set of folders
                    // that have RIGHT_PRIVATE (note that we build this list from the visible
                    // folder list, so we are
                    //
                    clonedLocal = handleLocalPermissionChecks(
                            clonedLocal, hasFolderRightPrivateSet, true);

                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("CLONED_LOCAL_AFTER_PERM=%s", clonedLocal);
                    }

                    // clonedLocal should only have the single INTERSECT in it
                    assert(clonedLocal.mQueryOperations.size() == 1);

                    QueryOperation optimizedClonedLocal = clonedLocal.optimize(mbox);
                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("CLONED_LOCAL_AFTER_OPTIMIZE=%s", optimizedClonedLocal);
                    }

                    UnionQueryOperation withPrivateExcluded = localOps;
                    localOps = new UnionQueryOperation();
                    localOps.add(withPrivateExcluded);
                    localOps.add(optimizedClonedLocal);

                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("LOCAL_WITH_CLONED=%s", localOps);
                    }

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
            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("BEFORE_FINAL_OPT=%s", union);
            }
            mOp = union.optimize(mbox);
            assert(union.mQueryOperations.size() > 0);
        }
        if (ZimbraLog.index_search.isDebugEnabled()) {
            ZimbraLog.index_search.debug("END_ZIMBRAQUERY_CONSTRUCTOR=%s", mOp);
        }
    }

    public void doneWithQuery() throws ServiceException {
        if (mResults != null)
            mResults.doneWithSearchResults();

        if (mOp != null)
            mOp.doneWithSearchResults();
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
    final public ZimbraQueryResults execute() throws ServiceException {

        if (mOp != null) {
            QueryTargetSet targets = mOp.getQueryTargets();
            assert(mOp instanceof UnionQueryOperation || targets.countExplicitTargets() <=1);
            assert(targets.size() >1 || !targets.hasExternalTargets() || mOp instanceof RemoteQueryOperation);

            if (ZimbraLog.index_search.isDebugEnabled())
                ZimbraLog.index_search.debug("OPERATION:"+mOp.toString());

            assert(mResults == null);

            mResults = mOp.run(mMbox, mParams, mChunkSize);

            mResults = HitIdGrouper.Create(mResults, mParams.getSortBy());

            if ((!mParams.getIncludeTagDeleted() && mParams.getMode() != SearchResultMode.IDS)
                            || mParams.getAllowableTaskStatuses()!=null) {
                // we have to do some filtering of the result set
                FilteredQueryResults filtered = new FilteredQueryResults(mResults);

                if (!mParams.getIncludeTagDeleted())
                    filtered.setFilterTagDeleted(true);
                if (mParams.getAllowableTaskStatuses()!=null)
                    filtered.setAllowedTaskStatuses(mParams.getAllowableTaskStatuses());
                mResults = filtered;
            }

            return mResults;
        } else {
            ZimbraLog.index_search.debug("Operation optimized to nothing.  Returning no results");
            return new EmptyQueryResults(mParams.getTypes(), mParams.getSortBy(), mParams.getMode());
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
                        LuceneFields.L_FIELD, CalendarItem.INDEX_FIELD_ITEM_CLASS_PRIVATE)), false);
            }
        }
    }

    /**
     * For the local targets:
     *   - exclude all the not-visible folders from the query
     *   - look at all the text-operations and figure out if private appointments need to be excluded
     */
    private static UnionQueryOperation handleLocalPermissionChecks(
            UnionQueryOperation union, Set<Folder> visibleFolders,
            boolean allowPrivateAccess) {

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
                    if (visibleFolders.size() == 0) {
                        union.mQueryOperations.remove(i);
                        ZimbraLog.index_search.debug("Query changed to NULL_QUERY_OPERATION, no visible folders");
                        union.mQueryOperations.add(i, new NoResultsQueryOperation());
                    } else {
                        union.mQueryOperations.remove(i);

                        // build a "and (in:visible1 or in:visible2 or in:visible3...)" query tree here!
                        IntersectionQueryOperation intersect = new IntersectionQueryOperation();
                        intersect.addQueryOp(op);

                        UnionQueryOperation newUnion = new UnionQueryOperation();
                        intersect.addQueryOp(newUnion);

                        for (Folder f : visibleFolders) {
                            DBQueryOperation newOp = new DBQueryOperation();
                            newUnion.add(newOp);
                            newOp.addInClause(f, true);
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
        for (Query clause : mClauses) {
            clause.toString(out);
        }
        return out.toString();
    }

    public String toQueryString() {
        if (mOp == null) {
            return "";
        } else {
            return mOp.toQueryString();
        }
    }

}
