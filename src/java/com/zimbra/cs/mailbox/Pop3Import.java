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
import javax.mail.Session;
import javax.mail.Store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.pop3.POP3Message;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;


public class Pop3Import
implements MailItemImport {

    private static Session sSession;
    private static Log sLog = LogFactory.getLog(Pop3Import.class); 
    
    static {
        Properties props = new Properties();
        sSession = Session.getDefaultInstance(props);
    }

    public void test(MailItemDataSource dataSource) {
        // TODO: Implement later
    }
    
    public void importData(MailItemDataSource dataSource)
    throws ServiceException {
        try {
            fetchMessages(dataSource);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE(getContext(dataSource), e);
        } catch (IOException e) {
            throw ServiceException.FAILURE(getContext(dataSource), e);
        }
    }
    
    private void fetchMessages(MailItemDataSource ds)
    throws MessagingException, IOException, ServiceException {
        sLog.debug(String.format("Fetching messages for ds=%s, host=%s, user=%s",
            ds.getName(), ds.getHost(), ds.getUsername()));
        
        // Connect (USER, PASS, STAT)
        Store store = sSession.getStore("pop3");
        store.connect(ds.getHost(), ds.getPort(), ds.getUsername(), ds.getPassword());
        POP3Folder folder = (POP3Folder) store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
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
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(ds.getMailboxId());
            
            for (int i = 0; i < msgs.length; i++) {
                POP3Message msg = (POP3Message) msgs[i];
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Enumeration e = msg.getAllHeaderLines();
                while (e.hasMoreElements()) {
                    String line = (String) e.nextElement();
                    os.write(line.getBytes());
                    os.write('\r');
                    os.write('\n');
                }
                
                InputStream is = msg.getRawInputStream();
                byte[] data = ByteUtil.getContent(is, 2048);
                os.write(data);
                ParsedMessage pm = new ParsedMessage(os.toByteArray(), mbox.attachmentsIndexingEnabled());
                mbox.addMessage(null, pm, ds.getFolderId(), false, Flag.BITMASK_UNREAD, null);
            }

            /*
            if (!ds.leaveMailOnServer()) {
                // Mark all messages for deletion (DELE)
                for (Message msg : msgs) {
                    msg.setFlag(Flags.Flag.DELETED, true);
                }
            }
            */
        }
        
        // Expunge if necessary and disconnect (QUIT)
        // folder.close(!ds.leaveMailOnServer());
        folder.close(false);
        store.close();
    }
    
    private String getContext(MailItemDataSource ds) {
        return String.format("%s %s",
            StringUtil.getSimpleClassName(this), ds.getName());
    }
}
