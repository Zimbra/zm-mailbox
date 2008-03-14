/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.AccountLogger;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CacheEntryBy;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DataSourceBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.Provisioning.SignatureBy;
import com.zimbra.cs.account.ldap.LdapEntrySearchFilter;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning.MailboxInfo;
import com.zimbra.cs.account.soap.SoapProvisioning.ReIndexBy;
import com.zimbra.cs.account.soap.SoapProvisioning.ReIndexInfo;
import com.zimbra.cs.account.soap.SoapProvisioning.QuotaUsage;
import com.zimbra.cs.extension.ExtensionDispatcherServlet;
import com.zimbra.cs.fb.FbCli;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.wiki.WikiUtil;
import com.zimbra.cs.zclient.ZClientException;
import com.zimbra.cs.zclient.ZMailboxUtil;
import com.zimbra.common.soap.SoapTransport.DebugListener;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashSet;

/**
 * @author schemers
 */
public class ProvUtil implements DebugListener {
    
    private static final String ERR_VIA_SOAP_ONLY = "can only be used with SOAP";
    private static final String ERR_VIA_LDAP_ONLY = "can only be used with  \"zmprov -l/--ldap\"";
 
    private boolean mInteractive = false;
    private boolean mVerbose = false;
    private boolean mDebug = false;
    private boolean mUseLdap = LC.zimbra_zmprov_default_to_ldap.booleanValue(); 
    private String mAccount = null;
    private String mPassword = null;
    private String mServer = LC.zimbra_zmprov_default_soap_server.value();
    private int mPort = LC.zimbra_admin_service_port.intValue();
    private Command mCommand;
    
    private Map<String,Command> mCommandIndex;
    private Provisioning mProv;
    private BufferedReader mReader;
    
    public void setDebug(boolean debug) { mDebug = debug; }
    
    public void setVerbose(boolean verbose) { mVerbose = verbose; }
    
    public void setUseLdap(boolean useLdap) { mUseLdap = useLdap; }

    public void setAccount(String account) { mAccount = account; mUseLdap = false;}

    public void setPassword(String password) { mPassword = password; mUseLdap = false; }
    
    public void setServer(String server ) {
        int i = server.indexOf(":");
        if (i == -1) {
            mServer = server;
        } else {
            mServer = server.substring(0, i);
            mPort = Integer.parseInt(server.substring(i+1));
        }
        mUseLdap = false;
    }

    private void usage() {
        usage(null);
    }

    private void usage(Command.Via violatedVia) {
        if (mCommand != null) {
            if (violatedVia == null)
                System.out.printf("usage:  %s(%s) %s\n", mCommand.getName(), mCommand.getAlias(), mCommand.getHelp());
            else {
                if (violatedVia == Command.Via.ldap)
                    System.out.printf("%s %s\n", mCommand.getName(), ERR_VIA_LDAP_ONLY);
                else
                    System.out.printf("%s %s\n", mCommand.getName(), ERR_VIA_SOAP_ONLY);
            }
        }

        if (mInteractive)
            return;
        
        System.out.println("");
        System.out.println("zmprov [args] [cmd] [cmd-args ...]");
        System.out.println("");
        System.out.println("  -h/--help                      display usage");
        System.out.println("  -f/--file                      use file as input stream");        
        System.out.println("  -s/--server   {host}[:{port}]  server hostname and optional port");
        System.out.println("  -l/--ldap                      provision via LDAP instead of SOAP");
        System.out.println("  -L/--logpropertyfile           log4j property file, valid only with -l");
        System.out.println("  -a/--account  {name}           account name to auth as");
        System.out.println("  -p/--password {pass}           password for account");
        System.out.println("  -P/--passfile {file}           read password from file");
        System.out.println("  -z/--zadmin                    use zimbra admin name/password from localconfig for admin/password");
        System.out.println("  -v/--verbose                   verbose mode (dumps full exception stack trace)");
        System.out.println("  -d/--debug                     debug mode (dumps SOAP messages)");
        System.out.println("");
        doHelp(null);
        System.exit(1);
    }

    public static enum Category {
        ACCOUNT("help on account-related commands"),
        CALENDAR("help on calendar resource-related commands"),
        COMMANDS("help on all commands"),
        CONFIG("help on config-related commands"),
        COS("help on COS-related commands"), 
        DOMAIN("help on domain-related commands"), 
        LIST("help on distribution list-related commands"), 
        MISC("help on misc commands"),
        MAILBOX("help on mailbox-related commands"),
        NOTEBOOK("help on notebook-related commands"), 
        SEARCH("help on search-related commands"), 
        SERVER("help on server-related commands"),
        FREEBUSY("help on free/busy-related commands");
        
        String mDesc;

        public String getDescription() { return mDesc; }
        
        Category(String desc) {
            mDesc = desc;
        }
    }
    
    public enum Command {
        
