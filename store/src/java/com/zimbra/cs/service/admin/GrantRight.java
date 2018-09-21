/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.FlushCacheRequest;
import com.zimbra.soap.admin.message.GrantRightRequest;
import com.zimbra.soap.admin.type.CacheEntrySelector;
import com.zimbra.soap.admin.type.CacheEntrySelector.CacheEntryBy;
import com.zimbra.soap.admin.type.CacheEntryType;
import com.zimbra.soap.admin.type.CacheSelector;
import com.zimbra.soap.admin.type.EffectiveRightsTargetSelector;
import com.zimbra.soap.admin.type.RightModifierInfo;
import com.zimbra.soap.type.TargetBy;
import com.zimbra.soap.type.ZmBoolean;

public class GrantRight extends RightDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        GrantRightRequest grReq = zsc.elementToJaxb(request);
        RightModifierInfo modifierInfo = grReq.getRight();
        if (modifierInfo == null) {
            throw ServiceException.INVALID_REQUEST("No information specified on what right to assign", null);
        }
        RightModifier rightModifier = getRightModifier(modifierInfo);

        // right checking is done in RightCommand
        EffectiveRightsTargetSelector erTargSel = grReq.getTarget();
        RightCommand.grantRight(Provisioning.getInstance(), getAuthenticatedAccount(zsc), erTargSel,
                                grReq.getGrantee(), modifierInfo.getValue(), rightModifier);
        // Bug 100965 Avoid Cross server delegate admin being broken after initial creation due to stale caches
        if (com.zimbra.soap.type.TargetType.domain == erTargSel.getType()) {
            TargetBy by = erTargSel.getBy();
            if ((TargetBy.id == by) || (TargetBy.name == by)) {
                CacheSelector cacheSel = new CacheSelector(true /* allServers */, CacheEntryType.domain.toString());
                CacheEntrySelector ceSel = new CacheEntrySelector(
                        (TargetBy.id == erTargSel.getBy()) ? CacheEntryBy.id : CacheEntryBy.name, erTargSel.getValue());
                cacheSel.addEntry(ceSel);
                FlushCacheRequest fcReq = new FlushCacheRequest(cacheSel);
                try {
                    FlushCache.doFlushCache(this, context, fcReq);
                } catch (ServiceException se) {
                    ZimbraLog.acl.info("Problem flushing acl cache for domain %s/%s after granting rights",
                            erTargSel.getBy(), erTargSel.getValue(), se);
                }
            }
        }
        Element response = zsc.createElement(AdminConstants.GRANT_RIGHT_RESPONSE);
        return response;
    }

    static RightModifier getRightModifier(RightModifierInfo eRight) throws ServiceException {
        boolean deny = ZmBoolean.toBool(eRight.getDeny(), false);
        boolean canDelegate = ZmBoolean.toBool(eRight.getCanDelegate(), false);
        boolean disinheritSubGroups = ZmBoolean.toBool(eRight.getDisinheritSubGroups(), false);
        boolean subDomain = ZmBoolean.toBool(eRight.getSubDomain(), false);

        int numModifiers = 0;
        if (deny) {
            numModifiers++;
        }
        if (canDelegate) {
            numModifiers++;
        }
        if (disinheritSubGroups) {
            numModifiers++;
        }
        if (subDomain) {
            numModifiers++;
        }

        if (numModifiers > 1) {
            throw ServiceException.INVALID_REQUEST("can only have one modifier", null);
        }

        RightModifier rightModifier = null;
        if (deny) {
            rightModifier = RightModifier.RM_DENY;
        } else if (canDelegate) {
            rightModifier = RightModifier.RM_CAN_DELEGATE;
        } else if (disinheritSubGroups) {
            rightModifier = RightModifier.RM_DISINHERIT_SUB_GROUPS;
        } else if (subDomain) {
            rightModifier = RightModifier.RM_SUBDOMAIN;
        }
        return rightModifier;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("Grantor must have the same or more rights on the same target or " +
                "on a larger target set.");
    }

}
