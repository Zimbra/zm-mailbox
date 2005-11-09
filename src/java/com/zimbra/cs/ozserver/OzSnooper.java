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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.ozserver;

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;

public class OzSnooper {
    
    public static final int NONE = 0;
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int INPUT = 4;
    
    public static final int ALL = 0xFFFFFFFF;
    
    private boolean mSnoopReads;
    private boolean mSnoopWrites;
    private boolean mSnoopInputs;
    
    private Log mLog;

    public OzSnooper(Log log) {
        mLog = log;
    }

    public OzSnooper(Log log, int mode) {
        mLog = log;
        setMode(mode);
    }

    public boolean snoopReads() {
        return mSnoopReads;
    }
    
    public boolean snoopWrites() {
        return mSnoopWrites;
    }
    
    public boolean snoopInputs() {
        return mSnoopInputs;
    }
    
    public void snoopRead(boolean v) {
        mSnoopReads = v;
    }

    public void snoopWrite(boolean v) {
        mSnoopWrites = v;
    }
    
    public void snoopInput(boolean v) {
        mSnoopInputs = v;
    }
    
    public void setMode(int mode) {
        mSnoopReads = (mode & READ) != 0;
        mSnoopWrites = (mode & WRITE) != 0;
        mSnoopInputs = (mode & INPUT) != 0;
    }

    private void print(String s) {
        if (mLog.isInfoEnabled()) {
            mLog.info("snoop: " + s);
        }
    }
    
    public void read(OzConnection handler, int bytesRead, ByteBuffer readBuffer) {
        if (bytesRead > 0) {
            print(OzUtil.byteBufferDebugDump("read bytes=" + bytesRead, readBuffer, true));
        } else {
            print("read bytes=" + bytesRead);
        }
    }

    public void input(OzConnection handler, ByteBuffer buffer, boolean matched) {
        print(OzUtil.byteBufferDebugDump("input matched=" + matched, buffer, false));
    }
    
    public void write(OzConnection handler, ByteBuffer buffer) {
        print(OzUtil.byteBufferDebugDump("write", buffer, false));
    }

    public void wrote(OzConnection handler, int wrote) {
        print("wrote bytes=" + wrote);
    }
}
