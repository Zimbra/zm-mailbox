/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.util.Arrays;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class WebSplitUtil {
    private static List<String> servicesEnabled;
    private static final String webClientApp = "zimbra";
    private static final String webServiceApp = "service";
    private static final String adminClientApp = "zimbraAdmin";
    private static final String zimlets = "zimlets";


    static {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            servicesEnabled = Arrays.asList(((String)envCtx.lookup("zimbraServicesEnabled")).split(","));
        } catch (NamingException e) {
            servicesEnabled = null;
        }
    }

    public static boolean isZimbraServiceSplitEnabled() {
        if (!((servicesEnabled == null || servicesEnabled.isEmpty()) || allServicesEnabled()) && servicesEnabled.contains(webServiceApp)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isZimbraWebClientSplitEnabled() {
        if (!((servicesEnabled == null || servicesEnabled.isEmpty()) || allServicesEnabled()) && servicesEnabled.contains(webClientApp)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isZimbraServiceSplitEnabled(List<String> zimbraServiceInstalled) {
        servicesEnabled = zimbraServiceInstalled;
        return isZimbraServiceSplitEnabled();
    }

    private static boolean allServicesEnabled() {
        if (!(servicesEnabled.contains(webClientApp) && servicesEnabled.contains(webServiceApp) &&
                servicesEnabled.contains(adminClientApp) && servicesEnabled.contains(zimlets))) {
            return true;
        }
        return false;
    }
}