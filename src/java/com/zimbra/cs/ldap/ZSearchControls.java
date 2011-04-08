package com.zimbra.cs.ldap;

public abstract class ZSearchControls {

    /*
     * All canned search controls have the following properties:
     * - no size limit
     * - no time limit
     * - requesting all attributes
     * - do not dereference links during search
     */
    
    public static ZSearchControls SEARCH_CTLS_OBJECT;
    public static ZSearchControls SEARCH_CTLS_SUBTREE;
    
    public abstract static class ZSearchControlsFactory {
        protected abstract ZSearchControls getSearchControl(ZSearchScope searchScope);
    }
    
    public static void init(ZSearchControlsFactory factory) {
        SEARCH_CTLS_OBJECT = factory.getSearchControl(ZSearchScope.SEARCH_SCOPE_OBJECT);
        SEARCH_CTLS_SUBTREE = factory.getSearchControl(ZSearchScope.SEARCH_SCOPE_SUBTREE);
    }
    
}

