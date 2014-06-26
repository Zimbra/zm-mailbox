/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.service.mail.WaitSetRequest;
import com.zimbra.soap.ZimbraSoapContext;

public class AdminWaitSetRequest extends AdminDocumentHandler {
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element response = zsc.createElement(AdminConstants.ADMIN_WAIT_SET_RESPONSE);
        return WaitSetRequest.staticHandle(request, context, response, true);
    }
    
    @Override
    public void preProxy(Element request, Map<String, Object> context) throws ServiceException {
        setProxyTimeout(WaitSetRequest.getTimeoutMillis(request, true) + 10 * Constants.MILLIS_PER_SECOND);
        super.preProxy(request, context);
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("If the waitset is on all accounts, " + AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
        notes.add("Otherwise, must be the owner of the specified waitset");
    }
}
