/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import java.io.IOException;

/**
 * This exception is thrown by {@link ImapProxy} if a network error occurred with the remote IMAP server.
 *
 * @author ysasaki
 */
final class ImapProxyException extends IOException {

    ImapProxyException(String message) {
        super(message);
    }

    ImapProxyException(Throwable e) {
        super(e);
    }

}
