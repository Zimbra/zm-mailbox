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
package com.zimbra.cs.mailclient.pop3;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.Iterator;
import java.io.IOException;

/**
 * Result of POP3 CAPA extension (see rfc2449)
 */
public class Pop3Capabilities {
    private Map<String, List<String>> capabilities;
    
    public static final String TOP = "TOP";
    public static final String USER = "USER";
    public static final String STLS = "STLS";
    public static final String SASL = "SASL";
    public static final String RESP_CODES = "RESP-CODES";
    public static final String LOGIN_DELAY = "LOGIN-DELAY";
    public static final String PIPELINING = "PIPELINING";
    public static final String EXPIRE = "EXPIRE";
    public static final String UIDL = "UIDL";
    public static final String IMPLEMENTATION = "IMPLEMENTATION";

    public static Pop3Capabilities read(ContentInputStream is) throws IOException {
        Pop3Capabilities caps = new Pop3Capabilities();
        caps.readCapabilities(is);
        return caps;
    }
    
    private Pop3Capabilities() {
        capabilities = new HashMap<String, List<String>>();
    }

    private void readCapabilities(ContentInputStream is) throws IOException {
        String line;
        while ((line = is.readLine()) != null) {
            String[] words = line.split(" ");
            if (words.length > 0) {
                String key = words[0];
                List<String> params = capabilities.get(key);
                if (params == null) {
                    params = new ArrayList<String>();
                    capabilities.put(key.toUpperCase(), params);
                }
                for (int i = 1; i < words.length; i++) {
                    params.add(words[i]);
                }
            }
        }
    }

    public boolean hasCapability(String cap) {
        return capabilities.containsKey(cap.toUpperCase());
    }

    public boolean hasCapability(String cap, String param) {
        if (param == null) {
            return hasCapability(cap);
        }
        List<String> params = getParameters(cap);
        return params != null && params.contains(param);
    }

    public List<String> getParameters(String cap) {
        return capabilities.get(cap.toUpperCase());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        Iterator<Map.Entry<String, List<String>>> entries =
            capabilities.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, List<String>> e = entries.next();
            sb.append(e.getKey());
            Iterator<String> params = e.getValue().iterator();
            if (params.hasNext()) {
                sb.append("=\"").append(params.next());
                while (params.hasNext()) {
                    sb.append(' ').append(params.next());
                }
                sb.append('"');
            }
            if (entries.hasNext()) {
                sb.append(',');
            }
        }
        sb.append('}');
        return sb.toString();
    }
}
