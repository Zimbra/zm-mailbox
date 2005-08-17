package com.liquidsys.coco.account;

import java.util.Map;

import com.liquidsys.coco.mailbox.calendar.ICalTimeZone;
import com.liquidsys.coco.service.ServiceException;

/**
 * @author schemers
 */
public interface Account extends NamedEntry {
    
    public String getUid();
    
    /**
     * @return the domain name for this account (foo.com), or null if an admin account. 
     */
    public String getDomainName();
    
    /**
     * @return the domain this account, or null if an admin account. 
     * @throws ServiceException
     */    
    public Domain getDomain() throws ServiceException;    
    
    public String getAccountStatus();
    
    /**
     * combines all liquidPref* attributes from the Account and the COS and returns as a map. Account preferences
     * override COS preferences.
     *  
     * @param prefsOnly return only liquidPref* attrs.
     * @param applyCos apply COS attributes
     * @return
     * @throws ServiceException
     */
    public Map getAttrs(boolean prefsOnly, boolean applyCos) throws ServiceException;    
    

    /**
     * @return the COS object for this account, or null if account has no COS
     * 
     * @throws ServiceException
     */
    public Cos getCOS() throws ServiceException;
   
    public String[] getAliases() throws ServiceException;
    
    /**
     * check whether this account's mailbox is supposed to be on this host
     * 
     * @throws ServiceException
     */
    public boolean isCorrectHost() throws ServiceException;

    /**
     * Returns account's time zone
     * @return
     * @throws ServiceException
     */
    public ICalTimeZone getTimeZone() throws ServiceException;
}
