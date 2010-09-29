/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.zimbra.common.util.EmailUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import org.apache.commons.codec.binary.Base64;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.datasource.DataSourceManager;

/**
 * @author schemers
 */
public class DataSource extends AccountProperty {

    private static final int SALT_SIZE_BYTES = 16;
    private static final int AES_PAD_SIZE = 16;
    private static final byte[] VERSION = { 1 };
    private static final String SIMPLE_CLASS_NAME =
        StringUtil.getSimpleClassName(DataSource.class.getName());

    public enum Type {
        pop3, imap, caldav, contacts, yab, rss, cal, gal, xsync;
        
        public static Type fromString(String s) throws ServiceException {
            try {
                return Type.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid type: " + s + ", valid values: " + Arrays.asList(Type.values()), e); 
            }
        }
    }

    public enum ConnectionType {
        cleartext, ssl, tls, tls_if_available;
        
        public static ConnectionType fromString(String s) throws ServiceException {
            try {
                return ConnectionType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid type: " + s + ", valid values: " + Arrays.asList(ConnectionType.values()), e); 
            }
        }
    }
    
    public interface DataImport {
        /**
         * Tests connecting to a data source.  
         * @throws ServiceException if an error occurred
         */
        public abstract void test() throws ServiceException;

        public abstract void importData(List<Integer> folderIds, boolean fullSync)
            throws ServiceException;
    }

    public static final String CT_CLEARTEXT = "cleartext";
    public static final String CT_SSL = "ssl";

    private Type mType;
    private DataSourceConfig.Service knownService;

    public DataSource(Account acct, Type type, String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(acct, name, id, attrs, null, prov);
        mType = type;

        initKnownService();
    }

    private void initKnownService() {
        final String serviceName;
        final String host = getHost();

        // Google Apps domain could be anything, but host is always imap.gmail.com or imap.googlemail.com
        if (host != null && (host.endsWith(".gmail.com") || host.endsWith(".googlemail.com")))
            serviceName = "gmail.com";
        else
            serviceName = getDomain();

        knownService = serviceName == null ? null : DataSourceManager.getConfig().getService(serviceName);
    }

    public Type getType() {
        return mType;
    }

    /**
     * Returns true if this data source is currently being managed by
     * DataSourceManager. Can be used to check if data source or account
     * has been removed while importing so we can abort sync.
     *  
     * @return true if data source is currently being managed
     */
    public boolean isManaged() {
        return DataSourceManager.isManaged(this);
    }
    
    public boolean isEnabled() { return getBooleanAttr(Provisioning.A_zimbraDataSourceEnabled, false); }

    public ConnectionType getConnectionType() {
        // First check for data source attribute
        String value = getAttr(Provisioning.A_zimbraDataSourceConnectionType);
        if (value == null) {
            // If data source attribute not found, use global setting
            try {
                value = Provisioning.getInstance().getConfig().getDataSourceConnectionTypeAsString();
            } catch (ServiceException e) {
                // Fall through...
            }
        }
        if (value != null) {
            try {
                return ConnectionType.valueOf(value);
            } catch (IllegalArgumentException e) {
                ZimbraLog.mailbox.warn("Illegal connection type: " + value);
            }
        }
        return ConnectionType.cleartext;
    }

    public boolean isSslEnabled() {
        return getConnectionType() == ConnectionType.ssl;
    }
    
    public int getFolderId() { return getIntAttr(Provisioning.A_zimbraDataSourceFolderId, -1); }
    
    public String getHost() { return getAttr(Provisioning.A_zimbraDataSourceHost); }
    
    public String getUsername() { return getAttr(Provisioning.A_zimbraDataSourceUsername); }
    
    public String getAuthId() { return getAttr(Provisioning.A_zimbraDataSourceAuthorizationId); }
    
    public String getAuthMechanism() { return getAttr(Provisioning.A_zimbraDataSourceAuthMechanism); }
    
    public String getDomain() {
        String domain = getAttr(Provisioning.A_zimbraDataSourceDomain, null);
        if (domain == null) {
            domain = EmailUtil.getValidDomainPart(getEmailAddress());
        }
        return domain;
    }

    public Integer getPort() {
        if (getAttr(Provisioning.A_zimbraDataSourcePort) == null) {
            return null;
        }
        return getIntAttr(Provisioning.A_zimbraDataSourcePort, -1);
    }
    
