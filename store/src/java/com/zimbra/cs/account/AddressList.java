package com.zimbra.cs.account;

import java.util.Map;

/**
 * @author zimbra
 *
 */
public class AddressList extends NamedEntry {

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
}
