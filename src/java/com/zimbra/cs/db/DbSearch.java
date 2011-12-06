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
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.imap.ImapMessage;
import com.zimbra.cs.index.DbSearchConstraints;
import com.zimbra.cs.index.SortBy;
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

    private static final byte[] APPOINTMENT_TABLE_TYPES = new byte[] {
        MailItem.Type.APPOINTMENT.toByte(), MailItem.Type.TASK.toByte()
    };

    private final Mailbox mailbox;
    private final boolean dumpster;
    private final StringBuilder sql = new StringBuilder();
    private final List<Object> params = new ArrayList<Object>();

    public DbSearch(Mailbox mbox) {
        this.mailbox = mbox;
        this.dumpster = false;
    }

    public DbSearch(Mailbox mbox, boolean dumpster) {
        this.mailbox = mbox;
        this.dumpster = dumpster;
    }

    /**
     * Returns true if this field is case-sensitive for search/sort, i.e. whether or not we need to do an UPPER() on it
     * in places.
     */
    private boolean isCaseSensitiveField(String fieldName) {
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
        Db db = Db.getInstance();
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
                return db.concat(db.sign(db.bitAND("mi.flags", String.valueOf(Flag.BITMASK_ATTACHED))),
                        db.lpad("mi.id", 10, "0"));
            case FLAG: // 0 or 1
                return db.concat(db.sign(db.bitAND("mi.flags", String.valueOf(Flag.BITMASK_FLAGGED))),
                        db.lpad("mi.id", 10, "0"));
            case PRIORITY: // 0 or 1 or 2
                return db.concat("(1 + " + db.sign(db.bitAND("mi.flags", String.valueOf(Flag.BITMASK_HIGH_PRIORITY))) +
                        " - " + db.sign(db.bitAND("mi.flags", String.valueOf(Flag.BITMASK_LOW_PRIORITY))) + ")",
                        db.lpad("mi.id", 10, "0"));
            case DATE:
            default:
                return "mi.date";
        }
    }

    private static String toStringSortField(String col) {
        Db db = Db.getInstance();
        if (Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON)) {
            return db.concat("UPPER(" + col + ")", db.lpad("mi.id", 10, "0"));
        } else {
            return db.concat(col, db.lpad("CAST(mi.id AS CHAR)", 10, "0"));
        }
    }

    /**
     * Generate a column-reference for the sort-by column.
     */
    private void addSortColumn(SortBy sort) {
        String field = toSortField(sort);
        if (field == null) { // no sort column for NONE
            return;
        }
        sql.append(", ").append(field).append(" AS ").append(SORT_COLUMN_ALIAS);
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

    public int countResults(DbConnection conn, DbSearchConstraints node) throws ServiceException {
        sql.append("SELECT COUNT(*) FROM ").append(DbMailItem.getMailItemTableName(mailbox, "mi", dumpster));
        sql.append(" WHERE ");
        if (!DebugConfig.disableMailboxGroups) {
            sql.append("mi.mailbox_id = ? AND ");
            params.add(mailbox.getId());
        }
        encodeConstraint(node, null, false);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql.toString());
            setParameters(stmt);
            //pos = setSearchVars(stmt, node, pos, null, false, dumpster);
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

    private String getForceIndexClause(DbSearchConstraints node, SortBy sort, boolean hasLimit) {
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

    private void encodeSelect(SortBy sort, FetchMode fetch, boolean joinAppt, DbSearchConstraints node,
            boolean validLIMIT) {
        // If you change the first for parameters, you must change the COLUMN_* constants.
        sql.append("SELECT ");
        switch (fetch) {
            case ID:
                sql.append("mi.id, mi.index_id, mi.type");
                break;
            case MAIL_ITEM:
                sql.append(DbMailItem.DB_FIELDS);
                break;
            case IMAP_MSG:
                sql.append("mi.id, mi.index_id, mi.type, mi.imap_id, mi.unread, mi.flags, mi.tag_names");
                break;
            case MODSEQ:
                sql.append("mi.id, mi.index_id, mi.type, mi.mod_metadata");
                break;
            case PARENT:
                sql.append("mi.id, mi.index_id, mi.type, mi.parent_id");
                break;
            case MODCONTENT:
                sql.append("mi.id, mi.index_id, mi.type, mi.mod_content");
                break;
        }
        addSortColumn(sort);
        sql.append(" FROM ").append(DbMailItem.getMailItemTableName(mailbox, "mi", dumpster));
        sql.append(getForceIndexClause(node, sort, validLIMIT));
        if (joinAppt) {
            sql.append(", ").append(DbMailItem.getCalendarItemTableName(mailbox, "ap", dumpster));
        }
        sql.append(" WHERE ");
        if (!DebugConfig.disableMailboxGroups) {
            sql.append("mi.mailbox_id = ? AND ");
            params.add(mailbox.getId());
        }
        if (joinAppt) {
            if (!DebugConfig.disableMailboxGroups) {
                sql.append("mi.mailbox_id = ap.mailbox_id AND ");
            }
            sql.append("mi.id = ap.item_id AND ");
        }
    }

    private void encodeConstraint(DbSearchConstraints node, byte[] calTypes, boolean inCalTable) {

        if (node instanceof DbSearchConstraints.Intersection || node instanceof DbSearchConstraints.Union) {
            boolean first = true;
            boolean and = node instanceof DbSearchConstraints.Intersection;
            sql.append('(');
            for (DbSearchConstraints child : node.getChildren()) {
                if (!first) {
                    sql.append(and ? " AND " : " OR ");
                }
                encodeConstraint(child, calTypes, inCalTable);
                first = false;
            }
            sql.append(") ");
            return;
        }

        // we're here, so we must be in a DbSearchConstraints leaf node
        DbSearchConstraints.Leaf constraint = node.toLeaf();
        assert(node instanceof DbSearchConstraints.Leaf && constraint != null);

        // if there are no possible matches, short-circuit here...
        if (constraint.noResults) {
            sql.append(Db.supports(Db.Capability.BOOLEAN_DATATYPE) ? "FALSE" : "0=1");
            return;
        }

        sql.append('(');

        // special-case this one, since there can't be a leading AND here...
        if (ListUtil.isEmpty(constraint.types)) {
            sql.append("mi.type NOT IN ").append(DbMailItem.NON_SEARCHABLE_TYPES);
        } else {
            sql.append(DbUtil.whereIn("mi.type", constraint.types.size()));
            for (MailItem.Type type : constraint.types) {
                params.add(type.toByte());
            }
        }

        encodeType(constraint.excludeTypes, false);
        encode("mi.type", inCalTable, calTypes);
        encodeTag(constraint.tags, true);
        encodeTag(constraint.excludeTags, false);
        encodeFolder(constraint.folders, true);
        encodeFolder(constraint.excludeFolders, false);

        if (constraint.convId > 0) {
            encode("mi.parent_id", true, constraint.convId);
        } else {
            encode("mi.parent_id", false, constraint.prohibitedConvIds);
        }
        encode("mi.id", true, constraint.itemIds);
        encode("mi.id", false, constraint.prohibitedItemIds);
        encode("mi.index_id", true, constraint.indexIds);
        if (constraint.cursorRange != null) {
            encodeCursorRange(constraint.cursorRange);
        }

        for (Map.Entry<DbSearchConstraints.RangeType, DbSearchConstraints.Range> entry : constraint.ranges.entries()) {
            switch (entry.getKey()) {
                case DATE:
                    encodeDateRange("mi.date", (DbSearchConstraints.NumericRange) entry.getValue());
                    break;
                case MDATE:
                    encodeDateRange("mi.change_date", (DbSearchConstraints.NumericRange) entry.getValue());
                    break;
                case MODSEQ:
                    encodeLongRange("mi.mod_metadata", (DbSearchConstraints.NumericRange) entry.getValue(), 1L);
                    break;
                case SIZE:
                    encodeIntRange("mi.size", (DbSearchConstraints.NumericRange) entry.getValue(), 0);
                    break;
                case CAL_START_DATE:
                    if (inCalTable) {
                        encodeTimestampRange("ap.start_time", (DbSearchConstraints.NumericRange) entry.getValue(), 1L);
                    }
                    break;
                case CAL_END_DATE:
                    if (inCalTable) {
                        encodeTimestampRange("ap.end_time", (DbSearchConstraints.NumericRange) entry.getValue(), 1L);
                    }
                    break;
                case SENDER:
                    encodeStringRange("mi.sender", (DbSearchConstraints.StringRange) entry.getValue());
                    break;
                case SUBJECT:
                    encodeStringRange("mi.subject", (DbSearchConstraints.StringRange) entry.getValue());
                    break;
                case CONV_COUNT:
                default:
                    break;
            }
        }

        Boolean isSoloPart = node.toLeaf().getIsSoloPart();
        if (isSoloPart != null) {
            if (isSoloPart.booleanValue()) {
                sql.append(" AND mi.parent_id is NULL ");
            } else {
                sql.append(" AND mi.parent_id is NOT NULL ");
            }
        }

        if (constraint.hasIndexId != null) {
            if (constraint.hasIndexId.booleanValue()) {
                sql.append(" AND mi.index_id is NOT NULL ");
            } else {
                sql.append(" AND mi.index_id is NULL ");
            }
        }

        sql.append(')');
    }

    /**
     * @return TRUE if some part of this query has a non-appointment select (ie 'type not in (11,15)' non-null
     */
    private boolean hasMailItemOnlyConstraints(DbSearchConstraints node) {
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
    private boolean hasAppointmentTableConstraints(DbSearchConstraints node) {
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
    
    private static List<Result> intersectSortedLists(List<Result> toRet, List<List<Result>> lists) {
        if (lists.size() < 0) {
            return toRet;
        }
        //optimize so shortest list is first
        Collections.sort(lists, new Comparator<List<Result>>() {
            @Override
            public int compare(List<Result> l1, List<Result> l2) {
                return l1.size() - l2.size();
            }
        });
        
        for (Result result : lists.get(0)) {
            boolean intersect = true;
            for (int i = 1; i < lists.size(); i++) {
                if (!lists.get(i).contains(result)) {
                    intersect = false;
                    break;
                }
            }
            if (intersect) {
                toRet.add(result);
            }
        }
        return toRet;
    }

    public List<Result> search(DbConnection conn, DbSearchConstraints node, SortBy sort, int offset, int limit,
            FetchMode fetch) throws ServiceException {
        if (!Db.supports(Db.Capability.AVOID_OR_IN_WHERE_CLAUSE) || !(node instanceof DbSearchConstraints.Union)) {
            try {
                return searchInternal(conn, node, sort, offset, limit, fetch);
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
        if (node instanceof DbSearchConstraints.Union) {
            for (DbSearchConstraints child : node.getChildren()) {
                result.addAll(new DbSearch(mailbox, dumpster).search(conn, child, sort, offset, limit, fetch));
            }
            Collections.sort(result, new ResultComparator(sort));
        } else if (node instanceof DbSearchConstraints.Intersection) {
            List<List<Result>> resultLists = new ArrayList<List<Result>>();
            
            for (DbSearchConstraints child : node.getChildren()) {
                resultLists.add(new DbSearch(mailbox, dumpster).search(conn, child, sort, offset, limit, fetch));
            }
            result = intersectSortedLists(result, resultLists);
        } else {
            throw ServiceException.FAILURE("Reached merge/intersect block with something other than OR/AND clause", null);
        }
        return result;
    }

    private List<Result> searchInternal(DbConnection conn, DbSearchConstraints node, SortBy sort, int offset, int limit,
            FetchMode fetch) throws SQLException, ServiceException {
        boolean hasValidLIMIT = offset >= 0 && limit >= 0;
        boolean hasMailItemOnlyConstraints = true;
        boolean hasAppointmentTableConstraints = hasAppointmentTableConstraints(node);
        if (hasAppointmentTableConstraints) {
            hasMailItemOnlyConstraints = hasMailItemOnlyConstraints(node);
        }
        boolean requiresUnion = hasMailItemOnlyConstraints && hasAppointmentTableConstraints;

        if (hasMailItemOnlyConstraints) {
            if (requiresUnion) {
                sql.append('(');
            }

            // SELECT mi.id,... FROM mail_item AS mi [FORCE INDEX (...)] WHERE mi.mailboxid = ? AND
            encodeSelect(sort, fetch, false, node, hasValidLIMIT);

            /*
             *( SUB-NODE AND/OR (SUB-NODE...) ) AND/OR ( SUB-NODE ) AND
             *    (
             *       one of: [type NOT IN (...)]  || [type = ?] || [type IN ( ...)]
             *       [ AND flags IN (...) ]
             *       ..etc
             *    )
             */
            encodeConstraint(node, hasAppointmentTableConstraints ? APPOINTMENT_TABLE_TYPES : null, false);

            if (requiresUnion) {
                sql.append(orderBy(sort, true));
                // LIMIT ?, ?
                if (hasValidLIMIT && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                    sql.append(' ').append(Db.getInstance().limit(offset, limit));
                }
            }
        }

        if (requiresUnion) {
            sql.append(" ) UNION ALL (");
        }

        if (hasAppointmentTableConstraints) {
            // SELECT...again...(this time with "appointment as ap")...WHERE...
            encodeSelect(sort, fetch, true, node, hasValidLIMIT);
            encodeConstraint(node, APPOINTMENT_TABLE_TYPES, true);
            if (requiresUnion) {
                sql.append(orderBy(sort, true));
                // LIMIT ?, ?
                if (hasValidLIMIT && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                    sql.append(' ').append(Db.getInstance().limit(offset, limit));
                }
                if (requiresUnion) {
                    sql.append(')');
                }
            }
        }

        // TODO FIXME: include COLLATION for sender/subject sort
        sql.append(orderBy(sort, true));

        // LIMIT ?, ?
        if (hasValidLIMIT && Db.supports(Db.Capability.LIMIT_CLAUSE)) {
            sql.append(' ').append(Db.getInstance().limit(offset, limit));
        }

        if (Db.supports(Db.Capability.SQL_PARAM_LIMIT)) {
            Db.getInstance().checkParamLimit(params.size());
        }
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // Create the statement and bind all our parameters!
            stmt = conn.prepareStatement(sql.toString());
            setParameters(stmt);
            // Limit query if DB doesn't support LIMIT clause
            if (hasValidLIMIT && !Db.supports(Db.Capability.LIMIT_CLAUSE)) {
                stmt.setMaxRows(offset + limit + 1);
            }
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
                        result.add(new ItemDataResult(rs, sortkey, dumpster));
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

    private void encode(String column, boolean bool, Object o) {
        sql.append(" AND ").append(column).append(bool ? " = ?" : " != ?");
        params.add(o);
    }

    private void encodeFolder(Set<Folder> folders, boolean bool) {
        if (folders.isEmpty()) {
            return;
        }
        sql.append(" AND ").append(DbUtil.whereIn("mi.folder_id", bool, folders.size()));
        for (Folder folder : folders) {
            params.add(folder.getId());
        }
    }

    private void encodeType(Set<MailItem.Type> types, boolean bool) {
        if (types.isEmpty()) {
            return;
        }
        sql.append(" AND ").append(DbUtil.whereIn("type", bool, types.size()));
        for (MailItem.Type type : types) {
            params.add(type.toByte());
        }
    }

    private void encode(String column, boolean bool, Collection<?> c) {
        if (ListUtil.isEmpty(c)) {
            return;
        }
        sql.append(" AND ").append(DbUtil.whereIn(column, bool, c.size()));
        params.addAll(c);
    }

    private void encode(String column, boolean bool, byte[] array) {
        if (array == null || array.length == 0) {
            return;
        }
        sql.append(" AND ").append(DbUtil.whereIn(column, bool, array.length));
        for (byte b : array) {
            params.add(b);
        }
    }

    private void encodeTag(Set<Tag> tags, boolean bool) {
        if (tags.isEmpty()) {
            return;
        }
        if (dumpster) { // There is no corresponding table to TAGGED_ITEM in dumpster, hence brute-force search.
            int flags = 0;
            for (Tag tag : tags) {
                if (tag instanceof Flag) {
                    Flag flag = (Flag) tag;
                    if (flag.getId() == Flag.ID_UNREAD) {
                        sql.append(" AND mi.unread = ?");
                        params.add(bool ? 1 : 0);
                    } else {
                        flags |= flag.toBitmask();
                    }
                } else {
                    sql.append(" AND (mi.tag_names");
                    if (!bool) { // Include NULL because LIKE does not match NULL.
                        sql.append(" IS NULL OR mi.tag_names NOT");
                    }
                    sql.append(" LIKE ?)");
                    params.add(DbTag.tagLIKEPattern(tag.getName()));
                }
            }
            if (flags != 0) {
                sql.append(" AND ").append(Db.getInstance().bitAND("mi.flags", "?")).append(" = ?");
                params.add(flags);
                params.add(bool ? flags : 0);
            }
        } else if (bool) { // Repeats "EXISTS (SELECT...)" as many times as tags.
            for (Tag tag : tags) {
                sql.append(" AND EXISTS (SELECT * FROM ").append(DbTag.getTaggedItemTableName(mailbox, "ti"));
                sql.append(" WHERE ");
                if (!DebugConfig.disableMailboxGroups) {
                    sql.append("mi.mailbox_id = ti.mailbox_id AND ");
                }
                sql.append("mi.id = ti.item_id AND ti.tag_id = ?)");
                params.add(tag.getId());
            }
        } else { // NOT EXISTS (SELECT... WHERE... tag_id IN...)
            sql.append(" AND NOT EXISTS (SELECT * FROM ").append(DbTag.getTaggedItemTableName(mailbox, "ti"));
            sql.append(" WHERE ");
            if (!DebugConfig.disableMailboxGroups) {
                sql.append("mi.mailbox_id = ti.mailbox_id AND ");
            }
            sql.append("mi.id = ti.item_id AND ").append(DbUtil.whereIn("ti.tag_id", tags.size())).append(')');
            for (Tag tag : tags) {
                params.add(tag.getId());
            }
        }
    }

    private void encodeDateRange(String column, DbSearchConstraints.NumericRange range) {
        encodeRange(column, range, 1L, (int) Math.min(range.min / 1000, Integer.MAX_VALUE),
                (int) Math.min(range.max / 1000, Integer.MAX_VALUE));
    }

    private void encodeTimestampRange(String column, DbSearchConstraints.NumericRange range, long cutoff) {
        encodeRange(column, range, cutoff, new Timestamp(range.min), new Timestamp(range.max));
    }

    private void encodeLongRange(String column, DbSearchConstraints.NumericRange range, long cutoff) {
        encodeRange(column, range, cutoff, range.min, range.max);
    }

    private void encodeIntRange(String column, DbSearchConstraints.NumericRange range, int cutoff) {
        encodeRange(column, range, cutoff, (int) range.min, (int) range.max);
    }

    private void encodeRange(String column, DbSearchConstraints.NumericRange range,
            long cutoff, Object min, Object max) {
        if (Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON) && isCaseSensitiveField(column)) {
            column = "UPPER(" + column + ")";
        }
        boolean lowValid = range.min >= cutoff;
        boolean highValid = range.max >= cutoff;
        if (!(lowValid || highValid)) {
            return;
        }

        sql.append(range.bool ? " AND (" : " AND NOT (");
        if (lowValid) {
            sql.append(column).append(range.minInclusive ? " >= ?" : " > ?");
            params.add(min);
        }
        if (highValid) {
            if (lowValid) {
                sql.append(" AND ");
            }
            sql.append(column).append(range.maxInclusive ? " <= ?" : " < ?");
            params.add(max);
        }
        sql.append(')');
    }

    private void encodeStringRange(String column, DbSearchConstraints.StringRange range) {
        if (Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON) && isCaseSensitiveField(column)) {
            column = "UPPER(" + column + ")";
        }
        sql.append(range.bool ?  " AND (" : " AND NOT (");
        if (range.min != null) {
            sql.append(column).append(range.minInclusive ? " >= ?" : " > ?");
            params.add(range.min.replace("\\\"", "\""));
        }
        if (range.max != null) {
            if (range.min != null) {
                sql.append(" AND ");
            }
            sql.append(column).append(range.maxInclusive ? " <= ?" : " < ?");
            params.add(range.max.replace("\\\"", "\""));
        }
        sql.append(')');
    }

    private void encodeCursorRange(DbSearchConstraints.CursorRange range) {
        // Can't use SORT_COLUMN_ALIAS because column aliases in SELECT are illegal to use in WHERE
        String col = toSortField(range.sortBy);
        sql.append(" AND (");
        if (range.min != null) {
            sql.append(col).append(range.minInclusive ? " >= ?" : " > ?");
            params.add(range.min.replace("\\\"", "\""));
        }
        if (range.max != null) {
            if (range.min != null) {
                sql.append(" AND ");
            }
            sql.append(col).append(range.maxInclusive ? " <= ?" : " < ?");
            params.add(range.max.replace("\\\"", "\""));
        }
        sql.append(')');
    }

    private void setParameters(PreparedStatement stmt) throws SQLException {
        int pos = 0;
        for (Object param : params) {
            stmt.setObject(++pos, param);
        }
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
            i4msg = new ImapMessage(getId(), getType(), rs.getInt(offset + 1), flags, DbTag.deserializeTags(rs.getString(offset + 4)));
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
                case SENDER:
                case SUBJECT:
                case NAME:
                case NAME_NATURAL_ORDER:
                    String s1 = (String) o1.getSortValue();
                    String s2 = (String) o2.getSortValue();
                    if (!StringUtil.equal(s1, s2)) {
                        if (sort.getDirection() == SortBy.Direction.DESC) {
                            return StringUtil.compareTo(s2, s1);
                         } else {
                            return StringUtil.compareTo(s1, s2);
                         }
                    }
                    break;
                case ID:
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
