/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.imap;

import java.util.Map;
import java.util.AbstractMap;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.io.IOException;

/**
 * IMAP4 ID extension (RFC 2971) information.
 */
public class IDInfo extends AbstractMap<String, String> {
    private final Map<String, String> attributes;
    
    private static final String NAME = "name";
    private static final String VERSION = "version";
    private static final String OS = "os";
    private static final String OS_VERSION = "os-version";
    private static final String VENDOR = "vendor";
    private static final String SUPPORT_URL = "support-url";
    private static final String ADDRESS = "address";
    private static final String DATE = "date";
    private static final String COMMAND = "command";
    private static final String ARGUMENTS = "arguments";
    private static final String ENVIRONMENT = "environment";

    public static IDInfo read(ImapInputStream is) throws IOException {
        is.skipChar(' ');
        if (!is.match('(')) {
            is.skipNil();
            return null;
        }
        IDInfo info = new IDInfo();
        if (is.peekChar() != ')') {
            do {
                String name = is.readString();
                is.skipChar(' ');
                String value = is.readNString();
                info.put(name, value);
            } while (is.match(' '));
        }
        is.skipChar(')');
        return info;
    }
    
    public IDInfo() {
        attributes = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    }

    public String getName() { return attributes.get(NAME); }
    public String getVersion() { return attributes.get(VERSION); }
    public String getOs() { return attributes.get(OS); }
    public String getOsVersion() { return attributes.get(OS_VERSION); }
    public String getVendor() { return attributes.get(VENDOR); }
    public String getSupportUrl() { return attributes.get(SUPPORT_URL); }
    public String getAddress() { return attributes.get(ADDRESS); }
    public String getDate() { return attributes.get(DATE); }
    public String getCommmand() { return attributes.get(COMMAND); }
    public String getArguments() { return attributes.get(ARGUMENTS); }
    public String getEnvironment() { return attributes.get(ENVIRONMENT); }

    @Override
    public String put(String name, String value) {
        return attributes.put(name, value);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return attributes.entrySet();
    }
    
    public void setName(String name) {
        attributes.put(NAME, name);
    }

    public void setVersion(String version) {
        attributes.put(VERSION, version);
    }

    public void setOs(String os) {
        attributes.put(OS, os);
    }

    public void setOsVersion(String version) {
        attributes.put(OS_VERSION, version);
    }

    public void setVendor(String vendor) {
        attributes.put(VENDOR, vendor);
    }

    public void setSupportUrl(String url) {
        attributes.put(SUPPORT_URL, url);
    }

    public void setAddress(String address) {
        attributes.put(ADDRESS, address);
    }

    public void setDate(String date) {
        attributes.put(DATE, date);
    }

    public void setCommand(String command) {
        attributes.put(COMMAND, command);
    }

    public void setArguments(String args) {
        attributes.put(ARGUMENTS, args);
    }

    public void setEnvironment(String env) {
        attributes.put(ENVIRONMENT, env);
    }

    public List<ImapData> toRequestParam() {
        List<ImapData> data = new ArrayList<ImapData>(attributes.size());
        for (Entry<String, String> e : entrySet()) {
            data.add(ImapData.asString(e.getKey()));
            data.add(ImapData.asNString(e.getValue()));
        }
        return data;
    }
}
