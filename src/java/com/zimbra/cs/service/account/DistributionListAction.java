package com.zimbra.cs.service.account;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.google.common.collect.Lists;
import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.GranteeBy;
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
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.UserRight;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.type.DistributionListSubscribeOp;

public class DistributionListAction extends DistributionListDocumentHandler {
    
    private static enum Operation {
        delete,
        modify,
        rename,
        addAlias,
        removeAlias,
        addOwner,
        removeOwner,
        addMembers,
        removeMembers,
        acceptSubsReq,
        rejectSubsReq;
        
        private static Operation fromString(String str) throws ServiceException {
            try {
                return Operation.valueOf(str);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid op: " + str, e);
            }
        }
    };
    
    public Element handle(Element request, Map<String, Object> context) 
    throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account acct = getRequestedAccount(zsc);
        
        Group group = getGroupBasic(request, prov);
        DistributionListActionHandler handler = new DistributionListActionHandler(
                group, request, prov, acct);
        handler.handle();
        
        Element response = zsc.createElement(AccountConstants.DISTRIBUTION_LIST_ACTION_RESPONSE);
        return response;
    }
    
    private static class DistributionListActionHandler extends SynchronizedGroupHandler {
        private Element request;
        private Provisioning prov;
        private Account acct;
        
        protected DistributionListActionHandler(Group group,
                Element request, Provisioning prov, Account acct) {
            super(group);
            this.request = request;
            this.prov = prov;
            this.acct = acct;
        }

        @Override
        protected void handleRequest() throws ServiceException {
            if (!isOwner(acct, group)) {
                throw ServiceException.PERM_DENIED(
                        "you do not have sufficient rights to access this distribution list");
            }
            
            Element eAction = request.getElement(AccountConstants.E_ACTION);
            Operation op = Operation.fromString(eAction.getAttribute(AccountConstants.A_OP));
            
            DLActionHandler handler = null;
            switch (op) {
                case delete:
                    handler = new DeleteHandler(eAction, group, prov, acct);
                    break;
                case modify:
                    handler = new ModifyHandler(eAction, group, prov, acct);
                    break;
                case rename:
                    handler = new RenameHandler(eAction, group, prov, acct);
                    break;
                case addAlias:
                    handler = new AddAliasHandler(eAction, group, prov, acct);
                    break;
                case removeAlias:
                    handler = new RemoveAliasHandler(eAction, group, prov, acct);
                    break;
                case addOwner:
                    handler = new AddOwnerHandler(eAction, group, prov, acct);
                    break;
                case removeOwner:
                    handler = new RemoveOwnerHandler(eAction, group, prov, acct);
                    break;
                case addMembers:
                    handler = new AddMembersHandler(eAction, group, prov, acct);
                    break;
                case removeMembers:
                    handler = new RemoveMembersHandler(eAction, group, prov, acct);
                    break;
                case acceptSubsReq:
                    handler = new AcceptSubsReqHandler(eAction, group, prov, acct);
                    break;
                case rejectSubsReq:
                    handler = new RejectSubsReqHandler(eAction, group, prov, acct);
                    break;     
                default:
                    throw ServiceException.FAILURE("unsupported op:" + op.name(), null);
            }
            
            handler.handle();
        }
        
    }
    
    private static abstract class DLActionHandler {
        protected Element eAction;
        protected Group group;
        protected Provisioning prov;
        protected Account requestedAcct;
        
        protected DLActionHandler(Element request, Group group, 
                Provisioning prov, Account requestedAcct) {
            this.eAction = request;
            this.group = group;
            this.prov = prov;
            this.requestedAcct = requestedAcct;
        }
        
        abstract void handle() throws ServiceException;
        abstract Operation getAction();
    }
    
    private static class DeleteHandler extends DLActionHandler {

        protected DeleteHandler(Element eAction, Group group, 
                Provisioning prov, Account requestedAcct) {
            super(eAction, group, prov, requestedAcct);
        }

        @Override
        Operation getAction() {
            return Operation.delete;
        }
        
        @Override
        void handle() throws ServiceException {
            prov.deleteGroup(group.getId());
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", group.getName(), "id", group.getId()}));
        }

    }
    
    private static class ModifyHandler extends DLActionHandler {

        protected ModifyHandler(Element eAction, Group group, 
                Provisioning prov, Account requestedAcct) {
            super(eAction, group, prov, requestedAcct);
        }
        
        @Override
        Operation getAction() {
            return Operation.modify;
        }

        @Override
        void handle() throws ServiceException {
            Map<String, Object> attrs = AccountService.getAttrs(
                    eAction, true, AccountConstants.A_N);
            prov.modifyAttrs(group, attrs, true);    
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", group.getName()}, attrs)); 
        }
    }
    
    private static class RenameHandler extends DLActionHandler {

        protected RenameHandler(Element eAction, Group group, 
                Provisioning prov, Account requestedAcct) {
            super(eAction, group, prov, requestedAcct);
        }
        
        @Override
        Operation getAction() {
            return Operation.rename;
        }

        @Override
        void handle() throws ServiceException {
            Element eNewName = eAction.getElement(AccountConstants.E_NEW_NAME);
            String newName = eNewName.getText();
            
            String oldName = group.getName();
            prov.renameGroup(group.getId(), newName);

            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", oldName, "newName", newName})); 
        }
    }
    
    private static class AddAliasHandler extends DLActionHandler {

        protected AddAliasHandler(Element eAction, Group group, 
                Provisioning prov, Account requestedAcct) {
            super(eAction, group, prov, requestedAcct);
        }
        
        @Override
        Operation getAction() {
            return Operation.addAlias;
        }

        @Override
        void handle() throws ServiceException {
            String alias = eAction.getAttribute(AccountConstants.E_ALIAS);
            prov.addGroupAlias(group, alias);
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", group.getName(), "alias", alias})); 
        }
    }
    
    private static class RemoveAliasHandler extends DLActionHandler {

        protected RemoveAliasHandler(Element eAction, Group group, 
                Provisioning prov, Account requestedAcct) {
            super(eAction, group, prov, requestedAcct);
        }
        
        @Override
        Operation getAction() {
            return Operation.removeAlias;
        }

        @Override
        void handle() throws ServiceException {
            String alias = eAction.getAttribute(AccountConstants.E_ALIAS);
            prov.removeGroupAlias(group, alias);
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", group.getName(), "alias", alias})); 
        }
    }
    
    static class AddOwnerHandler extends DLActionHandler {

        protected AddOwnerHandler(Element eAction, Group group, 
                Provisioning prov, Account requestedAcct) {
            super(eAction, group, prov, requestedAcct);
        }
        
        @Override
        Operation getAction() {
            return Operation.addOwner;
        }

        @Override
        void handle() throws ServiceException {
            Element eOwner = eAction.getElement(AccountConstants.E_OWNER);
            GranteeType ownerType = GranteeType.fromCode(eOwner.getAttribute(AccountConstants.A_TYPE));
            GranteeBy ownerBy = GranteeBy.fromString(eOwner.getAttribute(AccountConstants.A_BY));
            String owner = eOwner.getText();
            
            addOwner(prov, group, ownerType, ownerBy, owner);
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", group.getName(), "type", ownerType.getCode(),
                            "owner", owner})); 
        }
        
        
        public static void addOwner(Provisioning prov, Group group, GranteeType granteeType, 
                Key.GranteeBy granteeBy, String grantee) throws ServiceException {
            RightCommand.grantRight(prov,
                    null,  // grant the right as a a system admin
                    TargetType.dl.getCode(), Key.TargetBy.id, group.getId(),
                    granteeType.getCode(), granteeBy, grantee, null,
                    UserRight.RT_ownDistList, null);
        }
    }
    
    static class RemoveOwnerHandler extends DLActionHandler {

        protected RemoveOwnerHandler(Element eAction, Group group, 
                Provisioning prov, Account requestedAcct) {
            super(eAction, group, prov, requestedAcct);
        }
        
        @Override
        Operation getAction() {
            return Operation.removeOwner;
        }

        @Override
        void handle() throws ServiceException {
            Element eOwner = eAction.getElement(AccountConstants.E_OWNER);
            GranteeType ownerType = GranteeType.fromCode(eOwner.getAttribute(AccountConstants.A_TYPE));
            GranteeBy ownerBy = GranteeBy.fromString(eOwner.getAttribute(AccountConstants.A_BY));
            String owner = eOwner.getText();
            
            removeOwner(prov, group, ownerType, ownerBy, owner);
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                            "name", group.getName(), "type", ownerType.getCode(),
                            "owner", owner})); 
        }
        
        public static void removeOwner(Provisioning prov, Group group, GranteeType granteeType, 
                Key.GranteeBy granteeBy, String grantee) throws ServiceException {
            RightCommand.revokeRight(prov,
                    null,  // grant the right as a a system admin
                    TargetType.dl.getCode(), Key.TargetBy.id, group.getId(),
                    granteeType.getCode(), granteeBy, grantee, 
                    UserRight.RT_ownDistList, null);
        }
    }
    
    private static class AddMembersHandler extends DLActionHandler {

        protected AddMembersHandler(Element eAction, Group group, 
                Provisioning prov, Account requestedAcct) {
            super(eAction, group, prov, requestedAcct);
        }
        
        @Override
        Operation getAction() {
            return Operation.addMembers;
        }

        @Override
        void handle() throws ServiceException {
            List<String> memberList = new LinkedList<String>();
            for (Element elem : eAction.listElements(AccountConstants.E_DLM)) {
                memberList.add(elem.getTextTrim());
            }
            
            String[] members = (String[]) memberList.toArray(new String[0]); 
            prov.addGroupMembers(group, members);
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                   "name", group.getName(), "members", Arrays.deepToString(members)})); 
        }
    }
    
    private static class RemoveMembersHandler extends DLActionHandler {

        protected RemoveMembersHandler(Element eAction, Group group, 
                Provisioning prov, Account requestedAcct) {
            super(eAction, group, prov, requestedAcct);
        }
        
        @Override
        Operation getAction() {
            return Operation.removeMembers;
        }

        @Override
        void handle() throws ServiceException {
            List<String> memberList = new LinkedList<String>();
            for (Element elem : eAction.listElements(AccountConstants.E_DLM)) {
                memberList.add(elem.getTextTrim());
            }
            
            String[] members = (String[]) memberList.toArray(new String[0]); 
            prov.removeGroupMembers(group, members);
            
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                   "name", group.getName(), "members", Arrays.deepToString(members)})); 
        }
    }
    
    private static class SubscriptionResponseSender extends NotificationSender {
        private Account ownerAcct;  // owner who is handling the request
        private boolean bccOwners;
        private boolean accepted;
        
        private SubscriptionResponseSender(Provisioning prov, Group group, 
                Account ownerAcct, Account requestingAcct, 
                DistributionListSubscribeOp op, boolean bccOwners, boolean accepted) {
            super(prov, group, requestingAcct, op);
            this.ownerAcct = ownerAcct;
            this.bccOwners = bccOwners;
            this.accepted = accepted;
        }
        
        private void sendMessage() throws ServiceException {
            try {
                SMTPMessage out = new SMTPMessage(JMSession.getSmtpSession());
                
                Address fromAddr = AccountUtil.getFriendlyEmailAddress(ownerAcct);
                
                Address replyToAddr = fromAddr;
                String replyTo = ownerAcct.getAttr(Provisioning.A_zimbraPrefReplyToAddress);
                if (replyTo != null) {
                    replyToAddr = new JavaMailInternetAddress(replyTo);
                }
                
                // From
                out.setFrom(fromAddr);
                
                // Reply-To
                out.setReplyTo(new Address[]{replyToAddr});
                
                // To
                out.setRecipient(javax.mail.Message.RecipientType.TO, 
                        new JavaMailInternetAddress(requestingAcct.getName()));
                
                // Bcc all other owners of the list
                if (bccOwners) {
                    List<String> owners = new ArrayList<String>();
                    Group.GroupOwner.getOwnerEmails(group, owners);
                    
                    List<Address> addrs = Lists.newArrayList();
                    for (String ownerEmail : owners) {
                        if (!ownerEmail.equals(ownerAcct.getName())) {
                            addrs.add(new JavaMailInternetAddress(ownerEmail));
                        }
                    }
                    if (!addrs.isEmpty()) {
                        out.addRecipients(javax.mail.Message.RecipientType.BCC, 
                                addrs.toArray(new Address[addrs.size()]));
                    }
                }
                
                // Date
                out.setSentDate(new Date());
                
                // send in the receiver's(i.e. the requesting account) locale
                Locale locale = getLocale(requestingAcct);
                
                // Subject
                String subject = L10nUtil.getMessage(MsgKey.dlSubscriptionResponseSubject, locale);
                out.setSubject(subject);
                
                buildContentAndSend(out, locale, "group subscription response");
            
            } catch (MessagingException e) {
                ZimbraLog.account.warn("send share info notification failed, rcpt='" + 
                        requestingAcct.getName() +"'", e);
            }

        }

        @Override
        protected MimeMultipart buildMailContent(Locale locale)
        throws MessagingException {
            String text = textPart(locale);
            String html = htmlPart(locale);
            
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

            
            MsgKey msgKey;
            if (accepted) {
                msgKey = DistributionListSubscribeOp.subscribe == op ? MsgKey.dlSubscribeResponseAcceptedText :
                    MsgKey.dlUnsubscribeResponseAcceptedText;
            } else {
                msgKey = DistributionListSubscribeOp.subscribe == op ? MsgKey.dlSubscribeResponseRejectedText :
                    MsgKey.dlUnsubscribeResponseRejectedText;
                
            }
            
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

    }

    private static class AcceptSubsReqHandler extends DLActionHandler {

        protected AcceptSubsReqHandler(Element eAction, Group group, 
                Provisioning prov, Account requestedAcct) {
            super(eAction, group, prov, requestedAcct);
        }
        
        @Override
        Operation getAction() {
            return Operation.acceptSubsReq;
        }

        @Override
        void handle() throws ServiceException {
            
            Element eSubsReq = eAction.getElement(AccountConstants.E_DL_SUBS_REQ);
            DistributionListSubscribeOp subsOp = DistributionListSubscribeOp.fromString(
                    eSubsReq.getAttribute(AccountConstants.A_OP));
            boolean bccOwners = eSubsReq.getAttributeBool(AccountConstants.A_BCC_OWNERS, true);
            String memberEmail = eSubsReq.getText();
            
            Account memberAcct = prov.get(AccountBy.name, memberEmail);
            if (memberAcct == null) {
                throw ServiceException.DEFEND_ACCOUNT_HARVEST(memberEmail);
            }
            boolean isMember = DistributionListDocumentHandler.isMember(prov, memberAcct, group);
            
            boolean processed = false;
            if (isMember) {
                if (subsOp == DistributionListSubscribeOp.subscribe) {
                    // do nothing
                    ZimbraLog.account.debug("AcceptSubsReqHandler: " + memberEmail + 
                            " is currently a member in list " + group.getName() + 
                            ", no action taken for the subscribe request");
                } else {
                    prov.removeGroupMembers(group, new String[]{memberEmail});
                    processed = true;
                }
            } else {
                // not currently a member
                if (subsOp == DistributionListSubscribeOp.subscribe) {
                    prov.addGroupMembers(group, new String[]{memberEmail});
                    processed = true;
                } else {
                    // do nothing
                    ZimbraLog.account.debug("AcceptSubsReqHandler: " + memberEmail + 
                            " is currently not a member in list " + group.getName() + 
                            ", no action taken for the un-subscribe request");
                }
            }
            
            if (processed) {
                ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                        new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                       "name", group.getName(), "subsOp", subsOp.name(), "member", memberEmail})); 
                
                // send notification email to the user and bcc other owners
                SubscriptionResponseSender notifSender = new SubscriptionResponseSender(
                        prov, group, requestedAcct, memberAcct, 
                        subsOp, bccOwners, true);
                notifSender.sendMessage();
            }
        }
        
    }
    
    private static class RejectSubsReqHandler extends DLActionHandler {

        protected RejectSubsReqHandler(Element eAction, Group group, 
                Provisioning prov, Account requestedAcct) {
            super(eAction, group, prov, requestedAcct);
        }
        
        @Override
        Operation getAction() {
            return Operation.rejectSubsReq;
        }

        @Override
        void handle() throws ServiceException {
            
            Element eSubsReq = eAction.getElement(AccountConstants.E_DL_SUBS_REQ);
            DistributionListSubscribeOp subsOp = DistributionListSubscribeOp.fromString(
                    eSubsReq.getAttribute(AccountConstants.A_OP));
            boolean bccOwners = eSubsReq.getAttributeBool(AccountConstants.A_BCC_OWNERS, true);
            String memberEmail = eSubsReq.getText();
            
            Account memberAcct = prov.get(AccountBy.name, memberEmail);
            if (memberAcct == null) {
                throw ServiceException.DEFEND_ACCOUNT_HARVEST(memberEmail);
            }
            boolean isMember = DistributionListDocumentHandler.isMember(prov, memberAcct, group);
            
            boolean processed = false;
            if (isMember) {
                if (subsOp == DistributionListSubscribeOp.subscribe) {
                    // do nothing
                    ZimbraLog.account.debug("RejectSubsReqHandler: " + memberEmail + 
                            " is currently a member in list " + group.getName() + 
                            ", no action taken for the subscribe request");
                } else {
                    processed = true;
                }
            } else {
                // not currently a member
                if (subsOp == DistributionListSubscribeOp.subscribe) {
                    processed = true;
                } else {
                    // do nothing
                    ZimbraLog.account.debug("RejectSubsReqHandler: " + memberEmail + 
                            " is currently not a member in list " + group.getName() + 
                            ", no action taken for the un-subscribe request");
                }
            }
            
            if (processed) {
                ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                        new String[] {"cmd", "DistributionListAction", "op", getAction().name(), 
                       "name", group.getName(), "subsOp", subsOp.name(), "member", memberEmail})); 
                
                // send notification email to the user and bcc other owners
                SubscriptionResponseSender notifSender = new SubscriptionResponseSender(
                        prov, group, requestedAcct, memberAcct, 
                        subsOp, bccOwners, false);
                notifSender.sendMessage();
            }
        }
    }
}
