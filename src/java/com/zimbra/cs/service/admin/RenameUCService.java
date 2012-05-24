/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.UCServiceBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.UCService;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

public class RenameUCService extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String id = request.getAttribute(AdminConstants.E_ID);
        String newName = request.getAttribute(AdminConstants.E_NEW_NAME);

        UCService ucService = prov.get(UCServiceBy.id, id);
        if (ucService == null) {
            throw AccountServiceException.NO_SUCH_UC_SERVICE(id);
        }

        // check if the admin can rename the uc service
        checkRight(lc, context, ucService, Admin.R_renameUCService);

        String oldName = ucService.getName();

        prov.renameUCService(id, newName);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] { "cmd", "RenameUCService", "name", oldName, "newName", newName }));

        // get again with new name...

        ucService = prov.get(Key.UCServiceBy.id, id);
        if (ucService == null) {
            throw ServiceException.FAILURE("unabled to get renamed uc service: " + id, null);
        }
        Element response = lc.createElement(AdminConstants.RENAME_UC_SERVICE_RESPONSE);
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_renameUCService);
    }

}
