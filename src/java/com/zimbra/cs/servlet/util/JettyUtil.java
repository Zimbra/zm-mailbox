/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
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

package com.zimbra.cs.servlet.util;

import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.HttpChannel;

import com.zimbra.common.util.ZimbraLog;

public class JettyUtil {

    public static void setIdleTimeout(long timeout) {
        HttpChannel<?> channel = HttpChannel.getCurrentHttpChannel();
        if (channel != null) {
            EndPoint ep = channel.getEndPoint();
            if (ep != null) {
                ep.setIdleTimeout(timeout);
            } else {
                ZimbraLog.misc.warn("null endpoint setting Jetty timeout?", new Exception());
            }
        } else {
            ZimbraLog.misc.warn("null channel setting Jetty timeout?", new Exception());
        }
    }

}
