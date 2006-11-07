/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.pop3.POP3Message;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;


public class Pop3Import
implements MailItemImport {

    private static final int TIMEOUT = 10 * (int) Constants.MILLIS_PER_SECOND;
    private static Session sSession;
    private static Session sSelfSignedCertSession;
    private static Log sLog = LogFactory.getLog(Pop3Import.class);
    private static final byte[] CRLF = { '\r', '\n' };
    
    static {
        Properties props = new Properties();
        props.put("mail.pop3.connectiontimeout", TIMEOUT);
        sSession = Session.getInstance(props);
        
        props.put("mail.pop3.socketFactory.class", "com.zimbra.common.util.DummySSLSocketFactory");
        sSelfSignedCertSession = Session.getInstance(props);
    }

    public String test(DataSource ds)
    throws ServiceException {
        String error = null;
        
        validateDataSource(ds);

        try {
            Store store = getStore(ds.getConnectionType());
            store.connect(ds.getHost(), ds.getPort(), ds.getUsername(), ds.getDecryptedPassword());
            store.close();
        } catch (MessagingException e) {
            sLog.info("Testing connection to data source", e);
            error = e.getMessage();
        }
        return error;
    }
    
    public void importData(Account account, DataSource dataSource)
    throws ServiceException {
        try {
            fetchMessages(account, dataSource);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Importing data from " + dataSource, e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Importing data from " + dataSource, e);
        }
    }

    private void validateDataSource(DataSource ds)
    throws ServiceException {
        if (ds.getHost() == null) {
            throw ServiceException.FAILURE(ds + ": host not set", null);
        }
        if (ds.getPort() == null) {
            throw ServiceException.FAILURE(ds + ": port not set", null);
        }
        if (ds.getConnectionType() == null) {
            throw ServiceException.FAILURE(ds + ": connectionType not set", null);
        }
        if (ds.getUsername() == null) {
            throw ServiceException.FAILURE(ds + ": username not set", null);
        }
    }
    
    private Store getStore(String connectionType)
    throws NoSuchProviderException, ServiceException {
        if (connectionType.equals(DataSource.CT_CLEARTEXT)) {
            return sSession.getStore("pop3");
        } else if (connectionType.equals(DataSource.CT_SSL)) {
            if (LC.data_source_trust_self_signed_certs.booleanValue()) {
                return sSelfSignedCertSession.getStore("pop3s");
            } else {
                return sSession.getStore("pop3s");
            }
        } else {
            throw ServiceException.FAILURE("Invalid connectionType: " + connectionType, null);
        }
    }
    
    private void fetchMessages(Account account, DataSource ds)
    throws MessagingException, IOException, ServiceException {
        ZimbraLog.mailbox.info("Importing POP3 messages from " + ds);
        
        validateDataSource(ds);
        
        // Connect (USER, PASS, STAT)
        Store store = getStore(ds.getConnectionType());
        store.connect(ds.getHost(), ds.getPort(), ds.getUsername(), ds.getDecryptedPassword());
        POP3Folder folder = (POP3Folder) store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        
        Message msgs[] = folder.getMessages();
        
        sLog.debug("Retrieving " + msgs.length + " messages");

        if (msgs.length > 0) {
            /*
            if (ds.leaveMailOnServer()) {
                // Fetch message UID's for reconciliation (UIDL)
                FetchProfile fp = new FetchProfile();
                fp.add(UIDFolder.FetchProfileItem.UID);
                folder.fetch(folder.getMessages(), fp);
            }
            */
            
            // Fetch message bodies (RETR)
            for (int i = 0; i < msgs.length; i++) {
                POP3Message msg = (POP3Message) msgs[i];
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                
                // Headers
                Enumeration e = msg.getAllHeaderLines();
                while (e.hasMoreElements()) {
                    String line = (String) e.nextElement();
                    os.write(line.getBytes());
                    os.write(CRLF);
                }
                
                // Line break between headers and content
                os.write(CRLF);
                
                // Content
                InputStream is = msg.getRawInputStream();
                ByteUtil.copy(is, true, os, true);
                ParsedMessage pm = new ParsedMessage(os.toByteArray(), mbox.attachmentsIndexingEnabled());
                mbox.addMessage(null, pm, ds.getFolderId(), false, Flag.BITMASK_UNREAD, null);
            }

            /*
            // Mark all messages for deletion (DELE)
            for (Message msg : msgs) {
                msg.setFlag(Flags.Flag.DELETED, true);
            }
            */
        }
        
        // Expunge if necessary and disconnect (QUIT)
        // folder.close(!ds.leaveMailOnServer());
        folder.close(false);
        store.close();
    }
}
