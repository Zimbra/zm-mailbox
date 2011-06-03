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
import com.google.common.base.Strings;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.index.DbSearchConstraints;
import com.zimbra.cs.index.SortBy;
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
        switch (sort.getKey()) {
            case NONE:
                return null;
            case SENDER:
                return toStringSortField("mi.sender");
            case RCPT:
                return toStringSortField("mi.recipients");
            case SUBJECT:
                return toStringSortField("mi.subject");
            case NAME:
            case NAME_NATURAL_ORDER:
                return toStringSortField("mi.name");
            case ID:
                return "mi.id";
            case SIZE:
                return "mi.size";
            case ATTACHMENT: // 0 or 1
                return "CONCAT(SIGN(" + Db.getInstance().bitAND("mi.flags", String.valueOf(Flag.BITMASK_ATTACHED)) +
                    "), LPAD(mi.id, 10, '0'))";
            case FLAG: // 0 or 1
                return "CONCAT(SIGN(" + Db.getInstance().bitAND("mi.flags", String.valueOf(Flag.BITMASK_FLAGGED)) +
                    "), LPAD(mi.id, 10, '0'))";
            case PRIORITY: // 0 or 1 or 2
                return "CONCAT(1 + SIGN(" +
                    Db.getInstance().bitAND("mi.flags", String.valueOf(Flag.BITMASK_HIGH_PRIORITY)) + ") - SIGN(" +
                    Db.getInstance().bitAND("mi.flags", String.valueOf(Flag.BITMASK_LOW_PRIORITY)) +
                    "), LPAD(mi.id, 10, '0'))";
            case DATE:
            default:
                return "mi.date";
        }
    }

    private static String toStringSortField(String col) {
        return "CONCAT(" + (Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON) ? "UPPER(" + col + ")" : col) +
            ", LPAD(mi.id, 10, '0'))";
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
        if (sort.getKey() == SortBy.Key.NONE) { // no ORDER BY for NONE
            return "";
        }
        return " ORDER BY " + (alias ? SORT_COLUMN_ALIAS : toSortField(sort)) +
            (sort.getDirection() == SortBy.Direction.DESC ? " DESC" : "");
    }

    public static int countResults(DbConnection conn, DbSearchConstraints node, Mailbox mbox, boolean inDumpster)
            throws ServiceException {
        // Assemble the search query
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ");
        sql.append(DbMailItem.getMailItemTableName(mbox, "mi", inDumpster));
        if (node.toLeaf().fromContact != null) {
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

    private static String getForceIndexClause(DbSearchConstraints node, SortBy sort, boolean hasLimit) {
        if (LC.search_disable_database_hints.booleanValue()) {
            return NO_HINT;
        }
        if (!Db.supports(Db.Capability.FORCE_INDEX_EVEN_IF_NO_SORT) && sort.getKey() == SortBy.Key.NONE) {
            return NO_HINT;
        }
        String index = null;
        if (node instanceof DbSearchConstraints.Leaf) {
            DbSearchConstraints.Leaf constraints = node.toLeaf();
            if (!constraints.itemIds.isEmpty()) {
                return "";
            } else if (constraints.convId > 0) {
                index = MI_I_MBOX_PARENT;
            } else if (!constraints.indexIds.isEmpty()) {
                index = MI_I_MBOX_INDEX;
            } else if (sort.getKey() == SortBy.Key.DATE && hasLimit) {
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
            boolean includeCalTable, DbSearchConstraints node, boolean validLIMIT, boolean inDumpster) {
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
        if (node instanceof DbSearchConstraints.Leaf && node.toLeaf().fromContact != null) {
            out.append(", ").append(DbMailAddress.getTableName(mbox)).append(" AS ma");
        }

        // WHERE mi.mailboxId=? [AND ap.mailboxId=? AND mi.id = ap.id ] AND "
        out.append(" WHERE ");
        //TODO don't put a bare mailbox_id, but use a parameter
        out.append(DbMailItem.getInThisMailboxAnd(mbox.getId(), "mi", includeCalTable ? "ap" : null));
        if (includeCalTable) {
            out.append(" mi.id = ap.item_id AND ");
        }
        return out;
    }

    private static final int encodeConstraint(StringBuilder out, DbConnection conn, Mailbox mbox,
            DbSearchConstraints node, byte[] calTypes, boolean inCalTable) throws ServiceException {
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
        if (node instanceof DbSearchConstraints.Intersection || node instanceof DbSearchConstraints.Union) {
            boolean first = true;
            boolean and = node instanceof DbSearchConstraints.Intersection;
            out.append('(');
            for (DbSearchConstraints child : node.getChildren()) {
                if (!first) {
                    out.append(and ? " AND " : " OR ");
                }
                num += encodeConstraint(out, conn, mbox, child, calTypes, inCalTable);
                first = false;
            }
            out.append(") ");
            return num;
        }

        // we're here, so we must be in a DbSearchConstraints leaf node
        DbSearchConstraints.Leaf constraint = node.toLeaf();
        assert(node instanceof DbSearchConstraints.Leaf && constraint != null);
        constraint.validate();

        // if there are no possible matches, short-circuit here...
        TagConstraints tc = TagConstraints.getTagConstraints(mbox, constraint, conn);
        if (tc.noMatches) {
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
        if (constraint.cursorRange != null) {
            num += encodeRange(out, constraint.cursorRange);
        }
        num += encodeRange(out, "mi.date", constraint.dateRanges, 1);
        num += encodeRange(out, "mi.mod_metadata", constraint.modSeqRanges, 1);
        num += encodeRange(out, "mi.size", constraint.sizeRanges, 0);
        num += encodeRange(out, "mi.subject", constraint.subjectRanges);
        num += encodeRange(out, "mi.sender", constraint.senderRanges);

        Boolean isSoloPart = node.toLeaf().getIsSoloPart();
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
            num += encodeRange(out, "ap.start_time", constraint.calStartDateRanges, 1);
            num += encodeRange(out, "ap.end_time", constraint.calEndDateRanges, 1);
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
    private static final boolean hasMailItemOnlyConstraints(DbSearchConstraints node) {
        if (node instanceof DbSearchConstraints.Intersection || node instanceof DbSearchConstraints.Union) {
            for (DbSearchConstraints child : node.getChildren()) {
                if (hasMailItemOnlyConstraints(child)) {
                    return true;
                }
            }
            return false;
        }
        return node.toLeaf().hasNonAppointmentTypes();
    }

    /**
     * @return TRUE if this constraint needs to do a join with the Appointment table in order to be evaluated
     */
    private static final boolean hasAppointmentTableConstraints(DbSearchConstraints node) {
        if (node instanceof DbSearchConstraints.Intersection || node instanceof DbSearchConstraints.Union) {
            for (DbSearchConstraints child : node.getChildren()) {
                if (hasAppointmentTableConstraints(child)) {
                    return true;
                }
            }
            return false;
        }
        return node.toLeaf().hasAppointmentTableConstraints();
    }

    static final byte[] APPOINTMENT_TABLE_TYPES = new byte[] {
        MailItem.Type.APPOINTMENT.toByte(), MailItem.Type.TASK.toByte()
    };

    public static List<Result> search(DbConnection conn, Mailbox mbox, DbSearchConstraints node, SortBy sort,
            int offset, int limit, FetchMode fetch, boolean inDumpster) throws ServiceException {
        if (!Db.supports(Db.Capability.AVOID_OR_IN_WHERE_CLAUSE) ||
                (sort.getKey() != SortBy.Key.DATE && sort.getKey() != SortBy.Key.SIZE) ||
                !(node instanceof DbSearchConstraints.Union)) {
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
        for (DbSearchConstraints child : node.getChildren()) {
            result.addAll(search(conn, mbox, child, sort, offset, limit, fetch, inDumpster));
        }
        Collections.sort(result, new ResultComparator(sort));;
        return result;
    }

    private static List<Result> searchInternal(DbConnection conn, Mailbox mbox, DbSearchConstraints node,
            SortBy sort, int offset, int limit, FetchMode fetch, boolean inDumpster)
            throws SQLException, ServiceException {
        boolean hasValidLIMIT = offset >= 0 && limit >= 0;
        StringBuilder sql = new StringBuilder();
        int numParams = 0;
        boolean hasMailItemOnlyConstraints = true;
        boolean hasAppointmentTableConstraints = hasAppointmentTableConstraints(node);
        if (hasAppointmentTableConstraints) {
            hasMailItemOnlyConstraints = hasMailItemOnlyConstraints(node);
        }
        boolean requiresUnion = hasMailItemOnlyConstraints && hasAppointmentTableConstraints;

        if (hasMailItemOnlyConstraints) {
            if (requiresUnion) {
                sql.append("(");
            }

            // SELECT mi.id,... FROM mail_item AS mi [FORCE INDEX (...)] WHERE mi.mailboxid = ? AND
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

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
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
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    private static Object getSortKey(ResultSet rs, SortBy sort) throws SQLException {
        switch (sort.getKey()) {
            case NONE: // no sort column in the result set for NONE
                return null;
            case SUBJECT:
            case SENDER:
            case RCPT:
            case NAME:
            case NAME_NATURAL_ORDER:
            case ATTACHMENT:
            case FLAG:
            case PRIORITY:
                return Strings.nullToEmpty(rs.getString(SORT_COLUMN_ALIAS));
            case SIZE:
                return new Long(rs.getInt(SORT_COLUMN_ALIAS));
            case DATE:
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

    private static final int setDateRange(PreparedStatement stmt, int param,
            Collection<DbSearchConstraints.NumericRange> ranges) throws SQLException {
        for (DbSearchConstraints.NumericRange range : ranges) {
            if (range.min > 0) {
                stmt.setInt(param++, (int) Math.min(range.min / 1000, Integer.MAX_VALUE));
            }
            if (range.max > 0) {
                stmt.setInt(param++, (int) Math.min(range.max / 1000, Integer.MAX_VALUE));
            }
        }
        return param;
    }

    private static final int setTimestampRange(PreparedStatement stmt, int param,
            Collection<DbSearchConstraints.NumericRange> ranges) throws SQLException {
        for (DbSearchConstraints.NumericRange range : ranges) {
            if (range.min > 0) {
                stmt.setTimestamp(param++, new Timestamp(range.min));
            }
            if (range.max > 0) {
                stmt.setTimestamp(param++, new Timestamp(range.max));
            }
        }
        return param;
    }

    private static final int setLongRange(PreparedStatement stmt, int param,
            Collection<DbSearchConstraints.NumericRange> ranges, int min) throws SQLException {
        for (DbSearchConstraints.NumericRange range : ranges) {
            if (range.min >= min) {
                stmt.setLong(param++, range.min);
            }
            if (range.max >= min) {
                stmt.setLong(param++, range.max);
            }
        }
        return param;
    }

    private static final int setIntRange(PreparedStatement stmt, int param,
            Collection<DbSearchConstraints.NumericRange> ranges, int min) throws SQLException {
        for (DbSearchConstraints.NumericRange range : ranges) {
            if (range.min >= min) {
                stmt.setInt(param++, (int) range.min);
            }
            if (range.max >= min) {
                stmt.setInt(param++, (int) range.max);
            }
        }
        return param;
    }

    private static final int setStringRange(PreparedStatement stmt, int param,
            Collection<DbSearchConstraints.StringRange> ranges) throws SQLException {
        for (DbSearchConstraints.StringRange range: ranges) {
            if (range.min != null) {
                stmt.setString(param++, range.min.replace("\\\"", "\""));
            }
            if (range.max != null) {
                stmt.setString(param++, range.max.replace("\\\"", "\""));
            }
        }
        return param;
    }

    private static final int setCursorRange(PreparedStatement stmt, int param, DbSearchConstraints.CursorRange range)
            throws SQLException {
        if (range.min != null) {
            stmt.setString(param++, range.min.replace("\\\"", "\""));
        }
        if (range.max != null) {
            stmt.setString(param++, range.max.replace("\\\"", "\""));
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
    private static final int encodeRange(StringBuilder out, String column,
            Collection<DbSearchConstraints.NumericRange> ranges, long min) {
        if (ranges.isEmpty()) {
            return 0;
        }
        if (Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON) && isCaseSensitiveField(column) ) {
            column = "UPPER(" + column + ")";
        }
        int params = 0;
        for (DbSearchConstraints.NumericRange range : ranges) {
            boolean lowValid = range.min >= min;
            boolean highValid = range.max >= min;
            if (!(lowValid || highValid)) {
                continue;
            }
            out.append(range.bool ? " AND (" : " AND NOT (");
            if (lowValid) {
                out.append(column).append(range.minInclusive ? " >= ?" : " > ?");
                params++;
            }
            if (highValid) {
                if (lowValid) {
                    out.append(" AND ");
                }
                out.append(column).append(range.maxInclusive ? " <= ?" : " < ?");
                params++;
            }
            out.append(')');
        }
        return params;
    }

    /**
     * @return number of parameters bound
     */
    private static final int encodeRange(StringBuilder out, String column,
            Collection<DbSearchConstraints.StringRange> ranges) {
        if (ranges.isEmpty()) {
            return 0;
        }
        if (Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON) && isCaseSensitiveField(column)) {
            column = "UPPER(" + column + ")";
        }
        int params = 0;
        for (DbSearchConstraints.StringRange range : ranges) {
            out.append(range.bool ?  " AND (" : " AND NOT (");
            if (range.min != null) {
                params++;
                out.append(column).append(range.minInclusive ? " >= ?" : " > ?");
            }
            if (range.max != null) {
                if (range.min != null) {
                    out.append(" AND ");
                }
                params++;
                out.append(column).append(range.maxInclusive ? " <= ?" : " < ?");
            }
            out.append(')');
        }
        return params;
    }

    private static final int encodeRange(StringBuilder out, DbSearchConstraints.CursorRange range) {
        // Can't use SORT_COLUMN_ALIAS because column aliases in SELECT are illegal to use in WHERE
        String col = toSortField(range.sortBy);
        int params = 0;
        out.append(" AND (");
        if (range.min != null) {
            params++;
            out.append(col).append(range.minInclusive ? " >= ?" : " > ?");
        }
        if (range.max != null) {
            if (range.min != null) {
                out.append(" AND ");
            }
            params++;
            out.append(col).append(range.maxInclusive ? " <= ?" : " < ?");
        }
        out.append(')');
        return params;
    }

    public static final class TagConstraints {
        Set<Long> searchTagsets;
        Set<Long> searchFlagsets;
        Boolean unread;
        boolean noMatches;

        static TagConstraints getTagConstraints(Mailbox mbox, DbSearchConstraints.Leaf leaf, DbConnection conn)
                throws ServiceException {
            TagConstraints tc = leaf.tagConstraints = new TagConstraints();
            if (leaf.tags.isEmpty() && leaf.excludeTags.isEmpty()) {
                return tc;
            }
            int setFlagMask = 0;
            long setTagMask = 0;

            if (!ListUtil.isEmpty(leaf.tags)) {
                for (Tag tag : leaf.tags) {
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

            if (!ListUtil.isEmpty(leaf.excludeTags)) {
                for (Tag tag : leaf.excludeTags) {
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

    private static int setSearchVars(PreparedStatement stmt, DbSearchConstraints node, int param,
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
        if (node instanceof DbSearchConstraints.Intersection || node instanceof DbSearchConstraints.Union) {
            for (DbSearchConstraints child : node.getChildren()) {
                param = setSearchVars(stmt, child, param, calTypes, inCalTable);
            }
            return param;
        }

        // we're here, so we must be in a DbSearchConstraints leaf node
        DbSearchConstraints.Leaf leaf = node.toLeaf();
        assert(node instanceof DbSearchConstraints.Leaf && leaf != null);

        // if there are no possible matches, short-circuit here...
        if (leaf.tagConstraints.noMatches) {
            return param;
        }
        for (MailItem.Type type : leaf.types) {
            stmt.setByte(param++, type.toByte());
        }
        for (MailItem.Type type : leaf.excludeTypes) {
            stmt.setByte(param++, type.toByte());
        }
        param = setBytes(stmt, param, calTypes);

        param = setLongs(stmt, param, leaf.tagConstraints.searchTagsets);
        param = setLongs(stmt, param, leaf.tagConstraints.searchFlagsets);
        param = setBooleanAsInt(stmt, param, leaf.tagConstraints.unread);
        param = setFolders(stmt, param, leaf.folders);
        param = setFolders(stmt, param, leaf.excludeFolders);
        if (leaf.convId > 0) {
            stmt.setInt(param++, leaf.convId);
        } else {
            param = setIntegers(stmt, param, leaf.prohibitedConvIds);
        }
        param = setIntegers(stmt, param, leaf.itemIds);
        param = setIntegers(stmt, param, leaf.prohibitedItemIds);
        param = setIntegers(stmt, param, leaf.indexIds);
        if (leaf.cursorRange != null) {
            param = setCursorRange(stmt, param, leaf.cursorRange);
        }
        param = setDateRange(stmt, param, leaf.dateRanges);
        param = setLongRange(stmt, param, leaf.modSeqRanges, 1);
        param = setIntRange(stmt, param, leaf.sizeRanges, 0);
        param = setStringRange(stmt, param, leaf.subjectRanges);
        param = setStringRange(stmt, param, leaf.senderRanges);

        if (inCalTable) {
            param = setTimestampRange(stmt, param, leaf.calStartDateRanges);
            param = setTimestampRange(stmt, param, leaf.calEndDateRanges);
        }

        return param;
    }

    public static abstract class Result {
        private final Object sortValue;

        protected Result(Object sortValue) {
            assert (sortValue == null || sortValue instanceof String ||
                    sortValue instanceof Long || sortValue instanceof Integer) : sortValue;
            this.sortValue = sortValue;
        }

        public Object getSortValue() {
            return sortValue;
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
                .add("sort", sortValue)
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
            switch (sort.getKey()) {
                case SIZE:
                case DATE:
                    long date1 = (Long) o1.getSortValue();
                    long date2 = (Long) o2.getSortValue();
                    if (date1 != date2) {
                        long diff;
                        if (sort.getDirection() == SortBy.Direction.DESC) {
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
                    throw new UnsupportedOperationException(sort.getKey().toString());
            }
            if (sort.getDirection() == SortBy.Direction.DESC) {
                return o2.getId() - o1.getId();
            } else {
                return o1.getId() - o2.getId();
            }
        }
    }

}
