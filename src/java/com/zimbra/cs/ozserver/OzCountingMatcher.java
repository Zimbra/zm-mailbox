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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OzCountingMatcher implements OzMatcher {

    private static Log mLog = LogFactory.getLog(OzCountingMatcher.class);

    int mTarget = 0;
    
    int mMatched = 0;
    
    public void target(int target) {
    	mTarget = target;
    }
    
    public String toString() {
    	StringBuilder toRet = new StringBuilder("OzCountingMatcher('");
    	toRet.append(mTarget).append(", ");
    	toRet.append(mMatched).append(")");
    	return toRet.toString();
    }

    public boolean match(ByteBuffer buffer) {
        int nb = buffer.remaining();
        if (mLog.isDebugEnabled()) {
            mLog.debug("counting matcher: remaining=" + nb + " matched=" + mMatched + " target=" + mTarget);
        }
        if ((nb + mMatched) < mTarget) {
            mMatched += nb;
            buffer.position(buffer.limit());
            return false;
        } else {
            // Set the buffer position just after the match
            int newPosition = buffer.position() + (mTarget - mMatched);
            buffer.position(newPosition);
            mMatched = mTarget;
            return true;
        }
	}  

    public void reset() {
    	mMatched = 0;
    }

    public boolean matched() {
        return mMatched == mTarget;
    }
    
    public int trailingTrimLength() {
        assert(matched());
        return 0;
    }
}
