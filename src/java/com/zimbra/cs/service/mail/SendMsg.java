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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 17, 2004
 */
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.mail.ParseMimeMessage.MimeMessageData;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.Element;
import com.zimbra.soap.WriteOpDocumentHandler;
import com.zimbra.soap.ZimbraContext;


/**
 * @author tim
 *
 * Process the <SendMsg> request from the client and send an email message.
 */
public class SendMsg extends WriteOpDocumentHandler {
    
    private static Log mLog = LogFactory.getLog(SendMsg.class);

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Account acct = getRequestedAccount(lc);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();
        
        mLog.info("<SendMsg> " + lc.toString());
        
        // <M>
        Element msgElem = request.getElement(MailService.E_MSG);
        
        boolean saveToSent = shouldSaveToSent(acct);
        
        // check to see whether the entire message has been uploaded under separate cover
        String attachment = msgElem.getAttribute(MailService.A_ATTACHMENT_ID, null);
        
        // holds return data about the MimeMessage
        MimeMessageData mimeData = new MimeMessageData();
        MimeMessage mm;
        if (attachment != null) {
            mm = parseUploadedMessage(lc, attachment, mimeData);
        } else {
            mm = ParseMimeMessage.parseMimeMsgSoap(lc, mbox, msgElem, null, mimeData);
        }
        
        int msgId = sendMimeMessage(octxt, mbox, acct, saveToSent, mimeData, mm, msgElem);
        
