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
package com.zimbra.cs.store.triton;

import java.io.IOException;

import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.external.ExternalUploadedBlob;

/**
 * Blob wrapper which includes a previously generated Locator
 * Used to hold locator calculated during streaming upload so stage operation does not need to recalculate it
 *
 */
public class TritonBlob extends ExternalUploadedBlob {
    private final String locator;
    private final MozyServerToken serverToken;

    protected TritonBlob(Blob blob, String locator, String uploadUrl, MozyServerToken serverToken) throws IOException {
        super(blob, uploadUrl);
        this.locator = locator;
        this.serverToken = serverToken;
    }

    public String getLocator() {
        return locator;
    }

    public MozyServerToken getServerToken() {
        return serverToken;
    }
}
