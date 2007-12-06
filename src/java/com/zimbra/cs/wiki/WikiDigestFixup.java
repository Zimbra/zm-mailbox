/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.wiki;

import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;

/**
 * Migration utility to fix the mail_item.blob_digest database column
 * for Wiki items (bug 9693)
 * 
 * @author jhahm
 *
 */
public class WikiDigestFixup {

    private static StoreManager sStore;

    private static class Mbox {
        private int mId;
        private String mEmail;
        public Mbox(int id, String email) {
            mId = id;
            mEmail = email;
        }
        public int getId() { return mId; }
        public String getEmail() { return mEmail; }
    }

    private static List<Mbox> getMboxList(Connection conn) throws SQLException {
        List<Mbox> list = new ArrayList<Mbox>(1000);
        String sql = "SELECT id, comment FROM mailbox ORDER BY id";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt(1);
                String email = rs.getString(2);
                list.add(new Mbox(id, email));
            }
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
        return list;
    }

    private static class WikiDigest {
        private int mMboxId;
        private int mItemId;
        private String mDigest;

        public WikiDigest(int mboxId, int itemId, String digest) {
            mMboxId = mboxId;
            mItemId = itemId;
            mDigest = digest;
        }

        public int getMboxid() { return mMboxId; }
        public int getItemId() { return mItemId; }
        public String getDigest() { return mDigest; }
    }

    private static List<WikiDigest> getWikiDigests(int mboxId) throws IOException, ServiceException {
        Mailbox mbox = null;
        try {
            mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        } catch (ServiceException e) {
            String code = e.getCode();
            if (AccountServiceException.NO_SUCH_ACCOUNT.equals(code) ||
                ServiceException.WRONG_HOST.equals(code))
                return null;
            else
                throw e;
        }
        OperationContext octxt = new OperationContext(mbox);
        List<MailItem> items = new ArrayList<MailItem>();
        List<MailItem> wikis = mbox.getItemList(octxt, MailItem.TYPE_WIKI);
        if (wikis != null && wikis.size() > 0)
            items.addAll(wikis);
        List<MailItem> documents = mbox.getItemList(octxt, MailItem.TYPE_DOCUMENT);
        if (documents != null && documents.size() > 0)
            items.addAll(documents);
        int len = items != null ? items.size() : 0;
        List<WikiDigest> list = new ArrayList<WikiDigest>(len);
        if (len > 0) {
            for (MailItem item : items) {
                // didn't support >2GB wiki items when the bug was occurring
                if (item.getSize() > Integer.MAX_VALUE)
                    continue;
                int id = item.getId();
                int rev = item.getSavedSequence();
                short vol = item.getVolumeId();
                MailboxBlob blob = sStore.getMailboxBlob(mbox, id, rev, vol);
                InputStream is = null;
                try {
                    is = sStore.getContent(blob);
                    byte[] data = ByteUtil.getContent(is, (int) item.getSize());
                    String digest = ByteUtil.getDigest(data);
                    String currentDigest = item.getDigest();
                    if (!digest.equals(currentDigest)) {
                        System.out.println("Found id " + id + ", current digest = \"" + currentDigest + "\"");
                        WikiDigest wd = new WikiDigest(mboxId, id, digest);
                        list.add(wd);
                    } else {
                        System.out.println("Found id " + id + " but skipping because digest is correct.");
                    }
                } finally {
                    ByteUtil.closeStream(is);
                }
            }
        }
        return list;
    }

    private static void fixupItems(Connection conn, int mboxId, List<WikiDigest> digests)
    throws SQLException, ServiceException {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append("mailbox" + mboxId + ".mail_item");
        sql.append(" SET blob_digest = ? WHERE id = ? AND type in (");
        sql.append(MailItem.TYPE_WIKI);
        sql.append(", ");
        sql.append(MailItem.TYPE_DOCUMENT);
        sql.append(")");
        String sqlStr = sql.toString();

        boolean success = false;
        try {
            for (WikiDigest wd : digests) {
                int id = wd.getItemId();
                String digest = wd.getDigest();
                System.out.println("Setting digest = \"" + digest + "\" for id = " + id);
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(sqlStr);
                    int pos = 1;
                    stmt.setString(pos++, digest);
                    stmt.setInt(pos++, id);
                    stmt.executeUpdate();
                } finally {
                    if (stmt != null)
                        stmt.close();
                }
            }
            success = true;
        } finally {
            if (success)
                conn.commit();
            else
                conn.rollback();
        }
    }

    public static void main(String args[]) {
        CliUtil.toolSetup("WARN");
        sStore = StoreManager.getInstance();
        Connection conn = null;
        try {
            int numFixed = 0;
            conn = DbPool.getConnection();
            System.out.println("Getting mailbox list...");
            List<Mbox> mboxList = getMboxList(conn);
            for (Mbox m : mboxList) {
                int mboxId = m.getId();
                System.out.println("Processing mailbox " + mboxId + " (" + m.getEmail() + ") ...");
                System.out.println("Getting wiki items needing fixup...");
                List<WikiDigest> digests = getWikiDigests(mboxId);
                int howmany = digests != null ? digests.size() : 0;
                System.out.println("Got " + howmany + " wiki items to fixup.");
                if (howmany > 0) {
                    fixupItems(conn, mboxId, digests);
                    numFixed += howmany;
                }
                System.out.println("Done with mailbox " + mboxId + ".");
            }
            System.out.println("Done.  Fixed " + numFixed + " wiki items.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            DbPool.quietClose(conn);
            sStore.shutdown();
        }
    }
}
