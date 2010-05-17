/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.imap;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.io.IOException;

/**
 * IMAP4 ID extension (RFC 2971) information.
 */
public class IDInfo extends TreeMap<String, String> {
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
        is.skipSpaces();
        IDInfo info = new IDInfo();
        if (!is.match('(')) {
            is.skipNil();
            return info;
        }
        while (!is.match(')')) {
            String name = is.readString();
            is.skipChar(' ');
            String value = is.readNString();
            info.put(name, value);
            is.skipSpaces();
        }
        return info;
    }
    
    public IDInfo() {
        super(String.CASE_INSENSITIVE_ORDER);
    }

    public String getName() { return get(NAME); }
    public String getVersion() { return get(VERSION); }
    public String getOs() { return get(OS); }
    public String getOsVersion() { return get(OS_VERSION); }
    public String getVendor() { return get(VENDOR); }
    public String getSupportUrl() { return get(SUPPORT_URL); }
    public String getAddress() { return get(ADDRESS); }
    public String getDate() { return get(DATE); }
    public String getCommmand() { return get(COMMAND); }
    public String getArguments() { return get(ARGUMENTS); }
    public String getEnvironment() { return get(ENVIRONMENT); }

    public void setName(String name) { put(NAME, name); }
    public void setVersion(String version) { put(VERSION, version); }
    public void setOs(String os) { put(OS, os); }
    public void setOsVersion(String version) { put(OS_VERSION, version); }
    public void setVendor(String vendor) { put(VENDOR, vendor); }
    public void setSupportUrl(String url) { put(SUPPORT_URL, url); }
    public void setAddress(String address) { put(ADDRESS, address); }
    public void setDate(String date) { put(DATE, date); }
    public void setCommand(String command) { put(COMMAND, command); }
    public void setArguments(String args) { put(ARGUMENTS, args); }
    public void setEnvironment(String env) { put(ENVIRONMENT, env); }

    public List<ImapData> toRequestParam() {
        List<ImapData> data = new ArrayList<ImapData>(size());
        for (Map.Entry<String, String> e : entrySet()) {
            data.add(ImapData.asString(e.getKey()));
            data.add(ImapData.asNString(e.getValue()));
        }
        return data;
    }
}