        ADD_ACCOUNT_ALIAS("addAccountAlias", "aaa", "{name@domain|id} {alias@domain}", Category.ACCOUNT, 2, 2),
        ADD_ACCOUNT_LOGGER("addAccountLogger", "aal", "{name@domain|id} {logging-category} {debug|info|warn|error}", Category.MISC, 3, 3),
        ADD_DISTRIBUTION_LIST_ALIAS("addDistributionListAlias", "adla", "{list@domain|id} {alias@domain}", Category.LIST, 2, 2),
        ADD_DISTRIBUTION_LIST_MEMBER("addDistributionListMember", "adlm", "{list@domain|id} {member@domain}+", Category.LIST, 2, Integer.MAX_VALUE),
        AUTO_COMPLETE_GAL("autoCompleteGal", "acg", "{domain} {name}", Category.SEARCH, 2, 2),
        CHECK_PASSWORD_STRENGTH("checkPasswordStrength", "cps", "{name@domain|id} {password}", Category.ACCOUNT, 2, 2),
        COPY_COS("copyCos", "cpc", "{src-cos-name|id} {dest-cos-name}", Category.COS, 2, 2),
        CREATE_ACCOUNT("createAccount", "ca", "{name@domain} {password} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 2, Integer.MAX_VALUE),        
        CREATE_BULK_ACCOUNTS("createBulkAccounts", "cabulk"),  //("  CreateBulkAccounts(cabulk) {domain} {namemask} {number of accounts to create} ");
        CREATE_CALENDAR_RESOURCE("createCalendarResource",  "ccr", "{name@domain} {password} [attr1 value1 [attr2 value2...]]", Category.CALENDAR, 2, Integer.MAX_VALUE),
        CREATE_COS("createCos", "cc", "{name} [attr1 value1 [attr2 value2...]]", Category.COS, 1, Integer.MAX_VALUE),
        CREATE_DATA_SOURCE("createDataSource", "cds", "{name@domain} {ds-type} {ds-name} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 3, Integer.MAX_VALUE),                
        CREATE_DISTRIBUTION_LIST("createDistributionList", "cdl", "{list@domain}", Category.LIST, 1, Integer.MAX_VALUE),
        CREATE_DISTRIBUTION_LISTS_BULK("createDistributionListsBulk", "cdlbulk"),
        CREATE_DOMAIN("createDomain", "cd", "{domain} [attr1 value1 [attr2 value2...]]", Category.DOMAIN, 1, Integer.MAX_VALUE),
        CREATE_SERVER("createServer", "cs", "{name} [attr1 value1 [attr2 value2...]]", Category.SERVER, 1, Integer.MAX_VALUE),
        CREATE_IDENTITY("createIdentity", "cid", "{name@domain} {identity-name} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 2, Integer.MAX_VALUE),        
        CREATE_SIGNATURE("createSignature", "csig", "{name@domain} {signature-name} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 2, Integer.MAX_VALUE),        
        DELETE_ACCOUNT("deleteAccount", "da", "{name@domain|id}", Category.ACCOUNT, 1, 1),
        DELETE_CALENDAR_RESOURCE("deleteCalendarResource",  "dcr", "{name@domain|id}", Category.CALENDAR, 1, 1),
        DELETE_COS("deleteCos", "dc", "{name|id}", Category.COS, 1, 1),
        DELETE_DATA_SOURCE("deleteDataSource", "dds", "{name@domain|id} {ds-name|ds-id}", Category.ACCOUNT, 2, 2),                        
        DELETE_DISTRIBUTION_LIST("deleteDistributionList", "ddl", "{list@domain|id}", Category.LIST, 1, 1),
        DELETE_DOMAIN("deleteDomain", "dd", "{domain|id}", Category.DOMAIN, 1, 1),
        DELETE_IDENTITY("deleteIdentity", "did", "{name@domain|id} {identity-name}", Category.ACCOUNT, 2, 2),
        DELETE_SIGNATURE("deleteSignature", "dsig", "{name@domain|id} {signature-name}", Category.ACCOUNT, 2, 2),
        DELETE_SERVER("deleteServer", "ds", "{name|id}", Category.SERVER, 1, 1),
        EXIT("exit", "quit", "", Category.MISC, 0, 0),
        FLUSH_CACHE("flushCache", "fc", "{skin|locale|account|config|cos|domain|server|zimlet} [name1|id1 [name2|id2...]]", Category.MISC, 1, Integer.MAX_VALUE),
        GENERATE_DOMAIN_PRE_AUTH("generateDomainPreAuth", "gdpa", "{domain|id} {name} {name|id|foreignPrincipal} {timestamp|0} {expires|0}", Category.MISC, 5, 5),
        GENERATE_DOMAIN_PRE_AUTH_KEY("generateDomainPreAuthKey", "gdpak", "[-f] {domain|id}", Category.MISC, 1, 2),
        GET_ACCOUNT("getAccount", "ga", "{name@domain|id} [attr1 [attr2...]]", Category.ACCOUNT, 1, Integer.MAX_VALUE),
        GET_DATA_SOURCES("getDataSources", "gds", "{name@domain|id} [arg1 [arg2...]]", Category.ACCOUNT, 1, Integer.MAX_VALUE),                
        GET_IDENTITIES("getIdentities", "gid", "{name@domain|id} [arg1 [arg...]]", Category.ACCOUNT, 1, Integer.MAX_VALUE),
        GET_SIGNATURES("getSignatures", "gsig", "{name@domain|id} [arg1 [arg...]]", Category.ACCOUNT, 1, Integer.MAX_VALUE),
        GET_ACCOUNT_MEMBERSHIP("getAccountMembership", "gam", "{name@domain|id}", Category.ACCOUNT, 1, 2),
        GET_ALL_ACCOUNTS("getAllAccounts","gaa", "[-v] [{domain}] [-s server]", Category.ACCOUNT, 0, 4),
        GET_ACCOUNT_LOGGERS("getAccountLoggers", "gal", "{name@domain|id}", Category.MISC, 1, 1),
        GET_ALL_ACCOUNT_LOGGERS("getAllAccountLoggers", "gaal", "{server}", Category.MISC, 1, 1),
        GET_ALL_ADMIN_ACCOUNTS("getAllAdminAccounts", "gaaa", "[-v]", Category.ACCOUNT, 0, 1),
        GET_ALL_CALENDAR_RESOURCES("getAllCalendarResources", "gacr", "[-v] [{domain}]", Category.CALENDAR, 0, 2),
        GET_ALL_CONFIG("getAllConfig", "gacf", "[attr1 [attr2...]]", Category.CONFIG, 0, Integer.MAX_VALUE),
        GET_ALL_COS("getAllCos", "gac", "[-v]", Category.COS, 0, 1),
        GET_ALL_DISTRIBUTION_LISTS("getAllDistributionLists", "gadl", "[{domain}]", Category.LIST, 0, 1),
        GET_ALL_DOMAINS("getAllDomains", "gad", "[-v] [attr1 [attr2...]]", Category.DOMAIN, 0, Integer.MAX_VALUE),
        GET_ALL_FREEBUSY_PROVIDERS("getAllFbp", "gafbp", "[-v]", Category.FREEBUSY, 0, 1),
        GET_ALL_SERVERS("getAllServers", "gas", "[-v] [service]", Category.SERVER, 0, 1),
        GET_CALENDAR_RESOURCE("getCalendarResource",     "gcr", "{name@domain|id} [attr1 [attr2...]]", Category.CALENDAR, 1, Integer.MAX_VALUE), 
        GET_CONFIG("getConfig", "gcf", "{name}", Category.CONFIG, 1, 1),
        GET_COS("getCos", "gc", "{name|id} [attr1 [attr2...]]", Category.COS, 1, Integer.MAX_VALUE),
        GET_DISTRIBUTION_LIST("getDistributionList", "gdl", "{list@domain|id} [attr1 [attr2...]]", Category.LIST, 1, Integer.MAX_VALUE),
        GET_DISTRIBUTION_LIST_MEMBERSHIP("getDistributionListMembership", "gdlm", "{name@domain|id}", Category.LIST, 1, 1),
        GET_DOMAIN("getDomain", "gd", "{domain|id} [attr1 [attr2...]]", Category.DOMAIN, 1, Integer.MAX_VALUE),
        GET_DOMAIN_INFO("getDomainInfo", "gdi", "name|id|virtualHostname {value} [attr1 [attr2...]]", Category.DOMAIN, 2, Integer.MAX_VALUE), 
        GET_FREEBUSY_QUEUE_INFO("getFreebusyQueyeInfo", "gfbqi", "[{provider-name}]", Category.FREEBUSY, 0, 1),
        GET_MAILBOX_INFO("getMailboxInfo", "gmi", "{account}", Category.MAILBOX, 1, 1),
        GET_QUOTA_USAGE("getQuotaUsage", "gqu", "{server}", Category.MAILBOX, 1, 1),        
        GET_SERVER("getServer", "gs", "{name|id} [attr1 [attr2...]]", Category.SERVER, 1, Integer.MAX_VALUE), 
        HELP("help", "?", "commands", Category.MISC, 0, 1),
        IMPORT_NOTEBOOK("importNotebook", "impn", "{name@domain} {directory} {folder}", Category.NOTEBOOK),
        INIT_NOTEBOOK("initNotebook", "in", "[{name@domain}]", Category.NOTEBOOK),
        INIT_DOMAIN_NOTEBOOK("initDomainNotebook", "idn", "{domain} [{name@domain}]", Category.NOTEBOOK),
        LDAP(".ldap", ".l"), 
        MODIFY_ACCOUNT("modifyAccount", "ma", "{name@domain|id} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 3, Integer.MAX_VALUE),
        MODIFY_CALENDAR_RESOURCE("modifyCalendarResource",  "mcr", "{name@domain|id} [attr1 value1 [attr2 value2...]]", Category.CALENDAR, 3, Integer.MAX_VALUE),
        MODIFY_CONFIG("modifyConfig", "mcf", "attr1 value1 [attr2 value2...]", Category.CONFIG, 2, Integer.MAX_VALUE),
        MODIFY_COS("modifyCos", "mc", "{name|id} [attr1 value1 [attr2 value2...]]", Category.COS, 3, Integer.MAX_VALUE),
        MODIFY_DATA_SOURCE("modifyDataSource", "mds", "{name@domain|id} {ds-name|ds-id} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 4, Integer.MAX_VALUE),                
        MODIFY_DISTRIBUTION_LIST("modifyDistributionList", "mdl", "{list@domain|id} attr1 value1 [attr2 value2...]", Category.LIST, 3, Integer.MAX_VALUE),
        MODIFY_DOMAIN("modifyDomain", "md", "{domain|id} [attr1 value1 [attr2 value2...]]", Category.DOMAIN, 3, Integer.MAX_VALUE),
        MODIFY_IDENTITY("modifyIdentity", "mid", "{name@domain|id} {identity-name} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 4, Integer.MAX_VALUE),
        MODIFY_SIGNATURE("modifySignature", "msig", "{name@domain|id} {signature-name|signature-id} [attr1 value1 [attr2 value2...]]", Category.ACCOUNT, 4, Integer.MAX_VALUE),
        MODIFY_SERVER("modifyServer", "ms", "{name|id} [attr1 value1 [attr2 value2...]]", Category.SERVER, 3, Integer.MAX_VALUE),
        PUSH_FREEBUSY("pushFreebusy", "pfb", "{domain|account-id} [account-id ...]", Category.FREEBUSY, 1, Integer.MAX_VALUE),
        REMOVE_ACCOUNT_ALIAS("removeAccountAlias", "raa", "{name@domain|id} {alias@domain}", Category.ACCOUNT, 2, 2),
        REMOVE_ACCOUNT_LOGGER("removeAccountLogger", "ral", "{name@domain|id} {logging-category}", Category.MISC, 2, 2),
        REMOVE_DISTRIBUTION_LIST_ALIAS("removeDistributionListAlias", "rdla", "{list@domain|id} {alias@domain}", Category.LIST, 2, 2),
        REMOVE_DISTRIBUTION_LIST_MEMBER("removeDistributionListMember", "rdlm", "{list@domain|id} {member@domain}", Category.LIST, 2, Integer.MAX_VALUE),
        RENAME_ACCOUNT("renameAccount", "ra", "{name@domain|id} {newName@domain}", Category.ACCOUNT, 2, 2),
        RENAME_CALENDAR_RESOURCE("renameCalendarResource",  "rcr", "{name@domain|id} {newName@domain}", Category.CALENDAR, 2, 2),
        RENAME_COS("renameCos", "rc", "{name|id} {newName}", Category.COS, 2, 2),
        RENAME_DISTRIBUTION_LIST("renameDistributionList", "rdl", "{list@domain|id} {newName@domain}", Category.LIST, 2, 2),
        RENAME_DOMAIN("renameDomain", "rd", "{domain|id} {newDomain}", Category.DOMAIN, 2, 2, Via.ldap),
        REINDEX_MAILBOX("reIndexMailbox", "rim", "{name@domain|id} {action} [{reindex-by} {value1} [value2...]]", Category.MAILBOX, 2, Integer.MAX_VALUE),
        SEARCH_ACCOUNTS("searchAccounts", "sa", "[-v] {ldap-query} [limit {limit}] [offset {offset}] [sortBy {attr}] [attrs {a1,a2...}] [sortAscending 0|1*] [domain {domain}]", Category.SEARCH, 1, Integer.MAX_VALUE),
        SEARCH_CALENDAR_RESOURCES("searchCalendarResources", "scr", "[-v] domain attr op value [attr op value...]", Category.SEARCH),
        SEARCH_GAL("searchGal", "sg", "{domain} {name}", Category.SEARCH, 2, 2),
        SELECT_MAILBOX("selectMailbox", "sm", "{account-name} [{zmmailbox commands}]", Category.MAILBOX, 1, Integer.MAX_VALUE),        
        SET_ACCOUNT_COS("setAccountCos", "sac", "{name@domain|id} {cos-name|cos-id}", Category.ACCOUNT, 2, 2),
        SET_PASSWORD("setPassword", "sp", "{name@domain|id} {password}", Category.ACCOUNT, 2, 2),
        GET_ALL_MTA_AUTH_URLS("getAllMtaAuthURLs", "gamau", "", Category.SERVER, 0, 0),
        GET_ALL_REVERSE_PROXY_URLS("getAllReverseProxyURLs", "garpu", "", Category.SERVER, 0, 0),
        GET_ALL_REVERSE_PROXY_BACKENDS("getAllReverseProxyBackends", "garpb", "", Category.SERVER, 0, 0),
        GET_ALL_MEMCACHED_SERVERS("getAllMemcachedServers", "gamcs", "", Category.SERVER, 0, 0),
        SOAP(".soap", ".s"),
        SYNC_GAL("syncGal", "syg","{domain} [{token}]", Category.MISC, 1, 2);

        private String mName;
        private String mAlias;
        private String mHelp;
        private Category mCat;
        private int mMinArgLength = 0;
        private int mMaxArgLength = Integer.MAX_VALUE;
        private Via mVia;
        
        public static enum Via {
            soap, ldap;
        }

        public String getName() { return mName; }
        public String getAlias() { return mAlias; }
        public String getHelp() { return mHelp; }
        public Category getCategory() { return mCat; }
        public boolean hasHelp() { return mHelp != null; }
        public boolean checkArgsLength(String args[]) {
            int len = args == null ? 0 : args.length - 1;
            return len >= mMinArgLength && len <= mMaxArgLength;
        }
        public Via getVia() { return mVia; }

        private Command(String name, String alias) {
            mName = name;
            mAlias = alias;
        }

        private Command(String name, String alias, String help, Category cat)  {
            mName = name;
            mAlias = alias;
            mHelp = help;
            mCat = cat;
        }

        private Command(String name, String alias, String help, Category cat, int minArgLength, int maxArgLength)  {
            mName = name;
            mAlias = alias;
            mHelp = help;
            mCat = cat;
            mMinArgLength = minArgLength;
            mMaxArgLength = maxArgLength;            
        }
        
        private Command(String name, String alias, String help, Category cat, int minArgLength, int maxArgLength, Via via)  {
            mName = name;
            mAlias = alias;
            mHelp = help;
            mCat = cat;
            mMinArgLength = minArgLength;
            mMaxArgLength = maxArgLength;     
            mVia = via;
        }
        
    }
    
    private void addCommand(Command command) {
        String name = command.getName().toLowerCase();
        if (mCommandIndex.get(name) != null)
            throw new RuntimeException("duplicate command: "+name);
        
        String alias = command.getAlias().toLowerCase();
        if (mCommandIndex.get(alias) != null)
            throw new RuntimeException("duplicate command: "+alias);
        
        mCommandIndex.put(name, command);
        mCommandIndex.put(alias, command);
    }
    
    private void initCommands() {
        mCommandIndex = new HashMap<String, Command>();

        for (Command c : Command.values())
            addCommand(c);
    }
    
    private Command lookupCommand(String command) {
        return mCommandIndex.get(command.toLowerCase());
    }
    
    private boolean needsAuth(Command command) {
        if (command == Command.HELP)
            return false;
        else
            return true;
    }

    private ProvUtil() {
        initCommands();
    }
    
    public void initProvisioning() throws ServiceException, IOException {
        if (mUseLdap)
            mProv = Provisioning.getInstance();
        else {
            SoapProvisioning sp = new SoapProvisioning();            
            sp.soapSetURI(LC.zimbra_admin_service_scheme.value()+mServer+":"+mPort+ZimbraServlet.ADMIN_SERVICE_URI);
            if (mDebug) sp.soapSetTransportDebugListener(this);
            if (mAccount != null && mPassword != null)
                sp.soapAdminAuthenticate(mAccount, mPassword);
            else
                sp.soapZimbraAdminAuthenticate();
            mProv = sp;            
        }
    }
    
    private Command.Via violateVia(Command cmd) {
        Command.Via via = cmd.getVia();
        if (via == null)
            return null;
        
        if (via == Command.Via.ldap && !(mProv instanceof LdapProvisioning))
            return Command.Via.ldap;

        if (via == Command.Via.soap && !(mProv instanceof SoapProvisioning))
            return Command.Via.soap;
        
        return null;
    }
    
    private boolean execute(String args[]) throws ServiceException, ArgException, IOException {
        String [] members;
        Account account;
        
        mCommand = lookupCommand(args[0]);
        
        if (mCommand == null)
            return false;
        
        Command.Via violatedVia = violateVia(mCommand);
        if (violatedVia != null) {
            usage(violatedVia);
            return true;
        }
        
        if (!mCommand.checkArgsLength(args)) {
            usage();
            return true;
        }
        
        switch(mCommand) {
        case ADD_ACCOUNT_ALIAS:
            mProv.addAlias(lookupAccount(args[1]), args[2]);
            break;
        case ADD_ACCOUNT_LOGGER:
            doAddAccountLogger(args);
            break;
        case AUTO_COMPLETE_GAL:
            doAutoCompleteGal(args); 
            break; 
        case COPY_COS:
            System.out.println(mProv.copyCos(lookupCos(args[1]).getId(), args[2]).getId());
            break;  
        case CREATE_ACCOUNT:
            System.out.println(mProv.createAccount(args[1], args[2].equals("")? null : args[2], getMap(args, 3)).getId());
            break;                        
        case CREATE_COS:
            System.out.println(mProv.createCos(args[1], getMap(args, 2)).getId());
            break;        
        case CREATE_DOMAIN:
            System.out.println(mProv.createDomain(args[1], getMap(args, 2)).getId());
            break;
        case CREATE_IDENTITY:
            mProv.createIdentity(lookupAccount(args[1]), args[2], getMap(args, 3));
            break;
        case CREATE_SIGNATURE:
            System.out.println(mProv.createSignature(lookupAccount(args[1]), args[2], getMap(args, 3)).getId());
            break;      
        case CREATE_DATA_SOURCE:
            System.out.println(mProv.createDataSource(lookupAccount(args[1]), DataSource.Type.fromString(args[2]), args[3], getMap(args, 4)).getId());
            break;                                                
        case CREATE_SERVER:
            System.out.println(mProv.createServer(args[1], getMap(args, 2)).getId());
            break;            
        case EXIT:
            System.exit(0);
            break;
        case FLUSH_CACHE:
            doFlushCache(args);
            break;
        case GENERATE_DOMAIN_PRE_AUTH_KEY:
            doGenerateDomainPreAuthKey(args);
            break;
        case GENERATE_DOMAIN_PRE_AUTH:
            doGenerateDomainPreAuth(args);
            break;            
        case GET_ACCOUNT:
            dumpAccount(lookupAccount(args[1]), getArgNameSet(args, 2));
            break;
        case GET_ACCOUNT_MEMBERSHIP:
            doGetAccountMembership(args);
            break;
        case GET_IDENTITIES:
            doGetAccountIdentities(args);
            break;
        case GET_SIGNATURES:
            doGetAccountSignatures(args);
            break;      
        case GET_DATA_SOURCES:
            doGetAccountDataSources(args);
            break;
        case GET_ACCOUNT_LOGGERS:
            doGetAccountLoggers(args);
            break;
        case GET_ALL_ACCOUNT_LOGGERS:
            doGetAllAccountLoggers(args);
            break;
        case GET_ALL_ACCOUNTS:
            doGetAllAccounts(args); 
            break;            
        case GET_ALL_ADMIN_ACCOUNTS:
            doGetAllAdminAccounts(args);
            break;                        
        case GET_ALL_CONFIG:
            dumpAttrs(mProv.getConfig().getAttrs(), getArgNameSet(args, 1));            
            break;            
        case GET_ALL_COS:
            doGetAllCos(args); 
            break;                        
        case GET_ALL_DOMAINS:
            doGetAllDomains(args); 
            break;                        
        case GET_ALL_FREEBUSY_PROVIDERS:
        {
        	FbCli fbcli = new FbCli();
        	for (FbCli.FbProvider fbprov : fbcli.getAllFreeBusyProviders())
        		System.out.println(fbprov.toString());
        	break;
        }
        case GET_ALL_SERVERS:
            doGetAllServers(args); 
            break;
        case GET_CONFIG:
            doGetConfig(args); 
            break;                        
        case GET_COS:
            dumpCos(lookupCos(args[1]), getArgNameSet(args, 2));
            break;
        case GET_DISTRIBUTION_LIST_MEMBERSHIP:
            doGetDistributionListMembership(args);
            break;            
        case GET_DOMAIN:
            dumpDomain(lookupDomain(args[1]), getArgNameSet(args, 2));
            break;
        case GET_DOMAIN_INFO:
            doGetDomainInfo(args);
            break;
        case GET_FREEBUSY_QUEUE_INFO:
        {
        	FbCli fbcli = new FbCli();
        	String name = null;
        	if (args.length > 1)
        		name = args[1];
        	for (FbCli.FbQueue fbqueue : fbcli.getFreeBusyQueueInfo(name))
        		System.out.println(fbqueue.toString());
        	break;
        }
        case GET_SERVER:
            dumpServer(lookupServer(args[1]), getArgNameSet(args, 2));
            break;
        case HELP:
            doHelp(args); 
            break;            
        case MODIFY_ACCOUNT:
            mProv.modifyAttrs(lookupAccount(args[1]), getMap(args,2), true);
            break;            
        case MODIFY_DATA_SOURCE:
            account = lookupAccount(args[1]);
            mProv.modifyDataSource(account, lookupDataSourceId(account, args[2]), getMap(args,3));
            break;
        case MODIFY_IDENTITY:
            mProv.modifyIdentity(lookupAccount(args[1]), args[2], getMap(args,3));
            break; 
        case MODIFY_SIGNATURE:
            account = lookupAccount(args[1]);
            mProv.modifySignature(account, lookupSignatureId(account, args[2]), getMap(args,3));
            break;    
        case MODIFY_COS:
            mProv.modifyAttrs(lookupCos(args[1]), getMap(args, 2), true);            
            break;            
        case MODIFY_CONFIG:
            mProv.modifyAttrs(mProv.getConfig(), getMap(args, 1), true);            
            break;                        
        case MODIFY_DOMAIN:
            mProv.modifyAttrs(lookupDomain(args[1]), getMap(args, 2), true);            
            break; 
        case MODIFY_SERVER:
            mProv.modifyAttrs(lookupServer(args[1]), getMap(args, 2), true);            
            break;            
        case DELETE_ACCOUNT:
            doDeleteAccount(args);
            break;
        case DELETE_COS:
            mProv.deleteCos(lookupCos(args[1]).getId());
            break;            
        case DELETE_DOMAIN:
            mProv.deleteDomain(lookupDomain(args[1]).getId());            
            break;
        case DELETE_IDENTITY:
            mProv.deleteIdentity(lookupAccount(args[1]), args[2]);
            break;
        case DELETE_SIGNATURE:
            account = lookupAccount(args[1]);
            mProv.deleteSignature(account, lookupSignatureId(account, args[2]));
            break;     
        case DELETE_DATA_SOURCE:
            account = lookupAccount(args[1]);
            mProv.deleteDataSource(account, lookupDataSourceId(account, args[2]));
            break;                        
        case DELETE_SERVER:
            mProv.deleteServer(lookupServer(args[1]).getId());
            break;
        case PUSH_FREEBUSY:
        {
        	FbCli fbcli = new FbCli();
        	if (args.length == 2) {
        		try {
        			lookupDomain(args[1]);
        			fbcli.pushFreeBusyForDomain(args[1]);
        			return true;
        		} catch (ServiceException se) {
        		}
        	}
			HashSet<String> accounts = new HashSet<String>();
			java.util.Collections.addAll(accounts, args);
			accounts.remove(args[0]);
			fbcli.pushFreeBusyForAccounts(accounts);
        	break;
        }
        case REMOVE_ACCOUNT_ALIAS:
            mProv.removeAlias(lookupAccount(args[1], false), args[2]);
            break;
        case REMOVE_ACCOUNT_LOGGER:
            doRemoveAccountLogger(args);
            break;
        case RENAME_ACCOUNT:
            mProv.renameAccount(lookupAccount(args[1]).getId(), args[2]);            
            break;                        
        case RENAME_COS:
            mProv.renameCos(lookupCos(args[1]).getId(), args[2]);            
            break; 
        case RENAME_DOMAIN:
            doRenameDomain(args);            
            break;
        case SET_ACCOUNT_COS:
            mProv.setCOS(lookupAccount(args[1]),lookupCos(args[2])); 
            break;                        
        case SEARCH_ACCOUNTS:
            doSearchAccounts(args); 
            break;                                    
        case SEARCH_GAL:
            doSearchGal(args); 
            break;
        case SYNC_GAL:
            doSyncGal(args);
            break;
        case SET_PASSWORD:
            mProv.setPassword(lookupAccount(args[1]), args[2]);
            break;
        case CHECK_PASSWORD_STRENGTH:
            mProv.checkPasswordStrength(lookupAccount(args[1]), args[2]);
            System.out.println("Password passed strength check.");
            break;
        case CREATE_DISTRIBUTION_LIST:
            System.out.println(mProv.createDistributionList(args[1], getMap(args, 2)).getId());
            break;
        case CREATE_DISTRIBUTION_LISTS_BULK:
            doCreateDistributionListsBulk(args);            
            break;            
        case GET_ALL_DISTRIBUTION_LISTS:
            doGetAllDistributionLists(args);
            break;
        case GET_DISTRIBUTION_LIST:
            dumpDistributionList(lookupDistributionList(args[1]), getArgNameSet(args, 2));
            break;
        case MODIFY_DISTRIBUTION_LIST:
            mProv.modifyAttrs(lookupDistributionList(args[1]), getMap(args, 2), true);
            break;
        case DELETE_DISTRIBUTION_LIST:
            mProv.deleteDistributionList(lookupDistributionList(args[1]).getId());
            break;
        case ADD_DISTRIBUTION_LIST_MEMBER:
            members = new String[args.length - 2];
            System.arraycopy(args, 2, members, 0, args.length - 2);
            mProv.addMembers(lookupDistributionList(args[1]), members);
            break;
        case REMOVE_DISTRIBUTION_LIST_MEMBER:
            members = new String[args.length - 2];
            System.arraycopy(args, 2, members, 0, args.length - 2);
            mProv.removeMembers(lookupDistributionList(args[1]), members);
            break;
        case CREATE_BULK_ACCOUNTS:
            doCreateAccountsBulk(args);
            break;
        case ADD_DISTRIBUTION_LIST_ALIAS:
            mProv.addAlias(lookupDistributionList(args[1]), args[2]);
            break;
        case REMOVE_DISTRIBUTION_LIST_ALIAS:
            mProv.removeAlias(lookupDistributionList(args[1], false), args[2]);
            break;
        case RENAME_DISTRIBUTION_LIST:
            mProv.renameDistributionList(lookupDistributionList(args[1]).getId(), args[2]);
            break;
        case CREATE_CALENDAR_RESOURCE:
            System.out.println(mProv.createCalendarResource(args[1], args[2].equals("")? null : args[2], getMap(args, 3)).getId());
            break;
        case DELETE_CALENDAR_RESOURCE:
            mProv.deleteCalendarResource(lookupCalendarResource(args[1]).getId());
            break;
        case MODIFY_CALENDAR_RESOURCE:
            mProv.modifyAttrs(lookupCalendarResource(args[1]), getMap(args, 2), true);
            break;
        case RENAME_CALENDAR_RESOURCE:
            mProv.renameCalendarResource(lookupCalendarResource(args[1]).getId(), args[2]);
            break;
        case GET_CALENDAR_RESOURCE:
            dumpCalendarResource(lookupCalendarResource(args[1]), getArgNameSet(args, 2));
            break;
        case GET_ALL_CALENDAR_RESOURCES:
            doGetAllCalendarResources(args);
            break;
        case SEARCH_CALENDAR_RESOURCES:
            doSearchCalendarResources(args);
            break;
        case INIT_NOTEBOOK:
            initNotebook(args);
            break;
        case INIT_DOMAIN_NOTEBOOK:
            initDomainNotebook(args);
            break;
        case IMPORT_NOTEBOOK:
            importNotebook(args);
            break;
        case GET_QUOTA_USAGE:
            doGetQuotaUsage(args);
            break;
        case GET_MAILBOX_INFO:
            doGetMailboxInfo(args);
            break;
        case REINDEX_MAILBOX:
            doReIndexMailbox(args);
            break;
        case SELECT_MAILBOX:
            if (!(mProv instanceof SoapProvisioning))
                throwSoapOnly();
             ZMailboxUtil util = new ZMailboxUtil();
             util.setVerbose(mVerbose);
             util.setDebug(mDebug);
             boolean smInteractive = mInteractive && args.length < 3;
             util.setInteractive(smInteractive);
             util.selectMailbox(args[1], (SoapProvisioning) mProv);
             if (smInteractive) {
                 util.interactive(mReader);
             } else if (args.length > 2){
                 String newArgs[] = new String[args.length-2];
                 System.arraycopy(args, 2, newArgs, 0, newArgs.length);
                 util.execute(newArgs);
             } else {
                 throw ZClientException.CLIENT_ERROR("command only valid in interactive mode or with arguments", null);
             }
            break;
        case GET_ALL_MTA_AUTH_URLS:
            doGetAllMtaAuthURLs(args);
            break;
        case GET_ALL_REVERSE_PROXY_URLS:
            doGetAllReverseProxyURLs(args);
            break;
        case GET_ALL_REVERSE_PROXY_BACKENDS:
            doGetAllReverseProxyBackends(args);
            break;
        case GET_ALL_MEMCACHED_SERVERS:
            doGetAllMemcachedServers(args);
            break;
        case SOAP:
            // HACK FOR NOW
            SoapProvisioning sp = new SoapProvisioning();
            sp.soapSetURI("https://localhost:" + mPort + ZimbraServlet.ADMIN_SERVICE_URI);
            sp.soapZimbraAdminAuthenticate();
            mProv = sp;
            break;
        case LDAP:
            // HACK FOR NOW
            mProv = Provisioning.getInstance();
            break;            
        default:
            return false;
        }
        return true;
    }

    private void doGetDomainInfo(String[] args) throws ServiceException {
        if (!(mProv instanceof SoapProvisioning))
            throwSoapOnly();
        SoapProvisioning sp = (SoapProvisioning) mProv;
        DomainBy by = DomainBy.fromString(args[1]);
        dumpDomain(sp.getDomainInfo(by, args[2]), getArgNameSet(args, 3));
    }
    
    private void doRenameDomain(String[] args) throws ServiceException {
        if (!(mProv instanceof LdapProvisioning))
            throwLdapOnly();
        
        LdapProvisioning lp = (LdapProvisioning) mProv;
        Domain domain = lookupDomain(args[1]);
        lp.renameDomain(domain.getId(), args[2]);
        printOutput("domain " + args[1] + " renamed to " + args[2]);
    }

    private void doGetQuotaUsage(String[] args) throws ServiceException {
        if (!(mProv instanceof SoapProvisioning))
            throwSoapOnly();
        SoapProvisioning sp = (SoapProvisioning) mProv;
        List<QuotaUsage> result = sp.getQuotaUsage(args[1]);
        for (QuotaUsage u : result) {
            System.out.printf("%s %d %d\n", u.getName(), u.getLimit(), u.getUsed());
        }
    }

    private void doGetMailboxInfo(String[] args) throws ServiceException {
        if (!(mProv instanceof SoapProvisioning))
            throwSoapOnly();
        SoapProvisioning sp = (SoapProvisioning) mProv;
        Account acct = lookupAccount(args[1]);
        MailboxInfo info = sp.getMailbox(acct);
        System.out.printf("mailboxId: %s\nquotaUsed: %d\n", info.getMailboxId(), info.getUsed());
    }
    
    private void doReIndexMailbox(String[] args) throws ServiceException {
        if (!(mProv instanceof SoapProvisioning))
            throwSoapOnly();
        SoapProvisioning sp = (SoapProvisioning) mProv;
        Account acct = lookupAccount(args[1]);
        ReIndexBy by = null;
        String[] values = null;
        if (args.length > 3) {
            try {
                by = ReIndexBy.valueOf(args[3]);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid reindex-by", null);
            }
            if (args.length > 4) {
                values = new String[args.length - 4];
                System.arraycopy(args, 4, values, 0, args.length - 4);
            } else
                throw ServiceException.INVALID_REQUEST("missing reindex-by values", null);
        }
        ReIndexInfo info = sp.reIndex(acct, args[2], by, values);
        ReIndexInfo.Progress progress = info.getProgress();
        System.out.printf("status: %s\n", info.getStatus());
        if (progress != null)
            System.out.printf("progress: numSucceeded=%d, numFailed=%d, numRemaining=%d\n",
                              progress.getNumSucceeded(), progress.getNumFailed(), progress.getNumRemaining());
    }
    
    
    private void doAddAccountLogger(String[] args) throws ServiceException {
        if (!(mProv instanceof SoapProvisioning))
            throwSoapOnly();
        SoapProvisioning sp = (SoapProvisioning) mProv;
        Account acct = lookupAccount(args[1]);
        sp.addAccountLogger(acct, args[2], args[3]);
    }
    
    private void doGetAccountLoggers(String[] args) throws ServiceException {
        if (!(mProv instanceof SoapProvisioning))
            throwSoapOnly();
        SoapProvisioning sp = (SoapProvisioning) mProv;
        Account acct = lookupAccount(args[1]);
        for (AccountLogger accountLogger : sp.getAccountLoggers(acct)) {
            System.out.printf("%s=%s\n", accountLogger.getCategory(), accountLogger.getLevel());
        }
    }
    
    private void doGetAllAccountLoggers(String[] args) throws ServiceException {
        if (!(mProv instanceof SoapProvisioning))
            throwSoapOnly();
        SoapProvisioning sp = (SoapProvisioning) mProv;
        
        Map<String, List<AccountLogger>> allLoggers = sp.getAllAccountLoggers(args[1]);
        for (String accountName : allLoggers.keySet()) {
            System.out.printf("# name %s\n", accountName);
            for (AccountLogger logger : allLoggers.get(accountName)) {
                System.out.printf("%s=%s\n", logger.getCategory(), logger.getLevel());
            }
        }
    }
    
    private void doRemoveAccountLogger(String[] args) throws ServiceException {
        if (!(mProv instanceof SoapProvisioning))
            throwSoapOnly();
        SoapProvisioning sp = (SoapProvisioning) mProv;
        Account acct = lookupAccount(args[1]);
        sp.removeAccountLogger(acct, args[2]);
    }
    
    private void doCreateAccountsBulk(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String domain = args[1];
            String password = "test123";
            String nameMask = args[2];
            int numAccounts = Integer.parseInt(args[3]);            
            for(int ix=0; ix < numAccounts; ix++) {
            	String name = nameMask + Integer.toString(ix) + "@" + domain;
            	Map<String, Object> attrs = new HashMap<String, Object>();
            	String displayName = nameMask + " N. " + Integer.toString(ix);
            	StringUtil.addToMultiMap(attrs, "displayName", displayName);
                Account account = mProv.createAccount(name, password, attrs);
                System.out.println(account.getId());
           }
        }
    }

    private void doGetAccountMembership(String[] args) throws ServiceException {
        String key = null;
        boolean idsOnly = false;
        if (args.length > 2) {
            idsOnly = args[1].equals("-i");
            key = args[2];
        } else {
            key = args[1];
        }
        Account account = lookupAccount(key);
        if (idsOnly) {
            Set<String> lists = mProv.getDistributionLists(account);
            for (String id: lists) {
                System.out.println(id);
            }    
        } else {
            HashMap<String,String> via = new HashMap<String, String>();
            List<DistributionList> lists = mProv.getDistributionLists(account, false, via);
            for (DistributionList dl: lists) {
                String viaDl = via.get(dl.getName());
                if (viaDl != null) System.out.println(dl.getName()+" (via "+viaDl+")");
                else System.out.println(dl.getName());
            }
        }
    }

    private void doDeleteAccount(String[] args) throws ServiceException {
        String key = args[1];
        Account acct = lookupAccount(key);
        if (key.equalsIgnoreCase(acct.getId()) ||
                key.equalsIgnoreCase(acct.getName()) ||
                acct.getName().equalsIgnoreCase(key+"@"+acct.getDomainName())) {
            mProv.deleteAccount(acct.getId());
        } else {
            throw ServiceException.INVALID_REQUEST("argument to deleteAccount must be an account id or the account's primary name", null);
        }

    }
    
    private void doGetAccountIdentities(String[] args) throws ServiceException {
        Account account = lookupAccount(args[1]);
        Set<String> argNameSet = getArgNameSet(args, 2);
        for (Identity identity : mProv.getAllIdentities(account)) {
            dumpIdentity(identity, argNameSet);
        }    
    }
    
    private void doGetAccountSignatures(String[] args) throws ServiceException {
        Account account = lookupAccount(args[1]);
        Set<String> argNameSet = getArgNameSet(args, 2);
        for (Signature signature : mProv.getAllSignatures(account)) {
            dumpSignature(signature, argNameSet);
        }    
    }
    
    private void dumpDataSource(DataSource dataSource, Set<String> argNameSet) throws ServiceException {
        System.out.println("# name "+dataSource.getName());
        System.out.println("# type "+dataSource.getType());
        Map<String, Object> attrs = dataSource.getAttrs();
        dumpAttrs(attrs, argNameSet);
        System.out.println();
    }
    
    private void doGetAccountDataSources(String[] args) throws ServiceException {
        Account account = lookupAccount(args[1]);
        Set<String> attrNameSet = getArgNameSet(args, 2);
        for (DataSource dataSource : mProv.getAllDataSources(account)) {
            dumpDataSource(dataSource, attrNameSet);
        }    
    }
    
    private void doGetDistributionListMembership(String[] args) throws ServiceException {
        String key = args[1];
        DistributionList dist = lookupDistributionList(key);
        HashMap<String,String> via = new HashMap<String, String>();
        List<DistributionList> lists = mProv.getDistributionLists(dist, false, via);
        for (DistributionList dl: lists) {
            String viaDl = via.get(dl.getName());
            if (viaDl != null) System.out.println(dl.getName()+" (via "+viaDl+")");
            else System.out.println(dl.getName());
        }
    }

    private void doGetConfig(String[] args) throws ServiceException {
        String key = args[1];
        String value[] = mProv.getConfig().getMultiAttr(key);
        if (value != null && value.length != 0) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(key, value);
            dumpAttrs(map, null);
        }
    }

    private void doGetAllAccounts(Provisioning prov, Domain domain, Server server, final boolean verbose, final Set<String> attrNames) throws ServiceException {
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            public void visit(com.zimbra.cs.account.NamedEntry entry) throws ServiceException {
                if (verbose)
                    dumpAccount((Account) entry, attrNames);
                else 
                    System.out.println(entry.getName());                        
            }
        };
         prov.getAllAccounts(domain, server, visitor);
    }
    
    private void doGetAllAccounts(String[] args) throws ServiceException {
        boolean verbose = false;
        String d = null;
        String s = null;
        if (args.length == 2) {
            if (args[1].equals("-v")) 
                verbose = true;
            else 
                d = args[1];

        } else if (args.length == 3) {
            if (args[1].equals("-v")) {
                verbose = true;
                d = args[2];
            } else if (args[1].equals("-s")) {
                s = args[2];
            } else {
                usage();
                return;
            }
        } else if (args.length == 4) {
            if (args[1].equals("-v")) 
                verbose = true;
            else 
                d = args[1];
            
            if (args[2].equals("-s"))
                s = args[3];
            else {
                usage();
                return;
            }
        } else if (args.length == 5) {
            if (args[1].equals("-v")) {
                verbose = true;
                d = args[2];
            } else {
                usage();
                return;
            }
            
            if (args[3].equals("-s")) {
                s = args[4];
            } else {
                usage();
                return;
            }
        } else if (args.length != 1) {
            usage();
            return;
        }

        // always use LDAP
        Provisioning prov = Provisioning.getInstance();

        Server server = null;
        if (s != null)
            server = lookupServer(s);
                
        if (d == null) {
            List domains = prov.getAllDomains();
            for (Iterator dit=domains.iterator(); dit.hasNext(); ) {
                Domain domain = (Domain) dit.next();
                doGetAllAccounts(prov, domain, server, verbose, null);
            }
        } else {
            Domain domain = lookupDomain(d, prov);
            doGetAllAccounts(prov, domain, server, verbose, null);
        }
    }    

    private void doSearchAccounts(String[] args) throws ServiceException, ArgException {
        boolean verbose = false;
        int i = 1;
        
        if (args[i].equals("-v")) {
            verbose = true;
            i++;
            if (args.length < i-1) {
                usage();
                return;
                
            }
        }
        
        if (args.length < i+1) {
            usage();
            return;
        }
            
        String query = args[i];
        query = LdapEntrySearchFilter.toLdapIDNFilter(query);


        Map attrs = getMap(args, i+1);
//        int iPageNum = 0;
//        int iPerPage = 0;        

        String limitStr = (String) attrs.get("limit");
        int limit = limitStr == null ? Integer.MAX_VALUE : Integer.parseInt(limitStr);
        
        String offsetStr = (String) attrs.get("offset");
        int offset = offsetStr == null ? 0 : Integer.parseInt(offsetStr);        
        
		String sortBy  = (String)attrs.get("sortBy");
		String sortAscending  = (String) attrs.get("sortAscending");
		boolean isSortAscending = (sortAscending != null) ? "1".equalsIgnoreCase(sortAscending) : true;    

        String attrsStr = (String)attrs.get("attrs");
		String[] attrsToGet = attrsStr == null ? null : attrsStr.split(",");

        String typesStr = (String) attrs.get("types");
        int flags = Provisioning.SA_ACCOUNT_FLAG|Provisioning.SA_ALIAS_FLAG|Provisioning.SA_DISTRIBUTION_LIST_FLAG|Provisioning.SA_CALENDAR_RESOURCE_FLAG;
        
        if (typesStr != null)
            flags = Provisioning.searchAccountStringToMask(typesStr);

        String domainStr = (String)attrs.get("domain");
        List accounts;
        Provisioning prov = Provisioning.getInstance();
        if (domainStr != null) {
            Domain d = lookupDomain(domainStr, prov);
            accounts = prov.searchAccounts(d, query, attrsToGet, sortBy, isSortAscending, flags);
        } else {
            //accounts = mProvisioning.searchAccounts(query, attrsToGet, sortBy, isSortAscending, Provisioning.SA_ACCOUNT_FLAG);
            accounts = prov.searchAccounts(query, attrsToGet, sortBy, isSortAscending, flags);
        }

        //ArrayList accounts = (ArrayList) mProvisioning.searchAccounts(query);
        for (int j=offset; j < offset+limit && j < accounts.size(); j++) {
            NamedEntry account = (NamedEntry) accounts.get(j);
            if (verbose) {
                if (account instanceof Account)
                    dumpAccount((Account)account, true, null);
                else if (account instanceof Alias)
                    dumpAlias((Alias)account);
                else if (account instanceof DistributionList)
                    dumpDistributionList((DistributionList)account, null);
                else if (account instanceof Domain)
                    dumpDomain((Domain)account, null);
            } else {
                System.out.println(account.getName());
            }
        }
    }    

    private void doSearchGal(String[] args) throws ServiceException {
        boolean verbose = false;
        int i = 1;
        
        if (args.length < i+1) {
            usage();
            return;
        }

        if (args[i].equals("-v")) {
            verbose = true;
            i++;
            if (args.length < i-1) {
                usage();
                return;
            }
        }
        
        if (args.length < i+2) {
            usage();
            return;
        }

        String domain = args[i];
        String query = args[i+1];
        
        Domain d = lookupDomain(domain);

        SearchGalResult result = mProv.searchGal(d, query, Provisioning.GAL_SEARCH_TYPE.ALL, null);
        for (GalContact contact : result.matches)
            dumpContact(contact);
    }    

    private void doAutoCompleteGal(String[] args) throws ServiceException {

        String domain = args[1];
        String query = args[2];
        
        Domain d = lookupDomain(domain);

        SearchGalResult result = mProv.autoCompleteGal(d, query, Provisioning.GAL_SEARCH_TYPE.ALL, 100);
        for (GalContact contact : result.matches)
            dumpContact(contact);
    }    
    
    private void doSyncGal(String[] args) throws ServiceException {
        String domain = args[1];
        String token = args.length  == 3 ? args[2] : "";
        
        Domain d = lookupDomain(domain);

        SearchGalResult result = mProv.searchGal(d, "", Provisioning.GAL_SEARCH_TYPE.ALL, token);
        if (result.token != null)
            System.out.println("# token = "+result.token);
        for (GalContact contact : result.matches)
            dumpContact(contact);
    }    

    private void doGetAllAdminAccounts(String[] args) throws ServiceException {
        boolean verbose = args.length > 1 && args[1].equals("-v");
        List accounts = mProv.getAllAdminAccounts();
        Set<String> attrNames = getArgNameSet(args, verbose ? 2 : 1);
        for (Iterator it=accounts.iterator(); it.hasNext(); ) {
            Account account = (Account) it.next();
            if (verbose)
                dumpAccount(account, attrNames);
            else 
                System.out.println(account.getName());
        }
    }    
    
    private void doGetAllCos(String[] args) throws ServiceException {
        boolean verbose = args.length > 1 && args[1].equals("-v");
        Set<String> attrNames = getArgNameSet(args, verbose ? 2 : 1);
        List allcos = mProv.getAllCos();
        for (Iterator it=allcos.iterator(); it.hasNext(); ) {
            Cos cos = (Cos) it.next();
            if (verbose)
                dumpCos(cos, attrNames);
            else 
                System.out.println(cos.getName());
        }
    }        

    private void dumpCos(Cos cos, Set<String> attrNames) throws ServiceException {
        System.out.println("# name "+cos.getName());
        Map<String, Object> attrs = cos.getAttrs();
        dumpAttrs(attrs, attrNames);
        System.out.println();
    }

    private void doGetAllDomains(String[] args) throws ServiceException {
        boolean verbose = args.length > 1 && args[1].equals("-v");
        Set<String> attrNames = getArgNameSet(args, verbose ? 2 : 1);
        List domains = mProv.getAllDomains();
        for (Iterator it=domains.iterator(); it.hasNext(); ) {
            Domain domain = (Domain) it.next();
            if (verbose)
                dumpDomain(domain, attrNames);
            else
                System.out.println(domain.getName());
        }
    }        

    private void dumpDomain(Domain domain, Set<String> attrNames) throws ServiceException {
        System.out.println("# name "+domain.getName());
        Map<String, Object> attrs = domain.getAttrs();
        dumpAttrs(attrs, attrNames);
        System.out.println();
    }

    private void dumpDistributionList(DistributionList dl, Set<String> attrNames) throws ServiceException {
        String[] members = dl.getAllMembers();
        int count = members == null ? 0 : members.length; 
        System.out.println("# distributionList " + dl.getName() + " memberCount=" + count);
        Map<String, Object> attrs = dl.getAttrs();
        dumpAttrs(attrs, attrNames);
    }

    private void dumpAlias(Alias alias) throws ServiceException {
        System.out.println("# alias " + alias.getName());
        Map<String, Object> attrs = alias.getAttrs();
        dumpAttrs(attrs, null);        
    }

    private void doGetAllServers(String[] args) throws ServiceException {
        boolean verbose = false;
        String service = null;
        if (args.length > 1) {
            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("-v")) {
                    verbose = true;
                } else {
                    if (service != null) {
                        throw ServiceException.INVALID_REQUEST("more than one service specified in get all servers", null); 
                    }
                    service = args[i];
                }
            }
        }
        
        List servers = mProv.getAllServers(service);
        for (Iterator it=servers.iterator(); it.hasNext(); ) {
            Server server = (Server) it.next();
            if (verbose)
                dumpServer(server, null);
            else 
                System.out.println(server.getName());
        }
    }        

    private void dumpServer(Server server, Set<String> attrNames) throws ServiceException {
        System.out.println("# name "+server.getName());
        Map<String, Object> attrs = server.getAttrs(true);
        dumpAttrs(attrs, attrNames);
        System.out.println();
    }

    void dumpAccount(Account account, Set<String> attrNames) throws ServiceException {
        dumpAccount(account, true, attrNames);
    }

    private void dumpAccount(Account account, boolean expandCos, Set<String> attrNames) throws ServiceException {
        System.out.println("# name "+account.getName());
        Map<String, Object> attrs = account.getAttrs(expandCos);
        dumpAttrs(attrs, attrNames);
        System.out.println();
    }
    
    void dumpCalendarResource(CalendarResource  resource, Set<String> attrNames) throws ServiceException {
        dumpCalendarResource(resource, true, attrNames);
    }

    private void dumpCalendarResource(CalendarResource resource, boolean expandCos, Set<String> attrNames) throws ServiceException {
        System.out.println("# name "+resource.getName());
        Map<String, Object> attrs = resource.getAttrs(expandCos);
        dumpAttrs(attrs, attrNames);
        System.out.println();
    }
    
    private void dumpContact(GalContact contact) throws ServiceException {
        System.out.println("# name "+contact.getId());
        Map<String, Object> attrs = contact.getAttrs();
        dumpAttrs(attrs, null);
        System.out.println();
    }
 
    private void dumpIdentity(Identity identity, Set<String> attrNameSet) throws ServiceException {
        System.out.println("# name "+identity.getName());
        Map<String, Object> attrs = identity.getAttrs();
        dumpAttrs(attrs, attrNameSet);
        System.out.println();
    }
    
    private void dumpSignature(Signature signature, Set<String> attrNameSet) throws ServiceException {
        System.out.println("# name "+signature.getName());
        Map<String, Object> attrs = signature.getAttrs();
        dumpAttrs(attrs, attrNameSet);
        System.out.println();
    }

    private void dumpAttrs(Map<String, Object> attrsIn, Set<String> specificAttrs) {
        TreeMap<String, Object> attrs = new TreeMap<String, Object>(attrsIn);

        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String name = entry.getKey();
            if (specificAttrs == null || specificAttrs.contains(name.toLowerCase())) {
                Object value = entry.getValue();
                if (value instanceof String[]) {
                    String sv[] = (String[]) value;
                    for (String aSv : sv) {
                        printOutput(name + ": " + aSv);
                    }
                } else if (value instanceof String){
                    printOutput(name+": "+value);
                }
            }
        }
    }

