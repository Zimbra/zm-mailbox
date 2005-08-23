/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

package com.zimbra.cs.ozserver;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

class OzBufferManager {
    private static List mFreeBuffers = new LinkedList();
    
    private static int mInUse = 0;
    
    private static int mAllocated = 0;
    
    public static final int BUFFER_SIZE = 1000;
    
    private static final byte[] mEmptyBuffer = new byte[BUFFER_SIZE];
    
    static synchronized ByteBuffer getBuffer() {
        ByteBuffer buf = null;
        
        if (mFreeBuffers.isEmpty()) {
            buf = ByteBuffer.allocateDirect(BUFFER_SIZE);
            ZimbraLog.ozserver.info("New direct buffer inuse=" + mInUse + " allocated=" + mAllocated);
            mAllocated++;
        } else {
            buf = (ByteBuffer)mFreeBuffers.remove(0);
            buf.clear();
            buf.put(mEmptyBuffer);
            buf.clear();
        }
        mInUse++;
        return buf;
    }
    
    static synchronized void returnBuffer(ByteBuffer buf) {
        mInUse--;
        mFreeBuffers.add(buf);
    }
    
    static {
        TimerTask task = new TimerTask() {
            public void run() {
                ZimbraLog.ozserver.info("Buffer manager inUse=" + mInUse + " allocated=" + mAllocated);
            }
        };
        Zimbra.sTimer.scheduleAtFixedRate(task, 300000, 300000);
    }
}
