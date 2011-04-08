package com.zimbra.cs.ldap;

public abstract class ZSearchScope {
    // only the entry specified by the base DN should be considered.
    public static ZSearchScope SEARCH_SCOPE_OBJECT;
    
    // only entries that are immediate subordinates of the entry specified 
    // by the base DN (but not the base entry itself) should be considered.
    public static ZSearchScope SEARCH_SCOPE_ONELEVEL;
    
    // the base entry itself and any subordinate entries (to any depth) should be considered.
    public static ZSearchScope SEARCH_SCOPE_SUBTREE;
    
    public abstract static class ZSearchScopeFactory {
        protected abstract ZSearchScope getObjectSearchScope();
        protected abstract ZSearchScope getOnelevelSearchScope();
        protected abstract ZSearchScope getSubtreeSearchScope();
    }
    
    public static void init(ZSearchScopeFactory factory) {
        SEARCH_SCOPE_OBJECT = factory.getObjectSearchScope();
        SEARCH_SCOPE_ONELEVEL = factory.getOnelevelSearchScope();
        SEARCH_SCOPE_SUBTREE = factory.getSubtreeSearchScope();
    }

}
