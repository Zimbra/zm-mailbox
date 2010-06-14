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
package com.zimbra.cs.wiki;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.account.soap.SoapProvisioning.DelegateAuthResponse;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZSearchHit;
import com.zimbra.cs.zclient.ZSearchParams;
import com.zimbra.cs.zclient.ZSearchResult;
import com.zimbra.cs.zclient.ZFolder.View;
import com.zimbra.cs.zclient.ZGrant.GranteeType;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.CliUtil;

public abstract class WikiUtil {

    private static final String sDEFAULTFOLDER = "Notebook";
    private static final String sDEFAULTTEMPLATEFOLDER = "Template";

    private static final String sDEFAULTVIEW = "wiki";

    protected String mUsername;
    protected Provisioning mProv;

    private static class WikiMboxUtil extends WikiUtil {
        public WikiMboxUtil() {
            mProv = Provisioning.getInstance();
        }
        private void populateFolders(OperationContext octxt, Mailbox mbox, Folder where, File file) throws ServiceException, IOException {
            File[] list = file.listFiles();
            if (list == null)
                return;
            for (int i = 0; i < list.length; i++) {
                File f = list[i];

                // skip files and directories that start with "."
                if (f.getName().startsWith(".")) { continue;    }

                if (f.isDirectory()) {
                    Folder sub = mbox.createFolder(octxt, f.getName(), where.getId(), MailItem.getTypeForName(sDEFAULTVIEW), 0, MailItem.DEFAULT_COLOR, null);
                    populateFolders(octxt, mbox, sub, f);
                } else {
                    byte type = 0;
                    FileInputStream in = new FileInputStream(f);
                    String name = f.getName();
                    String contentType;

                    // XXX use .wiki extension to distinguish wiki vs documents.
                    if (name.endsWith(".wiki") || name.startsWith("_")) {
                        if (name.endsWith(".wiki"))
                            name = name.substring(0, name.length() - 5);
                        contentType = WikiItem.WIKI_CONTENT_TYPE;
                        type = MailItem.TYPE_WIKI;
                    } else {
                        contentType = URLConnection.getFileNameMap().getContentTypeFor(name);
                        if (contentType == null) {
                            contentType = "application/octet-stream";
                        }
                        type = MailItem.TYPE_DOCUMENT;
                    }
                    /*
                    if (f.getName().indexOf('.') != -1) {
                        contentType = URLConnection.getFileNameMap().getContentTypeFor(name);
                    } else {
                        contentType = WikiItem.WIKI_CONTENT_TYPE;
                    }
                    
                    if (contentType == null || !contentType.startsWith("text")) {
                        type = MailItem.TYPE_DOCUMENT;
                    } else {
                        type = MailItem.TYPE_WIKI;
                    }
                    */

                    mbox.createDocument(octxt, where.getId(), name, contentType, mbox.getAccount().getName(), in, type);
                }
            }
        }

        private void deleteItems(OperationContext octxt, Mailbox mbox, Folder where) throws ServiceException {
            if (where.getDefaultView() == MailItem.TYPE_WIKI) {
                mbox.emptyFolder(octxt, where.getId(), true);
                return;
            }
            List<Document> items = mbox.getDocumentList(octxt, where.getId());
            for (Document doc : items) {
                mbox.delete(octxt, doc, null);
            }
            List<Folder> folders = where.getSubfolders(octxt);
            for (Folder f : folders) {
                deleteItems(octxt, mbox, f);
                if (f.getDefaultView() == MailItem.TYPE_WIKI)
                	mbox.delete(octxt, f, null);
            }
        }

        protected void emptyNotebooks(String where) throws ServiceException {
            Account acct = mProv.get(AccountBy.name, mUsername);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
            OperationContext octxt = new OperationContext(acct);

            try {
                Folder f = mbox.getFolderByPath(octxt, where);
                deleteItems(octxt, mbox, f);
            } catch (ServiceException se) {
            	ZimbraLog.wiki.error("error emptying Notebook folders", se);
            }
        }

        public void startImport(String username, String where, File what) throws ServiceException, IOException {
            mUsername = username;
            emptyNotebooks(where);
            Account acct = mProv.get(AccountBy.name, mUsername);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
            OperationContext octxt = new OperationContext(acct);

            Folder f = null;
            try {
                f = mbox.getFolderByPath(octxt, where);
            } catch (ServiceException se) {
                f = mbox.createFolder(octxt, where, Mailbox.ID_FOLDER_USER_ROOT, MailItem.TYPE_WIKI, 0, MailItem.DEFAULT_COLOR, null);
            }
            populateFolders(octxt, mbox, f, what);
        }

