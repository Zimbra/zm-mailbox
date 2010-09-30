/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.db.DbSearchConstraints.NumericRange;
import com.zimbra.cs.db.DbSearchConstraints.StringRange;
import com.zimbra.cs.db.DbSearchConstraintsNode.NodeType;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.SortBy.SortCriterion;
import com.zimbra.cs.index.SortBy.SortDirection;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Tag;

public class DbSearch {

    private static Log sLog = LogFactory.getLog(DbSearch.class);

    public static final class SearchResult {
        public int    id;
        public String indexId;
        public byte   type;
        public Object sortkey;
        public Object extraData;

        public enum ExtraData { NONE, MAIL_ITEM, IMAP_MSG, MODSEQ, PARENT, MODCONTENT };

        public static class SizeEstimate {
            public SizeEstimate() {}
            public SizeEstimate(int initialval) { mSizeEstimate = initialval; }
            public int mSizeEstimate;
        }


        public static SearchResult createResult(ResultSet rs, SortBy sort, boolean inDumpster) throws SQLException {
            return createResult(rs, sort, ExtraData.NONE, inDumpster);
        }

        public static SearchResult createResult(ResultSet rs, SortBy sort, ExtraData extra, boolean inDumpster) throws SQLException {
            SearchResult result = new SearchResult();
            result.id      = rs.getInt(COLUMN_ID);
            result.indexId = rs.getString(COLUMN_INDEXID);
            result.type    = rs.getByte(COLUMN_TYPE);
            switch (sort.getCriterion()) {
                case SUBJECT:
                case SENDER:
                case NAME:
                case NAME_NATURAL_ORDER:
                    result.sortkey = rs.getString(COLUMN_SORTKEY);
                    break;
                case SIZE:
                    result.sortkey = new Long(rs.getInt(COLUMN_SORTKEY));
                    break;
                case NONE:
                    // note that there's no sort column in the result set for SORT_NONE
                    break;
                default:
                    result.sortkey = new Long(rs.getInt(COLUMN_SORTKEY) * 1000L);
                    break;
            }

            int offset = sort.getCriterion() == SortCriterion.NONE ? COLUMN_SORTKEY - 1 : COLUMN_SORTKEY;
            if (extra == ExtraData.MAIL_ITEM) {
                // note that there's no sort column in the result set for SORT_NONE
                result.extraData = DbMailItem.constructItem(rs, offset, inDumpster);
            } else if (extra == ExtraData.IMAP_MSG) {
                int flags = rs.getBoolean(offset + 2) ? Flag.BITMASK_UNREAD | rs.getInt(offset + 3) : rs.getInt(offset + 3);
                result.extraData = new ImapMessage(result.id, result.type, rs.getInt(offset + 1), flags, rs.getLong(offset + 4));
            } else if (extra == ExtraData.MODSEQ || extra == ExtraData.PARENT || extra==ExtraData.MODCONTENT) {
                int value = rs.getInt(offset + 1);
                result.extraData = rs.wasNull() ? -1 : value;
            }
            return result;
        }

        @Override public String toString() {
            return sortkey + " => (" + id + "," + type + ")";
        }

        @Override public int hashCode() {
            return id;
        }

        @Override public boolean equals(Object obj) {
            SearchResult other = (SearchResult) obj;
            return other.id == id;
        }
        
        private static class SearchResultComparator implements Comparator<SearchResult> {
            private SortBy mSort;
            SearchResultComparator(SortBy sort)  { mSort = sort; }

            @Override public int compare(SearchResult o1, SearchResult o2) {
                switch (mSort.getCriterion()) {
                    case SIZE:
                    case DATE:
                        long date1 = (Long) o1.sortkey;
                        long date2 = (Long) o2.sortkey;
                        if (date1 != date2) {
                            long diff;
                            if (mSort.getDirection() == SortDirection.DESCENDING) {
                                diff = date2 - date1;
                            } else {
                                diff = date1 - date2;
                            }
                            return (diff > 0) ? 1 : -1;
                        }
                        // fall through to ID-based comparison below!
                        break;
                    case NONE:
                        break;
                    default:
                        throw new UnsupportedOperationException("SearchResultComparator not implemented " +
                                                                " for anything except for DATE and SIZE right now. " +
                                                                " Feel free to fix it!"); 
                }
                if (mSort.getDirection() == SortDirection.DESCENDING) {
                    return o2.id - o1.id;
                } else {
                    return o1.id - o2.id;
                }
            }
        }
        
        public static Comparator<SearchResult> getComparator(SortBy sort) {
            return new SearchResultComparator(sort);
        }
    }

    // alias the sort column b/c of ambiguity problems (the sort column is included twice in the 
    // result set, and MySQL chokes on the ORDER BY when we do a UNION query (doesn't know
    // which 2 of the 4 sort columns are the "right" ones to use)
    public static final String SORT_COLUMN_ALIAS = "sortcol";
    
    /**
     * @param fieldName
     * @return TRUE if this field is case-sensitive for search/sort -- ie do we need to
     *         do an UPPER(fieldName) on it in places?
     */
    private static boolean isCaseSensitiveField(String fieldName) {
        // we need to handle things like "mi.sender" for the sender column, etc
        // so look for the last . in the fieldname, return the string after that.
        String colNameAfterPeriod; 
        int periodOffset = fieldName.lastIndexOf('.');
        if (periodOffset <= 0 && periodOffset < (fieldName.length()+1))
            colNameAfterPeriod = fieldName;
        else
            colNameAfterPeriod = fieldName.substring(periodOffset+1);
        
        return (colNameAfterPeriod.equals("sender") || 
                        colNameAfterPeriod.equals("subject") ||
                        colNameAfterPeriod.equals("name")); 
    }

