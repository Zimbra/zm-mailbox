/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
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
import com.zimbra.cs.index.LuceneQueryOperation.LuceneResultsChunk;
import com.zimbra.cs.index.SortBy.Key;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Flag.FlagInfo;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxOperation;
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
    private Mailbox authMailbox;
    private final boolean dumpster;
    private final StringBuilder sql = new StringBuilder();
    private final List<Object> params = new ArrayList<Object>();

    //used for relevance sorting
    private LuceneResultsChunk luceneResults;

    public DbSearch(Mailbox mbox) {
        this(mbox, false, null);
    }

    public DbSearch(Mailbox mbox, boolean dumpster) {
        this(mbox, dumpster, null);
    }

    public DbSearch(Mailbox mbox, boolean dumpster, LuceneResultsChunk luceneResults) {
        this(mbox, dumpster, luceneResults, null);
    }

    public DbSearch(Mailbox mbox, boolean dumpster, LuceneResultsChunk luceneResults, Mailbox authMailbox) {
        this.mailbox = mbox;
        this.dumpster = dumpster;
        this.luceneResults = luceneResults;
        this.authMailbox = authMailbox;
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
            case RELEVANCE:
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
            case UNREAD: // 0 or 1 or 2
                return "mi.unread";
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
     * Note: Assumes that <b>mi.id</b> is something defined in the SELECT which can be ordered by.
     */

    static String orderBy(SortBy sort, boolean alias) {
        SortBy.Key sortKey = sort.getKey();
        if (sortKey == Key.NONE || sortKey == Key.RELEVANCE) { // no ORDER BY for NONE or RELEVANCE
            return "";
        }
        StringBuilder orderBy = new StringBuilder(" ORDER BY ");
        if (sort.getKey() == SortBy.Key.RECENTLYVIEWED) {
            return orderBy.append("e.ts DESC, mi.date DESC").toString();
        }
        orderBy.append(alias ? SORT_COLUMN_ALIAS : toSortField(sort));
        if (sort.getDirection() == SortBy.Direction.DESC) {
            orderBy.append(" DESC");
        }

        if (sort.getDirection() == SortBy.Direction.ASC && sort.getKey() == SortBy.Key.UNREAD) {
            orderBy.append(" DESC");
        }
        if (sort.getKey() == SortBy.Key.UNREAD) {
            /* See also ResultsPager.forwardFindFirst() */
            return orderBy.append(", mi.id DESC").toString();
        }
        /* Successive searches using cursors need a predictable order, so add additional search column. */
        if (Key.ID != sort.getKey()) {
            orderBy.append(", mi.id");
            if (sort.getDirection() == SortBy.Direction.DESC) {
                orderBy.append(" DESC");
            }
        }
        return orderBy.toString();
    }

    public int countResults(DbConnection conn, DbSearchConstraints node) throws ServiceException {
        return countResults(conn, node, false);
    }

    public int countResults(DbConnection conn, DbSearchConstraints node, boolean ignoreNoRecipients) throws ServiceException {
        node = node.optimize();
        sql.append("SELECT COUNT(*) FROM ").append(DbMailItem.getMailItemTableName(mailbox, "mi", dumpster));
        sql.append(" WHERE ");
        if (!DebugConfig.disableMailboxGroups) {
            sql.append("mi.mailbox_id = ? AND ");
            params.add(mailbox.getId());
        }
        encodeConstraint(node, null, false, false);
        if (ignoreNoRecipients) {
            sql.append(" AND mi.recipients IS NOT NULL");
        }
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

    private void encodeSelect(SortBy sort, FetchMode fetch, boolean joinAppt, boolean joinTaggedItem,
            DbSearchConstraints node, boolean validLIMIT) {
        encodeSelect(sort, fetch, joinAppt, joinTaggedItem, node, validLIMIT, false);
    }

    private void encodeSelect(SortBy sort, FetchMode fetch, boolean joinAppt, boolean joinTaggedItem,
            DbSearchConstraints node, boolean validLIMIT, boolean maybeExcludeHasRecipients) {
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
        if (joinTaggedItem) {
            sql.append(", ").append(DbTag.getTaggedItemTableName(mailbox, "ti"));
        }
        if (!joinAppt && !joinTaggedItem && sort != null && sort.equals(SortBy.RECENTLY_VIEWED)) {
            Mailbox joinMailbox = mailbox;
            if (authMailbox != null && mailbox.getId() != authMailbox.getId()) {
                joinMailbox = authMailbox;
            }
            sql.append(" LEFT JOIN ").append(DbEvent.getEventTableName(joinMailbox, "e")).append(" ON mi.id = e.item_id and e.op = " + MailboxOperation.View.getCode());
        }
        sql.append(" WHERE ");
        if (!DebugConfig.disableMailboxGroups) {
            sql.append("mi.mailbox_id = ? AND ");
            params.add(mailbox.getId());
        }
        //Bug: 74521
        //for rcptAsc order make sure that the RECIPIENTS col is NOT null.
        //Bug: 82703
        //also doing this for sort=rcptDesc
        if (sort != null && (sort.equals(SortBy.RCPT_ASC) || sort.equals(SortBy.RCPT_DESC)) && maybeExcludeHasRecipients) {
            sql.append("(mi.recipients is NOT NULL) AND ");
        }
        if (joinAppt) {
            if (!DebugConfig.disableMailboxGroups) {
                sql.append("mi.mailbox_id = ap.mailbox_id AND ");
            }
            sql.append("mi.id = ap.item_id AND ");
        }
    }

    private static boolean searchingInDrafts(DbSearchConstraints node) {
        if (node instanceof DbSearchConstraints.Leaf) {
            for (Folder folder: ((DbSearchConstraints.Leaf) node).folders) {
                if(folder.getId() == Mailbox.ID_FOLDER_DRAFTS) {
                    return true;
                }
            }
            return false;
        } else {
            boolean success = false;
            for (DbSearchConstraints child: node.getChildren()) {
                if(searchingInDrafts(child)) {
                    success = true;
                }
            }
            return success;
        }
    }

    private void encodeConstraint(DbSearchConstraints node, byte[] calTypes, boolean inCalTable, boolean joinTaggedItem) {
        if (node instanceof DbSearchConstraints.Intersection || node instanceof DbSearchConstraints.Union) {
            boolean first = true;
            boolean and = node instanceof DbSearchConstraints.Intersection;
            sql.append('(');
            for (DbSearchConstraints child : node.getChildren()) {
                if (!child.isEmpty()) {
                    if (!first) {
                        sql.append(and ? " AND " : " OR ");
                    }
                    encodeConstraint(child, calTypes, inCalTable, joinTaggedItem);
                    first = false;
                }
            }
            sql.append(") ");
            return;
        }

        // we're here, so we must be in a DbSearchConstraints leaf node
        if (node.isEmpty()) {return; }
        DbSearchConstraints.Leaf constraint = node.toLeaf();
        assert(node instanceof DbSearchConstraints.Leaf && constraint != null);

        // if there are no possible matches, short-circuit here...
        if (constraint.noResults) {
            sql.append(Db.supports(Db.Capability.BOOLEAN_DATATYPE) ? "FALSE" : "0=1");
            return;
        }
        sql.append('(');
        boolean needAnd = false;
        // special-case this one, since there can't be a leading AND here...
        if (ListUtil.isEmpty(constraint.types)) {
            if (!constraint.typesFactoredOut) {
                //don't include the negative types clause if types already encoded in
                sql.append("mi.type NOT IN ").append(DbMailItem.NON_SEARCHABLE_TYPES);
                needAnd = true;
            }
        } else {
            sql.append(DbUtil.whereIn("mi.type", constraint.types.size()));
            for (MailItem.Type type : constraint.types) {
                params.add(type.toByte());
            }
            needAnd = true;
        }

        needAnd = needAnd | encodeNoRecipients(constraint.excludeHasRecipients, needAnd);
        needAnd = needAnd | encodeType(constraint.excludeTypes, false, needAnd);
        needAnd = needAnd | encode("mi.type", inCalTable, calTypes, needAnd);
        needAnd = needAnd | encodeTag(constraint.tags, true, joinTaggedItem, needAnd);
        needAnd = needAnd | encodeTag(constraint.excludeTags, false, false, needAnd);
        needAnd = needAnd | encodeFolder(constraint.folders, true,  needAnd);
        needAnd = needAnd | encodeFolder(constraint.excludeFolders, false, needAnd);

        if (constraint.convId > 0) {
            needAnd = needAnd | encode("mi.parent_id", true, constraint.convId, needAnd);
        } else {
            needAnd = needAnd | encode("mi.parent_id", false, constraint.prohibitedConvIds, needAnd);
        }
        needAnd = needAnd | encode("mi.id", true, constraint.itemIds, needAnd);
        needAnd = needAnd | encode("mi.id", false, constraint.prohibitedItemIds, needAnd);
        needAnd = needAnd | encode("mi.index_id", true, constraint.indexIds, needAnd);
        if (constraint.cursorRange != null) {
            needAnd = needAnd | encodeCursorRange(constraint.cursorRange, needAnd);
        }

        for (Map.Entry<DbSearchConstraints.RangeType, DbSearchConstraints.Range> entry : constraint.ranges.entries()) {
            switch (entry.getKey()) {
                case ITEMID:
                    needAnd = needAnd | encodeIntRange("mi.id", (DbSearchConstraints.NumericRange) entry.getValue(), 1, needAnd);
                    break;
                case DATE:
                    needAnd = needAnd | encodeDateRange("mi.date", (DbSearchConstraints.NumericRange) entry.getValue(), needAnd);
                    break;
                case MDATE:
                    needAnd = needAnd | encodeDateRange("mi.change_date", (DbSearchConstraints.NumericRange) entry.getValue(), needAnd);
                    break;
                case MODSEQ:
                    needAnd = needAnd | encodeLongRange("mi.mod_metadata", (DbSearchConstraints.NumericRange) entry.getValue(), 1L, needAnd);
                    break;
                case SIZE:
                    needAnd = needAnd | encodeLongRange("mi.size", (DbSearchConstraints.NumericRange) entry.getValue(), 0, needAnd);
                    break;
                case CAL_START_DATE:
                    if (inCalTable) {
                        needAnd = needAnd | encodeTimestampRange("ap.start_time", (DbSearchConstraints.NumericRange) entry.getValue(), 1L, needAnd);
                    }
                    break;
                case CAL_END_DATE:
                    if (inCalTable) {
                        needAnd = needAnd | encodeTimestampRange("ap.end_time", (DbSearchConstraints.NumericRange) entry.getValue(), 1L, needAnd);
                    }
                    break;
                case SENDER:
                    needAnd = needAnd | encodeStringRange("mi.sender", (DbSearchConstraints.StringRange) entry.getValue(), needAnd);
                    break;
                case SUBJECT:
                    needAnd = needAnd | encodeStringRange("mi.subject", (DbSearchConstraints.StringRange) entry.getValue(), needAnd);
                    break;
                case CONV_COUNT:
                default:
                    break;
            }
        }

        Boolean isSoloPart = node.toLeaf().getIsSoloPart();
        if (isSoloPart != null) {
            if (needAnd) {sql.append(" AND ");}
            needAnd = true;
            if (isSoloPart.booleanValue()) {
                sql.append("mi.parent_id is NULL ");
            } else {
                sql.append("mi.parent_id is NOT NULL ");
            }
        }

        if (constraint.hasIndexId != null) {
            if (needAnd) {sql.append(" AND ");}
            needAnd = true;
            if (constraint.hasIndexId.booleanValue()) {
                sql.append("mi.index_id is NOT NULL ");
            } else {
                sql.append("mi.index_id is NULL ");
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
        return search(conn, node, sort, offset, limit, fetch, true);
    }

    private List<Result> search(DbConnection conn, DbSearchConstraints node, SortBy sort, int offset, int limit,
            FetchMode fetch, boolean searchDraftsSeparately) throws ServiceException {
        if (!Db.supports(Db.Capability.AVOID_OR_IN_WHERE_CLAUSE) || !(node instanceof DbSearchConstraints.Union)) {
            try {
                node = node.optimize();
                return searchInternal(conn, node, sort, offset, limit, fetch, searchDraftsSeparately);
            } catch (SQLException e) {
                if (Db.errorMatches(e, Db.Error.TOO_MANY_SQL_PARAMS)) {
                    ZimbraLog.sqltrace.debug("Too many SQL params: %s", node, e); // fall back to splitting OR clauses
                } else {
                    throw ServiceException.FAILURE("Failed to search", e);
                }
            }
        }
        List<Result> result = new ArrayList<Result>();
        if (!(node instanceof DbSearchConstraints.Leaf)) {
            // case 1 (non-leaf node), if (where a or b) not supported or if we encountered too many sql params try splitting
            // run each toplevel ORed part as a separate SQL query, then merge the results in memory
            if (node instanceof DbSearchConstraints.Union) {
                for (DbSearchConstraints child : node.getChildren()) {
                    result.addAll(new DbSearch(mailbox, dumpster, null, authMailbox).search(conn, child, sort, offset, limit, fetch));
                }
                Collections.sort(result, new ResultComparator(sort));
            } else if (node instanceof DbSearchConstraints.Intersection) {
                List<List<Result>> resultLists = new ArrayList<List<Result>>();

                for (DbSearchConstraints child : node.getChildren()) {
                    resultLists.add(new DbSearch(mailbox, dumpster, null, authMailbox).search(conn, child, sort, offset, limit, fetch));
                }
                result = intersectSortedLists(result, resultLists);
            } else {
                throw ServiceException.FAILURE("Reached merge/intersect block with something other than OR/AND clause", null);
            }
        } else {
            //case 2 (leaf node), we could encounter a sql clause with too many folders involved, try splitting these folders
            //only deals with folders for now to avoid considering complicated situations (other constraints combined)
            DbSearchConstraints.Leaf leafNode = node.toLeaf();
            final int dbLimit = Db.getInstance().getParamLimit();
            //avoid edge cases
            int otherConstraintsCount = params.size() - leafNode.folders.size();
            final int softLimit = dbLimit - otherConstraintsCount - 10;
            if (leafNode.folders.size() > softLimit) {
                List<Folder> folderList = new ArrayList<Folder>(leafNode.folders);
                int end = leafNode.folders.size();
                int start = end - softLimit;
                leafNode.folders.clear();
                while (start > 0) {
                    DbSearchConstraints.Leaf subsetNode = leafNode.clone();
                    List<Folder> subList = folderList.subList(start, end);
                    subsetNode.folders.addAll(subList);
                    result.addAll(new DbSearch(mailbox, dumpster, null, authMailbox).search(conn, subsetNode, sort, offset, limit, fetch));
                    end -= softLimit;
                    start -= softLimit;
                }
                //0 to end
                DbSearchConstraints.Leaf subsetNode = leafNode.clone();
                List<Folder> subList = folderList.subList(0, end);
                subsetNode.folders.addAll(subList);
                result.addAll(new DbSearch(mailbox, dumpster, null, authMailbox).search(conn, subsetNode, sort, offset, limit, fetch));
                Collections.sort(result, new ResultComparator(sort));
            } else {
                throw ServiceException.FAILURE("splitting failed, too many constraints but not caused entirely by folders", null);
            }
        }

        return result;
    }

    private int countUnread(Folder folder) throws ServiceException {
        int count = folder.getUnreadCount();
        List<Folder> subFolders = folder.getSubfolders(null);
        if (subFolders.size() > 0) {
            for (Folder f : subFolders) {
                count += countUnread(f);
            }
        }
        return count;
    }

    private List<Result> searchInternal(DbConnection conn, DbSearchConstraints node, SortBy sort, int offset, int limit,
            FetchMode fetch, boolean searchDraftsSeparately) throws SQLException, ServiceException {
        if (sort.getKey() == Key.RELEVANCE) {
            assert(luceneResults != null);
        }
        //check if we need to run this as two queries: one with "mi.recipients is not NULL" and one in drafts with "mi.recipients is NULL"
        if (searchingInDrafts(node) && searchDraftsSeparately && sort != null && (sort.equals(SortBy.RCPT_ASC) || sort.equals(SortBy.RCPT_DESC))) {
            DbSearchConstraints.Leaf draftsConstraint = findDraftsConstraint(node).clone(); //clone the existing node containing the Drafts constraint
            for (Folder folder: draftsConstraint.folders) {
                if (folder.getId() == Mailbox.ID_FOLDER_DRAFTS) {
                    draftsConstraint.folders.clear();
                    draftsConstraint.folders.add(folder); //constrain the node to only drafts
                    break;
                }
            }
            draftsConstraint.excludeHasRecipients = true;
            DbSearchConstraints node1;
            DbSearchConstraints node2;
            if (sort.equals(SortBy.RCPT_ASC)) {
                node1 = node;
                node2 = draftsConstraint;
            } else {
                node1 = draftsConstraint;
                node2 = node;
            }

            List<Result> result = searchTwoConstraints(conn, node1, node2, sort, offset, limit, fetch);
            return result;

        }

        boolean hasValidLIMIT = offset >= 0 && limit >= 0;
        boolean hasMailItemOnlyConstraints = true;
        boolean hasAppointmentTableConstraints = hasAppointmentTableConstraints(node);
        if (hasAppointmentTableConstraints) {
            hasMailItemOnlyConstraints = hasMailItemOnlyConstraints(node);
        }
        boolean requiresUnion = hasMailItemOnlyConstraints && hasAppointmentTableConstraints;

        //HACK!HACK!HACK!
        //Bug: 68609
        //slow search "in:inbox is:unread or is:flagged"
        //its actually cheaper to do a D/B join between mail_item and tagged_item table when
        //the unread items are smaller in count. We can possibly do the same for user tags.
        boolean joinTaggedItem = false;
        if (node instanceof DbSearchConstraints.Leaf && !dumpster) {
            DbSearchConstraints.Leaf constraint = node.toLeaf();
            if (constraint.excludeTags.isEmpty() && !constraint.tags.isEmpty() && constraint.tags.size() == 1) {
                Tag tag = constraint.tags.iterator().next();
                if (tag.getId() == FlagInfo.UNREAD.toId() || tag.getId() > 0) {
                    long count = 0;
                    if (tag.getId() == FlagInfo.UNREAD.toId()) {
                        //let's make an estimate of # of unread items for this mailbox.
                        //It doesn't matter which folder(s) the user is trying to search because
                        //the performance solely depends on the # of unread items
                        count = countUnread(mailbox.getFolderById(null, Mailbox.ID_FOLDER_USER_ROOT));
                    } else if (tag.getId() > 0) {
                        count = tag.getSize(); //user tag
                    }

                    if (count < LC.search_tagged_item_count_join_query_cutoff.intValue())
                        joinTaggedItem = true;
                }
            }
        }

        if (hasMailItemOnlyConstraints) {
            if (requiresUnion) {
                sql.append('(');
            }
            boolean maybeExcludeNoRecipients = true;
            if (node.toLeaf() != null) {
                maybeExcludeNoRecipients = !node.toLeaf().excludeHasRecipients;
            }

            // SELECT mi.id,... FROM mail_item AS mi [FORCE INDEX (...)] WHERE mi.mailboxid = ? AND
            encodeSelect(sort, fetch, false, joinTaggedItem, node, hasValidLIMIT, maybeExcludeNoRecipients);

            /*
             *( SUB-NODE AND/OR (SUB-NODE...) ) AND/OR ( SUB-NODE ) AND
             *    (
             *       one of: [type NOT IN (...)]  || [type = ?] || [type IN ( ...)]
             *       [ AND flags IN (...) ]
             *       ..etc
             *    )
             */
            encodeConstraint(node, hasAppointmentTableConstraints ? APPOINTMENT_TABLE_TYPES : null, false, joinTaggedItem);

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
            encodeSelect(sort, fetch, true, false, node, hasValidLIMIT);
            encodeConstraint(node, APPOINTMENT_TABLE_TYPES, true, false);
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
            if (sort.getKey() == Key.RELEVANCE) {
                Collections.sort(result, new ResultComparator(sort));
            }
            return result;
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    /** This method can be used to search two constraint trees as if it were one.
     *
     * @param conn
     * @param node1
     * @param node2
     * @param sort
     * @param offset
     * @param limit
     * @param fetch
     * @return
     * @throws SQLException
     * @throws ServiceException
     */
    private List<Result> searchTwoConstraints(DbConnection conn, DbSearchConstraints node1, DbSearchConstraints node2, SortBy sort, int offset, int limit,
            FetchMode fetch) throws SQLException, ServiceException {
        //                 node1 results                     node2 results
        // |---------------------------------------|--------------------------------------|
        List<Result> result1 = new DbSearch(mailbox, dumpster).search(conn, node1, sort, offset, limit, fetch, false);
        if (result1.size() == 0) {
            //                                              |--- somewhere here----|
            // |---------------------------------------|--------------------------------------|
            // If there are no results in the first search, we need to run the second search with some possibly non-zero offset.
            // To find this offset, we need to find out how many total results there were in the first query and subtract that from the given offset.
            // To accurately calculate the number of results, we need to know whether to exclude mi.recipients=null or not,
            // which happens when node1 is NOT our custom draft constraint that has excludeHasRecipients=true.
            boolean ignoreNoRecipients = true;
            if (node1.toLeaf() != null) {
                ignoreNoRecipients = !node1.toLeaf().excludeHasRecipients;
            }
            int offset2 = offset - new DbSearch(mailbox, dumpster).countResults(conn, node1, ignoreNoRecipients);
            int limit2 = limit;
            List<Result> result2 = new DbSearch(mailbox, dumpster).search(conn, node2, sort, offset2, limit2, fetch, false);
            result1.addAll(result2);
        }
        else if (result1.size() < limit) {
            //                      |------ somewhere here------|
            // |---------------------------------------|--------------------------------------|
            // If the size of the result set is less than the given limit but is nonzero, then we are "straddling" the two operations.
            // This means we need to run the second query with offset=0, and limit={remainder from 1st query}
            int offset2 = 0;
            int limit2 = limit - result1.size();
            List<Result> result2 = new DbSearch(mailbox, dumpster).search(conn, node2, sort, offset2, limit2, fetch, false);
            result1.addAll(result2);
            }
        return result1;
        }


    private static DbSearchConstraints.Leaf findDraftsConstraint(DbSearchConstraints node) {
            if (node instanceof DbSearchConstraints.Leaf) {
                for (Folder folder: ((DbSearchConstraints.Leaf) node).folders) {
                    if(folder.getId() == Mailbox.ID_FOLDER_DRAFTS) {
                        return node.toLeaf();
                    }
                }
            } else {
                for (DbSearchConstraints child: node.getChildren()) {
                    DbSearchConstraints.Leaf drafts = findDraftsConstraint(child);
                    if(drafts != null) {
                        return drafts;
                    }
                }
            }
            return null;
        }

    private Object getSortKey(ResultSet rs, SortBy sort) throws SQLException {
        switch (sort.getKey()) {
            case NONE: // no sort column in the result set for NONE
                return null;
            case SUBJECT:
            case SENDER:
            case RCPT:
            case NAME:
            case NAME_NATURAL_ORDER:
            case UNREAD:
            case ATTACHMENT:
            case FLAG:
            case PRIORITY:
                return Strings.nullToEmpty(rs.getString(SORT_COLUMN_ALIAS));
            case SIZE:
                return Long.valueOf(rs.getInt(SORT_COLUMN_ALIAS));
            case ID:
                return Integer.valueOf(rs.getInt(SORT_COLUMN_ALIAS));
            case RELEVANCE:
                return luceneResults.getScore(rs.getInt(DbMailItem.CI_INDEX_ID));
            case DATE:
            default:
                // Assuming this multiplication by 1000 is intended for DATE in order to convert from a
                // UNIX time in seconds to milliseconds since epoc
                // seems odd to also do this for the default case though...
                return Long.valueOf(rs.getInt(SORT_COLUMN_ALIAS) * 1000L);
        }
    }

    private boolean encode(String column, boolean bool, Object o) {
        return encode( column, bool, o, true);
    }

    private boolean encode(String column, boolean bool, Object o, boolean and) {
        if (and) {sql.append(" AND "); }
        sql.append(column).append(bool ? " = ?" : " != ?");
        params.add(o);
        return true;
    }

    private boolean encodeFolder(Set<Folder> folders, boolean bool) {
        return encodeFolder(folders, bool, true);
    }

    private boolean encodeFolder(Set<Folder> folders, boolean bool, boolean and) {
        if (folders.isEmpty()) {
            return false;
        }
        if (and) { sql.append(" AND ");}
        sql.append(DbUtil.whereIn("mi.folder_id", bool, folders.size()));
        for (Folder folder : folders) {
            params.add(folder.getId());
        }
        return true;
    }

    private boolean encodeType(Set<MailItem.Type> types, boolean bool) {
        return encodeType(types, bool, true);
    }
    private boolean encodeType(Set<MailItem.Type> types, boolean bool, boolean and) {
        if (types.isEmpty()) {
            return false;
        }
        if (and) {sql.append(" AND "); }
        sql.append(DbUtil.whereIn("type", bool, types.size()));
        for (MailItem.Type type : types) {
            params.add(type.toByte());
        }
        return true;
    }

    private boolean encode(String column, boolean bool, Collection<?> c) {
        return encode(column, bool, c, true);
    }

    private boolean encode(String column, boolean bool, Collection<?> c, boolean and) {
        if (ListUtil.isEmpty(c)) {
            return false;
        }
        if (and) {sql.append(" AND "); }
        sql.append(DbUtil.whereIn(column, bool, c.size()));
        params.addAll(c);
        return true;
    }

    private boolean encode(String column, boolean bool, byte[] array) {
        return encode(column, bool, array, true);
    }

    private boolean encode(String column, boolean bool, byte[] array, boolean and) {
        if (array == null || array.length == 0) {
            return false;
        }
        if (and) {sql.append(" AND "); }
        sql.append(DbUtil.whereIn(column, bool, array.length));
        for (byte b : array) {
            params.add(b);
        }
        return true;
    }

    private boolean encodeNoRecipients(boolean excludeHasRecipients) {
        return encodeNoRecipients(excludeHasRecipients, true);
    }

    private boolean encodeNoRecipients(boolean excludeHasRecipients, boolean and) {
        if (excludeHasRecipients) {
            if (and) {sql.append(" AND "); }
            sql.append("mi.recipients is NULL");
            return true;
        } else {
            return false;
        }
    }

    private boolean encodeTag(Set<Tag> tags, boolean bool, boolean useJoin) {
        return encodeTag(tags, bool, useJoin, true);

    }
    private boolean encodeTag(Set<Tag> tags, boolean bool, boolean useJoin, boolean and) {
        if (tags.isEmpty()) {
            return false;
        }

        if (useJoin) {
            assert !dumpster;
            assert bool;
        }

        if (dumpster) { // There is no corresponding table to TAGGED_ITEM in dumpster, hence brute-force search.
            int flags = 0;
            for (Tag tag : tags) {
                if (tag instanceof Flag) {
                    Flag flag = (Flag) tag;
                    if (flag.getId() == Flag.ID_UNREAD) {
                        if (and) {sql.append(" AND "); }
                        sql.append("mi.unread = ?");
                        and = true;
                        params.add(bool ? 1 : 0);
                    } else {
                        flags |= flag.toBitmask();
                    }
                } else {
                    if (and) {sql.append(" AND "); }
                    and = true;
                    sql.append("(mi.tag_names");
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
            if (useJoin) {
                assert tags.size() == 1;

                Tag tag = tags.iterator().next();

                //AND (mi.id = ti.item_id AND mi.mailbox_id = ti.mailbox_id AND ti.tag_id = -10)
                if (and) {sql.append(" AND "); }
                sql.append("(mi.id = ti.item_id AND mi.mailbox_id = ti.mailbox_id AND ti.tag_id = ?)");
                params.add(tag.getId());
                and = true;
            } else {
                for (Tag tag : tags) {
                    if (and) {sql.append(" AND "); }
                    sql.append("EXISTS (SELECT * FROM ").append(DbTag.getTaggedItemTableName(mailbox, "ti"));
                    and = true;
                    sql.append(" WHERE ");
                    if (!DebugConfig.disableMailboxGroups) {
                        sql.append("mi.mailbox_id = ti.mailbox_id AND ");
                    }
                    sql.append("mi.id = ti.item_id AND ti.tag_id = ?)");
                    params.add(tag.getId());
                }
            }
        } else { // NOT EXISTS (SELECT... WHERE... tag_id IN...)
            if (and) {sql.append(" AND "); }
            sql.append("NOT EXISTS (SELECT * FROM ").append(DbTag.getTaggedItemTableName(mailbox, "ti"));
            and = true;
            sql.append(" WHERE ");
            if (!DebugConfig.disableMailboxGroups) {
                sql.append("mi.mailbox_id = ti.mailbox_id AND ");
            }
            sql.append("mi.id = ti.item_id AND ").append(DbUtil.whereIn("ti.tag_id", tags.size())).append(')');
            for (Tag tag : tags) {
                params.add(tag.getId());
            }
        }
        return true;
    }

    private boolean encodeDateRange(String column, DbSearchConstraints.NumericRange range) {
        return encodeDateRange(column, range, true);
    }

    private boolean encodeTimestampRange(String column, DbSearchConstraints.NumericRange range, long cutoff) {
        return encodeTimestampRange(column, range, cutoff, true);
    }

    private boolean encodeLongRange(String column, DbSearchConstraints.NumericRange range, long cutoff) {
        return encodeLongRange(column, range, cutoff, true);
    }

    private boolean encodeIntRange(String column, DbSearchConstraints.NumericRange range, int cutoff) {
        return encodeIntRange(column, range, cutoff, true);
    }

    private boolean encodeDateRange(String column, DbSearchConstraints.NumericRange range, boolean and) {
        return encodeRange(column, range, 1L, (int) Math.min(range.min / 1000, Integer.MAX_VALUE),
                          (int) Math.min(range.max / 1000, Integer.MAX_VALUE), and);
    }

    private boolean encodeTimestampRange(String column, DbSearchConstraints.NumericRange range, long cutoff, boolean and) {
        return encodeRange(column, range, cutoff, new Timestamp(range.min), new Timestamp(range.max), and);
    }

    private boolean encodeLongRange(String column, DbSearchConstraints.NumericRange range, long cutoff, boolean and) {
        return encodeRange(column, range, cutoff, range.min, range.max, and);
    }

    private boolean encodeIntRange(String column, DbSearchConstraints.NumericRange range, int cutoff, boolean and) {
        return encodeRange(column, range, cutoff, (int) range.min, (int) range.max, and);
    }

    private boolean encodeRange(String column, DbSearchConstraints.NumericRange range,
            long cutoff, Object min, Object max) {
        return encodeRange(column, range, cutoff, min, max, true);
    }

    private boolean encodeRange(String column, DbSearchConstraints.NumericRange range,
            long cutoff, Object min, Object max, boolean and) {
        if (Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON) && isCaseSensitiveField(column)) {
            column = "UPPER(" + column + ")";
        }
        boolean lowValid = range.min >= cutoff;
        boolean highValid = range.max >= cutoff;
        if (!(lowValid || highValid)) {
            return false;
        }
        if (and) {sql.append(" AND "); }
        sql.append(range.bool ? "(" : "NOT (");
        and = true;
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
        return true;
    }

    private boolean encodeStringRange(String column, DbSearchConstraints.StringRange range) {
        return encodeStringRange(column, range, true);
    }

    private boolean encodeStringRange(String column, DbSearchConstraints.StringRange range, boolean and) {
        if (Db.supports(Db.Capability.CASE_SENSITIVE_COMPARISON) && isCaseSensitiveField(column)) {
            column = "UPPER(" + column + ")";
        }
        if (and) {sql.append(" AND "); }
        sql.append(range.bool ?  "(" : "NOT (");
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
        return true;
    }

    private boolean encodeCursorRange(DbSearchConstraints.CursorRange range) {
        return encodeCursorRange(range, true);
    }

    private boolean encodeCursorRange(DbSearchConstraints.CursorRange range, boolean and) {
        // Can't use SORT_COLUMN_ALIAS because column aliases in SELECT are illegal to use in WHERE
        String col = toSortField(range.sortBy);
        if (and) {sql.append(" AND "); }
        sql.append("(");
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
        return true;
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
                    sortValue instanceof Long || sortValue instanceof Integer ||
                    sortValue instanceof Float) : sortValue;
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
            return MoreObjects.toStringHelper(this)
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
        private final ImapMessage i4msg;

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
                case RELEVANCE:
                    float score1 = (Float) o1.getSortValue();
                    float score2 = (Float) o2.getSortValue();
                    if (score1 != score2) {
                        float diff;
                        if (sort.getDirection() == SortBy.Direction.DESC) {
                            diff = score2 - score1;
                        } else {
                            diff = score1 - score2;
                        }
                        return (diff > 0) ? 1 : -1;
                    }
                    break;
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
