package com.zimbra.cs.service.mail;

import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetShareDetailsRequest;
import com.zimbra.soap.mail.message.GetShareDetailsResponse;
import com.zimbra.soap.mail.type.ShareDetails;
import com.zimbra.soap.mail.type.ShareGrantee;
import com.zimbra.soap.type.GrantGranteeType;

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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
public class GetShareDetails extends MailDocumentHandler {

    protected static final String[] TARGET_ITEM_PATH = new String[] { MailConstants.E_ITEM, MailConstants.A_ID };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_ITEM_PATH;
    }

    @Override
    protected String[] getResponseItemPath() {
        return TARGET_ITEM_PATH;
    }

    @Override
    protected boolean checkMountpointProxy(Element request) {
        return true;
    }

    @VisibleForTesting
    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) throws ServiceException {
        return super.proxyIfNecessary(request, context);
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getAuthenticatedAccount(zsc);
        if (acct.isAccountExternal() || acct.isIsExternalVirtualAccount()) {
            throw ServiceException.PERM_DENIED("you do not have permission to view share details");
        }
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        GetShareDetailsRequest req = JaxbUtil.elementToJaxb(request);
        ItemId iid = new ItemId(req.getItem().getId(), zsc);
        ACL acl = null;
        try {
            acl = mbox.getItemById(octxt, iid.getId(), MailItem.Type.UNKNOWN).getACL();
        } catch (NoSuchItemException nsie) {
            // mask nonexistent item as permission denied
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions");
        }

        GetShareDetailsResponse resp = new GetShareDetailsResponse(iid.toString(acct));
        ShareDetails details = resp.getShareDetails();
        if (acl != null) {
            for (ACL.Grant grant : acl.getGrants()) {
                byte granteeType = grant.getGranteeType();
                NamedEntry nentry = FolderAction.lookupGranteeByZimbraId(grant.getGranteeId(), granteeType);

                ShareGrantee grantee = new ShareGrantee();
                grantee.setPerm(ACL.rightsToString(grant.getGrantedRights()));
                grantee.setGranteeType(GrantGranteeType.fromString(ACL.typeToString(granteeType)));
                if (nentry != null) {
                    if (nentry instanceof Account) {
                        grantee.setGranteeName(((Account) nentry).getDisplayName());
                        grantee.setGranteeEmail(nentry.getName());
                    } else if (nentry instanceof Group) {
                        grantee.setGranteeName(((Group) nentry).getDisplayName());
                        grantee.setGranteeEmail(nentry.getName());
                    } else {
                        grantee.setGranteeName(nentry.getName());
                    }
                }
                details.addGrantee(grantee);
            }
        }
        return zsc.jaxbToElement(resp);
    }
}
