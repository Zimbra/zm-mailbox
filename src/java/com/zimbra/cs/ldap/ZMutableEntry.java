package com.zimbra.cs.ldap;

import java.util.Map;
import java.util.Set;

import com.zimbra.cs.ldap.LdapTODO.*;

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
     * Adds the provided attribute to this entry. 
     * If this entry already contains an attribute with the same name, 
     * then the current value will be *replace* by the new value.
     */
    @TODO
    public abstract void addAttr(String attrName, String value);
    
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
