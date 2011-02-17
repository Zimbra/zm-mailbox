/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.common.localconfig;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.zimbra.common.util.L10nUtil;

public class KnownKey {

    private static final Map<String, KnownKey> ALL = new LinkedHashMap<String, KnownKey>();

    static {
        // Since all the known keys are actually defined in another class, we
        // need to make sure that class' static initializer is run.
        LC.init();
    }

    /**
     * Factory method with string default value.
     *
     * @param defaultValue default value
     * @return new instance
     */
    static KnownKey newKey(String defaultValue) {
        return new KnownKey().setDefault(defaultValue);
    }

    /**
     * Factory method with boolean default value.
     *
     * @param defaultValue default value
     * @return new instance
     */
    static KnownKey newKey(boolean defaultValue) {
        return new KnownKey().setDefault(String.valueOf(defaultValue));
    }

    /**
     * Factory method with integer default value.
     *
     * @param defaultValue default value
     * @return new instance
     */
    static KnownKey newKey(int defaultValue) {
        return new KnownKey().setDefault(String.valueOf(defaultValue));
    }

    /**
     * Factory method with long default value.
     *
     * @param defaultValue default value
     * @return new instance
     */
    static KnownKey newKey(long defaultValue) {
        return new KnownKey().setDefault(String.valueOf(defaultValue));
    }

    /**
     * Factory method with float default value.
     *
     * @param defaultValue default value
     * @return new instance
     */
    static KnownKey newKey(float defaultValue) {
        return new KnownKey().setDefault(String.valueOf(defaultValue));
    }

    static String[] getAll() {
        return ALL.keySet().toArray(new String[0]);
    }

    static boolean isKnown(String key) {
        return ALL.containsKey(key);
    }

    static KnownKey get(String key) {
        return ALL.get(key);
    }

    static String getDefaultValue(String key) {
        KnownKey kk = ALL.get(key);
        if (kk == null) {
            return null;
        }
        return kk.mDefaultValue;
    }

    static void expandAll(LocalConfig lc) throws ConfigException {
        String[] keys = KnownKey.getAll();
        for (String key : keys) {
            KnownKey kk = ALL.get(key);
            kk.expand(lc);
        }
    }

    static String getValue(String key) throws ConfigException {
        KnownKey kk = ALL.get(key);
        if (kk == null) {
            return null;
        }
        if (kk.mValue == null) {
            kk.expand(LocalConfig.getInstance());
        }
        return kk.mValue;
    }

    public static boolean needForceToEdit(String key) {
        KnownKey kk = ALL.get(key);
        if (kk == null) {
            return false;
        }
        return kk.mForceToEdit;
    }

    private String mKey;
    private String mDefaultValue;
    private String mValue; //cached value after expansion
    private boolean mForceToEdit;
    private boolean reloadable = false;
    /* 
     * whether or not this is a 'supported' key (printing with zmlocalconfig -i)
     */
    private boolean supported = false;

    /**
     * The only public method here.  If you have a KnownKey object, this
     * is a shortcut to get it's value.
     *
     * @see LC#get
     */
    public String value() {
        assert mKey != null;
        return LC.get(mKey);
    }

    public boolean booleanValue() {
        assert mKey != null;
        String s = LC.get(mKey);
        if (s == null || s.length() == 0) {
            throw new IllegalStateException("'" + mKey + "' is not defined in LocalConfig");
        }
        return Boolean.valueOf(s).booleanValue();
    }

    public int intValue() {
        assert mKey != null;
        String s = LC.get(mKey);
        if (s == null || s.length() == 0) {
            throw new IllegalStateException("'" + mKey + "' is not defined in LocalConfig");
        }
        return Integer.parseInt(s);
    }

    /**
     * Returns the value of this KnownKey as an int, but forces it to be within
     * the range of minValue <= RETURN <= maxValue
     *
     * @param minValue
     * @param maxValue
     */
    public int intValueWithinRange(int minValue, int maxValue) {
        int toRet = intValue();
        if (toRet < minValue)
            toRet = minValue;
        if (toRet > maxValue)
            toRet = maxValue;
        return toRet;
    }

    public long longValue() {
        assert mKey != null;
        String s = LC.get(mKey);
        if (s == null || s.length() == 0) {
            throw new IllegalStateException("'" + mKey + "' is not defined in LocalConfig");
        }
        return Long.parseLong(s);
    }

    /**
     * Returns the value of this KnownKey as a long, but forces it to be within
     * the range of minValue <= RETURN <= maxValue
     *
     * @param minValue
     * @param maxValue
     */
    public long longValueWithinRange(long minValue, long maxValue) {
        long toRet = longValue();
        if (toRet < minValue)
            toRet = minValue;
        if (toRet > maxValue)
            toRet = maxValue;
        return toRet;
    }

    public String key() {
        return mKey;
    }

    void setKey(String name) {
        assert mKey == null : name;
        assert !ALL.containsKey(name) : name;
        mKey = name;
        ALL.put(name, this);
    }

    public String doc() {
        return doc(null);
    }

    public String doc(Locale locale) {
        return L10nUtil.getMessage(mKey, locale);
    }

    /**
     * You must call {@link #setKey(String)} before using this {@link KnownKey}.
     */
    KnownKey() {
    }

    public KnownKey(String key) {
        this(key, null);
    }

    

    public KnownKey(String key, String defaultValue) {
        mKey = key;
        if (ALL.containsKey(key)) {
            assert false : "duplicate key: " + key;
        }
        setDefault(defaultValue);
        ALL.put(key, this);
    }


    public KnownKey setDefault(String defaultValue) {
        mDefaultValue = defaultValue;
        mValue = null;
        return this;
    }

    public KnownKey setDefault(long defaultValue) {
        return setDefault(String.valueOf(defaultValue));
    }

    public KnownKey setForceToEdit(boolean value) {
        mForceToEdit = value;
        return this;
    }

    KnownKey protect() {
        mForceToEdit = true;
        return this;
    }

    /**
     * Mark this key as reloadable.
     * <p>
     * This is solely for documentation purpose. Developers are responsible for
     * providing accurate information. If it's reloadable, changes are in effect
     * after LC reload. Otherwise, changes are in effect after server restart.
     *
     */
    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public void setSupported(boolean supported){
        this.supported = supported;
    }

    /**
     * Whether or not the value of the key will be reloaded without a server restart
     * 
     */
    boolean isReloadable() {
        return reloadable;
    }
    /**
     * Whether or not to show the key for the -i option on the command line.
     */ 
    boolean isSupported() {
        return supported;
    }
    
    
    private void expand(LocalConfig lc) throws ConfigException {
        try {
            mValue = lc.expand(mKey, mDefaultValue);
        } catch (ConfigException x) {
            Logging.error("Can't expand config key " + mKey + "=" + mDefaultValue, x);
            throw x;
        }
    }

}
