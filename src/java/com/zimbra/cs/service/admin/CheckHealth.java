/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2005. 1. 26.
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbStatus;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;

/**
 * @author jhahm
 */
public class CheckHealth extends AdminDocumentHandler {

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element document, Map context)
            throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Element response = lc.createElement(AdminService.CHECK_HEALTH_RESPONSE);

        boolean dir = Provisioning.getInstance().healthCheck();
        boolean db = DbStatus.healthCheck();
        boolean healthy = dir && db;

        response.addAttribute(AdminService.A_HEALTHY, healthy);
        return response;
    }

    public boolean needsAuth(Map context) {
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
    public boolean needsAdminAuth(Map context) {
        return !clientIsLocal(context);
    }
}
