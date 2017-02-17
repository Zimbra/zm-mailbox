/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.store.external;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.StagedBlob;

public class ExternalStagedBlob extends StagedBlob {

    private final String locator;
    private boolean inserted;

    public ExternalStagedBlob(Mailbox mbox, String digest, long size, String locator) {
        super(mbox, digest, size);
        this.locator = locator;
    }

    @Override
    public String getLocator() {
        return locator;
    }

    ExternalStagedBlob markInserted() {
        inserted = true;
        return this;
    }

    boolean isInserted() {
        return inserted;
    }
}
