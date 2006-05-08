/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.localconfig;

import java.util.HashMap;
import java.util.Map;

public class KnownKey {

    private static final Map mKnownKeys = new HashMap();

    static {
        // Since all the known keys are actually defined
        // in another class, we need to make sure that
        // class' static initializer is run.
        LC.init();
    }

    static String[] getAll() {
        return (String[])mKnownKeys.keySet().toArray(new String[0]);
    }

    static boolean isKnown(String key) {
        return mKnownKeys.containsKey(key);
    }
    
    static String getDoc(String key) {
        KnownKey kk = (KnownKey)mKnownKeys.get(key);
        if (kk == null) {
            return null;
        }
        return kk.mDoc;
    }
    
    static String getDefaultValue(String key) {
        KnownKey kk = (KnownKey)mKnownKeys.get(key);
        if (kk == null) {
            return null;
        }
        return kk.mDefaultValue;
    }
    
    static boolean needForceToEdit(String key) {
        KnownKey kk = (KnownKey)mKnownKeys.get(key);
        if (kk == null) {
            return false;
        }
        return kk.mForceToEdit;
    }
    
    /*
     * Instance stuff.
     */
    
    private final String mKey;
    private String mDoc;
    private String mDefaultValue;
    private boolean mForceToEdit;
    
    /**
     * The only public method here.  If you have a KnownKey object, this
     * is a shortcut to get it's value.
     * 
     * @see LC.get()
     */
    public String value() {
        return LC.get(mKey);
    }

    public boolean booleanValue() {
        String s = LC.get(mKey);
        if (s == null || s.length() == 0) {
            throw new IllegalStateException("'" + mKey + "' is not defined in LocalConfig");
        }
        return Boolean.valueOf(s).booleanValue();
    }
    
    public int intValue() {
        String s = LC.get(mKey);
        if (s == null || s.length() == 0) {
            throw new IllegalStateException("'" + mKey + "' is not defined in LocalConfig");
        }
        return Integer.parseInt(s);
    }
    
    public long longValue() {
        String s = LC.get(mKey);
        if (s == null || s.length() == 0) {
            throw new IllegalStateException("'" + mKey + "' is not defined in LocalConfig");
        }
        return Long.parseLong(s);
    }
    
    public String key() {
    	return mKey;   
    }

    public KnownKey(String key, String defaultValue, String doc) {
        mKey = key;
        if (mKnownKeys.containsKey(key)) {
            Logging.warn("programming error - known key added more than once: " + key);
        }
        mKnownKeys.put(key, this);
        mDefaultValue = defaultValue;
        mDoc = doc;
    }

    public KnownKey(String key) 
    {
        mKey = key;
        if (mKnownKeys.containsKey(key)) {
            Logging.warn("programming error - known key added more than once: " + key);
        }
        mKnownKeys.put(key, this);
    }
    
    public void setDoc(String doc) {
        mDoc = doc;
    }
    
    public void setDefault(String defaultValue) {
        mDefaultValue = defaultValue;
    }
    
    public void setForceToEdit(boolean value) {
        mForceToEdit = value;
    }
}
