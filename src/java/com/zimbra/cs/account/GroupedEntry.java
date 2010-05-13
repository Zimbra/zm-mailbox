package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;

public interface GroupedEntry {
    
    /**
     * returns all addressed of this entry that can be identified as 
     * a member in a group.
     * 
     * @return
     */
    public String[] getAllAddrsAsGroupMember() throws ServiceException;
}
