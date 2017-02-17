/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.localconfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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

import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.soap.XmlParseException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.L10nUtil;

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

    public String getConfigFile() {
        return mConfigFile;
    }

    private final Map<String, String> mConfiguredKeys = new HashMap<String, String>();
    private final Map<String, String> mExpanded = new HashMap<String, String>();

    public void set(String key, String value) {
        mConfiguredKeys.put(key, value);
    }

    String getRaw(String key) {
        if (mConfiguredKeys.containsKey(key))
            return mConfiguredKeys.get(key);

        if (KnownKey.isKnown(key))
            return KnownKey.getDefaultValue(key);

        return null;
    }

    String findKey(String value) {
        int begin = value.indexOf("${");
        if (begin == -1) {
            return null;
        }

        int end = value.indexOf("}", begin);
        if (end == -1) {
            return null;
        }

        return value.substring(begin + 2, end);
    }

    private String expandDeep(String key, Set<String> seenKeys) throws ConfigException {
        if (seenKeys.contains(key)) {
            StringBuilder sb = new StringBuilder();
            sb.append("recursive expansion of key '" + key + "':");
            for (String seen : seenKeys)
                sb.append(" ").append(seen);
            throw new ConfigException(sb.toString());
        }

        seenKeys.add(key);

        String replacement = getRaw(key);
        if (replacement == null)
            throw new ConfigException("null valued key '" + key + "' referenced");

        String nestedKey = null;
        while ((nestedKey = findKey(replacement)) != null) {
            String expanded = expandDeep(nestedKey, seenKeys);
            String target = "${" + nestedKey +  "}";
            replacement = replacement.replace(target, expanded);
        }

        seenKeys.remove(key);
        return replacement;
    }

    String expand(String key, String rawValue) throws ConfigException {
        if (rawValue == null)
            return null;

        return expandDeep(key, new HashSet<String>());
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
    public void save() throws IOException, ConfigException {
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
            try (FileInputStream fis = new FileInputStream(cf)) {
                Document document = W3cDomUtil.parseXMLToDom4jDocUsingSecureProcessing(fis);
                Element root = document.getRootElement();

                if (!root.getName().equals(E_LOCALCONFIG))
                    throw new DocumentException("config file " + mConfigFile + " root tag is not " + E_LOCALCONFIG);

                for (Iterator<?> iter = root.elementIterator(E_KEY); iter.hasNext(); ) {
                    Element ekey = (Element) iter.next();
                    String key = ekey.attributeValue(A_NAME);
                    String value = ekey.elementText(E_VALUE);
                    set(key, value);
                }
            } catch (IOException | XmlParseException e) {
                Logging.warn(String.format("Problem parsing local config file '%s'", cf), e);
                throw new DocumentException(String.format("Problem parsing local config file '%s'", cf));
            }
        } else {
            Logging.warn("local config file `" + cf + "' is not readable");
        }
        expandAll();
    }

    public boolean isSet(String key) {
        return mConfiguredKeys.containsKey(key) || KnownKey.isKnown(key);
    }

    public void remove(String key) {
        mConfiguredKeys.remove(key);
    }

    //
    // Print
    //
    public static void printDoc(PrintStream ps, String[] keys, boolean printUnsupported) {
        if (keys.length == 0) {
            keys = KnownKey.getAll();
            Arrays.sort(keys);
        }
        // Get the default keyset for the default system locale
        Set<String> keySet = L10nUtil.getBundleKeySet(null);
        for (int i = 0; i < keys.length; i++) {
            KnownKey key = KnownKey.get(keys[i]);
            if (key == null) {
                Logging.warn("'" + keys[i] + "' is not a known key");
                continue;
            }

           if (keySet.contains(key.key()) && (key.isSupported() || printUnsupported)) {
            	String doc = key.doc();
            	if (i > 0) {
                    ps.println();
                }
                ps.println(keys[i] + ':');
                fmt(ps, doc, 80);
                if (!key.isReloadable()) {
                    ps.println("* Changes are in effect after server restart.");
                }

            }


        }
    }

    public void printChanged(OutputStream out, ConfigWriter writer, String[] keys)
        throws ConfigException, IOException {

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

    public void printDefaults(OutputStream out, ConfigWriter writer, String[] keys)
        throws IOException, ConfigException {

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

    public void print(OutputStream out, ConfigWriter writer, String[] keys)
        throws IOException, ConfigException {

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

        KnownKey.expandAll(this);
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
