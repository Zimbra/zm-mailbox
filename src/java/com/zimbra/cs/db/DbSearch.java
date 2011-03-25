/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
import java.util.List;
import java.util.Set;

import com.google.common.base.Objects;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.DbConnection;
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
import com.zimbra.cs.mailbox.Tag;

/**
 * Search related DAO.
 *
 * @author dkarp
 * @author tim
 * @author ysasaki
 */
public final class DbSearch {
    private static final int COLUMN_ID = 1;
    private static final int COLUMN_INDEXID = 2;
    private static final int COLUMN_TYPE = 3;

    /**
     * Alias the sort column b/c of ambiguity problems (the sort column is included twice in the result set, and MySQL
     * chokes on the ORDER BY when we do a UNION query (doesn't know which 2 of the 4 sort columns are the "right" ones
     * to use).
     */
    private static final String SORT_COLUMN_ALIAS = "sortcol";

    // Indexes on mail_item table
    private static final String MI_I_MBOX_FOLDER_DATE = "i_folder_id_date";
    private static final String MI_I_MBOX_PARENT = "i_parent_id";
    private static final String MI_I_MBOX_INDEX = "i_index_id";

    private static final String NO_HINT = "";

    public enum FetchMode { ID, MAIL_ITEM, IMAP_MSG, MODSEQ, PARENT, MODCONTENT };

    /**
     * Returns true if this field is case-sensitive for search/sort, i.e. whether or not we need to do an UPPER() on it
     * in places.
     */
    private static boolean isCaseSensitiveField(String fieldName) {
        // we need to handle things like "mi.sender" for the sender column, etc
        // so look for the last . in the fieldname, return the string after that.
        String colNameAfterPeriod;
        int periodOffset = fieldName.lastIndexOf('.');
        if (periodOffset <= 0 && periodOffset < (fieldName.length() + 1)) {
            colNameAfterPeriod = fieldName;
        } else {
            colNameAfterPeriod = fieldName.substring(periodOffset + 1);
        }
        return (colNameAfterPeriod.equals("sender") || colNameAfterPeriod.equals("subject") ||
                colNameAfterPeriod.equals("name"));
    }

    private static String toSortField(SortBy sort) {
        String result;
        boolean str = false;
        switch (sort.getCriterion()) {
            case NONE:
                return null;
            case SENDER:
                result = "mi.sender";
                str = true;
                break;
            case SUBJECT:
                result = "mi.subject";
                str = true;
                break;
            case NAME_NATURAL_ORDER:
            case NAME:
                result = "mi.name";
                str = true;
                break;
            case ID:
                result = "mi.id";
                break;
            case SIZE:
                result = "mi.size";
                break;
            case DATE:
            default:
                result = "mi.date";
                break;
        }

        if (str && Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON)) {
            result = "UPPER(" + result + ")";
        }

        //TODO Db.Capability.REQUEST_UTF8_UNICODE_COLLATION

