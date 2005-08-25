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
 */
package com.zimbra.cs.account;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.StringUtil;

/**
 * @author schemers
 *
 */
public class ProvUtil {
 
    private boolean mInteractive = false;
    
    private void usage() {
        System.out.println("zmprov [cmd] [args ...]");
        System.out.println("");

        //System.out.println("  CopyAccount(cpa) {name@domain} {ldap-url} {ldap-bind-dn} {ldap-bind-passwd}");
        //System.out.println("  CreateBulkAccounts(cabulk) {domain} {namemask} {number of accounts to create} ");
        System.out.println("  CreateAccount(ca) {name@domain} {password} [attr1 value1 [attr2 value2...]]");
        System.out.println("  DeleteAccount(da) {name@domain|id}");
        System.out.println("  GetAccount(ga) {name@domain|id}");
        System.out.println("  GetAllAccounts(gaa) [-v] [{domain}]");
        System.out.println("  GetAllAdminAccounts(gaaa) [-v]");
        System.out.println("  ModifyAccount(ma) {name@domain|id} [attr1 value1 [attr2 value2...]]");
        System.out.println("  SetPassword(sp) {name@domain|id} {password}");
        System.out.println("  AddAccountAlias(aaa) {name@domain|id} {alias@domain}");
        System.out.println("  RemoveAccountAlias(raa) {name@domain|id} {alias@domain}");
        System.out.println("  SetAccountCos(sac) {name@domain|id} {cos-name|cos-id}");        
        System.out.println("  SearchAccounts(sa) [-v] {ldap-query} [limit {limit}] [offset {offset}] [sortBy {attr}] [attrs {a1,a2...}] [sortAscending 0|1*] [applyCos [0|1*] [domain {domain}]");
        System.out.println("  SearchGal(sg) {domain} {name}");        
        System.out.println("  RenameAccount(ra) {name@domain|id} {newName@domain}");        
        System.out.println();

        System.out.println("  CreateDomain(cd) {domain} [attr1 value1 [attr2 value2...]]");
        System.out.println("  DeleteDomain(dd) {domain|id}");        
        System.out.println("  GetDomain(gd) {domain|id}");
        System.out.println("  GetAllDomains(gad) [-v]");
        System.out.println("  ModifyDomain(md) {domain|id} [attr1 value1 [attr2 value2...]]");        
        System.out.println();

        System.out.println("  CreateCos(cc) {name} [attr1 value1 [attr2 value2...]]");
        System.out.println("  DeleteCos(dc) {name|id}");        
        System.out.println("  GetCos(gc) {name|id}");
        System.out.println("  GetAllCos(gac) [-v]");        
        System.out.println("  ModifyCos(mc) {name|id} [attr1 value1 [attr2 value2...]]");
        System.out.println("  RenameCos(rc) {name|id} {newName}");
        System.out.println();
        
        System.out.println("  CreateServer(cs) {name} [attr1 value1 [attr2 value2...]]");
        System.out.println("  DeleteServer(ds) {name|id}");        
        System.out.println("  GetServer(gs) {name|id}");
        System.out.println("  GetAllServers(gas) [-v]");        
        System.out.println("  ModifyServer(ms) {name|id} [attr1 value1 [attr2 value2...]]");        
        System.out.println();
        
        System.out.println("  GetAllConfig(gacf)");
        System.out.println("  GetConfig(gcf) {name}");
        System.out.println("  ModifyConfig(mcf) attr1 value1 [attr2 value2...]");        
        System.out.println();

        System.out.println("  CreateDistributionList(cdl) {list@domain}");
        System.out.println("  GetAllDistributionLists(gadl) [-v]");
        System.out.println("  GetDistributionList(gdl) {list@domain|id}");
        System.out.println("  DeleteDistributionList(ddl) {list@domain|id}");
        System.out.println("  AddDistributionListMember(adlm) {list@domain|id} {member@domain}");
        System.out.println("  RemoveDistributionListMember(rdlm) {list@domain|id} {member@domain}");
        System.out.println();
        
        System.out.println("  exit (quit)");
        System.out.println("  help (?)");
        System.out.println();
        
        if (!mInteractive)
            System.exit(1);
    }

