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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
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
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.mail.ParseMimeMessage.MimeMessageData;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.Element;
import com.zimbra.soap.WriteOpDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;


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
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(lc);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();
        
        mLog.info("<SendMsg> " + lc.toString());
        
        // <M>
        Element msgElem = request.getElement(MailService.E_MSG);
        
        boolean saveToSent = acct.saveToSent();
        
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
        
        int origId = (int) msgElem.getAttributeLong(MailService.A_ORIG_ID, 0);
        String replyType = msgElem.getAttribute(MailService.A_REPLY_TYPE, MailSender.MSGTYPE_REPLY);

        int msgId = MailSender.sendMimeMessage(
                octxt, mbox, saveToSent, mm,
                mimeData.newContacts, mimeData.uploads,
                origId, replyType, false);
        
        Element response = lc.createElement(MailService.SEND_MSG_RESPONSE);
        Element respElement = response.addElement(MailService.E_MSG);
        if (saveToSent && msgId > 0)
            respElement.addAttribute(MailService.A_ID, msgId);
        return response;
    }

    static MimeMessage parseUploadedMessage(ZimbraSoapContext lc, String attachId, MimeMessageData mimeData) throws ServiceException {
        Upload up = FileUploadServlet.fetchUpload(lc.getAuthtokenAccountId(), attachId, lc.getRawAuthToken());
        if (up == null)
            throw MailServiceException.NO_SUCH_UPLOAD(attachId);
        (mimeData.uploads = new ArrayList<Upload>()).add(up);
        try {
            return new Mime.FixedMimeMessage(JMSession.getSession(), up.getInputStream());
        } catch (MessagingException e) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException when parsing upload", e);
        }
    }
}
