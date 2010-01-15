/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.zimlet.ZimletException;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class ModifyZimlet extends AdminDocumentHandler {

	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
		Element z = request.getElement(AdminConstants.E_ZIMLET);
		
		Element a = z.getOptionalElement(AdminConstants.E_ACL);
		if (a != null)
			doAcl(zsc, context, z);
		
		Element s = z.getOptionalElement(AdminConstants.E_STATUS);
		if (s != null)
			doStatus(zsc, context, z);
		
		Element p = z.getOptionalElement(AdminConstants.E_PRIORITY);
		if (p != null)
			doPriority(zsc, context, z);

	    Element response = zsc.createElement(AdminConstants.MODIFY_ZIMLET_RESPONSE);
		return response;
	}
	
    void doAcl(ZimbraSoapContext zsc, Map<String, Object> context, Element z) throws ServiceException {
	    String name = z.getAttribute(AdminConstants.A_NAME);
        Element a = z.getElement(AdminConstants.E_ACL);
        String cosName = a.getAttribute(AdminConstants.A_COS, null);
        if (cosName == null) return;
        
        Cos cos = Provisioning.getInstance().get(CosBy.name, cosName);
        if (cos == null)
            throw AccountServiceException.NO_SUCH_COS(cosName);
        checkRight(zsc, context, cos, Admin.R_manageZimlet);
        
        String acl = a.getAttribute(AdminConstants.A_ACL, null);
        if (acl == null)
        	throw ServiceException.INVALID_REQUEST("missing acl attribute", null);
		acl = acl.toLowerCase();
		try {
			if (acl.equals("grant")) {
				ZimletUtil.activateZimlet(name, cosName);
			} else if (acl.equals("deny")) {
				ZimletUtil.deactivateZimlet(name, cosName);
			} else {
				throw ServiceException.INVALID_REQUEST("invalid acl setting "+acl, null);
			}
		} catch (ZimletException ze) {
			throw ServiceException.FAILURE("cannot modify acl", ze);
		}
	}

    void doStatus(ZimbraSoapContext zsc, Map<String, Object> context, Element z) throws ServiceException {
	    String name = z.getAttribute(AdminConstants.A_NAME);
	    
	    Zimlet zimlet = Provisioning.getInstance().getZimlet(name);
        if (z == null)
            throw AccountServiceException.NO_SUCH_ZIMLET(name);
	    
        Element s = z.getElement(AdminConstants.E_STATUS);
        String val = s.getAttribute(AdminConstants.A_VALUE, null);
        if (val == null) return;
	    boolean status = val.equalsIgnoreCase("enabled");

	    Map<String, String> attrRightNeeded = new HashMap<String,String>();
	    attrRightNeeded.put(Provisioning.A_zimbraZimletEnabled, status ? Provisioning.TRUE : Provisioning.FALSE);
	    checkRight(zsc, context, zimlet, attrRightNeeded);
	    
		try {
			ZimletUtil.setZimletEnable(name, status);
		} catch (ZimletException ze) {
			throw ServiceException.FAILURE("cannot modify status", ze);
		}
	}

    void doPriority(ZimbraSoapContext zsc, Map<String, Object> context, Element z) throws ServiceException {
	    String name = z.getAttribute(AdminConstants.A_NAME);
	    
	    Zimlet zimlet = Provisioning.getInstance().getZimlet(name);
	    if (zimlet == null)
	        throw AccountServiceException.NO_SUCH_ZIMLET(name);
	        
        Element p = z.getElement(AdminConstants.E_PRIORITY);
        int val = (int)p.getAttributeLong(AdminConstants.A_VALUE, -1);
        if (val == -1) return;
        
        // ===========
        // check right
        //
        // need right to modify zimbraZimletPriority on *all* zimlets, because
        // all zimlets can be re-prioritized.
        Map<String, String> attrRightNeeded = new HashMap<String,String>();
        attrRightNeeded.put(Provisioning.A_zimbraZimletPriority, null); // yuck, pass null for the value
        
        List<Zimlet> allZimlets = Provisioning.getInstance().listAllZimlets();
        for (Zimlet zl : allZimlets) {
            checkRight(zsc, context, zl, attrRightNeeded);
        }
        //
        // end check right
        // ===============
        
		ZimletUtil.setPriority(name, val);
	}
    
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_manageZimlet);
        relatedRights.add(Admin.R_modifyZimlet);
        notes.add("For acl: needs " + Admin.R_manageZimlet.getName() + " on cos.");
        notes.add("For status: needs right to set " + Provisioning.A_zimbraZimletEnabled + " on the zimlet");
        notes.add("For priority: needs right to set " + Provisioning.A_zimbraZimletPriority + " on *all* zimlets, " +
                "because potentially the attribute can be modified on all zimlets.");
    }
}
