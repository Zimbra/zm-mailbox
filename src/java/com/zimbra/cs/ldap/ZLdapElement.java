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
    
    public static interface ZLdapElementDebugListener {
        void print(String txt);
        void println(String txt);
        void printStackTrace(Throwable e);
    }
    
    // a ZLdapElementDebugListener that dumps the debug info to a String
    public static class StringLdapElementDebugListener implements ZLdapElementDebugListener {
        private StringBuilder buffer = new StringBuilder();
        
        @Override
        public void print(String txt) {
            buffer.append(txt);
        }

        @Override
        public void println(String txt) {
            buffer.append(txt + "\n");
        }
        
        @Override
        public void printStackTrace(Throwable e) {
            buffer.append(e.toString()); // Throwable.toString() returns a short description of this throwable.
            
            // also log it
            ZimbraLog.ldap.debug(e);
        }
        
        public String getString() {
            return buffer.toString();
        }
        
    }

    public static final ZLdapElementDebugListener LOG_DEBUG_LISTENER =
        new ZLdapElementDebugListener() {

            @Override
            public void print(String txt) {
                ZimbraLog.ldap.debug(txt);
            }
        
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
            public void print(String txt) {
                System.out.print(txt);
            }
        
            @Override
            public void println(String txt) {
                System.out.println(txt);
            }

            @Override
            public void printStackTrace(Throwable e) {
                e.printStackTrace();
            }
    };

    
    private static ZLdapElementDebugListener DEFAULT_DEBUG_LISTENER = STDOUT_DEBUG_LISTENER;
    
    public static synchronized void setDefaultDebugListener(ZLdapElementDebugListener dbgListener) {
        DEFAULT_DEBUG_LISTENER = dbgListener;
    }
    
    protected void print(String txt) {
        print(null, txt);
    }
    
    protected void print(ZLdapElementDebugListener debugListener, String txt) {
        if (debugListener == null) {
            debugListener = DEFAULT_DEBUG_LISTENER;
        }
        debugListener.print(txt);
    }
    
    protected void println(String txt) {
        println(null, txt);
    }
    
    protected void println(ZLdapElementDebugListener debugListener, String txt) {
        if (debugListener == null) {
            debugListener = DEFAULT_DEBUG_LISTENER;
        }
        debugListener.println(txt);
    }
    
    protected void printStackTrace(Throwable e) {
        printStackTrace(null, e);
    }
    
    protected void printStackTrace(ZLdapElementDebugListener debugListener, Throwable e) {
        if (debugListener == null) {
            debugListener = DEFAULT_DEBUG_LISTENER;
        }
        debugListener.printStackTrace(e);
    }
    
    public void debug() {
        debug(null);
    }

    public void debug(ZLdapElementDebugListener debugListener) {
        print(debugListener, "NO DEBUG INFO");
    }
}
