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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.soap;

import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.Provisioning.GAL_SEARCH_TYPE;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.soap.Element;
import com.zimbra.soap.Element.XMLElement;

public class SoapDomain extends SoapNamedEntry implements Domain {

    public SoapDomain(String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs);
    }

    public SoapDomain(Element e) throws ServiceException {
        super(e);
    }

    @Override
    public void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        XMLElement req = new XMLElement(AdminService.MODIFY_DOMAIN_REQUEST);
        req.addElement(AdminService.E_ID).setText(getId());
        prov.addAttrElements(req, attrs);
        mAttrs = (new SoapDomain(prov.invoke(req).getElement(AdminService.E_DOMAIN))).mAttrs;
        resetData();        
    }

    @Override
    public void reload(SoapProvisioning prov) throws ServiceException {
        mAttrs = ((SoapDomain) prov.getDomainById(getId())).mAttrs;
        resetData();        
    }

    public SearchGalResult autoCompleteGal(String query, GAL_SEARCH_TYPE type, int limit) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public List getAllAccounts() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public void getAllAccounts(Visitor visitor) throws ServiceException {
        // TODO Auto-generated method stub
        
    }

    public List getAllCalendarResources() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public void getAllCalendarResources(Visitor visitor) throws ServiceException {
        // TODO Auto-generated method stub
        
    }

    public List getAllDistributionLists() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, Object> getAttrs(boolean applyConfig) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public List searchAccounts(String query, String[] returnAttrs, String sortAttr, boolean sortAscending, int flags) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public List searchCalendarResources(EntrySearchFilter filter, String[] returnAttrs, String sortAttr, boolean sortAscending) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public SearchGalResult searchGal(String query, GAL_SEARCH_TYPE type, String token) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }
}