    private static String sortField(SortBy sort, boolean useAlias, boolean includeCollation) {
        String str;
        boolean stringVal = false;
        switch (sort.getCriterion()) {
            case SENDER:   str = "mi.sender";   stringVal = true;  break;
            case SUBJECT:  str = "mi.subject";  stringVal = true;  break;
            case NAME_NATURAL_ORDER:
            case NAME:     str = "mi.name";     stringVal = true;  break;
            case ID:       str = "mi.id";    break;
            case SIZE:     str = "mi.size";  break;
            case DATE:
            default:       str = "mi.date";  break; 
            case NONE:     return null;
        }
        
        if (useAlias) {
            str = SORT_COLUMN_ALIAS; // still need the stringVal setting above!
        } else {
            if (stringVal && Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON)) 
                str = "UPPER(" + str + ")";
        }

        // tim: back this out for now, but leave code stub here until I deal with it
        // see bugs 22665, 36579
        //if (Db.supports(Db.Capability.REQUEST_UTF8_UNICODE_COLLATION) && stringVal && includeCollation) 
        //    str += " COLLATE utf8_unicode_ci";

        return str;
    }
    
    /**
     * generate a column-reference for the sort-by column.  This column reference
     * goes at the beginning of the SELECT statement (the ORDER BY part is generated
     * by sortQuery() below)
     */
    static String sortKey(SortBy sort) {
        String field = sortField(sort, false, false);
        // note that there's no sort column in the result set for SORT_NONE
        if (field == null)
            return "";
        return ", " + field + " AS " + SORT_COLUMN_ALIAS;
    }

    static String sortQuery(SortBy sort) {
        return sortQuery(sort, false);
    }

    /**
     * Generate the ORDER BY part that goes at the end of the select
     */
    static String sortQuery(SortBy sort, boolean useAlias) {
        // note that there's no need for an ORDER BY clause for SORT_NONE
        if (sort.getCriterion() == SortCriterion.NONE)
            return "";

        String direction = sort.getDirection() == SortDirection.DESCENDING ? " DESC" : "";
        StringBuilder statement = new StringBuilder(" ORDER BY ");
        statement.append(sortField(sort, useAlias, true)).append(direction);
        // when two items match in their sort field, let's use item ID as the tie breaker
        //   (commented out as a result of perf issues -- see bug 50469)
//        statement.append(", mi.id").append(direction);
        return statement.toString();
    }


    public static int countResults(Connection conn, DbSearchConstraintsNode node, Mailbox mbox, boolean inDumpster)
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        // Assemble the search query
        StringBuilder statement = new StringBuilder("SELECT count(*) ");
        statement.append(" FROM " + DbMailItem.getMailItemTableName(mbox, "mi", inDumpster));
        statement.append(" WHERE ").append(DbMailItem.IN_THIS_MAILBOX_AND);
        int num = DebugConfig.disableMailboxGroups ? 0 : 1;
        
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            num += encodeConstraint(mbox, node, null, false, statement, conn);

            stmt = conn.prepareStatement(statement.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            pos = setSearchVars(stmt, node, pos, null, false);

            if (sLog.isDebugEnabled())
                sLog.debug("SQL: " + statement);

            assert(pos == num + 1); 
            rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching search metadata", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    // Indexes on mail_item table
    private static final String MI_I_MBOX_FOLDER_DATE = "i_folder_id_date";
//    private static final String MI_I_MBOX_ID_PKEY     = "PRIMARY";
    private static final String MI_I_MBOX_PARENT      = "i_parent_id";
    private static final String MI_I_MBOX_INDEX       = "i_index_id";
//    private static final String MI_I_MBOX_DATE        = "i_date";
//    private static final String MI_I_MBOX_TAGS_DATE   = "i_tags_date";
//    private static final String MI_I_MBOX_FLAGS_DATE  = "i_flags_date";
//    private static final String MI_I_MBOX_TYPE        = "i_type";
//    private static final String MI_I_MBOX_UNREAD      = "i_unread";
//    private static final String MI_I_MBOX_MODMETADATA = "i_mod_metadata";
//    private static final String MI_I_MBOX_FOLDER_NAME = "i_name_folder_id";

    private static final String NO_HINT = "";

    private static String getForceIndexClause(DbSearchConstraintsNode node, SortBy sort, boolean hasLimit) {
        if (LC.search_disable_database_hints.booleanValue())
            return NO_HINT;

        if (!Db.supports(Db.Capability.FORCE_INDEX_EVEN_IF_NO_SORT) && sort.getCriterion() == SortCriterion.NONE)
            return NO_HINT;
        
        String index = null;

        DbSearchConstraintsNode.NodeType ntype = node.getNodeType();
        DbSearchConstraints constraints = node.getSearchConstraints();
        if (ntype == DbSearchConstraintsNode.NodeType.LEAF) {
            if (!constraints.itemIds.isEmpty()) {
                return "";
            } else if (constraints.convId > 0) {
                index = MI_I_MBOX_PARENT;
            } else if (!constraints.indexIds.isEmpty()) {
                index = MI_I_MBOX_INDEX;
            } else if (sort.getCriterion() == SortCriterion.DATE && hasLimit) {
                // Whenever we learn a new case of mysql choosing wrong index, add a case here.
                if (constraints.isSimpleSingleFolderMessageQuery()) {
                    // Optimization for folder query
                    //
                    // If looking at a single folder and sorting by date with a limit,
                    // force the use of i_folder_id_date index.  Typical example of
                    // such a query is the default "in:Inbox" search.
                    index = MI_I_MBOX_FOLDER_DATE;
                }
            }
        }

        return Db.forceIndex(index);
    }

    // put these into constants so that people can easily tell what is dependent on the positons
    private static final int COLUMN_ID      = 1;
    private static final int COLUMN_INDEXID = 2;
    private static final int COLUMN_TYPE    = 3;
    private static final int COLUMN_SORTKEY = 4;

    private static final String encodeSelect(Mailbox mbox, SortBy sort, SearchResult.ExtraData extra,
                                             boolean includeCalTable, DbSearchConstraintsNode node,
                                             boolean validLIMIT, boolean inDumpster) {
        /*
         * "SELECT mi.id,mi.date, [extrafields] FROM mail_item AS mi [, appointment AS ap]
         *    [FORCE INDEX (...)]
         *    WHERE mi.mailboxid=? [AND ap.mailboxId=? AND mi.id = ap.id ] AND
         * 
         *  If you change the first for parameters, you must change the COLUMN_* values above!
         */
        StringBuilder select = new StringBuilder("SELECT mi.id, mi.index_id, mi.type").append(sortKey(sort));
        if (extra == SearchResult.ExtraData.MAIL_ITEM)
            select.append(", " + DbMailItem.DB_FIELDS);
        else if (extra == SearchResult.ExtraData.IMAP_MSG)
            select.append(", mi.imap_id, mi.unread, mi.flags, mi.tags");
        else if (extra == SearchResult.ExtraData.MODSEQ)
            select.append(", mi.mod_metadata");
        else if (extra == SearchResult.ExtraData.PARENT)
            select.append(", mi.parent_id");
        else if (extra == SearchResult.ExtraData.MODCONTENT)
            select.append(", mi.mod_content");

        select.append(" FROM " + DbMailItem.getMailItemTableName(mbox, "mi", inDumpster));
        if (includeCalTable) 
            select.append(", ").append(DbMailItem.getCalendarItemTableName(mbox, "ap", inDumpster));
        
        /*
         * FORCE INDEX (...)
         */
        if (!includeCalTable) // can't force index when selecting from two tables?
            select.append(getForceIndexClause(node, sort, validLIMIT));
        
        /*
         *  WHERE mi.mailboxId=? [AND ap.mailboxId=? AND mi.id = ap.id ] AND "
         */
        select.append(" WHERE ");
        select.append(DbMailItem.getInThisMailboxAnd(mbox.getId(), "mi", includeCalTable ? "ap" : null));
        if (includeCalTable)
            select.append(" mi.id = ap.item_id AND ");
        
        return select.toString();
    }
    
    /**
     * @param mbox
     * @param node
     * @param calTypes
     * @param inCalTable
     * @param statement
     * @param conn
     * @return Number of constraints encoded 
     * @throws ServiceException
     */
    private static final int encodeConstraint(Mailbox mbox, DbSearchConstraintsNode node,
        byte[] calTypes, boolean inCalTable, StringBuilder statement, Connection conn) 
    throws ServiceException {
        /*
         *( SUB-NODE AND/OR (SUB-NODE...) ) AND/OR ( SUB-NODE ) AND
         *    ( 
         *       one of: [type NOT IN (...)]  || [type = ?] || [type IN ( ...)]
         *       [ AND tags != 0]
         *       [ AND tags IN ( ... ) ]
         *       [ AND flags IN (...) ] 
         *       ..etc
         *    )   
         */
        int num = 0;
        DbSearchConstraintsNode.NodeType ntype = node.getNodeType();
        if (ntype == DbSearchConstraintsNode.NodeType.AND || ntype == DbSearchConstraintsNode.NodeType.OR) {
            boolean first = true;
            boolean and = ntype == DbSearchConstraintsNode.NodeType.AND;
            statement.append('(');
            for (DbSearchConstraintsNode subnode : node.getSubNodes()) {
                if (!first)
                    statement.append(and ? " AND " : " OR ");
                num += encodeConstraint(mbox, subnode, calTypes, inCalTable, statement, conn);
                first = false;
            }
            statement.append(") ");
            return num;
        }
        
        // we're here, so we must be in a DbSearchConstraints leaf node
        DbSearchConstraints c = node.getSearchConstraints();
        assert(ntype == DbSearchConstraintsNode.NodeType.LEAF && c != null);
        c.checkDates();
        
        // if there are no possible matches, short-circuit here...
        TagConstraints tc = TagConstraints.getTagConstraints(mbox, c, conn);
        if (c.automaticEmptySet() || tc.noMatches) {
            statement.append(Db.supports(Db.Capability.BOOLEAN_DATATYPE) ?
                "FALSE" : "0=1"); 
            return num;
        }
        
        statement.append('(');

        // special-case this one, since there can't be a leading AND here...
        if (ListUtil.isEmpty(c.types)) {
            statement.append("type NOT IN " + DbMailItem.NON_SEARCHABLE_TYPES);
        } else {
            statement.append(DbUtil.whereIn("type", c.types.size()));
            num += c.types.size();
        }
        
        num += encode(statement, "mi.type", false, c.excludeTypes);
        num += encode(statement, "mi.type", inCalTable, calTypes);

        // if hasTags is NULL then nothing
        // if hasTags is TRUE then !=0
        // if hasTags is FALSE then = 0
        if (c.hasTags != null) {
            if (c.hasTags.booleanValue())
                statement.append(" AND mi.tags != 0");
            else
                statement.append(" AND mi.tags = 0");
        }
        
        num += encode(statement, "mi.tags", true, tc.searchTagsets);
        num += encode(statement, "mi.flags", true, tc.searchFlagsets);
        num += encode(statement, "unread", true, tc.unread);
        num += encode(statement, "mi.folder_id", true, c.folders);
        num += encode(statement, "mi.folder_id", false, c.excludeFolders);
        if (c.convId > 0)
            num += encode(statement, "mi.parent_id", true);
        else
            num += encode(statement, "mi.parent_id", false, c.prohibitedConvIds);
        num += encode(statement, "mi.id", true, c.itemIds);
        num += encode(statement, "mi.id", false, c.prohibitedItemIds);
        num += encode(statement, "mi.index_id", true, c.indexIds);
        num += encodeRangeWithMinimum(statement, "mi.date", c.dates, 1);
        num += encodeRangeWithMinimum(statement, "mi.mod_metadata", c.modified, 1);
        num += encodeRangeWithMinimum(statement, "mi.mod_content", c.modifiedContent, 1);
        num += encodeRangeWithMinimum(statement, "mi.size", c.sizes, 0);
        num += encodeRange(statement, "mi.subject", c.subjectRanges);
        num += encodeRange(statement, "mi.sender", c.senderRanges);
        
        Boolean isSoloPart = node.getSearchConstraints().getIsSoloPart();
        if (isSoloPart != null) {
            if (isSoloPart.booleanValue()) {
                statement.append(" AND mi.parent_id is NULL ");
            } else {
                statement.append(" AND mi.parent_id is NOT NULL ");
            }
        }
        
        if (c.hasIndexId != null) {
            if (c.hasIndexId.booleanValue()) {
                statement.append(" AND mi.index_id is NOT NULL ");
            } else {
                statement.append(" AND mi.index_id is NULL ");
            }
        }
        
        if (inCalTable) {
            num += encodeRangeWithMinimum(statement, "ap.start_time", c.calStartDates, 1);
            num += encodeRangeWithMinimum(statement, "ap.end_time", c.calEndDates, 1);
        }

        statement.append(')');
        
        return num;
    }
    
    /**
     * @return TRUE if some part of this query has a non-appointment select (ie 'type not in (11,15)' non-null 
     */
    private static final boolean hasMailItemOnlyConstraints(DbSearchConstraintsNode node) {
        DbSearchConstraintsNode.NodeType ntype = node.getNodeType();
        if (ntype == DbSearchConstraintsNode.NodeType.AND || ntype == DbSearchConstraintsNode.NodeType.OR) {
            for (DbSearchConstraintsNode subnode : node.getSubNodes()) {
                if (hasMailItemOnlyConstraints(subnode))
                    return true;
            }
            return false;
        }
        return node.getSearchConstraints().hasNonAppointmentTypes();
    }
    
    /**
     * @return TRUE if this constraint needs to do a join with the Appointment table in order to be evaluated
     */
    private static final boolean hasAppointmentTableConstraints(DbSearchConstraintsNode node) {
        DbSearchConstraintsNode.NodeType ntype = node.getNodeType();
        if (ntype == DbSearchConstraintsNode.NodeType.AND || ntype == DbSearchConstraintsNode.NodeType.OR) {
            for (DbSearchConstraintsNode subnode : node.getSubNodes()) {
                if (hasAppointmentTableConstraints(subnode))
                    return true;
            }
            return false;
        }
        return node.getSearchConstraints().hasAppointmentTableConstraints();
    }
    
    
    static final byte[] APPOINTMENT_TABLE_TYPES = new byte[] { MailItem.TYPE_APPOINTMENT, MailItem.TYPE_TASK };

    public static List<SearchResult> search(List<SearchResult> result, Connection conn, DbSearchConstraints c,
                                            Mailbox mbox, SortBy sort, SearchResult.ExtraData extra)
    throws ServiceException {
        return search(result, conn, c, mbox, sort, extra, false);
    }
    
    public static List<SearchResult> search(List<SearchResult> result, Connection conn, DbSearchConstraints c,
            Mailbox mbox, SortBy sort, SearchResult.ExtraData extra, boolean inDumpster)
    throws ServiceException {
        return search(result, conn, c, mbox, sort, -1, -1, extra, inDumpster);
    }

    private static <T> List<T> mergeSortedLists(List<T> toRet, List<List<T>> lists, Comparator<? super T> comparator) {
        // TODO find or code a proper merge-sort here
        int totalNumValues = 0;
        for (List<T> l : lists) {
            totalNumValues += l.size();
        }
        
        for (List<T> l : lists) {
            toRet.addAll(l);
        }
        
        Collections.sort(toRet, comparator);
        
        return toRet;
    }
    
    public static List<SearchResult> search(List<SearchResult> result, Connection conn,
                                            DbSearchConstraintsNode node, Mailbox mbox, SortBy sort,
                                            int offset, int limit, SearchResult.ExtraData extra, boolean inDumpster)
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        // this monstrosity for bug 31343
        if (!Db.supports(Db.Capability.AVOID_OR_IN_WHERE_CLAUSE) ||
                        (sort.getCriterion() != SortCriterion.DATE && sort.getCriterion() != SortCriterion.SIZE) || 
                        NodeType.OR != node.getNodeType()) {
            // do it the old way
            return searchInternal(result, conn, node, mbox, sort, offset, limit, extra, inDumpster);
        } else {
            // run each toplevel ORed part as a separate SQL query, then merge
            // the results in memory
            List<List<SearchResult>> resultLists = new ArrayList<List<SearchResult>>();
            
            for (DbSearchConstraintsNode subNode : node.getSubNodes()) {
                List<SearchResult> subNodeResults = new ArrayList<SearchResult>();
                search(subNodeResults, conn, subNode, mbox, sort, offset, limit, extra, inDumpster);
                resultLists.add(subNodeResults);
            }

            Comparator<SearchResult> comp = SearchResult.getComparator(sort);
            result = mergeSortedLists(result, resultLists, comp);
            return result;
        }
    }
        
    public static List<SearchResult> searchInternal(List<SearchResult> result, Connection conn,
                                                    DbSearchConstraintsNode node, Mailbox mbox, SortBy sort,
                                                    int offset, int limit, SearchResult.ExtraData extra,
                                                    boolean inDumpster)
    throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        boolean hasValidLIMIT = offset >= 0 && limit >= 0;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        StringBuilder statement = new StringBuilder();
        int numParams = 0;
        boolean hasMailItemOnlyConstraints = true;
        boolean hasAppointmentTableConstraints = hasAppointmentTableConstraints(node);
        if (hasAppointmentTableConstraints)
            hasMailItemOnlyConstraints = hasMailItemOnlyConstraints(node);
        boolean requiresUnion = hasMailItemOnlyConstraints && hasAppointmentTableConstraints;
        
        try {
            if (hasMailItemOnlyConstraints) {
                if (requiresUnion) {
                    statement.append("(");
                }
                
                /*
                 * "SELECT mi.id,mi.date, [extrafields] FROM mail_item AS mi 
                 *    [FORCE INDEX (...)]
                 *    WHERE mi.mailboxid=? AND
                 */
                statement.append(encodeSelect(mbox, sort, extra, false, node, hasValidLIMIT, inDumpster));
                
                /*
                 *( SUB-NODE AND/OR (SUB-NODE...) ) AND/OR ( SUB-NODE ) AND
                 *    ( 
                 *       one of: [type NOT IN (...)]  || [type = ?] || [type IN ( ...)]
                 *       [ AND tags != 0]
                 *       [ AND tags IN ( ... ) ]
                 *       [ AND flags IN (...) ] 
                 *       ..etc
                 *    )   
                 */
                numParams += encodeConstraint(mbox, node, 
                    (hasAppointmentTableConstraints ? APPOINTMENT_TABLE_TYPES : null), 
                    false, statement, conn);
                
                if (requiresUnion) {
                    /*
                     * ORDER BY (sortField) 
                     */
                    statement.append(sortQuery(sort, true));
                    
                    /*
                     * LIMIT ?, ? 
                     */
                    if (hasValidLIMIT && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                        statement.append(" LIMIT ").append(offset).append(',').append(limit);
                    }
                }
            }
            
            if (requiresUnion) {
                /*
                 * UNION
                 */
                statement.append(" ) UNION ALL (");
            }
            
            if (hasAppointmentTableConstraints) {
                /*
                 * SELECT...again...(this time with "appointment as ap")...WHERE...
                 */
                statement.append(encodeSelect(mbox, sort, extra, true, node, hasValidLIMIT, inDumpster));
                numParams += encodeConstraint(mbox, node, APPOINTMENT_TABLE_TYPES, true, statement, conn);
                
                if (requiresUnion) {
                    /*
                     * ORDER BY (sortField) 
                     */
                    statement.append(sortQuery(sort, true));
                    
                    /*
                     * LIMIT ?, ? 
                     */
                    if (hasValidLIMIT && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                        statement.append(" LIMIT ").append(offset).append(',').append(limit);
                    }
                    
                    if (requiresUnion) {
                        statement.append(")");
                    }
                }
            }
            
            //
            // TODO FIXME: include COLLATION for sender/subject sort
            //
            
            /*
             * ORDER BY (sortField) 
             */
            statement.append(sortQuery(sort, true));
            
            /*
             * LIMIT ?, ? 
             */
            if (hasValidLIMIT && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                statement.append(" LIMIT ").append(offset).append(',').append(limit);
            }

            /**********************************************************/
            /* Above here: build statement, below here bind params */
            /**********************************************************/

            /*
             * Create the statement and bind all our parameters!
             */
            if (sLog.isDebugEnabled())
                sLog.debug("SQL: ("+numParams+" parameters): "+statement.toString());
            
            long startTime = LC.zimbra_slow_logging_enabled.booleanValue() ? System.currentTimeMillis() : 0;
            
            stmt = conn.prepareStatement(statement.toString());
            int param = 1;
            
            
            if (hasMailItemOnlyConstraints) {
                param = setSearchVars(stmt, node, param, (hasAppointmentTableConstraints ? APPOINTMENT_TABLE_TYPES : null), false);
            }
            
            if (hasAppointmentTableConstraints) {
                param = setSearchVars(stmt, node, param, APPOINTMENT_TABLE_TYPES, true);
            }
            
            /*
             * Limit query if DB doesn't support LIMIT clause
             */
            if (hasValidLIMIT && !Db.supports(Db.Capability.LIMIT_CLAUSE))
                stmt.setMaxRows(offset + limit + 1);

            long prepTime = startTime > 0 ? System.currentTimeMillis() - startTime : 0;
            
            /*
             * EXECUTE!
             */
            assert(param == numParams+1);
            rs = stmt.executeQuery();
            
            long execTime = startTime > 0 ? System.currentTimeMillis() - startTime - prepTime : 0;
            
            /*
             * Return results
             */
            while (rs.next()) {
                if (hasValidLIMIT && !Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                    if (offset-- > 0)
                        continue;
                    if (limit-- <= 0)
                        break;
                }
                result.add(SearchResult.createResult(rs, sort, extra, inDumpster));
            }
            
            long fetchTime = startTime > 0 ? System.currentTimeMillis() - startTime - prepTime - execTime: 0;
            if (prepTime + execTime + fetchTime > LC.zimbra_slow_logging_threshold.longValue()) {
                sLog.warn("Slow SQL (start=%d prep=%d exec=%d fetch=%d rows=%d):\n" + statement.toString(),
                        startTime, prepTime, execTime, fetchTime, result.size());
            }
            
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("fetching search metadata", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    private static final int setBytes(PreparedStatement stmt, int param, byte[] c) throws SQLException {
        if (c != null && c.length > 0) {
            for (byte b: c)
                stmt.setByte(param++, b);
        }
        return param;
    }

    private static final int setBytes(PreparedStatement stmt, int param, Collection<Byte> c) throws SQLException {
        if (!ListUtil.isEmpty(c)) {
            for (byte b: c)
                stmt.setByte(param++, b);
        }
        return param;
    }

    private static final int setIntegers(PreparedStatement stmt, int param, Collection<Integer> c) throws SQLException {
        if (!ListUtil.isEmpty(c)) {
            for (int i: c)
                stmt.setInt(param++, i);
        }
        return param;
    }
    
    private static final int setStrings(PreparedStatement stmt, int param, Collection<String> c) throws SQLException {
        if (!ListUtil.isEmpty(c)) {
            for (String s: c)
                stmt.setString(param++, s);
        }
        return param;
    }

    private static final int setDateRange(PreparedStatement stmt, int param, Collection<NumericRange> c) throws SQLException {
        if (!ListUtil.isEmpty(c)) {
            for (NumericRange date : c) { 
                if (date.lowest >= 1)
                    stmt.setInt(param++, (int) Math.min(date.lowest / 1000, Integer.MAX_VALUE)); 
                if (date.highest >= 1)
                    stmt.setInt(param++, (int) Math.min(date.highest / 1000, Integer.MAX_VALUE));
            }
        }
        return param;
    }

    private static final int setTimestampRange(PreparedStatement stmt, int param, Collection<NumericRange> c) throws SQLException {
        if (!ListUtil.isEmpty(c)) {
            for (NumericRange date : c) { 
                if (date.lowest >= 1)
                    stmt.setTimestamp(param++, new Timestamp(date.lowest));
                if (date.highest >= 1)
                    stmt.setTimestamp(param++, new Timestamp(date.highest));
            }
        }
        return param;
    }

    private static final int setLongRangeWithMinimum(PreparedStatement stmt, int param, Collection<NumericRange> c, int minimum) throws SQLException {
        if (!ListUtil.isEmpty(c)) {
            for (NumericRange r : c) { 
                if (r.lowest >= minimum)
                    stmt.setLong(param++, r.lowest);
                if (r.highest >= minimum)
                    stmt.setLong(param++, r.highest);
            }
        }
        return param;
    }

    private static final int setIntRangeWithMinimum(PreparedStatement stmt, int param, Collection<NumericRange> c, int minimum) throws SQLException {
        if (!ListUtil.isEmpty(c)) {
            for (NumericRange r : c) { 
                if (r.lowest >= minimum)
                    stmt.setInt(param++, (int)r.lowest);
                if (r.highest >= minimum)
                    stmt.setInt(param++, (int)r.highest);
            }
        }
        return param;
    }

    private static final int setStringRange(PreparedStatement stmt, int param, Collection<StringRange> c) throws SQLException {
        if (!ListUtil.isEmpty(c)) {
            for (StringRange r: c) { 
                if (r.lowest != null) 
                    stmt.setString(param++, r.lowest.replace("\\\"", "\""));
                if (r.highest != null)
                    stmt.setString(param++, r.highest.replace("\\\"", "\""));
            }
        }
        return param;
    }

    private static final int setLongs(PreparedStatement stmt, int param, Collection<Long> c) throws SQLException {
        if (!ListUtil.isEmpty(c)) {
            for (long l: c)
                stmt.setLong(param++, l);
        }
        return param;
    }

    private static final int setFolders(PreparedStatement stmt, int param, Collection<Folder> c) throws SQLException {
        if (!ListUtil.isEmpty(c)) {
            for (Folder f : c) 
                stmt.setInt(param++, f.getId());
        }
        return param;
    }

    private static final int setBooleanAsInt(PreparedStatement stmt, int param, Boolean b) throws SQLException {
        if (b != null) {
            stmt.setInt(param++, b.booleanValue() ? 1 : 0);
        }
        return param;
    }
    
    /**
     * @param statement
     * @param column
     * @param truthiness
     *           if FALSE then sense is reversed (!=) 
     * @return number of parameters bound (always 0 in this case)
     */
    private static final int encode(StringBuilder statement, String column, boolean truthiness) {
        statement.append(" AND ").append(column).append(truthiness ? " = ?" : " != ?");
        return 1;
    }

    /**
     * @param statement
     * @param column
     * @param truthiness
     *           if FALSE then sense is reversed (!=) 
     * @param o
     *            if NULL, this function is a NoOp, otherwise puts ? to bind one value
     * @return number of parameters bound
     */
    private static final int encode(StringBuilder statement, String column, boolean truthiness, Object o) {
        if (o != null) {
            statement.append(" AND ").append(column).append(truthiness ? " = ?" : " != ?");
            return 1;
        }
        return 0;
    }

    /**
     * @param statement
     * @param column
     * @param truthiness
     *           if FALSE then sense is reversed (!=) 
     * @param c
     * @return number of parameters bound
     */
    private static final int encode(StringBuilder statement, String column, boolean truthiness, Collection<?> c) {
        if (!ListUtil.isEmpty(c)) {
            statement.append(" AND ").append(DbUtil.whereIn(column, truthiness, c.size()));
            return c.size();
        }
        return 0;
    }
    
    /**
     * @param statement
     * @param column
     * @param truthiness
     *           if FALSE then sense is reversed (!=) 
     * @param c
     * @return number of parameters bound
     */
    private static final int encode(StringBuilder statement, String column, boolean truthiness, byte[] c) {
        if (c != null && c.length > 0) {
            statement.append(" AND ").append(DbUtil.whereIn(column, truthiness, c.length));
            return c.length;
        }
        return 0;
    }
    
    /**
     * @param statement
     * @param column
     * @param ranges
     * @param lowestValue
     * @return number of parameters bound
     */
    private static final int encodeRangeWithMinimum(StringBuilder statement, String column, Collection<? extends DbSearchConstraints.NumericRange> ranges, long lowestValue) {
        if (ListUtil.isEmpty(ranges))
            return 0;
        
        if (Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON) && isCaseSensitiveField(column) ) {
            column = "UPPER("+column+")";
        }

        int params = 0;
        for (DbSearchConstraints.NumericRange r : ranges) {
            boolean lowValid = r.lowest >= lowestValue;
            boolean highValid = r.highest >= lowestValue;
            if (!(lowValid || highValid))
                continue;

            statement.append(r.negated ? " AND NOT (" : " AND (");
            if (lowValid) {
                if (r.lowestEqual)
                    statement.append(" " + column + " >= ?");
                else
                    statement.append(" " + column + " > ?");
                params++;
            }
            if (highValid) {
                if (lowValid)
                    statement.append(" AND");
                if (r.highestEqual)
                    statement.append(" " + column + " <= ?");
                else
                    statement.append(" " + column + " < ?");
                params++;
            }
            statement.append(')');
        }
        return params;
    }

    /**
     * @param statement
     * @param column
     * @param ranges
     * @return number of parameters bound
     */
    private static final int encodeRange(StringBuilder statement, String column, Collection<? extends DbSearchConstraints.StringRange> ranges) {
        int retVal = 0;
        
        if (Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON) && isCaseSensitiveField(column)) {
            column = "UPPER("+column+")";
        }
        
        if (!ListUtil.isEmpty(ranges)) {
            for (DbSearchConstraints.StringRange r : ranges) {
                statement.append(r.negated ? " AND NOT (" : " AND (");
                if (r.lowest != null) {
                    retVal++;
                    if (r.lowestEqual)
                        statement.append(" " + column + " >= ?");
                    else
                        statement.append(" " + column + " > ?");
                }
                if (r.highest != null) {
                    if (r.lowest != null)
                        statement.append(" AND");
                    retVal++;
                    if (r.highestEqual)
                        statement.append(" " + column + " <= ?");
                    else
                        statement.append(" " + column + " < ?");
                }
                statement.append(')');
            }
        }
        return retVal;
    }


    static class TagConstraints {
        Set<Long> searchTagsets;
        Set<Long> searchFlagsets;
        Boolean unread;
        boolean noMatches;

        static TagConstraints getTagConstraints(Mailbox mbox, DbSearchConstraints c, Connection conn) throws ServiceException {
            TagConstraints tc = c.tagConstraints = new TagConstraints();
            if (ListUtil.isEmpty(c.tags) && ListUtil.isEmpty(c.excludeTags))
                return tc;

            int setFlagMask = 0;
            long setTagMask = 0;

            if (!ListUtil.isEmpty(c.tags)) {
                for (Tag tag : c.tags) {
                    if (tag.getId() == Flag.ID_FLAG_UNREAD) {
                        tc.unread = Boolean.TRUE; 
                    } else if (tag instanceof Flag) {
                        setFlagMask |= tag.getBitmask();
                    } else {
                        setTagMask |= tag.getBitmask();
                    }
                }
            }

            int flagMask = setFlagMask;
            long tagMask = setTagMask;

            if (!ListUtil.isEmpty(c.excludeTags)) {
                for (Tag tag : c.excludeTags) {
                    if (tag.getId() == Flag.ID_FLAG_UNREAD) {
                        if (tc.unread == Boolean.TRUE)
                            tc.noMatches = true;
                        tc.unread = Boolean.FALSE;
                    } else if (tag instanceof Flag) {
                        if ((setFlagMask & tag.getBitmask()) != 0)
                            tc.noMatches = true;
                        flagMask |= tag.getBitmask();
                    } else {
                        if ((setTagMask & tag.getBitmask()) != 0)
                            tc.noMatches = true;
                        tagMask |= tag.getBitmask();
                    }
                }
            }

            // if we know we have no matches (e.g. "is:flagged and is:unflagged"), just stop here...
            if (tc.noMatches)
                return tc;

            TagsetCache tcFlags = DbMailItem.getFlagsetCache(conn, mbox);
            TagsetCache tcTags  = DbMailItem.getTagsetCache(conn, mbox);
            if (setTagMask != 0 || tagMask != 0) {
                // note that tcTags.getMatchingTagsets() returns null when *all* tagsets match
                tc.searchTagsets = tcTags.getMatchingTagsets(tagMask, setTagMask);
                // if no items match the specified tags...
                if (tc.searchTagsets != null && tc.searchTagsets.isEmpty()) {
                    tc.noMatches = true;
                    tc.searchTagsets = null; // otherwise we encode "tags IN()" which MySQL doesn't like
                }
            }

            if (setFlagMask != 0 || flagMask != 0) {
                // note that tcFlags.getMatchingTagsets() returns null when *all* flagsets match
                tc.searchFlagsets = tcFlags.getMatchingTagsets(flagMask, setFlagMask);
                // if no items match the specified flags...
                if (tc.searchFlagsets != null && tc.searchFlagsets.isEmpty()) {
                    tc.noMatches = true;
                    tc.searchFlagsets = null;  // otherwise we encode "flags IN()" which MySQL doesn't like
                }
            }

            return tc;
        }
    }
    
    private static int setSearchVars(PreparedStatement stmt, 
        DbSearchConstraintsNode node, int param, 
        byte[] calTypes, boolean inCalTable) throws SQLException {
        /*
         *( SUB-NODE AND/OR (SUB-NODE...) ) AND/OR ( SUB-NODE ) AND
         *    ( 
         *       one of: [type NOT IN (...)]  || [type = ?] || [type IN ( ...)]
         *       [ AND tags != 0]
         *       [ AND tags IN ( ... ) ]
         *       [ AND flags IN (...) ] 
         *       ..etc
         *    )   
         */
        
        DbSearchConstraintsNode.NodeType ntype = node.getNodeType();
        if (ntype == DbSearchConstraintsNode.NodeType.AND || ntype == DbSearchConstraintsNode.NodeType.OR) {
            for (DbSearchConstraintsNode subnode : node.getSubNodes())
                param = setSearchVars(stmt, subnode, param, calTypes, inCalTable);
            return param;
        }

        // we're here, so we must be in a DbSearchConstraints leaf node
        DbSearchConstraints c = node.getSearchConstraints();
        assert(ntype == DbSearchConstraintsNode.NodeType.LEAF && c != null);
        
        // if there are no possible matches, short-circuit here...
        if (c.automaticEmptySet() || c.tagConstraints.noMatches)
            return param;

        param = setBytes(stmt, param, c.types);
        param = setBytes(stmt, param, c.excludeTypes);
        param = setBytes(stmt, param, calTypes);
        
        param = setLongs(stmt, param, c.tagConstraints.searchTagsets);
        param = setLongs(stmt, param, c.tagConstraints.searchFlagsets);
        param = setBooleanAsInt(stmt, param, c.tagConstraints.unread);
        param = setFolders(stmt, param, c.folders);
        param = setFolders(stmt, param, c.excludeFolders);
        if (c.convId > 0)
            stmt.setInt(param++, c.convId);
        else
            param = setIntegers(stmt, param, c.prohibitedConvIds);
        param = setIntegers(stmt, param, c.itemIds);
        param = setIntegers(stmt, param, c.prohibitedItemIds);
        param = setStrings(stmt, param, c.indexIds);
        param = setDateRange(stmt, param, c.dates);
        param = setLongRangeWithMinimum(stmt, param, c.modified, 1);
        param = setLongRangeWithMinimum(stmt, param, c.modifiedContent, 1);
        param = setIntRangeWithMinimum(stmt, param, c.sizes, 0);
        param = setStringRange(stmt, param, c.subjectRanges);
        param = setStringRange(stmt, param, c.senderRanges);
        
        if (inCalTable) {
            param = setTimestampRange(stmt, param, c.calStartDates);
            param = setTimestampRange(stmt, param, c.calEndDates);
        }
        
        return param;
    }


    public static void main(String[] args) throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(1);

        DbSearchConstraints hasTags = new DbSearchConstraints();
        hasTags.hasTags = true;

        DbSearchConstraints inTrash = new DbSearchConstraints();
        Set<Folder> folders = new HashSet<Folder>();  folders.add(mbox.getFolderById(null, Mailbox.ID_FOLDER_TRASH));
        inTrash.folders = folders;

        DbSearchConstraints isUnread = new DbSearchConstraints();
        Set<Tag> tags = new HashSet<Tag>();  tags.add(mbox.getFlagById(Flag.ID_FLAG_UNREAD));
        isUnread.tags = tags;

        DbSearchConstraintsInnerNode orClause = DbSearchConstraintsInnerNode.OR();
        orClause.addSubNode(hasTags);
        DbSearchConstraintsInnerNode andClause = DbSearchConstraintsInnerNode.AND();
        andClause.addSubNode(inTrash);
        andClause.addSubNode(isUnread);
        orClause.addSubNode(andClause);

        // "is:unread" (first 5 results)
        //System.out.println(search(new ArrayList<DbSearch.XSearchResult>(), DbPool.getConnection(), isUnread, 1, DEFAULT_SORT_ORDER, 0, 5, DbSearch.XSearchResult.ExtraData.NONE));
        // "has:tags or (in:trash is:unread)" (first 5 results)
        //System.out.println(search(new ArrayList<DbSearch.XSearchResult>(), DbPool.getConnection(), orClause, 1, DEFAULT_SORT_ORDER, 0, 5, DbSearch.XSearchResult.ExtraData.NONE));
    }
}
