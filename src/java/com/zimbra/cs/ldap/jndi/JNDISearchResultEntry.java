package com.zimbra.cs.ldap.jndi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapTODO.TODO;
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZSearchResultEntry;

public class JNDISearchResultEntry extends ZSearchResultEntry {

    SearchResult wrapped;
    JNDIAttributes zAttributes;
    
    JNDISearchResultEntry(SearchResult searchResult) {
        wrapped = searchResult;
        zAttributes = new JNDIAttributes(wrapped.getAttributes());
    }

    /*
    private Attributes getNativeAttributes() {
        return wrapped.getAttributes();
    }
    */
    
    @Override
    @TODO
    public void debug() {
        println(wrapped.toString());
    }

    @Override
    public ZAttributes getAttributes() {
        return zAttributes;
    }

    @Override
    public String getDN() {
        return wrapped.getNameInNamespace();
    }

}
