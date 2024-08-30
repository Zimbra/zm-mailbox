/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.RightCommand;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.FlushCacheRequest;
import com.zimbra.soap.admin.message.RevokeRightRequest;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.CacheSelector;
import com.zimbra.soap.admin.type.EffectiveRightsTargetSelector;

public class RevokeRight extends RightDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        RevokeRightRequest rrReq = zsc.elementToJaxb(request);
        RightModifier rightModifier = GrantRight.getRightModifier(rrReq.getRight());

        // right checking is done in RightCommand

        RightCommand.revokeRight(Provisioning.getInstance(), getAuthenticatedAccount(zsc), rrReq.getTarget(),
                                rrReq.getGrantee(), rrReq.getRight().getValue(), rightModifier);
        EffectiveRightsTargetSelector erTargSel = rrReq.getTarget();
        if (com.zimbra.soap.type.TargetType.global == erTargSel.getType()) {
            CacheSelector cacheSel = new CacheSelector(true /* allServers */, CacheEntryType.globalgrant.toString());
            FlushCacheRequest fcReq = new FlushCacheRequest(cacheSel);
            try {
                FlushCache.doFlushCache(this, context, fcReq);
            } catch (ServiceException se) {
                ZimbraLog.acl.warn("Problem flushing acl cache for global %s/%s after revoking rights",
                        erTargSel.getBy(), erTargSel.getValue(), se);
            }
        }

        Element response = zsc.createElement(AdminConstants.REVOKE_RIGHT_RESPONSE);
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("Grantor must have the same or more rights on the same target or on a larger target set.");
    }
}