        Element response = lc.createElement(MailService.SEND_MSG_RESPONSE);
        Element respElement = response.addElement(MailService.E_MSG);
        if (saveToSent && msgId > 0)
            respElement.addAttribute(MailService.A_ID, msgId);
        return response;
    }

    protected static boolean shouldSaveToSent(Account acct) {
        return acct.getBooleanAttr(Provisioning.A_zimbraPrefSaveToSent, false);        
    }
    
    protected static int getSentFolder(Account acct, Mailbox mbox, OperationContext octxt) throws ServiceException{
        int folderId = Mailbox.ID_FOLDER_SENT;

        String sentFolder = acct.getAttr(Provisioning.A_zimbraPrefSentMailFolder, null);
        if (sentFolder != null)
            try {
                folderId = mbox.getFolderByPath(octxt, sentFolder).getId();
            } catch (NoSuchItemException nsie) { }
        return folderId;
    }

    static MimeMessage parseUploadedMessage(ZimbraContext lc, String attachId, MimeMessageData mimeData) throws ServiceException {
        Upload up = FileUploadServlet.fetchUpload(lc.getAuthtokenAccountId(), attachId, lc.getRawAuthToken());
        if (up == null)
            throw MailServiceException.NO_SUCH_UPLOAD(attachId);
        (mimeData.uploads = new ArrayList()).add(up);
        try {
            return new MimeMessage(JMSession.getSession(), up.getInputStream());
        } catch (MessagingException e) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException when parsing upload", e);
        }
    }

    protected static final String TYPE_REPLY   = Flag.getAbbreviation(Flag.ID_FLAG_REPLIED) + "";
    protected static final String TYPE_FORWARD = Flag.getAbbreviation(Flag.ID_FLAG_FORWARDED) + "";
    
    protected static int sendMimeMessage(OperationContext octxt, Mailbox mbox, Account acct, boolean saveToSent,
                                         MimeMessageData mimeData, MimeMessage mm, Element msgElem) 
    throws ServiceException {
        int origId = (int) msgElem.getAttributeLong(MailService.A_ORIG_ID, 0);
        String replyType = msgElem.getAttribute(MailService.A_REPLY_TYPE, TYPE_REPLY);

        return sendMimeMessage(octxt, mbox, acct, saveToSent, mimeData, mm, origId, replyType);
    }

    static int sendMimeMessage(OperationContext octxt, Mailbox mbox, Account acct, boolean saveToSent,
                               MimeMessageData mimeData, MimeMessage mm, int origId, String replyType) 
    throws ServiceException {
        int folderId = 0;
        if (saveToSent) {
            folderId = getSentFolder(acct, mbox, octxt);
        }

        return sendMimeMessage(octxt, mbox, acct, folderId, mimeData, mm, origId, replyType, false);
    }
    
    private static Address[] removeInvalidAddresses(Address[] orig, Address[] invalidAddrs)
    {
        if (orig == null || invalidAddrs == null) 
            return orig;
            
        ArrayList newTo = new ArrayList();
        for (int i = 0; i < orig.length; i++) {
            boolean invalid = false;
            for (int j = 0; j < invalidAddrs.length; j++) {
                if (invalidAddrs[j].equals(orig[i])) {
                    invalid = true;
                    break;
                }
            }
            if (!invalid)
                newTo.add(orig[i]);
        }
        Address[] toRet = new Address[newTo.size()];
        toRet = (Address[])(newTo.toArray(toRet));
        return toRet;
    }
    
    
    /**
     * Returns the msg-id of the copy in the first saved folder, or 0 if none
     * 
     * @param mbox
     * @param acct
     * @param saveToFolders[] list of folderIds to save a copy of this msg into.  This is a list primarily 
     * so that we can do special things with INVITE messages (save copy to calendar folder).  Messages are
     * guaranteed to be inserted in the specified-order (this matters for calendaring!)
     * @param mimeData
     * @param mm
     * @param origId
     * @param replyType
     * @param ignoreFailedAddresses If TRUE, then will attempt to send even if some addresses fail (no error is returned!)
     * @return
     * @throws ServiceException
     */
    public static int sendMimeMessage(OperationContext octxt, Mailbox mbox, Account acct, int saveToFolder,
                               MimeMessageData mimeData, MimeMessage mm, int origId, String replyType,
                               boolean ignoreFailedAddresses)
    throws ServiceException {
        try {
            // slot the message in the parent's conversation if the subjects match
            int convId = Mailbox.ID_AUTO_INCREMENT;
            if (origId > 0)
                convId = mbox.getConversationIdFromReferent(mm, origId);

            String replyTo = acct.getAttr(Provisioning.A_zimbraPrefReplyToAddress);
        	mm.setFrom(AccountUtil.getOutgoingFromAddress(acct));
			mm.setSentDate(new Date());
            if (replyTo != null && !replyTo.trim().equals(""))
                mm.setHeader("Reply-To", replyTo);
            mm.saveChanges();

            // save a copy of the message to the Sent Mail folder
            Message  msg = null;
            if (saveToFolder != 0) {
                int flags = Flag.FLAG_FROM_ME;
                ParsedMessage pm = new ParsedMessage(mm, mm.getSentDate().getTime(),
                        mbox.attachmentsIndexingEnabled());
                
                pm.analyze();
                
                // save it to the requested folder
                msg = mbox.addMessage(octxt, pm, saveToFolder, true, flags, null, convId);
            }
            
            // send the message via SMTP
            try {
                boolean retry = ignoreFailedAddresses;
                if (mm.getAllRecipients() != null) {
                    do {
                        try {
                            Transport.send(mm);
                            retry = false;
                        } catch (SendFailedException sfe) {
                            Address[] invalidAddrs = sfe.getInvalidAddresses();
                            if (!retry)
                                throw sfe;
                            
                            Address[] to = removeInvalidAddresses(mm.getRecipients(RecipientType.TO), invalidAddrs);
                            Address[] cc = removeInvalidAddresses(mm.getRecipients(RecipientType.CC), invalidAddrs);
                            Address[] bcc = removeInvalidAddresses(mm.getRecipients(RecipientType.BCC), invalidAddrs);
                            
                            // if there are NO valid addrs, then give up!
                            if ((to == null || to.length == 0) && (cc == null || cc.length == 0) && (bcc == null || bcc.length == 0))
                                retry = false;
                            
                            mm.setRecipients(RecipientType.TO, to);
                            mm.setRecipients(RecipientType.CC, cc);
                            mm.setRecipients(RecipientType.BCC, bcc);
                        }
                    } while(retry);
                }
            } catch (MessagingException e) {
                rollbackMessage(octxt, mbox, msg);
                throw e;
            } catch (RuntimeException e) {
                rollbackMessage(octxt, mbox, msg);
                throw e;
            } 

            // check if this is a reply, and if so flag the message appropriately
            if (origId > 0) {
                try {
                    if (TYPE_REPLY.equals(replyType))
                        mbox.alterTag(octxt, origId, MailItem.TYPE_MESSAGE, Flag.ID_FLAG_REPLIED, true);
                    else if (TYPE_FORWARD.equals(replyType))
                        mbox.alterTag(octxt, origId, MailItem.TYPE_MESSAGE, Flag.ID_FLAG_FORWARDED, true);
                } catch (ServiceException e) {
                    // this is not an error case: when accepting/declining an appointment, the original
                    // message may be gone
                }
            }


            if (mimeData != null) {
                // we can now purge the uploaded attachments
                if (mimeData.uploads != null)
                    FileUploadServlet.deleteUploads(mimeData.uploads);
                
                // add any new contacts to the personal address book
                for (Iterator it = mimeData.newContacts.iterator(); it.hasNext(); ) {
                    ParsedAddress addr = new ParsedAddress((InternetAddress) it.next());
                    try {
                        mbox.createContact(octxt, addr.getAttributes(), Mailbox.ID_FOLDER_CONTACTS, null);
                    } catch (ServiceException e) {
                        mLog.warn("ignoring error while auto-adding contact", e);
                    }
                }
            }

	        return (msg != null ? msg.getId() : 0);

	    } catch (SendFailedException sfe) {
            mLog.warn("exception ocurred during SendMsg", sfe);
            Address[] invalidAddrs = sfe.getInvalidAddresses();
            if (invalidAddrs != null && invalidAddrs.length > 0) { 
                StringBuffer msg = new StringBuffer("Invalid address").append(invalidAddrs.length > 1 ? "es: " : ": ");
                if (invalidAddrs != null && invalidAddrs.length > 0) {
                    for (int i = 0; i < invalidAddrs.length; i++) {
                        if (i > 0) {
                            msg.append(",");
                        }
                        msg.append(invalidAddrs[i]);
                    }
                }
                if (JMSession.getSmtpConfig().getSendPartial()) {
                    throw MailServiceException.SEND_PARTIAL_ADDRESS_FAILURE(msg.toString(), sfe);
                } else {
                    throw MailServiceException.SEND_ABORTED_ADDRESS_FAILURE(msg.toString(), sfe);
                }
            } else {
                throw MailServiceException.SEND_FAILURE("SMTP server reported: " + sfe.getMessage().trim(), sfe);
            }
	    } catch (IOException ioe) {
	        mLog.warn("exception occured during send msg", ioe);
	        throw ServiceException.FAILURE("IOException", ioe);
	    } catch (MessagingException me) {
	        mLog.warn("exception occurred during SendMsg", me);
	        throw ServiceException.FAILURE("MessagingException", me);
	    }
	}
    
    private static void rollbackMessage(OperationContext octxt, Mailbox mbox, Message msg) {
        // clean up save-to-sent if needed
        if (msg == null)
            return;
        try {
            mbox.delete(octxt, msg.getId(), msg.getType());
        } catch (Exception e) {
            mLog.warn("ignoring error while deleting saved sent message: " + msg.getId(), e);
        }
    }
}
