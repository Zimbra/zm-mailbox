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

import java.io.PrintStream;
import java.nio.ByteBuffer;

public class OzSnooper {
    
    public static final int NONE = 0;
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int INPUT = 4;
    
    public static final int ALL = 0xFFFFFFFF;
    
    private boolean mSnoopReads;
    private boolean mSnoopWrites;
    private boolean mSnoopInputs;
    
    private PrintStream mPS = null;

    public OzSnooper(PrintStream ps) {
        mPS = ps;
    }

    public OzSnooper(PrintStream ps, int mode) {
        mPS = ps;
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
        synchronized (mPS) {
            mPS.println("[snoop] [" + Thread.currentThread().getName() + "] " + s);
        }
    }
    
    public void read(OzConnectionHandler handler, int bytesRead, ByteBuffer readBuffer) {
        if (bytesRead > 0) {
            print(OzUtil.byteBufferToString("read cid=" + handler.getId() + " bytes=" + bytesRead, readBuffer, true));
        } else {
            print("read cid=" + handler.getId() + " bytes=" + bytesRead);
        }
    }

    public void input(OzConnectionHandler handler, ByteBuffer buffer, boolean matched) {
        print(OzUtil.byteBufferToString("input cid=" + handler.getId() + " matched=" + matched, buffer, false));
    }
    
    public void write(OzConnectionHandler handler, ByteBuffer buffer) {
        print(OzUtil.byteBufferToString("write cid=" + handler.getId(), buffer, false));
    }

    public void wrote(OzConnectionHandler handler, int wrote) {
        print("wrote cid=" + handler.getId() + " bytes=" + wrote);
    }

}
