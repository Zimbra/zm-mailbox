/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