    private static final int UNKNOWN_COMMAND = -1;
    
    private static final int CREATE_ACCOUNT = 1;
    private static final int GET_ACCOUNT =  3;
    private static final int GET_ALL_ACCOUNTS = 4;
    private static final int GET_ALL_ADMIN_ACCOUNTS = 5;
    private static final int MODIFY_ACCOUNT =  6;
    private static final int DELETE_ACCOUNT = 7;
    private static final int SET_PASSWORD =  8;
    private static final int ADD_ACCOUNT_ALIAS = 9;
    private static final int REMOVE_ACCOUNT_ALIAS = 10;
        
    private static final int CREATE_DOMAIN = 11;
    private static final int GET_DOMAIN =  12;
    private static final int GET_ALL_DOMAINS = 13;
    private static final int MODIFY_DOMAIN = 14;
    private static final int DELETE_DOMAIN = 15;

    private static final int CREATE_COS = 16;
    private static final int GET_COS = 17;
    private static final int GET_ALL_COS = 18;
    private static final int MODIFY_COS = 19;
    private static final int DELETE_COS = 20;
    
    private static final int CREATE_SERVER = 21;
    private static final int GET_SERVER = 22;
    private static final int GET_ALL_SERVERS = 23;
    private static final int MODIFY_SERVER = 24;
    private static final int DELETE_SERVER = 25;
    
    private static final int EXIT = 26;
    private static final int HELP = 27;
    
    private static final int GET_ALL_CONFIG = 28;
    private static final int GET_CONFIG = 29;
    private static final int MODIFY_CONFIG = 30;
    
    private static final int SET_ACCOUNT_COS = 31;
    
    private static final int SEARCH_ACCOUNTS = 32;
    private static final int RENAME_ACCOUNT = 33;
    private static final int SEARCH_GAL = 34;    

    private static final int CREATE_DISTRIBUTION_LIST = 35;
    private static final int GET_ALL_DISTRIBUTION_LISTS = 36;
    private static final int GET_DISTRIBUTION_LIST = 37;
    private static final int DELETE_DISTRIBUTION_LIST = 38;
    private static final int ADD_DISTRIBUTION_LIST_MEMBER = 39;
    private static final int REMOVE_DISTRIBUTION_LIST_MEMBER = 40;
    private static final int RENAME_COS = 41;    
    
    private static final int COPY_ACCOUNT = 42;
    private static final int CREATE_BULK_ACCOUNTS = 43;
    private static final int BY_ID = 1;
    private static final int BY_EMAIL = 2;
    private static final int BY_NAME = 3;
    
    private Map mCommandIndex;
    private Provisioning mProvisioning;
    
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
    
    private void addCommand(String name, String alias, int index) {
        name = name.toLowerCase();
        if (mCommandIndex.get(name) != null)
            throw new RuntimeException("duplicate command: "+name);
        
        alias = alias.toLowerCase();
        if (mCommandIndex.get(alias) != null)
            throw new RuntimeException("duplicate command: "+alias);
        
        Integer i = new Integer(index);
        mCommandIndex.put(name, i);
        mCommandIndex.put(alias, i);
    }
    
