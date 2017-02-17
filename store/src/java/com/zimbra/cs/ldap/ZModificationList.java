/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.cs.account.Entry;

public abstract class ZModificationList extends ZLdapElement {
    
    public void addAttr(String name, String value, Entry entry,
            boolean containsBinaryData, boolean isBinaryTransfer) {
        String[] val = new String[]{value};
        addAttr(name, val, entry, containsBinaryData, isBinaryTransfer);
    }
    
    public void removeAttr(String name, String value, Entry entry, 
            boolean containsBinaryData, boolean isBinaryTransfer) {
        String[] val = new String[]{value};
        removeAttr(name, val, entry, containsBinaryData, isBinaryTransfer);
    }
    
    public abstract boolean isEmpty();
    
    public abstract void addAttr(String name, String value[], Entry entry, 
            boolean containsBinaryData, boolean isBinaryTransfer);
    
    /**
     * If value is null or "", remove attribute, otherwise replace it.
     */
    public abstract void modifyAttr(String name, String value, Entry entry, 
            boolean containsBinaryData, boolean isBinaryTransfer);
    
    public abstract void modifyAttr(String name, String[] value, 
            boolean containsBinaryData, boolean isBinaryTransfer);
    
    public abstract void removeAttr(String attrName, boolean isBinaryTransfer);
    
    public abstract void removeAttr(String name, String value[], Entry entry, 
            boolean containsBinaryData, boolean isBinaryTransfer);
}
