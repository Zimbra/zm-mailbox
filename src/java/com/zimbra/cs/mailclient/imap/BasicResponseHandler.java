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

import java.util.List;

/**
 * A ResponseHandler implementations which accumulates untagged data results
 * matching the specified command code.
 */
public class BasicResponseHandler implements ResponseHandler {
    private final Atom code;
    private final List results;

    public BasicResponseHandler(Atom code, List results) {
        this.code = code;
        this.results = results;
    }

    public BasicResponseHandler(CAtom code, List results) {
        this(code.atom(), results);
    }

    @SuppressWarnings("unchecked")
    public void handleResponse(ImapResponse res) {
        if (res.getCode().equals(code)) {
            results.add(res.getData());
        }
    }
}
