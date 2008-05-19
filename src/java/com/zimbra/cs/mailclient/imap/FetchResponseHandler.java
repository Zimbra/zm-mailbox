/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.imap;

public abstract class FetchResponseHandler implements ResponseHandler {
    public boolean handleResponse(ImapResponse res) throws Exception {
        if (res.getCCode() == CAtom.FETCH) {
            handleFetchResponse((MessageData) res.getData());
            return true;
        }
        return false;
    }

    public abstract void handleFetchResponse(MessageData md) throws Exception;
}
