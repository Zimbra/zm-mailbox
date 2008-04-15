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
import java.io.IOException;

/**
 * Result of POP3 CAPA extension (see rfc2449)
 */
public class Pop3Capabilities {
    private Map<String, String[]> capabilities;
    
    public static final String TOP = "TOP";
    public static final String USER = "USER";
    public static final String SASL = "SASL";
    public static final String RESP_CODES = "RESP-CODES";
    public static final String LOGIN_DELAY = "LOGIN-DELAY";
    public static final String PIPELINING = "PIPELINING";
    public static final String EXPIRE = "EXPIRE";
    public static final String UIDL = "UIDL";
    public static final String IMPLENTATION = "IMPLEMENTATION";

    public Pop3Capabilities read(ContentInputStream is) throws IOException {
        Pop3Capabilities caps = new Pop3Capabilities();
        caps.readCapabilities(is);
        return caps;
    }
    
    private Pop3Capabilities() {
        capabilities = new HashMap<String, String[]>();
    }

    private void readCapabilities(ContentInputStream is) throws IOException {
        String line;
        while ((line = is.readLine()) != null) {
            String[] words = line.split(" ");
            if (words.length > 0) {
                String keyword = words[0];
                String[] params = null;
                if (words.length > 1) {
                    params = new String[words.length - 1];
                    System.arraycopy(words, 1, params, 0, params.length);
                }
                capabilities.put(keyword, params);
            }
        }
    }

    public boolean hasCapability(String cap) {
        return capabilities.containsKey(cap);
    }

    public String[] getParameters(String cap) {
        return capabilities.get(cap);
    }
}