    private void initCommands() {
        mCommandIndex = new HashMap();
        addCommand("addAccountAlias", "aaa", ADD_ACCOUNT_ALIAS);
        addCommand("createAccount", "ca", CREATE_ACCOUNT);
        addCommand("createBulkAccounts", "cabulk", CREATE_BULK_ACCOUNTS);
        addCommand("copyAccount", "cpa", COPY_ACCOUNT);        
        addCommand("getAccount", "ga", GET_ACCOUNT);        
        addCommand("getAllAccounts","gaa", GET_ALL_ACCOUNTS);
        addCommand("getAllAdminAccounts", "gaaa", GET_ALL_ADMIN_ACCOUNTS);
        addCommand("modifyAccount", "ma", MODIFY_ACCOUNT);
        addCommand("deleteAccount", "da", DELETE_ACCOUNT);
        addCommand("removeAccountAlias", "raa", REMOVE_ACCOUNT_ALIAS);
        addCommand("setPassword", "sp", SET_PASSWORD);
        addCommand("setAccountCos", "sac", SET_ACCOUNT_COS);        
        addCommand("searchAccounts", "sa", SEARCH_ACCOUNTS);                
        addCommand("renameAccount", "ra", RENAME_ACCOUNT);                        
        addCommand("searchGal", "sg", SEARCH_GAL);                                
    
        addCommand("createDomain", "cd", CREATE_DOMAIN);
        addCommand("getDomain", "gd", GET_DOMAIN);
        addCommand("getAllDomains", "gad", GET_ALL_DOMAINS);
        addCommand("modifyDomain", "md", MODIFY_DOMAIN);
        addCommand("deleteDomain", "dd", DELETE_DOMAIN);
        
        addCommand("createCos", "cc", CREATE_COS);
        addCommand("getCos", "gc", GET_COS);
        addCommand("getAllCos", "gac", GET_ALL_COS);
        addCommand("modifyCos", "mc", MODIFY_COS);
        addCommand("deleteCos", "dc", DELETE_COS);
        addCommand("renameCos", "rc", RENAME_COS);        
        
        addCommand("createServer", "cs", CREATE_SERVER);
        addCommand("getServer", "gs", GET_SERVER);
        addCommand("getAllServers", "gas", GET_ALL_SERVERS);
        addCommand("modifyServer", "ms", MODIFY_SERVER);
        addCommand("deleteServer", "ds", DELETE_SERVER);
        
        addCommand("getAllConfig", "gacf", GET_ALL_CONFIG);
        addCommand("getConfig", "gcf", GET_CONFIG);
        addCommand("modifyConfig", "mcf", MODIFY_CONFIG);

        addCommand("createDistributionList", "cdl", CREATE_DISTRIBUTION_LIST);
        addCommand("getAllDistributionLists", "gadl", GET_ALL_DISTRIBUTION_LISTS);
        addCommand("getDistributionList", "gdl", GET_DISTRIBUTION_LIST);
        addCommand("deleteDistributionList", "ddl", DELETE_DISTRIBUTION_LIST);
        addCommand("addDistributionListMember", "adlm", ADD_DISTRIBUTION_LIST_MEMBER);
        addCommand("removeDistributionListMember", "rdlm", REMOVE_DISTRIBUTION_LIST_MEMBER);

        addCommand("exit", "quit", EXIT);
        addCommand("help", "?", HELP);        
    }
    
    private int lookupCommand(String command) {
        Integer i = (Integer) mCommandIndex.get(command.toLowerCase());
        if (i == null)
            return UNKNOWN_COMMAND;
        else
            return i.intValue();
    }

    private ProvUtil() {
        initCommands();
        mProvisioning = Provisioning.getInstance();
    }
    
    private boolean execute(String args[]) throws ServiceException, ArgException {

        int c = lookupCommand(args[0]);
        
        if (c == UNKNOWN_COMMAND)
            return false;
        
        switch(c) {
        case ADD_ACCOUNT_ALIAS:
            doAddAccountAlias(args); 
            break;
        case COPY_ACCOUNT:
            doCopyAccount(args); 
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
        case GET_ACCOUNT:
            doGetAccount(args); 
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
        case SET_PASSWORD:
            doSetPassword(args); 
            break;
        case CREATE_DISTRIBUTION_LIST:
            doCreateDistributionList(args);
            break;
        case GET_ALL_DISTRIBUTION_LISTS:
            doGetAllDistributionLists(args);
            break;
        case GET_DISTRIBUTION_LIST:
            doGetDistributionList(args);
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
        default:
            return false;
        }
        return true;
    }

