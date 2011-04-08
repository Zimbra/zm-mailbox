package com.zimbra.cs.ldap.jndi;

import javax.naming.directory.SearchResult;

import com.zimbra.cs.ldap.LdapTODO.TODO;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZSearchResultEntry;

public class JNDISearchResultEntry extends ZSearchResultEntry {

    SearchResult wrapped;
    JNDIAttributes zAttributes;
    
    JNDISearchResultEntry(SearchResult searchResult) {
        wrapped = searchResult;
        zAttributes = new JNDIAttributes(wrapped.getAttributes());
    }
    
    @Override
    public ZAttributes getAttributes() {
        return zAttributes;
    }

    @Override
    public String getDN() {
        return wrapped.getNameInNamespace();
    }

    @Override
    @TODO
    public void debug() {
        println(wrapped.toString());
    }

}
