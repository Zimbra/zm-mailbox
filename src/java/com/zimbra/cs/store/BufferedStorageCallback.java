/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DigestCopyStream;

public class BufferedStorageCallback extends StorageCallback {
    DigestCopyStream dcs;

    public BufferedStorageCallback(long sizeHint) throws ServiceException {
        this(sizeHint, getDiskStreamingThreshold());
    }
    
    public BufferedStorageCallback(long sizeHint, int maxBuffer) {
        this(sizeHint, maxBuffer, maxBuffer);
    }
    
    public BufferedStorageCallback(long sizeHint, int maxBuffer, long maxSize) {
        dcs = new DigestCopyStream(sizeHint, maxBuffer, maxBuffer);
    }
    
    @Override
    public void wrote(Blob blob, byte[] data, int offset, int len) throws
        IOException {
        dcs.write(data, offset, len);
    }

    public byte[] getData() { return dcs.getBuffer(); }
    
    public String getDigest() { return dcs.getDigest(); }
    
    public long getSize() { return dcs.getSize(); }
}
