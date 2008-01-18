/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.db.DbSearchConstraints.NumericRange;
import com.zimbra.cs.db.DbSearchConstraints.StringRange;
import com.zimbra.cs.imap.ImapMessage;
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
        public int    indexId;
        public byte   type;
        public Object sortkey;
        public Object extraData;

        public enum ExtraData { NONE, MAIL_ITEM, IMAP_MSG, MODSEQ };

        public static class SizeEstimate {
            public SizeEstimate() {}
            public SizeEstimate(int initialval) { mSizeEstimate = initialval; }
            public int mSizeEstimate;
        }


        public static SearchResult createResult(ResultSet rs, byte sort) throws SQLException {
            return createResult(rs, sort, ExtraData.NONE);
        }

        public static SearchResult createResult(ResultSet rs, byte sort, ExtraData extra) throws SQLException {
            int sortField = (sort & SORT_FIELD_MASK);

            SearchResult result = new SearchResult();
            result.id      = rs.getInt(COLUMN_ID);
            result.indexId = rs.getInt(COLUMN_INDEXID);
            result.type    = rs.getByte(COLUMN_TYPE);
            switch (sortField) {
                case SORT_BY_SUBJECT:
                case SORT_BY_SENDER:
                case SORT_BY_NAME:
                case SORT_BY_NAME_NATURAL_ORDER:
                    result.sortkey = rs.getString(COLUMN_SORTKEY);
                    break;
                default:
                    result.sortkey = new Long(rs.getInt(COLUMN_SORTKEY) * 1000L);
                    break;
            }

            if (extra == ExtraData.MAIL_ITEM) {
                result.extraData = DbMailItem.constructItem(rs, COLUMN_SORTKEY);
            } else if (extra == ExtraData.IMAP_MSG) {
                int flags = rs.getBoolean(6) ? Flag.BITMASK_UNREAD | rs.getInt(7) : rs.getInt(7);
                result.extraData = new ImapMessage(result.id, result.type, rs.getInt(5), flags, rs.getLong(8));
            } else if (extra == ExtraData.MODSEQ) {
                result.extraData = rs.getInt(5);
            }
            return result;
        }

        public String toString() {
            return sortkey + " => (" + id + "," + type + ")";
        }

        public int hashCode() {
            return id;
        }

        public boolean equals(Object obj) {
            SearchResult other = (SearchResult) obj;
            return other.id == id;
        }
    }


    public static final byte SORT_DESCENDING = 0x00;
    public static final byte SORT_ASCENDING  = 0x01;

    public static final byte SORT_BY_DATE    = 0x00;
    public static final byte SORT_BY_SENDER  = 0x02;
    public static final byte SORT_BY_SUBJECT = 0x04;
    public static final byte SORT_BY_ID      = 0x08;
    public static final byte SORT_NONE       = 0x10;
    public static final byte SORT_BY_NAME    = 0x20;
    public static final byte SORT_BY_NAME_NATURAL_ORDER = 0x40;  // natural order.  see MailItem.java for implementation

    public static final byte DEFAULT_SORT_ORDER = SORT_BY_DATE | SORT_DESCENDING;

    public static final byte SORT_DIRECTION_MASK = 0x01;
    public static final byte SORT_FIELD_MASK     = 0x6E;
    
    // alias the sort column b/c of ambiguity problems (the sort column is included twice in the 
    // result set, and MySQL chokes on the ORDER BY when we do a UNION query (doesn't know
    // which 2 of the 4 sort columns are the "right" ones to use)
    public static final String SORT_COLUMN_ALIAS = "sortcol";

    static String sortField(byte sort, boolean useAlias) {
        String str;
        boolean stringVal = false;
        switch (sort & SORT_FIELD_MASK) {
            case SORT_BY_SENDER:   str = "mi.sender";   stringVal = true;  break;
            case SORT_BY_SUBJECT:  str = "mi.subject";  stringVal = true;  break;
            case SORT_BY_NAME_NATURAL_ORDER:
            case SORT_BY_NAME:     str = "mi.name";     stringVal = true;  break;
            case SORT_BY_ID:       str = "mi.id";    break;
            case SORT_NONE:        str = "NULL";     break;
            case SORT_BY_DATE:
            default:               str = "mi.date";  break; 
        }

        if (stringVal && Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON)) 
            str = "UPPER(" + str + ")";

        return useAlias ? str + " AS " + SORT_COLUMN_ALIAS : str;
    }

    static String sortQuery(byte sort) {
        return sortQuery(sort, false);
    }

    static String sortQuery(byte sort, boolean useAlias) {
        if (sort == SORT_NONE)
            return "";

        StringBuilder statement = new StringBuilder(" ORDER BY ");
        statement.append(useAlias ? SORT_COLUMN_ALIAS : sortField(sort, useAlias));
        if ((sort & SORT_DIRECTION_MASK) == SORT_DESCENDING)
            statement.append(" DESC");
        return statement.toString();
    }


    public static int countResults(Connection conn, DbSearchConstraintsNode node, Mailbox mbox) throws ServiceException {
        int mailboxId = mbox.getId();
        // Assemble the search query
        StringBuilder statement = new StringBuilder("SELECT count(*) ");
        statement.append(" FROM " + DbMailItem.getMailItemTableName(mbox, " mi"));
        statement.append(" WHERE ");
        statement.append("mailbox_id = ? AND ");
        int num = 1;
        
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            num += encodeConstraint(mbox, node, null, false, statement, conn);

            stmt = conn.prepareStatement(statement.toString());
            int param = 1;
            stmt.setInt(param++, mailboxId);
            param = setSearchVars(stmt, node, param, null, false);

            // FIXME: include COLLATION for sender/subject sort

            if (sLog.isDebugEnabled())
                sLog.debug("SQL: " + statement);

            assert(param == num+1); 
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

    private static String getForceIndexClause(DbSearchConstraintsNode node, byte sortInfo, boolean hasLimit) {
        if (LC.search_disable_database_hints.booleanValue())
            return NO_HINT;

        int sortBy = sortInfo & SORT_FIELD_MASK;
        if (sortBy == SORT_NONE)
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
            } else if (sortBy == SORT_BY_DATE && hasLimit) {
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

    private static final String encodeSelect(Mailbox mbox,
        byte sort, SearchResult.ExtraData extra, boolean includeCalTable,
        DbSearchConstraintsNode node, boolean validLIMIT) {
        /*
         * "SELECT mi.id,mi.date, [extrafields] FROM mail_item AS mi [, appointment AS ap]
         *    [FORCE INDEX (...)]
         *    WHERE mi.mailboxid=? [AND ap.mailboxId=? AND mi.id = ap.id ] AND
         * 
         *  If you change the first for parameters, you must change the COLUMN_* values above!
         */
        StringBuilder select = new StringBuilder("SELECT mi.id, mi.index_id, mi.type, " + sortField(sort, true));
        if (extra == SearchResult.ExtraData.MAIL_ITEM)
            select.append(", " + DbMailItem.DB_FIELDS);
        else if (extra == SearchResult.ExtraData.IMAP_MSG)
            select.append(", mi.imap_id, mi.unread, mi.flags, mi.tags");
        else if (extra == SearchResult.ExtraData.MODSEQ)
            select.append(", mi.mod_metadata");

        select.append(" FROM " + DbMailItem.getMailItemTableName(mbox, "mi"));
        if (includeCalTable) 
            select.append(", ").append(DbMailItem.getCalendarItemTableName(mbox, "ap"));
        
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
        if (c.automaticEmptySet()) {
            encodeBooleanValue(statement, false); 
            return num;
        }
        
        statement.append('(');

        // special-case this one, since there can't be a leading AND here...
        if (ListUtil.isEmpty(c.types)) {
            statement.append("type NOT IN " + DbMailItem.NON_SEARCHABLE_TYPES);
        } else {
            statement.append("type IN ").append(DbUtil.suitableNumberOfVariables(c.types));
            num += c.types.size();
        }
        
        num += encode(statement, "mi.type", false, c.excludeTypes);
        num += encode(statement, "mi.type", inCalTable, calTypes);

        // Determine the set of matching tags
        TagConstraints tc = TagConstraints.getTagContraints(mbox, c, conn);
        if (tc.noMatches)
            encodeBooleanValue(statement, false);

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

    public static Collection<SearchResult> search(Connection conn, DbSearchConstraints c) throws ServiceException {
        return search(new ArrayList<SearchResult>(), conn, c, SearchResult.ExtraData.NONE);
    }

    public static Collection<SearchResult> search(Connection conn, DbSearchConstraints c, SearchResult.ExtraData extra) throws ServiceException {
        return search(new ArrayList<SearchResult>(), conn, c, extra);
    }

    public static Collection<SearchResult> search(Collection<SearchResult> result, Connection conn, DbSearchConstraints c) throws ServiceException {
        return search(result, conn, c, SearchResult.ExtraData.NONE);
    }

    public static Collection<SearchResult> search(Collection<SearchResult> result, Connection conn, DbSearchConstraints c, SearchResult.ExtraData extra) throws ServiceException {
        return search(result, conn, c, c.mailbox, c.sort, c.offset, c.limit, extra);
    }

    public static Collection<SearchResult> search(Collection<SearchResult> result, 
        Connection conn, DbSearchConstraintsNode node, Mailbox mbox, 
        byte sort, int offset, int limit, SearchResult.ExtraData extra) throws ServiceException {

        boolean hasValidLIMIT = offset >= 0 && limit >= 0;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        StringBuilder statement = new StringBuilder();
        int numParams = 0;
        boolean hasMailItemOnlyConstraints = true;
        boolean hasAppointmentTableConstraints = hasAppointmentTableConstraints(node);
        if (hasAppointmentTableConstraints) {
            hasMailItemOnlyConstraints = hasMailItemOnlyConstraints(node);
        }
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
                statement.append(encodeSelect(mbox, sort, extra, false, node, hasValidLIMIT));
                
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
                statement.append(encodeSelect(mbox, sort, extra, true, node, hasValidLIMIT));
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

            /*
             * EXECUTE!
             */
            assert(param == numParams+1);
            rs = stmt.executeQuery();
            
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
                result.add(SearchResult.createResult(rs, sort, extra));
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
                    stmt.setString(param++, r.lowest);
                if (r.highest != null)
                    stmt.setString(param++, r.highest);
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
    
    private static final void encodeBooleanValue(StringBuilder statement, boolean truthiness) {
        if (truthiness) {
            if (Db.supports(Db.Capability.BOOLEAN_DATATYPE)) {
                statement.append(" AND TRUE");
            } else {
                statement.append(" AND 1=1");
            }
        } else {
            if (Db.supports(Db.Capability.BOOLEAN_DATATYPE)) {
                statement.append(" AND FALSE");
            } else {
                statement.append(" AND 0=1");
            }
        }
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
            statement.append(" AND ").append(column);
            if (truthiness)
                statement.append(" IN");
            else
                statement.append(" NOT IN");
            statement.append(DbUtil.suitableNumberOfVariables(c));
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
            statement.append(" AND ").append(column);
            if (truthiness)
                statement.append(" IN");
            else
                statement.append(" NOT IN");
            statement.append(DbUtil.suitableNumberOfVariables(c));
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

        static TagConstraints getTagContraints(Mailbox mbox, DbSearchConstraints c, Connection conn) throws ServiceException {
            TagConstraints tc = c.tagConstraints = new TagConstraints();
            if (ListUtil.isEmpty(c.tags) && ListUtil.isEmpty(c.excludeTags))
                return tc;

            int setFlagMask = 0;
            long setTagMask = 0;

            if (!ListUtil.isEmpty(c.tags)) {
                for (Tag curTag : c.tags) {
                    if (curTag.getId() == Flag.ID_FLAG_UNREAD) {
                        tc.unread = Boolean.TRUE; 
                    } else if (curTag instanceof Flag) {
                        setFlagMask |= curTag.getBitmask();
                    } else {
                        setTagMask |= curTag.getBitmask();
                    }
                }
            }

            int flagMask = setFlagMask;
            long tagMask = setTagMask;

            if (!ListUtil.isEmpty(c.excludeTags)) {
                for (Tag t : c.excludeTags) {
                    if (t.getId() == Flag.ID_FLAG_UNREAD) {
                        tc.unread = Boolean.FALSE;
                    } else if (t instanceof Flag) {
                        flagMask |= t.getBitmask();
                    } else {
                        tagMask |= t.getBitmask();
                    }
                }
            }

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
        if (c.automaticEmptySet())
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
        param = setIntegers(stmt, param, c.indexIds);
        param = setDateRange(stmt, param, c.dates);
        param = setLongRangeWithMinimum(stmt, param, c.modified, 1);
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
        Set<Tag> tags = new HashSet<Tag>();  tags.add(mbox.mUnreadFlag);
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
