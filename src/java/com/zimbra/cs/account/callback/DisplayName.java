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

package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProv;
 
public class DisplayName extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) 
    throws ServiceException {

        if (!((entry instanceof Account)||(entry instanceof DistributionList))) return;
        
        String displayName;
        
        // update cn only if we are not unsetting display name(cn is required for ZIMBRA_DEFAULT_PERSON_OC)
        SingleValueMod mod = singleValueMod(attrName, value);
        if (mod.unsetting())
            return;
        else
            displayName = mod.value();
        
        String namingRdnAttr = null;
        Provisioning prov = Provisioning.getInstance();
        if (prov instanceof LdapProv) {
            namingRdnAttr = ((LdapProv) prov).getDIT().getNamingRdnAttr(entry);
        }
        
        // update cn only if it is not the naming attr
        if (namingRdnAttr == null ||   // non LdapProvisioning, pass thru
            !namingRdnAttr.equals(Provisioning.A_cn)) {
            if (!attrsToModify.containsKey(Provisioning.A_cn)) {
                attrsToModify.put(Provisioning.A_cn, displayName);
            }
        }

    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