    private void doCreateDistributionListsBulk(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String domain = args[1];
            String nameMask = args[2];
            int numAccounts = Integer.parseInt(args[3]);            
            for(int ix=0; ix < numAccounts; ix++) {
            	String name = nameMask + Integer.toString(ix) + "@" + domain;
            	Map<String, Object> attrs = new HashMap<String, Object>();
            	String displayName = nameMask + " N. " + Integer.toString(ix);
            	StringUtil.addToMultiMap(attrs, "displayName", displayName);
            	DistributionList dl  = mProv.createDistributionList(name, attrs);
            	System.out.println(dl.getId());
           }
        }
    }
    
    private void doGetAllDistributionLists(String[] args) throws ServiceException {
        String d = args.length == 2 ? args[1] : null;

        if (d == null) {
            List domains = mProv.getAllDomains();
            for (Iterator dit=domains.iterator(); dit.hasNext(); ) {
                Domain domain = (Domain) dit.next();
                Collection dls = mProv.getAllDistributionLists(domain);
                for (Iterator it = dls.iterator(); it.hasNext();) {
                    DistributionList dl = (DistributionList)it.next();
                    System.out.println(dl.getName());
                }
            }
        } else {
            Domain domain = lookupDomain(d);
            Collection dls = mProv.getAllDistributionLists(domain);
            for (Iterator it = dls.iterator(); it.hasNext();) {
                DistributionList dl = (DistributionList) it.next();
                System.out.println(dl.getName());
            }
        }
    }