        protected void setFolderPermission(Account account, String grantee, String name, String id) throws ServiceException {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            OperationContext octxt = new OperationContext(account);
            Folder template;
            try {
                template = mbox.getFolderByPath(octxt, sDEFAULTTEMPLATEFOLDER);
            } catch (ServiceException se) {
                template = mbox.createFolder(octxt,
                        sDEFAULTTEMPLATEFOLDER,
                        Mailbox.ID_FOLDER_USER_ROOT,
                        MailItem.getTypeForName(sDEFAULTVIEW),
                        0, MailItem.DEFAULT_COLOR, null);
            }
            mbox.grantAccess(octxt,
                    Mailbox.ID_FOLDER_NOTEBOOK,
                    id,
                    grantee.equals("pub") ? ACL.GRANTEE_PUBLIC : ACL.GRANTEE_DOMAIN,
                    ACL.stringToRights("rwid"),
                    null);
            mbox.grantAccess(octxt,
                    template.getId(),
                    GuestAccount.GUID_PUBLIC,
                    ACL.GRANTEE_PUBLIC,
                    ACL.stringToRights("r"),
                    null);
        }
        
        public void updateTemplates(Server server, File templateDir) throws ServiceException {
        	throw WikiServiceException.ERROR("use soap based utility.");
        }
    }

    private static class WikiSoapUtil extends WikiUtil {
        private ZMailbox mMbox;

        public WikiSoapUtil(Provisioning soapProv) throws ServiceException {
            mProv = soapProv;
        }

        private void auth() throws ServiceException {
        	if (!(mProv instanceof SoapProvisioning))
        		return;
            SoapProvisioning prov = (SoapProvisioning) mProv;
            DelegateAuthResponse dar = prov.delegateAuth(AccountBy.name, mUsername, 60*60*24);
            ZMailbox.Options options = new ZMailbox.Options(dar.getAuthToken(), prov.soapGetURI());
            options.setTargetAccount(mUsername);
            mMbox = ZMailbox.getMailbox(options);
        }

        private ZFolder findFolder(ZFolder root, String dir) {
            if (root == null)
                return null;
            if (dir.equals("/"))
            	dir = "USER_ROOT";
            if (root.getName().equals(dir))
            	return root;
            List<ZFolder> list = root.getSubFolders();
            if (list == null)
                return null;
            for (ZFolder f : list) {
                if (f.getName().equals(dir)) {
                    return f;
                }
            }
            return null;
        }

        private ZFolder createFolder(ZFolder parent, String dir) throws ServiceException {
            System.out.println("Creating folder " + dir);
            return mMbox.createFolder(parent.getId(), dir, ZFolder.View.valueOf(sDEFAULTVIEW), null, null, null);
        }

        private ZFolder getRootFolder() throws ServiceException {
            return mMbox.getUserRoot();
        }

        protected void setFolderPermission(Account account, String grantee, String name, String id) throws ServiceException {
            System.out.println("Initializing folders ");
            auth();
            ZFolder root = getRootFolder();
            ZFolder f = findFolder(root, sDEFAULTFOLDER);
            if (f == null)
            	throw WikiServiceException.ERROR("can't open mailbox for " + account.getName() + " (wrong host?)");
            mMbox.modifyFolderGrant(f.getId(), GranteeType.fromString(grantee), name, "rwid", null);
            f = findFolder(root, sDEFAULTTEMPLATEFOLDER);
            if (f == null)
                f = createFolder(root, sDEFAULTTEMPLATEFOLDER);
            mMbox.modifyFolderGrant(f.getId(), GranteeType.pub, name, "r", null);
        }

        private boolean purgeFolder(ZFolder folder) throws ServiceException {
            if (folder == null)
                return true;
            if (folder.getDefaultView() == null ||
                !folder.getDefaultView().equals(View.valueOf(sDEFAULTVIEW))) {
                return false;
            }
            mMbox.emptyFolder(folder.getId());
            return true;
        }

        private void deleteAllItemsInFolder(ZFolder folder) throws IOException, ServiceException {
            if (purgeFolder(folder))
                return;
            StringBuilder buf = new StringBuilder();
            ZSearchParams params = new ZSearchParams("inid:"+folder.getId());
            params.setTypes("wiki,document");
            ZSearchResult result = mMbox.search(params);
            for (ZSearchHit hit: result.getHits()) {
                if (buf.length() > 0)
                    buf.append(",");
                buf.append(hit.getId());
            }
            if (buf.length() > 0)
            	mMbox.deleteItem(buf.toString(), null);
            for (ZFolder f : new ArrayList<ZFolder>(folder.getSubFolders())) {
            	int fid = Integer.valueOf(f.getId());
            	View v = f.getDefaultView();
            	if (fid == Mailbox.ID_FOLDER_NOTEBOOK || fid >= Mailbox.FIRST_USER_ID)
            		deleteAllItemsInFolder(f);
            	if (v != null && v.equals(View.valueOf(sDEFAULTVIEW)) && fid >= Mailbox.FIRST_USER_ID)
            		mMbox.deleteFolder(f.getId());
            }
        }

