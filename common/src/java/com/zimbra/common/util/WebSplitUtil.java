/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.util;

import java.util.Arrays;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class WebSplitUtil {
    private static List<String> servicesEnabled;
    private static final String webClientApp = "zimbra";
    private static final String webClientClassicApp = "zimbra-classic";
    private static final String webServiceApp = "service";
    private static final String adminClientApp = "zimbraAdmin";
    private static final String zimletApp = "zimlet";


    static {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            servicesEnabled = Arrays.asList(((String)envCtx.lookup("zimbraServicesEnabled")).split(","));
            if (servicesEnabled != null) {
            	ZimbraLog.misc.debug("got services enabled %d", servicesEnabled.size());
		for (String service: servicesEnabled) {
			ZimbraLog.misc.debug("service=%s", service);
		}
             }
        } catch (NamingException e) {
            servicesEnabled = null;
            ZimbraLog.misc.debug("Naming exception while getting servicesEnabled",e);
        }
    }

    public static boolean isZimbraServiceSplitEnabled() {
        if (!((servicesEnabled == null || servicesEnabled.isEmpty()) || allServicesEnabled()) && servicesEnabled.contains(webServiceApp)) {
        	ZimbraLog.misc.debug("service split enabled = true");
            return true;
        } else {
        	ZimbraLog.misc.debug("service split enabled = false");
            return false;
        }
    }

    public static boolean isZimbraWebClientSplitEnabled() {
        if (!((servicesEnabled == null || servicesEnabled.isEmpty()) || allServicesEnabled())
            && (servicesEnabled.contains(webClientApp) || servicesEnabled.contains(webClientClassicApp))) {
        	ZimbraLog.misc.debug("webclient split enabled = true");
            return true;
        } else {
        	ZimbraLog.misc.debug("webclient split enabled = false");
            return false;
        }
    }

    private static boolean allServicesEnabled() {
        if ( (servicesEnabled.contains(webClientApp) || servicesEnabled.contains(webClientClassicApp))
                && servicesEnabled.contains(webServiceApp) &&
                servicesEnabled.contains(adminClientApp) && servicesEnabled.contains(zimletApp)) {
        	ZimbraLog.misc.debug("all services enabled = true");
            return true;
        }
        ZimbraLog.misc.debug("all services enabled = false");
        return false;
    }
}
