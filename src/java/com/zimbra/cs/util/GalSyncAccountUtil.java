/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.httpclient.URLUtil;

public class GalSyncAccountUtil {
	private static final int CREATE_ACCOUNT = 10;
	private static final int CREATE_DATASOURCE = 11;  // CREATE_DATASOURCE is just an alias of CREATE_ACCOUNT, see https://bugzilla.zimbra.com/show_bug.cgi?id=41322#c2
	private static final int DELETE_ACCOUNT = 12;
	private static final int TRICKLE_SYNC = 13;
	private static final int FULL_SYNC = 14;
	private static final int FORCE_SYNC = 15;

	private static final String CREATE_ACCOUNT_COMMAND = "createaccount";
	private static final String CREATE_DATASOURCE_COMMAND = "createdatasource";
	private static final String DELETE_ACCOUNT_COMMAND = "deleteaccount";
	private static final String TRICKLE_SYNC_COMMAND = "tricklesync";
	private static final String FULL_SYNC_COMMAND = "fullsync";
	private static final String FORCE_SYNC_COMMAND = "forcesync";
	
	private static Map<String,Integer> mCommands;
	
	private static void usage() {
		System.out.println("zmgsautil: {command}");
		System.out.println("\tcreateAccount -a {account-name} -n {datasource-name} --domain {domain-name} -t zimbra|ldap [-f {folder-name}] [-p {polling-interval}]");
		System.out.println("\tcreateDataSource -a {account-name} -n {datasource-name} --domain {domain-name} -t zimbra|ldap [-f {folder-name}] [-p {polling-interval}]");
		System.out.println("\tdeleteAccount [-a {account-name} | -i {account-id}]");
		System.out.println("\ttrickleSync [-a {account-name} | -i {account-id}] [-d {datasource-id}] [-n {datasource-name}]");
		System.out.println("\tfullSync [-a {account-name} | -i {account-id}] [-d {datasource-id}] [-n {datasource-name}]");
		System.out.println("\tforceSync [-a {account-name} | -i {account-id}] [-d {datasource-id}] [-n {datasource-name}]");
		System.exit(1);
	}
	
	private static void addCommand(String cmd, int cmdId) {
		mCommands.put(cmd, new Integer(cmdId));
	}
	
	private static int lookupCmd(String cmd) {
		Integer i = (Integer) mCommands.get(cmd.toLowerCase());
		if (i == null) {
			usage();
		}
		return i.intValue();
	}
	
	private static void setup() {
		mCommands = new HashMap<String,Integer>();
		addCommand(CREATE_ACCOUNT_COMMAND, CREATE_ACCOUNT);
		addCommand(CREATE_DATASOURCE_COMMAND, CREATE_DATASOURCE);
		addCommand(DELETE_ACCOUNT_COMMAND, DELETE_ACCOUNT);
		addCommand(TRICKLE_SYNC_COMMAND, TRICKLE_SYNC);
		addCommand(FULL_SYNC_COMMAND, FULL_SYNC);
		addCommand(FORCE_SYNC_COMMAND, FORCE_SYNC);
	}
	
	private String mUsername;
	private String mPassword;
	private String mAdminURL;
	private ZAuthToken mAuth;
	private SoapHttpTransport mTransport;

	private GalSyncAccountUtil() {
        String server = LC.zimbra_zmprov_default_soap_server.value();
        mAdminURL = URLUtil.getAdminURL(server);
        mUsername = LC.zimbra_ldap_user.value();
        mPassword = LC.zimbra_ldap_password.value();
	}

	private void checkArgs() throws ServiceException {
		if (mAccountName != null) {
	    	Account acct = Provisioning.getInstance().getAccountByName(mAccountName);
	    	if (acct == null)
	    		throw AccountServiceException.NO_SUCH_ACCOUNT(mAccountName);
	    	mAccountId = acct.getId();
		}
		if (mAccountId == null || (mDataSourceId == null && mDataSourceName == null))
			usage();
	}

	private String mAccountId;
	private String mAccountName;
	private String mDataSourceId;
	private String mDataSourceName;
	private boolean mFullSync;
	private boolean mForceSync;
	
