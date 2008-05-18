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
package com.zimbra.cs.mailclient.util;

import java.io.IOException;
import java.lang.reflect.Method;

public final class Password {
    private final Method readPassword;
    
    private static final Password INSTANCE = new Password();

    /**
     * Reads a password from the console. If Java version is at least 1.6 then
     * uses java.io.Console to read the password without echo. Otherwise,
     * defaults to reading password with echo.
     *
     * @param prompt the password prompt
     * @return the password, or null if the password could not be read
     */
    public static String read(String prompt) {
        return INSTANCE.readPassword(prompt);
    }
    
    private Password() {
        readPassword = getReadPassword();
    }

    private static Method getReadPassword() {
        try {
            Class cls = Class.forName("java.io.Console");
            return cls.getMethod("readPassword", String.class, Object[].class);
        } catch (ClassNotFoundException e) {
        } catch (NoSuchMethodException e) {
        }
        return null;
    }

    private String readPassword(String prompt) {
        if (readPassword != null) {
            try {
                return new String((char[]) readPassword.invoke(null, "%s", prompt));
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        try {
            return defaultReadPassword(prompt);
        } catch (IOException e) {
            return null;
        }
    }

    // For Java version < 1.6 reading password with echo is our only option
    private static String defaultReadPassword(String prompt) throws IOException {
        System.out.print(prompt);
        System.out.flush();
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = System.in.read()) != -1 && c != '\n') {
            if (c != '\r') sb.append((char) c);
        }
        return sb.toString();
    }
}
