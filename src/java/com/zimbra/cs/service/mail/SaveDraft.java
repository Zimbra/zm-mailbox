/*
 * Created on Jun 11, 2005
 */
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author dkarp
 */
public class SaveDraft extends WriteOpDocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Account acct = getRequestedAccount(lc);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        Element msgElem = request.getElement(MailService.E_MSG);

        int id = (int) msgElem.getAttributeLong(MailService.A_ID, Mailbox.ID_AUTO_INCREMENT);
        int origId = (int) msgElem.getAttributeLong(MailService.A_ORIG_ID, 0);
        String replyType = msgElem.getAttribute(MailService.A_REPLY_TYPE, null);

        // check to see whether the entire message has been uploaded under separate cover
        String attachment = msgElem.getAttribute(MailService.A_ATTACHMENT_ID, null);

        ParseMimeMessage.MimeMessageData mimeData = new ParseMimeMessage.MimeMessageData();
        MimeMessage mm;
        if (attachment != null)
            mm = SendMsg.parseUploadedMessage(mbox, attachment, mimeData);
        else
            mm = ParseMimeMessage.parseMimeMsgSoap(octxt, mbox, msgElem, null, mimeData);

        long date = System.currentTimeMillis();
        try {
            mm.setFrom(AccountUtil.getOutgoingFromAddress(acct));
            Date d = new Date();
            mm.setSentDate(d);
            date = d.getTime();
        } catch (Exception e) { }

        try {
            mm.saveChanges();
        } catch (MessagingException me) {
            throw ServiceException.FAILURE("completing MIME message object", me);
        }

        ParsedMessage pm = new ParsedMessage(mm, date, mbox.attachmentsIndexingEnabled());
        try {
            pm.analyze();
        } catch (ServiceException e) {
            if (ConversionException.isTemporaryCauseOf(e))
                throw e;
        }

		try {
			Message msg = mbox.saveDraft(octxt, pm, id, origId, replyType);

            // we can now purge the uploaded attachments
            if (mimeData.attachId != null)
                FileUploadServlet.deleteUploads(mbox.getAccountId(), mimeData.attachId);

            Element response = lc.createElement(MailService.SAVE_DRAFT_RESPONSE);
            // FIXME: inefficient -- this recalculates the MimeMessage (but SaveDraft is called rarely)
            ToXML.encodeMessageAsMP(response, msg, false, null);
            return response;
		} catch (IOException ioe) {
			throw ServiceException.FAILURE("IOException while saving draft", ioe);
        }
    }
}
