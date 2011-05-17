/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
