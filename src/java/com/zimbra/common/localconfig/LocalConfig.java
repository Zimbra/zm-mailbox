/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.PrintStream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.zimbra.common.util.FileUtil;

public class LocalConfig {

    static final String E_LOCALCONFIG = "localconfig";
    static final String E_KEY = "key";
    static final String A_NAME = "name";
    static final String E_VALUE = "value";

    private String mConfigFile;

    private String defaultConfigFile() {
        String zimbra_config = System.getProperty("zimbra.config");
        if (zimbra_config == null) {
            final String FS = File.separator;
            zimbra_config = FS + "opt" + FS + "zimbra" + FS + "conf" + FS + "localconfig.xml";
        }
        return zimbra_config;
    }

    String getConfigFile() {
        return mConfigFile;
    }

    private Map<String, String> mConfiguredKeys = new HashMap<String, String>();
    private Map<String, String> mExpanded = new HashMap<String, String>();

    void set(String key, String value)
    {
        mConfiguredKeys.put(key, value);
    }

    String getRaw(String key)
    {
        if (mConfiguredKeys.containsKey(key))
            return mConfiguredKeys.get(key);

        if (KnownKey.isKnown(key))
            return KnownKey.getDefaultValue(key);

        return null;
    }

    private boolean expandOnce(StringBuffer value, Set<String> seenKeys) throws ConfigException {
        int begin = value.indexOf("${");
        if (begin == -1) {
            return false;
        }

        int end = value.indexOf("}", begin);
        if (end == -1) {
            return false;
        }

        String key = value.substring(begin + 2, end);

        if (seenKeys.contains(key)) {
            StringBuffer sb = new StringBuffer(128);
            sb.append("recursive expansion of key '" + key + "':");
            for (String seen : seenKeys)
                sb.append(" ").append(seen);
            throw new ConfigException(sb.toString());
        }

        seenKeys.add(key);

        String replacement = getRaw(key);
        if (replacement == null)
            throw new ConfigException("null valued key '" + key + "' referenced");
        value.replace(begin, end+1, replacement);

        return true;
    }

    String expand(String key, String rawValue) throws ConfigException {
        if (rawValue == null)
            return null;

        Set<String> seenKeys = new HashSet<String>();
        seenKeys.add(key);
        StringBuffer result = new StringBuffer(rawValue);
        while (expandOnce(result, seenKeys));
        return result.toString();
    }

    String get(String key) throws ConfigException {
        if (mExpanded.containsKey(key)) {
            return mExpanded.get(key);
        }
        if (KnownKey.isKnown(key)) {
            return KnownKey.getValue(key);
        }
        return null;
    }

    //
    // Load & save
    //
    void save() throws IOException, ConfigException {
        ConfigWriter xmlWriter = ConfigWriter.getInstance("xml", false, false);
        for (String key : mConfiguredKeys.keySet()) {
            String value = getRaw(key);
            xmlWriter.add(key, value);
        }

        File configFile = new File(mConfigFile);
        File directory = configFile.getCanonicalFile().getParentFile();
        File tempFile = File.createTempFile("localconfig.xml.", "", directory);
        FileWriter fileWriter = new FileWriter(tempFile);

        xmlWriter.write(fileWriter);
        fileWriter.close();
        configFile.delete();
        tempFile.renameTo(configFile);
    }

    void backup(String suffix) throws IOException {
    FileUtil.copy(new File(mConfigFile), new File(mConfigFile + suffix), true);
    }

    public LocalConfig(String file) throws DocumentException, ConfigException {
        mConfigFile = file;
        if (mConfigFile == null) {
            mConfigFile = defaultConfigFile();
        }

        File cf = new File(mConfigFile);
        if (cf.exists() && cf.canRead()) {
            SAXReader reader = new SAXReader();
            Document document = reader.read(cf);
            Element root = document.getRootElement();

            if (!root.getName().equals(E_LOCALCONFIG))
                throw new DocumentException("config file " + mConfigFile + " root tag is not " + E_LOCALCONFIG);

            for (Iterator<?> iter = root.elementIterator(E_KEY); iter.hasNext(); ) {
                Element ekey = (Element) iter.next();
                String key = ekey.attributeValue(A_NAME);
                String value = ekey.elementText(E_VALUE);
                set(key, value);
            }
        } else {
            Logging.warn("local config file `" + cf + "' is not readable");
        }
        expandAll();
    }

    boolean isSet(String key) {
        return mConfiguredKeys.containsKey(key) || KnownKey.isKnown(key);
    }

    void remove(String key) {
        mConfiguredKeys.remove(key);
    }

