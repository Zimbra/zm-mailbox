package com.zimbra.cs.ldap;

public abstract class ZSearchControls {

    public static final int SIZE_UNLIMITED  = 0;
    public static final int TIME_UNLIMITED  = 0;
    public static final String[] RETURN_ALL_ATTRS = null;
    
    /*
     * Note: In additional to the params that can be specified, all ZSearchControls 
     * have the following properties:
     * - no time limit
     * - do not dereference links during search
     */
    
    public static ZSearchControls SEARCH_CTLS_SUBTREE() {
        return createSearchControls(ZSearchScope.SEARCH_SCOPE_SUBTREE,
                SIZE_UNLIMITED, RETURN_ALL_ATTRS);
    }
    
    public static ZSearchControls createSearchControls(ZSearchScope searchScope,
            int sizeLimit, String[] returnAttrs) {
        return LdapClient.getInstance().createSearchControlsImpl(
                ZSearchScope.SEARCH_SCOPE_SUBTREE, sizeLimit, returnAttrs);
    }


}

