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

package org.apache.mina.filter.ssl;
import javax.net.ssl.SSLContext;

import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;

/** classpath visibility workaround for access into SSL handshake status **/
public class ZimbraSslFilter extends SslFilter {

    private static final AttributeKey SSL_HANDLER_KEY = new AttributeKey(SslFilter.class, "handler");

    public ZimbraSslFilter(SSLContext sslContext) {
        super(sslContext);
    }

    public boolean isSslHandshakeComplete(IoSession session) {
        SslHandler handler = getSslSessionHandler(session);
        return handler != null && handler.isHandshakeComplete();
    }

    private SslHandler getSslSessionHandler(IoSession session) {
        SslHandler handler = (SslHandler) session.getAttribute(SSL_HANDLER_KEY);

        if (handler == null) {
            throw new IllegalStateException();
        }

        if (handler.getSslFilter() != this) {
            throw new IllegalArgumentException("Not managed by this filter.");
        }

        return handler;
    }
}
