/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.io.IOException;

import com.zimbra.common.mailbox.ZimbraQueryHit;
import com.zimbra.common.mailbox.ZimbraQueryHitResults;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.ZimbraQueryResults;

public class LocalQueryHitResults implements ZimbraQueryHitResults {

    private final ZimbraQueryResults zQueryResults;

    public LocalQueryHitResults(ZimbraQueryResults zqr) {
        zQueryResults = zqr;
    }

    @Override
    public ZimbraQueryHit getNext() throws ServiceException {
        return zQueryResults.getNext();
    }

    @Override
    public void close() throws IOException {
        zQueryResults.close();
    }
}
