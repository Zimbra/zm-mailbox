/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.soap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.session.RemoteSoapSession;
import com.zimbra.cs.session.SoapSession;

public class SoapSessionFactory {

    private static SoapSessionFactory sSessionFactory = null;

    public synchronized static SoapSessionFactory getInstance() {
        if (sSessionFactory == null) {
            String className = LC.zimbra_class_soapsessionfactory.value();
            if (className != null && !className.equals("")) {
                try {
                    try {
                        sSessionFactory = (SoapSessionFactory) Class.forName(className).newInstance();
                    } catch (ClassNotFoundException cnfe) {
                        // ignore and look in extensions
                        sSessionFactory = (SoapSessionFactory) ExtensionUtil.findClass(className).newInstance();
                    }
                } catch (Exception e) {
                    ZimbraLog.account.error("could not instantiate SoapSessionFactory class '" + className + "'; defaulting to SoapSessionFactory", e);
                }
            }
            if (sSessionFactory == null) {
                sSessionFactory = new SoapSessionFactory();
            }
        }
        return sSessionFactory;
    }


    public SoapSession getSoapSession(ZimbraSoapContext zsc) throws ServiceException {
        return getSoapSession(zsc,null);
    }

    public SoapSession getSoapSession(ZimbraSoapContext zsc, String sessionId) throws ServiceException {
        if (zsc.isAuthUserOnLocalhost()) {
            return new SoapSession(zsc, sessionId);
        } else {
            return new RemoteSoapSession(zsc, sessionId);
        }
    }
}
