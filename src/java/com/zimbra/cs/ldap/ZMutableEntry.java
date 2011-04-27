package com.zimbra.cs.ldap;

import java.util.Map;
import java.util.Set;

/**
 * Represents a transient, mutable entry in memory.
 * 
 * This class should be used for constructing an entry that is to be created in LDAP.
 *  
 * @author pshao
 *
 */
public abstract class ZMutableEntry extends ZEntry {

    /**
     * Sets the provided attribute to this entry. 
     * If this entry already contains an attribute with the same name, 
     * then the current value will be *replaced* by the new value.
     */
    public abstract void setAttr(String attrName, String value);
    
    /**
     * Adds the provided attribute to this entry. 
     * If this entry already contains an attribute with the same name, 
     * then their values will be *merged*.
     */
    public abstract void addAttr(String attrName, Set<String> values);
    
    public abstract String getAttrString(String attrName) throws LdapException;
    
    public abstract boolean hasAttribute(String attrName);

    
    /**
     * take a map (key = String, value = String | String[]) and populate ZAttributes.
     */
    public abstract void mapToAttrs(Map<String, Object> mapAttrs);
    
    public abstract void setDN(String dn);
    
}
