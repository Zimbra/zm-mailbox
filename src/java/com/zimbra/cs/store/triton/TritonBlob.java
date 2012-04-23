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

import java.io.IOException;

import com.zimbra.cs.store.Blob;

/**
 * Blob wrapper which includes a previously generated Locator
 * Used to hold locator calculated during streaming upload so stage operation does not need to recalculate it
 *
 */
public class TritonBlob extends Blob {
    private String locator;

    protected TritonBlob(Blob blob, String locator) throws IOException {
        super(blob.getFile(), blob.getRawSize(), blob.getDigest());
        this.locator = locator;
    }

    public String getLocator() {
        return locator;
    }
}
