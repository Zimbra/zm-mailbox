package com.zimbra.qa.unittest;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import junit.framework.TestCase;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.index.LiquidHit;
import com.liquidsys.coco.index.LiquidQueryResults;
import com.liquidsys.coco.index.MailboxIndex;
import com.liquidsys.coco.mailbox.MailItem;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Message;
import com.liquidsys.coco.mime.Mime;
import com.liquidsys.coco.mime.MimeVisitor;
import com.liquidsys.coco.mime.TnefExtractor;
import com.liquidsys.coco.util.LiquidLog;


/**
 * @author bburtin
 */
public class TestConversion extends TestCase {

    public void testTnef()
    throws Exception {
        LiquidLog.test.debug("testTnef()");
        
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
        LiquidLog.test.debug("testTnefExtractor()");
        
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
        LiquidQueryResults results = mbox.search(query,
            new byte[] { MailItem.TYPE_MESSAGE}, MailboxIndex.SEARCH_ORDER_SUBJ_ASC);
        assertTrue("No results found for '" + query + "'", results.hasNext());
        
        // Make sure that attachments have been extracted out of winmail.dat
        LiquidHit hit = results.getNext();
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
