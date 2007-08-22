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

import java.nio.ByteBuffer;

import static com.zimbra.cs.mina.MinaUtil.*;

public class LineBuffer {
    private ByteBuffer mBuffer;
    private boolean mComplete;

    public LineBuffer() {}
    
    public LineBuffer(int size) {
        mBuffer = ByteBuffer.allocate(size);
    }

    public void parse(ByteBuffer bb) {
        if (isComplete()) return;
        int pos = findLF(bb);
        if (pos == -1) {
            // No end of line found, so just add remaining bytes to buffer
            mBuffer = MinaUtil.expand(mBuffer, bb.remaining()).put(bb);
            return;
        }
        // End of line found,
        int len = pos - bb.position();
        ByteBuffer lbb = bb.slice();
        bb.position(pos + 1);
        // Remove trailing CR's
        while (len > 0 && lbb.get(len - 1) == CR) --len;
        lbb.limit(len);
        mBuffer = MinaUtil.expand(mBuffer, len, len).put(lbb);
        mBuffer.flip();
        mComplete = true;
    }

    public ByteBuffer getByteBuffer() {
        return isComplete() ? mBuffer : null;
    }

    public String toString() {
        return isComplete() ? MinaUtil.toString(mBuffer) : null;
    }
    
    public boolean isComplete() {
        return mComplete;
    }

    private static int findLF(ByteBuffer bb) {
        int limit = bb.limit();
        for (int pos = bb.position(); pos < limit; pos++) {
            if (bb.get(pos) == LF) return pos;
        }
        return -1;
    }
}
