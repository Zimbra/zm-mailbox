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

import com.google.common.base.Strings;
import com.zimbra.common.mime.InternetAddress;
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

    static String getTableName(Mailbox mbox) {
        return DbMailbox.qualifyTableName(mbox, "mail_address");
    }

    public static int save(DbConnection conn, Mailbox mbox, String addr, int count) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + getTableName(mbox) +
                    " (mailbox_id, address, contact_count) VALUES (?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, addr);
            stmt.setInt(3, count);
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw ServiceException.FAILURE("Failed to get mail address ID", null);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to save mail address", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    public static int delete(DbConnection conn, Mailbox mbox) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + getTableName(mbox) + " WHERE mailbox_id = ?");
            stmt.setInt(1, mbox.getId());
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to delete mail addresses", e);
        } finally {
            conn.closeQuietly(stmt);
        }
    }

    public static int getId(DbConnection conn, Mailbox mbox, String addr) throws ServiceException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT id FROM " + getTableName(mbox) +
                    " WHERE mailbox_id = ? AND address = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, addr);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return -1;
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to get mail address ID", e);
        } finally {
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

    public static void incCount(DbConnection conn, Mailbox mbox, int id) throws ServiceException {
        PreparedStatement stmt = null;
        try {
            // ID is unique in the table, but specify mailbox_id as a precaution
            stmt = conn.prepareStatement("UPDATE " + getTableName(mbox) +
                    " SET contact_count = contact_count + 1 WHERE mailbox_id = ? AND id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setInt(2, id);
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
            stmt = conn.prepareStatement("UPDATE " + getTableName(mbox) +
                    " SET contact_count = contact_count - 1 WHERE mailbox_id = ? AND address = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, addr);
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
            // ID is unique in the table, but specify mailbox_id as a precaution
            stmt = conn.prepareStatement("SELECT contact_count FROM " + getTableName(mbox) +
                    " WHERE mailbox_id = ? AND id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setInt(2, id);
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
            // ID is unique in the table, but specify mailbox_id as a precaution
            stmt = conn.prepareStatement("SELECT contact_count FROM " + getTableName(mbox) +
                    " WHERE mailbox_id = ? AND address = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setString(2, addr);
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

    public static void rebuild(DbConnection conn, Mailbox mbox) throws ServiceException {
        String[] emailFields = Contact.getEmailFields(mbox.getAccount());
        PreparedStatement stmt = null;
        PreparedStatement inner = null;
        ResultSet rs = null;
        try {
            // clear all sender_id in mail_item
            stmt = conn.prepareStatement("UPDATE " + DbMailItem.getMailItemTableName(mbox) +
                    " SET sender_id = NULL WHERE mailbox_id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.executeUpdate();
            stmt.close();
            // delete all rows in mail_address
            DbMailAddress.delete(conn, mbox);

            // extract email addresses from all contacts, and put them into mail_address
            stmt = conn.prepareStatement("SELECT metadata FROM " + DbMailItem.getMailItemTableName(mbox) +
                    " WHERE mailbox_id = ? AND type = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setByte(2, MailItem.Type.CONTACT.toByte());
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
                        DbMailAddress.save(conn, mbox, addr, 1);
                    } else {
                        DbMailAddress.incCount(conn, mbox, senderId);
                    }
                }
            }
            rs.close();
            stmt.close();

            // extract sender addresses from all messages, put them into mail_address, and set sender_id
            stmt = conn.prepareStatement("SELECT id, sender FROM " + DbMailItem.getMailItemTableName(mbox) +
                    " WHERE mailbox_id = ? AND type = ? AND sender IS NOT NULL");
            stmt.setInt(1, mbox.getId());
            stmt.setByte(2, MailItem.Type.MESSAGE.toByte());
            rs = stmt.executeQuery();
            while (rs.next()) {
                int itemId = rs.getInt(1);
                String sender = rs.getString(2);
                for (InternetAddress iaddr : InternetAddress.parseHeader(sender)) {
                    String addr = iaddr.getAddress();
                    if (!Strings.isNullOrEmpty(addr)) {
                        addr = addr.trim().toLowerCase();
                        int senderId = DbMailAddress.getId(conn, mbox, addr);
                        if (senderId < 0) {
                            senderId = DbMailAddress.save(conn, mbox, addr, 0);
                        }
                        inner = conn.prepareStatement("UPDATE " + DbMailItem.getMailItemTableName(mbox) +
                                " SET sender_id = ? WHERE mailbox_id = ? AND id = ?");
                        inner.setInt(1, senderId);
                        inner.setInt(2, mbox.getId());
                        inner.setInt(3, itemId);
                        inner.executeUpdate();
                        inner.close();
                    }
                }
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("Failed to rebuild Mail Address Directory", e);
        } finally {
            conn.closeQuietly(inner);
            conn.closeQuietly(rs);
            conn.closeQuietly(stmt);
        }
    }

}
