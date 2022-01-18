/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.soap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.admin.type.Attr;
import com.zimbra.soap.admin.type.DLInfo;
import com.zimbra.soap.admin.type.DistributionListInfo;
import com.zimbra.soap.admin.type.DistributionListMembershipInfo;

public class SoapDynamicGroup extends DynamicGroup implements SoapEntry {
    
    List<String> membersList;

    SoapDynamicGroup(String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(name, id, attrs, prov);
    }

    /**
     * @param dlInfo contains information about a DL that another DL is
     *        a member of.
     * @throws ServiceException
     */
    SoapDynamicGroup(DistributionListMembershipInfo dlInfo,
            Provisioning prov)
    throws ServiceException {
        // DistributionListMembershipInfo does not supply attributes
        super(dlInfo.getName(), dlInfo.getId(),
                new HashMap<String,Object>(), prov);
        // DistributionListMembershipInfo does not supply membership info
        addDlm(new ArrayList<String>(), getRawAttrs());
    }

    SoapDynamicGroup(DistributionListInfo dlInfo, Provisioning prov)
    throws ServiceException {
        super(dlInfo.getName(), dlInfo.getId(), 
                Attr.collectionToMap(dlInfo.getAttrList()), prov);
        addDlm(dlInfo.getMembers(), getRawAttrs());
    }

    SoapDynamicGroup(DLInfo dlInfo, Provisioning prov)
    throws ServiceException {
        super(dlInfo.getName(), dlInfo.getId(), 
                Attr.collectionToMap(dlInfo.getAttrList()), prov);
        // DLInfo does not supply membership info
        addDlm(new ArrayList<String>(), getRawAttrs());
    }

    SoapDynamicGroup(Element e, Provisioning prov) throws ServiceException {
        super(e.getAttribute(AdminConstants.A_NAME), e.getAttribute(AdminConstants.A_ID), SoapProvisioning.getAttrs(e), prov);
        addDlm(e, getRawAttrs());
    }

    @Override
    public String[] getAllMembers() throws ServiceException {
        if (membersList == null) {
            return new String[0];
        } else {
            return getMultiAttr(Provisioning.A_member);
        }
    }
    
    @Override
    public Set<String> getAllMembersSet() throws ServiceException {
        if (membersList == null) {
            return Sets.newHashSet();
        } else {
            return Sets.newHashSet(membersList);
        }
    }
    
    private void addDlm(List <String> members, Map<String, Object> attrs) {
        /*
        attrs.put(Provisioning.A_member,
                members.toArray(new String[members.size()]));
        */
        setMembers(members);
    }
    
    private void setMembers(List <String> members) {
        membersList = members;
    }
    
    private void addDlm(Element e, Map<String, Object> attrs) {
        ArrayList<String> list = new ArrayList<String>();
        for (Element dlm : e.listElements(AdminConstants.E_DLM)) {
            list.add(dlm.getText());
        }
        addDlm(list, attrs);
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
        a.addAttribute(AdminConstants.A_BY, Key.DistributionListBy.id.name());
        Element dl = prov.invoke(req).getElement(AdminConstants.E_DL);
        Map<String, Object> attrs = SoapProvisioning.getAttrs(dl);
        addDlm(dl, attrs);                
        setAttrs(attrs);
    }
}
