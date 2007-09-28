/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.security.sasl;

import javax.security.sasl.SaslServer;
import javax.security.sasl.Sasl;

public final class SaslUtil {
    /** Default max send buffer size */
    public static final int MAX_SEND_SIZE = 4096;
    
    /** Default max receive buffer size */
    public static final int MAX_RECV_SIZE = 4096;

    public static int getMaxSendSize(SaslServer server) {
        String s = (String) server.getNegotiatedProperty(Sasl.RAW_SEND_SIZE);
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return MAX_SEND_SIZE;
        }
    }

    public static int getMaxRecvSize(SaslServer server) {
        String s = (String) server.getNegotiatedProperty(Sasl.MAX_BUFFER);
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return MAX_RECV_SIZE;
        }
    }
}
