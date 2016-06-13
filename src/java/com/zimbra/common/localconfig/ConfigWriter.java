/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;

public abstract class ConfigWriter {

    private boolean mExpandVariables;
    private boolean mHidePasswords;
    
    public static final String FORMAT_PLAIN = "plain";
    public static final String FORMAT_SHELL = "shell";
    public static final String FORMAT_EXPORT = "export";
    public static final String FORMAT_XML = "xml";
    public static final String FORMAT_NOKEY = "nokey";
 
    private ConfigWriter(boolean expandVariables, boolean hidePasswords) {
        mExpandVariables = expandVariables;
        mHidePasswords = hidePasswords;
    }
    
    public boolean expand() {
        return mExpandVariables;
    }
    
    public static ConfigWriter getInstance(String format, boolean expandVariables, boolean hidePasswords) throws ConfigException {
        if (format == null || format.equals(FORMAT_PLAIN))
            return new PlainConfigWriter(expandVariables, hidePasswords);
        if (format.equals(FORMAT_XML))
            return new XmlConfigWriter(expandVariables, hidePasswords);
        if (format.equals(FORMAT_SHELL))
            return new ShellConfigWriter();
        if (format.equals(FORMAT_EXPORT))
            return new ExportConfigWriter();
        if (format.equals(FORMAT_NOKEY))
            return new NokeyConfigWriter(expandVariables, hidePasswords);
        throw new ConfigException("format " + format + " not known");
    }
    
    protected List mItems = new ArrayList(20);
    
    protected static final class Pair {
        String mKey;
        String mValue;
        
        Pair(String k, String v) {
            mKey = k;
            mValue = v;
        }
    }

    public void add(String key, String value) {
        if (value != null) {
            if (mHidePasswords && (key.indexOf("password") != -1)) {
                mItems.add(new Pair(key, "*"));
            } else {
                mItems.add(new Pair(key, value));
            }
        }
    }
    
    abstract public void write(Writer writer) throws IOException;
    
    private static class PlainConfigWriter extends ConfigWriter {
        
        private PlainConfigWriter(boolean expandVariables, boolean hidePasswords) {
            super(expandVariables, hidePasswords);
        }
        
        public void write(Writer writer) {
            PrintWriter pw = new PrintWriter(writer);
            for (Iterator iter = mItems.iterator(); iter.hasNext();) {
                Pair p = (Pair)iter.next();
                pw.println(p.mKey + " = " + p.mValue);
            }
            pw.flush();
        }
        
    }

    private static class XmlConfigWriter extends ConfigWriter {

        private XmlConfigWriter(boolean expandVariables, boolean hidePasswords) {
            super(expandVariables, hidePasswords);
        }
        
        public void write(Writer writer) throws IOException {
            Document doc = DocumentHelper.createDocument();
            Element lce = DocumentHelper.createElement(LocalConfig.E_LOCALCONFIG);
            doc.add(lce);
            for (Iterator iter = mItems.iterator(); iter.hasNext();) {
                Pair p = (Pair)iter.next();
                
                Element key = DocumentHelper.createElement(LocalConfig.E_KEY);
                key.addAttribute(LocalConfig.A_NAME, p.mKey);
                key.addElement(LocalConfig.E_VALUE).addText(p.mValue);
                lce.add(key);
            }
            
            XMLWriter xw = new XMLWriter(writer, org.dom4j.io.OutputFormat.createPrettyPrint());
            xw.write(doc);
            xw.flush();
        }
        
    }

    private static class ShellConfigWriter extends ConfigWriter {

        private ShellConfigWriter() {
            // ShellWriter always expands variables and does not hide passwords!
            super(true, false);
        }
        
        public void write(Writer writer) {
            PrintWriter pw = new PrintWriter(writer);
            for (Iterator iter = mItems.iterator(); iter.hasNext();) {
                Pair p = (Pair)iter.next();
                int vlen = p.mValue.length();
                StringBuffer sb = new StringBuffer(vlen + 2);
                sb.append('\'');
                for (int i = 0; i < vlen; i++) {
                    char ch = p.mValue.charAt(i);
                    if (ch == '\'') {
                        sb.append("''");
                    } else {
                        sb.append(ch);
                    }
                }
                sb.append('\'');
                pw.println(p.mKey + "=" + sb + ";");
            }
            pw.flush();
        }
    }

    private static class ExportConfigWriter extends ConfigWriter {

        private ExportConfigWriter() {
            // ExportWriter always expands variables and does not hide passwords!
            super(true, false);
        }
        
        public void write(Writer writer) {
            PrintWriter pw = new PrintWriter(writer);
            for (Iterator iter = mItems.iterator(); iter.hasNext();) {
                Pair p = (Pair)iter.next();
                int vlen = p.mValue.length();
                StringBuffer sb = new StringBuffer(vlen + 2);
                sb.append('\'');
                for (int i = 0; i < vlen; i++) {
                    char ch = p.mValue.charAt(i);
                    if (ch == '\'') {
                        sb.append("''");
                    } else {
                        sb.append(ch);
                    }
                }
                sb.append('\'');
                pw.println("export " + p.mKey + "=" + sb + ";");
            }
            pw.flush();
        }
    }

    private static class NokeyConfigWriter extends ConfigWriter {

        private NokeyConfigWriter(boolean expandVariables, boolean hidePasswords) {
            super(expandVariables, hidePasswords);
        }
        
        public void write(Writer writer) {
            PrintWriter pw = new PrintWriter(writer);
            for (Iterator iter = mItems.iterator(); iter.hasNext();) {
                Pair p = (Pair)iter.next();
                pw.println(p.mValue);
            }
            pw.flush();
        }
    }
}
