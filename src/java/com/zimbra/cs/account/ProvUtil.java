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

package com.zimbra.cs.account;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.wiki.WikiUtil;

/**
 * @author schemers
 */
public class ProvUtil {
 
    private boolean mInteractive = false;
    
    private void usage() {
        System.out.println("zmprov [cmd] [args ...]");
        System.out.println("");

        for (Command c : Command.values()) {
            if (!c.hasHelp()) continue;
            System.out.printf("  %s(%s) %s\n", c.getName(), c.getAlias(), c.getHelp());
        }
        
        System.out.println("");        
        
        StringBuilder sb = new StringBuilder();
        EntrySearchFilter.Operator vals[] = EntrySearchFilter.Operator.values();
        for (int i = 0; i < vals.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(vals[i].toString());
        }
        System.out.println("    op = " + sb.toString());
        System.out.println();
        
        if (!mInteractive)
            System.exit(1);
    }

    private static final int UNKNOWN_COMMAND = -1;
    
    public enum Command {
    
        ADD_ACCOUNT_ALIAS("addAccountAlias", "aaa", "{name@domain|id} {alias@domain}"),
        ADD_DISTRIBUTION_LIST_ALIAS("addDistributionListAlias", "adla", "{list@domain|id} {alias@domain}"),
        ADD_DISTRIBUTION_LIST_MEMBER("addDistributionListMember", "adlm", "{list@domain|id} {member@domain}"),
        AUTO_COMPLETE_GAL("autoCompleteGal", "acg", "{domain} {name}"),
        CREATE_ACCOUNT("createAccount", "ca", "{name@domain} {password} [attr1 value1 [attr2 value2...]]"),
        CREATE_BULK_ACCOUNTS("createBulkAccounts", "cabulk"),  //("  CreateBulkAccounts(cabulk) {domain} {namemask} {number of accounts to create} ");
        CREATE_CALENDAR_RESOURCE("createCalendarResource",  "ccr", "{name@domain} [attr1 value1 [attr2 value2...]]"),
        CREATE_COS("createCos", "cc", "{name} [attr1 value1 [attr2 value2...]]"),
        CREATE_DISTRIBUTION_LIST("createDistributionList", "cdl", "{list@domain}"),
        CREATE_DISTRIBUTION_LISTS_BULK("createDistributionListsBulk", "cdlbulk"),
        CREATE_DOMAIN("createDomain", "cd", "{domain} [attr1 value1 [attr2 value2...]]"),
        CREATE_SERVER("createServer", "cs", "{name} [attr1 value1 [attr2 value2...]]"),
        DELETE_ACCOUNT("deleteAccount", "da", "{name@domain|id}"),
        DELETE_CALENDAR_RESOURCE("deleteCalendarResource",  "dcr", "{name@domain|id}"),
        DELETE_COS("deleteCos", "dc", "{name|id}"),
        DELETE_DISTRIBUTION_LIST("deleteDistributionList", "ddl", "{list@domain|id}"),
        DELETE_DOMAIN("deleteDomain", "dd", "{domain|id}"),
        DELETE_SERVER("deleteServer", "ds", "{name|id}"),
        EXIT("exit", "quit", ""),
        GENERATE_DOMAIN_PRE_AUTH("generateDomainPreAuth", "gdpa", "{domain|id} {name} {name|id|foreignPrincipal} {timestamp|0} {expires|0}"),
        GENERATE_DOMAIN_PRE_AUTH_KEY("generateDomainPreAuthKey", "gdpak", "{domain|id}"),
        GET_ACCOUNT("getAccount", "ga", "{name@domain|id}"),
        GET_ACCOUNT_MEMBERSHIP("getAccountMembership", "gam", "{name@domain|id}"),
        GET_ALL_ACCOUNTS("getAllAccounts","gaa", "[-v] [{domain}]"),
        GET_ALL_ADMIN_ACCOUNTS("getAllAdminAccounts", "gaaa", "[-v]"),
        GET_ALL_CALENDAR_RESOURCES("getAllCalendarResources", "gacr", "[-v] [{domain}]"),
        GET_ALL_CONFIG("getAllConfig", "gacf", ""),
        GET_ALL_COS("getAllCos", "gac", "[-v]"),
        GET_ALL_DISTRIBUTION_LISTS("getAllDistributionLists", "gadl", "[-v]"),
        GET_ALL_DOMAINS("getAllDomains", "gad", "[-v]"),
        GET_ALL_SERVERS("getAllServers", "gas", "[-v] [service]"),
        GET_CALENDAR_RESOURCE("getCalendarResource",     "gcr", "{name@domain|id}"), 
        GET_CONFIG("getConfig", "gcf", "{name}"),
        GET_COS("getCos", "gc", "{name|id}"),
        GET_DISTRIBUTION_LIST("getDistributionList", "gdl", "{list@domain|id}"),
        GET_DISTRIBUTION_LIST_MEMBERSHIP("getDistributionListMembership", "gdlm", "{name@domain|id}"),
        GET_DOMAIN("getDomain", "gd", "{domain|id}"), 
        GET_SERVER("getServer", "gs", "{name|id}"), 
        HELP("help", "?", ""),
        IMPORT_NOTEBOOK("importNotebook", "impn", "[ -u {username} ] [ -p {password} ] [ -f {from dir} ] [ -t {to folder} ]"),
        INIT_NOTEBOOK("initNotebook", "in", "[ -u {username} ] [ -p {password} ] [ -d {domain} ] [ -f {from dir} ] [ -t {to folder} ]"),
        LDAP(".ldap", ".l"), 
        MODIFY_ACCOUNT("modifyAccount", "ma", "{name@domain|id} [attr1 value1 [attr2 value2...]]"),
        MODIFY_CALENDAR_RESOURCE("modifyCalendarResource",  "mcr", "{name@domain|id} [attr1 value1 [attr2 value2...]]"),
        MODIFY_CONFIG("modifyConfig", "mcf", "attr1 value1 [attr2 value2...]"),
        MODIFY_COS("modifyCos", "mc", "{name|id} [attr1 value1 [attr2 value2...]]"),
        MODIFY_DISTRIBUTION_LIST("modifyDistributionList", "mdl", "{list@domain|id} attr1 value1 [attr2 value2...]"),
        MODIFY_DOMAIN("modifyDomain", "md", "{domain|id} [attr1 value1 [attr2 value2...]]"),
        MODIFY_SERVER("modifyServer", "ms", "{name|id} [attr1 value1 [attr2 value2...]]"),
        REMOVE_ACCOUNT_ALIAS("removeAccountAlias", "raa", "{name@domain|id} {alias@domain}"),
        REMOVE_DISTRIBUTION_LIST_ALIAS("removeDistributionListAlias", "rdla", "{list@domain|id} {alias@domain}"),
        REMOVE_DISTRIBUTION_LIST_MEMBER("removeDistributionListMember", "rdlm", "{list@domain|id} {member@domain}"),
        RENAME_ACCOUNT("renameAccount", "ra", "{name@domain|id} {newName@domain}"),
        RENAME_CALENDAR_RESOURCE("renameCalendarResource",  "rcr", "{name@domain|id} {newName@domain}"),
        RENAME_COS("renameCos", "rc", "{name|id} {newName}"),
        RENAME_DISTRIBUTION_LIST("renameDistributionList", "rdl", "{list@domain|id} {newName@domain}"),
        SEARCH_ACCOUNTS("searchAccounts", "sa", "[-v] {ldap-query} [limit {limit}] [offset {offset}] [sortBy {attr}] [attrs {a1,a2...}] [sortAscending 0|1*] [applyCos [0|1*] [domain {domain}]"),
        SEARCH_CALENDAR_RESOURCES("searchCalendarResources", "scr", "[-v] domain attr op value [attr op value...]"),
        SEARCH_GAL("searchGal", "sg", "{domain} {name}"),
        SET_ACCOUNT_COS("setAccountCos", "sac", "{name@domain|id} {cos-name|cos-id}"),
        SET_PASSWORD("setPassword", "sp", "{name@domain|id} {password}"),
        SOAP(".soap", ".s"),
        SYNC_GAL("syncGal", "syg");

