package com.zimbra.cs.ldap.jndi;

import javax.naming.directory.SearchControls;

import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchScope;

public class JNDISearchControls extends ZSearchControls {

    // the wrapped JNDI SearchControls object
    final private SearchControls wrapped;
    
    private JNDISearchControls(JNDISearchScope searchScope) {
        wrapped = new SearchControls(searchScope.get(), 0, 0, null, false, false);
    }
    
    // get the wrapped object
    SearchControls get() {
        return wrapped;
    }
    
    public static class JNDISearchControlsFactory extends ZSearchControlsFactory {
        protected ZSearchControls getSearchControl(ZSearchScope searchScope) {
            return new JNDISearchControls((JNDISearchScope)searchScope);
        }
    }
}
