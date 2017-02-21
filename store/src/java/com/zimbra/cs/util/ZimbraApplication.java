/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.util;

import java.util.List;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * Zimbra Servers enable/disable settings overridable by LC.
 */

public class ZimbraApplication {

    private static ZimbraApplication sServices;

    public static ZimbraApplication getInstance() {
        if (sServices == null) {
            String className = LC.zimbra_class_application.value();
            if (className != null && !className.equals("")) {
                try {
                    sServices = (ZimbraApplication)Class.forName(className)
                        .newInstance();
                } catch (Exception e) {
                    ZimbraLog.misc.error(
                        "could not instantiate ZimbraServices interface of class '"
                            + className + "'; defaulting to ZimbraServices", e);
                }
            }
            if (sServices == null)
                sServices = new ZimbraApplication();
        }
        return sServices;
    }

    public String getId() {
        return "zimbra";
    }

    public String getClientId() {
        return "01234567-89AB-CDEF--FEDC-BA9876543210";
    }

    public boolean supports(String className) {
        return true;
    }

    public boolean supports(Class cls) {
        return supports(cls.getName());
    }

    public void startup() {}

    public void initialize(boolean forMailboxd) {}

    public void initializeZimbraDb(boolean forMailboxd) throws ServiceException {}

    private boolean isShutdown;

    public void shutdown() {
        isShutdown = true;
    }

    public boolean isShutdown() {
        return isShutdown;
    }
    
    public void addExtensionName(String name) {
        assert false;
    }
    
    public List<String> getExtensionNames() {
        assert false;
        return null;
    }
}
