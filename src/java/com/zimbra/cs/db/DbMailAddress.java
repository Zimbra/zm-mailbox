/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
import java.util.HashSet;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;

/**
 * DAO for {@code MAIL_ADDRESS} table.
 *
 * @author ysasaki
 */
public final class DbMailAddress {
    private final Mailbox mailbox;
    private int id;
    private String address;
    private int contactCount = 0;

    public DbMailAddress(Mailbox mbox) {
        mailbox = mbox;
    }

    public DbMailAddress setId(int value) {
        id = value;
        return this;
    }

    public DbMailAddress setAddress(String value) {
        address = value;
        return this;
    }

    public DbMailAddress setContactCount(int value) {
        contactCount = value;
        return this;
    }

    static String getTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, "mail_address");
    }

    public int create() throws ServiceException {
        return create(mailbox.getOperationConnection());
    }

    @VisibleForTesting
    int create(DbConnection conn) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(new SQL(mailbox).add("INSERT INTO ").mail_address()
                    .add(" (").addIf("mailbox_id, ").add("id, address, contact_count) VALUES (")
                    .addIf("?, ").add("?, ?, ?)").build());
            int pos = SQL.setIf(stmt, 1, mailbox.getId());
            stmt.setInt(pos++, id);
            stmt.setString(pos++, address);
            stmt.setInt(pos, contactCount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to create", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
        return id;
    }

    public static int delete(DbConnection conn, Mailbox mbox) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(new SQL(mbox).add("DELETE FROM ").mail_address()
                    .addIf(" WHERE mailbox_id = ?").build());
            SQL.setIf(stmt, 1, mbox.getId());
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to delete all", e);
        } finally {
            conn.closeQuietly(stmt);
        }
    }

    public static int getLastId(DbConnection conn, Mailbox mbox) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(new SQL(mbox).add("SELECT MAX(id) FROM ").mail_address()
                    .addIf(" WHERE mailbox_id = ?").build());
            SQL.setIf(stmt, 1, mbox.getId());
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to get last ID", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    public static int getId(DbConnection conn, Mailbox mbox, String addr) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(new SQL(mbox).add("SELECT id FROM ").mail_address()
                    .add(" WHERE ").addIf("mailbox_id = ? AND ").add("address = ?").build());
            int pos = SQL.setIf(stmt, 1, mbox.getId());
            stmt.setString(pos, addr);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return -1;
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to get ID", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    /**
     * Returns a subset of the specified email addresses. Each email address in the returned set is referred by at least
     * one contact.
     */
    public static Set<String> existsInContacts(DbConnection conn, Mailbox mbox, Set<String> addrs)
            throws ServiceException {
        if (addrs.isEmpty()) {
            return addrs;
        }

        SQL sql = new SQL(mbox).add("SELECT address FROM ").mail_address().add(" WHERE ").addIf("mailbox_id = ? AND ");
        sql.add("contact_count > ? AND ");
        if (addrs.size() == 1) {
            sql.add("address = ?");
        } else {
            sql.add("address IN (").add(Strings.repeat("?, ", addrs.size() - 1)).add("?)");
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql.build());
            int pos = SQL.setIf(stmt, 1, mbox.getId());
            stmt.setInt(pos++, 0);
            for (String addr : addrs) {
                stmt.setString(pos++, addr);
            }
            rs = stmt.executeQuery();
            Set<String> result = new HashSet<String>();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            return result;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to search addresses", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    public static void incCount(DbConnection conn, Mailbox mbox, int id) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(new SQL(mbox).add("UPDATE ").mail_address()
                    .add(" SET contact_count = contact_count + 1 WHERE ").addIf("mailbox_id = ? AND ")
                    .add("id = ?").build());
            int pos = SQL.setIf(stmt, 1, mbox.getId());
            stmt.setInt(pos, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to increment contact count", e);
        } finally {
            conn.closeQuietly(stmt);
        }
    }

    public static void decCount(DbConnection conn, Mailbox mbox, String addr) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(new SQL(mbox).add("UPDATE ").mail_address()
                    .add(" SET contact_count = contact_count - 1 WHERE ").addIf("mailbox_id = ? AND ")
                    .add("address = ?").build());
            int pos = SQL.setIf(stmt, 1, mbox.getId());
            stmt.setString(pos, addr);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to decrement contact count", e);
        } finally {
            conn.closeQuietly(stmt);
        }
    }

    public static int getCount(DbConnection conn, Mailbox mbox, int id) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(new SQL(mbox).add("SELECT contact_count FROM ").mail_address()
                    .add(" WHERE ").addIf("mailbox_id = ? AND ").add("id = ?").build());
            int pos = SQL.setIf(stmt, 1, mbox.getId());
            stmt.setInt(pos, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return -1;
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to get contact count", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    public static int getCount(DbConnection conn, Mailbox mbox, String addr) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(new SQL(mbox).add("SELECT contact_count FROM ").mail_address()
                    .add(" WHERE ").addIf("mailbox_id = ? AND ").add("address = ?").build());
            int pos = SQL.setIf(stmt, 1, mbox.getId());
            stmt.setString(pos, addr);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return -1;
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to get contact count", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    /**
     * Deletes orphan rows. Orphans are 1) contact_count = 0 and 2) referred by no MAIL_ITEM.SENDER_ID.
     *
     * @return number of rows deleted
     */
    public static int purge(DbConnection conn, Mailbox mbox) throws ServiceException {
        // In MySQL (single-table syntax), can't use "AS alias" after "DELETE FROM table".
        SQL sql = new SQL(mbox).add("DELETE FROM ").mail_address().add(" WHERE ").addIf("mailbox_id = ? AND ")
            .add("contact_count = ? AND NOT EXISTS (SELECT * FROM ").mail_item().add(" WHERE ");
        if (SQL.isMailboxGrouped()) {
            sql.add("mailbox_id = ").mail_address().add(".mailbox_id AND ");
        }
        sql.add("sender_id = ").mail_address().add(".id)");

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql.build());
            int pos = SQL.setIf(stmt, 1, mbox.getId());
            stmt.setInt(pos, 0);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to purge", e);
        } finally {
            conn.closeQuietly(stmt);
        }
    }

    public static int rebuild(DbConnection conn, Mailbox mbox, int lastAddressId) throws ServiceException {
        String[] emailFields = Contact.getEmailFields(mbox.getAccount());
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // reset all counts
            stmt = conn.prepareStatement(new SQL(mbox).add("UPDATE ").mail_address()
                    .add(" SET contact_count = ?").addIf(" WHERE mailbox_id = ?").build());
            int pos = 1;
            stmt.setInt(pos++, 0);
            pos = SQL.setIf(stmt, pos, mbox.getId());
            stmt.executeUpdate();
            stmt.close();

            // extract email addresses from all contacts, and put them into mail_address
            stmt = conn.prepareStatement(new SQL(mbox).add("SELECT metadata FROM ").mail_item()
                    .add(" WHERE ").addIf("mailbox_id = ? AND ").add("type = ?").build());
            pos = SQL.setIf(stmt, 1, mbox.getId());
            stmt.setByte(pos, MailItem.Type.CONTACT.toByte());
            rs = stmt.executeQuery();
            while (rs.next()) {
                Metadata metadata = new Metadata(rs.getString(1));
                Metadata fields = metadata.getMap(Metadata.FN_FIELDS);
                Set<String> emails = new HashSet<String>(); // merge duplicates
                for (String name : emailFields) {
                    String addr = fields.get(name, null);
                    if (!Strings.isNullOrEmpty(addr)) {
                        emails.add(addr.trim().toLowerCase());
                    }
                }
                for (String addr : emails) {
                    int senderId = DbMailAddress.getId(conn, mbox, addr);
                    if (senderId < 0) {
                        new DbMailAddress(mbox).setId(++lastAddressId).setAddress(addr).setContactCount(1).create(conn);
                    } else {
                        DbMailAddress.incCount(conn, mbox, senderId);
                    }
                }
            }
            return lastAddressId;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to rebuild", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    private static final class SQL {
        private final StringBuilder out = new StringBuilder();
        private final Mailbox mailbox;

        SQL(Mailbox mbox) {
            mailbox = mbox;
        }

        SQL add(String str) {
            out.append(str);
            return this;
        }

        SQL addIf(String str) {
            if (isMailboxGrouped()) {
                out.append(str);
            }
            return this;
        }

        SQL mail_item() {
            out.append(DbMailItem.getMailItemTableName(mailbox));
            return this;
        }

        SQL mail_address() {
            out.append(getTableName(mailbox));
            return this;
        }

        String build() {
            return out.toString();
        }

        static boolean isMailboxGrouped() {
            return !DebugConfig.disableMailboxGroups;
        }

        static int setIf(PreparedStatement stmt, int pos, int value) throws SQLException {
            if (!DebugConfig.disableMailboxGroups) {
                stmt.setInt(pos++, value);
            }
            return pos;
        }
    }

}