    public String getDecryptedPassword() throws ServiceException {
        String data = getAttr(Provisioning.A_zimbraDataSourcePassword);
        return data == null ? null : decryptData(getId(), data); 
    }

    public Integer getConnectTimeout(int defaultValue) {
        return getIntAttr(Provisioning.A_zimbraDataSourceConnectTimeout, defaultValue);
    }

    public int getReadTimeout(int defaultValue) {
        return getIntAttr(Provisioning.A_zimbraDataSourceReadTimeout, defaultValue);
    }

    public int getMaxTraceSize() {
        return getIntAttr(Provisioning.A_zimbraDataSourceMaxTraceSize, 64);
    }

    /**
     * Returns the polling interval in milliseconds.  If <tt>zimbraDataSourcePollingInterval</tt>
     * is not specified on the data source, uses the value set for the account.  If not
     * set on either the data source or account, returns <tt>0</tt>.
     */
    public long getPollingInterval() throws ServiceException {
        long interval;
        String val = getAttr(Provisioning.A_zimbraDataSourcePollingInterval);
        Provisioning prov = Provisioning.getInstance();
        Account account = getAccount(prov);

        // Get interval from data source or account.
        if (val != null) {
            interval = getTimeInterval(Provisioning.A_zimbraDataSourcePollingInterval, 0);
        } else {
            migratePollingIntervalIfNecessary(prov, account);
            switch(getType()) {
            case pop3:
                interval = account.getDataSourcePop3PollingInterval();
                break;
            case imap:
                interval = account.getDataSourceImapPollingInterval();
                break;
            case rss:
                interval = account.getDataSourceRssPollingInterval();
                break;
            case caldav:
                interval = account.getDataSourceCaldavPollingInterval();
                break;
            case yab:
                interval = account.getDataSourceYabPollingInterval();
                break;
            case cal:
                interval = account.getDataSourceCalendarPollingInterval();
                break;
            case gal:
                interval = account.getDataSourceGalPollingInterval();
                break;
            default:
                return 0;
            }
        }
        
        if (interval < 0) {
            // Getters return -1 when the interval is not set.
            return 0;
        }

        // Don't allow anyone to poll more frequently than zimbraDataSourceMinPollingInterval
        // or 10 seconds, whichever is greater.
        long min = account.getDataSourceMinPollingInterval();
        long safeguard = 10 * Constants.MILLIS_PER_SECOND;
        if (min < safeguard) {
            min = safeguard;
        }
        if (0 < interval && interval < min) {
            interval = min;
        }
        
        return interval;
    }
    
    /**
     * Migrates the old <tt>zimbraDataSourcePollingInterval</tt> on account to
     * <tt>zimbraDataSourcePop3PollingInterval</tt> and <tt>zimbraDataSourceImapPollingInterval</tt>.
     * Runs only once per account.  This code can be removed after 6.0. 
     */
    private void migratePollingIntervalIfNecessary(Provisioning prov, Account account)
    throws ServiceException {
        // Migrate Account value.
        String oldInterval = account.getAttr(Provisioning.A_zimbraDataSourcePollingInterval, false);
        if (!StringUtil.isNullOrEmpty(oldInterval)) {
            ZimbraLog.datasource.info("Migrating account POP3 and IMAP polling intervals to %s.", oldInterval);
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, "");
            attrs.put(Provisioning.A_zimbraDataSourcePop3PollingInterval, oldInterval);
            attrs.put(Provisioning.A_zimbraDataSourceImapPollingInterval, oldInterval);
            prov.modifyAttrs(account, attrs, true, false); // Don't run callback so we don't trigger database code.
        }
        
