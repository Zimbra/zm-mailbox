/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.soap.admin.message.GetAllConfigResponse;
import com.zimbra.soap.admin.message.GetConfigResponse;
import com.zimbra.soap.admin.type.Attr;

class SoapConfig extends Config implements SoapEntry {
    
    SoapConfig(Map<String, Object> attrs, Provisioning provisioning) {
        super(attrs, provisioning);
    }

    SoapConfig(GetAllConfigResponse resp, Provisioning provisioning)
    throws ServiceException {
        super(Attr.collectionToMap(resp.getAttrs()), provisioning);
    }
    
    SoapConfig(GetConfigResponse resp, Provisioning provisioning)
    throws ServiceException {
        super(Attr.collectionToMap(resp.getAttrs()), provisioning);
    }

    SoapConfig(Element e, Provisioning provisioning) throws ServiceException {
        super(SoapProvisioning.getAttrs(e), provisioning);
    }

    public void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        XMLElement req = new XMLElement(AdminConstants.MODIFY_CONFIG_REQUEST);
        SoapProvisioning.addAttrElements(req, attrs);
        setAttrs(SoapProvisioning.getAttrs(prov.invoke(req)));
    }

    public void reload(SoapProvisioning prov) throws ServiceException {
        XMLElement req = new XMLElement(AdminConstants.GET_ALL_CONFIG_REQUEST);
        setAttrs(SoapProvisioning.getAttrs(prov.invoke(req)));
    }
}
