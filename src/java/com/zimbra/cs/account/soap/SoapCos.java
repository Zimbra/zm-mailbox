/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.soap;

import java.util.Map;

import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.soap.Element;
import com.zimbra.soap.Element.XMLElement;

public class SoapCos extends SoapNamedEntry implements Cos {

    public SoapCos(String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs);
    }

    public SoapCos(Element e) throws ServiceException {
        super(e);
    }
    
    @Override
    public void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.MODIFY_COS_REQUEST);
        req.addElement(AdminService.E_ID).setText(getId());
        SoapProvisioning.addAttrElements(req, attrs);
        mAttrs = SoapProvisioning.getAttrs(prov.invoke(req).getElement(AdminService.E_COS));        
        resetData();        
    }

    @Override
    public void reload(SoapProvisioning prov) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_COS_REQUEST);
        Element a = req.addElement(AdminService.E_COS);
        a.setText(getId());
        a.addAttribute(AdminService.A_BY, CosBy.id.name());
        mAttrs = SoapProvisioning.getAttrs(prov.invoke(req).getElement(AdminService.E_COS));        
        resetData();
    }
    
}
