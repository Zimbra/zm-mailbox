package com.zimbra.cs.account;

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
}
