package com.zimbra.cs.service.admin;

import java.util.List;

import com.zimbra.cs.account.accesscontrol.AdminRight;

public interface AdminRightCheckPoint {

    /**
     * canned notes
     */
    static class Notes {
        // only system admins are allowed
        public static final String SYSTEM_ADMINS_ONLY = "Only system admins are allowed.";
        
        // no right is needed, any admin can do it.
        public static final String ALLOW_ALL_ADMINS = "Do not need any right, all admins are allowed.";
   
        // in the end no one should refer to this string
        protected static final String TODO = "TDB";
                
        protected static final String GET_ENTRY =
            "Attributes that are not allowed to be get by the authenticated admin will be returned " +
            "as <a n=\"{attr-name}\" pd=\"1\"/>." +
            "To allow an admin to get all attributes, grant the %s right";
        
        protected static final String MODIFY_ENTRY = 
            "All attrs provided in the attribute list have to settable by. " + 
            "the authed admin.   You can grant the %s right, which allows " + 
            "setting all attributes on %s, or grant the set attrs right just " +
            "for the attributes the admin needs to set while creating an entry.";
        
        protected static final String LIST_ENTRY = 
            "If the authed admin does not have corresponding list{Entry} right " +
            "or get{Entry} right for an entry that fulfill the list/search criteria, " +
            "the entry is skipped in the getAllXXX/searchXXX/searchDirectoryResponse,  " +
            "no PERM_DENIED exception will be thrown.";

        
    }
    
    public void docRights(List<AdminRight> relatedRights, List<String> notes);
}
