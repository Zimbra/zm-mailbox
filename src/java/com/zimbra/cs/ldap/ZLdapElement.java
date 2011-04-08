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
