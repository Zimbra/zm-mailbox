/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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
import java.util.*;

import javax.mail.Address;
import javax.mail.Transport;
import javax.mail.SendFailedException;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapDomain;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.ExceptionToString;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.ParseMimeMessage.MimeMessageData;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.WriteOpDocumentHandler;


/**
 * @author tim
 *
 * Process the <SendMsg> request from the client and send an email message.
 */
public class SendMsg extends WriteOpDocumentHandler {
    
    private static Log mLog = LogFactory.getLog(SendMsg.class);
    private static StopWatch sWatch = StopWatch.getInstance("SendMsg");

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();

        try {
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
            if (attachment != null)
                mm = parseUploadedMessage(mbox, attachment, mimeData);
            else
                mm = ParseMimeMessage.parseMimeMsgSoap(octxt, mbox, msgElem, null, mimeData);
            int msgId = sendMimeMessage(octxt, mbox, acct, saveToSent, mimeData, mm, msgElem);

            Element response = lc.createElement(MailService.SEND_MSG_RESPONSE);
            Element respElement = response.addElement(MailService.E_MSG);
            if (saveToSent && msgId > 0)
            	respElement.addAttribute(MailService.A_ID, msgId);
            return response;
        } finally {
            sWatch.stop(startTime);
        }
    }
    
    /**
     * Check to see if all the addresses are "Valid" -- right now this means that they are either to a remote
     * domain, or they are a local domain and we can find them in LDAP. 
     * 
     * @param mm
     * @throws ServiceException if couldn't read addresses from mm
     * @throws MailServiceException.UNKNOWN_LOCAL_RECIPIENTS if one of the addresses was invalid
     */
    protected static void validateAddresses(MimeMessage mm) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        
        List myDomains = prov.getAllDomains();
        
        ArrayList unknownRecipients = new ArrayList();
        
        try {
            Address[] addrs = mm.getAllRecipients();
            
            AllAddresses: for (int i = addrs.length-1; i >=0; i--) {
                InternetAddress ia = (InternetAddress) addrs[i];
                
                String addr = ia.getAddress();
                
                if (prov.getAccountByName(addr) == null) {
                    
                    // couldn't find it -- is it a local domain?
                    String parts[] = addr.split("@");
                    if (parts.length > 1) {
                        IsLocalDomain: for (Iterator domainIter = myDomains.iterator(); domainIter.hasNext();) {
                            Domain domain = (Domain)domainIter.next(); 
                            String domainStr = domain.getName(); 
                            if (parts[1].equals(domainStr)) {
                                // it IS a local address.  Why couldn't we find it?
                                
                                // is it a distribution list?
                                if (prov.getDistributionListByName(addr) == null) {
                                    unknownRecipients.add(addr);
                                } 
                                
                                break IsLocalDomain; // go onto the next address 
                            }
                        }
                    }
                }
            }
            
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Caught MessagingException trying to getAllRecipients "+e, e);
        }
        
        if (unknownRecipients.size() > 0) {
            String unknown[] = new String[unknownRecipients.size()];
            unknownRecipients.toArray(unknown);
            throw MailServiceException.UNKNOWN_LOCAL_RECIPIENTS(unknown);
        }
    }

    protected static boolean shouldSaveToSent(Account acct) {
        return acct.getBooleanAttr(Provisioning.A_zimbraPrefSaveToSent, false);        
    }
    
    protected static int getSentFolder(Account acct, Mailbox mbox) throws ServiceException{
        int folderId = Mailbox.ID_FOLDER_SENT;

        String sentFolder = acct.getAttr(Provisioning.A_zimbraPrefSentMailFolder, null);
        if (sentFolder != null)
            try {
                folderId = mbox.getFolderByPath(sentFolder).getId();
            } catch (NoSuchItemException nsie) { }
        return folderId;
    }

    static MimeMessage parseUploadedMessage(Mailbox mbox, String attachId, MimeMessageData mimeData) throws ServiceException {
        mimeData.attachId = attachId;

        List uploads = FileUploadServlet.fetchUploads(mbox.getAccountId(), attachId);
        if (uploads == null || uploads.size() == 0)
            throw MailServiceException.NO_SUCH_UPLOAD(attachId);
        else if (uploads.size() > 1)
            throw MailServiceException.TOO_MANY_UPLOADS(attachId);

        FileItem fi = (FileItem) uploads.get(0);
        try {
            return new MimeMessage(JMSession.getSession(), fi.getInputStream());
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
    
    protected static Message saveToSent(OperationContext octxt, Mailbox mbox, Account acct, MimeMessage mm, int convId) 
    throws ServiceException {
        try {
            int folderId = getSentFolder(acct, mbox);

            int flags = Flag.FLAG_FROM_ME;
            ParsedMessage pm = new ParsedMessage(mm, mm.getSentDate().getTime(),
                                                 mbox.attachmentsIndexingEnabled());
            try {
                pm.analyze();
            } catch (ServiceException e) {
                if (ConversionException.isTemporaryCauseOf(e))
                    throw e;
            }
            return mbox.addMessage(octxt, pm, folderId, false, flags, null, convId);
        } catch (IOException ioe) {
            String excepStr = ExceptionToString.ToString(ioe);
            mLog.warn(excepStr);
            throw ServiceException.FAILURE("IOException", ioe);
        } catch (MessagingException me) {
            String excepStr = ExceptionToString.ToString(me);
            mLog.warn(excepStr);
            throw ServiceException.FAILURE("MessagingException", me);
        }
    }

    static int sendMimeMessage(OperationContext octxt, Mailbox mbox, Account acct, boolean saveToSent,
                               MimeMessageData mimeData, MimeMessage mm, int origId, String replyType) 
    throws ServiceException {
        int folderId[] = null; 
        if (saveToSent)
            folderId = new int[] { getSentFolder(acct, mbox) };

        return sendMimeMessage(octxt, mbox, acct, folderId, mimeData, mm, origId, replyType);
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
     * @return
     * @throws ServiceException
     */
    static int sendMimeMessage(OperationContext octxt, Mailbox mbox, Account acct, int saveToFolders[],
                               MimeMessageData mimeData, MimeMessage mm, int origId, String replyType)
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
            Message[] msg = null;
            if (saveToFolders != null) {
                msg = new Message[saveToFolders.length];
                int flags = Flag.FLAG_FROM_ME;
                ParsedMessage pm = new ParsedMessage(mm, mm.getSentDate().getTime(),
                        mbox.attachmentsIndexingEnabled());
                
                pm.analyze();
                
                // save it to the 1st requested folder
                msg[0] = mbox.addMessage(octxt, pm, saveToFolders[0], false, flags, null, convId);

                // copy it to other requested folders
                for (int i = 1; i < saveToFolders.length; i++) 
                     msg[i] = (Message) mbox.copy(octxt, msg[0].getId(), msg[0].getType(), saveToFolders[i]);
            }
            
            // send the message via SMTP
            try {
                if (mm.getAllRecipients() != null)
                    Transport.send(mm);
            } catch (MessagingException e) {
                for (int i = msg.length-1; i >= 0; i--)
                    rollbackMessage(octxt, mbox, msg[i]);
                throw e;
            } catch (RuntimeException e) {
                for (int i = msg.length-1; i >= 0; i--)
                    rollbackMessage(octxt, mbox, msg[i]);
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
                    mLog.warn("ignoring error while setting REPLIED/FORWARDED tag on message: " + origId, e);
                }
            }


            if (mimeData != null) {
                // we can now purge the uploaded attachments
                if (mimeData.attachId != null)
                    FileUploadServlet.deleteUploads(mbox.getAccountId(), mimeData.attachId);
                
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

	        return (msg != null ? msg[0].getId() : 0);

	    } catch (SendFailedException failure) {
	        String excepStr = ExceptionToString.ToString(failure);
	        mLog.warn(excepStr);
	        throw ServiceException.INVALID_REQUEST("SendFailure", failure);
	    } catch (IOException ioe) {
	        String excepStr = ExceptionToString.ToString(ioe);
	        mLog.warn(excepStr);
	        throw ServiceException.FAILURE("IOException", ioe);
	    } catch (MessagingException me) {
	        String excepStr = ExceptionToString.ToString(me);
	        mLog.warn(excepStr);
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
