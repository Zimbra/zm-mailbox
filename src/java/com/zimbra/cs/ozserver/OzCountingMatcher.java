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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

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
