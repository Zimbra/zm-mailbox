/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ldap;

import com.zimbra.common.util.ZimbraLog;

public abstract class ZLdapElement {
    
    public static final ZLdapElementDebugListener LOG_DEBUG_LISTENER =
        new ZLdapElementDebugListener() {

            @Override
            public void println(String txt) {
                ZimbraLog.ldap.debug(txt);
            }

            @Override
            public void printStackTrace(Throwable e) {
                ZimbraLog.ldap.debug(e);
            }
    };
    
    public static final ZLdapElementDebugListener STDOUT_DEBUG_LISTENER =
        new ZLdapElementDebugListener() {

            @Override
            public void println(String txt) {
                System.out.println(txt);
            }

            @Override
            public void printStackTrace(Throwable e) {
                e.printStackTrace();
            }
    };
    
    public static interface ZLdapElementDebugListener {
        void println(String txt);
        void printStackTrace(Throwable e);
    }
    
    private static ZLdapElementDebugListener debugListener = STDOUT_DEBUG_LISTENER;
    
    public static synchronized void setDebugListener(ZLdapElementDebugListener dbgListener) {
        debugListener = dbgListener;
    }
    
    protected void println(String txt) {
        debugListener.println(txt);
    }
    
    protected void printStackTrace(Throwable e) {
        debugListener.printStackTrace(e);
    }
    
    public abstract void debug();
}
