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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.service.ServiceException;

/**
 * classes that implement Account can extend this abstract class, all methods throw UnsupportOperationException by
 * default. New methods that get implemented in the Account interface will be added to this classes as well.
 * 
 */
public abstract class AbstractAccount implements Account {

    public String getAccountStatus() {
        throw new UnsupportedOperationException();
    }

    public String[] getAliases() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getAttrs(boolean prefsOnly, boolean applyCos)
            throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public CalendarUserType getCalendarUserType() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public String getDomainName() {
        throw new UnsupportedOperationException();        
    }
    
    public String getAccountCOSId() {
        throw new UnsupportedOperationException();        
    }

    public ICalTimeZone getTimeZone() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public String getUid() {
        throw new UnsupportedOperationException();
    }

    public boolean saveToSent() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public String getId() {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        throw new UnsupportedOperationException();
    }

    public String getAttr(String name) {
        throw new UnsupportedOperationException();
    }

    public String getAttr(String name, String defaultValue) {
        throw new UnsupportedOperationException();
    }

    public Map<String, Object> getAttrs() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public boolean getBooleanAttr(String name, boolean defaultValue) {
        throw new UnsupportedOperationException();
    }

    public Object getCachedData(Object key) {
        throw new UnsupportedOperationException();
    }

    public Date getGeneralizedTimeAttr(String name, Date defaultValue) {
        throw new UnsupportedOperationException();
    }

    public int getIntAttr(String name, int defaultValue) {
        throw new UnsupportedOperationException();
    }

    public Locale getLocale() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public long getLongAttr(String name, long defaultValue) {
        throw new UnsupportedOperationException();
    }

    public String[] getMultiAttr(String name) {
        throw new UnsupportedOperationException();
    }

    public Set<String> getMultiAttrSet(String name) {
        throw new UnsupportedOperationException();
    }

    public long getTimeInterval(String name, long defaultValue) {
        throw new UnsupportedOperationException();
    }

    public void setCachedData(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    public int compareTo(Object o) {
        throw new UnsupportedOperationException();
    }
}
