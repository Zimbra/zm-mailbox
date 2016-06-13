/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.CosBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class CopyCos extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        String destCosName = request.getElement(AdminConstants.E_NAME).getText().toLowerCase();
        Element srcCosElem = request.getElement(AdminConstants.E_COS);
        String srcCosNameOrId = srcCosElem.getText();
        Key.CosBy srcCosBy = Key.CosBy.fromString(srcCosElem.getAttribute(AdminConstants.A_BY));
        
        Cos srcCos = prov.get(srcCosBy, srcCosNameOrId);
        if (srcCos == null)
            throw AccountServiceException.NO_SUCH_COS(srcCosNameOrId);
        
        checkRight(lc, context, null, Admin.R_createCos);
        checkRight(lc, context, srcCos, Admin.R_getCos);
        
        Cos cos = prov.copyCos(srcCos.getId(), destCosName);
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "CopyCos","name", destCosName, "cos", srcCosNameOrId}));         

        Element response = lc.createElement(AdminConstants.COPY_COS_RESPONSE);
        GetCos.encodeCos(response, cos);

        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_createCos);
        relatedRights.add(Admin.R_getCos);
        notes.add("Need the " + Admin.R_getCos.getName() + " right on the source cos.");
    }
}