/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
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

import java.nio.ByteBuffer;

public class OzAllMatcher implements OzMatcher {

    public boolean match(ByteBuffer buf) {
       int n = buf.remaining();
       for (int i = 0; i < n; i++) {
            buf.get();
       }
       return true;
    }

    public boolean matched() {
        return true;
    }

    public void reset() {
    }

    public int trailingTrimLength() {
        return 0;
    }

}
