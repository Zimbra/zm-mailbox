/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.ldap;

import java.util.Map;
import java.util.Set;

/**
 * Represents a transient, mutable entry in memory.
 *  
 * @author pshao
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
    
    /**
     * Adds the provided attribute to this entry. 
     * If this entry already contains an attribute with the same name, 
     * then their values will be *merged*.
     */
    public abstract void addAttr(String attrName, String value);
    
    public abstract String getAttrString(String attrName) throws LdapException;
    
    public abstract boolean hasAttribute(String attrName);

    
    /**
     * take a map (key = String, value = String | String[]) and populate ZAttributes.
     */
    public abstract void mapToAttrs(Map<String, Object> mapAttrs);
    
    public abstract void setDN(String dn);
    
}
