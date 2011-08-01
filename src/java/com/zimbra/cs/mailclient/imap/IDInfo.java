/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 Zimbra, Inc.
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
public final class IDInfo {
    public static final String NAME = "name";
    public static final String VERSION = "version";
    public static final String OS = "os";
    public static final String OS_VERSION = "os-version";
    public static final String VENDOR = "vendor";
    public static final String SUPPORT_URL = "support-url";
    public static final String ADDRESS = "address";
    public static final String DATE = "date";
    public static final String COMMAND = "command";
    public static final String ARGUMENTS = "arguments";
    public static final String ENVIRONMENT = "environment";
    public static final String X_ORIGINATING_IP = "X-ORIGINATING-IP";
    public static final String X_VIA = "X-VIA";

    private final Map<String, String> fields = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

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
            info.fields.put(name, value);
            is.skipSpaces();
        }
        return info;
    }

    public String get(String key) {
        return fields.get(key);
    }

    public String put(String key, String value) {
        return fields.put(key, value);
    }

    public List<ImapData> toRequestParam() {
        List<ImapData> data = new ArrayList<ImapData>(fields.size());
        for (Map.Entry<String, String> e : fields.entrySet()) {
            data.add(ImapData.asString(e.getKey()));
            data.add(ImapData.asNString(e.getValue()));
        }
        return data;
    }
}
