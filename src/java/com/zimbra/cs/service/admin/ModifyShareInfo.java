/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ShareInfo;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyShareInfo extends ShareInfoHandler {

    private static final String[] OWNER_ACCOUNT_PATH = new String[] { AdminConstants.E_SHARE, AdminConstants.E_OWNER};
    protected String[] getProxiedAccountElementPath()  { return OWNER_ACCOUNT_PATH; }
    
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);
        Provisioning prov = Provisioning.getInstance();
        
        // entry to modify the share info for 
        NamedEntry publishingOnEntry = getTargetEntry(zsc, request, prov);
        
        checkShareInfoRight(zsc, prov, publishingOnEntry);
        
        Element eShare = request.getElement(AdminConstants.E_SHARE);
        ShareInfo.Publishing.Action action = ShareInfo.Publishing.Action.fromString(eShare.getAttribute(AdminConstants.A_ACTION));
            
        String ownerAcctId = getOwner(zsc, eShare, prov, true).getId();
            
        Element eFolder = eShare.getElement(AdminConstants.E_FOLDER);
        String folderPath = eFolder.getAttribute(AdminConstants.A_PATH, null);
        String folderId = eFolder.getAttribute(AdminConstants.A_FOLDER, null);
        String folderIdOrPath = eFolder.getAttribute(AdminConstants.A_PATH_OR_ID, null);
            
        ShareInfo.Publishing si = null;
            
        if (folderPath != null) {
            ensureOtherFolderDescriptorsAreNotPresent(folderId, folderIdOrPath);
            si = new ShareInfo.Publishing(action, ownerAcctId, folderPath, Boolean.FALSE);
        } else if (folderId != null) {
            ensureOtherFolderDescriptorsAreNotPresent(folderPath, folderIdOrPath);
            si = new ShareInfo.Publishing(action, ownerAcctId, folderId, Boolean.TRUE);
        } else if (folderIdOrPath != null) {
            ensureOtherFolderDescriptorsAreNotPresent(folderPath, folderId);
            si = new ShareInfo.Publishing(action, ownerAcctId, folderIdOrPath, null);
        } else
            throw ServiceException.INVALID_REQUEST("missing folder descriptor", null);

        if (si.validateAndDiscoverGrants(octxt, publishingOnEntry))
            si.persist(prov, publishingOnEntry);
        
        Element response = zsc.createElement(AdminConstants.MODIFY_SHARE_INFO_RESPONSE);
        return response;
    }
    
    private void ensureOtherFolderDescriptorsAreNotPresent(String other1, String other2) throws ServiceException {
        if (other1 != null || other2 != null)
            throw ServiceException.INVALID_REQUEST("can only specify one of " + 
                                                   AdminConstants.A_PATH + " or " +
                                                   AdminConstants.A_FOLDER + 
                                                   AdminConstants.A_PATH_OR_ID, null);
    }
    
    private void checkShareInfoRight(ZimbraSoapContext zsc, Provisioning prov, NamedEntry publishingOnEntry) 
        throws ServiceException {
        
        if (publishingOnEntry instanceof Account) {
            Account acct = (Account)publishingOnEntry;
            
            if (acct.isCalendarResource()) {
                // need a CalendarResource instance for RightChecker
                CalendarResource resource = prov.get(CalendarResourceBy.id, acct.getId());
                checkCalendarResourceRight(zsc, resource, Admin.R_publishCalendarResourceShareInfo);
            } else
                checkAccountRight(zsc, acct, Admin.R_publishAccountShareInfo);
        } else if (publishingOnEntry instanceof DistributionList) {
            checkDistributionListRight(zsc, (DistributionList)publishingOnEntry, Admin.R_publishDistributionListShareInfo);
        }
    }
    
    @Override
    protected void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_publishAccountShareInfo);
        relatedRights.add(Admin.R_publishCalendarResourceShareInfo);
        relatedRights.add(Admin.R_publishDistributionListShareInfo);
    }
}
