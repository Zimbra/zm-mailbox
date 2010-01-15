/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class GetZimletStatus extends AdminDocumentHandler {

	@Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
		Provisioning prov = Provisioning.getInstance();
		
        Element response = zsc.createElement(AdminConstants.GET_ZIMLET_STATUS_RESPONSE);
        Element elem = response.addElement(AccountConstants.E_ZIMLETS);
		@SuppressWarnings({"unchecked"})
		List<Zimlet> zimlets = prov.listAllZimlets();
    	zimlets = ZimletUtil.orderZimletsByPriority(zimlets);
    	int priority = 0;
		for (Zimlet z : zimlets) {
		    // check if the zimlet can be listed
		    if (!hasRightsToList(zsc, z, Admin.R_listZimlet, null))
		        continue;
		    
			doZimlet(zsc, context, z, elem, priority++);
		}
        
		Iterator<Cos> cos = prov.getAllCos().iterator();
		
		while (cos.hasNext()) {
		    Cos c = (Cos) cos.next();
		    
		    // check if the cos can be listed 
		    if (!hasRightsToListCos(zsc, c, Admin.R_listCos, needGetAttrsRight()))
		        continue;
			
			elem = response.addElement(AdminConstants.E_COS);
			elem.addAttribute(AdminConstants.E_NAME, c.getName());
			String[] z = ZimletUtil.getAvailableZimlets(c).getZimletNamesAsArray();
			for (int i = 0; i < z.length; i++) {
			    doZimlet(zsc, context, prov.getZimlet(z[i]), elem, -1);
			}
		}
        return response;
    }

	private void doZimlet(ZimbraSoapContext zsc, Map<String, Object> context,
	        Zimlet z, Element elem, int priority) throws ServiceException {
		if (z == null)
			return;
		
		// skip if no get right
		try {
            checkRight(zsc, context, z, Admin.R_getZimlet);
        } catch (ServiceException e) {
            if (ServiceException.PERM_DENIED.equals(e.getCode()))
                return;
            else
                throw e;
        }
        
        Element zim = elem.addElement(AccountConstants.E_ZIMLET);
		zim.addAttribute(AdminConstants.A_NAME, z.getName());
		zim.addAttribute(AdminConstants.A_STATUS, (z.isEnabled() ? "enabled" : "disabled"));
		zim.addAttribute(AdminConstants.A_EXTENSION, (z.isExtension() ? "true" : "false"));
		if (priority >= 0) {
			zim.addAttribute(AdminConstants.A_PRIORITY, priority);
		}
    }
	
    private Set<String> needGetAttrsRight() {
        Set<String> attrsNeeded = new HashSet<String>();
        attrsNeeded.add(Provisioning.A_zimbraZimletAvailableZimlets);
        return attrsNeeded;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_listZimlet);
        relatedRights.add(Admin.R_listCos);
        relatedRights.add(Admin.R_getZimlet);
        
        notes.add("Only zimlets on which the authed admin has effective " + 
                  Admin.R_listZimlet.getName() + " and " + Admin.R_getZimlet.getName() + 
                  " rights will appear in <zimlets> in the response.");
        
        notes.add("Only zimlets on which the authed admin has effective " + 
                  Admin.R_listCos.getName() +
                  " right will be appear in <cos> in the response.");
      
        notes.add("e.g. there are zimlet1, zimlet2, zimlet3 and cos1, cos2 , if an admin has effective " + 
                  Admin.R_listZimlet.getName() + " and " + Admin.R_getZimlet.getName() +  
                  " rights on zimlet1, zimlet2, " + 
                  "then only zimlet1, zimlet2 will appear in <zimlets> in GetZimletStatusResponse, " + 
                  "and only cos1 will appear in <cos> in the resposne.  " +
                  "The GetZimletStatusRequest itself will not get PERM_DENIED.");
    }
}
