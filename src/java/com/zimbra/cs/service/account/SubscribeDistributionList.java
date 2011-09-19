package com.zimbra.cs.service.account;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import com.zimbra.common.util.ZimbraLog;
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
            } else { // APPROAVAL
                sendRequestForApprovalEmail(prov, group, acct, op);
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
            } else { // APPROAVAL
                sendRequestForApprovalEmail(prov, group, acct, op);
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
    
    private void sendRequestForApprovalEmail(Provisioning prov, Group group, Account acct, SubscribeOp op) 
    throws ServiceException {
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
            sendMessage(prov, acct, owner);
        }
    }
    
    private void sendMessage(Provisioning prov, Account fromAcct, String toAddr) 
    throws ServiceException {
        try {
            SMTPMessage out = new SMTPMessage(JMSession.getSmtpSession());
            
            Address fromAddr = AccountUtil.getFriendlyEmailAddress(fromAcct);
            
            Address replyToAddr = fromAddr;
            String replyTo = fromAcct.getAttr(Provisioning.A_zimbraPrefReplyToAddress);
            if (replyTo != null) {
                replyToAddr = new JavaMailInternetAddress(replyTo);
            }
            
            // From
            out.setFrom(fromAddr);
            
            // Reply-To
            out.setReplyTo(new Address[]{replyToAddr});
            
            // To
            out.setRecipient(javax.mail.Message.RecipientType.TO, new JavaMailInternetAddress(toAddr));
            
            // Date
            out.setSentDate(new Date());
            
            // Subject
            Locale locale = getLocale(prov, fromAcct, toAddr);
            String subject = "Group subscription approval request"; // L10nUtil.getMessage(MsgKey.groupSubscriptionAppraval, locale);
            out.setSubject(subject);
            
            buildContentAndSend(out);
        
        } catch (MessagingException e) {
            ZimbraLog.account.warn("send share info notification failed rcpt='" + toAddr +"'", e);
        }
    }
    
    private Locale getLocale(Provisioning prov, Account fromAcct, String toAddr) throws ServiceException {
        Locale locale;
        Account rcptAcct = prov.get(AccountBy.name, toAddr);
        if (rcptAcct != null)
            locale = rcptAcct.getLocale();
        else if (fromAcct != null)
            locale = fromAcct.getLocale();
        else
            locale = prov.getConfig().getLocale();

        return locale;
    }
    
    private void buildContentAndSend(SMTPMessage out)
    throws MessagingException {
    
        MimeMultipart mmp = buildMailContent();
        out.setContent(mmp);
        Transport.send(out);
    
        // log
        Address[] rcpts = out.getRecipients(javax.mail.Message.RecipientType.TO);
        StringBuilder rcptAddr = new StringBuilder();
        for (Address a : rcpts) {
            rcptAddr.append(a.toString());
        }
        ZimbraLog.account.info("group subscription request sent rcpt='" + rcptAddr + "' Message-ID=" + out.getMessageID());
    }
    
    private MimeMultipart buildMailContent()
    throws MessagingException {
        String shareInfoText = "hello";
        String shareInfoHtml = null;
        String shareInfoXml = null;
    
        // Body
        MimeMultipart mmp = new JavaMailMimeMultipart("alternative");
    
        // TEXT part (add me first!)
        MimeBodyPart textPart = new JavaMailMimeBodyPart();
        textPart.setText(shareInfoText, MimeConstants.P_CHARSET_UTF8);
        mmp.addBodyPart(textPart);
    
        /*
        // HTML part
        MimeBodyPart htmlPart = new JavaMailMimeBodyPart();
        htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(shareInfoHtml)));
        mmp.addBodyPart(htmlPart);
    
        // XML part
        if (shareInfoXml != null) {
            MimeBodyPart xmlPart = new JavaMailMimeBodyPart();
            xmlPart.setDataHandler(new DataHandler(new XmlPartDataSource(shareInfoXml)));
            mmp.addBodyPart(xmlPart);
        }
        */
        
        return mmp;
    }
}
