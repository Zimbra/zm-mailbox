/*
 * Created on Sep 29, 2004
 */
package com.liquidsys.coco.service.mail;


import java.io.IOException;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.mailbox.Flag;
import com.liquidsys.coco.mailbox.Folder;
import com.liquidsys.coco.mailbox.MailItem;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Message;
import com.liquidsys.coco.mailbox.MailServiceException.NoSuchItemException;
import com.liquidsys.coco.mailbox.Mailbox.OperationContext;
import com.liquidsys.coco.mime.ParsedMessage;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.FileUploadServlet;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.stats.StopWatch;
import com.zimbra.soap.LiquidContext;
import com.zimbra.soap.WriteOpDocumentHandler;


/**
 * @author tim
 *
 */
public class AddMsg extends WriteOpDocumentHandler {
    private static Log mLog = LogFactory.getLog(AddMsg.class);
    private static StopWatch sWatch = StopWatch.getInstance("AddMsg");

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element request, Map context) throws ServiceException
    {
        long startTime = sWatch.start();
        
        try {
            LiquidContext lc = getLiquidContext(context);
            Mailbox mbox = getRequestedMailbox(lc);
            OperationContext octxt = lc.getOperationContext();

            Element msgElem = request.getElement(MailService.E_MSG);

            String flagsStr = msgElem.getAttribute(MailService.A_FLAGS, null);
	        String tagsStr = msgElem.getAttribute(MailService.A_TAGS, null);
	        String folderStr = msgElem.getAttribute(MailService.A_FOLDER, null);
            long date = msgElem.getAttributeLong(MailService.A_DATE, System.currentTimeMillis());

	        if (mLog.isDebugEnabled()) {
	            StringBuffer toPrint = new StringBuffer("<AddMsg ");
	            if (tagsStr != null)
	                toPrint.append(" tags=\"").append(tagsStr).append("\"");
	            if (folderStr != null)
	                toPrint.append(" folder=\"").append(folderStr).append("\"");
	            toPrint.append(">");
	            mLog.debug(toPrint);
	        }

	        // sanity-check the supplied tag list
	        if (tagsStr != null) {
	            String[] splitTags = tagsStr.split("\\s*,\\s*");
	            if (splitTags.length > 0)
	                for (int i = 0; i < splitTags.length; i++) {
	                    try {
	                        int tagId = Integer.parseInt(splitTags[i]);
	                        if (mbox.getTagById(tagId) == null)
	                        	throw ServiceException.INVALID_REQUEST("Unknown tag: \"" + tagId + "\"", null);
	                    } catch (NumberFormatException e) {};
	                }
	        }

	        Folder folder = null;
	        if (folderStr != null) {
                try {
                    int folderId = Integer.parseInt(folderStr);
                    folder = mbox.getFolderById(folderId);
                } catch (NoSuchItemException nsie) {
                } catch (NumberFormatException e) {}

                if (folder == null)
                    folder = mbox.getFolderByPath(folderStr);

                if (mLog.isDebugEnabled())
                    mLog.debug("folder = " + folder.toString());
	        }

            // check to see whether the entire message has been uploaded under separate cover
            String attachment = msgElem.getAttribute(MailService.A_ATTACHMENT_ID, null);

            ParseMimeMessage.MimeMessageData mimeData = new ParseMimeMessage.MimeMessageData();
            MimeMessage mm;
            if (attachment != null)
                mm = SendMsg.parseUploadedMessage(mbox, attachment, mimeData);
            else
                mm = ParseMimeMessage.importMsgSoap(msgElem);

	        int messageId = -1;
	        try {
                ParsedMessage pm = new ParsedMessage(mm, date, mbox.attachmentsIndexingEnabled());
	            Message msg = mbox.addMessage(octxt, pm, folder.getId(), Flag.flagsToBitmask(flagsStr), tagsStr);
	            if (msg != null)
                    messageId = msg.getId();
	        } catch(IOException ioe) {
	            throw ServiceException.FAILURE("Error While Delivering Message", ioe);
	        }

            // we can now purge the uploaded attachments
            if (mimeData.attachId != null)
                FileUploadServlet.deleteUploads(mbox.getAccountId(), mimeData.attachId);

            Element response = lc.createElement(MailService.ADD_MSG_RESPONSE);
            if (messageId != -1)
	    	    response.addElement(MailService.E_MSG).addAttribute(MailService.A_ID, messageId);

            return response;
        } finally {
            sWatch.stop(startTime);
        }
    }
}
