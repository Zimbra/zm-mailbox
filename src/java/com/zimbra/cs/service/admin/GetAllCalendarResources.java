/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.service.admin.GetAllAccounts.AccountVisitor;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author jhahm
 */
public class GetAllCalendarResources extends GetAllAccounts {

    protected QName getResponseQName() {
        return AdminConstants.GET_ALL_CALENDAR_RESOURCES_RESPONSE;
    }
    
    protected static class CalendarResourceVisitor extends AccountVisitor {
        CalendarResourceVisitor(ZimbraSoapContext zsc, AdminDocumentHandler handler, Element parent) 
        throws ServiceException {
            super(zsc, handler, parent);
        }
        
        public void visit(com.zimbra.cs.account.NamedEntry entry) throws ServiceException {
            if (mHandler.hasRightsToList(mZsc, entry, Admin.R_listCalendarResource, null))
                ToXML.encodeCalendarResource(mParent, (CalendarResource)entry, true, null, mAAC.getAttrRightChecker(entry));
        }
    }
    
    /*
     * server s is not used, need to use the same signature as GetAllAccounts.doDomain 
     * so the overridden doDomain is called.
     */
    protected void doDomain(ZimbraSoapContext zsc, final Element e, Domain d, Server s) throws ServiceException {
        CalendarResourceVisitor visitor = new CalendarResourceVisitor(zsc, this, e);
        Provisioning.getInstance().getAllCalendarResources(d, s, visitor);
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_listCalendarResource);
        relatedRights.add(Admin.R_getCalendarResource);
        
        notes.add(AdminRightCheckPoint.Notes.LIST_ENTRY);
    }
}
