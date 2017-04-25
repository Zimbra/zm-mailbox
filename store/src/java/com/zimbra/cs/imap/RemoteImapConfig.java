/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

package com.zimbra.cs.imap;

import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraRemoteImapBindPort;
import static com.zimbra.common.account.ZAttrProvisioning.A_zimbraRemoteImapSSLBindPort;


public class RemoteImapConfig extends ImapConfig {
    public static final int D_REMOTE_IMAP_BIND_PORT = 8143;
    public static final int D_REMOTE_IMAP_SSL_BIND_PORT = 8993;

    public RemoteImapConfig(boolean ssl) {
        super(ssl);
    }

    @Override
    public int getBindPort() {
        return isSslEnabled() ?
            getIntAttr(A_zimbraRemoteImapSSLBindPort, D_REMOTE_IMAP_SSL_BIND_PORT) :
            getIntAttr(A_zimbraRemoteImapBindPort, D_REMOTE_IMAP_BIND_PORT);
    }
}
