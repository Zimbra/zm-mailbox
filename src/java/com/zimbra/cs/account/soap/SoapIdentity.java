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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;

import java.util.Map;

class SoapIdentity extends Identity implements SoapEntry {
    
    SoapIdentity(Account acct, String name, String id,
            Map<String, Object> attrs, Provisioning prov) {
        super(acct, name, id, attrs, prov);
    }

    SoapIdentity(Account acct, com.zimbra.soap.account.type.Identity id,
                Provisioning prov)
    throws ServiceException {
        super(acct, id.getName(), id.getId(),
                id.getAttrsAsOldMultimap(), prov);
    }
    
    SoapIdentity(Account acct, Element e, Provisioning prov)
    throws ServiceException {
        super(acct, e.getAttribute(AccountConstants.A_NAME),
                e.getAttribute(AccountConstants.A_ID),
                SoapProvisioning.getAttrs(e, AccountConstants.A_NAME), prov);
    }
    
    public void modifyAttrs(SoapProvisioning prov,
            Map<String, ? extends Object> attrs, boolean checkImmutable)
    throws ServiceException {
        /*
        XMLElement req = new XMLElement(AccountService.MODIFY_IDENTITY_REQUEST);
        Element identity = req.addElement(AccountService.E_IDENTITY);
        identity.addAttribute(AccountService.A_NAME, getName());
        SoapProvisioning.addAttrElements(identity, attrs);
        setAttrs(SoapProvisioning.getAttrs(prov.invoke(req)));
        */
    }

    public void reload(SoapProvisioning prov) throws ServiceException {
        //XMLElement req = new XMLElement(AdminService.GET_ALL_CONFIG_REQUEST);
        //setAttrs(SoapProvisioning.getAttrs(prov.invoke(req)));
    }
    
    public Account getAccount() throws ServiceException {
        throw ServiceException.INVALID_REQUEST(
                "unsupported, use getAccount(Provisioning)", null);
    }

}
