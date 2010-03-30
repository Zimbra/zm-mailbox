/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.nio;

import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * Represents request that has been received by a MINA-based server.
 */
public interface NioRequest {
    /**
     * Parses specified bytes for the request. Any remaining bytes are left
     * in the specified buffer.
     * 
     * @param bb the byte buffer containing the request bytes
     * @throws IllegalArgumentException if the request could not be parsed
     * @throws IOException if an I/O error occurs
     */
    void parse(ByteBuffer bb) throws IOException;

    /**
     * Returns true if the request is complete and no more bytes are required.
     * 
     * @return true if the request is complete, otherwise false
     */
    boolean isComplete();
}
