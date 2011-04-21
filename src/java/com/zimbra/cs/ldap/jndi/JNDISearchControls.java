package com.zimbra.cs.ldap.jndi;

import javax.naming.directory.SearchControls;

import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchScope;

public class JNDISearchControls extends ZSearchControls {

    // the wrapped JNDI SearchControls object
    final private SearchControls wrapped;
    
    JNDISearchControls(ZSearchScope searchScope, int sizeLimit, String[] returnAttrs) {
        wrapped = new SearchControls(((JNDISearchScope) searchScope).get(), sizeLimit, 
                TIME_UNLIMITED, returnAttrs, false, false);
    }
    
    // get the wrapped object
    SearchControls get() {
        return wrapped;
    }

}
