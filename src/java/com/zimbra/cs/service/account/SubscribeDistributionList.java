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
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.ACLUtil;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;

public class SubscribeDistributionList extends AccountDocumentHandler {
    
    private enum SubscribeOp {
        subscribe,
        unsubscribe;
        
        private static SubscribeOp fromString(String str) throws ServiceException {
            try {
                return SubscribeOp.valueOf(str);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid op: " + str, e);
            }
        }
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Group group = GetDistributionList.getGroup(request, prov);
        
        SubscribeOp op = SubscribeOp.fromString(request.getAttribute(AccountConstants.A_OP));
        
        Account acct = getRequestedAccount(zsc);
        String[] members = new String[]{acct.getName()};
        
        boolean accepted = false;
        if (op == SubscribeOp.subscribe) {
            DistributionListSubscriptionPolicy policy = group.getDistributionListSubscriptionPolicy();
            if (policy == null) {
                policy = CreateDistributionList.DEFAULT_SUBSCRIPTION_POLICY;
            }
            
            if (policy == DistributionListSubscriptionPolicy.ACCEPT) {
                prov.addGroupMembers(group, members);
                accepted = true;
            } else if (policy == DistributionListSubscriptionPolicy.REJECT) {
                throw ServiceException.PERM_DENIED("subscription policy for group " + group.getName() + " is reject");
            } else { // REQUEST APPROAVAL
                ApprovalSender sender = new ApprovalSender(prov, group, acct, op);
                sender.composeAndSend();
            }
            
        } else {
            DistributionListUnsubscriptionPolicy policy = group.getDistributionListUnsubscriptionPolicy();
            if (policy == null) {
                policy = CreateDistributionList.DEFAULT_UNSUBSCRIPTION_POLICY;
            }
            
            if (policy == DistributionListUnsubscriptionPolicy.ACCEPT) {
                prov.removeGroupMembers(group, members);
                accepted = true;
            } else if (policy == DistributionListUnsubscriptionPolicy.REJECT) {
                throw ServiceException.PERM_DENIED("un-subscription policy for group " + group.getName() + " is reject");
            } else { // REQUEST APPROAVAL
                ApprovalSender sender = new ApprovalSender(prov, group, acct, op);
                sender.composeAndSend();
            }
            
        }
        
        if (accepted) {
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                        new String[] {"cmd", "SubscribeDistributionList","name", group.getName(), 
                        "op", op.name(),        
                        "member", Arrays.deepToString(members)})); 
        }
        
        Element response = zsc.createElement(AccountConstants.SUBSCRIBE_DISTRIBUTION_LIST_RESPONSE);

        return response;
    }
    
    private static class ApprovalSender {
        private Provisioning prov;
        private Group group;
        private Account requestingAcct;
        private SubscribeOp op;
        
        private ApprovalSender(Provisioning prov, Group group, Account requestingAcct, SubscribeOp op) {
            this.prov = prov;
            this.group = group;
            this.requestingAcct = requestingAcct;
            this.op = op;
        }
        
        private void composeAndSend() throws ServiceException {
            // list of owner emails
            List<String> owners = new ArrayList<String>();
            
            List<ZimbraACE> acl = ACLUtil.getAllACEs(group);
            if (acl != null) {
                for (ZimbraACE ace : acl) {
                    Right right = ace.getRight();
                    if (User.R_ownDistList == right) {
                        owners.add(ace.getGranteeDisplayName());
                    }
                }
            }
            
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
            String html = htmlPart(locale);;
        
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

            return mmp;
        }
        
        private String textPart(Locale locale) {
            StringBuilder sb = new StringBuilder();

            MsgKey msgKey = SubscribeOp.subscribe == op ? MsgKey.groupSubscriptionAppravalText :
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
                MimeConstants.CT_TEXT_HTML + "; " + MimeConstants.P_CHARSET + "=" + MimeConstants.P_CHARSET_UTF8;
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
    }

}
