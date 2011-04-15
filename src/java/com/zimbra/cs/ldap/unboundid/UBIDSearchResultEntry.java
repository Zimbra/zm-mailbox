package com.zimbra.cs.ldap.unboundid;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.SearchResultEntry;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZSearchResultEntry;

/**
 * Represents one LDAP entry in a search result.
 *
 */
public class UBIDSearchResultEntry extends ZSearchResultEntry  {

    private SearchResultEntry wrapped;
    private UBIDAttributes zAttributes;
    
    UBIDSearchResultEntry(SearchResultEntry searchResultEntry) {
        wrapped = searchResultEntry;
        zAttributes = new UBIDAttributes(wrapped);
    }

    @Override
    public void debug() {
        println(wrapped.toString());
    }
    
    @Override
    public ZAttributes getAttributes() {
        return zAttributes;
    }

    @Override
    public String getDN() {
        return wrapped.getDN();
    }

}
