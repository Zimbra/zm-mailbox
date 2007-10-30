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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.redolog;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides <tt>InputStream</tt> access to RedoLogInput.
 * Stops reading after the given number of bytes.
 * 
 * @author bburtin
 */
public class RedoLogInputStream extends InputStream {

    private RedoLogInput mInput;
    private int mMaxBytes = Integer.MAX_VALUE;
    private int mNumRead = 0;
    
    public RedoLogInputStream(RedoLogInput input) {
        mInput = input;
    }
    
    public RedoLogInputStream(RedoLogInput input, int maxBytes) {
        mInput = input;
        mMaxBytes = maxBytes;
    }
    
    @Override
    public int read() throws IOException {
        if (mNumRead >= mMaxBytes) {
            return -1;
        }
        byte b = mInput.readByte();
        mNumRead++;
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (mNumRead >= mMaxBytes) {
            return -1;
        }
        if (mNumRead + len > mMaxBytes) {
            len = mMaxBytes - mNumRead;
        }
        mInput.readFully(b, off, len);
        mNumRead += len;
        return len;
    }
}
