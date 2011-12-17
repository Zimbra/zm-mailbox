/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.common.net;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;

class SecureProtocolSocketFactoryWrapper
    extends ProtocolSocketFactoryWrapper implements SecureProtocolSocketFactory {
    
    private SSLSocketFactory factory;

    SecureProtocolSocketFactoryWrapper(SSLSocketFactory factory) {
        super(factory);
        this.factory = factory;
    }
    
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return factory.createSocket(socket, host, port, autoClose);
    }
}