        private void createItem(ZFolder where, File what) throws IOException, ServiceException {
            byte[] content = ByteUtil.getContent(what);
            String name = what.getName();
            // XXX use .wiki extension to distinguish wiki vs documents.
            if (name.endsWith(".wiki") || name.startsWith("_")) {
                if (name.endsWith(".wiki"))
                    name = name.substring(0, name.length()-5);
                System.out.println("Creating wiki page " + name + " in folder " + where.getName());
                mMbox.createWiki(where.getId(), name, new String(content, "UTF-8"));
            } else {
                System.out.println("Creating file document " + name + " in folder " + where.getName());
                String contentType = URLConnection.getFileNameMap().getContentTypeFor(name);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                String attachmentId = mMbox.uploadAttachment(name, content, contentType, 10*1000);
                mMbox.createDocument(where.getId(), name, attachmentId);
            }
        }
        private void populateFolders(ZFolder where, File file) throws IOException, ServiceException {
            if (where == null) {
                throw new IllegalArgumentException("null folder");
            }
            File[] list = file.listFiles();
            if (list == null)
                throw WikiServiceException.ERROR("directory does not exist : "+file.getPath());
            for (int i = 0; i < list.length; i++) {
                File f = list[i];

                // skip files and directories that start with "."
                if (f.getName().startsWith(".")) { continue;    }

                if (f.isDirectory()) {
                    ZFolder sub = findFolder(where, f.getName());
                    if (sub == null)
                        sub = createFolder(where, f.getName());
                    populateFolders(sub, f);
                } else {
                    createItem(where, f);
                }
            }
        }

        protected void emptyNotebooks(String where) throws IOException, ServiceException {
        	ZFolder root = getRootFolder();
        	ZFolder f = findFolder(root, where);
        	deleteAllItemsInFolder(f);
        }

        public void startImport(String username, String where, File what) throws IOException, ServiceException {
            mUsername = username;

            System.out.println("Initializing...");
            auth();

            if (where.startsWith("/") && where.length() > 1)
            	where = where.substring(1);
            if (where == null)
            	where = sDEFAULTFOLDER;

            emptyNotebooks(where);
            ZFolder root = getRootFolder();
            ZFolder f;
            if (where.equals("/"))
            	f = root;
            else {
            	f = findFolder(root, where);
            	if (f == null)
            		f = createFolder(root, where);
            }

            // start populating directories and files.
            populateFolders(f, what);

        }
        
        public void updateTemplates(Server server, File templateDir) throws IOException, ServiceException {
        	if (!templateDir.exists())
                throw WikiServiceException.ERROR("template dir "+templateDir.getCanonicalPath()+" doesn't exist");

            Config globalConfig = mProv.getConfig();
            String wikiAccount = globalConfig.getAttr(Provisioning.A_zimbraNotebookAccount, null);
            if (isAccountOnServer(wikiAccount, server)) {
            	System.out.println("updating global wiki account "+wikiAccount);
            	startImport(wikiAccount, WikiUtil.sDEFAULTTEMPLATEFOLDER, templateDir);
            }
            for (Domain d : mProv.getAllDomains()) {
            	wikiAccount = d.getAttr(Provisioning.A_zimbraNotebookAccount, null);
                if (isAccountOnServer(wikiAccount, server)) {
                	System.out.println("updating wiki account "+wikiAccount+" for domain "+d.getName());
                	startImport(wikiAccount, WikiUtil.sDEFAULTTEMPLATEFOLDER, templateDir);
                }
            }
        }
        
        private boolean isAccountOnServer(String acct, Server server) throws ServiceException {
        	if (acct == null)
        		return false;
        	Account a = mProv.get(AccountBy.name, acct);
        	if (a == null)
        		return false;
        	if (server == null)
        		return true;
            String target = a.getAttr(Provisioning.A_zimbraMailHost);
            String host = server.getAttr(Provisioning.A_zimbraServiceHostname);
            return (target != null && target.equalsIgnoreCase(host));
        }
    }

    public abstract void startImport(String username, String folder, File dir) throws ServiceException, IOException;
    public abstract void updateTemplates(Server server, File templateDir) throws ServiceException, IOException;
    protected abstract void emptyNotebooks(String folder) throws ServiceException, IOException;
    protected abstract void setFolderPermission(Account account, String grantee, String name, String id) throws ServiceException, IOException;

