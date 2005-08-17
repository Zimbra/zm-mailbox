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
        Liquid.sTimer.scheduleAtFixedRate(task, 300000, 300000);
    }
}
