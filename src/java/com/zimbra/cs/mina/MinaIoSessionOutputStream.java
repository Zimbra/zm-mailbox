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

import org.apache.mina.common.IoSession;

import java.nio.ByteBuffer;

/**
 * OutputStream implementation for writing bytes to a MINA IoSession. The bytes
 * are buffered in order to minimize the number of MINA requests passed to the
 * I/O processor for output.
 */
public class MinaIoSessionOutputStream extends MinaOutputStream {
    private final IoSession mSession;

    /**
     * Creates a new output stream for writing output bytes using a specified
     * buffer size.
     *  
     * @param session the IoSession to which bytes are written
     * @param size the size of the output buffer
     */
    public MinaIoSessionOutputStream(IoSession session, int size) {
        super(size);
        mSession = session;
    }

    /**
     * Creates a new output stream for writing output bytes with a default
     * buffer size of 1024 bytes.
     * 
     * @param session the IoSession to which bytes are written
     */
    public MinaIoSessionOutputStream(IoSession session) {
        this(session, 1024);
    }

    @Override
    protected void flushBytes(ByteBuffer bb) {
        mSession.write(MinaUtil.toMinaByteBuffer(bb));
    }
    
}

