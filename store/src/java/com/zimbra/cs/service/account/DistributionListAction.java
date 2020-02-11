/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Group.GroupOwner;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.ACLUtil;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.account.accesscontrol.ZimbraACE.ExternalGroupInfo;
import com.zimbra.cs.account.names.NameUtil.EmailAddress;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.type.DistributionListAction.Operation;
import com.zimbra.soap.account.type.DistributionListSubscribeOp;
import com.zimbra.soap.admin.type.GranteeSelector.GranteeBy;
import com.zimbra.soap.type.TargetBy;

public class DistributionListAction extends DistributionListDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account acct = getAuthenticatedAccount(zsc);

        Group group = getGroupBasic(request, prov);
        DistributionListActionHandler handler = new DistributionListActionHandler(
                group, request, prov, acct);
        handler.handle();

        Element response = zsc.createElement(AccountConstants.DISTRIBUTION_LIST_ACTION_RESPONSE);
        return response;
    }

    private static class DistributionListActionHandler extends SynchronizedGroupHandler {
        private final Element request;
        private final Provisioning prov;
        private final Account acct;

        protected DistributionListActionHandler(Group group,
                Element request, Provisioning prov, Account acct) {
            super(group);
            this.request = request;
            this.prov = prov;
            this.acct = acct;
        }

        @Override
        protected void handleRequest() throws ServiceException {
            Element eAction = request.getElement(AccountConstants.E_ACTION);
            Operation op = Operation.fromString(eAction.getAttribute(AccountConstants.A_OP));

            // all ops need owner right
            // delete and rename ops also need create right, will check in the handlers
            if (!GroupOwner.hasOwnerPrivilege(acct, group)) {
                throw ServiceException.PERM_DENIED(
                        "you do not have sufficient rights to access this distribution list");
            }

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
                case addOwners:
                    handler = new AddOwnersHandler(eAction, group, prov, acct);
                    break;
                case removeOwners:
                    handler = new RemoveOwnersHandler(eAction, group, prov, acct);
                    break;
                case setOwners:
                    handler = new SetOwnersHandler(eAction, group, prov, acct);
                    break;
                case grantRights:
                    handler = new GrantRightsHandler(eAction, group, prov, acct);
                    break;
                case revokeRights:
                    handler = new RevokeRightsHandler(eAction, group, prov, acct);
                    break;
                case setRights:
                    handler = new SetRightsHandler(eAction, group, prov, acct);
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
        protected Account authedAcct;

        protected DLActionHandler(Element request, Group group,
                Provisioning prov, Account authedAcct) {
            this.eAction = request;
            this.group = group;
            this.prov = prov;
            this.authedAcct = authedAcct;
        }

        abstract void handle() throws ServiceException;
        abstract Operation getAction();
    }

    private static class DeleteHandler extends DLActionHandler {

        protected DeleteHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
        }

        @Override
        Operation getAction() {
            return Operation.delete;
        }

        @Override
        void handle() throws ServiceException {
            if (!AccessManager.getInstance().canCreateGroup(authedAcct, group.getName())) {
                throw ServiceException.PERM_DENIED(
                        "you do not have sufficient rights to delete this distribution list");
            }

            prov.deleteGroup(group.getId());

            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(),
                            "name", group.getName(), "id", group.getId()}));
        }

    }

    private static class ModifyHandler extends DLActionHandler {

        protected ModifyHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
        }

        @Override
        Operation getAction() {
            return Operation.modify;
        }

        @Override
        void handle() throws ServiceException {
            Map<String, Object> attrs = AccountService.getKeyValuePairs(
                    eAction, AccountConstants.E_A, AccountConstants.A_N);
            prov.modifyAttrs(group, attrs, true);

            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(),
                            "name", group.getName()}, attrs));
        }
    }

    private static class RenameHandler extends DLActionHandler {

        protected RenameHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
        }

        @Override
        Operation getAction() {
            return Operation.rename;
        }

        @Override
        void handle() throws ServiceException {

            Element eNewName = eAction.getElement(AccountConstants.E_NEW_NAME);
            String newName = eNewName.getText();

            /*
             * need the create right on the current domain.
             *
             * The create right means being able to create/rename/delete.
             */
            String oldName = group.getName();
            if (!AccessManager.getInstance().canCreateGroup(authedAcct, oldName)) {
                throw ServiceException.PERM_DENIED(
                        "you do not have sufficient rights to rename this distribution list");
            }

            /*
             * if domain is different, need the create right on the new domain.
             */
            String curDomain = new EmailAddress(oldName).getDomain();
            String newDomain = new EmailAddress(newName).getDomain();
            if (!curDomain.equalsIgnoreCase(newDomain)) {
                if (!AccessManager.getInstance().canCreateGroup(authedAcct, newName)) {
                    throw ServiceException.PERM_DENIED(
                            "you do not have sufficient rights to rename this distribution list");
                }
            }

            prov.renameGroup(group.getId(), newName);

            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(),
                            "name", oldName, "newName", newName}));
        }
    }

    private static abstract class ModifyRightHandler extends DLActionHandler {

        protected ModifyRightHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
        }

        protected class Grantee {
            GranteeType type;
            GranteeBy by;
            String grantee;

            private Grantee(GranteeType type, GranteeBy by, String grantee) {
                this.type = type;
                this.by = by;
                this.grantee = grantee;
            }
        };

        protected List<Grantee> parseGrantees(Element parent, String granteeElem)
        throws ServiceException {
            List<Grantee> grantees = Lists.newArrayList();

            String domain = prov.getDomain(authedAcct).getName();
            for (Element eGrantee : parent.listElements(granteeElem)) {
                GranteeType type = GranteeType.fromCode(eGrantee.getAttribute(AccountConstants.A_TYPE));

                GranteeBy by = null;
                String grantee = null;

                if (type.needsGranteeIdentity()) {
                    by = GranteeBy.fromString(eGrantee.getAttribute(AccountConstants.A_BY));
                    grantee = eGrantee.getText();
                }

                type = GranteeType.determineGranteeType(type, by, grantee, domain);

                if (type == GranteeType.GT_EXT_GROUP) {
                    grantee = ExternalGroupInfo.encodeIfExtGroupNameMissingDomain(domain, grantee);
                }
                grantees.add(new Grantee(type, by, grantee));
            }

            return grantees;
        }

        protected void verifyGrantRight(Right right, GranteeType granteeType,
                GranteeBy granteeBy, String grantee) throws ServiceException {
            RightCommand.verifyGrantRight(prov,
                    null,  // grant the right as a a system admin
                    TargetType.dl.getCode(), TargetBy.id, group.getId(),
                    granteeType.getCode(), granteeBy, grantee, null,
                    right.getName(), null);
        }

        protected void grantRight(Right right, GranteeType granteeType,
                GranteeBy granteeBy, String grantee)
        throws ServiceException {
            RightCommand.grantRight(prov,
                    null,  // grant the right as a a system admin
                    TargetType.dl.getCode(), TargetBy.id, group.getId(),
                    granteeType.getCode(), granteeBy, grantee, null,
                    right.getName(), null);

            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(),
                            "name", group.getName(), "type", granteeType.getCode(),
                            "grantee", grantee}));
        }

        protected void revokeRight(Right right, GranteeType granteeType,
                GranteeBy granteeBy, String grantee)
        throws ServiceException {
            RightCommand.revokeRight(prov,
                    null,  // grant the right as a a system admin
                    TargetType.dl.getCode(), TargetBy.id, group.getId(),
                    granteeType.getCode(), granteeBy, grantee,
                    right.getName(), null);

            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(),
                            "name", group.getName(), "type", granteeType.getCode(),
                            "grantee", grantee}));
        }
    }

    static class AddOwnersHandler extends ModifyRightHandler {

        /*
         * valid owner types for owner right
         * owner cannot be external users or public
         */
        private static final Set<GranteeType> VALID_GRANTEE_TYPES =
            ImmutableSet.of(GranteeType.GT_USER,
                            GranteeType.GT_GROUP,
                            GranteeType.GT_EXT_GROUP,
                            GranteeType.GT_DOMAIN,
                            GranteeType.GT_AUTHUSER);

        protected AddOwnersHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
        }

        @Override
        Operation getAction() {
            return Operation.addOwners;
        }

        @Override
        void handle() throws ServiceException {
            List<Grantee> owners = parseGrantees(eAction, AccountConstants.E_OWNER);

            for (Grantee owner : owners) {
                verifyOwner(this, owner.type, owner.by, owner.grantee);
            }

            for (Grantee owner : owners) {
                addOwner(this, owner.type, owner.by, owner.grantee);
            }
        }

        private static void verifyOwner(ModifyRightHandler handler,
                GranteeType granteeType, GranteeBy granteeBy, String grantee)
        throws ServiceException {
            if (!VALID_GRANTEE_TYPES.contains(granteeType)) {
                throw ServiceException.INVALID_REQUEST(
                        "invalid grantee type for groups owner", null);
            }

            handler.verifyGrantRight(Group.GroupOwner.GROUP_OWNER_RIGHT,
                    granteeType, granteeBy, grantee);
        }

        private static void addOwner(ModifyRightHandler handler,
                GranteeType granteeType, GranteeBy granteeBy, String grantee)
        throws ServiceException {
            handler.grantRight(Group.GroupOwner.GROUP_OWNER_RIGHT,
                    granteeType, granteeBy, grantee);
        }
    }

    static class RemoveOwnersHandler extends ModifyRightHandler {

        protected RemoveOwnersHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
        }

        @Override
        Operation getAction() {
            return Operation.removeOwners;
        }

        @Override
        void handle() throws ServiceException {
            List<Grantee> owners = parseGrantees(eAction, AccountConstants.E_OWNER);
            for (Grantee owner : owners) {
                removeOwner(this, owner.type, owner.by, owner.grantee);
            }
        }
        private static void removeOwner(ModifyRightHandler handler,
                GranteeType granteeType, GranteeBy granteeBy, String grantee)
        throws ServiceException {
            handler.revokeRight(Group.GroupOwner.GROUP_OWNER_RIGHT,
                    granteeType, granteeBy, grantee);
        }
    }

    static class SetOwnersHandler extends ModifyRightHandler {

        protected SetOwnersHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
        }

        @Override
        Operation getAction() {
            return Operation.setOwners;
        }

        @Override
        void handle() throws ServiceException {
            List<Grantee> owners = parseGrantees(eAction, AccountConstants.E_OWNER);

            // bug 72791:
            // validate if the grant can be made before actually modifying anything
            for (Grantee owner : owners) {
                AddOwnersHandler.verifyOwner(this, owner.type, owner.by, owner.grantee);
            }

            // remove all current owners
            List<GroupOwner> curOwners = GroupOwner.getOwners(group, false);
            for (GroupOwner owner : curOwners) {
                RemoveOwnersHandler.removeOwner(this, owner.getType(),
                        GranteeBy.id, owner.getId());
            }

            // add owners
            for (Grantee owner : owners) {
                AddOwnersHandler.addOwner(this, owner.type, owner.by, owner.grantee);
            }
        }
    }

    private static abstract class ModifyMultipleRightsHandler extends ModifyRightHandler {
        protected ModifyMultipleRightsHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
        }

        protected Map<Right, List<Grantee>> parseRights() throws ServiceException {
            RightManager rightMgr = RightManager.getInstance();

            // keep the soap order, use LinkedHashMap
            Map<Right, List<Grantee>> rights = new LinkedHashMap<Right, List<Grantee>>();
            for (Element eRight : eAction.listElements(AccountConstants.E_RIGHT)) {
                Right right = rightMgr.getUserRight(eRight.getAttribute(AccountConstants.A_RIGHT));

                if (Group.GroupOwner.GROUP_OWNER_RIGHT == right) {
                    throw ServiceException.INVALID_REQUEST(right.getName() +
                            " cannot be granted directly, use addOwners/removeOwners/setOwners" +
                            " operation instead", null);
                }
                List<Grantee> grantees = parseGrantees(eRight, AccountConstants.E_GRANTEE);
                rights.put(right, grantees);
            }

            return rights;
        }
    }

    static class GrantRightsHandler extends ModifyMultipleRightsHandler {
        protected GrantRightsHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
        }

        @Override
        Operation getAction() {
            return Operation.grantRights;
        }

        @Override
        void handle() throws ServiceException {
            Map<Right, List<Grantee>> rights = parseRights();
            for (Map.Entry<Right, List<Grantee>> entry : rights.entrySet()) {
                Right right = entry.getKey();
                List<Grantee> grantees = entry.getValue();
                for (Grantee grantee : grantees) {
                    grantRight(right, grantee.type, grantee.by, grantee.grantee);
                }
            }
        }
    }

    static class RevokeRightsHandler extends ModifyMultipleRightsHandler {
        protected RevokeRightsHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
        }

        @Override
        Operation getAction() {
            return Operation.revokeRights;
        }

        @Override
        void handle() throws ServiceException {
            Map<Right, List<Grantee>> rights = parseRights();

            for (Map.Entry<Right, List<Grantee>> entry : rights.entrySet()) {
                Right right = entry.getKey();
                List<Grantee> grantees = entry.getValue();
                for (Grantee grantee : grantees) {
                    revokeRight(right, grantee.type, grantee.by, grantee.grantee);
                }
            }
        }
    }

    static class SetRightsHandler extends ModifyMultipleRightsHandler {

        protected SetRightsHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
        }

        @Override
        Operation getAction() {
            return Operation.setRights;
        }

        @Override
        void handle() throws ServiceException {
            Map<Right, List<Grantee>> rights = parseRights();

            for (Map.Entry<Right, List<Grantee>> entry : rights.entrySet()) {
                Right right = entry.getKey();
                List<Grantee> grantees = entry.getValue();

                // remove all current grants for the right
                List<ZimbraACE> acl = ACLUtil.getACEs(group, Collections.singleton(right));
                if (acl != null) {
                    for (ZimbraACE ace : acl) {
                        revokeRight(right, ace.getGranteeType(),
                                GranteeBy.id, ace.getGrantee());
                    }
                }

                // grant the right to the new grantees
                for (Grantee grantee : grantees) {
                    grantRight(right, grantee.type, grantee.by, grantee.grantee);
                }
            }
        }
    }

    private static class AddMembersHandler extends DLActionHandler {

        protected AddMembersHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
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

            String[] members = memberList.toArray(new String[memberList.size()]);
            addGroupMembers(prov, group, members);

            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(),
                   "name", group.getName(), "members", Arrays.deepToString(members)}));
        }
    }

    private static class RemoveMembersHandler extends DLActionHandler {

        protected RemoveMembersHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
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

            String[] members = memberList.toArray(new String[memberList.size()]);
            removeGroupMembers(prov, group, members);

            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "DistributionListAction", "op", getAction().name(),
                   "name", group.getName(), "members", Arrays.deepToString(members)}));
        }
    }

    private static class SubscriptionResponseSender extends NotificationSender {
        private final Account ownerAcct;  // owner who is handling the request
        private final boolean bccOwners;
        private final boolean accepted;

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
                SMTPMessage out = AccountUtil.getSmtpMessageObj(ownerAcct);
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
            MimeMultipart mmp = new ZMimeMultipart("alternative");

            // TEXT part (add me first!)
            MimeBodyPart textPart = new ZMimeBodyPart();
            textPart.setText(text, MimeConstants.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);

            // HTML part
            MimeBodyPart htmlPart = new ZMimeBodyPart();
            htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(html)));
            mmp.addBodyPart(htmlPart);

            return mmp;
        }

        private String textPart(Locale locale) {
            StringBuilder sb = new StringBuilder();


            MsgKey msgKey;
            if (accepted) {
                msgKey = DistributionListSubscribeOp.subscribe == op ?
                        MsgKey.dlSubscribeResponseAcceptedText :
                        MsgKey.dlUnsubscribeResponseAcceptedText;
            } else {
                msgKey = DistributionListSubscribeOp.subscribe == op ?
                        MsgKey.dlSubscribeResponseRejectedText :
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
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
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
            boolean isMember = group.isMemberOf(memberAcct);

            boolean processed = false;
            if (isMember) {
                if (subsOp == DistributionListSubscribeOp.subscribe) {
                    // do nothing
                    ZimbraLog.account.debug("AcceptSubsReqHandler: " + memberEmail +
                            " is currently a member in list " + group.getName() +
                            ", no action taken for the subscribe request");
                } else {
                    removeGroupMembers(prov, group, new String[]{memberEmail});
                    processed = true;
                }
            } else {
                // not currently a member
                if (subsOp == DistributionListSubscribeOp.subscribe) {
                    addGroupMembers(prov, group, new String[]{memberEmail});
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
                        prov, group, authedAcct, memberAcct,
                        subsOp, bccOwners, true);
                notifSender.sendMessage();
            }
        }

    }

    private static class RejectSubsReqHandler extends DLActionHandler {

        protected RejectSubsReqHandler(Element eAction, Group group,
                Provisioning prov, Account authedAcct) {
            super(eAction, group, prov, authedAcct);
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
            boolean isMember = group.isMemberOf(memberAcct);

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
                        prov, group, authedAcct, memberAcct,
                        subsOp, bccOwners, false);
                notifSender.sendMessage();
            }
        }
    }
}
