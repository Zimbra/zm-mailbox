/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Entry {

    public String getAttr(String name);

    public String[] getMultiAttr(String name);    

    public String getAttr(String name, String defaultValue);

    /**
     * place all the values for the specified attr in a set and return it.
     * @param name
     * @return
     */
    public Set getMultiAttrSet(String name);

    public Map getAttrs() throws ServiceException;
    

    /**
     * modifies the specified attrs. attrs should be a map consisting of keys that are Strings, and values that are
     * either: null (in which case the attr is removed), a String (in which case the attr is modified), or a String[],
     * (in which case a multi-valued attr is updated).
     * 
     * calls modifyAttrs(attrs, false). 
     * 
     * @param attrs
     * @throws NamingException
     */
    public void modifyAttrs(Map attrs) throws ServiceException;
    

    /**
     * modifies the specified attrs. attrs should be a map consisting of keys that are Strings, and values that are
     * either: null (in which case the attr is removed), a String (in which case the attr is modified), or a String[],
     * (in which case a multi-valued attr is updated).
     * @param attrs 
     * @param checkImmutable if set to true, don't allow attributes marked as immutable to be modified.
     * @throws NamingException
     * 
     */
    public void modifyAttrs(Map attrs, boolean checkImmutable) throws ServiceException;
/**
     * 
     * @param name name of the attribute to retreive. 
     * @param defaultValue value to use if attr is not present
     * @return
     */
    public boolean getBooleanAttr(String name, boolean defaultValue);

    /**
     * immediately updates the specified boolean attribute. Use modifyAttrs if you need to update more then one attr.
     * 
     * @param name name of the attribute to set/update
     * @param value
     */
    public void setBooleanAttr(String name, boolean value) throws ServiceException;

    /**
     * 
     * @param name name of the attribute to retreive. 
     * @param defaultValue value to use if attr is not present or can't be parsed.
     * @return
     */
    public int getIntAttr(String name, int defaultValue);

    /**
     * 
     * @param name name of the attribute to retreive. 
     * @param defaultValue value to use if attr is not present or can't be parsed.
     * @return
     */
    public long getLongAttr(String name, long defaultValue);

    /**
     * get a time interval, which is a number, optional followed by a character denoting the units
     * (d = days, h = hours, m = minutes, s = seconds. If no character unit is specified, the default is
     * seconds.
     * 
     * the time interval is returned in miliseconds.
     * 
     * @param name name of the attribute to retreive. 
     * @param defaultValue value to use if attr is not present or can't be parsed.
     * @return interval in milliseconds
     * @throws AccountServiceException 
     */
    public long getTimeInterval(String name, long defaultValue) throws AccountServiceException;

    /**
     * 
     * @param name name of the attribute to retreive. 
     * @param defaultValue value to use if attr is not present or can't be parsed.
     * @return
     */
    public Date getGeneralizedTimeAttr(String name, Date defaultValue);    
    
    /**
     * reload/refresh the entry.
     */
    public void reload() throws ServiceException;
    
    /**
     * temporarily associate a key/value pair with this entry. When an entry is reloaded, any cached data is cleared.
     * @param key
     * @param value
     */
    public void setCachedData(Object key, Object value);

    /**
     * get an entry from the cache.
     * @param key
     * @return
     */
    public Object getCachedData(Object key);
}
