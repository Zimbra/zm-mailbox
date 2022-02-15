/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2021 Synacor, Inc.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.event.MailboxEvent;
import com.zimbra.cs.mailbox.event.MailboxEvent.EventFilter;

public class DbEvent {

    public static final String TABLE_EVENT = "event";
    public static final String TABLE_WATCH = "watch";

    private static void commit(DbConnection conn, boolean succeeded) throws ServiceException {
        try {
            if (succeeded) {
                conn.commit();
            } else {
                conn.rollback();
            }
        } finally {
            DbPool.quietClose(conn);
        }
    }

    public static void logEvent(Mailbox mbox, MailboxEvent ev) throws ServiceException {
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        boolean succeeded = false;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + getEventTableName(mbox) +
                        "(mailbox_id, account_id, item_id, version, folder_id, op, ts, user_agent, arg) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setString(pos++, ev.getAccountId());
            stmt.setInt(pos++, ev.getItemId().getId());
            stmt.setInt(pos++, ev.getVersion());
            stmt.setInt(pos++, ev.getFolderId());
            stmt.setByte(pos++, (byte)ev.getOperation().getCode());
            stmt.setInt(pos++, (int)(ev.getTimestamp() / 1000L));
            stmt.setString(pos++, ev.getUserAgent());
            stmt.setString(pos++, ev.getArgString());

            if (stmt.executeUpdate() != 1) {
                throw ServiceException.FAILURE("can't add event", null);
            }
            succeeded = true;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("can't add event", e);
        } finally {
            DbPool.closeStatement(stmt);
            commit(conn, succeeded);
        }
    }

    public static void updateEvent(Mailbox mbox, MailboxEvent ev) throws ServiceException {
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        boolean succeeded = false;
        try {
            stmt = conn.prepareStatement("UPDATE " + getEventTableName(mbox) +
                        " set ts = ?, version = ?, user_agent = ?, arg = ? " +
                        "where mailbox_id = ? AND account_id = ? AND item_id = ? AND folder_id = ? AND op = ?");
            int pos = 1;
            stmt.setInt(pos++, (int)(ev.getTimestamp() / 1000L));
            stmt.setInt(pos++, ev.getVersion());
            stmt.setString(pos++, ev.getUserAgent());
            stmt.setString(pos++, ev.getArgString());

            stmt.setInt(pos++, mbox.getId());
            stmt.setString(pos++, ev.getAccountId());
            stmt.setInt(pos++, ev.getItemId().getId());
            stmt.setInt(pos++, ev.getFolderId());
            stmt.setByte(pos++, (byte)ev.getOperation().getCode());

            if (stmt.executeUpdate() != 1) {
                ZimbraLog.activity.info("Update event failed while updating event table");
                throw ServiceException.FAILURE("can't update event", null);
            }
            succeeded = true;
        } catch (SQLException e) {
            ZimbraLog.activity.info("Update event failed while updating event table");
            throw ServiceException.FAILURE("can't update event", e);
        } finally {
            DbPool.closeStatement(stmt);
            commit(conn, succeeded);
        }
    }

    public static Collection<MailboxEvent> getEventsForItem(Mailbox mbox, int itemId, int offset, int count, EventFilter filter) throws ServiceException {
        return getEvents(mbox, Arrays.asList(itemId), offset, count, filter);
    }

    private static String listIds(Collection<Integer> itemIds) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < itemIds.size(); i++) {
            if (ret.length() > 0)
                ret.append(" OR");
            ret.append(" folder_id = ? OR item_id = ?");
        }
        return ret.toString();
    }

    private static String addLimit(EventFilter filter) {
        StringBuilder ret = new StringBuilder();
        if (!filter.ids.isEmpty()) {
            ret.append(" AND ");
            ret.append(DbUtil.whereIn("account_id", filter.ids.size()));
        }
        if (!filter.ops.isEmpty()) {
            ret.append(" AND ");
            ret.append(DbUtil.whereIn("op", filter.ops.size()));
        }
        if (filter.since > 0) {
            ret.append(" AND ");
            ret.append("ts > ").append(filter.since);
        }
        return ret.toString();
    }

    public static Collection<MailboxEvent> getEvents(Mailbox mbox, Collection<Integer> itemIds, int offset, int count, EventFilter filter) throws ServiceException {
        ArrayList<MailboxEvent> events = new ArrayList<MailboxEvent>();
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean succeeded = false;
        try {
            stmt = conn.prepareStatement("SELECT " +
                    " account_id, op, item_id, version, folder_id, ts, user_agent, arg" +
                    " FROM " + getEventTableName(mbox) +
                    " WHERE " + "mailbox_id = ? AND" +
                    "   (" + listIds(itemIds) + ")" +
                    addLimit(filter) +
                    " ORDER BY ts DESC LIMIT " + offset + "," + count);
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            for (int itemId : itemIds) {
                stmt.setInt(pos++, itemId);
                stmt.setInt(pos++, itemId);
            }
            for (String id : filter.ids) {
                stmt.setString(pos++, id);
            }
            for (MailboxOperation op : filter.ops) {
                stmt.setByte(pos++, (byte)op.getCode());
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                pos = 1;
                events.add(new MailboxEvent(rs.getString(pos++), MailboxOperation.fromInt(rs.getByte(pos++)), mbox.getAccountId(), rs.getInt(pos++), rs.getInt(pos++), rs.getInt(pos++), (rs.getInt(pos++) * 1000L), rs.getString(pos++), rs.getString(pos++)));
            }
            succeeded = true;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("can't fetch events for Mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            commit(conn, succeeded);
        }
        return events;
    }

    public static Collection<MailboxEvent> getEventsBefore(Mailbox mbox, Collection<Integer> itemIds, long timestamp, int count, EventFilter filter) throws ServiceException {
        ArrayList<MailboxEvent> events = new ArrayList<MailboxEvent>();
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean succeeded = false;
        try {
            stmt = conn.prepareStatement("SELECT " +
                    " account_id, op, item_id, version, folder_id, ts, user_agent, arg" +
                    " FROM " + getEventTableName(mbox) +
                    " WHERE " + "mailbox_id = ? AND ts < ? AND " +
                    "   (" + listIds(itemIds) + ")" +
                    addLimit(filter) +
                    " ORDER BY ts DESC LIMIT " + count);
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setInt(pos++, (int)(timestamp / 1000L));
            for (int itemId : itemIds) {
                stmt.setInt(pos++, itemId);
                stmt.setInt(pos++, itemId);
            }
            for (String id : filter.ids) {
                stmt.setString(pos++, id);
            }
            for (MailboxOperation op : filter.ops) {
                stmt.setByte(pos++, (byte)op.getCode());
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                pos = 1;
                events.add(new MailboxEvent(rs.getString(pos++), MailboxOperation.fromInt(rs.getByte(pos++)), mbox.getAccountId(), rs.getInt(pos++), rs.getInt(pos++), rs.getInt(pos++), (rs.getInt(pos++) * 1000L), rs.getString(pos++), rs.getString(pos++)));
            }
            succeeded = true;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("can't fetch events for Mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            commit(conn, succeeded);
        }
        return events;
    }

    public static int getEventCount(Mailbox mbox, Collection<Integer> itemIds, EventFilter filter) throws ServiceException {
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean succeeded = false;
        try {
            stmt = conn.prepareStatement("SELECT count(*)" +
                    " FROM " + getEventTableName(mbox) +
                    " WHERE " + "mailbox_id = ? AND " +
                    "   (" + listIds(itemIds) + ")" +
                    addLimit(filter)
                    );
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            for (int itemId : itemIds) {
                stmt.setInt(pos++, itemId);
                stmt.setInt(pos++, itemId);
            }
            for (String id : filter.ids) {
                stmt.setString(pos++, id);
            }
            for (MailboxOperation op : filter.ops) {
                stmt.setByte(pos++, (byte)op.getCode());
            }
            rs = stmt.executeQuery();

            succeeded = true;
            if (!rs.next()) {
                return 0;
            }
            return rs.getInt(1);
        } catch (SQLException e) {
            throw ServiceException.FAILURE("can't fetch event count for Mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            commit(conn, succeeded);
        }
    }

    public static Collection<MailboxOperation> getEventOps(Mailbox mbox, Collection<Integer> itemIds, EventFilter filter) throws ServiceException {
        EnumSet<MailboxOperation> ops = EnumSet.noneOf(MailboxOperation.class);
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean succeeded = false;
        try {
            stmt = conn.prepareStatement("SELECT " +
                    " DISTINCT op" +
                    " FROM " + getEventTableName(mbox) +
                    " WHERE " + "mailbox_id = ? AND " +
                    "   (" + listIds(itemIds) + ")" +
                    addLimit(filter)
                    );
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            for (int itemId : itemIds) {
                stmt.setInt(pos++, itemId);
                stmt.setInt(pos++, itemId);
            }
            for (String id : filter.ids) {
                stmt.setString(pos++, id);
            }
            for (MailboxOperation op : filter.ops) {
                stmt.setByte(pos++, (byte)op.getCode());
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                pos = 1;
                ops.add(MailboxOperation.fromInt(rs.getByte(pos++)));
            }
            succeeded = true;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("can't fetch event operations for Mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            commit(conn, succeeded);
        }
        return ops;
    }

    public static Collection<String> getEventUsers(Mailbox mbox, Collection<Integer> itemIds, EventFilter filter) throws ServiceException {
        ArrayList<String> users = new ArrayList<String>();
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean succeeded = false;
        try {
            stmt = conn.prepareStatement("SELECT " +
                    " DISTINCT account_id" +
                    " FROM " + getEventTableName(mbox) +
                    " WHERE " + "mailbox_id = ? AND " +
                    "   (" + listIds(itemIds) + ")" +
                    addLimit(filter)
                    );
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            for (int itemId : itemIds) {
                stmt.setInt(pos++, itemId);
                stmt.setInt(pos++, itemId);
            }
            for (String id : filter.ids) {
                stmt.setString(pos++, id);
            }
            for (MailboxOperation op : filter.ops) {
                stmt.setByte(pos++, (byte)op.getCode());
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                pos = 1;
                users.add(rs.getString(pos++));
            }
            succeeded = true;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("can't fetch event users for Mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            commit(conn, succeeded);
        }
        return users;
    }

    public static void addWatch(Mailbox mbox, String targetAccountId, int itemId) throws ServiceException {
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        boolean succeeded = false;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + getWatchTableName(mbox) +
                        " (mailbox_id, target, item_id) " +
                        " VALUES (?, ?, ?)");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setString(pos++, targetAccountId);
            stmt.setInt(pos++, itemId);

            if (stmt.executeUpdate() != 1) {
                throw ServiceException.FAILURE("can't add watch mapping", null);
            }
            succeeded = true;
        } catch (SQLException e) {
            // catch item_id uniqueness constraint violation and return failure
            if (com.zimbra.cs.db.Db.errorMatches(e, com.zimbra.cs.db.Db.Error.DUPLICATE_ROW)) {
                throw MailServiceException.ALREADY_EXISTS("watch mapping already exists", e);
            } else {
                throw ServiceException.FAILURE("can't add watch mapping", e);
            }
        } finally {
            DbPool.closeStatement(stmt);
            commit(conn, succeeded);
        }
    }

    public static void removeWatch(Mailbox mbox, String targetAccountId, int itemId) throws ServiceException {
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        boolean succeeded = false;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + getWatchTableName(mbox) +
                        " WHERE mailbox_id = ? AND target = ? AND item_id = ?");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setString(pos++, targetAccountId);
            stmt.setInt(pos++, itemId);
            stmt.executeUpdate();
            succeeded = true;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("can't delete watch mapping", e);
        } finally {
            DbPool.closeStatement(stmt);
            commit(conn, succeeded);
        }
    }

    public static Collection<Pair<String,Integer>> getWatchingItems(Mailbox mbox) throws ServiceException {
        ArrayList<Pair<String,Integer>> items = new ArrayList<Pair<String,Integer>>();
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean succeeded = false;
        try {
            stmt = conn.prepareStatement("SELECT target, item_id FROM " + getWatchTableName(mbox) +
                    " WHERE mailbox_id = ?");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            rs = stmt.executeQuery();

            while (rs.next()) {
                pos = 1;
                Pair<String,Integer> item = new Pair<String,Integer>(rs.getString(pos++), rs.getInt(pos++));
                items.add(item);
            }
            succeeded = true;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("can't fetch watching items for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            commit(conn, succeeded);
        }
        return items;
    }

    public static Collection<Integer> getWatchingItems(Mailbox mbox, String targetAccountId) throws ServiceException {
        ArrayList<Integer> items = new ArrayList<Integer>();
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean succeeded = false;
        try {
            stmt = conn.prepareStatement("SELECT item_id FROM " + getWatchTableName(mbox) +
                    " WHERE mailbox_id = ? AND target = ?");
            int pos = 1;
            stmt.setInt(pos++, mbox.getId());
            stmt.setString(pos++, targetAccountId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                pos = 1;
                items.add(rs.getInt(pos++));
            }
            succeeded = true;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("can't fetch watching items for mailbox " + mbox.getId(), e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            commit(conn, succeeded);
        }
        return items;
    }

    public static String getEventTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_EVENT);
    }

    public static String getEventTableName(Mailbox mbox, String alias) {
        return getEventTableName(mbox) + " AS " + alias;
    }

    public static String getWatchTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, TABLE_WATCH);
    }
}