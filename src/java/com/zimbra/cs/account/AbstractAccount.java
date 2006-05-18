package com.zimbra.cs.account;

import java.util.Date;
import java.util.List;
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

    public Cos getCOS() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public CalendarUserType getCalendarUserType() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public Set<String> getDistributionLists() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public List<DistributionList> getDistributionLists(boolean directOnly,
            Map<String, String> via) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public Domain getDomain() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public String getDomainName() {
        throw new UnsupportedOperationException();        
    }

    public Server getServer() throws ServiceException {
        throw new UnsupportedOperationException();        
    }

    public ICalTimeZone getTimeZone() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public String getUid() {
        throw new UnsupportedOperationException();
    }

    public boolean inDistributionList(String zimbraId) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public boolean isCorrectHost() throws ServiceException {
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

    public long getTimeInterval(String name, long defaultValue)
            throws AccountServiceException {
        throw new UnsupportedOperationException();
    }

    public void modifyAttrs(Map<String, ? extends Object> attrs)
            throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public void modifyAttrs(Map<String, ? extends Object> attrs,
            boolean checkImmutable) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public void reload() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public void setBooleanAttr(String name, boolean value)
            throws ServiceException {
        throw new UnsupportedOperationException();
    }

    public void setCachedData(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    public int compareTo(Object o) {
        throw new UnsupportedOperationException();
    }
}
