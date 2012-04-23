/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
