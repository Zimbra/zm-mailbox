/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailclient.imap;

public abstract class FetchResponseHandler implements ResponseHandler {
    private boolean dispose;

    public FetchResponseHandler(boolean dispose) {
        this.dispose = dispose;
    }

    public FetchResponseHandler() {
        this(true);
    }

    public void handleResponse(ImapResponse res) throws Exception {
        if (res.getCCode() == CAtom.FETCH) {
            MessageData md = (MessageData) res.getData();
            try {
                handleFetchResponse(md);
            } finally {
                if (dispose) md.dispose();
            }
        }
    }

    public abstract void handleFetchResponse(MessageData md) throws Exception;
}