    private void doHelp(String[] args) throws ServiceException {
        usage();
    }
    
    /**
     * @param args
     * @throws ServiceException
     */
    private void doSetPassword(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String password = args[2];
            Account account = lookupAccount(key);
            mProvisioning.setPassword(account, password);
        }
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doSetAccountCos(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String cosKey = args[2];
            Account account = lookupAccount(key);
            Cos cos = lookupCos(cosKey);
            mProvisioning.setCOS(account, cos);
        }
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doAddAccountAlias(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String alias = args[2];
            Account account = lookupAccount(key);
            mProvisioning.addAlias(account, alias);
        }
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doRemoveAccountAlias(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String alias = args[2];
            Account account = lookupAccount(key);
            mProvisioning.removeAlias(account, alias);
        }
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doRenameAccount(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String newName = args[2];
            Account account = lookupAccount(key);
            mProvisioning.renameAccount(account.getId(), newName);
        }
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doRenameCos(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String newName = args[2];
            Cos cos = lookupCos(key);
            mProvisioning.renameCos(cos.getId(), newName);
        }
    }

    /**
     * @param args
     * @throws ServiceException
     * @throws ArgException
     */
    private void doCreateAccount(String[] args) throws ServiceException, ArgException {
        if (args.length < 3) {
            usage();
        } else {
            String name = args[1];
            String password = args[2];
            Map attrs = getMap(args, 3);
            if (password != null && password.equals(""))
                password = null;
            Account account = mProvisioning.createAccount(name, password, attrs);
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
            	HashMap attrs = new HashMap();
            	String displayName = nameMask + " N. " + Integer.toString(ix);
            	StringUtil.addToMultiMap(attrs, "displayName", displayName);
                Account account = mProvisioning.createAccount(name, password, attrs);
                System.out.println(account.getId());
           }
        }
    }
    /**
     * @param args
     * @throws ServiceException
     */
    private void doCopyAccount(String[] args) throws ServiceException {
        if (args.length < 5) {
            usage();
        } else {
            String name = args[1];
            String url = args[2];
            String dn = args[3];
            String password = args[4];
            Account account = mProvisioning.copyAccount(name, url, dn, password);
            System.out.println(account.getId());
        }
    }

    /**
     * @param args
     * @throws ServiceException
     * @throws ArgException
     */
    private void doModifyAccount(String[] args) throws ServiceException, ArgException {
        if (args.length < 4) {
            usage();
        } else {
            String key = args[1];
            Map attrs = getMap(args, 2);
            Account account = lookupAccount(key);
            account.modifyAttrs(attrs, true);
        }
    }

    /**
     * @param args
     * @throws ServiceException
     * @throws ArgException
     */
    private void doModifyCos(String[] args) throws ServiceException, ArgException {
        if (args.length < 4) {
            usage();
        } else {
            String key = args[1];
            Map attrs = getMap(args, 2);
            Cos cos = lookupCos(key);
            cos.modifyAttrs(attrs, true);
        }
    }

    /**
     * @param args
     * @throws ServiceException
     * @throws ArgException
     */
    private void doModifyConfig(String[] args) throws ServiceException, ArgException {
        if (args.length < 3) {
            usage();
        } else {
            Map attrs = getMap(args, 1);
            mProvisioning.getConfig().modifyAttrs(attrs, true);
        }
    }

    /**
     * @param args
     * @throws ServiceException
     * @throws ArgException
     */
    private void doModifyDomain(String[] args) throws ServiceException, ArgException {
        if (args.length < 4) {
            usage();
        } else {
            String key = args[1];
            Map attrs = getMap(args, 2);
            Domain domain = lookupDomain(key);
            domain.modifyAttrs(attrs, true);
        }
    }    

    /**
     * @param args
     * @throws ServiceException
     * @throws ArgException
     */
    private void doModifyServer(String[] args) throws ServiceException, ArgException {
        if (args.length < 4) {
            usage();
        } else {
            String key = args[1];
            Map attrs = getMap(args, 2);
            Server server = lookupServer(key);
            server.modifyAttrs(attrs, true);
        }
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doDeleteAccount(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Account account = lookupAccount(key);
            mProvisioning.deleteAccount(account.getId());
        }
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doDeleteCos(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Cos cos = lookupCos(key);
            mProvisioning.deleteCos(cos.getId());
        }
    }
    
    /**
     * @param args
     * @throws ServiceException
     */
    private void doDeleteDomain(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Domain domain = lookupDomain(key);
            mProvisioning.deleteDomain(domain.getId());
        }
    }    

    /**
     * @param args
     * @throws ServiceException
     */
    private void doDeleteServer(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Server server = lookupServer(key);
            mProvisioning.deleteServer(server.getId());
        }
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doGetAccount(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Account account = lookupAccount(key);
            dumpAccount(account);
        }
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doGetCos(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Cos cos = lookupCos(key);
            dumpCos(cos);
        }
    }


    /**
     * @param args
     * @throws ServiceException
     */
    private void doGetConfig(String[] args) throws ServiceException {
        if (args.length < 2) {
            usage();
        } else {
            String key = args[1];
            String value[] = mProvisioning.getConfig().getMultiAttr(key);
            if (value != null && value.length != 0) {
                HashMap map = new HashMap();
                map.put(key, value);
                dumpAttrs(map);
            }
        }
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doGetDomain(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Domain domain = lookupDomain(key);
            dumpDomain(domain);
        }
    }    
    
    /**
     * @param args
     * @throws ServiceException
     */
    private void doGetServer(String[] args) throws ServiceException {
        if (args.length != 2) {
            usage();
        } else {
            String key = args[1];
            Server server = lookupServer(key);
            dumpServer(server);
        }
    }    

    /**
     * @param args
     * @throws ServiceException
     */
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
            List domains = mProvisioning.getAllDomains();
            for (Iterator dit=domains.iterator(); dit.hasNext(); ) {
                Domain domain = (Domain) dit.next();
                Collection accounts = domain.getAllAccounts();
                for (Iterator it=accounts.iterator(); it.hasNext(); ) {
                    Account account = (Account) it.next();
                    if (verbose)
                        dumpAccount(account);
                    else
                        System.out.println(account.getName());
                }
            }
        } else {
            Domain domain = lookupDomain(d);
            Collection accounts = domain.getAllAccounts();
            for (Iterator it=accounts.iterator(); it.hasNext(); ) {
                Account account = (Account) it.next();
                if (verbose)
                    dumpAccount(account);
                else
                    System.out.println(account.getName());
            }
        }
    }    

    /**
     * @param args
     * @throws ServiceException
     * @throws ArgException
     */
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
        int iPageNum = 0;
        int iPerPage = 0;        

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

        String domainStr = (String)attrs.get("domain");
        ArrayList accounts;
        if (domainStr != null) {
            Domain d = lookupDomain(domainStr);
            accounts = d.searchAccounts(query, attrsToGet, sortBy, isSortAscending);    
        } else {
            accounts = mProvisioning.searchAccounts(query, attrsToGet, sortBy, isSortAscending);    
        }

        //ArrayList accounts = (ArrayList) mProvisioning.searchAccounts(query);
        for (int j=offset; j < offset+limit && j < accounts.size(); j++) {
            Account account = (Account) accounts.get(j);
            if (verbose)
                dumpAccount(account, applyCos);
            else
                System.out.println(account.getName());
        }
    }    

    /**
     * @param args
     * @throws ServiceException
     */
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

        List contacts = d.searchGal(query);
        for (Iterator it=contacts.iterator(); it.hasNext(); ) {
            GalContact contact = (GalContact) it.next();
            dumpContact(contact);
        }
    }    

    /**
     * @param args
     * @throws ServiceException
     */
    private void doGetAllAdminAccounts(String[] args) throws ServiceException {
        boolean verbose = args.length > 1 && args[1].equals("-v");
        List accounts = mProvisioning.getAllAdminAccounts();
        for (Iterator it=accounts.iterator(); it.hasNext(); ) {
            Account account = (Account) it.next();
            if (verbose)
                dumpAccount(account);
            else 
                System.out.println(account.getName());
        }
    }    
    
    /**
     * @param args
     * @throws ServiceException
     */
    private void doGetAllCos(String[] args) throws ServiceException {
        boolean verbose = args.length > 1 && args[1].equals("-v");
        List allcos = mProvisioning.getAllCos();
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
        Map attrs = cos.getAttrs();
        dumpAttrs(attrs);
        System.out.println();
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doGetAllConfig(String[] args) throws ServiceException {
        dumpAttrs(mProvisioning.getConfig().getAttrs());
    }        

    /**
     * @param args
     * @throws ServiceException
     */
    private void doGetAllDomains(String[] args) throws ServiceException {
        boolean verbose = args.length > 1 && args[1].equals("-v");
                
        List domains = mProvisioning.getAllDomains();
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
        Map attrs = domain.getAttrs();
        dumpAttrs(attrs);
        System.out.println();
    }

    private void dumpDistributionList(DistributionList dl) throws ServiceException {
        String[] members = dl.getAllMembers();
        int count = members == null ? 0 : members.length; 
        System.out.println("# distributionList " + dl.getName() + " memberCount=" + count);
        Map attrs = dl.getAttrs();
        dumpAttrs(attrs);        
    }
    
    /**
     * @param args
     * @throws ServiceException
     */
    private void doGetAllServers(String[] args) throws ServiceException {
        boolean verbose = args.length > 1 && args[1].equals("-v");
        List servers = mProvisioning.getAllServers();
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
        Map attrs = server.getAttrs(true);
        dumpAttrs(attrs);
        System.out.println();
    }

    private void dumpAccount(Account account) throws ServiceException {
        dumpAccount(account, true);
    }

    private void dumpAccount(Account account, boolean expandCos) throws ServiceException {
        System.out.println("# name "+account.getName());
        Map attrs = account.getAttrs(false, expandCos);
        dumpAttrs(attrs);
        System.out.println();
    }
    
    private void dumpContact(GalContact contact) throws ServiceException {
        System.out.println("# name "+contact.getId());
        Map attrs = contact.getAttrs();
        dumpAttrs(attrs);
        System.out.println();
    }
    
    /**
     * @param attrs
     */
    private void dumpAttrs(Map attrsIn) {
        TreeMap attrs = new TreeMap(attrsIn);
        
        for (Iterator mit=attrs.entrySet().iterator(); mit.hasNext(); ) {
            Map.Entry entry = (Entry) mit.next();
            String name = (String) entry.getKey();
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

    /**
     * @param args
     * @throws ServiceException
     * @throws ArgException
     */
    private void doCreateCos(String[] args) throws ServiceException, ArgException {
        if (args.length < 2) {
            usage();
        } else {
            String name = args[1];
            Map attrs = getMap(args, 2);
            Cos cos = mProvisioning.createCos(name,attrs);
            System.out.println(cos.getId());
        }
    }
    
    /**
     * @param args
     * @throws ServiceException
     * @throws ArgException
     */
    private void doCreateDomain(String[] args) throws ServiceException, ArgException {
        if (args.length < 2) {
            usage();
        } else {
            String name = args[1];
            Map attrs = getMap(args, 2);
            Domain domain = mProvisioning.createDomain(name,attrs);
            System.out.println(domain.getId());
        }
    }
    
    /**
     * @param args
     * @throws ServiceException
     * @throws ArgException
     */
    private void doCreateServer(String[] args) throws ServiceException, ArgException {
        if (args.length < 2) {
            usage();
        } else {
            String name = args[1];
            Map attrs = getMap(args, 2);
            Server server = mProvisioning.createServer(name,attrs);
            System.out.println(server.getId());
        }
    }    

    /**
     * @param args
     * @throws ServiceException
     * @throws ArgException
     */
    private void doCreateAdminAccount(String[] args) throws ServiceException, ArgException {
        if (args.length < 3) {
            usage();
        } else {
            String name = args[1];
            String password = args[2];
            Map attrs = getMap(args, 3);
            Account account = mProvisioning.createAdminAccount(name, password, attrs);
            System.out.println(account.getId());
        }
    }

    /**
     * @param args
     * @throws ServiceException
     * @throws ArgException
     */
    private void doCreateDistributionList(String[] args) throws ServiceException, ArgException {
        if (args.length < 2) {
            usage();
        } else {
            String name = args[1];
            Map attrs = getMap(args, 2);
            DistributionList dl = mProvisioning.createDistributionList(name, attrs);
            System.out.println(dl.getId());
        }
    }
    
    /**
     * @param args
     * @throws ServiceException
     */
    private void doGetAllDistributionLists(String[] args) throws ServiceException {
        boolean verbose = args.length > 1 && args[1].equals("-v");
                
        List dls = mProvisioning.getAllDistributionLists();
        for (Iterator it=dls.iterator(); it.hasNext(); ) {
            DistributionList dl = (DistributionList) it.next();
            if (verbose)
                dumpDistributionList(dl);
            else
                System.out.println(dl.getName());
        }
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doGetDistributionList(String[] args) throws ServiceException {
        if (args.length < 2) {
            usage();
        } else {
            String key = args[1];
            DistributionList dl = lookupDistributionList(key);
            dumpDistributionList(dl);
        }
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doDeleteDistributionList(String[] args) throws ServiceException {
        if (args.length < 2) {
            usage();
        } else {
            String key = args[1];
            DistributionList dl = lookupDistributionList(key);
            mProvisioning.deleteDistributionList(dl.getId());
        }
    }
    
    /**
     * @param args
     * @throws ServiceException
     */
    private void doAddDistributionListMember(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String member = args[2];
            DistributionList dl = lookupDistributionList(key);
            dl.addMember(member);
        }        
    }

    /**
     * @param args
     * @throws ServiceException
     */
    private void doRemoveDistributionListMember(String[] args) throws ServiceException {
        if (args.length < 3) {
            usage();
        } else {
            String key = args[1];
            String member = args[2];
            DistributionList dl = lookupDistributionList(key);
            dl.removeMember(member);
        }
    }

    private Account lookupAccount(String key) throws ServiceException {
        Account a = null;
        switch(guessType(key)) {
        case BY_ID:
            a = mProvisioning.getAccountById(key);
            break;
        case BY_EMAIL:
            a = mProvisioning.getAccountByName(key);
            break;
        case BY_NAME:
            a = mProvisioning.getAccountByName(key);            
            break;
        }
        if (a == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(key);
        else
            return a;
    }

    private Domain lookupDomain(String key) throws ServiceException {
        Domain d = null;
        switch(guessType(key)) {
        case BY_ID:
            d = mProvisioning.getDomainById(key);
            break;
        case BY_NAME:
        default:
            d = mProvisioning.getDomainByName(key);
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
            c = mProvisioning.getCosById(key);
            break;
        case BY_NAME:
            c = mProvisioning.getCosByName(key);
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
            s = mProvisioning.getServerById(key);
            break;
        case BY_NAME:
            s = mProvisioning.getServerByName(key);
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
            dl = mProvisioning.getDistributionListById(key);
            break;
        case BY_EMAIL:
            dl = mProvisioning.getDistributionListByName(key);
            break;
        }
        
        if (dl == null)
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(key);
        else
            return dl;
    }

    /**
     * @param args
     * @param i
     * @return
     * @throws ArgException
     */
    private Map getMap(String[] args, int offset) throws ArgException {
        HashMap attrs = new HashMap();
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

