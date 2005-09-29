/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

class OzBufferPool {
    private List mFreeBuffers;
    
    private List mAllocatedBuffers;
    
    private int mInUse = 0;
    
    public int mBufferSize;
    
    private final byte[] mEmptyBuffer;
    
    private String mName;
    
    private static final int USAGE_DISPLAY_INTERVAL = 10000;
    
    private boolean mDestroyed;
    
    OzBufferPool(String name, int bufferSize) {
        mName = name;
        mBufferSize = bufferSize;
        mFreeBuffers = new LinkedList();
        mAllocatedBuffers = new ArrayList(16);
        mEmptyBuffer = new byte[mBufferSize];

        TimerTask task = new TimerTask() {
            public void run() {
                ZimbraLog.ozserver.info("buffer pool name=" + mName + " inuse=" + mInUse + " allocated=" + mAllocatedBuffers.size());
            }
        };
        Zimbra.sTimer.scheduleAtFixedRate(task, USAGE_DISPLAY_INTERVAL, USAGE_DISPLAY_INTERVAL);
    }
    
    synchronized ByteBuffer get() {
        if (mDestroyed) {
            throw new IllegalStateException("trying to get from a destroyed buffer pool");
        }

        ByteBuffer buf = null;
        
        if (mFreeBuffers.isEmpty()) {
            buf = ByteBuffer.allocateDirect(mBufferSize);
            mAllocatedBuffers.add(buf);
            ZimbraLog.ozserver.info("New direct buffer inuse=" + mInUse + " allocated=" + mAllocatedBuffers.size());
        } else {
            buf = (ByteBuffer)mFreeBuffers.remove(0);
            buf.clear();
            buf.put(mEmptyBuffer);
            buf.clear();
        }
        mInUse++;
        return buf;
    }
    
    synchronized void recycle(ByteBuffer buf) {
        if (mDestroyed) {
            throw new IllegalStateException("trying to recycle into a destroyed buffer pool");
        }
        mInUse--;
        mFreeBuffers.add(buf);
    }
    
    synchronized void destroy() {
        if (mDestroyed) {
            throw new IllegalStateException("trying to destroy a destroyed buffer pool");
        }
        mDestroyed = true;
        ZimbraLog.ozserver.info("destroying buffer pool name=" + mName + " inuse=" + mInUse + " allocated=" + mAllocatedBuffers.size());
        mAllocatedBuffers.clear();
        mFreeBuffers.clear();
    }
}
