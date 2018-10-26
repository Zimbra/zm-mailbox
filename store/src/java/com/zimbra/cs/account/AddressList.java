/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
package com.zimbra.cs.account;

import java.util.Map;

import com.zimbra.cs.account.ldap.entry.LdapEntry;

/**
 * @author zimbra
 *
 */
public class AddressList extends NamedEntry implements LdapEntry {

    private String mDn;

    /**
     * @param dn
     * @param name
     * @param id
     * @param attrs
     * @param defaults
     * @param prov
     */
    public AddressList(String dn, String name, String id, Map<String, Object> attrs,
        Map<String, Object> defaults, Provisioning prov) {
        this(name, id, attrs, defaults, prov);
        this.mDn = dn;
    }

    /**
     * @param name
     * @param id
     * @param attrs
     * @param defaults
     * @param prov
     */
    public AddressList(String name, String id, Map<String, Object> attrs,
                          Map<String, Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
        
    }

    @Override
    public EntryType getEntryType() {
        return EntryType.ADDRESS_LIST;
    }
    
    public boolean isActive() {
        return getBooleanAttr(Provisioning.A_zimbraIsAddressListActive, false);
    }

    public String getGalSearchQuery() {
        return getAttr(Provisioning.A_zimbraAddressListGalFilter);
    }
    public String getLdapSearchQuery() {
        return getAttr(Provisioning.A_zimbraAddressListLdapFilter);
    }
    
    public String getDisplayName() {
        return getAttr(Provisioning.A_displayName);
    }

    public String getDN() {
        return mDn;
    }
}