        private String mName;
        private String mAlias;
        private String mHelp;

        public String getName() { return mName; }
        public String getAlias() { return mAlias; }
        public String getHelp() { return mHelp; }
        public boolean hasHelp() { return mHelp != null; }

        private Command(String name, String alias) {
            mName = name;
            mAlias = alias;
        }

        private Command(String name, String alias, String help) {
            mName = name;
            mAlias = alias;
            mHelp = help;
        }
    }
    
    private static final int BY_ID = 1;
    private static final int BY_EMAIL = 2;
    private static final int BY_NAME = 3;
    
    private Map<String,Command> mCommandIndex;
    private Provisioning mProv;
    
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

    private ProvUtil() {
        initCommands();
        mProv = Provisioning.getInstance();
    }
    
    private boolean execute(String args[]) throws ServiceException, ArgException, IOException {

        Command c = lookupCommand(args[0]);
        
        if (c == null)
            return false;
        
        switch(c) {
        case ADD_ACCOUNT_ALIAS:
            doAddAccountAlias(args); 
            break;
        case AUTO_COMPLETE_GAL:
            doAutoCompleteGal(args); 
            break;            
        case CREATE_ACCOUNT:
            doCreateAccount(args); 
            break;                        
        case CREATE_COS:
            doCreateCos(args);
            break;        
        case CREATE_DOMAIN:
            doCreateDomain(args);
            break;
        case CREATE_SERVER:
            doCreateServer(args);
            break;            
        case EXIT:
            System.exit(0);
            break;
        case GENERATE_DOMAIN_PRE_AUTH_KEY:
            doGenerateDomainPreAuthKey(args);
            break;
        case GENERATE_DOMAIN_PRE_AUTH:
            doGenerateDomainPreAuth(args);
            break;            
        case GET_ACCOUNT:
            doGetAccount(args); 
            break;
        case GET_ACCOUNT_MEMBERSHIP:
            doGetAccountMembership(args);
            break;
        case GET_ALL_ACCOUNTS:
            doGetAllAccounts(args); 
            break;            
        case GET_ALL_ADMIN_ACCOUNTS:
            doGetAllAdminAccounts(args);
            break;                        
        case GET_ALL_CONFIG:
            doGetAllConfig(args); 
            break;            
        case GET_ALL_COS:
            doGetAllCos(args); 
            break;                        
        case GET_ALL_DOMAINS:
            doGetAllDomains(args); 
            break;                        
        case GET_ALL_SERVERS:
            doGetAllServers(args); 
            break;
        case GET_CONFIG:
            doGetConfig(args); 
            break;                        
        case GET_COS:
            doGetCos(args); 
            break;
        case GET_DISTRIBUTION_LIST_MEMBERSHIP:
            doGetDistributionListMembership(args);
            break;            
        case GET_DOMAIN:
            doGetDomain(args); 
            break;                        
        case GET_SERVER:
            doGetServer(args); 
            break;
        case HELP:
            doHelp(args); 
            break;            
        case MODIFY_ACCOUNT:
            doModifyAccount(args); 
            break;            
        case MODIFY_COS:
            doModifyCos(args); 
            break;            
        case MODIFY_CONFIG:
            doModifyConfig(args); 
            break;                        
        case MODIFY_DOMAIN:
            doModifyDomain(args); 
            break;            
        case MODIFY_SERVER:
            doModifyServer(args); 
            break;            
        case DELETE_ACCOUNT:
            doDeleteAccount(args); 
            break;            
        case DELETE_COS:
            doDeleteCos(args); 
            break;            
        case DELETE_DOMAIN:
            doDeleteDomain(args); 
            break;            
        case DELETE_SERVER:
            doDeleteServer(args); 
            break;
        case REMOVE_ACCOUNT_ALIAS:
            doRemoveAccountAlias(args); 
            break;            
        case RENAME_ACCOUNT:
            doRenameAccount(args); 
            break;                        
        case RENAME_COS:
            doRenameCos(args); 
            break;                                    
        case SET_ACCOUNT_COS:
            doSetAccountCos(args); 
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
            doSetPassword(args); 
            break;
        case CREATE_DISTRIBUTION_LIST:
            doCreateDistributionList(args);
            break;
        case CREATE_DISTRIBUTION_LISTS_BULK:
            doCreateDistributionListsBulk(args);
            break;            
        case GET_ALL_DISTRIBUTION_LISTS:
            doGetAllDistributionLists(args);
            break;
        case GET_DISTRIBUTION_LIST:
            doGetDistributionList(args);
            break;
        case MODIFY_DISTRIBUTION_LIST:
            doModifyDistributionList(args);
            break;
        case DELETE_DISTRIBUTION_LIST:
            doDeleteDistributionList(args);
            break;
        case ADD_DISTRIBUTION_LIST_MEMBER:
            doAddDistributionListMember(args);
            break;
        case REMOVE_DISTRIBUTION_LIST_MEMBER:
            doRemoveDistributionListMember(args);
            break;
        case CREATE_BULK_ACCOUNTS:
            doCreateAccountsBulk(args);
            break;
        case ADD_DISTRIBUTION_LIST_ALIAS:
            doAddDistributionListAlias(args);
            break;
        case REMOVE_DISTRIBUTION_LIST_ALIAS:
            doRemoveDistributionListAlias(args);
            break;
        case RENAME_DISTRIBUTION_LIST:
            doRenameDistributionList(args);
            break;
        case CREATE_CALENDAR_RESOURCE:
            doCreateCalendarResource(args);
            break;
        case DELETE_CALENDAR_RESOURCE:
            doDeleteCalendarResource(args);
            break;
        case MODIFY_CALENDAR_RESOURCE:
            doModifyCalendarResource(args);
            break;
        case RENAME_CALENDAR_RESOURCE:
            doRenameCalendarResource(args);
            break;
        case GET_CALENDAR_RESOURCE:
            doGetCalendarResource(args);
            break;
        case GET_ALL_CALENDAR_RESOURCES:
            doGetAllCalendarResources(args);
            break;
        case SEARCH_CALENDAR_RESOURCES:
            doSearchCalendarResources(args);
            break;
        case INIT_NOTEBOOK:
            importNotebook(args, true);
            break;
        case IMPORT_NOTEBOOK:
            importNotebook(args, false);
            break;
        case SOAP:
            // HACK FOR NOW
            SoapProvisioning sp = new SoapProvisioning();
            sp.soapSetURI("https://localhost:7071/service/admin/soap");
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

    private void doGenerateDomainPreAuthKey(String[] args) throws ServiceException {
        if (args.length < 2) {
            usage();
        } else {
            String key = args[1];
            Domain domain = lookupDomain(key);
            String preAuthKey = PreAuthKey.generateRandomPreAuthKey();
            HashMap<String,String> attrs = new HashMap<String,String>();
            attrs.put(Provisioning.A_zimbraPreAuthKey, preAuthKey);
            mProv.modifyAttrs(domain, attrs);
            System.out.printf("preAuthKey: %s\n", preAuthKey);
        }
    }

    private void doGenerateDomainPreAuth(String[] args) throws ServiceException {
        if (args.length != 6) {
            usage();
        } else {
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
    }

    private void doHelp(String[] args) {
        usage();
    }

    private void doSetPassword(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String password = args[2];
            Account account = lookupAccount(key);
            mProv.setPassword(account, password);
        }
    }

    private void doSetAccountCos(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String cosKey = args[2];
            Account account = lookupAccount(key);
            Cos cos = lookupCos(cosKey);
            mProv.setCOS(account, cos);
        }
    }

    private void doAddAccountAlias(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String alias = args[2];
            Account account = lookupAccount(key);
            mProv.addAlias(account, alias);
        }
    }