	private void syncGalAccount() throws ServiceException, IOException {
		checkArgs();
        mTransport = null;
        try {
            mTransport = new SoapHttpTransport(mAdminURL);
            auth();
            mTransport.setAuthToken(mAuth);
    		XMLElement req = new XMLElement(AdminConstants.SYNC_GAL_ACCOUNT_REQUEST);
    		Element acct = req.addElement(AdminConstants.E_ACCOUNT);
    		acct.addAttribute(AdminConstants.A_ID, mAccountId);
    		Element ds = acct.addElement(AdminConstants.E_DATASOURCE);
    		if (mDataSourceId != null)
    			ds.addAttribute(AdminConstants.A_BY, "id").setText(mDataSourceId);
    		else
    			ds.addAttribute(AdminConstants.A_BY, "name").setText(mDataSourceName);
    		if (mFullSync)
    			ds.addAttribute(AdminConstants.A_FULLSYNC, "TRUE");
    		if (mForceSync)
    			ds.addAttribute(AdminConstants.A_RESET, "TRUE");
    			
    		mTransport.invoke(req);
        } finally {
            if (mTransport != null)
                mTransport.shutdown();
        }                        
	}
	private Element createGalSyncAccount(String accountName, String dsName, String domain, String type, String folder, String pollingInterval) throws ServiceException, IOException {
        mTransport = null;
        try {
            mTransport = new SoapHttpTransport(mAdminURL);
            auth();
            mTransport.setAuthToken(mAuth);
    		XMLElement req = new XMLElement(AdminConstants.CREATE_GAL_SYNC_ACCOUNT_REQUEST);
    		req.addAttribute(AdminConstants.A_NAME, dsName);
    		req.addAttribute(AdminConstants.A_DOMAIN, domain);
    		req.addAttribute(AdminConstants.A_TYPE, type);
    		if (folder != null)
        		req.addAttribute(AdminConstants.E_FOLDER, folder);
    		Element acct = req.addElement(AdminConstants.E_ACCOUNT);
    		acct.addAttribute(AdminConstants.A_BY, AccountBy.name.name());
    		acct.setText(accountName);
    		if (pollingInterval != null)
    			req.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, Provisioning.A_zimbraDataSourcePollingInterval).setText(pollingInterval);
    			
    		return mTransport.invokeWithoutSession(req);
        } finally {
            if (mTransport != null)
                mTransport.shutdown();
        }                        
	}
	private Element deleteGalSyncAccount(String name, String id) throws ServiceException, IOException {
        mTransport = null;
        try {
            mTransport = new SoapHttpTransport(mAdminURL);
            auth();
            mTransport.setAuthToken(mAuth);
    		XMLElement req = new XMLElement(AdminConstants.DELETE_GAL_SYNC_ACCOUNT_REQUEST);
    		Element acct = req.addElement(AdminConstants.E_ACCOUNT);
    		String account;
    		AccountBy by;
    		if (name == null) {
    			by = AccountBy.id;
    			account = id;
    		} else {
    			by = AccountBy.name;
    			account = name;
    		}
    		acct.addAttribute(AdminConstants.A_BY, by.name());
    		acct.setText(account);
    		return mTransport.invokeWithoutSession(req);
        } finally {
            if (mTransport != null)
                mTransport.shutdown();
        }                        
	}
	private void setAccountId(String aid) {
		mAccountId = aid;
	}
	private void setAccountName(String name) {
		mAccountName = name;
	}
	private void setDataSourceId(String did) {
		mDataSourceId = did;
	}
	private void setDataSourceName(String name) {
		mDataSourceName = name;
	}
	private void setFullSync() {
		mFullSync = true;
	}
	private void setForceSync() {
		mForceSync = true;
	}
	private void auth() throws ServiceException, IOException {
		XMLElement req = new XMLElement(AdminConstants.AUTH_REQUEST);
		req.addElement(AdminConstants.E_NAME).setText(mUsername);
		req.addElement(AdminConstants.E_PASSWORD).setText(mPassword);
		Element resp = mTransport.invoke(req);
		mAuth = new ZAuthToken(resp.getElement(AccountConstants.E_AUTH_TOKEN), true);
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length < 1)
			usage();
		CliUtil.toolSetup();
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption("a", "account", true, "gal sync account name");
        options.addOption("i", "id", true, "gal sync account id");
        options.addOption("n", "name", true, "datasource name");
        options.addOption("d", "did", true, "datasource id");
        options.addOption("x", "domain", true, "for domain gal sync account");
        options.addOption("f", "folder", true, "folder id");
        options.addOption("p", "polling", true, "polling interval");
        options.addOption("t", "type", true, "gal type");
        options.addOption("h", "help", true, "help");
        CommandLine cl = null;
        boolean err = false;
        try {
            cl = parser.parse(options, args, false);
        } catch (ParseException pe) {
            System.out.println("error: " + pe.getMessage());
            err = true;
        }

        GalSyncAccountUtil cli = new GalSyncAccountUtil();
        if (err || cl.hasOption('h')) {
        	usage();
        }
        if (cl.hasOption('i'))
            cli.setAccountId(cl.getOptionValue('i'));
        if (cl.hasOption('a'))
        	cli.setAccountName(cl.getOptionValue('a'));
        if (cl.hasOption('n'))
            cli.setDataSourceName(cl.getOptionValue('n'));
        if (cl.hasOption('d'))
            cli.setDataSourceId(cl.getOptionValue('d'));
		setup();
		int cmd = lookupCmd(args[0]);
		try {
			switch (cmd) {
			case TRICKLE_SYNC:
				cli.syncGalAccount();
				break;
			case FULL_SYNC:
				cli.setFullSync();
				cli.syncGalAccount();
				break;
			case FORCE_SYNC:
				cli.setForceSync();
				cli.syncGalAccount();
				break;
			case CREATE_ACCOUNT:
			case CREATE_DATASOURCE:    
				String acctName = cl.getOptionValue('a');
				String dsName = cl.getOptionValue('n');
				String domain = cl.getOptionValue('x');
				String type = cl.getOptionValue('t');
				String folderName = cl.getOptionValue('f');
				String pollingInterval = cl.getOptionValue('p');
				if (acctName == null || dsName == null || type == null || type.compareTo("zimbra") != 0 && type.compareTo("ldap") != 0)
					usage();
				for (Element account : cli.createGalSyncAccount(acctName, dsName, domain, type, folderName, pollingInterval).listElements(AdminConstants.A_ACCOUNT))
					System.out.println(account.getAttribute(AdminConstants.A_NAME)+"\t"+account.getAttribute(AdminConstants.A_ID));
				break;
			case DELETE_ACCOUNT:
				String name = cl.getOptionValue('a');
				String id = cl.getOptionValue('i');
				if (name == null && id == null)
					usage();
				cli.deleteGalSyncAccount(name, id);
				break;
			default:
				usage();
			}
		} catch (ServiceException se) {
			System.out.println("Error: "+se.getMessage());
		}
	}
}
