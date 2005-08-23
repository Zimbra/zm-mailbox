/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
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
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.mime.TnefExtractor;
import com.zimbra.cs.util.ZimbraLog;


/**
 * @author bburtin
 */
public class TestConversion extends TestCase {

    public void testTnef()
    throws Exception {
        ZimbraLog.test.debug("testTnef()");
        
        AttachmentFinder finder = new AttachmentFinder("upload.gif");
        Message msg = getTnefMessage();
        Mime.accept(finder, msg.getMimeMessage());
        assertTrue("Could not find upload.gif", finder.found());
        
        finder = new AttachmentFinder("upload2.gif");
        Mime.accept(finder, msg.getMimeMessage());
        assertTrue("Could not find upload2.gif", finder.found());
    }
    
    public void testTnefExtractor()
    throws Exception {
        ZimbraLog.test.debug("testTnefExtractor()");
        
        TnefExtractor extractor = new TnefExtractor();
        Message msg = getTnefMessage();
        Mime.accept(extractor, msg.getMimeMessage());
        assertEquals("Could not find TNEF attachment", 1, extractor.getTnefsAsMime().length);
    }
    
    private Message getTnefMessage()
    throws Exception {
        // Search for the sample message that has a TNEF attachment
        Account account = TestUtil.getAccount("user1");
        Mailbox mbox = Mailbox.getMailboxByAccount(account);
        String query = "subject:Rich text (TNEF) test";
        ZimbraQueryResults results = mbox.search(query,
            new byte[] { MailItem.TYPE_MESSAGE}, MailboxIndex.SEARCH_ORDER_SUBJ_ASC);
        assertTrue("No results found for '" + query + "'", results.hasNext());
        
        // Make sure that attachments have been extracted out of winmail.dat
        ZimbraHit hit = results.getNext();
        return mbox.getMessageById(hit.getItemId());
    }
    
    private class AttachmentFinder implements MimeVisitor {
        private String mFilename;
        private boolean mFound = false;
        
        public AttachmentFinder(String filename) {
            mFilename = filename;
        }
        
        public boolean found() {
            return mFound;
        }
        
        public void visitBodyPart(MimeBodyPart bp)
        throws MessagingException {
            if (bp.getFileName() != null && bp.getFileName().equals(mFilename)) {
                mFound = true;
            }
        }
        
        public void visitMessage(MimeMessage msg, int visitKind) {
        }
        
        public void visitMultipart(MimeMultipart mp, int visitKind) {
        }
    }
}
