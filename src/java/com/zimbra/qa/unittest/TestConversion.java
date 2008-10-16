/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.common.util.ZimbraLog;


/**
 * @author bburtin
 */
public class TestConversion extends TestCase {

    public void testTnef()
    throws Exception {
        ZimbraLog.test.debug("testTnef()");
        
        AttachmentFinder finder = new AttachmentFinder("upload.gif");
        Message msg = getTnefMessage();
        finder.accept(msg.getMimeMessage());
        assertTrue("Could not find upload.gif", finder.found());
        
        finder = new AttachmentFinder("upload2.gif");
        finder.accept(msg.getMimeMessage());
        assertTrue("Could not find upload2.gif", finder.found());
    }

    private Message getTnefMessage()
    throws Exception {
        // Search for the sample message that has a TNEF attachment
        Account account = TestUtil.getAccount("user1");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        String query = "subject:Rich text (TNEF) test";
        ZimbraQueryResults results = mbox.search(new Mailbox.OperationContext(mbox), query,
            new byte[] { MailItem.TYPE_MESSAGE}, MailboxIndex.SortBy.SUBJ_ASCENDING, 100);
        assertTrue("No results found for '" + query + "'", results.hasNext());
        
        // Make sure that attachments have been extracted out of winmail.dat
        ZimbraHit hit = results.getNext();
        results.doneWithSearchResults();
        return mbox.getMessageById(null, hit.getItemId());
    }
    
    private class AttachmentFinder extends MimeVisitor {
        private String mFilename;
        private boolean mFound = false;
        
        public AttachmentFinder(String filename) {
            mFilename = filename;
        }
        
        public boolean found() {
            return mFound;
        }
        
        protected boolean visitBodyPart(MimeBodyPart bp) throws MessagingException {
            String filename = Mime.getFilename(bp);
            if (filename != null && filename.equals(mFilename))
                mFound = true;
            return false;
        }
        
        protected boolean visitMessage(MimeMessage msg, VisitPhase visitKind) {
            return false;
        }
        
        protected boolean visitMultipart(MimeMultipart mp, VisitPhase visitKind) {
            return false;
        }
    }
}
