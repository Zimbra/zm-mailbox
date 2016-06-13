/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.server;

import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * Represents request that has been received by a MINA-based server.
 */
public interface NioRequest {
    /**
     * Parses specified bytes for the request. Any remaining bytes are left in the specified buffer.
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
