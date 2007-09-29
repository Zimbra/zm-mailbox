/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
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
package com.zimbra.cs.ozserver;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class OzFilter {

    public abstract void read(ByteBuffer rbb) throws IOException;

    public abstract void write(ByteBuffer wbb) throws IOException;

    public abstract void close() throws IOException;

    private OzFilter mNextFilter;
    
    void setNextFilter(OzFilter next) {
        mNextFilter = next;
    }
    
    protected OzFilter getNextFilter() {
        return mNextFilter;
    }

    /**
     * Try to flush any outstanding writes, and return true is there are no
     * bytes left to send out.
     */
	public abstract boolean flush() throws IOException;
}
