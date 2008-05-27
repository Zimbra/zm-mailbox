package com.zimbra.cs.account;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * for group membership caching
 * 
 * Contains only "upward" memberships.  i.e. all direct and indirect groups this object is a member of
 * 
 * @author pshao
 *
 */
public class Group extends MailTarget {
    
    // ids of all direct and indirect groups this object is a member of
    private Map<String, String> mInGroups;
    
    public Group(String name, String id, List<DistributionList> inGroups) {
        super(name, id, null, null);
        
        if (inGroups != null) {
            mInGroups = new HashMap<String, String>();
            for (DistributionList dl : inGroups)
                mInGroups.put(dl.getId(), dl.getName());
            mInGroups = Collections.unmodifiableMap(mInGroups);
        }
    }
    
    public boolean isMemberOf(String id) {
        if (mInGroups == null)
            return false;
        else
            return (mInGroups.get(id) != null);
    }

}
