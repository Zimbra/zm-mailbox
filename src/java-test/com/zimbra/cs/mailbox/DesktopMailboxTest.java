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

package com.zimbra.cs.mailbox;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.mime.ParsedMessage;

public class DesktopMailboxTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(); //TODO: allow paths to be specified so we can run tests outside of ZimbraServer
        MailboxManager.setInstance(new MailboxManager() {
            @Override
            protected Mailbox instantiateMailbox(MailboxData data) {
                //mock the behaviors we need to test in DesktopMailbox
                return new Mailbox(data) {
                    @Override
                    protected boolean needRedo(OperationContext octxt) {
                        return false;
                    }
                };
            }
            //TODO: eventually move this into ZimbraOffline and implement all of the provisioning needed for real DesktopMailbox
//                try {
//                    return new DesktopMailbox(data) {
//                    };
//                } catch (ServiceException e) {
//                    throw new RuntimeException(e);
//                }
        });

        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    public static final DeliveryOptions STANDARD_DELIVERY_OPTIONS = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);

    private void useMVCC(Mailbox mbox) throws ServiceException, SQLException {
        //tell HSQLDB to use multiversion so our asserts can read while write is open 
        PreparedStatement stmt = null;
        ResultSet rs = null;
        DbConnection conn = DbPool.getConnection(mbox);
        try {
            stmt = conn.prepareStatement("SET DATABASE TRANSACTION CONTROL MVCC");
            stmt.executeUpdate();
        } finally {
            DbPool.closeResults(rs);
            DbPool.quietCloseStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    private int countInboxMessages(Mailbox mbox) throws ServiceException, SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        DbConnection conn = DbPool.getConnection(mbox);
        try {
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM mboxgroup1.mail_item WHERE mailbox_id = ? and folder_id = ?");
            stmt.setInt(1, mbox.getId());
            stmt.setInt(2, Mailbox.ID_FOLDER_INBOX);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } finally {
            DbPool.closeResults(rs);
            DbPool.quietCloseStatement(stmt);
            DbPool.quietClose(conn);
        }
    }
    
    @Test
    public void nestedTxn() throws Exception {
        Account acct = Provisioning.getInstance().getAccount("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        useMVCC(mbox);
        
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX);
        mbox.lock.lock();
        try {
            OperationContext octx = new OperationContext(acct);
            mbox.beginTransaction("outer", octx);
            mbox.beginTransaction("inner1", octx);
            mbox.addMessage(octx, new ParsedMessage("From: test1-1@sub1.zimbra.com".getBytes(), false), dopt, null);

            //nothing committed yet
            Assert.assertEquals(0, countInboxMessages(mbox));
            mbox.endTransaction(true); //inner 1

            //nothing committed yet
            Assert.assertEquals(0, countInboxMessages(mbox));
            
            mbox.beginTransaction("inner2", null);
            mbox.addMessage(null, new ParsedMessage("From: test1-2@sub1.zimbra.com".getBytes(), false), dopt, null);

            //nothing committed yet
            Assert.assertEquals(0, countInboxMessages(mbox));
            mbox.endTransaction(true); //inner 2

            //nothing committed yet
            Assert.assertEquals(0, countInboxMessages(mbox));
            
            mbox.endTransaction(true); //outer

            //committed
            Assert.assertEquals(2, countInboxMessages(mbox));
        } finally {
            mbox.lock.release();
        }
    }
}