    //
    // Print
    //
    static void printDoc(PrintStream ps, String[] keys) {
        if (keys.length == 0) {
            keys = KnownKey.getAll();
            Arrays.sort(keys);
        }
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                ps.println();
            }
            String doc = KnownKey.getDoc(keys[i]);
            if (doc == null) {
                Logging.warn("'" + keys[i] + "' is not a known key");;
            } else {
                fmt(ps, keys[i] + ": " + doc, 60);
            }
        }
    }

    void printChanged(OutputStream out, ConfigWriter writer, String[] keys) throws ConfigException, IOException {
        if (keys.length == 0) {
            keys = mConfiguredKeys.keySet().toArray(new String[0]);
            Arrays.sort(keys);
        }
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            boolean add = true;
            if (KnownKey.isKnown(key)) {
                String configuredValue = get(key);
                String defaultValue = KnownKey.getDefaultValue(key);
                if (configuredValue.equals(defaultValue)) {
                    add = false;
                }
            }
            if (add) {
                String value = writer.expand() ? get(key) : getRaw(key);
                if (value == null) {
                    Logging.warn("null valued key '" + key + "'");
                } else {
                    writer.add(key, value);
                }
            }
        }
        writer.write(new OutputStreamWriter(out));
    }

    void printDefaults(OutputStream out, ConfigWriter writer, String[] keys) throws IOException, ConfigException
    {
        if (keys.length == 0) {
            keys = KnownKey.getAll();
            Arrays.sort(keys);
        }
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            if (!KnownKey.isKnown(key)) {
                Logging.warn("not a known key '" + key + "'");
            } else {
                String value = KnownKey.getDefaultValue(key);
                if (writer.expand()) {
                    value = expand(key, value);
                }
                writer.add(key, value);
            }
        }
        writer.write(new OutputStreamWriter(out));
    }

    /*
     * Return all keys - known or otherwise.
     */
    String[] allKeys() {
        Set<String> union = new HashSet<String>();

        // Add known keys.
        String[] knownKeys = KnownKey.getAll();
        for (int i = 0; i < knownKeys.length; i++)
            union.add(knownKeys[i]);

        // Add set keys (this might contain unknown keys)
        for (String key : mConfiguredKeys.keySet())
            union.add(key);

        return union.toArray(new String[0]);
    }

    void print(OutputStream out, ConfigWriter writer, String[] keys) throws IOException, ConfigException {
        if (keys.length == 0) {
            keys = allKeys();
            Arrays.sort(keys);
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                String value = writer.expand() ? get(key) : getRaw(key);
                writer.add(key, value);
            }
        } else {
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                String value = writer.expand() ? get(key) : getRaw(key);
                if (value == null) {
                    Logging.warn("null valued key '" + key + "'");
                } else {
                    writer.add(key, value);
                }
            }
        }
        writer.write(new OutputStreamWriter(out));
    }

    private static void fmt(PrintStream ps, String str, int limit) {
        String[] tokens  = str.split("\\s");
        int cols = 0;
        for (int x = 0; x < tokens.length; x++) {
            String tok = tokens[x];
            if ((tok.length() + cols) > limit) {
                cols = 0;
                ps.println();
            }
            if (tok.length() >= limit) {
                cols = 0;
                ps.println(tok);
            } else {
                if (cols != 0) {
                    ps.print(" ");
                }
                ps.print(tok);
                cols += tok.length() + 1;
            }
        }
        if (cols != 0) {
            ps.println();
        }
    }

    //
    // Expand and cache all keys
    //
    private void expandAll() throws ConfigException {
        String minimize = mConfiguredKeys.get(LC.zimbra_minimize_resources.key());

        KnownKey.expandAll(this, minimize == null ? false : Boolean.valueOf(minimize));
        for (String key : mConfiguredKeys.keySet()) {
            mExpanded.put(key, expand(key, mConfiguredKeys.get(key)));
        }
    }

    /**
     * The singleton instance. This is a volatile variable, so that we can
     * reload the config file on the fly without locking.
     */
    private static volatile LocalConfig mLocalConfig;

    static LocalConfig getInstance() {
        return mLocalConfig;
    }

    /**
     * Loads the config file.
     *
     * @param path config file path or null to use the default path
     * @throws DocumentException if the config file was syntactically invalid
     * @throws ConfigException if the config file was semantically invalid
     */
    static synchronized void load(String path) throws DocumentException, ConfigException {
        mLocalConfig = new LocalConfig(path);
    }

    static {
        try {
            load(null);
        } catch (DocumentException de) {
            throw new RuntimeException(de);
        } catch (ConfigException ce) {
            throw new RuntimeException(ce);
        }
    }
}
