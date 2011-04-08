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
    
    
    public abstract void addAttr(String name, String value[], Entry entry, 
            boolean containsBinaryData, boolean isBinaryTransfer);
    
    /**
     * If value is null or "", remove attribute, otherwise replace it.
     */
    public abstract void modifyAttr(String name, String value, Entry entry, 
            boolean containsBinaryData, boolean isBinaryTransfer);
    
    public abstract void modifyAttr(String name, String[] value, 
            boolean containsBinaryData, boolean isBinaryTransfer);
    
    public abstract void removeAttr(String attrName);
    
    public abstract void removeAttr(String name, String value[], Entry entry, 
            boolean containsBinaryData, boolean isBinaryTransfer);
}
