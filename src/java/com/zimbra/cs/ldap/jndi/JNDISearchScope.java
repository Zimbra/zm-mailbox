package com.zimbra.cs.ldap.jndi;

import javax.naming.directory.SearchControls;

import com.zimbra.cs.ldap.ZSearchScope;

public class JNDISearchScope extends ZSearchScope {

    // the wrapped JNDI search scope 
    final private int wrapped;
    
    private JNDISearchScope(int searchScope) {
        this.wrapped = searchScope;
    }
    
    // get the wrapped value
    int get() {
        return wrapped;
    }
    
    public static class JNDISearchScopeFactory extends ZSearchScope.ZSearchScopeFactory {
        @Override
        protected ZSearchScope getObjectSearchScope() {
            return new JNDISearchScope(SearchControls.OBJECT_SCOPE);
        }
        
        @Override
        protected ZSearchScope getOnelevelSearchScope() {
            return new JNDISearchScope(SearchControls.ONELEVEL_SCOPE);
        }
        
        @Override
        protected ZSearchScope getSubtreeSearchScope() {
            return new JNDISearchScope(SearchControls.SUBTREE_SCOPE);
        }
    }


}
