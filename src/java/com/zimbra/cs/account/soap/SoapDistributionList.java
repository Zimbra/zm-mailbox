/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.soap;

import java.util.ArrayList;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.common.soap.Element.XMLElement;

class SoapDistributionList extends DistributionList implements SoapEntry {

    SoapDistributionList(String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(name, id, attrs, prov);
    }

    SoapDistributionList(Element e, Provisioning prov) throws ServiceException {
        super(e.getAttribute(AdminConstants.A_NAME), e.getAttribute(AdminConstants.A_ID), SoapProvisioning.getAttrs(e), prov);
        addDlm(e, getRawAttrs());
    }

    private void addDlm(Element e, Map<String, Object> attrs) {
        ArrayList<String> list = new ArrayList<String>();
        for (Element dlm : e.listElements(AdminConstants.E_DLM)) {
            list.add(dlm.getText());
        }
        attrs.put(Provisioning.A_zimbraMailForwardingAddress, list.toArray(new String[list.size()]));
    }

    public void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        XMLElement req = new XMLElement(AdminConstants.MODIFY_DISTRIBUTION_LIST_REQUEST);
        req.addElement(AdminConstants.E_ID).setText(getId());
        SoapProvisioning.addAttrElements(req, attrs);
        Element dl = prov.invoke(req).getElement(AdminConstants.E_DL);
        Map<String, Object> newAttrs = SoapProvisioning.getAttrs(dl);        
        addDlm(dl, newAttrs);
        setAttrs(newAttrs);        
    }

    public void reload(SoapProvisioning prov) throws ServiceException {
        XMLElement req = new XMLElement(AdminConstants.GET_DISTRIBUTION_LIST_REQUEST);
        Element a = req.addElement(AdminConstants.E_DL);
        a.setText(getId());
        a.addAttribute(AdminConstants.A_BY, DistributionListBy.id.name());
        Element dl = prov.invoke(req).getElement(AdminConstants.E_DL);
        Map<String, Object> attrs = SoapProvisioning.getAttrs(dl);
        addDlm(dl, attrs);                
        setAttrs(attrs);
    }
}
