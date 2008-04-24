/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;

/**
 * @author schemers
 */
public abstract class AttributeCallback {

    /**
     * called before an attribute is modified. If a ServiceException is thrown, no attributes will
     * be modified. The attrsToModify map should not be modified, other then for the current attrName
     * being called.
     * 
     * TODO: if dn/name/type is needed on a create (for whatever reason), we could consider passing
     * them in context with well-known-keys, or having separate *Create callbacks.
     * 
     * @param context place to stash data between invocations of pre/post
     * @param attrName name of the attribute being modified so the callback can be used with multiple attributes.
     * @param attrValue will be null, String, or String[]
     * @param attrsToModify a map of all the attributes being modified
     * @param entry entry object being modified. null if entry is being created.
     * @param isCreate set to true if called during create
     * @throws ServiceException causes the whole transaction to abort.
     */
    public abstract void preModify(
            Map context,
            String attrName,
            Object attrValue,
            Map attrsToModify,
            Entry entry,
            boolean isCreate) throws ServiceException;

    /**
     * called after a successful modify of the attributes. should not throw any exceptions.
     * 
     * @param context
     * @param attrName
     * @param entry Set on modify and create.
     * @param isCreate set to true if called during create
     */
    public abstract void postModify(
            Map context,
            String attrName,
            Entry entry,
            boolean isCreate);
    
    
    protected static class SingleValueMod {
        
        static enum Mod {
            SETTING,
            UNSETTING
        }
        
        Mod mMod;
        String mValue;
        
        public boolean setting() { return mMod==Mod.SETTING; }
        public boolean unsetting() { return mMod==Mod.UNSETTING; }
        public String value() { return mValue; }
    }
    
    protected static class MultiValueMod {
        
        static enum Mod {
            ADDING,
            REMOVING,  // removing some values
            REPLACING,
            DELETING   // removing all values
        }
        
        Mod mMod;
        List<String> mValues;
        
        public boolean adding() { return mMod==Mod.ADDING; }
        public boolean removing() { return mMod==Mod.REMOVING; }
        public boolean replacing() { return mMod==Mod.REPLACING; }
        public boolean deleting() { return mMod==Mod.DELETING; }
        public List<String> values() { return mValues; }
        public Set<String> valuesSet() { return new HashSet<String>(mValues); }
    }
    
    protected SingleValueMod singleValueMod(String attrName, Object value)  throws ServiceException {
        SingleValueMod svm = new SingleValueMod();
        if (value == null)
            svm.mMod = SingleValueMod.Mod.UNSETTING;
        else if (!(value instanceof String))
            throw ServiceException.INVALID_REQUEST(attrName + " is a single-valued attribute", null);
        else {
            String s = (String)value;
            if ("".equals(s))
                svm.mMod = SingleValueMod.Mod.UNSETTING;
            else {
                svm.mMod = SingleValueMod.Mod.SETTING;
                svm.mValue = s;
            }
        }
        return svm;
    }
    
    /**
     * 
     * @param attrsToModify
     * @param attrName
     * @return how the attribute named attrName is being modified, returns null if it it not being modified.
     *         Note, this can be called to test other attributes in the modifyAttr, not only the attribute for 
     *         which the callback function is called.
     *         If attrName is the attr for which the callback is invoked (i.e. is the attr passed in to preModify),
     *         then multiValueMod should not return null.
     *         Null can be returned only when attrName is not the attr for which the preModify callback is called.
     *         
     * @throws ServiceException
     */
    protected MultiValueMod multiValueMod(Map attrsToModify, String attrName)  throws ServiceException {
        MultiValueMod mvm = new MultiValueMod();
        Object v = attrsToModify.get(attrName);
        if (v != null) {
            if ((v instanceof String) && ((String)v).length() == 0)
                mvm.mMod = MultiValueMod.Mod.DELETING;
            else
                mvm.mMod = MultiValueMod.Mod.REPLACING;
        } else if (attrsToModify.keySet().contains(attrName)) {
            // attrsToModify contains attrName, and the value is null
            mvm.mMod = MultiValueMod.Mod.DELETING;
        }           
           
        if (v == null) {
            v = attrsToModify.get("+" + attrName);
            if (v != null)
                mvm.mMod = MultiValueMod.Mod.ADDING;
        }

        if (v == null) {
            v = attrsToModify.get("-" + attrName);
            if (v != null)
                mvm.mMod = MultiValueMod.Mod.REMOVING;
        }
                
        if (mvm.mMod != null && mvm.mMod != MultiValueMod.Mod.DELETING)
            mvm.mValues = getMultiValue(v);
        
        return (mvm.mMod == null)? null : mvm;
    }

    private List<String> getMultiValue(Object value) throws ServiceException {
        
        List<String> list = null;
        
        if (value instanceof String) {
            list = new ArrayList<String>(1);
            list.add((String)value);
        } else if (value instanceof String[]) {
            list = new ArrayList<String>(Arrays.asList((String[])value));
        } else if (value instanceof Collection) {
            list = new ArrayList<String>();
            for (Object o : (Collection)value)
                list.add(o.toString());
        } else
            throw ServiceException.INVALID_REQUEST("value not a String or String[]", null);

        return list;
    }
}
