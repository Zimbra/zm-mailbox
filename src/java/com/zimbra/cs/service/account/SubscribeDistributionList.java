/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.DistributionListSubscriptionPolicy;
import com.zimbra.common.account.ZAttrProvisioning.DistributionListUnsubscriptionPolicy;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.mime.shim.JavaMailMimeBodyPart;
import com.zimbra.common.mime.shim.JavaMailMimeMultipart;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.type.DistributionListSubscribeOp;
import com.zimbra.soap.account.type.DistributionListSubscribeStatus;

public class SubscribeDistributionList extends DistributionListDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Group group = getGroup(request, prov);
        
        DistributionListSubscribeOp op = DistributionListSubscribeOp.fromString(request.getAttribute(AccountConstants.A_OP));
        
        Account acct = getRequestedAccount(zsc);
        String[] members = new String[]{acct.getName()};
        
        DistributionListSubscribeStatus status = null;
        boolean accepted = false;
        if (op == DistributionListSubscribeOp.subscribe) {
            DistributionListSubscriptionPolicy policy = group.getSubscriptionPolicy();
            
            if (policy == DistributionListSubscriptionPolicy.ACCEPT) {
                prov.addGroupMembers(group, members);
                accepted = true;
                status = DistributionListSubscribeStatus.subscribed;
            } else if (policy == DistributionListSubscriptionPolicy.REJECT) {
                throw ServiceException.PERM_DENIED("subscription policy for group " + group.getName() + " is reject");
            } else { // REQUEST APPROAVAL
                ApprovalSender sender = new ApprovalSender(prov, group, acct, op);
                sender.composeAndSend();
                status = DistributionListSubscribeStatus.awaiting_approval;
            }
            
        } else {
            DistributionListUnsubscriptionPolicy policy = group.getUnsubscriptionPolicy();
            
            if (policy == DistributionListUnsubscriptionPolicy.ACCEPT) {
                prov.removeGroupMembers(group, members);
                accepted = true;
                status = DistributionListSubscribeStatus.unsubscribed;
            } else if (policy == DistributionListUnsubscriptionPolicy.REJECT) {
                throw ServiceException.PERM_DENIED("un-subscription policy for group " + group.getName() + " is reject");
            } else { // REQUEST APPROAVAL
                ApprovalSender sender = new ApprovalSender(prov, group, acct, op);
                sender.composeAndSend();
                status = DistributionListSubscribeStatus.awaiting_approval;
            }
            
        }
        
        if (accepted) {
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                        new String[] {"cmd", "SubscribeDistributionList","name", group.getName(), 
                        "op", op.name(),        
                        "member", Arrays.deepToString(members)})); 
        }
        
        Element response = zsc.createElement(AccountConstants.SUBSCRIBE_DISTRIBUTION_LIST_RESPONSE);
        response.addAttribute(AccountConstants.A_STATUS, status.name());
        
        return response;
    }
    
    private static class ApprovalSender {
        private Provisioning prov;
        private Group group;
        private Account requestingAcct;
        private DistributionListSubscribeOp op;
        
        private ApprovalSender(Provisioning prov, Group group, Account requestingAcct, 
                DistributionListSubscribeOp op) {
            this.prov = prov;
            this.group = group;
            this.requestingAcct = requestingAcct;
            this.op = op;
        }
        
        private void composeAndSend() throws ServiceException {
            // list of owner emails
            List<String> owners = new ArrayList<String>();
            
            Group.GroupOwner.getOwnerEmails(group, owners);
            
            for (String owner : owners) {
                sendMessage(owner);
            }
        }
        
        private void sendMessage(String ownerEmail) throws ServiceException {
            try {
                SMTPMessage out = new SMTPMessage(JMSession.getSmtpSession());
                
                Address fromAddr = AccountUtil.getFriendlyEmailAddress(requestingAcct);
                
                Address replyToAddr = fromAddr;
                String replyTo = requestingAcct.getAttr(Provisioning.A_zimbraPrefReplyToAddress);
                if (replyTo != null) {
                    replyToAddr = new JavaMailInternetAddress(replyTo);
                }
                
                // From
                out.setFrom(fromAddr);
                
                // Reply-To
                out.setReplyTo(new Address[]{replyToAddr});
                
                // To
                out.setRecipient(javax.mail.Message.RecipientType.TO, new JavaMailInternetAddress(ownerEmail));
                
                // Date
                out.setSentDate(new Date());
                
                // Subject
                Locale locale = getLocale(ownerEmail);
                String subject = L10nUtil.getMessage(MsgKey.groupSubscriptionAppravalSubject, locale);
                out.setSubject(subject);
                
                buildContentAndSend(out, locale);
            
            } catch (MessagingException e) {
                ZimbraLog.account.warn("send share info notification failed rcpt='" + ownerEmail +"'", e);
            }
        }
        
        private Locale getLocale(String ownerEmail) throws ServiceException {
            Locale locale;
            Account rcptAcct = prov.get(AccountBy.name, ownerEmail);
            if (rcptAcct != null)
                locale = rcptAcct.getLocale();
            else if (requestingAcct != null)
                locale = requestingAcct.getLocale();
            else
                locale = prov.getConfig().getLocale();

            return locale;
        }
        
        private void buildContentAndSend(SMTPMessage out, Locale locale)
        throws MessagingException {
        
            MimeMultipart mmp = buildMailContent(locale);
            out.setContent(mmp);
            Transport.send(out);
        
            // log
            Address[] rcpts = out.getRecipients(javax.mail.Message.RecipientType.TO);
            StringBuilder rcptAddr = new StringBuilder();
            for (Address a : rcpts) {
                rcptAddr.append(a.toString());
            }
            ZimbraLog.account.info("group subscription request sent: rcpt='" + rcptAddr + 
                    "' Message-ID=" + out.getMessageID());
        }
        
        private MimeMultipart buildMailContent(Locale locale)
        throws MessagingException {
            String text = textPart(locale);
            String html = htmlPart(locale);
            String xml = xmlPart(locale);
            
            // Body
            MimeMultipart mmp = new JavaMailMimeMultipart("alternative");
        
            // TEXT part (add me first!)
            MimeBodyPart textPart = new JavaMailMimeBodyPart();
            textPart.setText(text, MimeConstants.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);

            // HTML part
            MimeBodyPart htmlPart = new JavaMailMimeBodyPart();
            htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(html)));
            mmp.addBodyPart(htmlPart);

            // XML part
            MimeBodyPart xmlPart = new JavaMailMimeBodyPart();
            xmlPart.setDataHandler(new DataHandler(new XmlPartDataSource(xml)));
            mmp.addBodyPart(xmlPart);
            
            return mmp;
        }
        
        private String textPart(Locale locale) {
            StringBuilder sb = new StringBuilder();

            MsgKey msgKey = DistributionListSubscribeOp.subscribe == op ? MsgKey.groupSubscriptionAppravalText :
                MsgKey.groupUnsubscriptionAppravalText;
            
            sb.append("\n");
            sb.append(L10nUtil.getMessage(msgKey, locale, 
                    requestingAcct.getName(), group.getName()));
            sb.append("\n\n");
            return sb.toString();
        }

        private String htmlPart(Locale locale) {
            StringBuilder sb = new StringBuilder();

            sb.append("<h4>\n");
            sb.append("<p>" + textPart(locale) + "</p>\n");
            sb.append("</h4>\n");
            sb.append("\n");
            return sb.toString();
        }
        
        private String xmlPart(Locale locale) {
            StringBuilder sb = new StringBuilder();

            final String URI = "urn:zimbraDLSubscription";
            final String VERSION = "0.1";

            // make notes xml friendly
            // notes = StringEscapeUtils.escapeXml(notes);
            
            String groupDisplayName = group.getDisplayName();
            groupDisplayName = groupDisplayName == null ? "" : groupDisplayName;
            
            String userDisplayName = requestingAcct.getDisplayName();
            userDisplayName = userDisplayName == null ? "" : userDisplayName;
            
            sb.append(String.format("<%s xmlns=\"%s\" version=\"%s\" action=\"%s\">\n", 
                    MailConstants.E_DL_SUBSCRIPTION_NOTIFICATION, URI, VERSION, op.name()));
            sb.append(String.format("<dl id=\"%s\" email=\"%s\" name=\"%s\">\n",
                    group.getId(), group.getName(), groupDisplayName));
            sb.append(String.format("<user id=\"%s\" email=\"%s\" name=\"%s\">\n",
                    requestingAcct.getId(), requestingAcct.getName(), userDisplayName));
            sb.append(String.format("</%s>\n", MailConstants.E_DL_SUBSCRIPTION_NOTIFICATION));

            return sb.toString();
        }
        
        private static abstract class MimePartDataSource implements DataSource {

            private String mText;
            private byte[] mBuf = null;

            public MimePartDataSource(String text) {
                mText = text;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                synchronized(this) {
                    if (mBuf == null) {
                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        OutputStreamWriter wout =
                            new OutputStreamWriter(buf, MimeConstants.P_CHARSET_UTF8);
                        String text = mText;
                        wout.write(text);
                        wout.flush();
                        mBuf = buf.toByteArray();
                    }
                }
                return new ByteArrayInputStream(mBuf);
            }

            @Override
            public OutputStream getOutputStream() {
                throw new UnsupportedOperationException();
            }
        }
        
        private static class HtmlPartDataSource extends MimePartDataSource {
            private static final String CONTENT_TYPE =
                MimeConstants.CT_TEXT_HTML + "; " + 
                MimeConstants.P_CHARSET + "=" + MimeConstants.P_CHARSET_UTF8;
            private static final String NAME = "HtmlDataSource";

            HtmlPartDataSource(String text) {
                super(text);
            }

            @Override
            public String getContentType() {
                return CONTENT_TYPE;
            }

            @Override
            public String getName() {
                return NAME;
            }
        }
        
        private static class XmlPartDataSource extends MimePartDataSource {
            private static final String CONTENT_TYPE =
                MimeConstants.CT_XML_ZIMBRA_DL_SUBSCRIPTION + "; " + 
                MimeConstants.P_CHARSET + "=" + MimeConstants.P_CHARSET_UTF8;
            private static final String NAME = "XmlDataSource";

            XmlPartDataSource(String text) {
                super(text);
            }

            @Override
            public String getContentType() {
                return CONTENT_TYPE;
            }

            @Override
            public String getName() {
                return NAME;
            }
        }
    }

}
