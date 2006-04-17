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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.InvalidAttributesException;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.*;
import com.zimbra.cs.account.ldap.LdapGroupEntryCache.LdapGroupEntry;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Constants;
import com.zimbra.cs.util.DateUtil;
import com.zimbra.cs.util.EmailUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.cs.zimlet.ZimletException;
import com.zimbra.cs.zimlet.ZimletUtil;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LdapProvisioning extends Provisioning {

    // object classes
    public static final String C_zimbraAccount = "zimbraAccount";
    public static final String C_amavisAccount = "amavisAccount";
    public static final String C_zimbraCOS = "zimbraCOS";
    public static final String C_zimbraDomain = "zimbraDomain";
    public static final String C_zimbraMailList = "zimbraDistributionList";
    public static final String C_zimbraMailRecipient = "zimbraMailRecipient";
    public static final String C_zimbraServer = "zimbraServer";
    public static final String C_zimbraCalendarResource = "zimbraCalendarResource";
    public static final String C_zimbraAlias = "zimbraAlias";

    private static final long ONE_DAY_IN_MILLIS = 1000*60*60*24;

    private static final SearchControls sObjectSC = new SearchControls(SearchControls.OBJECT_SCOPE, 0, 0, null, false, false);

    static final SearchControls sSubtreeSC = new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, null, false, false);
    
    
    private final static String[] sGroupReturnAttrs = 
        { Provisioning.A_zimbraId, Provisioning.A_zimbraGroupId, Provisioning.A_zimbraMemberOf };

    private final static SearchControls sGroupSearchControls = 
                new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, sGroupReturnAttrs, false, false);
    
    
    private static Log mLog = LogFactory.getLog(LdapProvisioning.class);
    
    private static LdapConfig sConfig = null;
    
    private static Pattern sValidCosName = Pattern.compile("^\\w+$");

    private static final String[] sInvalidAccountCreateModifyAttrs = {
            Provisioning.A_zimbraMailAlias,
            Provisioning.A_zimbraMailDeliveryAddress,
            Provisioning.A_uid,
            Provisioning.A_userPassword
    };

    private static final String FILTER_ACCOUNT_OBJECTCLASS =
        //"(objectclass=zimbraAccount)(!(objectclass=zimbraCalendarResource))";
        "(objectclass=zimbraAccount)";
    private static final String FILTER_CALENDAR_RESOURCE_OBJECTCLASS =
        "(objectclass=zimbraCalendarResource)";

    private static ZimbraLdapEntryCache sAccountCache =
        new ZimbraLdapEntryCache(
                LC.ldap_cache_account_maxsize.intValue(),
                LC.ldap_cache_account_maxage.intValue() * Constants.MILLIS_PER_MINUTE); 

    private static ZimbraLdapEntryCache sCosCache =
        new ZimbraLdapEntryCache(
                LC.ldap_cache_cos_maxsize.intValue(),
                LC.ldap_cache_cos_maxage.intValue() * Constants.MILLIS_PER_MINUTE); 

    private static ZimbraLdapEntryCache sDomainCache =
        new ZimbraLdapEntryCache(
                LC.ldap_cache_domain_maxsize.intValue(),
                LC.ldap_cache_domain_maxage.intValue() * Constants.MILLIS_PER_MINUTE);     

    private static ZimbraLdapEntryCache sServerCache =
        new ZimbraLdapEntryCache(
                LC.ldap_cache_server_maxsize.intValue(),
                LC.ldap_cache_server_maxage.intValue() * Constants.MILLIS_PER_MINUTE);

    // we are only caching zimbraGroupId/zimbraMemberOf
    private static LdapGroupEntryCache sGroupCache = 
        new LdapGroupEntryCache(
                LC.ldap_cache_group_maxsize.intValue(),
                LC.ldap_cache_group_maxage.intValue() * Constants.MILLIS_PER_MINUTE);                

    private static boolean sTimeZoneInited = false;
    private static final Object sTimeZoneGuard = new Object();
    private static Map /*<String tzId, WellKnownTimeZone>*/ sTimeZoneMap = new HashMap(LC.ldap_cache_timezone_maxsize.intValue());
    // list of time zones to preserve sort order
    private static List /*<WellKnownTimeZone>*/ sTimeZoneList = new ArrayList(LC.ldap_cache_timezone_maxsize.intValue());

    private static ZimbraLdapEntryCache sZimletCache = 
        new ZimbraLdapEntryCache(
                LC.ldap_cache_zimlet_maxsize.intValue(),
                LC.ldap_cache_zimlet_maxage.intValue() * Constants.MILLIS_PER_MINUTE);                

    private static final String CONFIG_BASE = "cn=config,cn=zimbra";     
    private static final String COS_BASE = "cn=cos,cn=zimbra"; 
    private static final String SERVER_BASE = "cn=servers,cn=zimbra";
    private static final String ADMIN_BASE = "cn=admins,cn=zimbra";
    private static final String ZIMLET_BASE = "cn=zimlets,cn=zimbra";

    private static final int BY_ID = 1;

    private static final int BY_EMAIL = 2;

    private static final int BY_NAME = 3;

    private static final Random sPoolRandom = new Random();
    
    private static String cosNametoDN(String name) {
        return "cn="+name+","+COS_BASE;
    }
    
    private static String serverNametoDN(String name) {
        return "cn="+name+","+SERVER_BASE;
    }

    static String adminNameToDN(String name) {
        return "uid="+name+","+ADMIN_BASE;
    }
    
    static String emailToDN(String localPart, String domain) {
        return "uid="+localPart+","+domainToAccountBaseDN(domain);
    }
    
    static String domainToAccountBaseDN(String domain) {
        return "ou=people,"+LdapUtil.domainToDN(domain);
    }
    
    static String zimletNameToDN(String name) {
    	return "cn="+name+","+ZIMLET_BASE;
    }
    	

    private static Pattern sNamePattern = Pattern.compile("([/+])"); 
    
    static String mimeConfigToDN(String name) {
        name = sNamePattern.matcher(name).replaceAll("\\\\$1");
        return "cn=" + name + ",cn=mime," + CONFIG_BASE;
    }

    /**
     * Status check on LDAP connection.  Search for global config entry.
     */
    public boolean healthCheck() {
        boolean result = false;
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            Attributes attrs = ctxt.getAttributes(CONFIG_BASE);
            result = attrs != null;
        } catch (NamingException e) {
            mLog.warn("LDAP health check error", e);
        } catch (ServiceException e) {
            mLog.warn("LDAP health check error", e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
        return result;
    }

    public Config getConfig() throws ServiceException
    {
        // TODO: failure scenarios? fallback to static config file or hard-coded defaults?
        if (sConfig == null) {
            synchronized(LdapProvisioning.class) {
                if (sConfig == null) {
                    DirContext ctxt = null;
                    try {
                        ctxt = LdapUtil.getDirContext();
                        Attributes attrs = ctxt.getAttributes(CONFIG_BASE);
                        sConfig = new LdapConfig(CONFIG_BASE, attrs);
                    } catch (NamingException e) {
                        throw ServiceException.FAILURE("unable to get config", e);
                    } finally {
                        LdapUtil.closeContext(ctxt);
                    }
                }
            }
        }
        return sConfig;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getMimeType(java.lang.String)
     */
    public synchronized MimeTypeInfo getMimeType(String name) throws ServiceException {
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            String dn = mimeConfigToDN(name);
            Attributes attrs = ctxt.getAttributes(dn);
            return new LdapMimeType(dn, attrs);
        } catch (NamingException e) {
            return null;
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }
    
    public synchronized MimeTypeInfo getMimeTypeByExtension(String ext) throws ServiceException {
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            ext = LdapUtil.escapeSearchFilterArg(ext);
            NamingEnumeration ne = ctxt.search("cn=mime," + CONFIG_BASE, "(" + Provisioning.A_zimbraMimeFileExtension + "=" + ext + ")", sSubtreeSC);
            if (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                ne.close();
                return new LdapMimeType(sr.getNameInNamespace(), sr.getAttributes());
            }
            return null;
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;                        
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to get mime type for file extension " + ext, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
        
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getObjectType(java.lang.String)
     */
    public synchronized List getObjectTypes() throws ServiceException {
    	return listAllZimlets();
    }

    private LdapAccount getAccountByQuery(String base, String query, DirContext initCtxt) throws ServiceException {
        DirContext ctxt = initCtxt;
        try {
            if (ctxt == null)
                ctxt = LdapUtil.getDirContext();
            NamingEnumeration ne = ctxt.search(base, query, sSubtreeSC);
            if (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                if (ne.hasMore())
                    throw AccountServiceException.MULTIPLE_ACCOUNTS_MATCHED("getAccountByQuery: "+query);
                ne.close();
                return makeLdapAccount(sr.getNameInNamespace(), sr.getAttributes(), this);
            }
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;            
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup account via query: "+query, e);
        } finally {
            if (initCtxt == null)
                LdapUtil.closeContext(ctxt);
        }
        return null;
    }

    private Account getAccountById(String zimbraId, DirContext ctxt) throws ServiceException {
        if (zimbraId == null)
            return null;
        LdapAccount a = (LdapAccount) sAccountCache.getById(zimbraId);
        if (a == null) {
            zimbraId= LdapUtil.escapeSearchFilterArg(zimbraId);
            a = getAccountByQuery(
                    "",
                    "(&(zimbraId=" + zimbraId + ")" +
                    FILTER_ACCOUNT_OBJECTCLASS + ")",
                    ctxt);
            sAccountCache.put(a);
        }
        return a;
    }
    
    public Account getAccountById(String zimbraId) throws ServiceException {
        return getAccountById(zimbraId, null);
    }

    public Account getAccountByForeignPrincipal(String foreignPrincipal) throws ServiceException {
        foreignPrincipal = LdapUtil.escapeSearchFilterArg(foreignPrincipal);
        return getAccountByQuery(
                "",
                "(&(zimbraForeignPrincipal=" + foreignPrincipal + ")" +
                FILTER_ACCOUNT_OBJECTCLASS + ")",
                null);
    }

    public Account getAdminAccountByName(String name) throws ServiceException {
        LdapAccount a = (LdapAccount) sAccountCache.getByName(name);
        if (a == null) {
            name = LdapUtil.escapeSearchFilterArg(name);
            a = getAccountByQuery(
                    ADMIN_BASE,
                    "(&(uid="+name+")" +
                    FILTER_ACCOUNT_OBJECTCLASS + ")",
                    null);
            sAccountCache.put(a);
        }
        return a;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getDomainByName(java.lang.String)
     */
    public Account getAccountByName(String emailAddress) throws ServiceException {
        
        int index = emailAddress.indexOf('@');
        String domain = null;
        if (index == -1) {
             domain = getConfig().getAttr(Provisioning.A_zimbraDefaultDomainName, null);
            if (domain == null)
                throw ServiceException.INVALID_REQUEST("must be valid email address: "+emailAddress, null);
            else
                emailAddress = emailAddress + "@" + domain;            
         }
        
        LdapAccount account = (LdapAccount) sAccountCache.getByName(emailAddress);
        if (account == null) {
            emailAddress = LdapUtil.escapeSearchFilterArg(emailAddress);
            account = getAccountByQuery(
                    "",
                    "(&(|(zimbraMailDeliveryAddress=" + emailAddress +
                    ")(zimbraMailAlias=" + emailAddress + "))" +
                    FILTER_ACCOUNT_OBJECTCLASS + ")",
                    null);
            sAccountCache.put(account);
        }
        return account;
    }
    
    private int guessType(String value) {
        if (value.indexOf("@") != -1)
            return BY_EMAIL;
        else if (value.length() == 36 &&
                value.charAt(8) == '-' &&
                value.charAt(13) == '-' &&
                value.charAt(18) == '-' &&
                value.charAt(23) == '-')
            return BY_ID;
        else return BY_NAME;
    }
  
    private Cos lookupCos(String key, DirContext ctxt) throws ServiceException {
        Cos c = null;
        switch(guessType(key)) {
        case BY_ID:
            c = getCosById(key, ctxt);
            break;
        case BY_NAME:
            c = getCosByName(key, ctxt);
            break;
        }
        if (c == null)
            throw AccountServiceException.NO_SUCH_COS(key);
        else
            return c;
    }
    
    public Account createAccount(String emailAddress, String password, Map acctAttrs) throws ServiceException {
        return createAccount(emailAddress, password, acctAttrs, null);
    }

    private Account createAccount(String emailAddress,
                                  String password,
                                  Map acctAttrs,
                                  String[] additionalObjectClasses)
    throws ServiceException {

        emailAddress = emailAddress.toLowerCase().trim();

        HashMap attrManagerContext = new HashMap();
        AttributeManager.getInstance().preModify(acctAttrs, null, attrManagerContext, true, true);

        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);

            String parts[] = emailAddress.split("@");
            if (parts.length != 2)
                throw ServiceException.INVALID_REQUEST("must be valid email address: "+emailAddress, null);
            
            String uid = parts[0];
            String domain = parts[1];

            Domain d = getDomainByName(domain, ctxt);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);
            String domainType = d.getAttr(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
            if (!domainType.equals(Provisioning.DOMAIN_TYPE_LOCAL))
                throw ServiceException.INVALID_REQUEST("domain type must be local", null);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(acctAttrs, attrs);

            for (int i=0; i < sInvalidAccountCreateModifyAttrs.length; i++) {
                String a = sInvalidAccountCreateModifyAttrs[i];
                if (attrs.get(a) != null)
                    throw ServiceException.INVALID_REQUEST("invalid attribute for CreateAccount: "+a, null);
            }
            
            Attribute oc = LdapUtil.addAttr(attrs, A_objectClass, "organizationalPerson");
            oc.add(C_zimbraAccount);
            oc.add(C_amavisAccount);
            if (additionalObjectClasses != null) {
                for (int i = 0; i < additionalObjectClasses.length; i++)
                    oc.add(additionalObjectClasses[i]);
            }
            
            String zimbraIdStr = LdapUtil.generateUUID();
            attrs.put(A_zimbraId, zimbraIdStr);

            // default account status is active
            if (attrs.get(Provisioning.A_zimbraAccountStatus) == null)
                attrs.put(A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);

            Cos cos = null;
            Attribute cosIdAttr = attrs.get(Provisioning.A_zimbraCOSId);
            String cosId = null;

            if (cosIdAttr != null) {
                cosId = (String) cosIdAttr.get();
                cos = lookupCos(cosId, ctxt);
                if (!cos.getId().equals(cosId)) {
                    cosId = cos.getId();
                }
                attrs.put(Provisioning.A_zimbraCOSId, cosId);
            } else {
                String domainCosId = domain != null ? d.getAttr(Provisioning.A_zimbraDomainDefaultCOSId, null) : null;
                if (domainCosId != null) cos = getCosById(domainCosId);
                if (cos == null) cos = getCosByName(Provisioning.DEFAULT_COS_NAME, ctxt);
            }

            // if zimbraMailHost is not specified, and we have a COS, see if there is a pool to
            // pick from.
            if (cos != null && attrs.get(Provisioning.A_zimbraMailHost) == null) {
                String mailHostPool[] = cos.getMultiAttr(Provisioning.A_zimbraMailHostPool);
                addMailHost(attrs, mailHostPool, cos.getName());
            }

            // if zimbraMailHost not specified default to local server's zimbraServiceHostname
            // this means every account will always have a mailbox
            if (attrs.get(Provisioning.A_zimbraMailHost) == null) {
            	String localMailHost = getLocalServer().getAttr(Provisioning.A_zimbraServiceHostname);
            	if (localMailHost != null) {
                    attrs.put(Provisioning.A_zimbraMailHost, localMailHost);
                    int lmtpPort = getLocalServer().getIntAttr(Provisioning.A_zimbraLmtpBindPort, com.zimbra.cs.util.Config.D_LMTP_BIND_PORT);
                    String transport = "lmtp:" + localMailHost + ":" + lmtpPort;
                    attrs.put(Provisioning.A_zimbraMailTransport, transport);
            	}
            }

            // set all the mail-related attrs if zimbraMailHost was specified
            if (attrs.get(Provisioning.A_zimbraMailHost) != null) {
                // default mail status is enabled
                if (attrs.get(Provisioning.A_zimbraMailStatus) == null)
                    attrs.put(A_zimbraMailStatus, MAIL_STATUS_ENABLED);

                // default account mail delivery address is email address
                if (attrs.get(Provisioning.A_zimbraMailDeliveryAddress) == null) {
                    attrs.put(A_zimbraMailDeliveryAddress, emailAddress);
                }
                attrs.put(A_mail, emailAddress);                
            }
            
            // required for organizationalPerson class
            if (attrs.get(Provisioning.A_cn) == null)
                attrs.put(A_cn, uid);

            // required for organizationalPerson class
            if (attrs.get(Provisioning.A_sn) == null)
                attrs.put(A_sn, uid);
            
            attrs.put(A_uid, uid);

            if (password != null) {
                setPassword(cos, attrs, password);
            }
            
            String dn = emailToDN(uid, domain);
            createSubcontext(ctxt, dn, attrs, "createAccount");
            LdapAccount acct = (LdapAccount) getAccountById(zimbraIdStr, ctxt);
            AttributeManager.getInstance().postModify(acctAttrs, acct, attrManagerContext, true);

            return acct;
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.ACCOUNT_EXISTS(emailAddress);
        } catch (NamingException e) {
           throw ServiceException.FAILURE("unable to create account: "+emailAddress, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    private String addMailHost(Attributes attrs, String[] mailHostPool, String cosName) throws ServiceException {
        if (mailHostPool.length == 0) {
            return null;
        } else if (mailHostPool.length > 1) {
            // copy it, since we are dealing with a cached String[]
            String pool[] = new String[mailHostPool.length];
            System.arraycopy(mailHostPool, 0, pool, 0, mailHostPool.length);
            mailHostPool = pool;
        }

        // shuffule up and deal
        int max = mailHostPool.length;
        while (max > 0) {
            int i = sPoolRandom.nextInt(max);
            String mailHostId = mailHostPool[i];
            Server s = (mailHostId == null) ? null : getServerById(mailHostId);
            if (s != null) {
                String mailHost = s.getAttr(Provisioning.A_zimbraServiceHostname);
                if (mailHost != null) {
                	attrs.put(Provisioning.A_zimbraMailHost, mailHost);
                	int lmtpPort = s.getIntAttr(Provisioning.A_zimbraLmtpBindPort, com.zimbra.cs.util.Config.D_LMTP_BIND_PORT);
                	String transport = "lmtp:" + mailHost + ":" + lmtpPort;
                	attrs.put(Provisioning.A_zimbraMailTransport, transport);
                	return mailHost;
                } else {
                    ZimbraLog.account.warn("cos("+cosName+") mailHostPool server("+s.getName()+") has no service hostname");
                }
            } else {
                ZimbraLog.account.warn("cos("+cosName+") has invalid server in pool: "+mailHostId);
            }
            if (i != max-1) {
                mailHostPool[i] = mailHostPool[max-1];
            }
            max--;
        }
        return null;
    }

    /**
     * copy an account with the specified name (user@domain) from the source to the local system.
     * @throws ServiceException
     * 
     */
    public Account copyAccount(String emailAddress, String remoteURL, 
            String remoteBindDn, String remoteBindPassword) throws ServiceException {
        
        emailAddress = emailAddress.toLowerCase().trim();
        
        DirContext ctxt = null;
        DirContext rctxt = null;
        
        try {
            ctxt = LdapUtil.getDirContext(true);
            
            String parts[] = emailAddress.split("@");
            
            if (parts.length != 2)
                throw ServiceException.INVALID_REQUEST("must be valid email address: "+emailAddress, null);
            
            String uid = parts[0];
            String domain = parts[1];

            Domain d = getDomainByName(domain, ctxt);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);

            rctxt = LdapUtil.getDirContext(new String[] {remoteURL}, remoteBindDn, remoteBindPassword);
            
            String dn = domainToAccountBaseDN(domain);
            LdapAccount remoteAccount = getAccountByQuery(
                    dn,
                    "(&(|(uid=" + uid +
                    ")(zimbraMailAlias=" + emailAddress + "))" +
                    FILTER_ACCOUNT_OBJECTCLASS + ")",
                    rctxt);
            Attributes attrs = remoteAccount.getRawAttrs();
            
            String accountDn = emailToDN(uid, domain);
            createSubcontext(ctxt, accountDn, attrs, "copyAccount");
            LdapAccount acct = (LdapAccount) getAccountById(remoteAccount.getId(), ctxt);
            return acct;
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.ACCOUNT_EXISTS(emailAddress);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to create account: "+emailAddress, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }
            
    public Account createAdminAccount(String uid, String password, Map acctAttrs) throws ServiceException {

        uid = uid.toLowerCase().trim();
        
        if (uid.indexOf("@") != -1)
            throw ServiceException.FAILURE("admin account names must not have an '@' in them", null);
        
        HashMap attrManagerContext = new HashMap();
        AttributeManager.getInstance().preModify(acctAttrs, null, attrManagerContext, true, true);

        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(acctAttrs, attrs);
            
            Attribute oc = LdapUtil.addAttr(attrs, A_objectClass, "organizationalPerson");
            oc.add(C_zimbraAccount);
            
            String zimbraIdStr = LdapUtil.generateUUID();
            attrs.put(A_zimbraId, zimbraIdStr);
            
            if (attrs.get(Provisioning.A_zimbraAccountStatus) == null)
                attrs.put(A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
            
            attrs.put(A_uid, uid);
            
            // required for organizationalPerson class
            if (attrs.get(Provisioning.A_cn) == null)
                attrs.put(A_cn, uid);

            // required for organizationalPerson class
            if (attrs.get(Provisioning.A_sn) == null)
                attrs.put(A_sn, uid);            
            
            if (password != null)
                attrs.put(A_userPassword, LdapUtil.generateSSHA(password, null));

            String dn = adminNameToDN(uid);
            createSubcontext(ctxt, dn, attrs, "createAdminAccount");
            Account acct = getAccountById(zimbraIdStr, ctxt); 
            
            AttributeManager.getInstance().postModify(acctAttrs, acct, attrManagerContext, true);
            return acct;            

        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.ACCOUNT_EXISTS(uid);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getAllDomains()
     */
    public List getAllAdminAccounts() throws ServiceException {
        return searchAccounts("(|(zimbraIsAdminAccount=TRUE)(zimbraIsDomainAdminAccount=TRUE))", null, null, true, Provisioning.SA_ACCOUNT_FLAG);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#searchAccounts(java.lang.String)
     */
    public List searchAccounts(String query, String returnAttrs[], final String sortAttr, final boolean sortAscending, int flags)  
        throws ServiceException
    {
        //flags &= ~Provisioning.SA_DOMAIN_FLAG; // leaving on for now
        return searchObjects(query, returnAttrs, sortAttr, sortAscending, "", flags);          
    }
    
    private static String getObjectClassQuery(int flags) {
        boolean accounts = (flags & Provisioning.SA_ACCOUNT_FLAG) != 0; 
        boolean aliases = (flags & Provisioning.SA_ALIAS_FLAG) != 0;
        boolean lists = (flags & Provisioning.SA_DISTRIBUTION_LIST_FLAG) != 0;
        boolean domains = (flags & Provisioning.SA_DOMAIN_FLAG) != 0;
        boolean calendarResources =
            (flags & Provisioning.SA_CALENDAR_RESOURCE_FLAG) != 0;

        int num = (accounts ? 1 : 0) +
                  (aliases ? 1 : 0) +
                  (lists ? 1 : 0) +
                  (domains ? 1 : 0) +                  
                  (calendarResources ? 1 : 0);
        if (num == 0)
            accounts = true;

        // If searching for user accounts/aliases/lists, filter looks like:
        //
        //   (&(objectclass=zimbraAccount)!(objectclass=zimbraCalendarResource))
        //
        // If searching for calendar resources, filter looks like:
        //
        //   (objectclass=zimbraCalendarResource)
        //
        // The !resource condition is there in first case because a calendar
        // resource is also a zimbraAccount.
        //
        StringBuffer oc = new StringBuffer();
        if (!calendarResources) oc.append("(&");
        if (num > 1) oc.append("(|");
        if (accounts) oc.append("(objectclass=zimbraAccount)");
        if (aliases) oc.append("(objectclass=zimbraAlias)");
        if (lists) oc.append("(objectclass=zimbraDistributionList)");
        if (domains) oc.append("(objectclass=zimbraDomain)");        
        if (calendarResources)
            oc.append("(objectclass=zimbraCalendarResource)");
        if (num > 1) oc.append(")");
        if (!calendarResources)
            oc.append("(!(objectclass=zimbraCalendarResource)))");
        return oc.toString();
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#searchAccounts(java.lang.String)
     */
    List searchObjects(String query, String returnAttrs[], final String sortAttr, final boolean sortAscending, String base, int flags)  
        throws ServiceException
    {
        final List result = new ArrayList();
        
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            public void visit(com.zimbra.cs.account.NamedEntry entry) throws ServiceException {
                result.add(entry);
            }
        };
        
        searchObjects(query, returnAttrs, base, flags, visitor);

        final boolean byName = sortAttr == null || sortAttr.equals("name"); 
        Comparator comparator = new Comparator() {
            public int compare(Object oa, Object ob) {
                LdapNamedEntry a = (LdapNamedEntry) oa;
                LdapNamedEntry b = (LdapNamedEntry) ob;
                int result = 0;
                if (byName)
                    result = a.getName().compareToIgnoreCase(b.getName());
                else {
                    String sa = a.getAttr(sortAttr);
                    String sb = b.getAttr(sortAttr);
                    if (sa == null) sa = "";
                    if (sb == null) sb = "";
                    result = sa.compareToIgnoreCase(sb);
                }
                return sortAscending ? result : -result;
            }
        };
        Collections.sort(result, comparator);        
        return result;
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#searchAccounts(java.lang.String)
     */
    void searchObjects(String query, String returnAttrs[], String base, int flags, NamedEntry.Visitor visitor)
        throws ServiceException
    {
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            
            String objectClass = getObjectClassQuery(flags);
            
            if (query == null || query.equals("")) {
                query = objectClass;
            } else {
                if (query.startsWith("(") && query.endsWith(")")) {
                    query = "(&"+query+objectClass+")";                    
                } else {
                    query = "(&("+query+")"+objectClass+")";
                }
            }
            
            returnAttrs = fixReturnAttrs(returnAttrs, flags);

            SearchControls searchControls = 
                new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, returnAttrs, false, false);

            //Set the page size and initialize the cookie that we pass back in subsequent pages
            int pageSize = 1000;
            byte[] cookie = null;
 
            LdapContext lctxt = (LdapContext)ctxt; 
 
            // we don't want to ever cache any of these, since they might not have all their attributes

            NamingEnumeration ne = null;

            try {
                do {
                    lctxt.setRequestControls(new Control[]{new PagedResultsControl(pageSize, cookie, Control.CRITICAL)});
                    
                    ne = ctxt.search(base, query, searchControls);
                    while (ne != null && ne.hasMore()) {
                        SearchResult sr = (SearchResult) ne.nextElement();
                        String dn = sr.getNameInNamespace();
                        // skip admin accounts
                        if (dn.endsWith("cn=zimbra")) continue;
                        Attributes attrs = sr.getAttributes();
                        Attribute objectclass = attrs.get("objectclass");
                        if (objectclass == null || objectclass.contains(C_zimbraAccount)) visitor.visit(makeLdapAccount(dn, attrs, this));
                        else if (objectclass.contains(C_zimbraAlias)) visitor.visit(new LdapAlias(dn, attrs));
                        else if (objectclass.contains(C_zimbraMailList)) visitor.visit(new LdapDistributionList(dn, attrs));
                        else if (objectclass.contains(C_zimbraDomain)) visitor.visit(new LdapDomain(dn, attrs, this));                        
                    }
                    cookie = getCookie(lctxt);
                } while (cookie != null);
            } finally {
                if (ne != null) ne.close();
            }
        } catch (InvalidSearchFilterException e) {
            throw ServiceException.INVALID_REQUEST("invalid search filter "+e.getMessage(), e);
        } catch (NameNotFoundException e) {
            // happens when base doesn't exist
            ZimbraLog.account.warn("unable to list all objects", e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all objects", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to list all objects", e);            
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    private byte[] getCookie(LdapContext lctxt) throws NamingException {
        Control[] controls = lctxt.getResponseControls();
        if (controls != null) {
            for (int i = 0; i < controls.length; i++) {
                if (controls[i] instanceof PagedResultsResponseControl) {
                    PagedResultsResponseControl prrc =
                        (PagedResultsResponseControl)controls[i];
                    return prrc.getCookie();
                }
            }
        }
        return null;
    }

    /**
     * add "uid" to list of return attrs if not specified, since we need it to construct an Account
     * @param returnAttrs
     * @return
     */
    private String[] fixReturnAttrs(String[] returnAttrs, int flags) {
        if (returnAttrs == null || returnAttrs.length == 0)
            return null;
        
        boolean needUID = true;
        boolean needID = true;
        boolean needCOSId = true;
        boolean needObjectClass = true;        
        boolean needAliasTargetId = (flags & Provisioning.SA_ALIAS_FLAG) != 0;
        boolean needCalendarUserType = true;
        
        for (int i=0; i < returnAttrs.length; i++) {
            if (Provisioning.A_uid.equalsIgnoreCase(returnAttrs[i]))
                needUID = false;
            else if (Provisioning.A_zimbraId.equalsIgnoreCase(returnAttrs[i]))
                needID = false;
            else if (Provisioning.A_zimbraCOSId.equalsIgnoreCase(returnAttrs[i]))
                needCOSId = false;
            else if (Provisioning.A_zimbraAliasTargetId.equalsIgnoreCase(returnAttrs[i]))
                needAliasTargetId = false;
            else if (Provisioning.A_objectClass.equalsIgnoreCase(returnAttrs[i]))
                needObjectClass = false;            
            else if (Provisioning.A_zimbraAccountCalendarUserType.equalsIgnoreCase(returnAttrs[i]))
            	needCalendarUserType = false;
        }
        
        int num = (needUID ? 1 : 0) + (needID ? 1 : 0) + (needCOSId ? 1 : 0) + (needAliasTargetId ? 1 : 0) + (needObjectClass ? 1 :0) + (needCalendarUserType ? 1 : 0);
        
        if (num == 0) return returnAttrs;
       
        String[] result = new String[returnAttrs.length+num];
        int i = 0;
        if (needUID) result[i++] = Provisioning.A_uid;
        if (needID) result[i++] = Provisioning.A_zimbraId;
        if (needCOSId) result[i++] = Provisioning.A_zimbraCOSId;
        if (needAliasTargetId) result[i++] = Provisioning.A_zimbraAliasTargetId;
        if (needObjectClass) result[i++] = Provisioning.A_objectClass;
        if (needCalendarUserType) result[i++] = Provisioning.A_zimbraAccountCalendarUserType;
        System.arraycopy(returnAttrs, 0, result, i, returnAttrs.length);
        return result;
    }

    public void setCOS(Account acct, Cos cos) throws ServiceException
    {
        HashMap attrs = new HashMap();
        attrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        acct.modifyAttrs(attrs);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Account#modifyAccountStatus(java.lang.String)
     */
    public void modifyAccountStatus(Account acct, String newStatus) throws ServiceException {
        HashMap attrs = new HashMap();
        attrs.put(Provisioning.A_zimbraAccountStatus, newStatus);
        acct.modifyAttrs(attrs);
    }

    
    static String[] addMultiValue(String values[], String value) {
        List list = new ArrayList(Arrays.asList(values));        
        list.add(value);
        return (String[]) list.toArray(new String[list.size()]);
    }
    
    String[] addMultiValue(NamedEntry acct, String attr, String value) {
        return addMultiValue(acct.getMultiAttr(attr), value);
    }

    String[] removeMultiValue(NamedEntry acct, String attr, String value) {
        return LdapUtil.removeMultiValue(acct.getMultiAttr(attr), value);
    }

    public void addAlias(Account acct, String alias) throws ServiceException {
        addAliasInternal(acct, alias);
    }
	
    public void removeAlias(Account acct, String alias) throws ServiceException {
        removeAliasInternal(acct, alias, acct.getAliases());
    }
    
    public void addAlias(DistributionList dl, String alias) throws ServiceException {
        addAliasInternal(dl, alias);
    }

    public void removeAlias(DistributionList dl, String alias) throws ServiceException {
        removeAliasInternal(dl, alias, dl.getAliases());
    }

    private void addAliasInternal(NamedEntry entry, String alias) throws ServiceException {
    	assert(entry instanceof Account || entry instanceof DistributionList);
        alias = alias.toLowerCase().trim();
        int loc = alias.indexOf("@"); 
        if (loc == -1)
            throw ServiceException.INVALID_REQUEST("alias must include the domain", null);
        
        String aliasDomain = alias.substring(loc+1);
        String aliasName = alias.substring(0, loc);
        
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);

            Domain domain = getDomainByName(aliasDomain, ctxt);
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(aliasDomain);
            
            String aliasDn = LdapProvisioning.emailToDN(aliasName, aliasDomain);
            // the create and addAttr ideally would be in the same transaction
            LdapUtil.simpleCreate(ctxt, aliasDn, "zimbraAlias",
                    new String[] { Provisioning.A_uid, aliasName, 
                    Provisioning.A_zimbraId, LdapUtil.generateUUID(),
                    Provisioning.A_zimbraAliasTargetId, entry.getId()} );
            HashMap attrs = new HashMap();
            attrs.put(Provisioning.A_zimbraMailAlias, addMultiValue(entry, Provisioning.A_zimbraMailAlias, alias));
            attrs.put(Provisioning.A_mail, addMultiValue(entry, Provisioning.A_mail, alias));
            // UGH
            ((LdapNamedEntry)entry).modifyAttrsInternal(ctxt, attrs);
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.ACCOUNT_EXISTS(alias);
        } catch (InvalidNameException e) {
            throw ServiceException.INVALID_REQUEST("invalid alias name", e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to create alias", e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }                
    }
    
    private void removeAliasInternal(NamedEntry entry, String alias, String[] aliases) throws ServiceException {
        
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);
            
            int loc = alias.indexOf("@"); 
            if (loc == -1)
                throw ServiceException.INVALID_REQUEST("alias must include the domain", null);

            alias = alias.toLowerCase();

            boolean found = false;
            for (int i=0; !found && i < aliases.length; i++) {
                found = aliases[i].equalsIgnoreCase(alias);
            }
            
            if (!found)
                throw AccountServiceException.NO_SUCH_ALIAS(alias);
            
            String aliasDomain = alias.substring(loc+1);
            String aliasName = alias.substring(0, loc);

            Domain domain = getDomainByName(aliasDomain, ctxt);
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(aliasDomain);
            
            String aliasDn = LdapProvisioning.emailToDN(aliasName, aliasDomain);            
            
            // remove zimbraMailAlias attr first, then alias
            try {
                HashMap attrs = new HashMap();
                attrs.put(Provisioning.A_mail, removeMultiValue(entry, Provisioning.A_mail, alias));
                attrs.put(Provisioning.A_zimbraMailAlias, removeMultiValue(entry, Provisioning.A_zimbraMailAlias, alias));                
                ((LdapNamedEntry)entry).modifyAttrsInternal(ctxt, attrs);
            } catch (ServiceException e) {
                ZimbraLog.account.warn("unable to remove zimbraMailAlias/mail attrs: "+alias);
                // try to remove alias
            }

            // remove address from all DLs
            removeAddressFromAllDistributionLists(alias);

            try {
                Attributes aliasAttrs = ctxt.getAttributes(aliasDn);
                // make sure aliasedObjectName points to this account
                Attribute a = aliasAttrs.get(Provisioning.A_zimbraAliasTargetId);
                if ( a != null && ( (String)a.get()).equals(entry.getId())) {
                    ctxt.unbind(aliasDn);
                    removeAddressFromAllDistributionLists(alias); // doesn't throw exception
                } else {
                    ZimbraLog.account.warn("unable to remove alias object: "+alias);
                }                
            } catch (NameNotFoundException e) {
                ZimbraLog.account.warn("unable to remove alias object: "+alias);                
            }
        } catch (NamingException e) {
            ZimbraLog.account.error("unable to remove alias: "+alias, e);                
            throw ServiceException.FAILURE("unable to remove alias", e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }        
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.zimbra.cs.account.Provisioning#createDomain(java.lang.String,
     *      java.util.Map)
     */
    public Domain createDomain(String name, Map domainAttrs) throws ServiceException {
        name = name.toLowerCase().trim();
        
        DirContext ctxt = null;
        try {
            
            ctxt = LdapUtil.getDirContext(true);
            
            LdapDomain d = (LdapDomain) getDomainByName(name, ctxt);
            if (d != null) throw AccountServiceException.DOMAIN_EXISTS(name);
            
            HashMap attrManagerContext = new HashMap();
            
            // Attribute checking can not express "allow setting on
            // creation, but do not allow modifies afterwards"
            String domainType = (String)domainAttrs.get(A_zimbraDomainType);
            if (domainType == null) {
                domainType = DOMAIN_TYPE_LOCAL;
            } else {
                domainAttrs.remove(A_zimbraDomainType); // add back later
            }
            
            AttributeManager.getInstance().preModify(domainAttrs, null, attrManagerContext, true, true);
            
            // Add back attrs we circumvented from attribute checking
            domainAttrs.put(A_zimbraDomainType, domainType);
            
            String parts[] = name.split("\\.");        
            String dns[] = LdapUtil.domainToDNs(parts);
            createParentDomains(ctxt, parts, dns);
            
            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(domainAttrs, attrs);
            
            Attribute oc = LdapUtil.addAttr(attrs, A_objectClass, "dcObject");
            oc.add("organization");
            oc.add("zimbraDomain");
            
            String zimbraIdStr = LdapUtil.generateUUID();
            attrs.put(A_zimbraId, zimbraIdStr);
            attrs.put(A_zimbraDomainName, name);
            attrs.put(A_zimbraMailStatus, MAIL_STATUS_ENABLED);
            
            if (domainType.equalsIgnoreCase(DOMAIN_TYPE_ALIAS)) {
                attrs.put(A_zimbraMailCatchAllAddress, "@" + name);
            }
            
            attrs.put(A_o, name+" domain");
            attrs.put(A_dc, parts[0]);
            
            String dn = dns[0];
            //NOTE: all four of these should be in a transaction...
            try {
                createSubcontext(ctxt, dn, attrs, "createDomain");
            } catch (NameAlreadyBoundException e) {
                ctxt.modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE, attrs);
            }
            
            LdapUtil.simpleCreate(ctxt, "ou=people,"+dn, "organizationalRole",
                    new String[] { A_ou, "people", A_cn, "people"});
            
            Domain domain = getDomainById(zimbraIdStr, ctxt);
            
            AttributeManager.getInstance().postModify(domainAttrs, domain, attrManagerContext, true);
            return domain;
            
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.DOMAIN_EXISTS(name);
        } catch (NamingException e) {
            //if (e instanceof )
            throw ServiceException.FAILURE("unable to create domain: "+name, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    private LdapDomain getDomainByQuery(String query, DirContext initCtxt) throws ServiceException {
        DirContext ctxt = initCtxt;
        try {
            if (ctxt == null)
                ctxt = LdapUtil.getDirContext();
            NamingEnumeration ne = ctxt.search("", query, sSubtreeSC);
            if (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                ne.close();
                return new LdapDomain(sr.getNameInNamespace(), sr.getAttributes(), this);
            }
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;                        
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup domain via query: "+query, e);
        } finally {
            if (initCtxt == null)
                LdapUtil.closeContext(ctxt);
        }
        return null;
    }

    private Domain getDomainById(String zimbraId, DirContext ctxt) throws ServiceException {
        if (zimbraId == null)
            return null;
        LdapDomain domain = (LdapDomain) sDomainCache.getById(zimbraId);
        if (domain == null) {
            zimbraId = LdapUtil.escapeSearchFilterArg(zimbraId);
            domain = getDomainByQuery("(&(zimbraId="+zimbraId+")(objectclass=zimbraDomain))", ctxt);
            sDomainCache.put(domain);
        }
        return domain;
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getDomainById(java.lang.String)
     */
    public Domain getDomainById(String zimbraId) throws ServiceException {
        return getDomainById(zimbraId, null);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getDomainByName(java.lang.String)
     */
    public Domain getDomainByName(String name) throws ServiceException {
            return getDomainByName(name, null);
    }        
        
   private Domain getDomainByName(String name, DirContext ctxt) throws ServiceException {
        LdapDomain domain = (LdapDomain) sDomainCache.getByName(name);
        if (domain == null) {
            name = LdapUtil.escapeSearchFilterArg(name);
            domain = getDomainByQuery("(&(zimbraDomainName="+name+")(objectclass=zimbraDomain))", ctxt);
            sDomainCache.put(domain);
        }
        return domain;        
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getAllDomains()
     */
    public List getAllDomains() throws ServiceException {
        List<LdapDomain> result = new ArrayList<LdapDomain>();
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();

            NamingEnumeration ne = ctxt.search("", "(objectclass=zimbraDomain)", sSubtreeSC);
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                result.add(new LdapDomain(sr.getNameInNamespace(), sr.getAttributes(), this));
            }
            ne.close();
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all domains", e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
        Collections.sort(result);
        return result;
    }

    private static boolean domainDnExists(DirContext ctxt, String dn) throws NamingException, ServiceException {
        try {
            NamingEnumeration ne = ctxt.search(dn,"objectclass=dcObject", sObjectSC);
            boolean result = ne.hasMore();
            ne.close();
            return result;
        } catch (InvalidNameException e) {
            return false;                        
        } catch (NameNotFoundException nnfe) {
            return false;
        }
    }

    private static void createParentDomains(DirContext ctxt, String parts[], String dns[]) throws NamingException, ServiceException {
        for (int i=dns.length-1; i > 0; i--) {        
            if (!domainDnExists(ctxt, dns[i])) {
                String dn = dns[i];
                String domain = parts[i];
                // don't create ZimbraDomain objects, since we don't want them to show up in list domains
                LdapUtil.simpleCreate(ctxt, dn, new String[] {"dcObject", "organization"}, 
                        new String[] { A_o, domain+" domain", A_dc, domain });
            }
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#createCos(java.lang.String, java.util.Map)
     */
    public Cos createCos(String name, Map cosAttrs) throws ServiceException {
        name = name.toLowerCase().trim();

        if (!sValidCosName.matcher(name).matches())
            throw ServiceException.INVALID_REQUEST("invalid name: "+name, null);

        HashMap attrManagerContext = new HashMap();
        AttributeManager.getInstance().preModify(cosAttrs, null, attrManagerContext, true, true);

        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(cosAttrs, attrs);
            Attribute oc = LdapUtil.addAttr(attrs, A_objectClass, "zimbraCOS");
            
            String zimbraIdStr = LdapUtil.generateUUID();
            attrs.put(A_zimbraId, zimbraIdStr);
            attrs.put(A_cn, name);
            String dn = cosNametoDN(name);
            createSubcontext(ctxt, dn, attrs, "createCos");

            Cos cos = getCosById(zimbraIdStr, ctxt);
            AttributeManager.getInstance().postModify(cosAttrs, cos, attrManagerContext, true);
            return cos;
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.COS_EXISTS(name);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#deleteAccountById(java.lang.String)
     */
    public void renameCos(String zimbraId, String newName) throws ServiceException {
        LdapCos cos = (LdapCos) getCosById(zimbraId);
        if (cos == null)
            throw AccountServiceException.NO_SUCH_COS(zimbraId);

        if (cos.getName().equals(DEFAULT_COS_NAME))
            throw ServiceException.INVALID_REQUEST("unable to rename default cos", null);

        if (!sValidCosName.matcher(newName).matches())
            throw ServiceException.INVALID_REQUEST("invalid name: "+newName, null);
       
        newName = newName.toLowerCase().trim();
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);
            String newDn = cosNametoDN(newName);
            ctxt.rename(cos.mDn, newDn);
            // remove old account from cache
            sCosCache.remove(cos);
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.COS_EXISTS(newName);            
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to rename cos: "+zimbraId, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }
    
    private LdapCos getCOSByQuery(String query, DirContext initCtxt) throws ServiceException {
        DirContext ctxt = initCtxt;
        try {
            if (ctxt == null)
                ctxt = LdapUtil.getDirContext();
            NamingEnumeration ne = ctxt.search(COS_BASE, query, sSubtreeSC);
            if (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                ne.close();
                return new LdapCos(sr.getNameInNamespace(), sr.getAttributes(), this);
            }
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;                        
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup cos via query: "+query, e);
        } finally {
            if (initCtxt == null)
                LdapUtil.closeContext(ctxt);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getCOSById(java.lang.String)
     */
    private Cos getCosById(String zimbraId, DirContext ctxt ) throws ServiceException {
        if (zimbraId == null)
            return null;

        LdapCos cos = (LdapCos) sCosCache.getById(zimbraId);
        if (cos == null) {
            zimbraId = LdapUtil.escapeSearchFilterArg(zimbraId);
            cos = getCOSByQuery("(&(zimbraId="+zimbraId+")(objectclass=zimbraCOS))", ctxt);
            sCosCache.put(cos);
        }
        return cos;
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getCOSById(java.lang.String)
     */
    public Cos getCosById(String zimbraId) throws ServiceException {
        return getCosById(zimbraId, null);
    }    

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getCOSByName(java.lang.String)
     */
    public Cos getCosByName(String name) throws ServiceException {
        return getCosByName(name, null);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getCOSByName(java.lang.String)
     */
    private Cos getCosByName(String name, DirContext initCtxt) throws ServiceException {
        DirContext ctxt = initCtxt;
        LdapCos cos = (LdapCos) sCosCache.getByName(name);
        if (cos != null)
            return cos;

        try {
            if (ctxt == null)
                ctxt = LdapUtil.getDirContext();
            String dn = cosNametoDN(name);            
            Attributes attrs = ctxt.getAttributes(dn);
            cos  = new LdapCos(dn, attrs, this);
            sCosCache.put(cos);            
            return cos;
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup COS by name: "+name, e);
        } finally {
            if (initCtxt == null)
                LdapUtil.closeContext(ctxt);
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getAllCOS()
     */
    public List getAllCos() throws ServiceException {
        List result = new ArrayList();
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            NamingEnumeration ne = ctxt.search(COS_BASE, "(objectclass=zimbraCOS)", sSubtreeSC);
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                result.add(new LdapCos(sr.getNameInNamespace(), sr.getAttributes(),this));
            }
            ne.close();
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all COS", e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
        Collections.sort(result);
        return result;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#deleteAccountById(java.lang.String)
     */
    public void deleteAccount(String zimbraId) throws ServiceException {
        LdapAccount acc = (LdapAccount) getAccountById(zimbraId);
        if (acc == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(zimbraId);

        removeAddressFromAllDistributionLists(acc.getName()); // this doesn't throw any exceptions

        String aliases[] = acc.getAliases();
        if (aliases != null)
            for (int i=0; i < aliases.length; i++)
                removeAlias(acc, aliases[i]); // this also removes each alias from any DLs

        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);
            ctxt.unbind(acc.getDN());
            sAccountCache.remove(acc);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to purge account: "+zimbraId, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
        
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#deleteAccountById(java.lang.String)
     */
    public void renameAccount(String zimbraId, String newName) throws ServiceException {
        
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);
            
            LdapAccount acc = (LdapAccount) getAccountById(zimbraId, ctxt);
            if (acc == null)
                throw AccountServiceException.NO_SUCH_ACCOUNT(zimbraId);

            String oldEmail = acc.getName();
            
            newName = newName.toLowerCase().trim();
            String[] parts = EmailUtil.getLocalPartAndDomain(newName);
            if (parts == null)
                throw ServiceException.INVALID_REQUEST("bad value for newName", null);
            String newLocal = parts[0];
            String newDomain = parts[1];
            
            Domain domain = getDomainByName(newDomain, ctxt);
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(newDomain);
            
            String newDn = emailToDN(newLocal,domain.getName());
            ctxt.rename(acc.mDn, newDn);

            renameAddressInAllDistributionLists(oldEmail, newName); // doesn't throw exceptions, just logs
            
            // remove old account from cache
            sAccountCache.remove(acc);
            acc = (LdapAccount) getAccountById(zimbraId,ctxt);
            HashMap amap = new HashMap();
            amap.put(Provisioning.A_zimbraMailDeliveryAddress, newName);
            String mail[] = acc.getMultiAttr(Provisioning.A_mail);
            if (mail.length == 0) {
                amap.put(Provisioning.A_mail, newName);
            } else {
                // not the most efficient, but renames dont' happen often
                mail = LdapUtil.removeMultiValue(mail, oldEmail);
                mail = addMultiValue(mail, newName);
                amap.put(Provisioning.A_mail, mail);
            }
            // this is non-atomic. i.e., rename could succeed and updating zimbraMailDeliveryAddress
            // could fail. So catch service exception here and log error            
            try {
                acc.modifyAttrsInternal(ctxt, amap);
            } catch (ServiceException e) {
                ZimbraLog.account.error("account renamed to "+newName+
                        " but failed to update zimbraMailDeliveryAddress", e);
                throw ServiceException.FAILURE("unable to rename account: "+zimbraId, e);
            }
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.ACCOUNT_EXISTS(newName);            
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to rename account: "+zimbraId, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }
        
    public void deleteDomain(String zimbraId) throws ServiceException {
        // TODO: should only allow a domain delete to succeed if there are no people
        // if there aren't, we need to delete the people trees first, then delete the domain.
        DirContext ctxt = null;
        LdapDomain d = null;
        try {
            ctxt = LdapUtil.getDirContext(true);

            d = (LdapDomain) getDomainById(zimbraId, ctxt);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(zimbraId);

            String name = d.getName();

            ctxt.unbind("ou=people,"+d.getDN());
            ctxt.unbind(d.getDN());
            sDomainCache.remove(d);
            
            String defaultDomain = getConfig().getAttr(A_zimbraDefaultDomainName, null);
            if (name.equalsIgnoreCase(defaultDomain)) {
                try {
                    HashMap attrs = new HashMap();
                    attrs.put(A_zimbraDefaultDomainName, "");
                    getConfig().modifyAttrs(attrs);
                } catch (Exception e) {
                    ZimbraLog.account.warn("unable to remove config attr:"+A_zimbraDefaultDomainName, e); 
                }
            }
        } catch (ContextNotEmptyException e) {
            throw AccountServiceException.DOMAIN_NOT_EMPTY(d.getName());
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to purge domain: "+zimbraId, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }
    
    public void deleteCos(String zimbraId) throws ServiceException {
        LdapCos c = (LdapCos) getCosById(zimbraId);
        if (c == null)
            throw AccountServiceException.NO_SUCH_COS(zimbraId);
        
        if (c.getName().equals(DEFAULT_COS_NAME))
            throw ServiceException.INVALID_REQUEST("unable to delete default cos", null);

        // TODO: should we go through all accounts with this cos and remove the zimbraCOSId attr?
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);
            ctxt.unbind(c.getDN());
            sCosCache.remove(c);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to purge cos: "+zimbraId, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#createServer(java.lang.String, java.util.Map)
     */
    public Server createServer(String name, Map serverAttrs) throws ServiceException {
        name = name.toLowerCase().trim();

        HashMap attrManagerContext = new HashMap();
        AttributeManager.getInstance().preModify(serverAttrs, null, attrManagerContext, true, true);

        String authHost = (String)serverAttrs.get(A_zimbraMtaAuthHost);
        if (authHost != null) {
            serverAttrs.put(A_zimbraMtaAuthURL, URLUtil.getMtaAuthURL(authHost));
        }
        
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(serverAttrs, attrs);
            Attribute oc = LdapUtil.addAttr(attrs, A_objectClass, "zimbraServer");

            String zimbraIdStr = LdapUtil.generateUUID();
            attrs.put(A_zimbraId, zimbraIdStr);
            attrs.put(A_cn, name);
            String dn = serverNametoDN(name);
            
            String zimbraServerHostname = null;

            Attribute zimbraServiceHostnameAttr = attrs.get(Provisioning.A_zimbraServiceHostname);
            if (zimbraServiceHostnameAttr == null) {
                zimbraServerHostname = name;
                attrs.put(Provisioning.A_zimbraServiceHostname, name);
            } else {
                zimbraServerHostname = (String) zimbraServiceHostnameAttr.get();
            }
            
            createSubcontext(ctxt, dn, attrs, "createServer");

            Server server = getServerById(zimbraIdStr, ctxt, true);
            AttributeManager.getInstance().postModify(serverAttrs, server, attrManagerContext, true);
            return server;

        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.SERVER_EXISTS(name);
        } catch (NamingException e) {
            //if (e instanceof )
            throw ServiceException.FAILURE("unable to create server: "+name, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    private Server getServerByQuery(String query, DirContext initCtxt) throws ServiceException {
        DirContext ctxt = initCtxt;
        try {
            if (ctxt == null)
                ctxt = LdapUtil.getDirContext();
            NamingEnumeration ne = ctxt.search(SERVER_BASE, query, sSubtreeSC);
            if (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                ne.close();
                return new LdapServer(sr.getNameInNamespace(), sr.getAttributes(), this);
            }
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;                        
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup server via query: "+query, e);
        } finally {
            if (initCtxt == null)
                LdapUtil.closeContext(ctxt);
        }
        return null;
    }

    private Server getServerById(String zimbraId, DirContext ctxt, boolean nocache) throws ServiceException {
        if (zimbraId == null)
            return null;
        LdapServer s = null;
        if (!nocache)
            s = (LdapServer) sServerCache.getById(zimbraId);
        if (s == null) {
            zimbraId = LdapUtil.escapeSearchFilterArg(zimbraId);
            s = (LdapServer)getServerByQuery("(&(zimbraId="+zimbraId+")(objectclass=zimbraServer))", ctxt); 
            sServerCache.put(s);
        }
        return s;
    }

    public Server getServerById(String zimbraId) throws ServiceException {
        return getServerById(zimbraId, null, false);
    }

    public Server getServerById(String zimbraId, boolean nocache) throws ServiceException {
        return getServerById(zimbraId, null, nocache);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getCOSByName(java.lang.String)
     */
    public Server getServerByName(String name) throws ServiceException {
        return getServerByName(name, false);
    }

    public Server getServerByName(String name, boolean nocache) throws ServiceException {
        if (!nocache) {
        	LdapServer s = (LdapServer) sServerCache.getByName(name);
            if (s != null)
                return s;
        }
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            String dn = serverNametoDN(name);            
            Attributes attrs = ctxt.getAttributes(dn);
            LdapServer s = new LdapServer(dn, attrs, this);
            sServerCache.put(s);            
            return s;
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;            
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup server by name: "+name, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    public List getAllServers() throws ServiceException {
        return getAllServers(null);
    }
    
    public List getAllServers(String service) throws ServiceException {
        List result = new ArrayList();
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            String filter = "(objectclass=zimbraServer)";
            if (service != null) {
                filter = "(&(objectclass=zimbraServer)(zimbraServiceEnabled=" + LdapUtil.escapeSearchFilterArg(service) + "))";
            } else {
                filter = "(objectclass=zimbraServer)";
            }
            NamingEnumeration ne = ctxt.search(SERVER_BASE, filter, sSubtreeSC);
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                LdapServer s = new LdapServer(sr.getNameInNamespace(), sr.getAttributes(), this);
                result.add(s);
            }
            ne.close();
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all servers", e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
        if (result.size() > 0)
            sServerCache.put(result, true);
        Collections.sort(result);
        return result;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#purgeServer(java.lang.String)
     */
    public void deleteServer(String zimbraId) throws ServiceException {
        LdapServer s = (LdapServer) getServerById(zimbraId);
        if (s == null)
            throw AccountServiceException.NO_SUCH_SERVER(zimbraId);

        // TODO: what if accounts still have this server as a mailbox?
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);
            ctxt.unbind(s.getDN());
            sServerCache.remove(s);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to purge server: "+zimbraId, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    /*
     *  Time Zones
     */

    private static void initTimeZoneCache() throws ServiceException {
    	synchronized (sTimeZoneGuard) {
    		if (sTimeZoneInited)
                return;

            DirContext ctxt = null;
            try {
                ctxt = LdapUtil.getDirContext();
                NamingEnumeration ne = ctxt.search("cn=timezones," + CONFIG_BASE, "(objectclass=zimbraTimeZone)", sSubtreeSC);
                sTimeZoneMap.clear();
                while (ne.hasMore()) {
                    SearchResult sr = (SearchResult) ne.next();
                    LdapWellKnownTimeZone tz = new LdapWellKnownTimeZone(sr.getNameInNamespace(), sr.getAttributes());
                    sTimeZoneMap.put(tz.getId(), tz);
                    sTimeZoneList.add(tz);
                }
                ne.close();
                sTimeZoneInited = true;
            } catch (NamingException e) {
                throw ServiceException.FAILURE("unable to list all time zones", e);
            } finally {
                LdapUtil.closeContext(ctxt);
            }
            Collections.sort(sTimeZoneList);
        }
    }

    /**
     * Returned list is read-only.
     * @return
     * @throws ServiceException
     */
    public List /*<WellKnownTimeZone>*/ getAllTimeZones() throws ServiceException {
        initTimeZoneCache();
        return sTimeZoneList;
    }

    public WellKnownTimeZone getTimeZoneById(String tzId) throws ServiceException {
        initTimeZoneCache();
        WellKnownTimeZone tz = (WellKnownTimeZone) sTimeZoneMap.get(tzId);
        return tz;
    }

    /*
     *  Distribution lists.
     */

    public DistributionList createDistributionList(String listAddress, Map listAttrs) throws ServiceException {

        listAddress = listAddress.toLowerCase().trim();

        HashMap attrManagerContext = new HashMap();
        AttributeManager.getInstance().preModify(listAttrs, null, attrManagerContext, true, true);

        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);

            String parts[] = listAddress.split("@");
            if (parts.length != 2)
                throw ServiceException.INVALID_REQUEST("must be valid list address: " + listAddress, null);

            String list = parts[0];
            String domain = parts[1];

            Domain d = getDomainByName(domain, ctxt);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);
            String domainType = d.getAttr(Provisioning.A_zimbraDomainType, Provisioning.DOMAIN_TYPE_LOCAL);
            if (!domainType.equals(Provisioning.DOMAIN_TYPE_LOCAL))
                throw ServiceException.INVALID_REQUEST("domain type must be local", null);

            Attributes attrs = new BasicAttributes(true);
            LdapUtil.mapToAttrs(listAttrs, attrs);
            Attribute oc = LdapUtil.addAttr(attrs, A_objectClass, "zimbraDistributionList");
            oc.add("zimbraMailRecipient");

            String zimbraIdStr = LdapUtil.generateUUID();
            attrs.put(A_zimbraId, zimbraIdStr);
            attrs.put(A_zimbraMailAlias, listAddress);
            attrs.put(A_mail, listAddress);

            // by default a distribution list is always created enabled
            if (attrs.get(Provisioning.A_zimbraMailStatus) == null) {
                attrs.put(A_zimbraMailStatus, MAIL_STATUS_ENABLED);
            }

            String dn = emailToDN(list, domain);
            createSubcontext(ctxt, dn, attrs, "createDistributionList");

            DistributionList dlist = getDistributionListById(zimbraIdStr, ctxt);
            AttributeManager.getInstance().postModify(listAttrs, dlist, attrManagerContext, true);
            return dlist;

        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.DISTRIBUTION_LIST_EXISTS(listAddress);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    private LdapDistributionList getDistributionListByQuery(String base, String query, DirContext initCtxt) throws ServiceException {
        DirContext ctxt = initCtxt;
        try {
            if (ctxt == null)
                ctxt = LdapUtil.getDirContext();
            NamingEnumeration ne = ctxt.search(base, query, sSubtreeSC);
            if (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                ne.close();
                return new LdapDistributionList(sr.getNameInNamespace(), sr.getAttributes());
            }
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;                        
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup distribution list via query: "+query, e);
        } finally {
            if (initCtxt == null)
                LdapUtil.closeContext(ctxt);
        }
        return null;
    }

    public void renameDistributionList(String zimbraId, String newEmail) throws ServiceException {
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);
            
            LdapDistributionList dl = (LdapDistributionList) getDistributionListById(zimbraId, ctxt);
            if (dl == null)
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(zimbraId);

            String oldEmail = dl.getName();

            newEmail = newEmail.toLowerCase().trim();
            String[] parts = EmailUtil.getLocalPartAndDomain(newEmail);
            if (parts == null)
                throw ServiceException.INVALID_REQUEST("bad value for newName", null);
            String newLocal = parts[0];
            String newDomain = parts[1];

            Domain domain = getDomainByName(newDomain, ctxt);
            if (domain == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(newDomain);
    
            String newDn = emailToDN(newLocal, domain.getName());
            ctxt.rename(dl.mDn, newDn);
            
            renameAddressInAllDistributionLists(oldEmail, newEmail); // doesn't throw exceptions, just logs
            
            dl = (LdapDistributionList) getDistributionListById(zimbraId,ctxt);
            HashMap attrs = new HashMap();
            String mail[] = dl.getMultiAttr(Provisioning.A_mail);
            if (mail.length == 0) {
                attrs.put(Provisioning.A_mail, newEmail);
            } else {
                // not the most efficient, but renames don't happen often
                mail = LdapUtil.removeMultiValue(mail, oldEmail);
                mail = addMultiValue(mail, newEmail);
                attrs.put(Provisioning.A_mail, mail);
            }

            String aliases[] = dl.getMultiAttr(Provisioning.A_zimbraMailAlias);
            if (aliases.length == 0) {
                attrs.put(Provisioning.A_zimbraMailAlias, newEmail);
            } else {
                // not the most efficient, but renames don't happen often
                aliases = LdapUtil.removeMultiValue(aliases, oldEmail);
                aliases = addMultiValue(aliases, newEmail);
                attrs.put(Provisioning.A_zimbraMailAlias, aliases);
            }
            
            // this is non-atomic. i.e., rename could succeed and updating A_mail
            // could fail. So catch service exception here and log error            
            try {
                dl.modifyAttrsInternal(ctxt, attrs);
            } catch (ServiceException e) {
                ZimbraLog.account.error("distribution list renamed to " + newLocal +
                        " but failed to move old name's LDAP attributes", e);
                throw ServiceException.FAILURE("unable to rename distribution list: "+zimbraId, e);
            }
        } catch (NameAlreadyBoundException nabe) {
            throw AccountServiceException.DISTRIBUTION_LIST_EXISTS(newEmail);            
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to rename distribution list: " + zimbraId, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    private DistributionList getDistributionListById(String zimbraId, DirContext ctxt) throws ServiceException {
        //zimbraId = LdapUtil.escapeSearchFilterArg(zimbraId);
        return getDistributionListByQuery("","(&(zimbraId="+zimbraId+")(objectclass=zimbraDistributionList))", ctxt);
    }
    
    LdapGroupEntry getGroupEntryById(String zimbraGroupId, DirContext initCtxt) throws ServiceException {
        //zimbraId = LdapUtil.escapeSearchFilterArg(zimbraId);
        LdapGroupEntry group = sGroupCache.getByGroupId(zimbraGroupId);
        if (group != null) return group;
        
        String query = "(&(zimbraGroupId="+zimbraGroupId+")(objectclass=zimbraDistributionList)(objectclass=zimbraSecurityGroup))";

        DirContext ctxt = initCtxt;
        try {
            if (ctxt == null)
                ctxt = LdapUtil.getDirContext();
            NamingEnumeration ne = ctxt.search("", query, sGroupSearchControls);            
            
            if (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                ne.close();
                group = sGroupCache.put(new LdapDistributionList(sr.getNameInNamespace(), sr.getAttributes()));
            }
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;                        
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup distribution list via query: "+query, e);
        } finally {
            if (initCtxt == null)
                LdapUtil.closeContext(ctxt);
        }
        return group;        
    }

    public DistributionList getDistributionListById(String zimbraId) throws ServiceException {
        return getDistributionListById(zimbraId, null);
    }

    public DistributionList getDistributionListByGroupId(String zimbraGroupId) throws ServiceException {
        return getDistributionListByQuery("","(&(zimbraGroupId="+zimbraGroupId+")(objectclass=zimbraDistributionList))", null);
    }
    
    public void deleteDistributionList(String zimbraId) throws ServiceException {
        LdapDistributionList dl = (LdapDistributionList) getDistributionListById(zimbraId);
        if (dl == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(zimbraId);

        // if it is a security group, turn of security group to update zimbraMemberOf on all members
        try {
            if (dl.isSecurityGroup())
                dl.setSecurityGroup(false);
        } catch (ServiceException se) {
            ZimbraLog.account.warn("exception while clearing security group", se);
        }
        
        removeAddressFromAllDistributionLists(dl.getName()); // this doesn't throw any exceptions
        
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext(true);
            ctxt.unbind(dl.getDN());
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to purge distribution list: "+zimbraId, e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
    }

    public DistributionList getDistributionListByName(String listAddress) throws ServiceException {
        String parts[] = listAddress.split("@");
        
        if (parts.length != 2)
            throw ServiceException.INVALID_REQUEST("must be valid list address: "+listAddress, null);

        String uid = LdapUtil.escapeSearchFilterArg(parts[0]);
        String domain = parts[1];
        String dn = "ou=people,"+LdapUtil.domainToDN(domain);
        return getDistributionListByQuery(dn, "(&(uid="+uid+")(objectclass=zimbraDistributionList))", null);
    }

    public Server getLocalServer() throws ServiceException {
        String hostname = LC.zimbra_server_hostname.value();
        if (hostname == null) {
            Zimbra.halt("zimbra_server_hostname not specified in localconfig.xml");
        }
        Server local = getServerByName(hostname);
        if (local == null) {
            Zimbra.halt("Could not find an LDAP entry for server '" + hostname + "'");
        }
        return local;
    }

    /**
     * checks to make sure the specified address is a valid email address (addr part only, no personal part) 
     *
     * TODO To change the template for this generated type comment go to
     * Window - Preferences - Java - Code Style - Code Templates
     * @throws ServiceException
     */
    private static void validEmailAddress(String addr) throws ServiceException {
        try {
            InternetAddress ia = new InternetAddress(addr, true);
            // is this even needed?
            ia.validate();
            if (ia.getPersonal() != null && !ia.getPersonal().equals(""))
                throw ServiceException.INVALID_REQUEST("invalid email address", null);
        } catch (AddressException e) {
            throw ServiceException.INVALID_REQUEST("invalid email address", e);
        }
    }
    
    public static final long TIMESTAMP_WINDOW = Constants.MILLIS_PER_MINUTE * 5; 

    public void preAuthAccount(Account acct, String acctValue, String acctBy, long timestamp, long expires, String preAuth) throws ServiceException {
        if (preAuth == null || preAuth.length() == 0)
            throw ServiceException.INVALID_REQUEST("preAuth must not be empty", null);

        // see if domain is configured for preauth
        String domainPreAuthKey = acct.getDomain().getAttr(Provisioning.A_zimbraPreAuthKey, null);
        if (domainPreAuthKey == null)
            throw ServiceException.INVALID_REQUEST("domain is not configured for preauth", null);
        
        // see if request is recent
        long now = System.currentTimeMillis();
        long diff = Math.abs(now-timestamp);
        if (diff > TIMESTAMP_WINDOW)
            throw AccountServiceException.AUTH_FAILED(acct.getName()+" (preauth timestamp is too old)");
        
        // compute expected preAuth
        HashMap<String,String> params = new HashMap<String,String>();
        params.put("account", acctValue);
        params.put("by", acctBy);
        params.put("timestamp", timestamp+"");
        params.put("expires", expires+"");
        String computedPreAuth = PreAuthKey.computePreAuth(params, domainPreAuthKey);
        if (!computedPreAuth.equalsIgnoreCase(preAuth))
            throw AccountServiceException.AUTH_FAILED(acct.getName()+" (preauth mismatch)");
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Account#authAccount(java.lang.String)
     */
    public void authAccount(Account acct, String password) throws ServiceException {
        if (password == null || password.equals(""))
            throw AccountServiceException.AUTH_FAILED(acct.getName()+ " (empty password)");
        authAccount(acct, password, true);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Account#authAccount(java.lang.String)
     */
    private void authAccount(Account acct, String password, boolean checkPasswordPolicy) throws ServiceException {
        acct.reload();
        String accountStatus = acct.getAccountStatus();
        if (accountStatus == null)
            throw AccountServiceException.AUTH_FAILED(acct.getName());
        if (accountStatus.equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE)) 
            throw AccountServiceException.MAINTENANCE_MODE();
        if (!accountStatus.equals(Provisioning.ACCOUNT_STATUS_ACTIVE))
            throw AccountServiceException.AUTH_FAILED(acct.getName());

        verifyPassword(acct, password);

        if (!checkPasswordPolicy)
            return;

        // below this point, the only fault that may be thrown is CHANGE_PASSWORD
        int maxAge = acct.getIntAttr(Provisioning.A_zimbraPasswordMaxAge, 0);
        if (maxAge > 0) {
            Date lastChange = acct.getGeneralizedTimeAttr(Provisioning.A_zimbraPasswordModifiedTime, null);
            if (lastChange != null) {
                long last = lastChange.getTime();
                long curr = System.currentTimeMillis();
                if ((last+(ONE_DAY_IN_MILLIS * maxAge)) < curr)
                    throw AccountServiceException.CHANGE_PASSWORD();
            }
        }

        boolean mustChange = acct.getBooleanAttr(Provisioning.A_zimbraPasswordMustChange, false);
        if (mustChange)
            throw AccountServiceException.CHANGE_PASSWORD();

        // update/check last logon
        Date lastLogon = acct.getGeneralizedTimeAttr(Provisioning.A_zimbraLastLogonTimestamp, null);
        if (lastLogon == null) {
            HashMap attrs = new HashMap();
            attrs.put(Provisioning.A_zimbraLastLogonTimestamp, DateUtil.toGeneralizedTime(new Date()));
            try {
                acct.modifyAttrs(attrs);
            } catch (ServiceException e) {
                ZimbraLog.account.warn("updating zimbraLastLogonTimestamp", e);
            }
        } else {
            Config config = Provisioning.getInstance().getConfig();
            long freq = config.getTimeInterval(
                    Provisioning.A_zimbraLastLogonTimestampFrequency,
                    com.zimbra.cs.util.Config.D_ZIMBRA_LAST_LOGON_TIMESTAMP_FREQUENCY);
            long current = System.currentTimeMillis();
            if (current - freq >= lastLogon.getTime()) {
                HashMap attrs = new HashMap();
                attrs.put(Provisioning.A_zimbraLastLogonTimestamp, DateUtil.toGeneralizedTime(new Date()));
                try {
                    acct.modifyAttrs(attrs);
                } catch (ServiceException e) {
                    ZimbraLog.account.warn("updating zimbraLastLogonTimestamp", e);
                }
            }
        }
    }

    private void externalLdapAuth(Domain d, Account acct, String password) throws ServiceException {
        String url[] = d.getMultiAttr(Provisioning.A_zimbraAuthLdapURL);
        
        if (url == null || url.length == 0) {
            String msg = "attr not set "+Provisioning.A_zimbraAuthLdapURL;
            ZimbraLog.account.fatal(msg);
            throw ServiceException.FAILURE(msg, null);
        }

        try {
            // try explicit externalDn first
            String externalDn = acct.getAttr(Provisioning.A_zimbraAuthLdapExternalDn);

            if (externalDn != null) {
                if (ZimbraLog.account.isDebugEnabled()) ZimbraLog.account.debug("auth with explicit dn of "+externalDn);
                LdapUtil.ldapAuthenticate(url, externalDn, password);
                return;
            }

            String searchFilter = d.getAttr(Provisioning.A_zimbraAuthLdapSearchFilter);
            if (searchFilter != null) {
                String searchPassword = d.getAttr(Provisioning.A_zimbraAuthLdapSearchBindPassword);
                String searchDn = d.getAttr(Provisioning.A_zimbraAuthLdapSearchBindDn);
                String searchBase = d.getAttr(Provisioning.A_zimbraAuthLdapSearchBase);
                if (searchBase == null) searchBase = "";
                searchFilter = LdapUtil.computeAuthDn(acct.getName(), searchFilter);
                if (ZimbraLog.account.isDebugEnabled()) ZimbraLog.account.debug("auth with search filter of "+searchFilter);
                LdapUtil.ldapAuthenticate(url, password, searchBase, searchFilter, searchDn, searchPassword);
                return;
            }
            
            String bindDn = d.getAttr(Provisioning.A_zimbraAuthLdapBindDn);
            if (bindDn != null) {
                String dn = LdapUtil.computeAuthDn(acct.getName(), bindDn);
                if (ZimbraLog.account.isDebugEnabled()) ZimbraLog.account.debug("auth with bind dn template of "+dn);
                LdapUtil.ldapAuthenticate(url, dn, password);
                return;
            }

        } catch (AuthenticationException e) {
            throw AccountServiceException.AUTH_FAILED(acct.getName(), e);
        } catch (AuthenticationNotSupportedException e) {
            throw AccountServiceException.AUTH_FAILED(acct.getName(), e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }
        
        String msg = "one of the following attrs must be set "+
                Provisioning.A_zimbraAuthLdapBindDn+", "+Provisioning.A_zimbraAuthLdapSearchFilter;
        ZimbraLog.account.fatal(msg);
        throw ServiceException.FAILURE(msg, null);
    }

    /*
     * authAccount does all the status/mustChange checks, this just takes the
     * password and auths the user
     */
    private void verifyPassword(Account acct, String password) throws ServiceException {
        String encodedPassword = acct.getAttr(Provisioning.A_userPassword);
        
        String authMech = Provisioning.AM_ZIMBRA;        

        Domain d = acct.getDomain();
        // see if it specifies an alternate auth
        if (d != null) {
            String am = d.getAttr(Provisioning.A_zimbraAuthMech);
            if (am != null)
                authMech = am;
        }
        
        if (authMech.equals(Provisioning.AM_LDAP) || authMech.equals(Provisioning.AM_AD)) {
            boolean allowFallback = 
                d.getBooleanAttr(Provisioning.A_zimbraAuthFallbackToLocal, false) ||
                acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false) ||
                acct.getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);                
            try {
                externalLdapAuth(d, acct, password);
                return;
            } catch (ServiceException e) {
                if (!allowFallback) throw e;
            }
        } else if (!authMech.equals(Provisioning.AM_ZIMBRA)) {
            ZimbraLog.account.warn("unknown value for "+Provisioning.A_zimbraAuthMech+": "+
                    authMech+", falling back to default mech");
            // fallback to zimbra
        }

        // fall back to zimbra
        if (encodedPassword == null)
            throw AccountServiceException.AUTH_FAILED(acct.getName());

        if (!LdapUtil.verifySSHA(encodedPassword, password))
            throw AccountServiceException.AUTH_FAILED(acct.getName());
    }
 
     /**
       * Takes the specified format string, and replaces any % followed by a single character
       * with the value in the specified vars hash. If the value isn't found in the hash, uses
       * a default value of "".
       * @param fmt the format string
       * @param vars should have a key which is a String, and a value which is also a String.
       * @return the formatted string
       */
      static String expandStr(String fmt, Map vars) {
         if (fmt == null || fmt.equals(""))
             return fmt;
         
         if (fmt.indexOf('%') == -1)
             return fmt;
         
         StringBuffer sb = new StringBuffer(fmt.length()+32);
         for (int i=0; i < fmt.length(); i++) {
             char ch = fmt.charAt(i);
             if (ch == '%') {
                 i++;
                 if (i > fmt.length())
                     return sb.toString();
                 ch = fmt.charAt(i);
                 if (ch != '%') {
                     String val = (String) vars.get(Character.toString(ch));
                     if (val != null)
                         sb.append(val);
                     else
                         sb.append(ch);
                 } else {
                     sb.append(ch);
                 }
             } else {
                 sb.append(ch);
             }
         }
         return sb.toString();
     }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Account#changePassword(java.lang.String, java.lang.String)
     */
    public void changePassword(Account acct, String currentPassword, String newPassword) throws ServiceException {
        authAccount(acct, currentPassword, false);
        boolean locked = acct.getBooleanAttr(Provisioning.A_zimbraPasswordLocked, false);
        if (locked)
            throw AccountServiceException.PASSWORD_LOCKED();
        setPassword(acct, newPassword);        
    }

    /**
     * @param newPassword
     * @param multiAttr
     * @throws AccountServiceException
     */
    private void checkHistory(String newPassword, String[] history) throws AccountServiceException {
        if (history == null)
            return;
        for (int i=0; i < history.length; i++) {
            int sepIndex = history[i].indexOf(':');
            if (sepIndex != -1)  {
                String encoded = history[i].substring(sepIndex+1);
                if (LdapUtil.verifySSHA(encoded, newPassword))
                    throw AccountServiceException.PASSWORD_RECENTLY_USED();
            }            
        }
    }



    /**
     * update password history
     * @param history current history
     * @param currentPassword the current encoded-password
     * @param maxHistory number of prev passwords to keep
     * @return new hsitory
     */
    private String[] updateHistory(String history[], String currentPassword, int maxHistory) {
        String[] newHistory = history;
        if (currentPassword == null)
            return null;

        String currentHistory = System.currentTimeMillis() + ":"+currentPassword;
        
        // just add if empty or room
        if (history == null || history.length < maxHistory) {
        
            if (history == null) {
                newHistory = new String[1];
            } else {
                newHistory = new String[history.length+1];
                System.arraycopy(history, 0, newHistory, 0, history.length);
            }
            newHistory[newHistory.length-1] = currentHistory;
            return newHistory;
        }
        
        // remove oldest, add current
        long min = Long.MAX_VALUE;
        int minIndex = -1;
        for (int i = 0; i < history.length; i++) {
            int sepIndex = history[i].indexOf(':');
            if (sepIndex == -1) {
                // nuke it if no separator
                minIndex = i;
                break;
            }
            long val = Long.parseLong(history[i].substring(0, sepIndex));
            if (val < min) {
                min = val;
                minIndex = i;
            }
        }
        if (minIndex == -1)
            minIndex = 0;
        history[minIndex] = currentHistory;
        return history;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Account#setPassword(java.lang.String)
     */
    public void setPassword(Account acct, String newPassword) throws ServiceException {
        setPassword(acct, newPassword, true);
    }

    private int getInt(Attributes attrs, String name, int defaultValue) throws NamingException {
        String v = LdapUtil.getAttrString(attrs, name);
        if (v == null)
            return defaultValue;
        else {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    // called by create account
    private void setPassword(Cos cos, Attributes attrs, String newPassword) throws AccountServiceException, NamingException {
        int minLength = getInt(attrs, Provisioning.A_zimbraPasswordMinLength, -1);
        if (minLength == -1) minLength = cos != null ? cos.getIntAttr(Provisioning.A_zimbraPasswordMinLength, 0) : 0;

        if (minLength > 0 && newPassword.length() < minLength)
            throw AccountServiceException.INVALID_PASSWORD("too short");
        
        int maxLength = getInt(attrs, Provisioning.A_zimbraPasswordMaxLength, -1);
        if (maxLength == -1) maxLength = cos != null ? cos.getIntAttr(Provisioning.A_zimbraPasswordMaxLength, 0) : 0;
        
        if (maxLength > 0 && newPassword.length() > maxLength)
            throw AccountServiceException.INVALID_PASSWORD("too long");


        String encodedPassword = LdapUtil.generateSSHA(newPassword, null);

        attrs.put(Provisioning.A_userPassword, encodedPassword);
        attrs.put(Provisioning.A_zimbraPasswordModifiedTime, DateUtil.toGeneralizedTime(new Date()));
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Account#setPassword(java.lang.String)
     */
    void setPassword(Account acct, String newPassword, boolean enforcePolicy) throws ServiceException {

        if (enforcePolicy) {
            int minLength = acct.getIntAttr(Provisioning.A_zimbraPasswordMinLength, 0);
            if (minLength > 0 && newPassword.length() < minLength)
                throw AccountServiceException.INVALID_PASSWORD("too short");
            int maxLength = acct.getIntAttr(Provisioning.A_zimbraPasswordMaxLength, 0);        
            if (maxLength > 0 && newPassword.length() > maxLength)
                throw AccountServiceException.INVALID_PASSWORD("too long");

            int minAge = acct.getIntAttr(Provisioning.A_zimbraPasswordMinAge, 0);
            if (minAge > 0) {
                Date lastChange = acct.getGeneralizedTimeAttr(Provisioning.A_zimbraPasswordModifiedTime, null);
                if (lastChange != null) {
                    long last = lastChange.getTime();
                    long curr = System.currentTimeMillis();
                    if ((last+(ONE_DAY_IN_MILLIS * minAge)) > curr)
                        throw AccountServiceException.PASSWORD_CHANGE_TOO_SOON();
                }
            }
            
        }            

        HashMap attrs = new HashMap();

        int enforceHistory = acct.getIntAttr(Provisioning.A_zimbraPasswordEnforceHistory, 0);
        if (enforceHistory > 0) {
            String[] newHistory = updateHistory(
                    acct.getMultiAttr(Provisioning.A_zimbraPasswordHistory),
                    acct.getAttr(Provisioning.A_userPassword),                    
                    enforceHistory);
            attrs.put(Provisioning.A_zimbraPasswordHistory, newHistory);
            checkHistory(newPassword, newHistory);
        }

        String encodedPassword = LdapUtil.generateSSHA(newPassword, null);

        boolean mustChange = acct.getBooleanAttr(Provisioning.A_zimbraPasswordMustChange, false);
        // unset it so it doesn't take up space...
        if (mustChange)
            attrs.put(Provisioning.A_zimbraPasswordMustChange, "");

        attrs.put(Provisioning.A_userPassword, encodedPassword);
        attrs.put(Provisioning.A_zimbraPasswordModifiedTime, DateUtil.toGeneralizedTime(new Date()));
        
        acct.modifyAttrs(attrs);
    }
    
    private void createSubcontext(DirContext ctxt, String dn, Attributes attrs, String method)
        throws NameAlreadyBoundException, ServiceException
    {
        Context newCtxt = null;
        try {
            newCtxt = ctxt.createSubcontext(dn, attrs);
        } catch (NameAlreadyBoundException e) {            
            throw e;
       } catch (InvalidAttributeIdentifierException e) {
            throw AccountServiceException.INVALID_ATTR_NAME(method+" invalid attr name", e);
        } catch (InvalidAttributeValueException e) {
            throw AccountServiceException.INVALID_ATTR_VALUE(method+" invalid attr value", e);            
        } catch (InvalidAttributesException e) {
            throw ServiceException.INVALID_REQUEST(method+" invalid set of attributes", e);
        } catch (InvalidNameException e) {
            throw ServiceException.INVALID_REQUEST(method+" invalid name", e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE(method, e);
        } finally {
            LdapUtil.closeContext(newCtxt);
        }
    }

    public Zimlet getZimlet(String name) throws ServiceException {
    	return getZimlet(name, null, true);
    }

    Zimlet lookupZimlet(String name, DirContext ctxt) throws ServiceException {
    	return getZimlet(name, ctxt, false);
    }
    
    private Zimlet getZimlet(String name, DirContext initCtxt, boolean useCache) throws ServiceException {
    	LdapZimlet zimlet = (LdapZimlet) sZimletCache.getByName(name);
    	if (!useCache || zimlet == null) {
        	DirContext ctxt = initCtxt;
        	try {
        		if (ctxt == null) {
        		    ctxt = LdapUtil.getDirContext();
        		}
        		String dn = zimletNameToDN(name);            
        		Attributes attrs = ctxt.getAttributes(dn);
        		zimlet = new LdapZimlet(dn, attrs);
        		if (useCache) {
        			ZimletUtil.reloadZimlet(name);
        			sZimletCache.put(zimlet);  // put LdapZimlet into the cache after successful ZimletUtil.reloadZimlet()
        		}
        	} catch (NamingException ne) {
        		throw ServiceException.FAILURE("unable to get zimlet: "+name, ne);
        	} catch (ZimletException ze) {
        		throw ServiceException.FAILURE("unable to load zimlet: "+name, ze);
            } finally {
            	if (initCtxt == null) {
            		LdapUtil.closeContext(ctxt);
            	}
        	}
    	}
    	return zimlet;
    }
    
    public List listAllZimlets() throws ServiceException {
    	List result = new ArrayList();
    	DirContext ctxt = null;
    	try {
    		ctxt = LdapUtil.getDirContext();
    		NamingEnumeration ne = ctxt.search("", "(objectclass=zimbraZimletEntry)", sSubtreeSC);
    		while (ne.hasMore()) {
    			SearchResult sr = (SearchResult) ne.next();
             result.add(new LdapZimlet(sr.getNameInNamespace(), sr.getAttributes()));
    		}
    		ne.close();
    	} catch (NamingException e) {
    		throw ServiceException.FAILURE("unable to list all zimlets", e);
    	} finally {
    		LdapUtil.closeContext(ctxt);
    	}
    	Collections.sort(result);
    	return result;
    }
    
    public Zimlet createZimlet(String name, Map zimletAttrs) throws ServiceException {
    	name = name.toLowerCase().trim();
    	
    	HashMap attrManagerContext = new HashMap();
    	AttributeManager.getInstance().preModify(zimletAttrs, null, attrManagerContext, true, true);
    	
    	DirContext ctxt = null;
    	try {
    		ctxt = LdapUtil.getDirContext();
    		
    		Attributes attrs = new BasicAttributes(true);
    		String hasKeyword = LdapUtil.LDAP_FALSE;
    		if (zimletAttrs.containsKey(A_zimbraZimletKeyword)) {
    			hasKeyword = LdapUtil.LDAP_TRUE;
    		}
    		LdapUtil.mapToAttrs(zimletAttrs, attrs);
    		LdapUtil.addAttr(attrs, A_objectClass, "zimbraZimletEntry");
    		LdapUtil.addAttr(attrs, A_zimbraZimletEnabled, LdapUtil.LDAP_FALSE);
    		LdapUtil.addAttr(attrs, A_zimbraZimletIndexingEnabled, hasKeyword);
    		
    		String dn = zimletNameToDN(name);
    		createSubcontext(ctxt, dn, attrs, "createZimlet");
    		
    		Zimlet zimlet = lookupZimlet(name, ctxt);
    		AttributeManager.getInstance().postModify(zimletAttrs, zimlet, attrManagerContext, true);
    		return zimlet;
    	} catch (NameAlreadyBoundException nabe) {
    		throw ServiceException.FAILURE("zimlet already exists: "+name, nabe);
    	} catch (ServiceException se) {
    		throw se;
    	} finally {
    		LdapUtil.closeContext(ctxt);
    	}
    }
    
    public void deleteZimlet(String name) throws ServiceException {
    	DirContext ctxt = null;
    	try {
    		ctxt = LdapUtil.getDirContext();
    		LdapZimlet zimlet = (LdapZimlet)lookupZimlet(name, ctxt);
    		ctxt.unbind(zimlet.getDN());
    		sZimletCache.remove(zimlet);
    	} catch (NamingException e) {
    		throw ServiceException.FAILURE("unable to delete zimlet: "+name, e);
    	} finally {
    		LdapUtil.closeContext(ctxt);
    	}
    }
    
    public void addZimletToCOS(String zimlet, String cos) throws ServiceException {
    	DirContext ctxt = LdapUtil.getDirContext();
    	
    	try {
    		lookupZimlet(zimlet, ctxt);
    	} catch (ServiceException e) {
    		LdapUtil.closeContext(ctxt);
    		throw ServiceException.FAILURE("zimlet does not exist: "+zimlet, e);
    	}
    	
    	try {
    		String cosDN = cosNametoDN(cos);
    		LdapUtil.addAttr(ctxt, 
    				cosDN, 
    				A_zimbraZimletAvailableZimlets,
    				zimlet);
    	} catch (NamingException e) {
    		throw ServiceException.FAILURE("unable to add zimlet " + zimlet + " to cos "+cos, e);
    	} finally {
    		LdapUtil.closeContext(ctxt);
    	}
    }
    
    public void removeZimletFromCOS(String zimlet, String cos) throws ServiceException {
    	DirContext ctxt = LdapUtil.getDirContext();
    	
    	try {
    		lookupZimlet(zimlet, ctxt);
    	} catch (ServiceException e) {
    		LdapUtil.closeContext(ctxt);
    		throw ServiceException.FAILURE("zimlet does not exist: "+zimlet, e);
    	}
    	
    	try {
    		String cosDN = cosNametoDN(cos);
    		LdapUtil.removeAttr(ctxt, 
    				cosDN, 
    				A_zimbraZimletAvailableZimlets,
    				zimlet);
    	} catch (NamingException e) {
    		throw ServiceException.FAILURE("unable to remove Zimlet " + zimlet + " from cos "+cos, e);
    	} finally {
    		LdapUtil.closeContext(ctxt);
    	}
    }
    	 
    public void updateZimletConfig(String zimlet, String config) throws ServiceException {
    	DirContext ctxt = LdapUtil.getDirContext();

    	try {
    		Zimlet zim = lookupZimlet(zimlet, ctxt);
    		Map map = new HashMap();
    		map.put(Provisioning.A_zimbraZimletHandlerConfig, config);
    		zim.modifyAttrs(map);
    	} catch (ServiceException e) {
    		LdapUtil.closeContext(ctxt);
    		throw ServiceException.FAILURE("unable to update zimlet config: "+zimlet, e);
    	}
    	
    }

    public void addAllowedDomains(String domains, String cosName) throws ServiceException {
    	Cos cos = getCosByName(cosName);
    	Set domainSet = cos.getMultiAttrSet(Provisioning.A_zimbraProxyAllowedDomains);
    	String[] domainArray = domains.toLowerCase().split(",");
    	for (int i = 0; i < domainArray.length; i++) {
    		domainSet.add(domainArray[i]);
    	}
    	Map newlist = new HashMap();
    	newlist.put(Provisioning.A_zimbraProxyAllowedDomains, domainSet.toArray(new String[0]));
    	cos.modifyAttrs(newlist);
    }

    public void removeAllowedDomains(String domains, String cosName) throws ServiceException {
    	Cos cos = getCosByName(cosName);
    	Set domainSet = cos.getMultiAttrSet(Provisioning.A_zimbraProxyAllowedDomains);
    	String[] domainArray = domains.toLowerCase().split(",");
    	for (int i = 0; i < domainArray.length; i++) {
    		domainSet.remove(domainArray[i]);
    	}
    	Map newlist = new HashMap();
    	newlist.put(Provisioning.A_zimbraProxyAllowedDomains, domainSet.toArray(new String[0]));
    	cos.modifyAttrs(newlist);
    }

    public CalendarResource createCalendarResource(String emailAddress,
                                                   Map calResAttrs)
    throws ServiceException {
        emailAddress = emailAddress.toLowerCase().trim();

        calResAttrs.put(Provisioning.A_zimbraAccountCalendarUserType,
                        Account.CalendarUserType.RESOURCE.toString());

        HashMap attrManagerContext = new HashMap();
        AttributeManager.getInstance().
            preModify(calResAttrs, null, attrManagerContext, true, true);
        createAccount(emailAddress, null, calResAttrs,
                      new String[] { C_zimbraCalendarResource });
        LdapCalendarResource resource =
            (LdapCalendarResource) getCalendarResourceByName(emailAddress);
        AttributeManager.getInstance().
            postModify(calResAttrs, resource, attrManagerContext, true);
        return resource;
    }

    public void deleteCalendarResource(String zimbraId)
    throws ServiceException {
        deleteAccount(zimbraId);
    }

    public void renameCalendarResource(String zimbraId, String newName)
    throws ServiceException {
        renameAccount(zimbraId, newName);
    }

    public CalendarResource getCalendarResourceById(String zimbraId)
    throws ServiceException {
        if (zimbraId == null)
            return null;
        LdapCalendarResource resource =
            (LdapCalendarResource) sAccountCache.getById(zimbraId);
        if (resource == null) {
            zimbraId = LdapUtil.escapeSearchFilterArg(zimbraId);
            resource = (LdapCalendarResource) getAccountByQuery(
                "",
                "(&(zimbraId=" + zimbraId + ")" +
                FILTER_CALENDAR_RESOURCE_OBJECTCLASS + ")",
                null);
            sAccountCache.put(resource);
        }
        return resource;
    }

    public CalendarResource getCalendarResourceByName(String emailAddress)
    throws ServiceException {
        int index = emailAddress.indexOf('@');
        String domain = null;
        if (index == -1) {
             domain = getConfig().getAttr(
                     Provisioning.A_zimbraDefaultDomainName, null);
            if (domain == null)
                throw ServiceException.INVALID_REQUEST(
                        "must be valid email address: "+ emailAddress, null);
            else
                emailAddress = emailAddress + "@" + domain;
         }

        LdapCalendarResource resource =
            (LdapCalendarResource) sAccountCache.getByName(emailAddress);
        if (resource == null) {
            emailAddress = LdapUtil.escapeSearchFilterArg(emailAddress);
            resource = (LdapCalendarResource) getAccountByQuery(
                "",
                "(&(|(zimbraMailDeliveryAddress=" + emailAddress +
                ")(zimbraMailAlias=" + emailAddress + "))" +
                FILTER_CALENDAR_RESOURCE_OBJECTCLASS + ")",
                null);
            sAccountCache.put(resource);
        }
        return resource;
    }

    public CalendarResource getCalendarResourceByForeignPrincipal(
            String foreignPrincipal)
    throws ServiceException {
        LdapCalendarResource res = null;
        foreignPrincipal = LdapUtil.escapeSearchFilterArg(foreignPrincipal);
        LdapCalendarResource resource =
            (LdapCalendarResource) getAccountByQuery(
                "",
                "(&(zimbraForeignPrincipal=" + foreignPrincipal + ")" +
                FILTER_CALENDAR_RESOURCE_OBJECTCLASS + ")",
                null);
        sAccountCache.put(resource);
        return resource;
    }

    public List searchCalendarResources(
        EntrySearchFilter filter,
        String returnAttrs[],
        String sortAttr,
        boolean sortAscending)
    throws ServiceException {
        return searchCalendarResources(filter, returnAttrs,
                                       sortAttr, sortAscending,
                                       "");
    }

    List searchCalendarResources(
        EntrySearchFilter filter,
        String returnAttrs[],
        String sortAttr,
        boolean sortAscending,
        String base)
    throws ServiceException {
        String query = LdapEntrySearchFilter.toLdapCalendarResourcesFilter(filter);
        return searchObjects(query, returnAttrs,
                              sortAttr, sortAscending,
                              base,
                              Provisioning.SA_CALENDAR_RESOURCE_FLAG);
    }

    private static LdapAccount makeLdapAccount(String dn,
                                               Attributes attrs,
                                               LdapProvisioning prov)
    throws ServiceException {
        LdapAccount account = new LdapAccount(dn, attrs, prov);
        if (account.getCalendarUserType().
                equals(Account.CalendarUserType.RESOURCE))
            return new LdapCalendarResource(account);
        else
            return account;
    }

    /**
     *  called when an account/dl is renamed
     *  
     */
    void renameAddressInAllDistributionLists(String oldName, String newName)
    {
        String addrs[] = new String[] { oldName };
        List<DistributionList> lists = null; 
        HashMap attrs = null;
        
        try {
            lists = getAllDistributionListsForAddresses(addrs);
        } catch (ServiceException se) {
            ZimbraLog.account.warn("unable to rename addr "+oldName+" in all DLs ", se);
            return;
        }
        
        for (DistributionList list: lists) {
            // should we just call removeMember/addMember? This might be quicker, because calling
            // removeMember/addMember might have to update an entry's zimbraMemberId twice
            if (attrs == null) {
                attrs = new HashMap();
                attrs.put("-"+Provisioning.A_zimbraMailForwardingAddress, oldName);
                attrs.put("+"+Provisioning.A_zimbraMailForwardingAddress, newName);                
            }
            try {
                list.modifyAttrs(attrs);
                //list.removeMember(oldName)
                //list.addMember(newName);                
            } catch (ServiceException se) {
                // log warning an continue
                ZimbraLog.account.warn("unable to rename "+oldName+" to "+newName+" in DL "+list.getName(), se);
            }
        }
    }

    /**
     *  called when an account is being deleted. swallows all exceptions (logs warnings).
     */
    void removeAddressFromAllDistributionLists(String address)
    {
        String addrs[] = new String[] { address } ;
        List<DistributionList> lists = null; 
        HashMap attrs = null;
        try {
            lists = getAllDistributionListsForAddresses(addrs);
        } catch (ServiceException se) {
            ZimbraLog.account.warn("unable to remove "+address+" from all DLs ", se);
            return;
        }

        for (DistributionList list: lists) { 
            try {
                list.removeMember(address);                
            } catch (ServiceException se) {
                // log warning and continue
                ZimbraLog.account.warn("unable to remove "+address+" from DL "+list.getName(), se);
            }
        }
    }

    static String[] getAllAddrsForDistributionList(DistributionList list) throws ServiceException {
        String aliases[] =list.getAliases();
        String addrs[] = new String[aliases.length+1];
        addrs[0] = list.getName();
        for (int i=0; i < aliases.length; i++)
            addrs[i+1] = aliases[i];
        return addrs;
    }

    public List<DistributionList> getAllDistributionListsForAddresses(String addrs[]) throws ServiceException {
        if (addrs == null || addrs.length == 0) return new ArrayList<DistributionList>();
        StringBuilder sb = new StringBuilder();
        if (addrs.length > 1) sb.append("(&");
        for (int i=0; i < addrs.length; i++) {
            sb.append(String.format("(%s=%s)", Provisioning.A_zimbraMailForwardingAddress, addrs[0]));    
        }
        if (addrs.length > 1) sb.append(")");
        return (List<DistributionList>)searchAccounts(sb.toString(), null, null, true, Provisioning.SA_DISTRIBUTION_LIST_FLAG);        
    }
    
    public static void main(String args[]) {
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", null));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", ""));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "WTF"));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "%n"));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "%u"));        
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "%d"));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "%D"));                
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "uid=%u,ou=people,%D"));
        System.out.println(LdapUtil.computeAuthDn("schemers@example.zimbra.com", "n(%n)u(%u)d(%d)D(%D)(%%)"));
    }  

}
