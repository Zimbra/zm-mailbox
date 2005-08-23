/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

package com.zimbra.cs.localconfig;

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

public class LocalConfig {

    static final String E_LOCALCONFIG = "localconfig";
    static final String E_KEY = "key";
    static final String A_NAME = "name";
    static final String E_VALUE = "value";
    
    private String mConfigFile;
    
    private String defaultConfigFile() {
        String zmHome = System.getProperty("zimbra.home");
        if (zmHome == null) {
            zmHome = File.separator + "opt" + File.separator + "zimbra";
        }
        return zmHome + File.separator + "conf" + File.separator + "localconfig.xml";
    }
        
    String getConfigFile() {
        return mConfigFile;
    }

    private Map mConfiguredKeys = new HashMap(); 

    void set(String key, String value) 
    {
        mConfiguredKeys.put(key, value);
    }

    private String getRaw(String key) 
    {
        if (mConfiguredKeys.containsKey(key))
            return (String)mConfiguredKeys.get(key);
        
        if (KnownKey.isKnown(key))
            return KnownKey.getDefaultValue(key);
        
        return null;
    }

    private boolean expandOnce(StringBuffer value, Set seenKeys) throws ConfigException {
        int len = value.length();

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
            for (Iterator iter = seenKeys.iterator(); iter.hasNext();) {
                String seen = (String)iter.next();
                sb.append(" ").append(seen);
            }
            throw new ConfigException(sb.toString());
        }
        
        seenKeys.add(key);
        
        String replacement = getRaw(key);
        if (replacement == null) {
            throw new ConfigException("null valued key '" + key + "' referenced");
        }
        value.replace(begin, end+1, replacement);
        
        return true;
    }

    private String expand(String key, String rawValue) throws ConfigException {
        Set seenKeys = new HashSet();
        seenKeys.add(key);
        StringBuffer result = new StringBuffer(rawValue);
        while (expandOnce(result, seenKeys));
        return result.toString();
    }

    String get(String key) throws ConfigException {
        String raw = getRaw(key);
        if (raw == null) {
            return null;
        }
        return expand(key, raw);
    }
    
    //
    // Load & save
    //
    
    void save() throws IOException, ConfigException {
        ConfigWriter xmlWriter = ConfigWriter.getInstance("xml", false, false);
        for (Iterator iter = mConfiguredKeys.keySet().iterator(); iter.hasNext();) {
            String key = (String)iter.next();
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
            
            if (!root.getName().equals(E_LOCALCONFIG)) {
                throw new DocumentException("config file " + mConfigFile + " root tag is not " + E_LOCALCONFIG);
            }
            
            for (Iterator iter = root.elementIterator(E_KEY); iter.hasNext();) {
                Element ekey = (Element) iter.next();
                String key = ekey.attributeValue(A_NAME);
                String value = (String) ekey.elementText(E_VALUE);
                set(key, value);
            }
        } else {
            Logging.warn("local config file `" + cf + "' is not readable");
        }
        verify();
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
            keys = (String[])mConfiguredKeys.keySet().toArray(new String[0]);
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
        Set union = new HashSet();
        
        // Add known keys.
        String[] knownKeys = KnownKey.getAll();
        for (int i = 0; i < knownKeys.length; i++) {
            union.add(knownKeys[i]);
        }
        
        // Add set keys (this might contain unknown keys)
        for (Iterator iter = mConfiguredKeys.keySet().iterator(); iter.hasNext();) {
            union.add((String)iter.next());
        }
        
        return (String[])union.toArray(new String[0]);
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
    // Verify
    //
    private void verify() throws ConfigException {
        String[] keys = KnownKey.getAll();
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            String value = KnownKey.getDefaultValue(key);
            value = expand(key, value);
        }

        for (Iterator iter = mConfiguredKeys.keySet().iterator(); iter.hasNext();) {
            String key = (String)iter.next();
            String value = get(key);
        }
    }

    //
    // The instance
    //
    private static LocalConfig mLocalConfig;
    
    static LocalConfig getInstance() {
        return mLocalConfig;
    }
    
    static void readConfig(String path) throws DocumentException, ConfigException {
        mLocalConfig = new LocalConfig(path);
    }

    static {
        try {
            readConfig(null);
        } catch (DocumentException de) {
            throw new RuntimeException(de);
        } catch (ConfigException ce) {
            throw new RuntimeException(ce);
        }
    }
}
