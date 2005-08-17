package com.zimbra.cs.ozserver;

import java.nio.ByteBuffer;

import com.zimbra.cs.util.ZimbraLog;

public class OzCountingMatcher implements OzMatcher {

    int mTarget = 0;
    
    int mMatched = 0;
    
    public void target(int target) {
    	mTarget = target;
    }

    public int match(ByteBuffer buffer) {
        int nb = buffer.limit() - buffer.position();
        ZimbraLog.ozserver.debug("counting matcher: position="+ buffer.position() + " limit=" + buffer.limit() +
                " new=" + nb + " matched=" + mMatched + " target=" + mTarget);
        if ((nb + mMatched) < mTarget) {
            mMatched += nb;
            buffer.position(buffer.limit());
            return -1;
        } else {
            // Set the buffer position just after the match
            int newPosition = buffer.position() + (mTarget - mMatched);
            buffer.position(newPosition);
            mMatched = mTarget;
            return buffer.position();
        }
	}  

	public void trim(ByteBuffer buffer) {
		// nothing to do
	}

    public void clear() {
    	mMatched = 0;
    }
}
