package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;

public interface AliasedEntry {

    /*
     * entry with aliases
     */
    
    /**
     * returns whether addr is the entry's primary address or 
     * one of the aliases.
     * 
     * @param addr
     * @return
     */
    public boolean isAddrOfEntry(String addr);
    
    public String[] getAliases() throws ServiceException;
}
