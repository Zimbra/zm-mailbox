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

import java.util.ArrayList;
import java.util.Map;

import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.soap.Element;
import com.zimbra.soap.Element.XMLElement;

class SoapDistributionList extends DistributionList implements SoapEntry {

    SoapDistributionList(String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs);
    }

    SoapDistributionList(Element e) throws ServiceException {
        super(e.getAttribute(AdminService.A_NAME), e.getAttribute(AdminService.A_ID), SoapProvisioning.getAttrs(e));
        addDlm(e, getRawAttrs());
    }

    private void addDlm(Element e, Map<String, Object> attrs) {
        ArrayList<String> list = new ArrayList<String>();
        for (Element dlm : e.listElements(AdminService.E_DLM)) {
            list.add(dlm.getText());
        }
        attrs.put(Provisioning.A_zimbraMailForwardingAddress, list.toArray(new String[list.size()]));
    }

    public void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.MODIFY_DISTRIBUTION_LIST_REQUEST);
        req.addElement(AdminService.E_ID).setText(getId());
        SoapProvisioning.addAttrElements(req, attrs);
        Element dl = prov.invoke(req).getElement(AdminService.E_DL);
        Map<String, Object> newAttrs = SoapProvisioning.getAttrs(dl);        
        addDlm(dl, newAttrs);
        setAttrs(newAttrs);        
    }

    public void reload(SoapProvisioning prov) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.GET_DISTRIBUTION_LIST_REQUEST);
        Element a = req.addElement(AdminService.E_DL);
        a.setText(getId());
        a.addAttribute(AdminService.A_BY, DistributionListBy.id.name());
        Element dl = prov.invoke(req).getElement(AdminService.E_DL);
        Map<String, Object> attrs = SoapProvisioning.getAttrs(dl);
        addDlm(dl, attrs);                
        setAttrs(attrs);
    }
}
