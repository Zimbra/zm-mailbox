/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on 2005. 1. 26.
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.db.DbStatus;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.CheckHealthResponse;

/**
 * @author jhahm
 */
public class CheckHealth extends AdminDocumentHandler {

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        if (needsAdminAuth(context)) {
            Server localServer = Provisioning.getInstance().getLocalServer();
            checkRight(lc, context, localServer, Admin.R_checkHealth);
        }

        boolean dir = Provisioning.getInstance().healthCheck();
        boolean db = DbStatus.healthCheck();
        boolean healthy = dir && db;

        return lc.jaxbToElement(new CheckHealthResponse(healthy));
    }

    public boolean needsAuth(Map<String, Object> context) {
        // Must return false to leave the auth decision entirely up to
        // needsAdminAuth().
    	return false;
    }

    /**
     * No auth required if client is localhost.  Otherwise, admin auth is
     * required.
     * @param context
     * @return
     */
    public boolean needsAdminAuth(Map<String, Object> context) {
        return !clientIsLocal(context);
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_checkHealth);
        notes.add("The " + Admin.R_checkHealth.getName() + " is needed " +
                "only when the client making the SOAP request is localhost.");
    }
}