    private void initFolders(Account account, Entry entry) throws ServiceException, IOException {
        String grantee, name, id;
        Domain dom = null;
        if (entry instanceof Domain)
            dom = (Domain)entry;
        if (dom == null) {
            grantee = "pub";
            name = GuestAccount.GUID_PUBLIC;
            id = GuestAccount.GUID_PUBLIC;
        } else {
            grantee = "dom";
            name = dom.getName();
            id = dom.getId();
        }
        setFolderPermission(account, grantee, name, id);
    }

    public void initDefaultWiki(String username) throws ServiceException {
        Config globalConfig = mProv.getConfig();
        mUsername = globalConfig.getAttr(Provisioning.A_zimbraNotebookAccount, null);
        initWiki(globalConfig, username);
    }

    public void initDomainWiki(String domain, String username) throws ServiceException {
        if (domain == null) {
            throw WikiServiceException.ERROR("invalid argument - empty domain");
        }
        Domain dom = mProv.get(DomainBy.name, domain);
        if (dom == null) {
            throw WikiServiceException.ERROR("invalid domain: " + domain);
        }
        initDomainWiki(dom, username);
    }

    public void initDomainWiki(Domain dom, String username) throws ServiceException {
        Config globalConfig = mProv.getConfig();
        String globalWikiAcct = globalConfig.getAttr(Provisioning.A_zimbraNotebookAccount, null);
        if (globalWikiAcct == null)
            throw WikiServiceException.ERROR("must initialize global wiki before domain wiki");
        if (globalWikiAcct.equals(username))
            throw WikiServiceException.ERROR("domain wiki account must be different from global wiki account");
        initWiki(dom, username);
    }

    public void initWiki(Entry entry, String username) throws ServiceException {
    	String defaultUsername = entry.getAttr(Provisioning.A_zimbraNotebookAccount, null);

        if (username == null && defaultUsername == null) {
            String errStr = (entry instanceof Config) ?
                    " in globalConfig" : " in domain "+((Domain)entry).getName();
            throw WikiServiceException.ERROR(
                    Provisioning.A_zimbraNotebookAccount + " must be set" + errStr);
        }
        
    	if (username != null && !username.equals(defaultUsername)) {
    		Map<String,String> attrMap = new HashMap<String,String>();
    		attrMap.put(Provisioning.A_zimbraNotebookAccount, username);
    		mProv.modifyAttrs(entry, attrMap);
    	} else if (username == null) {
    		username = defaultUsername;
    	}

		if (mProv.get(AccountBy.name, username) == null)
			throw AccountServiceException.NO_SUCH_ACCOUNT(username);
		
		mUsername = username;
        Account acct = mProv.get(Provisioning.AccountBy.name, mUsername);
        if (!acct.getBooleanAttr(Provisioning.A_zimbraHideInGal, false) ||
        		!acct.getBooleanAttr(Provisioning.A_zimbraIsSystemResource, false)) {
        	Map<String,Object> attrs = new HashMap<String, Object>();
        	attrs.put(Provisioning.A_zimbraHideInGal, Provisioning.TRUE);
        	attrs.put(Provisioning.A_zimbraIsSystemResource, Provisioning.TRUE);
        	mProv.modifyAttrs(acct, attrs, true);
        }
        try {
            initFolders(acct, entry);
        } catch (IOException e) {
            throw WikiServiceException.ERROR("cannot initialize folders", e);
        }
    }

    /*
    * returns mbox based implementation.  used within the servlet container server.
    */
    public static WikiUtil getInstance() {
        return new WikiMboxUtil();
    }

    /*
    * returns soap based implementation.  used for command line based usage.
    */
    public static WikiUtil getInstance(Provisioning prov) throws ServiceException {
        return new WikiSoapUtil(prov);
    }

    public static void main(String[] args) throws Exception {
        String defaultUsername = "user1";

        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("WikiUtil", BuildInfo.VERSION);

        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption("v", "verbose",  false, "verbose");
        options.addOption("s", "server",   true, "server name");
        options.addOption("u", "username", true, "username of the target account");

        Option opt = new Option("d", "dir", true, "top level directory");
        opt.setArgName("top-level-dir");
        opt.setRequired(true);
        options.addOption(opt);

        CommandLine cl = null;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException pe) {
            System.err.println("error: "+pe.getMessage());
            System.exit(1);
        }

        assert(cl != null);

        String server, username;
        String dir = cl.getOptionValue("d");
        server = cl.getOptionValue("s", LC.zimbra_zmprov_default_soap_server.value());
        username = cl.getOptionValue("u", defaultUsername);

        SoapProvisioning sp = new SoapProvisioning();
        sp.soapSetURI("https://"+server+":"+LC.zimbra_admin_service_port.intValue()+AdminConstants.ADMIN_SERVICE_URI);
        sp.soapAdminAuthenticate(LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());

        WikiUtil prog = WikiUtil.getInstance(sp);
        prog.initDefaultWiki(username);
        prog.startImport(username, "Template", new File(dir));
    }
}
