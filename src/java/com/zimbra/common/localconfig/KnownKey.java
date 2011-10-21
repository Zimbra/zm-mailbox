/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import com.google.common.base.Strings;
import com.zimbra.common.util.L10nUtil;

public final class KnownKey {

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
    public static KnownKey newKey(String defaultValue) {
        return new KnownKey().setDefault(defaultValue);
    }

    /**
     * Factory method with boolean default value.
     *
     * @param defaultValue default value
     * @return new instance
     */
    public static KnownKey newKey(boolean defaultValue) {
        return new KnownKey().setDefault(String.valueOf(defaultValue));
    }

    /**
     * Factory method with integer default value.
     *
     * @param defaultValue default value
     * @return new instance
     */
    public static KnownKey newKey(int defaultValue) {
        return new KnownKey().setDefault(String.valueOf(defaultValue));
    }

    /**
     * Factory method with long default value.
     *
     * @param defaultValue default value
     * @return new instance
     */
    public static KnownKey newKey(long defaultValue) {
        return new KnownKey().setDefault(String.valueOf(defaultValue));
    }

    /**
     * Factory method with float default value.
     *
     * @param defaultValue default value
     * @return new instance
     */
    public static KnownKey newKey(float defaultValue) {
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
        return kk.defaultValue;
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
        if (kk.value == null) {
            kk.expand(LocalConfig.getInstance());
        }
        return kk.value;
    }

    public static boolean needForceToEdit(String key) {
        KnownKey kk = ALL.get(key);
        if (kk == null) {
            return false;
        }
        return kk.forceToEdit;
    }

    private String key;
    private String defaultValue;
    private String value; //cached value after expansion
    private boolean forceToEdit;
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
        assert key != null;
        return LC.get(key);
    }

    public boolean booleanValue() {
        assert key != null;
        String s = LC.get(key);
        if (Strings.isNullOrEmpty(s)) {
            s = defaultValue;  // fallback to the default value
        }
        return Boolean.valueOf(s).booleanValue();
    }

    public int intValue() {
        assert key != null;
        String s = LC.get(key);
        if (Strings.isNullOrEmpty(s)) {
            s = defaultValue; // fallback to the default value
        }
        return Integer.parseInt(s);
    }

    /**
     * Returns the value of this KnownKey as an int, but forces it to be within
     * the range of minValue <= RETURN <= maxValue
     */
    public int intValueWithinRange(int minValue, int maxValue) {
        int result = intValue();
        if (result < minValue) {
            return minValue;
        } else if (result > maxValue) {
            return maxValue;
        } else {
            return result;
        }
    }

    public long longValue() {
        assert key != null;
        String s = LC.get(key);
        if (Strings.isNullOrEmpty(s)) {
            s = defaultValue; // fallback to the default value
        }
        return Long.parseLong(s);
    }

    /**
     * Returns the value of this KnownKey as a long, but forces it to be within
     * the range of minValue <= RETURN <= maxValue
     */
    public long longValueWithinRange(long minValue, long maxValue) {
        long result = longValue();
        if (result < minValue) {
            return minValue;
        } else if (result > maxValue) {
            return maxValue;
        } else {
            return result;
        }
    }

    public String key() {
        return key;
    }

    public void setKey(String name) {
        assert key == null : name;
        assert !ALL.containsKey(name) : name;
        key = name;
        ALL.put(name, this);
    }

    public String doc() {
        return doc(null);
    }

    public String doc(Locale locale) {
        return L10nUtil.getMessage(key, locale);
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
        this.key = key;
        if (ALL.containsKey(key)) {
            assert false : "duplicate key: " + key;
        }
        setDefault(defaultValue);
        ALL.put(key, this);
    }


    public KnownKey setDefault(String defaultValue) {
        this.defaultValue = defaultValue;
        this.value = null;
        return this;
    }

    public KnownKey setDefault(long defaultValue) {
        return setDefault(String.valueOf(defaultValue));
    }

    public KnownKey setForceToEdit(boolean value) {
        forceToEdit = value;
        return this;
    }

    KnownKey protect() {
        forceToEdit = true;
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
            value = lc.expand(key, defaultValue);
        } catch (ConfigException e) {
            Logging.error("Can't expand config key " + key + "=" + defaultValue, e);
            throw e;
        }
    }

}
