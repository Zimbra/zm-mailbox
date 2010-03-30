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

public class NioTextLineRequest implements NioRequest {
    private final LineBuffer mBuffer;

    public NioTextLineRequest() {
        mBuffer = new LineBuffer();
    }

    public void parse(ByteBuffer bb) {
        mBuffer.parse(bb);
    }

    public boolean isComplete() {
        return mBuffer.isComplete();
    }

    public String getLine() {
        return mBuffer.isComplete() ? mBuffer.toString() : null;
    }

    public String toString() {
        return getLine();
    }
}