    private void doRemoveAccountAlias(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String alias = args[2];
            Account account = lookupAccount(key);
            mProv.removeAlias(account, alias);
        }
    }

    private void doRenameAccount(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String newName = args[2];
            Account account = lookupAccount(key);
            mProv.renameAccount(account.getId(), newName);
        }
    }

    private void doRenameCos(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String newName = args[2];
            Cos cos = lookupCos(key);
            mProv.renameCos(cos.getId(), newName);
        }
    }

    private void doCreateAccount(String[] args) throws ServiceException, ArgException {
        if (args.length < 3) {
            usage();
        } else {
            String name = args[1];
            String password = args[2];
            Map<String, Object> attrs = getMap(args, 3);
            if (password != null && password.equals(""))
                password = null;
            Account account = mProv.createAccount(name, password, attrs);
            System.out.println(account.getId());
        }
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

    private void doModifyAccount(String[] args) throws ServiceException, ArgException {
        if (args.length < 4) {
            usage();
        } else {
            String key = args[1];
            Map<String, Object> attrs = getMap(args, 2);
            Account account = lookupAccount(key);
            mProv.modifyAttrs(account, attrs, true);
        }
    }

    private void doModifyCos(String[] args) throws ServiceException, ArgException {
        if (args.length < 4) {
            usage();
        } else {
            String key = args[1];
            Map<String, Object> attrs = getMap(args, 2);
            Cos cos = lookupCos(key);
            mProv.modifyAttrs(cos, attrs, true);
        }
    }

    private void doModifyConfig(String[] args) throws ServiceException, ArgException {
        if (args.length < 3) {
            usage();
        } else {
            Map<String, Object> attrs = getMap(args, 1);
            mProv.modifyAttrs(mProv.getConfig(), attrs, true);
        }
    }

    private void doModifyDomain(String[] args) throws ServiceException, ArgException {
        if (args.length < 4) {
            usage();
        } else {
            String key = args[1];
            Map<String, Object> attrs = getMap(args, 2);
            Domain domain = lookupDomain(key);
            mProv.modifyAttrs(domain, attrs, true);
        }
    }    

    private void doModifyServer(String[] args) throws ServiceException, ArgException {
        if (args.length < 4) {
            usage();
        } else {
            String key = args[1];
            Map<String, Object> attrs = getMap(args, 2);
            Server server = lookupServer(key);
            mProv.modifyAttrs(server, attrs, true);
        }
    }

    private void doDeleteAccount(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Account account = lookupAccount(key);
            mProv.deleteAccount(account.getId());
        }
    }

    private void doDeleteCos(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Cos cos = lookupCos(key);
            mProv.deleteCos(cos.getId());
        }
    }
    
    private void doDeleteDomain(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Domain domain = lookupDomain(key);
            mProv.deleteDomain(domain.getId());
        }
    }    

    private void doDeleteServer(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Server server = lookupServer(key);
            mProv.deleteServer(server.getId());
        }
    }

    private void doGetAccount(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Account account = lookupAccount(key);
            dumpAccount(account);
        }
    }

    private void doGetAccountMembership(String[] args) throws ServiceException {
        if (args.length < 2) {
            usage();
        } else {
            String key = null;
            boolean idsOnly = false;
            if (args.length > 2) {
                idsOnly = args[1].equals("-g");
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
    }

    private void doGetDistributionListMembership(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
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
    }

    private void doGetCos(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Cos cos = lookupCos(key);
            dumpCos(cos);
        }
    }

    private void doGetConfig(String[] args) throws ServiceException {
        if (args.length < 2) {
            usage();
        } else {
            String key = args[1];
            String value[] = mProv.getConfig().getMultiAttr(key);
            if (value != null && value.length != 0) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put(key, value);
                dumpAttrs(map);
            }
        }
    }

    private void doGetDomain(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Domain domain = lookupDomain(key);
            dumpDomain(domain);
        }
    }    
    
    private void doGetServer(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Server server = lookupServer(key);
            dumpServer(server);
        }
    }    

    private void doGetAllAccounts(Domain domain, final boolean verbose) throws ServiceException {
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            public void visit(com.zimbra.cs.account.NamedEntry entry) throws ServiceException {
                if (verbose)
                    dumpAccount((Account) entry);
                else 
                    System.out.println(entry.getName());                        
            }
        };
         mProv.getAllAccounts(domain, visitor);
    }
    
    private void doGetAllAccounts(String[] args) throws ServiceException {
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
                doGetAllAccounts(domain, verbose);
            }
        } else {
            Domain domain = lookupDomain(d);
            doGetAllAccounts(domain, verbose);
        }
    }    

    private void doSearchAccounts(String[] args) throws ServiceException, ArgException {
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
        
        if (args.length < i+1) {
            usage();
            return;
        }
            
        String query = args[i];

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

        String applyCosStr  = (String) attrs.get("applyCos");
        boolean applyCos = (applyCosStr != null) ? "1".equalsIgnoreCase(applyCosStr) : true;    

        String typesStr = (String) attrs.get("types");
        int flags = Provisioning.SA_ACCOUNT_FLAG|Provisioning.SA_ALIAS_FLAG|Provisioning.SA_DISTRIBUTION_LIST_FLAG|Provisioning.SA_CALENDAR_RESOURCE_FLAG;
        
        if (typesStr != null)
            flags = Provisioning.searchAccountStringToMask(typesStr);

        String domainStr = (String)attrs.get("domain");
        List accounts;
        if (domainStr != null) {
            Domain d = lookupDomain(domainStr);
            accounts = Provisioning.getInstance().searchAccounts(d, query, attrsToGet, sortBy, isSortAscending, flags);
        } else {
            //accounts = mProvisioning.searchAccounts(query, attrsToGet, sortBy, isSortAscending, Provisioning.SA_ACCOUNT_FLAG);
            accounts = mProv.searchAccounts(query, attrsToGet, sortBy, isSortAscending, flags);
        }

        //ArrayList accounts = (ArrayList) mProvisioning.searchAccounts(query);
        for (int j=offset; j < offset+limit && j < accounts.size(); j++) {
            NamedEntry account = (NamedEntry) accounts.get(j);
            if (verbose) {
                if (account instanceof Account)
                    dumpAccount((Account)account, applyCos);
                else if (account instanceof Alias)
                    dumpAlias((Alias)account);
                else if (account instanceof DistributionList)
                    dumpDistributionList((DistributionList)account);
                else if (account instanceof Domain)
                    dumpDomain((Domain)account);
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

        SearchGalResult result = mProv.autoCompleteGal(d, query, Provisioning.GAL_SEARCH_TYPE.ALL, 100);
        for (GalContact contact : result.matches)
            dumpContact(contact);
    }    
    
    private void doSyncGal(String[] args) throws ServiceException {
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
        String token = args[i+1];
        
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
        for (Iterator it=accounts.iterator(); it.hasNext(); ) {
            Account account = (Account) it.next();
            if (verbose)
                dumpAccount(account);
            else 
                System.out.println(account.getName());
        }
    }    
    
    private void doGetAllCos(String[] args) throws ServiceException {
        boolean verbose = args.length > 1 && args[1].equals("-v");
        List allcos = mProv.getAllCos();
        for (Iterator it=allcos.iterator(); it.hasNext(); ) {
            Cos cos = (Cos) it.next();
            if (verbose)
                dumpCos(cos);
            else 
                System.out.println(cos.getName());
        }
    }        

    private void dumpCos(Cos cos) throws ServiceException {
        System.out.println("# name "+cos.getName());
        Map<String, Object> attrs = cos.getAttrs();
        dumpAttrs(attrs);
        System.out.println();
    }

    private void doGetAllConfig(String[] args) throws ServiceException {
        dumpAttrs(mProv.getConfig().getAttrs());
    }        

    private void doGetAllDomains(String[] args) throws ServiceException {
        boolean verbose = args.length > 1 && args[1].equals("-v");
                
        List domains = mProv.getAllDomains();
        for (Iterator it=domains.iterator(); it.hasNext(); ) {
            Domain domain = (Domain) it.next();
            if (verbose)
                dumpDomain(domain);
            else
                System.out.println(domain.getName());
        }
    }        

    private void dumpDomain(Domain domain) throws ServiceException {
        System.out.println("# name "+domain.getName());
        Map<String, Object> attrs = domain.getAttrs();
        dumpAttrs(attrs);
        System.out.println();
    }

    private void dumpDistributionList(DistributionList dl) throws ServiceException {
        String[] members = dl.getAllMembers();
        int count = members == null ? 0 : members.length; 
        System.out.println("# distributionList " + dl.getName() + " memberCount=" + count);
        Map<String, Object> attrs = dl.getAttrs();
        dumpAttrs(attrs);        
    }

    private void dumpAlias(Alias alias) throws ServiceException {
        System.out.println("# alias " + alias.getName());
        Map<String, Object> attrs = alias.getAttrs();
        dumpAttrs(attrs);        
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
                dumpServer(server);
            else 
                System.out.println(server.getName());
        }
    }        

    private void dumpServer(Server server) throws ServiceException {
        System.out.println("# name "+server.getName());
        Map<String, Object> attrs = server.getAttrs(true);
        dumpAttrs(attrs);
        System.out.println();
    }

    void dumpAccount(Account account) throws ServiceException {
        dumpAccount(account, true);
    }

    private void dumpAccount(Account account, boolean expandCos) throws ServiceException {
        System.out.println("# name "+account.getName());
        Map<String, Object> attrs = account.getAttrs(expandCos);
        dumpAttrs(attrs);
        System.out.println();
    }
    
    void dumpCalendarResource(CalendarResource  resource) throws ServiceException {
        dumpCalendarResource(resource, true);
    }

    private void dumpCalendarResource(CalendarResource resource, boolean expandCos) throws ServiceException {
        System.out.println("# name "+resource.getName());
        Map<String, Object> attrs = resource.getAttrs(expandCos);
        dumpAttrs(attrs);
        System.out.println();
    }
    
    private void dumpContact(GalContact contact) throws ServiceException {
        System.out.println("# name "+contact.getId());
        Map<String, Object> attrs = contact.getAttrs();
        dumpAttrs(attrs);
        System.out.println();
    }
    
    private void dumpAttrs(Map<String, Object> attrsIn) {
        TreeMap<String, Object> attrs = new TreeMap<String, Object>(attrsIn);

        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++) {
                    System.out.println(name+": "+sv[i]);
                }
            } else if (value instanceof String){
                System.out.println(name+": "+value);
            }
        }
    }

    private void doCreateCos(String[] args) throws ServiceException, ArgException {
        if (args.length < 2) {
            usage();
        } else {
            String name = args[1];
            Map<String, Object> attrs = getMap(args, 2);
            Cos cos = mProv.createCos(name, attrs);
            System.out.println(cos.getId());
        }
    }
    
    private void doCreateDomain(String[] args) throws ServiceException, ArgException {
        if (args.length < 2) {
            usage();
        } else {
            String name = args[1];
            Map<String, Object> attrs = getMap(args, 2);
            Domain domain = mProv.createDomain(name,attrs);
            System.out.println(domain.getId());
        }
    }
    
    private void doCreateServer(String[] args) throws ServiceException, ArgException {
        if (args.length < 2) {
            usage();
        } else {
            String name = args[1];
            Map<String, Object> attrs = getMap(args, 2);
            Server server = mProv.createServer(name, attrs);
            System.out.println(server.getId());
        }
    }    

    private void doCreateDistributionList(String[] args) throws ServiceException, ArgException {
        if (args.length < 2) {
            usage();
        } else {
            String name = args[1];
            Map<String, Object> attrs = getMap(args, 2);
            DistributionList dl = mProv.createDistributionList(name, attrs);
            System.out.println(dl.getId());
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
                Collection dls = mProv.getAllDistributionLists(domain);
                for (Iterator it = dls.iterator(); it.hasNext();) {
                    DistributionList dl = (DistributionList)it.next();
                    if (verbose)
                        dumpDistributionList(dl);
                    else
                        System.out.println(dl.getName());
                }
            }
        } else {
            Domain domain = lookupDomain(d);
            Collection dls = mProv.getAllDistributionLists(domain);
            for (Iterator it = dls.iterator(); it.hasNext();) {
                DistributionList dl = (DistributionList) it.next();
                if (verbose)
                    dumpDistributionList(dl);
                else
                    System.out.println(dl.getName());
            }
        }
    }

    private void doGetDistributionList(String[] args) throws ServiceException {
        if (args.length < 2) {
            usage();
        } else {
            String key = args[1];
            DistributionList dl = lookupDistributionList(key);
            dumpDistributionList(dl);
        }
    }

    private void doModifyDistributionList(String[] args) throws ServiceException, ArgException {
        if (args.length < 4) {
            usage();
        } else {
            String key = args[1];
            Map<String, Object> attrs = getMap(args, 2);
            DistributionList dl = lookupDistributionList(key);
            mProv.modifyAttrs(dl, attrs, true);
        }
    }

    private void doDeleteDistributionList(String[] args) throws ServiceException {
        if (args.length < 2) {
            usage();
        } else {
            String key = args[1];
            DistributionList dl = lookupDistributionList(key);
            mProv.deleteDistributionList(dl.getId());
        }
    }
    
    private void doAddDistributionListMember(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            DistributionList dl = lookupDistributionList(key);
            String [] members = new String[args.length - 2];
            System.arraycopy(args, 2, members, 0, args.length - 2);
            mProv.addMembers(dl, members);
        }        
    }

    private void doRemoveDistributionListMember(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            DistributionList dl = lookupDistributionList(key);
            String [] members = new String[args.length - 2];
            System.arraycopy(args, 2, members, 0, args.length - 2);
            mProv.removeMembers(dl, members);
        }
    }

    private void doAddDistributionListAlias(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String alias = args[2];
            DistributionList dl = lookupDistributionList(key);
            mProv.addAlias(dl, alias);
        }
    }

    private void doRemoveDistributionListAlias(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String alias = args[2];
            DistributionList dl = lookupDistributionList(key);
            mProv.removeAlias(dl, alias);
        }
    }

    private void doRenameDistributionList(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String newName = args[2];
            DistributionList dl = lookupDistributionList(key);
            mProv.renameDistributionList(dl.getId(), newName);
        }
    }

    private void doCreateCalendarResource(String[] args)
    throws ServiceException, ArgException {
        if (args.length < 2) {
            usage();
        } else {
            String name = args[1];
            Map<String, Object> attrs = getMap(args, 2);
            CalendarResource resource = mProv.createCalendarResource(name, attrs);
            System.out.println(resource.getId());
        }
    }

    private void doDeleteCalendarResource(String[] args)
    throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            CalendarResource resource = lookupCalendarResource(key);
            mProv.deleteCalendarResource(resource.getId());
        }
    }

    private void doModifyCalendarResource(String[] args)
    throws ServiceException, ArgException {
        if (args.length < 4) {
            usage();
        } else {
            String key = args[1];
            Map<String, Object> attrs = getMap(args, 2);
            CalendarResource res = lookupCalendarResource(key);
            mProv.modifyAttrs(res, attrs, true);
        }
    }

    private void doRenameCalendarResource(String[] args)
    throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String newName = args[2];
            CalendarResource res = lookupCalendarResource(key);
            mProv.renameCalendarResource(res.getId(), newName);
        }
    }

    private void doGetCalendarResource(String[] args)
    throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            CalendarResource res = lookupCalendarResource(key);
            dumpCalendarResource(res);
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
                    dumpCalendarResource((CalendarResource) entry);
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
                System.err.println("Bad search op in: " + attr + " " + op + " '" + value + "'");
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
                dumpCalendarResource(resource);
            else
                System.out.println(resource.getName());
        }
    }

    private void importNotebook(String[] args, boolean initialize) throws ServiceException {
    	if (args.length < 2) {usage(); return; }
    	
    	String domain = null;
    	String username = null;
    	String password = null;
    	String toFolder = null;
    	String fromDir = null;
    	boolean verbose = false;
    	
    	for (int i = 1; i < args.length;) {
    		String opt = args[i++];
    		if (opt.equals("-d")) domain = args[i++];
    		else if (opt.equals("-u")) username = args[i++];
    		else if (opt.equals("-p")) password = args[i++];
    		else if (opt.equals("-t")) toFolder = args[i++];
    		else if (opt.equals("-f")) fromDir = args[i++];
    		else if (opt.equals("-v")) verbose = true;
    		else { usage(); return; }
    	}
    	
    	WikiUtil wu = new WikiUtil(username, password);
    	if (verbose)
    		wu.setVerbose();
    	
    	if (initialize) {
    		// don't create mailboxes.  use soap instead.
    		if (domain == null) {
    			wu.initDefaultWiki(true);
    		} else {
    			wu.initDomainWiki(domain, true);
    		}
    	}
    	
    	if (fromDir != null) {
    		try {
    			wu.startImportSoap(toFolder, new java.io.File(fromDir));
    		} catch (Exception e) {
    			System.err.println("Cannot import templates from " + fromDir);
    			e.printStackTrace();
    			return;
    		}
    	}
    }
    
    private Account lookupAccount(String key) throws ServiceException {
        Account a = null;
        switch(guessType(key)) {
        case BY_ID:
            a = mProv.get(AccountBy.id, key);
            break;
        case BY_EMAIL:
            a = mProv.get(AccountBy.name, key);
            break;
        case BY_NAME:
            a = mProv.get(AccountBy.name, key);            
            break;
        }
        if (a == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(key);
        else
            return a;
    }

    private CalendarResource lookupCalendarResource(String key) throws ServiceException {
        CalendarResource res = null;
        switch(guessType(key)) {
        case BY_ID:
            res = mProv.get(CalendarResourceBy.id, key);
            break;
        case BY_EMAIL:
            res = mProv.get(CalendarResourceBy.name, key);
            break;
        case BY_NAME:
            res = mProv.get(CalendarResourceBy.name, key);            
            break;
        }
        if (res == null)
            throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(key);
        else
            return res;
    }

    private Domain lookupDomain(String key) throws ServiceException {
        Domain d = null;
        switch(guessType(key)) {
        case BY_ID:
            d = mProv.get(DomainBy.id, key);
            break;
        case BY_NAME:
        default:
            d = mProv.get(DomainBy.name, key);
            break;
        }
        
        if (d == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(key);
        else
            return d;
    }
    
    private Cos lookupCos(String key) throws ServiceException {
        Cos c = null;
        switch(guessType(key)) {
        case BY_ID:
            c = mProv.get(CosBy.id, key);
            break;
        case BY_NAME:
        default:            
            c = mProv.get(CosBy.name, key);
            break;
        }
        
        if (c == null)
            throw AccountServiceException.NO_SUCH_COS(key);
        else
            return c;
    }

    private Server lookupServer(String key) throws ServiceException {
        Server s = null;
        switch(guessType(key)) {
        case BY_ID:
            s = mProv.get(ServerBy.id, key);
            break;
        case BY_NAME:
        default:            
            s = mProv.get(ServerBy.name, key);
            break;
        }
        
        if (s == null)
            throw AccountServiceException.NO_SUCH_SERVER(key);
        else
            return s;
    }

    private DistributionList lookupDistributionList(String key) throws ServiceException {
        DistributionList dl = null;
        switch(guessType(key)) {
        case BY_ID:
            dl = mProv.get(DistributionListBy.id, key);
            break;
        case BY_EMAIL:
        default:            
            dl = mProv.get(DistributionListBy.name, key);
            break;
        }
        
        if (dl == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(key);
        else
            return dl;
    }

    private Map<String, Object> getMap(String[] args, int offset) throws ArgException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        for (int i = offset; i < args.length; i+=2) {
            String n = args[i];
            if (i+1 >= args.length)
                throw new ArgException("not enough arguments");
            String v = args[i+1];
            StringUtil.addToMultiMap(attrs, n, v);
        }
        return attrs;
    }

    private void interactive(boolean verbose) throws IOException {
        mInteractive = true;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("prov> ");
            String line = in.readLine();
            if (line == null || line.length() == -1)
                break;
            if (verbose) {
                System.out.println(line);
            }
            String args[] = StringUtil.parseLine(line);
            if (args.length == 0)
                continue;
            try {
                if (!execute(args)) 
                    usage();
            } catch (ServiceException e) {
                Throwable cause = e.getCause();
                System.err.println("ERROR: " + e.getCode() + " (" + e.getMessage() + ")" + 
                        (cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")"));
                if (verbose) e.printStackTrace(System.err);
            } catch (ArgException e) {
                    usage();
            }
        }
    }

    public static void main(String args[]) throws IOException {
        Zimbra.toolSetup();
        
        ProvUtil pu = new ProvUtil();
        if (args.length < 1) {
            pu.interactive(false);
        } else if (args.length == 1 && args[0].equals("-v")) {
            pu.interactive(true);
        } else {
            try {
                if (!pu.execute(args))
                    pu.usage();
            } catch (ServiceException e) {
            	Throwable cause = e.getCause();
                System.err.println("ERROR: " + e.getCode() + " (" + e.getMessage() + ")" + 
                		(cause == null ? "" : " (cause: " + cause.getClass().getName() + " " + cause.getMessage() + ")"));  
                System.exit(2);
            } catch (ArgException e) {
                pu.usage();
            }
        }
    }
    
    class ArgException extends Exception {
        ArgException(String msg) {
            super(msg);
        }
    }
}

