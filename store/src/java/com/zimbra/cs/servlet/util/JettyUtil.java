/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.servlet.util;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.HttpConnection;

import com.zimbra.common.util.ZimbraLog;

public class JettyUtil {

    public static void setIdleTimeout(long timeout, HttpServletRequest request) {
        if (request != null) {
            Object attr = request.getAttribute("org.eclipse.jetty.server.HttpConnection");
            if (attr instanceof HttpConnection) {
                @SuppressWarnings("resource")
                HttpConnection conn = (HttpConnection) attr;
                EndPoint ep = conn.getEndPoint();
                if (ep != null) {
                    ep.setIdleTimeout(timeout);
                } else {
                    ZimbraLog.misc.warn("null endpoint setting Jetty timeout?", new Exception());
                }
            } else {
                //this won't work for SPDY connections, so we'll have to consider this further once we enable it.
                ZimbraLog.misc.warn("got [%s] not instanceof org.eclipse.jetty.server.HttpConnection", attr, new Exception());
            }
        } else {
            ZimbraLog.misc.warn("cannot set timeout for null request", new Exception());
        }
    }
}