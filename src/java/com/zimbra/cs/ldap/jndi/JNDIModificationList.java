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
package com.zimbra.cs.ldap.jndi;

import java.util.ArrayList;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import com.unboundid.ldap.sdk.ModificationType;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.ZModificationList;

public class JNDIModificationList extends ZModificationList {

    private ArrayList<ModificationItem> modList = new ArrayList<ModificationItem>();
    
    JNDIModificationList() {
    }
    
    @Override
    public void debug() {
        // TODO Auto-generated method stub
        
    }
    
    ModificationItem[] getModListAsArray() {
        ModificationItem[] mods = new ModificationItem[modList.size()];
        return modList.toArray(mods);
    }
    

    @Override
    public boolean isEmpty() {
        return modList.size() == 0;
    }

    
    @Override
    public void addAttr(String name, String value[], Entry entry, 
            boolean containsBinaryData, boolean isBinaryTransfer) {
        String[] currentValues = entry.getMultiAttr(name, false);
        
        BasicAttribute ba = null;
        for (int i=0; i < value.length; i++) {
            if (LdapUtil.contains(currentValues, value[i])) {
                continue;
            }
            if (ba == null) {
                ba = JNDIUtil.newAttribute(isBinaryTransfer, name);
            }
            ba.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, value[i]));
        }
        if (ba != null) {
            modList.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, ba));
        }
    }
    
    @Override
    public void modifyAttr(String name, String value, Entry entry, 
            boolean containsBinaryData, boolean isBinaryTransfer) {
        int modOp = (StringUtil.isNullOrEmpty(value)) ? DirContext.REMOVE_ATTRIBUTE : DirContext.REPLACE_ATTRIBUTE;
        if (modOp == DirContext.REMOVE_ATTRIBUTE) {
            // make sure it exists
            if (entry.getAttr(name, false) == null) {
                return;
            }
        }
        
        if (modOp == DirContext.REMOVE_ATTRIBUTE) {
            removeAttr(name, isBinaryTransfer);
        } else {
            String[] val = new String[]{value};
            modifyAttr(name, val, containsBinaryData, isBinaryTransfer);
        }
    }
    
    @Override
    public void modifyAttr(String name, String[] value, 
            boolean containsBinaryData, boolean isBinaryTransfer) {
        BasicAttribute ba = JNDIUtil.newAttribute(isBinaryTransfer, name);
        for (int i=0; i < value.length; i++) {
            ba.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, value[i]));
        }
        modList.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, ba));
    }
    
    @Override
    public void removeAttr(String name, String value[], Entry entry, 
            boolean containsBinaryData, boolean isBinaryTransfer) {
        String[] currentValues = entry.getMultiAttr(name, false);
        if (currentValues == null || currentValues.length == 0) {
            return;
        }
        
        BasicAttribute ba = null;
        for (int i=0; i < value.length; i++) {
            if (!LdapUtil.contains(currentValues, value[i])) {
                continue;
            }
            if (ba == null) {
                ba = JNDIUtil.newAttribute(isBinaryTransfer, name);
            }
            ba.add(LdapUtil.decodeBase64IfBinary(containsBinaryData, value[i]));
        }
        if (ba != null) {
            modList.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, ba));
        }
    }
    

    @Override
    public void removeAttr(String attrName, boolean isBinaryTransfer) {
        BasicAttribute ba = JNDIUtil.newAttribute(isBinaryTransfer, attrName);
        modList.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, ba));
    }

}
