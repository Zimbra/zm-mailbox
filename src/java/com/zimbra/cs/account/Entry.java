/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    public Set<String> getMultiAttrSet(String name);

    public Map<String, Object> getAttrs() throws ServiceException;
    
    /**
     * Modifies this entry.  <code>attrs</code> is a <code>Map</code> consisting of
     * keys that are <code>String</code>s, and values that are either
     * <ul>
     *   <li><code>null</code>, in which case the attr is removed</li>
     *   <li>a single <code>Object</code>, in which case the attr is modified
     *     based on the object's <code>toString()</code> value</li>
     *   <li>an <code>Object</code> array or <code>Collection</code>,
     *     in which case a multi-valued attr is updated</li>
     * </ul>
     * 
     * Calls {@link #modifyAttrs(Map, boolean)} with <code>checkImmutable=false</code>.
     */
    public void modifyAttrs(Map<String, ? extends Object> attrs) throws ServiceException;
    
    /**
     * Modifies this entry.  <code>attrs</code> is a <code>Map</code> consisting of
     * keys that are <code>String</code>s, and values that are either
     * <ul>
     *   <li><code>null</code>, in which case the attr is removed</li>
     *   <li>a single <code>Object</code>, in which case the attr is modified
     *     based on the object's <code>toString()</code> value</li>
     *   <li>an <code>Object</code> array or <code>Collection</code>,
     *     in which case a multi-valued attr is updated</li>
     * </ul>
     */
    public void modifyAttrs(Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException;

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
     */
    public long getTimeInterval(String name, long defaultValue);

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

    /**
     * return the entry's Locale
     * @return
     * @throws ServiceException
     */
    public Locale getLocale() throws ServiceException ;
}
