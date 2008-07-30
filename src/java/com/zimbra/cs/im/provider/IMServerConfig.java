/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.im.provider;

import org.jivesoftware.util.IMConfigProperty;
import org.jivesoftware.util.IMConfigProperty.ConstantStr;
import org.jivesoftware.util.LdapConfig.ServerConfigProvider;
import org.jivesoftware.wildfire.vcard.DefaultVCardProvider;

/**
 * 
 */
public class IMServerConfig implements ServerConfigProvider {

    public IMConfigProperty getLdapProp(String name) {
        if ("zimbraXmppClientTlsPolicy".equals(name)) {
            return new ConstantStr("disabled","");
        } else 
            return new ConstantStr(null, null);
    }

    public IMConfigProperty getAuthProvider() {
        return new ConstantStr(ZimbraAuthProvider.class.getName(), "Class name for Auth Provider");
    }

    public IMConfigProperty getConnectionProvider() {
        return new ConstantStr(ZimbraConnectionProvider.class.getName(), "Class name for DB Connection Provider");
    }

    public IMConfigProperty getGroupProvider() {
        return new ConstantStr(ZimbraGroupProvider.class.getName(), "Class name for Group Provider");
    }

    public IMConfigProperty getProxyTransferProvider() {
        return new ConstantStr("org.jivesoftware.wildfire.filetransfer.proxy.DefaultProxyTransfer", "Class name for file proxy transfer");
    }

    public IMConfigProperty getRoutingTableProvider() {
        return new ConstantStr(ZimbraRoutingTableImpl.class.getName(), "Class name for routing table");
    }

    public IMConfigProperty getUserProvider() {
        return new ConstantStr(ZimbraUserProvider.class.getName(), "Class name for User Provider");
    }

    public IMConfigProperty getVCardProvider() {
        return new ConstantStr(DefaultVCardProvider.class.getName(), "Class name for vCard provider");
    }

}
