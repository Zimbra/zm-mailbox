/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mina;

import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * Represents request that has been received by a MINA-based server.
 */
public interface MinaRequest {
    /**
     * Parses specified bytes for the request. Any remaining bytes are left
     * in the specified buffer.
     * 
     * @param bb the byte buffer containing the request bytes
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
