/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.*;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.mime.MimeTypeInfo;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.EmailUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

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

    private static final long ONE_DAY_IN_MILLIS = 1000*60*60*24;

    private static final SearchControls sObjectSC = new SearchControls(SearchControls.OBJECT_SCOPE, 0, 0, null, true, false);

    static final SearchControls sSubtreeSC = new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, null, true, false);
    
    private static Log mLog = LogFactory.getLog(LdapProvisioning.class);
    
    private static LdapConfig sConfig = null;
    
    private static Pattern sValidCosName = Pattern.compile("^\\w+$");

    private static final String[] sInvalidAccountCreateModifyAttrs = {
            Provisioning.A_zimbraMailAlias,
            Provisioning.A_zimbraMailDeliveryAddress,
            Provisioning.A_uid,
            Provisioning.A_userPassword
    };
    
    /**
     *  TODO: 15 minutes. get from CONFIG...
     */
    private static final int ENTRY_TTL = 1000*60*15; 
    private static final int MAX_ACCOUNT_CACHE = 5000;
    private static ZimbraLdapEntryCache sAccountCache = new ZimbraLdapEntryCache(MAX_ACCOUNT_CACHE, ENTRY_TTL);
	
	private static final int MAX_COS_CACHE = 100;
    private static ZimbraLdapEntryCache sCosCache = new ZimbraLdapEntryCache(MAX_COS_CACHE, ENTRY_TTL);
	
    private static final int MAX_DOMAIN_CACHE = 100;
    private static ZimbraLdapEntryCache sDomainCache = new ZimbraLdapEntryCache(MAX_DOMAIN_CACHE, ENTRY_TTL);
    
    private static final int MAX_SERVER_CACHE = 100;
    private static ZimbraLdapEntryCache sServerCache = new ZimbraLdapEntryCache(MAX_SERVER_CACHE, ENTRY_TTL);

    private static final int MAX_TIMEZONE_CACHE = 100;
    private static boolean sTimeZoneInited = false;
    private static final Object sTimeZoneGuard = new Object();
    private static Map /*<String tzId, WellKnownTimeZone>*/ sTimeZoneMap = new HashMap(MAX_TIMEZONE_CACHE);
    // list of time zones to preserve sort order
    private static List /*<WellKnownTimeZone>*/ sTimeZoneList = new ArrayList(MAX_TIMEZONE_CACHE);

    private static Map /*<String name, Zimlet>*/ sZimletMap = new HashMap();

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
                Context srctxt = (Context) sr.getObject();
                String name = srctxt.getNameInNamespace();
                srctxt.close();
                ne.close();
                return new LdapMimeType(name, sr.getAttributes());
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
    	return getZimlets();
    }

    private LdapAccount getAccountByQuery(String base, String query, DirContext initCtxt) throws ServiceException {
        DirContext ctxt = initCtxt;
        try {
            if (ctxt == null)
                ctxt = LdapUtil.getDirContext();
            NamingEnumeration ne = ctxt.search(base, query, sSubtreeSC);
            if (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                // GAG
                Context srctxt = (Context) sr.getObject();
                String name = srctxt.getNameInNamespace();
                srctxt.close();
                ne.close();
                return new LdapAccount(name, sr.getAttributes(), this);
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

    private synchronized Account getAccountById(String zimbraId, DirContext ctxt) throws ServiceException {
        if (zimbraId == null)
            return null;
        LdapAccount a = (LdapAccount) sAccountCache.getById(zimbraId);
        if (a == null) {
            zimbraId= LdapUtil.escapeSearchFilterArg(zimbraId);
       		a = getAccountByQuery("","(&(zimbraId="+zimbraId+")(objectclass=zimbraAccount))", ctxt);
            sAccountCache.put(a);
        }
        return a;
    }
    
    public synchronized Account getAccountById(String zimbraId) throws ServiceException {
        return getAccountById(zimbraId, null);
    }

    public synchronized Account getAdminAccountByName(String name) throws ServiceException {
        LdapAccount a = (LdapAccount) sAccountCache.getByname(name);
        if (a == null) {
            name = LdapUtil.escapeSearchFilterArg(name);
            a = getAccountByQuery(ADMIN_BASE, "(&(uid="+name+")(objectclass=zimbraAccount))", null);
            sAccountCache.put(a);
        }
        return a;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getDomainByName(java.lang.String)
     */
    public synchronized Account getAccountByName(String emailAddress) throws ServiceException {
        
        int index = emailAddress.indexOf('@');
        String domain = null;
        if (index == -1) {
             domain = getConfig().getAttr(Provisioning.A_zimbraDefaultDomainName, null);
            if (domain == null)
                throw ServiceException.INVALID_REQUEST("must be valid email address: "+emailAddress, null);
            else
                emailAddress = emailAddress + "@" + domain;            
         }
        
        LdapAccount account = (LdapAccount) sAccountCache.getByname(emailAddress);
        if (account == null) {
            emailAddress = LdapUtil.escapeSearchFilterArg(emailAddress);
            account = getAccountByQuery("", "(&(|(zimbraMailDeliveryAddress="+emailAddress+")(zimbraMailAlias="+emailAddress+"))(objectclass=zimbraAccount))", null);
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
  
    private Cos lookupCos(String key) throws ServiceException {
        Cos c = null;
        switch(guessType(key)) {
        case BY_ID:
            c = getCosById(key);
            break;
        case BY_NAME:
            c = getCosByName(key);
            break;
        }
        if (c == null)
            throw AccountServiceException.NO_SUCH_COS(key);
        else
            return c;
    }
    
    public Account createAccount(String emailAddress, String password, Map acctAttrs) throws ServiceException {

        emailAddress = emailAddress.toLowerCase().trim();

        HashMap attrManagerContext = new HashMap();
        AttributeManager.getInstance().preModify(acctAttrs, null, attrManagerContext, true, true);

        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();

            String parts[] = emailAddress.split("@");
            if (parts.length != 2)
                throw ServiceException.INVALID_REQUEST("must be valid email address: "+emailAddress, null);
            
            String uid = parts[0];
            String domain = parts[1];

            Domain d = getDomainByName(domain);
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
                cos = lookupCos(cosId);
                if (!cos.getId().equals(cosId)) {
                    cosId = cos.getId();
                }
                attrs.put(Provisioning.A_zimbraCOSId, cosId);
            } else {
                cos = getCosByName(Provisioning.DEFAULT_COS_NAME);
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

            String dn = emailToDN(uid, domain);
            createSubcontext(ctxt, dn, attrs, "createAccount");
            LdapAccount acct = (LdapAccount) getAccountById(zimbraIdStr, ctxt);
            // set password using the real account object, so we correctly maintain zimbraPassword* attrs
            // don't reset passwordMustChange attr if we want it changed on first login...
            if (password != null) {
                setPassword(acct, password, true, false);
            }
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
            ctxt = LdapUtil.getDirContext();
            
            String parts[] = emailAddress.split("@");
            
            if (parts.length != 2)
                throw ServiceException.INVALID_REQUEST("must be valid email address: "+emailAddress, null);
            
            String uid = parts[0];
            String domain = parts[1];

            Domain d = getDomainByName(domain);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);

            rctxt = LdapUtil.getDirContext(new String[] {remoteURL}, remoteBindDn, remoteBindPassword);
            
            String dn = domainToAccountBaseDN(domain);
            LdapAccount remoteAccount =
                getAccountByQuery(dn, "(&(|(uid="+uid+")(zimbraMailAlias="+emailAddress+"))(objectclass=zimbraAccount))", rctxt);
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
            ctxt = LdapUtil.getDirContext();

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
        return searchAccounts("(zimbraIsAdminAccount=TRUE)", null, null, true, Provisioning.SA_ACCOUNT_FLAG);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#searchAccounts(java.lang.String)
     */
    public ArrayList searchAccounts(String query, String returnAttrs[], final String sortAttr, final boolean sortAscending, int flags)  
        throws ServiceException
    {
        return searchAccounts(query, returnAttrs, sortAttr, sortAscending, "", flags);          
    }

    private static String getObjectClassQuery(int flags) {
        boolean accounts = (flags & Provisioning.SA_ACCOUNT_FLAG) != 0; 
        boolean aliases = (flags & Provisioning.SA_ALIAS_FLAG) != 0;
        boolean lists = (flags & Provisioning.SA_DISTRIBUTION_LIST_FLAG) != 0;
        int num = (accounts ? 1 : 0) + (aliases ? 1 : 0) + (lists ? 1 : 0);

        if (num == 0) {
            accounts = true;
            num = 1;
        }

        StringBuffer oc = new StringBuffer();
        
        if (num > 1) oc.append("(|");
        if (accounts) oc.append("(objectclass=zimbraAccount)");
        if (aliases) oc.append("(objectclass=zimbraAlias)");
        if (lists) oc.append("(objectclass=zimbraDistributionList)");        
        if (num > 1) oc.append(")");
        return oc.toString();
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#searchAccounts(java.lang.String)
     */
    ArrayList searchAccounts(String query, String returnAttrs[], final String sortAttr, final boolean sortAscending, String base, int flags)  
        throws ServiceException
    {
        ArrayList result = new ArrayList();
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
                new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, returnAttrs, true, false);

            // we don't want to ever cache any of these, since they might not have all their attributes

            NamingEnumeration ne = ctxt.search(base, query, searchControls);
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                Context srctxt = (Context) sr.getObject();
                String dn = srctxt.getNameInNamespace();
                srctxt.close();
                // skip admin accounts
                if (dn.endsWith("cn=zimbra")) continue;
                Attributes attrs = sr.getAttributes();
                Attribute objectclass = attrs.get("objectclass");
                if (objectclass == null || objectclass.contains("zimbraAccount")) result.add(new LdapAccount(dn, attrs, this));
                else if (objectclass.contains("zimbraAlias")) result.add(new LdapAlias(dn, attrs));
                else if (objectclass.contains("zimbraDistributionList")) result.add(new LdapDistributionList(dn, attrs));                
            }
            ne.close();
        } catch (NameNotFoundException e) {
            return result;
        } catch (InvalidNameException e) {
            return result;
        } catch (InvalidSearchFilterException e) {
            throw ServiceException.INVALID_REQUEST("invalid search filter "+e.getMessage(), e);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all accounts", e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }

        if (sortAttr != null) {
            Comparator comparator = new Comparator() {
                public int compare(Object oa, Object ob) {
                    LdapNamedEntry a = (LdapNamedEntry) oa;
                    LdapNamedEntry b = (LdapNamedEntry) ob;
                    String sa = a.getAttr(sortAttr);
                    String sb = b.getAttr(sortAttr);
                    int result = (sa == null || sb == null) ? -1 : sa.compareTo(sb);
                    return sortAscending ? result : -result;
                }
            };
            Collections.sort(result, comparator);        
        } else {
            Collections.sort(result);
        }
        return result;
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
        }
        
        int num = (needUID ? 1 : 0) + (needID ? 1 : 0) + (needCOSId ? 1 : 0) + (needAliasTargetId ? 1 : 0) + (needObjectClass ? 1 :0);
        
        if (num == 0) return returnAttrs;
       
        String[] result = new String[returnAttrs.length+num];
        int i = 0;
        if (needUID) result[i++] = Provisioning.A_uid;
        if (needID) result[i++] = Provisioning.A_zimbraId;
        if (needCOSId) result[i++] = Provisioning.A_zimbraCOSId;
        if (needAliasTargetId) result[i++] = Provisioning.A_zimbraAliasTargetId;
        if (needObjectClass) result[i++] = Provisioning.A_objectClass;
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
        ArrayList list = new ArrayList(Arrays.asList(values));        
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

        Domain domain = getDomainByName(aliasDomain);
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(aliasDomain);
        
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
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

        Domain domain = getDomainByName(aliasDomain);
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(aliasDomain);
        
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
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
            
            try {
                Attributes aliasAttrs = ctxt.getAttributes(aliasDn);
                // make sure aliasedObjectName points to this account
                Attribute a = aliasAttrs.get(Provisioning.A_zimbraAliasTargetId);
                if ( a != null && ( (String)a.get()).equals(entry.getId())) {
                    ctxt.unbind(aliasDn);
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
        
        LdapDomain d = (LdapDomain) getDomainByName(name);
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
        
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
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
            
            /*
             * LdapUtil.simpleCreate(ctxt, "ou=groups,"+dn, "organizationalRole",
             * new String[] { A_ou, "groups", A_cn, "groups"});
             */
            
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
                Context srctxt = (Context) sr.getObject();
                String name = srctxt.getNameInNamespace();
                ne.close();
                srctxt.close();
                return new LdapDomain(name, sr.getAttributes(), this);
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

    private synchronized Domain getDomainById(String zimbraId, DirContext ctxt) throws ServiceException {
        if (zimbraId == null)
            return null;
        LdapDomain domain = (LdapDomain) sDomainCache.get(zimbraId);
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
    public synchronized Domain getDomainById(String zimbraId) throws ServiceException {
        return getDomainById(zimbraId, null);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getDomainByName(java.lang.String)
     */
    public synchronized Domain getDomainByName(String name) throws ServiceException {
        LdapDomain domain = (LdapDomain) sDomainCache.get(name);
        if (domain == null) {
            name = LdapUtil.escapeSearchFilterArg(name);
            domain = getDomainByQuery("(&(zimbraDomainName="+name+")(objectclass=zimbraDomain))", null);
            sDomainCache.put(domain);
        }
        return domain;        
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getAllDomains()
     */
    public List getAllDomains() throws ServiceException {
        ArrayList result = new ArrayList();
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            NamingEnumeration ne = ctxt.search("", "(objectclass=zimbraDomain)", sSubtreeSC);
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                Context srctxt = (Context) sr.getObject();
                result.add(new LdapDomain(srctxt.getNameInNamespace(), sr.getAttributes(), this));
                srctxt.close();
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
            ctxt = LdapUtil.getDirContext();

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
            ctxt = LdapUtil.getDirContext();
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
                Context srctxt = (Context) sr.getObject();
                String name = srctxt.getNameInNamespace();
                srctxt.close();
                ne.close();
                return new LdapCos(name, sr.getAttributes(), this);
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
    private synchronized Cos getCosById(String zimbraId, DirContext ctxt ) throws ServiceException {
        if (zimbraId == null)
            return null;

        LdapCos cos = (LdapCos) sCosCache.get(zimbraId);
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
    public synchronized Cos getCosById(String zimbraId) throws ServiceException {
        return getCosById(zimbraId, null);
    }    

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getCOSByName(java.lang.String)
     */
    public synchronized Cos getCosByName(String name) throws ServiceException {
        LdapCos cos = (LdapCos) sCosCache.get(name);
        if (cos != null)
            return cos;

        DirContext ctxt = null;
        try {
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
            LdapUtil.closeContext(ctxt);
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#getAllCOS()
     */
    public List getAllCos() throws ServiceException {
        ArrayList result = new ArrayList();
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            NamingEnumeration ne = ctxt.search(COS_BASE, "(objectclass=zimbraCOS)", sSubtreeSC);
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                Context srctxt = (Context) sr.getObject();
                result.add(new LdapCos(srctxt.getNameInNamespace(), sr.getAttributes(),this));
                srctxt.close();
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

        String aliases[] = acc.getAliases();
        if (aliases != null)
            for (int i=0; i < aliases.length; i++)
                removeAlias(acc, aliases[i]);

        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
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

        LdapAccount acc = (LdapAccount) getAccountById(zimbraId);
        if (acc == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(zimbraId);

        String oldEmail = acc.getName();
        
        newName = newName.toLowerCase().trim();
        String[] parts = EmailUtil.getLocalPartAndDomain(newName);
        if (parts == null)
            throw ServiceException.INVALID_REQUEST("bad value for newName", null);
        String newLocal = parts[0];
        String newDomain = parts[1];
        
        Domain domain = getDomainByName(newDomain);
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(newDomain);
        
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            String newDn = emailToDN(newLocal,domain.getName());
            ctxt.rename(acc.mDn, newDn);
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
        LdapDomain d = (LdapDomain) getDomainById(zimbraId);
        if (d == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(zimbraId);
        
        // TODO: should only allow a domain delete to succeed if there are no people/groups.
        // if there aren't, we need to delete the group/people trees first, then delete the domain.
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            ctxt.unbind("ou=people,"+d.getDN());
            ctxt.unbind(d.getDN());
            sDomainCache.remove(d);
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
            ctxt = LdapUtil.getDirContext();
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

        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();

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
                Context srctxt = (Context) sr.getObject();
                String name = srctxt.getNameInNamespace();
                srctxt.close();
                ne.close();
                return new LdapServer(name, sr.getAttributes(), this);
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

    private synchronized Server getServerById(String zimbraId, DirContext ctxt, boolean nocache) throws ServiceException {
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
    public synchronized Server getServerByName(String name) throws ServiceException {
        return getServerByName(name, false);
    }

    public Server getServerByName(String name, boolean nocache) throws ServiceException {
        if (!nocache) {
        	LdapServer s = (LdapServer) sServerCache.getByname(name);
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

    public synchronized List getAllServers() throws ServiceException {
        ArrayList result = new ArrayList();
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            NamingEnumeration ne = ctxt.search(SERVER_BASE, "(objectclass=zimbraServer)", sSubtreeSC);
            synchronized (sServerCache) {
                sServerCache.clear();
                while (ne.hasMore()) {
                    SearchResult sr = (SearchResult) ne.next();
                    Context srctxt = (Context) sr.getObject();
                    LdapServer s = new LdapServer(srctxt.getNameInNamespace(), sr.getAttributes(), this);
                    LdapServer cs = (LdapServer) sServerCache.getById((s.getId()));
                    sServerCache.put(s);
                    result.add(s);
                    srctxt.close();
                }
            }
            ne.close();
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all servers", e);
        } finally {
            LdapUtil.closeContext(ctxt);
        }
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
            ctxt = LdapUtil.getDirContext();
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
                    Context srctxt = (Context) sr.getObject();
                    LdapWellKnownTimeZone tz = new LdapWellKnownTimeZone(srctxt.getNameInNamespace(), sr.getAttributes());
                    sTimeZoneMap.put(tz.getId(), tz);
                    sTimeZoneList.add(tz);
                    srctxt.close();
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
            ctxt = LdapUtil.getDirContext();

            String parts[] = listAddress.split("@");
            if (parts.length != 2)
                throw ServiceException.INVALID_REQUEST("must be valid list address: " + listAddress, null);

            String list = parts[0];
            String domain = parts[1];

            Domain d = getDomainByName(domain);
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
                // GAG
                Context srctxt = (Context) sr.getObject();
                String name = srctxt.getNameInNamespace();
                srctxt.close();
                ne.close();
                return new LdapDistributionList(name, sr.getAttributes());
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
        LdapDistributionList dl = (LdapDistributionList) getDistributionListById(zimbraId);
        if (dl == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(zimbraId);

        String oldEmail = dl.getName();

        newEmail = newEmail.toLowerCase().trim();
        String[] parts = EmailUtil.getLocalPartAndDomain(newEmail);
        if (parts == null)
            throw ServiceException.INVALID_REQUEST("bad value for newName", null);
        String newLocal = parts[0];
        String newDomain = parts[1];

        Domain domain = getDomainByName(newDomain);
        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(newDomain);
        
        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
            String newDn = emailToDN(newLocal, domain.getName());
            ctxt.rename(dl.mDn, newDn);
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
            throw AccountServiceException.DISTRIBUTION_LIST_EXISTS(newLocal);            
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
    
    public DistributionList getDistributionListById(String zimbraId) throws ServiceException {
        return getDistributionListById(zimbraId, null);
    }

    public void deleteDistributionList(String zimbraId) throws ServiceException {
        LdapDistributionList dl = (LdapDistributionList) getDistributionListById(zimbraId);
        if (dl == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(zimbraId);

        DirContext ctxt = null;
        try {
            ctxt = LdapUtil.getDirContext();
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

    public synchronized Server getLocalServer() throws ServiceException {
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
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Account#authAccount(java.lang.String)
     */
    public void authAccount(Account acct, String password) throws ServiceException {
        if (password == null || password.equals(""))
            throw AccountServiceException.AUTH_FAILED("empty password");
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
            attrs.put(Provisioning.A_zimbraLastLogonTimestamp, LdapUtil.generalizedTime(new Date()));
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
                attrs.put(Provisioning.A_zimbraLastLogonTimestamp, LdapUtil.generalizedTime(new Date()));
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
            boolean allowFallback = d.getBooleanAttr(Provisioning.A_zimbraAuthFallbackToLocal, false);
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
        setPassword(acct, newPassword, true, true);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Account#setPassword(java.lang.String)
     */
    void setPassword(Account acct, String newPassword, boolean enforcePolicy, boolean checkMustChange) throws ServiceException {

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

        if (checkMustChange) {
            boolean mustChange = acct.getBooleanAttr(Provisioning.A_zimbraPasswordMustChange, false);
            // unset it so it doesn't take up space...
            if (mustChange)
                attrs.put(Provisioning.A_zimbraPasswordMustChange, "");
        }

        attrs.put(Provisioning.A_userPassword, encodedPassword);
        attrs.put(Provisioning.A_zimbraPasswordModifiedTime, LdapUtil.generalizedTime(new Date()));
        
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

    public List getZimlets() throws ServiceException {
    	ArrayList result = new ArrayList();
    	DirContext ctxt = null;
    	try {
    		ctxt = LdapUtil.getDirContext();
    		NamingEnumeration ne = ctxt.search("", "(objectclass=zimbraZimletEntry)", sSubtreeSC);
    		while (ne.hasMore()) {
    			SearchResult sr = (SearchResult) ne.next();
    			Context srctxt = (Context) sr.getObject();
    			result.add(new LdapZimlet(srctxt.getNameInNamespace(), sr.getAttributes()));
    			srctxt.close();
    		}
    		ne.close();
    	} catch (NamingException e) {
    		throw ServiceException.FAILURE("unable to list all Zimlets", e);
    	} finally {
    		LdapUtil.closeContext(ctxt);
    	}
    	return result;
    }
    
    private Zimlet getZimlet(String name, DirContext ctxt) throws ServiceException {
    	try {
    		ctxt = LdapUtil.getDirContext();
    		String dn = zimletNameToDN(name);            
    		Attributes attrs = ctxt.getAttributes(dn);
    		Zimlet z = new LdapZimlet(dn, attrs);
    		return z;
    	} catch (NameNotFoundException e) {
    		return null;
    	} catch (InvalidNameException e) {
    		return null;            
    	} catch (NamingException e) {
    		throw ServiceException.FAILURE("unable to get zimlet by name: "+name, e);
    	} finally {
    		LdapUtil.closeContext(ctxt);
    	}
    }
    
    public Zimlet createZimlet(String name, Map zimletAttrs) throws ServiceException {
    	name = name.toLowerCase().trim();
    	
    	HashMap attrManagerContext = new HashMap();
    	AttributeManager.getInstance().preModify(zimletAttrs, null, attrManagerContext, true, true);
    	
    	DirContext ctxt = null;
    	try {
    		ctxt = LdapUtil.getDirContext();
    		
    		Attributes attrs = new BasicAttributes(true);
    		LdapUtil.mapToAttrs(zimletAttrs, attrs);
    		LdapUtil.addAttr(attrs, A_objectClass, "zimbraZimletEntry");
    		
    		String dn = zimletNameToDN(name);
    		createSubcontext(ctxt, dn, attrs, "createZimlet");
    		
    		Zimlet zimlet = getZimlet(name, ctxt);
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
    		LdapZimlet zimlet = (LdapZimlet)getZimlet(name, ctxt);
    		ctxt.unbind(zimlet.getDN());
    	} catch (NamingException e) {
    		throw ServiceException.FAILURE("unable to delete zimlet: "+name, e);
    	} finally {
    		LdapUtil.closeContext(ctxt);
    	}
    }
    
    public void addZimletToCOS(String zimlet, String cos) throws ServiceException {
    	DirContext ctxt = LdapUtil.getDirContext();
    	
    	try {
    		getZimlet(zimlet, ctxt);
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
    		throw ServiceException.FAILURE("unable to add Zimlet " + zimlet + " to cos "+cos, e);
    	} finally {
    		LdapUtil.closeContext(ctxt);
    	}
    }
    
    public void removeZimletFromCOS(String zimlet, String cos) throws ServiceException {
    	DirContext ctxt = LdapUtil.getDirContext();
    	
    	try {
    		getZimlet(zimlet, ctxt);
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