        return result;
    }

    /**
     * Generate a column-reference for the sort-by column.
     */
    private static void addSortColumn(StringBuilder stmt, SortBy sort) {
        String field = toSortField(sort);
        if (field == null) { // no sort column for NONE
            return;
        }
        stmt.append(", ").append(field).append(" AS ").append(SORT_COLUMN_ALIAS);
    }

    /**
     * Generate the ORDER BY part that goes at the end of the SELECT.
     */
    static String orderBy(SortBy sort, boolean alias) {
        if (sort.getCriterion() == SortCriterion.NONE) { // no ORDER BY for NONE
            return "";
        }
        return " ORDER BY " + (alias ? SORT_COLUMN_ALIAS : toSortField(sort)) +
            (sort.getDirection() == SortDirection.DESCENDING ? " DESC" : "");
    }

    public static int countResults(DbConnection conn, DbSearchConstraintsNode node, Mailbox mbox, boolean inDumpster)
            throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        // Assemble the search query
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ");
        sql.append(DbMailItem.getMailItemTableName(mbox, "mi", inDumpster));
        if (node.getSearchConstraints().fromContact != null) {
            sql.append(", ").append(DbMailAddress.getTableName(mbox)).append(" AS ma");
        }

        sql.append(" WHERE ").append(DbMailItem.IN_THIS_MAILBOX_AND);
        int num = DebugConfig.disableMailboxGroups ? 0 : 1;
        num += encodeConstraint(sql, conn, mbox, node, null, false);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql.toString());
            int pos = 1;
            pos = DbMailItem.setMailboxId(stmt, mbox, pos);
            pos = setSearchVars(stmt, node, pos, null, false);
            assert(pos == num + 1);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw ServiceException.FAILURE("Failed to count DB search results", null);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to count DB search results", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

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
                if (constraints.getOnlyFolder() != null) {
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

    private static final StringBuilder encodeSelect(Mailbox mbox, StringBuilder out, SortBy sort, FetchMode fetch,
            boolean includeCalTable, DbSearchConstraintsNode node, boolean validLIMIT, boolean inDumpster) {
        // SELECT mi.id, ...
        // If you change the first for parameters, you must change the COLUMN_* constants.
        out.append("SELECT ");
        switch (fetch) {
            case ID:
                out.append("mi.id, mi.index_id, mi.type");
                break;
            case MAIL_ITEM:
                out.append(DbMailItem.DB_FIELDS);
                break;
            case IMAP_MSG:
                out.append("mi.id, mi.index_id, mi.type, mi.imap_id, mi.unread, mi.flags, mi.tags");
                break;
            case MODSEQ:
                out.append("mi.id, mi.index_id, mi.type, mi.mod_metadata");
                break;
            case PARENT:
                out.append("mi.id, mi.index_id, mi.type, mi.parent_id");
                break;
            case MODCONTENT:
                out.append("mi.id, mi.index_id, mi.type, mi.mod_content");
                break;
        }
        addSortColumn(out, sort);

        // FROM mail_item AS mi FORCE INDEX (...) [, mail_address AS ma] [, appointment AS ap]
        out.append(" FROM ").append(DbMailItem.getMailItemTableName(mbox, "mi", inDumpster));
        out.append(getForceIndexClause(node, sort, validLIMIT));
        if (includeCalTable) {
            out.append(", ").append(DbMailItem.getCalendarItemTableName(mbox, "ap", inDumpster));
        }
        if (node.getSearchConstraints().fromContact != null) {
            out.append(", ").append(DbMailAddress.getTableName(mbox)).append(" AS ma");
        }

        // WHERE mi.mailboxId=? [AND ap.mailboxId=? AND mi.id = ap.id ] AND "
        out.append(" WHERE ");
        out.append(DbMailItem.getInThisMailboxAnd(mbox.getId(), "mi", includeCalTable ? "ap" : null));
        if (includeCalTable) {
            out.append(" mi.id = ap.item_id AND ");
        }
        return out;
    }

    private static final int encodeConstraint(StringBuilder out, DbConnection conn, Mailbox mbox,
            DbSearchConstraintsNode node, byte[] calTypes, boolean inCalTable) throws ServiceException {
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
            out.append('(');
            for (DbSearchConstraintsNode subnode : node.getSubNodes()) {
                if (!first) {
                    out.append(and ? " AND " : " OR ");
                }
                num += encodeConstraint(out, conn, mbox, subnode, calTypes, inCalTable);
                first = false;
            }
            out.append(") ");
            return num;
        }

        // we're here, so we must be in a DbSearchConstraints leaf node
        DbSearchConstraints constraint = node.getSearchConstraints();
        assert(ntype == DbSearchConstraintsNode.NodeType.LEAF && constraint != null);
        constraint.checkDates();

        // if there are no possible matches, short-circuit here...
        TagConstraints tc = TagConstraints.getTagConstraints(mbox, constraint, conn);
        if (constraint.automaticEmptySet() || tc.noMatches) {
            out.append(Db.supports(Db.Capability.BOOLEAN_DATATYPE) ? "FALSE" : "0=1");
            return num;
        }

        out.append('(');

        // special-case this one, since there can't be a leading AND here...
        if (ListUtil.isEmpty(constraint.types)) {
            out.append("type NOT IN " + DbMailItem.NON_SEARCHABLE_TYPES);
        } else {
            out.append(DbUtil.whereIn("type", constraint.types.size()));
            num += constraint.types.size();
        }

        num += encode(out, "mi.type", false, constraint.excludeTypes);
        num += encode(out, "mi.type", inCalTable, calTypes);

        // if hasTags is NULL then nothing
        // if hasTags is TRUE then !=0
        // if hasTags is FALSE then = 0
        if (constraint.hasTags != null) {
            if (constraint.hasTags.booleanValue()) {
                out.append(" AND mi.tags != 0");
            } else {
                out.append(" AND mi.tags = 0");
            }
        }

        num += encode(out, "mi.tags", true, tc.searchTagsets);
        num += encode(out, "mi.flags", true, tc.searchFlagsets);
        num += encode(out, "unread", true, tc.unread);
        num += encode(out, "mi.folder_id", true, constraint.folders);
        num += encode(out, "mi.folder_id", false, constraint.excludeFolders);
        if (constraint.convId > 0) {
            num += encode(out, "mi.parent_id", true);
        } else {
            num += encode(out, "mi.parent_id", false, constraint.prohibitedConvIds);
        }
        num += encode(out, "mi.id", true, constraint.itemIds);
        num += encode(out, "mi.id", false, constraint.prohibitedItemIds);
        num += encode(out, "mi.index_id", true, constraint.indexIds);
        num += encodeRangeWithMinimum(out, "mi.date", constraint.dates, 1);
        num += encodeRangeWithMinimum(out, "mi.mod_metadata", constraint.modified, 1);
        num += encodeRangeWithMinimum(out, "mi.mod_content", constraint.modifiedContent, 1);
        num += encodeRangeWithMinimum(out, "mi.size", constraint.sizes, 0);
        num += encodeRange(out, "mi.subject", constraint.subjectRanges);
        num += encodeRange(out, "mi.sender", constraint.senderRanges);

        Boolean isSoloPart = node.getSearchConstraints().getIsSoloPart();
        if (isSoloPart != null) {
            if (isSoloPart.booleanValue()) {
                out.append(" AND mi.parent_id is NULL ");
            } else {
                out.append(" AND mi.parent_id is NOT NULL ");
            }
        }

        if (constraint.hasIndexId != null) {
            if (constraint.hasIndexId.booleanValue()) {
                out.append(" AND mi.index_id is NOT NULL ");
            } else {
                out.append(" AND mi.index_id is NULL ");
            }
        }

        if (inCalTable) {
            num += encodeRangeWithMinimum(out, "ap.start_time", constraint.calStartDates, 1);
            num += encodeRangeWithMinimum(out, "ap.end_time", constraint.calEndDates, 1);
        }

        if (constraint.fromContact != null) {
            if (constraint.fromContact) {
                out.append(" AND mi.sender_id = ma.id AND ma.contact_count > 0");
            } else {
                out.append(" AND mi.sender_id = ma.id AND ma.contact_count = 0");
            }
        }

        out.append(')');

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

    static final byte[] APPOINTMENT_TABLE_TYPES = new byte[] {
        MailItem.Type.APPOINTMENT.toByte(), MailItem.Type.TASK.toByte()
    };

    public static List<Result> search(DbConnection conn, Mailbox mbox, DbSearchConstraintsNode node, SortBy sort,
            int offset, int limit, FetchMode fetch, boolean inDumpster) throws ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        if (!Db.supports(Db.Capability.AVOID_OR_IN_WHERE_CLAUSE) ||
                (sort.getCriterion() != SortCriterion.DATE && sort.getCriterion() != SortCriterion.SIZE) ||
                NodeType.OR != node.getNodeType()) {
            try {
                return searchInternal(conn, mbox, node, sort, offset, limit, fetch, inDumpster);
            } catch (SQLException e) {
                if (Db.errorMatches(e, Db.Error.TOO_MANY_SQL_PARAMS)) {
                    ZimbraLog.sqltrace.debug("Too many SQL params: %s", node, e); // fall back to splitting OR clauses
                } else {
                    throw ServiceException.FAILURE("Failed to search", e);
                }
            }
        }
        // if (where a or b) not supported or if we encountered too many sql params try splitting
        // run each toplevel ORed part as a separate SQL query, then merge the results in memory
        List<Result> result = new ArrayList<Result>();
        for (DbSearchConstraintsNode sub : node.getSubNodes()) {
            result.addAll(search(conn, mbox, sub, sort, offset, limit, fetch, inDumpster));
        }
        Collections.sort(result, new ResultComparator(sort));;
        return result;
    }

    private static List<Result> searchInternal(DbConnection conn, Mailbox mbox, DbSearchConstraintsNode node,
            SortBy sort, int offset, int limit, FetchMode fetch, boolean inDumpster)
            throws SQLException, ServiceException {
        assert(Db.supports(Db.Capability.ROW_LEVEL_LOCKING) || Thread.holdsLock(mbox));

        boolean hasValidLIMIT = offset >= 0 && limit >= 0;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        StringBuilder sql = new StringBuilder();
        int numParams = 0;
        boolean hasMailItemOnlyConstraints = true;
        boolean hasAppointmentTableConstraints = hasAppointmentTableConstraints(node);
        if (hasAppointmentTableConstraints)
            hasMailItemOnlyConstraints = hasMailItemOnlyConstraints(node);
        boolean requiresUnion = hasMailItemOnlyConstraints && hasAppointmentTableConstraints;

        try {
            if (hasMailItemOnlyConstraints) {
                if (requiresUnion) {
                    sql.append("(");
                }

                // SELECT mi.id,... FROM mail_item AS mi [FORCE INDEX (...)] WHERE mi.mailboxid=? AND
                encodeSelect(mbox, sql, sort, fetch, false, node, hasValidLIMIT, inDumpster);

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
                numParams += encodeConstraint(sql, conn, mbox, node,
                        hasAppointmentTableConstraints ? APPOINTMENT_TABLE_TYPES : null, false);

                if (requiresUnion) {
                    sql.append(orderBy(sort, true));
                    // LIMIT ?, ?
                    if (hasValidLIMIT && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                        sql.append(" LIMIT ").append(offset).append(',').append(limit);
                    }
                }
            }

            if (requiresUnion) {
                sql.append(" ) UNION ALL (");
            }

            if (hasAppointmentTableConstraints) {
                // SELECT...again...(this time with "appointment as ap")...WHERE...
                encodeSelect(mbox, sql, sort, fetch, true, node, hasValidLIMIT, inDumpster);
                numParams += encodeConstraint(sql, conn, mbox, node, APPOINTMENT_TABLE_TYPES, true);

                if (requiresUnion) {
                    sql.append(orderBy(sort, true));
                    // LIMIT ?, ?
                    if (hasValidLIMIT && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                        sql.append(" LIMIT ").append(offset).append(',').append(limit);
                    }
                    if (requiresUnion) {
                        sql.append(")");
                    }
                }
            }

            // TODO FIXME: include COLLATION for sender/subject sort
            sql.append(orderBy(sort, true));

            // LIMIT ?, ?
            if (hasValidLIMIT && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                sql.append(" LIMIT ").append(offset).append(',').append(limit);
            }

            // Create the statement and bind all our parameters!
            stmt = conn.prepareStatement(sql.toString());
            int param = 1;


            if (hasMailItemOnlyConstraints) {
                param = setSearchVars(stmt, node, param, (hasAppointmentTableConstraints ? APPOINTMENT_TABLE_TYPES : null), false);
            }

            if (hasAppointmentTableConstraints) {
                param = setSearchVars(stmt, node, param, APPOINTMENT_TABLE_TYPES, true);
            }

            // Limit query if DB doesn't support LIMIT clause
            if (hasValidLIMIT && !Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                stmt.setMaxRows(offset + limit + 1);
            }

            assert(param == numParams + 1);
            rs = stmt.executeQuery();

            List<Result> result = new ArrayList<Result>();
            while (rs.next()) {
                if (hasValidLIMIT && !Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                    if (offset-- > 0) {
                        continue;
                    }
                    if (limit-- <= 0) {
                        break;
                    }
                }

                Object sortkey = getSortKey(rs, sort);
                switch (fetch) {
                    case ID:
                        result.add(new IdResult(rs, sortkey));
                        break;
                    case MAIL_ITEM:
                        result.add(new ItemDataResult(rs, sortkey, inDumpster));
                        break;
                    case IMAP_MSG:
                        result.add(new ImapResult(rs, sortkey));
                        break;
                    case MODSEQ:
                        result.add(new ModSeqResult(rs, sortkey));
                        break;
                    case MODCONTENT:
                        result.add(new ModContentResult(rs, sortkey));
                        break;
                    case PARENT:
                        result.add(new ParentResult(rs, sortkey));
                        break;
                    default:
                        assert false : fetch;
                }
            }

            return result;
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
        }
    }

    private static Object getSortKey(ResultSet rs, SortBy sort) throws SQLException {
        switch (sort.getCriterion()) {
            case SUBJECT:
            case SENDER:
            case NAME:
            case NAME_NATURAL_ORDER:
                return rs.getString(SORT_COLUMN_ALIAS);
            case SIZE:
                return new Long(rs.getInt(SORT_COLUMN_ALIAS));
            case NONE: // no sort column in the result set for NONE
                return null;
            default:
                return new Long(rs.getInt(SORT_COLUMN_ALIAS) * 1000L);
        }
    }

    private static final int setBytes(PreparedStatement stmt, int param, byte[] c) throws SQLException {
        if (c != null && c.length > 0) {
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

        static TagConstraints getTagConstraints(Mailbox mbox, DbSearchConstraints c, DbConnection conn) throws ServiceException {
            TagConstraints tc = c.tagConstraints = new TagConstraints();
            if (ListUtil.isEmpty(c.tags) && ListUtil.isEmpty(c.excludeTags))
                return tc;

            int setFlagMask = 0;
            long setTagMask = 0;

            if (!ListUtil.isEmpty(c.tags)) {
                for (Tag tag : c.tags) {
                    if (tag.getId() == Flag.ID_UNREAD) {
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
                    if (tag.getId() == Flag.ID_UNREAD) {
                        if (tc.unread == Boolean.TRUE) {
                            tc.noMatches = true;
                        }
                        tc.unread = Boolean.FALSE;
                    } else if (tag instanceof Flag) {
                        if ((setFlagMask & tag.getBitmask()) != 0) {
                            tc.noMatches = true;
                        }
                        flagMask |= tag.getBitmask();
                    } else {
                        if ((setTagMask & tag.getBitmask()) != 0) {
                            tc.noMatches = true;
                        }
                        tagMask |= tag.getBitmask();
                    }
                }
            }

            // if we know we have no matches (e.g. "is:flagged and is:unflagged"), just stop here...
            if (tc.noMatches) {
                return tc;
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

    private static int setSearchVars(PreparedStatement stmt, DbSearchConstraintsNode node, int param,
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

        for (MailItem.Type type : c.types) {
            stmt.setByte(param++, type.toByte());
        }
        for (MailItem.Type type : c.excludeTypes) {
            stmt.setByte(param++, type.toByte());
        }
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

    public static abstract class Result {
        private final Object sortkey;

        protected Result(Object sortkey) {
            assert (sortkey == null || sortkey instanceof String || sortkey instanceof Long) : sortkey;
            this.sortkey = sortkey;
        }

        public Object getSortKey() {
            return sortkey;
        }

        public abstract int getId();
        public abstract int getIndexId();
        public abstract MailItem.Type getType();

        public MailItem.UnderlyingData getItemData() {
            throw new IllegalStateException();
        }

        public MailItem getItem() {
            throw new IllegalStateException();
        }

        public ImapMessage getImapMessage() {
            throw new IllegalStateException();
        }

        public int getModSeq() {
            throw new IllegalStateException();
        }

        public int getModContent() {
            throw new IllegalStateException();
        }

        public int getParentId() {
            throw new IllegalStateException();
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                .add("id", getId())
                .add("type", getType())
                .add("sort", sortkey)
                .toString();
        }

        @Override
        public int hashCode() {
            return getId();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Result && ((Result) obj).getId() == getId();
        }
    }

    private static class IdResult extends Result {
        private final int id;
        private final int indexId;
        private final MailItem.Type type;

        IdResult(ResultSet rs, Object sortkey) throws SQLException {
            super(sortkey);
            id = rs.getInt(COLUMN_ID);
            indexId = getInt(rs, COLUMN_INDEXID);
            type = MailItem.Type.of(rs.getByte(COLUMN_TYPE));
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public int getIndexId() {
            return indexId;
        }

        @Override
        public MailItem.Type getType() {
            return type;
        }

        /**
         * Returns {@link ResultSet#getInt(int)} or -1 if the column is null.
         */
        int getInt(ResultSet rs, int offset) throws SQLException {
            int value = rs.getInt(offset);
            return rs.wasNull() ? -1 : value;
        }
    }

    /**
     * {@link MailItem.UnderlyingData} to {@link MailItem} conversion should be done in {@link Mailbox} layer because
     * DB layer is not aware of mailbox transaction.
     */
    private static final class ItemDataResult extends Result {
        private final MailItem.UnderlyingData udata;

        ItemDataResult(ResultSet rs, Object sortkey, boolean inDumpster) throws ServiceException, SQLException {
            super(sortkey);
            udata = DbMailItem.constructItem(rs, 0, inDumpster);
        }

        @Override
        public int getId() {
            return udata.id;
        }

        @Override
        public int getIndexId() {
            return udata.indexId;
        }

        @Override
        public MailItem.Type getType() {
            return MailItem.Type.of(udata.type);
        }

        @Override
        public MailItem.UnderlyingData getItemData() {
            return udata;
        }
    }

    private static final class ImapResult extends IdResult {
        private ImapMessage i4msg;

        ImapResult(ResultSet rs, Object sortkey) throws SQLException {
            super(rs, sortkey);
            int offset = COLUMN_TYPE;
            int flags = rs.getBoolean(offset + 2) ? Flag.BITMASK_UNREAD | rs.getInt(offset + 3) : rs.getInt(offset + 3);
            i4msg = new ImapMessage(getId(), getType(), rs.getInt(offset + 1), flags, rs.getLong(offset + 4));
        }

        @Override
        public ImapMessage getImapMessage() {
            return i4msg;
        }
    }

    private static final class ModSeqResult extends IdResult {
        private final int modseq;

        ModSeqResult(ResultSet rs, Object sortkey) throws SQLException {
            super(rs, sortkey);
            modseq = getInt(rs, COLUMN_TYPE + 1);
        }

        @Override
        public int getModSeq() {
            return modseq;
        }
    }

    private static final class ModContentResult extends IdResult {
        private final int modc;

        ModContentResult(ResultSet rs, Object sortkey) throws SQLException {
            super(rs, sortkey);
            modc = getInt(rs, COLUMN_TYPE + 1);
        }

        @Override
        public int getModContent() {
            return modc;
        }
    }

    private static final class ParentResult extends IdResult {
        private final int parentId;

        ParentResult(ResultSet rs, Object sortkey) throws SQLException {
            super(rs, sortkey);
            parentId = getInt(rs, COLUMN_TYPE + 1);
        }

        @Override
        public int getParentId() {
            return parentId;
        }
    }

    private static final class ResultComparator implements Comparator<Result> {
        private final SortBy sort;

        ResultComparator(SortBy sort) {
            this.sort = sort;
        }

        @Override
        public int compare(Result o1, Result o2) {
            switch (sort.getCriterion()) {
                case SIZE:
                case DATE:
                    long date1 = (Long) o1.getSortKey();
                    long date2 = (Long) o2.getSortKey();
                    if (date1 != date2) {
                        long diff;
                        if (sort.getDirection() == SortDirection.DESCENDING) {
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
                    throw new UnsupportedOperationException(sort.getCriterion().toString());
            }
            if (sort.getDirection() == SortDirection.DESCENDING) {
                return o2.getId() - o1.getId();
            } else {
                return o1.getId() - o2.getId();
            }
        }
    }

}
