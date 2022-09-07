/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.accesscontrol.ACLAccessManager;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.zimlet.ZimletPresence.Presence;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAdminExtensionZimlets extends AdminDocumentHandler  {

    public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
		
        Element response = zsc.createElement(AdminConstants.GET_ADMIN_EXTENSION_ZIMLETS_RESPONSE);
        Element zimlets = response.addUniqueElement(AccountConstants.E_ZIMLETS);
        doExtensionZimlets(zsc, context, zimlets);
        
        return response;
    }

	private void doExtensionZimlets(ZimbraSoapContext zsc, Map<String, Object> context, Element response) throws ServiceException {
        
        List<Zimlet> zimletsList = Provisioning.getInstance().listAllZimlets();
        zimletsList.removeIf(x -> AdminConstants.ZEXTRAS_PACKAGES_LIST.contains(x.getName()));
        Iterator<Zimlet> zimlets = zimletsList.iterator();
		while (zimlets.hasNext()) {
		    
		    Zimlet z = (Zimlet) zimlets.next();
		    
		    if (!hasRightsToList(zsc, z, Admin.R_listZimlet, Admin.R_getZimlet)) {
                continue;
            }

            if (z.isExtension()) {
                boolean include = true;
                if ("com_zimbra_delegatedadmin".equals(z.getName())) {
                    include = (AccessManager.getInstance() instanceof ACLAccessManager);
                }

                if (include) {
                    ZimletUtil.listZimlet(response, z, -1, Presence.enabled);
                    // admin zimlets are all enabled
                }
            }
        }
    }
	
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_listZimlet);
        relatedRights.add(Admin.R_getZimlet);
        
        notes.add("Only zimlets on which the authed admin has effective " + 
                  Admin.R_listZimlet.getName() + " and " + Admin.R_getZimlet.getName() + 
                  " rights will appear in the response.");
        
        notes.add("e.g. there are zimlet1, zimlet2, zimlet3, if an admin has effective " + 
                  Admin.R_listZimlet.getName() + " and " + Admin.R_getZimlet.getName() +  
                  " rights on zimlet1, zimlet2, " + 
                  "then only zimlet1, zimlet2 will appear in the GetAdminExtensionZimletsResponse.  " + 
                  "The GetAdminExtensionZimletsRequest itself will not get PERM_DENIED.");
    }
	
}
