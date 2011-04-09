package com.zimbra.cs.ldap;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ZSearchResultEntry extends ZLdapElement {

    public abstract String getDN();
    
    public abstract ZAttributes getAttributes();
    
    /**
     * Retrieves the value for this attribute as a string. 
     * If this attribute has multiple values, then the first value will be returned.
     */
    public abstract String getAttrString(String attrName, boolean containsBinaryData) throws LdapException;
    
    /**
     * Retrieves the values for this attribute as a list of strings.
     * 
     * Note: This method does not handle binary data.
     */
    public abstract List<String> getMultiAttrString(String attrName) throws LdapException;
    
    public abstract Map<String, Object> getAttrs(Set<String> binaryAttrs) throws LdapException;
}
