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

package com.zimbra.cs.tcpserver;

import EDU.oswego.cs.dl.util.concurrent.ThreadFactory;

public class TcpThreadFactory implements ThreadFactory {
	private int mCount;
	private String mPrefix;
	private boolean mIsDaemon;
    private int mPriority;

	public TcpThreadFactory(String prefix, boolean isDaemon) {
        this(prefix, isDaemon, Thread.NORM_PRIORITY);
	}

    public TcpThreadFactory(String prefix, boolean isDaemon, int priority) {
        mCount = 0;
        mPrefix = prefix;
        mIsDaemon = isDaemon;
        mPriority = priority;
    }

	public Thread newThread(Runnable runnable) {
        int n;
        synchronized (this) {
            n = ++mCount;
        }
        StringBuffer sb = new StringBuffer(mPrefix);
        sb.append('-').append(n);
        Thread t = new Thread(runnable, sb.toString());
        t.setDaemon(mIsDaemon);
        t.setPriority(mPriority);
        return t;
	}
}