    private void doGetAllCalendarResources(String[] args)
    throws ServiceException {
        boolean verbose = false;
        String d = null;
        if (args.length == 2) {
            if (args[1].equals("-v")) 
                verbose = true;
            else 
                d = args[1];
        } else if (args.length == 3) {
            if (args[1].equals("-v")) 
                verbose = true;
            else  {
                usage();
                return;
            }
            d = args[2];            
        } else if (args.length != 1) {
            usage();
            return;
        }

        if (d == null) {
            List domains = mProv.getAllDomains();
            for (Iterator dit=domains.iterator(); dit.hasNext(); ) {
                Domain domain = (Domain) dit.next();
                doGetAllCalendarResources(domain, verbose);
            }
        } else {
            Domain domain = lookupDomain(d);
            doGetAllCalendarResources(domain, verbose);
        }
    }    

    private void doGetAllCalendarResources(Domain domain,
                                           final boolean verbose)
    throws ServiceException {
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            public void visit(com.zimbra.cs.account.NamedEntry entry)
            throws ServiceException {
                if (verbose) {
                    dumpCalendarResource((CalendarResource) entry, null);
                } else {
                    System.out.println(entry.getName());                        
                }
            }
        };
        mProv.getAllCalendarResources(domain, visitor);
    }

    private void doSearchCalendarResources(String[] args) throws ServiceException {
        boolean verbose = false;
        int i = 1;

        if (args.length < i + 1) { usage(); return; }
        if (args[i].equals("-v")) { verbose = true; i++; }

        if (args.length < i + 1) { usage(); return; }
        Domain d = lookupDomain(args[i++]);

        if ((args.length - i) % 3 != 0) { usage(); return; }

        EntrySearchFilter.Multi multi =
            new EntrySearchFilter.Multi(false, EntrySearchFilter.AndOr.and);
        for (; i < args.length; ) {
            String attr = args[i++];
            String op = args[i++];
            String value = args[i++];
            try {
                EntrySearchFilter.Single single =
                    new EntrySearchFilter.Single(false, attr, op, value);
                multi.add(single);
            } catch (IllegalArgumentException e) {
                printError("Bad search op in: " + attr + " " + op + " '" + value + "'");
                e.printStackTrace();
                usage();
                return;
            }
        }
        EntrySearchFilter filter = new EntrySearchFilter(multi);

        List resources = mProv.searchCalendarResources(d, filter, null, null, true);
        for (Iterator iter = resources.iterator(); iter.hasNext(); ) {
            CalendarResource resource = (CalendarResource) iter.next();
            if (verbose)
                dumpCalendarResource(resource, null);
            else
                System.out.println(resource.getName());
        }
    }

    private void initNotebook(String[] args) throws ServiceException {
    	if (args.length > 2) {usage(); return; }
    	String username = null;
    	
    	if (args.length == 2)
    		username = args[1];

    	WikiUtil wu = WikiUtil.getInstance(mProv);
    	wu.initDefaultWiki(username);
    }
    private void initDomainNotebook(String[] args) throws ServiceException {
    	if (args.length < 2 || args.length > 3) {usage(); return; }
    	
    	String domain = null;
    	String username = null;

    	domain = args[1];
    	if (args.length == 3)
    		username = args[2];
    	
    	if (mProv.get(AccountBy.name, username) == null)
    		throw AccountServiceException.NO_SUCH_ACCOUNT(username);

    	WikiUtil wu = WikiUtil.getInstance(mProv);
    	wu.initDomainWiki(domain, username);
    }
    private void importNotebook(String[] args) throws ServiceException {
    	if (args.length != 4) {usage(); return; }
    	
    	WikiUtil wu = WikiUtil.getInstance(mProv);
    	doImport(args[1], args[2], args[3], wu);
    }
    
    private void doImport(String username, String fromDir, String toFolder, WikiUtil wu) throws ServiceException {
    	try {
    		wu.startImport(username, toFolder, new java.io.File(fromDir));
    	} catch (IOException e) {
    		throw ServiceException.FAILURE("Cannot import Wiki documents from "+fromDir, e);
    	}
    }
    
    private Account lookupAccount(String key, boolean mustFind) throws ServiceException {
        Account a = mProv.getAccount(key);
        
        if (mustFind && a == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(key);
        else
            return a;
    }
    
    private Account lookupAccount(String key) throws ServiceException {
        return lookupAccount(key, true);
    }

    private CalendarResource lookupCalendarResource(String key) throws ServiceException {
        CalendarResource res = mProv.get(guessCalendarResourceBy(key), key);
        if (res == null)
            throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(key);
        else
            return res;
    }

    private Domain lookupDomain(String key) throws ServiceException {
        return lookupDomain(key, mProv);
    }
    
    private Domain lookupDomain(String key, Provisioning prov) throws ServiceException {
        Domain d = prov.get(guessDomainBy(key), key);
        if (d == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(key);
        else
            return d;
    }
    
    private Cos lookupCos(String key) throws ServiceException {
        Cos c = mProv.get(guessCosBy(key), key);
        if (c == null)
            throw AccountServiceException.NO_SUCH_COS(key);
        else
            return c;
    }

    private Server lookupServer(String key) throws ServiceException {
        Server s = mProv.get(guessServerBy(key), key);
        if (s == null)
            throw AccountServiceException.NO_SUCH_SERVER(key);
        else
            return s;
    }
    
    private String lookupDataSourceId(Account account, String key) throws ServiceException {
        if (Provisioning.isUUID(key)) {
            return key;
        }
        DataSource ds = mProv.get(account, DataSourceBy.name, key);
        if (ds == null)
            throw AccountServiceException.NO_SUCH_DATA_SOURCE(key);
        else
            return ds.getId();
    }
    
    private String lookupSignatureId(Account account, String key) throws ServiceException {
        Signature sig = mProv.get(account, guessSignatureBy(key), key);
        if (sig == null)
            throw AccountServiceException.NO_SUCH_SIGNATURE(key);
        else
            return sig.getId();
    }

    private DistributionList lookupDistributionList(String key, boolean mustFind) throws ServiceException {
        DistributionList dl = mProv.get(guessDistributionListBy(key), key);
        if (mustFind && dl == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(key);
        else
            return dl;
    }
    
    private DistributionList lookupDistributionList(String key) throws ServiceException {
        return lookupDistributionList(key, true);
    }

    private String getAllFreebusyProviders(String[] args) throws ServiceException {
    	return "";
    }
    
    public static AccountBy guessAccountBy(String value) {
        if (Provisioning.isUUID(value))
            return AccountBy.id;
        return AccountBy.name;
    }
  
    
    public static CosBy guessCosBy(String value) {
        if (Provisioning.isUUID(value))
            return CosBy.id;
        return CosBy.name;
    }
    
    public static DomainBy guessDomainBy(String value) {
        if (Provisioning.isUUID(value))
            return DomainBy.id;
        return DomainBy.name;
    }

    public static ServerBy guessServerBy(String value) {
        if (Provisioning.isUUID(value))
            return ServerBy.id;
        return ServerBy.name;
    }

    public static CalendarResourceBy guessCalendarResourceBy(String value) {
        if (Provisioning.isUUID(value))
            return CalendarResourceBy.id;
        return CalendarResourceBy.name;
    }

    public static DistributionListBy guessDistributionListBy(String value) {
        if (Provisioning.isUUID(value))
            return DistributionListBy.id;
        return DistributionListBy.name;
    }
    
    public static SignatureBy guessSignatureBy(String value) {
        if (Provisioning.isUUID(value))
            return SignatureBy.id;
        return SignatureBy.name;
    }
    
    
    private Map<String, Object> getMap(String[] args, int offset) throws ArgException {
        try {
            return StringUtil.keyValueArrayToMultiMap(args, offset);
        } catch (IllegalArgumentException iae) {
            throw new ArgException("not enough arguments");
        }
    }

    private Set<String> getArgNameSet(String[] args, int offset) {
        if (offset >= args.length) return null;

        Set<String> result = new HashSet<String>();
        for (int i=offset; i < args.length; i++)
            result.add(args[i].toLowerCase());

        return result;
    }

    private void interactive(BufferedReader in) throws IOException {
        mReader = in;
        mInteractive = true;
        while (true) {
            System.out.print("prov> ");
            String line = StringUtil.readLine(in);
            if (line == null)
                break;
            if (mVerbose) {
                System.out.println(line);
            }
            String args[] = StringUtil.parseLine(line);
            if (args.length == 0)
                continue;
            try {
                if (!execute(args)) {
                    System.out.println("Unknown command. Type: 'help commands' for a list");
                }
            } catch (ServiceException e) {
                Throwable cause = e.getCause();
                String errText = "ERROR: " + e.getCode() + " (" + e.getMessage() + ")" + 
                        (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")");

                printError(errText);
                
                if (mVerbose) e.printStackTrace(System.err);
            } catch (ArgException e) {
                    usage();
            }
        }
    }

    private static void printError(String text) {
        PrintStream ps = System.err;
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(ps, "UTF-8"));
            writer.write(text+"\n");
            writer.flush();
        } catch (UnsupportedEncodingException e) {
            ps.println(text);
        } catch (IOException e) {
            ps.println(text);
        }
    }
    
    private static void printOutput(String text) {
        PrintStream ps = System.out;
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(ps, "UTF-8"));
            writer.write(text+"\n");
            writer.flush();
        } catch (UnsupportedEncodingException e) {
            ps.println(text);
        } catch (IOException e) {
            ps.println(text);
        }
    }
    
    public static void main(String args[]) throws IOException, ParseException {
        CliUtil.toolSetup();
        
        ProvUtil pu = new ProvUtil();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("h", "help", false, "display usage");
        options.addOption("f", "file", true, "use file as input stream");
        options.addOption("s", "server", true, "host[:port] of server to connect to");
        options.addOption("l", "ldap", false, "provision via LDAP");
        options.addOption("L", "logpropertyfile", true, "log4j property file");
        options.addOption("a", "account", true, "account name (not used with --ldap)");
        options.addOption("p", "password", true, "password for account");
        options.addOption("P", "passfile", true, "filename with password in it");
        options.addOption("z", "zadmin", false, "use zimbra admin name/password from localconfig for account/password");        
        options.addOption("v", "verbose", false, "verbose mode");
        options.addOption("d", "debug", false, "debug mode");
        
        CommandLine cl = null;
        boolean err = false;
        
        try {
            cl = parser.parse(options, args, true);
        } catch (ParseException pe) {
            printError("error: " + pe.getMessage());
            err = true;
        }
        
        if (cl.hasOption('l') && cl.hasOption('s')) {
            printError("error: cannot specify both -l and -s at the same time");
            System.exit(2);
        }
        
        pu.setVerbose(cl.hasOption('v'));
        if (cl.hasOption('l')) pu.setUseLdap(true);
        
        if (err || cl.hasOption('h')) {
            pu.usage();
        }
        
        if (cl.hasOption('L')) {
            if (cl.hasOption('l'))
                ZimbraLog.toolSetupLog4j("INFO", cl.getOptionValue('L'));
            else {
                printError("error: cannot specify -L when -l is not specified");
                System.exit(2);
            }
        }
        
        if (cl.hasOption('z')) {
            pu.setAccount(LC.zimbra_ldap_user.value());
            pu.setPassword(LC.zimbra_ldap_password.value());
        }
        if (cl.hasOption('s')) pu.setServer(cl.getOptionValue('s'));
        if (cl.hasOption('a')) pu.setAccount(cl.getOptionValue('a'));
        if (cl.hasOption('p')) pu.setPassword(cl.getOptionValue('p'));
        if (cl.hasOption('P')) {
            pu.setPassword(StringUtil.readSingleLineFromFile(cl.getOptionValue('P')));
        }
        if (cl.hasOption('d')) pu.setDebug(true);
        
        args = cl.getArgs();
        
        try {
            
            if (args.length < 1) {
                pu.initProvisioning();
                InputStream is = cl.hasOption('f') ? new FileInputStream(cl.getOptionValue('f')) : System.in;
                pu.interactive(new BufferedReader(new InputStreamReader(is, "UTF-8")));
            } else {
                Command cmd = pu.lookupCommand(args[0]);
                if (cmd == null)
                    pu.usage();
                if (pu.needsAuth(cmd))
                    pu.initProvisioning();
                
                try {
                    if (!pu.execute(args))
                        pu.usage();
                } catch (ArgException e) {
                    pu.usage();
                }
            }
        } catch (ServiceException e) {
            Throwable cause = e.getCause();
            String errText = "ERROR: " + e.getCode() + " (" + e.getMessage() + ")" + 
                    (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")");  
            
            printError(errText);
            
            if (pu.mVerbose) e.printStackTrace(System.err);
            System.exit(2);
        }
    }
    
    class ArgException extends Exception {
        ArgException(String msg) {
            super(msg);
        }
    }
    
    private void doFlushCache(String[] args) throws ServiceException {
        CacheEntry[] entries = null;
        
        if (args.length > 2) {
            entries = new CacheEntry[args.length - 2];
            for (int i=2; i<args.length; i++) {
                CacheEntryBy entryBy;
                if (Provisioning.isUUID(args[i]))
                    entryBy = CacheEntryBy.id;
                else
                    entryBy = CacheEntryBy.name;
                
                entries[i-2] = new CacheEntry(entryBy, args[i]);
            }
        }
        
        if (mProv instanceof SoapProvisioning) {
            SoapProvisioning sp = (SoapProvisioning)mProv;
            
            // use this interface to accommodate skin and locale caches
            sp.flushCache(args[1], entries);
        } else {
            if (args[1].equals("skin") || args[1].equals("locale"))
                throw ServiceException.INVALID_REQUEST("cache type "+args[1]+" is only supported via SOAP", null);
            // this interface only allows ldap caches, or should we just disallow this?? 
            mProv.flushCache(CacheEntryType.fromString(args[1]), entries);
        }
    }

    private void doGenerateDomainPreAuthKey(String[] args) throws ServiceException {
        String key = null;
        boolean force = false;
        if (args.length == 3) {
            if (args[1].equals("-f")) 
                force = true;
            else  {
                usage();
                return;
            }
            key = args[2];            
        } else {
            key = args[1];  
        }
        
        Domain domain = lookupDomain(key);
        String curPreAuthKey = domain.getAttr(Provisioning.A_zimbraPreAuthKey);
        if (curPreAuthKey != null && !force)
            throw ServiceException.INVALID_REQUEST("pre auth key exists for domain " + key + ", use command -f option to force overwriting the existing key", null);
        
        String preAuthKey = PreAuthKey.generateRandomPreAuthKey();
        HashMap<String,String> attrs = new HashMap<String,String>();
        attrs.put(Provisioning.A_zimbraPreAuthKey, preAuthKey);
        mProv.modifyAttrs(domain, attrs);
        System.out.printf("preAuthKey: %s\n", preAuthKey);
        if (curPreAuthKey != null)
            System.out.printf("previous preAuthKey: %s\n", curPreAuthKey);
    }

    private void doGenerateDomainPreAuth(String[] args) throws ServiceException {
        String key = args[1];
        Domain domain = lookupDomain(key);
        String preAuthKey = domain.getAttr(Provisioning.A_zimbraPreAuthKey, null);
        if (preAuthKey == null)
            throw ServiceException.INVALID_REQUEST("domain not configured for preauth", null);

        String name = args[2];
        String by = args[3];            
        long timestamp = Long.parseLong(args[4]);
        if (timestamp == 0) timestamp = System.currentTimeMillis();
        long expires = Long.parseLong(args[5]);
        HashMap<String,String> params = new HashMap<String,String>();
        params.put("account", name);
        params.put("by", by);            
        params.put("timestamp", timestamp+"");
        params.put("expires", expires+"");
        System.out.printf("account: %s\nby: %s\ntimestamp: %s\nexpires: %s\npreAuth: %s\n", name, by, timestamp, expires,PreAuthKey.computePreAuth(params, preAuthKey));
    }
        
    private void doGetAllMtaAuthURLs(String[] args) throws ServiceException {
        List<Server> servers = mProv.getAllServers();
        for (Server server : servers ) {
            boolean isTarget = server.getBooleanAttr(Provisioning.A_zimbraMtaAuthTarget, true);
            if (isTarget) {
                System.out.print(URLUtil.getAdminURL(server) + " ");
            }
        }
        System.out.println();
    }
    
    private void doGetAllReverseProxyURLs(String[] args) throws ServiceException {
        // String REVERSE_PROXY_PROTO = "http://";
        String REVERSE_PROXY_PROTO = "";  // don't need proto for nginx.conf
        int REVERSE_PROXY_PORT = 7072;
        // String REVERSE_PROXY_PATH = "/service/extension/nginx-lookup";
        String REVERSE_PROXY_PATH = ExtensionDispatcherServlet.EXTENSION_PATH + "/nginx-lookup";
        
        List<Server> servers = mProv.getAllServers();
        for (Server server : servers ) {
            boolean isTarget = server.getBooleanAttr(Provisioning.A_zimbraReverseProxyLookupTarget, false);
            if (isTarget) {
                String serviceName = server.getAttr(Provisioning.A_zimbraServiceHostname, "");
                System.out.print(REVERSE_PROXY_PROTO + serviceName + ":" + REVERSE_PROXY_PORT + REVERSE_PROXY_PATH + " ");
            }
        }
        System.out.println();
    }

    private void doGetAllReverseProxyBackends(String[] args) throws ServiceException {
	List<Server> servers = mProv.getAllServers();
        for (Server server : servers) {
            boolean isTarget = server.getBooleanAttr(Provisioning.A_zimbraReverseProxyLookupTarget, false);
	    if (!isTarget) {
		return;
	    }

	    // (For now) assume HTTP can be load balanced to...
	    String mode = server.getAttr(Provisioning.A_zimbraMailMode, null);
	    boolean isPlain = mode.equalsIgnoreCase(Provisioning.MAIL_MODE.http.toString()) ||
		mode.equalsIgnoreCase(Provisioning.MAIL_MODE.mixed.toString()) ||
		mode.equalsIgnoreCase(Provisioning.MAIL_MODE.both.toString());
	    if (!isPlain) {
		return;
	    }

	    int backendPort = server.getIntAttr(Provisioning.A_zimbraMailPort, 0);
	    String serviceName = server.getAttr(Provisioning.A_zimbraServiceHostname, "");
	    System.out.println("    server " + serviceName + ":" + backendPort + ";");
        }
    }

    private void doGetAllMemcachedServers(String[] args) throws ServiceException {
        List<Server> servers = mProv.getAllServers(Provisioning.SERVICE_MEMCACHED);
        for (Server server : servers ) {
            System.out.print(server.getAttr(Provisioning.A_zimbraServiceHostname, "") + ":" +
                             server.getAttr(Provisioning.A_zimbraMemcachedBindPort, "") + " ");
        }
        System.out.println();
    }
    
    private void doHelp(String[] args) {
        Category cat = null;
        if (args != null && args.length >= 2) {
            String s = args[1].toUpperCase();
            try {
                cat = Category.valueOf(s);
            } catch (IllegalArgumentException e) {
                for (Category c : Category.values()) {
                    if (c.name().startsWith(s)) {
                        cat = c;
                        break;
                    }
                }             
            }
        }

        if (args == null || args.length == 1 || cat == null) {
            System.out.println(" zmprov is used for provisioning. Try:");
            System.out.println("");
            for (Category c: Category.values()) {
                System.out.printf("     zmprov help %-15s %s\n", c.name().toLowerCase(), c.getDescription());
            }
            
        }
        
        if (cat != null) {
            System.out.println("");            
            for (Command c : Command.values()) {
                if (!c.hasHelp()) continue;
                if (cat == Category.COMMANDS || cat == c.getCategory()) {
                    Command.Via via = c.getVia();
                    /*
                    if (via == null || 
                        (via == Command.Via.ldap && mUseLdap) ||
                        (via == Command.Via.soap && !mUseLdap))
                        System.out.printf("  %s(%s) %s\n\n", c.getName(), c.getAlias(), c.getHelp());
                    */
                    System.out.printf("  %s(%s) %s\n", c.getName(), c.getAlias(), c.getHelp());
                    if (via == Command.Via.ldap)
                        System.out.printf("    -- NOTE: %s can only be used with \"zmprov -l/--ldap\"\n", c.getName());
                    System.out.printf("\n");
                }
            }
        
            if (cat == Category.CALENDAR) {
                System.out.println("");                
                StringBuilder sb = new StringBuilder();
                EntrySearchFilter.Operator vals[] = EntrySearchFilter.Operator.values();
                for (int i = 0; i < vals.length; i++) {
                    if (i > 0)
                        sb.append(", ");
                    sb.append(vals[i].toString());
                }
                System.out.println("    op = " + sb.toString());
            }
        }
        System.out.println();
    }

    private long mSendStart;
    
    public void receiveSoapMessage(Element envelope) {
        long end = System.currentTimeMillis();        
        System.out.printf("======== SOAP RECEIVE =========\n");
        System.out.println(envelope.prettyPrint());
        System.out.printf("=============================== (%d msecs)\n", end-mSendStart);
        
    }

    public void sendSoapMessage(Element envelope) {
        mSendStart = System.currentTimeMillis();
        System.out.println("========== SOAP SEND ==========");
        System.out.println(envelope.prettyPrint());
        System.out.println("===============================");
    }

    
    void throwSoapOnly() throws ServiceException {
        throw ServiceException.INVALID_REQUEST(ERR_VIA_SOAP_ONLY, null);
    }
    
    void throwLdapOnly() throws ServiceException {
        throw ServiceException.INVALID_REQUEST(ERR_VIA_LDAP_ONLY, null);
    }
}

