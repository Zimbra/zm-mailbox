/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
    
    public SoapSession getSoapSession(String authAccountId, boolean isLocal) throws ServiceException {
        if (isLocal) {
            return new SoapSession(authAccountId);
        } else {
            return new RemoteSoapSession(authAccountId);
        }
    }
}