        // Migrate Cos value.
        Cos cos = account.getCOS();
        oldInterval = cos.getAttr(Provisioning.A_zimbraDataSourcePollingInterval, false);
        if (!StringUtil.isNullOrEmpty(oldInterval)) {
            ZimbraLog.datasource.info("Migrating COS POP3 and IMAP polling intervals to %s.", oldInterval);
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, "");
            attrs.put(Provisioning.A_zimbraDataSourcePop3PollingInterval, oldInterval);
            attrs.put(Provisioning.A_zimbraDataSourceImapPollingInterval, oldInterval);
            prov.modifyAttrs(cos, attrs, true, false);  // Don't run callback so we don't trigger database code.
        }
    }
    
    /**
     * Returns <tt>true</tt> if this data source has a scheduled poll interval.
     * @see #getPollingInterval
     */
    public boolean isScheduled() throws ServiceException {
        return getPollingInterval() > 0;
    }

    /**
     * Should POP3 messages be left on the server or deleted?  Default
     * is <code>true</code> for data sources created before the leave on
     * server feature was implemented. 
     */
    public boolean leaveOnServer() {
        return getBooleanAttr(Provisioning.A_zimbraDataSourceLeaveOnServer, true);
    }
    
    public String getEmailAddress() {
        return getAttr(Provisioning.A_zimbraDataSourceEmailAddress);
    }
    
    public boolean useAddressForForwardReply() {
        return getBooleanAttr(Provisioning.A_zimbraDataSourceUseAddressForForwardReply, false);
    }
    
    public String getDefaultSignature() {
        return getAttr(Provisioning.A_zimbraPrefDefaultSignatureId);
    }
    
    public String getFromDisplay() {
        return getAttr(Provisioning.A_zimbraPrefFromDisplay);
    }  
    
    public String getFromAddress() {
        return getAttr(Provisioning.A_zimbraPrefFromAddress);
    }
    
    public String getReplyToAddress() {
        return getAttr(Provisioning.A_zimbraPrefReplyToAddress);
    } 
    
    public String getReplyToDisplay() {
        return getAttr(Provisioning.A_zimbraPrefReplyToDisplay);
    }
    
    boolean isRequestScopeDebugTraceOn = false;
    public synchronized void setRequestScopeDebugTraceOn(boolean b) {
        isRequestScopeDebugTraceOn = b;
    }
    
    public boolean isImportOnly() {
        return getBooleanAttr(Provisioning.A_zimbraDataSourceImportOnly, false);
    }

    public boolean isInternal() {
        return getBooleanAttr(Provisioning.A_zimbraDataSourceIsInternal, false);
    }
    
    public boolean isDebugTraceEnabled() {
    	return isRequestScopeDebugTraceOn || getBooleanAttr(Provisioning.A_zimbraDataSourceEnableTrace, false);
    }
    
    //IMAP datasources can override these
        
    protected DataSourceConfig.Folder getKnownFolderByRemotePath(String remotePath) {
        return knownService != null ? knownService.getFolderByRemotePath(remotePath) : null;
    }

    protected DataSourceConfig.Folder getKnownFolderByLocalPath(String localPath) {
        return knownService != null ? knownService.getFolderByLocalPath(localPath) : null;
    }

    /**
     * Map well known remote path to a local path if mapping exists.
     *
     * @param remotePath remote path
     * @return local path if mapping exists; null if not
     */
    public String mapRemoteToLocalPath(String remotePath) {
        DataSourceConfig.Folder kf = getKnownFolderByRemotePath(remotePath);
        return kf != null ? kf.getLocalPath() : null;
    }

    /**
     * Map local path to a well known remote path if mapping exists.
     *
     * @param localPath local path
     * @return remote path if mapping exists ; null if not
     */
    public String mapLocalToRemotePath(String localPath) {
        DataSourceConfig.Folder kf = getKnownFolderByLocalPath(localPath);
        return kf != null ? kf.getRemotePath() : null;
    }

    /**
     * Returns true if remote path should be ignored.
     */
    public boolean ignoreRemotePath(String remotePath) {
        DataSourceConfig.Folder kf = getKnownFolderByRemotePath(remotePath);
        if (kf != null) return kf.isIgnore();
        // Also ignore remote path that would conflict with known local path
        String localPath = "/" + remotePath;
        return getKnownFolderByLocalPath(localPath) != null;
    }

    public boolean isSyncInboxOnly() {
    	return false;
    }
    
    public boolean isSyncEnabled(String localPath) {
    	return true;
    }

    // Overridden in OfflineDataSource
    public boolean isSaveToSent() {
        return true;
    }

    public boolean isOffline() {
        return false;
    }

    @SuppressWarnings("unused")
    public boolean checkPendingMessages() throws ServiceException {
        // Does nothing for online
        return false;
    }

    public long getSyncFrequency() {
        return 0;
    }

    public void reportError(int itemId, String error, Exception e) {
        // Do nothing by default...
    }
    
    private static byte[] randomSalt() {
        SecureRandom random = new SecureRandom();
        byte[] pad = new byte[SALT_SIZE_BYTES];
        random.nextBytes(pad);
        return pad;
    }

    private static Cipher getCipher(String dataSourceId, byte[] salt, boolean encrypt) throws GeneralSecurityException, UnsupportedEncodingException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(salt);
        md5.update(dataSourceId.getBytes("utf-8"));
        byte[] key = md5.digest();
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, skeySpec);
        return cipher;
    }
    
    public static String encryptData(String dataSourceId, String data) throws ServiceException {
        try {
            byte[] salt = randomSalt();
            Cipher cipher = getCipher(dataSourceId, salt, true);
            byte[] dataBytes = cipher.doFinal(data.getBytes("utf-8"));
            byte[] toEncode = new byte[VERSION.length + salt.length + dataBytes.length];
            System.arraycopy(VERSION, 0, toEncode, 0, VERSION.length);            
            System.arraycopy(salt, 0, toEncode, VERSION.length, salt.length);
            System.arraycopy(dataBytes, 0, toEncode, VERSION.length+salt.length, dataBytes.length); 
            return new String(Base64.encodeBase64(toEncode));
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.FAILURE("caught unsupport encoding exception", e);
        } catch (GeneralSecurityException e) {
            throw ServiceException.FAILURE("caught security exception", e); 
        }
    }
    
    public static String decryptData(String dataSourceId, String data) throws ServiceException {
        try {
            byte[] encoded = Base64.decodeBase64(data.getBytes());
            if (encoded.length < VERSION.length + SALT_SIZE_BYTES + AES_PAD_SIZE)
                throw ServiceException.FAILURE("invalid encoded size: "+encoded.length, null);
            byte[] version = new byte[VERSION.length];
            byte[] salt = new byte[SALT_SIZE_BYTES];
            System.arraycopy(encoded, 0, version, 0, VERSION.length);
            if (!Arrays.equals(version, VERSION))
                throw ServiceException.FAILURE("unsupported version", null);            
            System.arraycopy(encoded, VERSION.length, salt, 0, SALT_SIZE_BYTES);
            Cipher cipher = getCipher(dataSourceId, salt, false);
            return new String(cipher.doFinal(encoded, VERSION.length + SALT_SIZE_BYTES, encoded.length - SALT_SIZE_BYTES - VERSION.length), "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw ServiceException.FAILURE("caught unsupport encoding exception", e);
        } catch (GeneralSecurityException e) {
            throw ServiceException.FAILURE("caught security exception", e); 
        }
    }

    public boolean isSyncEnabled(Folder folder) {
        return DataSourceManager.getInstance().isSyncEnabled(this, folder);
    }

    public boolean isSyncCapable(Folder folder) {
        return DataSourceManager.getInstance().isSyncCapable(this, folder);
    }

    public boolean isSyncNeeded() throws ServiceException {
        return false;
    }

    public void mailboxDeleted() {}
    
    public Mailbox getMailbox() throws ServiceException {
        return DataSourceManager.getInstance().getMailbox(this);
    }

    public String toString() {
        List<String> parts = new ArrayList<String>();
        parts.add("id=" + getId());
        parts.add("type=" + getType());
        parts.add("isEnabled=" + isEnabled());
        if (getName() != null) {
            parts.add("name=" + getName());
        }
        if (getHost() != null) {
            parts.add("host=" + getHost());
        }
        if (getPort() != null) {
            parts.add("port=" + getPort());
        }
        if (getConnectionType() != null) {
            parts.add("connectionType=" + getConnectionType().name());
        }
        if (getUsername() != null) {
            parts.add("username=" + getUsername());
        }
        parts.add("folderId=" + getFolderId());
        return String.format("%s: { %s }",
            SIMPLE_CLASS_NAME, StringUtil.join(", ", parts));
    }

    public static void main(String args[]) throws ServiceException {
        String dataSourceId = UUID.randomUUID().toString();
        String enc = encryptData(dataSourceId, "helloworld");
        System.out.println(enc);
        System.out.println(decryptData(dataSourceId, enc));
    }
}
