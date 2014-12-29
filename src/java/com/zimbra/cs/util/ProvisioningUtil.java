/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;

/**
 * @author sankumar
 * Utility methods which handle service exception and return default value, rather the propagating this bloat in all java files.
 * Crated while moving lc attributes to ldap.
 *
 */
public class ProvisioningUtil {
    public static  boolean getServerAttribute(String name, boolean def){
        try {
            return Provisioning.getInstance().getLocalServer().getBooleanAttr(name, def);
        } catch (ServiceException e) {
            ZimbraLog.misc.error(e);
            return def;
        }
    }

    public static int getServerAttribute(String name, int def){
        try {
            return Provisioning.getInstance().getLocalServer().getIntAttr(name, def);
        } catch (ServiceException e) {
            ZimbraLog.misc.error(e);
            return def;
        }
    }

    public static long getServerAttribute(String name, long def){
        try {
            return Provisioning.getInstance().getLocalServer().getLongAttr(name, def);
        } catch (ServiceException e) {
            ZimbraLog.misc.error(e);
            return def;
        }
    }

    public static String getServerAttribute(String name, String def){
        try {
            return Provisioning.getInstance().getLocalServer().getAttr(name, def);
        } catch (ServiceException e) {
            ZimbraLog.misc.error(e);
            return def;
        }
    }
}
