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

import com.zimbra.common.util.ZimbraLog;

/**
 * String wrapper for passing upload URL between output stream instantiations
 *
 */
public class TritonUploadUrl {
    private String uploadUrl = null;

    public TritonUploadUrl() {
        super();
    }

    public void setUploadUrl(String uploadUrl) {
        if (isInitialized()) {
            ZimbraLog.store.warn("TritonUploadUrl already set to %s but changing to %s", this.uploadUrl, uploadUrl);
        }
        this.uploadUrl = uploadUrl;
    }

    public boolean isInitialized() {
        return uploadUrl != null;
    }

    @Override
    public String toString() {
        return uploadUrl;
    }
}
