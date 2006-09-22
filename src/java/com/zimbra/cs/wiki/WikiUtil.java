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
package com.zimbra.cs.wiki;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.client.*;
import com.zimbra.cs.client.soap.*;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapParseException;

public abstract class WikiUtil {
    
    private static final String sDEFAULTPASSWORD = "zimbra";
    
    private static final String sDEFAULTFOLDER = "Notebook";
    private static final String sDEFAULTTEMPLATEFOLDER = "Template";
    
    private static final String sDEFAULTVIEW = "wiki";

    protected String mUsername;
    protected String mPassword;
    
    protected Provisioning mProv;

    private static class WikiMboxUtil extends WikiUtil {
        public WikiMboxUtil(String user, String pass) {
            mUsername = user;
            mPassword = pass;
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
                    byte[] contents = ByteUtil.getContent(f);
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
                    
                    mbox.createDocument(octxt, where.getId(), name, contentType, mbox.getAccount().getName(), contents, null, type);
                }
            }
        }
        
        private void deleteItems(OperationContext octxt, Mailbox mbox, Folder where) throws ServiceException {
            
            List<Document> items = mbox.getWikiList(octxt, where.getId());
            for (Document doc : items) {
                mbox.delete(octxt, doc, null);
            }
            List<Folder> folders = where.getSubfolders(octxt);
            for (Folder f : folders) {
                deleteItems(octxt, mbox, f);
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
                
            }
        }
        
        public void startImport(String where, File what) throws ServiceException, IOException {
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
                    true,
                    null);
            mbox.grantAccess(octxt, 
                    template.getId(), 
                    ACL.GUID_PUBLIC, 
                    ACL.GRANTEE_PUBLIC, 
                    ACL.stringToRights("r"),
                    true,
                    null);
        }
        
        public void setVerbose() {
        }
        
    }
    
    private static class WikiSoapUtil extends WikiUtil {
        private LmcSession mSession;
        
        private String mUrl;
        private String mUploadUrl;
        
        public WikiSoapUtil(Provisioning soapProv, String server, String user, String pass) throws ServiceException {
        	mProv = soapProv;
        	mUsername = user;
        	mPassword = pass;
        	if (pass == null)
        		mPassword = sDEFAULTPASSWORD;
        	Server s;
        	if (server == null || server.equals("localhost"))
        		s = mProv.getLocalServer();
        	else
        		s = mProv.get(ServerBy.name, server);
        	mUrl = URLUtil.getMailURL(s, ZimbraServlet.USER_SERVICE_URI, false);
        	mUploadUrl = URLUtil.getMailURL(s, "/service/upload", false);
        }
        
        public void setVerbose() {
            LmcSoapRequest.setDumpXML(true);
        }
        
        private void auth() throws LmcSoapClientException, IOException, 
                SoapFaultException, ServiceException, SoapParseException {
            if (mSession != null)
                return;
            
            LmcAuthRequest auth = new LmcAuthRequest();
            auth.setUsername(mUsername);
            auth.setPassword(mPassword);
            LmcAuthResponse resp = (LmcAuthResponse) auth.invoke(mUrl);
            mSession = resp.getSession();
        }
        
        private LmcFolder findFolder(LmcFolder root, String dir) {
            if (root == null)
                return null;
            LmcFolder[] list = root.getSubFolders();
            if (list == null)
                return null;
            for (int i = 0; i < list.length; i++) {
                if (list[i].getName().equals(dir)) {
                    return list[i];
                }
            }
            return null;
        }

        private LmcFolder createFolder(LmcFolder parent, String dir) throws LmcSoapClientException, IOException, 
                SoapFaultException, ServiceException, SoapParseException {
            System.out.println("Creating folder " + dir);
            auth();
            LmcCreateFolderRequest req = new LmcCreateFolderRequest();
            req.setSession(mSession);
            req.setName(dir);
            req.setParentID(parent.getFolderID());
            req.setView(sDEFAULTVIEW);
            LmcCreateFolderResponse resp = (LmcCreateFolderResponse) req.invoke(mUrl);
            return resp.getFolder();
        }
        
        private LmcFolder getRootFolder() throws LmcSoapClientException, IOException, 
        SoapFaultException, ServiceException, SoapParseException {
            auth();
            LmcGetFolderRequest req = new LmcGetFolderRequest();
            req.setSession(mSession);
            LmcGetFolderResponse resp = (LmcGetFolderResponse) req.invoke(mUrl);
            return resp.getRootFolder();
        }

        private void setFolderAccess(LmcFolder f, String perm, String grantee, String d, boolean inherit)
        throws LmcSoapClientException, IOException, SoapFaultException, ServiceException, SoapParseException {
            auth();
            LmcFolderActionRequest req = new LmcFolderActionRequest();
            req.setSession(mSession);
            req.setFolderList(f.getFolderID());
            req.setOp("grant");
            req.setGrant(perm, grantee, d, inherit);
            req.invoke(mUrl);
        }
        
        protected void setFolderPermission(Account account, String grantee, String name, String id) 
        throws ServiceException {
            System.out.println("Initializing folders ");
            try {
                LmcFolder root = getRootFolder();
                LmcFolder f = findFolder(root, sDEFAULTFOLDER);
                setFolderAccess(f, "rwid", grantee, name, true);
                
                f = findFolder(root, sDEFAULTTEMPLATEFOLDER);
                if (f == null)
                    f = createFolder(root, sDEFAULTTEMPLATEFOLDER);
                
                setFolderAccess(f, "r", "pub", ACL.GUID_PUBLIC, true);
            } catch (Exception e) {
                throw WikiServiceException.ERROR("import", e);
            }
        }

        private boolean purgeFolder(LmcFolder folder) throws LmcSoapClientException, IOException, 
                SoapFaultException, ServiceException, SoapParseException {
            if (folder == null)
                return true;
            auth();
            if (folder.getView() == null ||
                !folder.getView().equals(sDEFAULTVIEW)) {
                return false;
            }
            LmcFolderActionRequest req = new LmcFolderActionRequest();
            req.setSession(mSession);
            req.setFolderList(folder.getFolderID());
            req.setOp("empty");
            req.invoke(mUrl);
            return true;
        }
        
        private void deleteAllItemsInFolder(LmcFolder folder, String parent) throws LmcSoapClientException, IOException, 
                SoapFaultException, ServiceException, SoapParseException {
            auth();
            if (purgeFolder(folder))
                return;
            String folderName = parent;
            if (folderName.length() == 0)
                folderName = "/";
            else if (folderName.equals("/"))
                folderName += folder.getName();
            else
                folderName += "/" + folder.getName();
            LmcSearchRequest req = new LmcSearchRequest();
            req.setSession(mSession);
            req.setQuery("in:\""+folderName+"\"");
            req.setTypes("wiki,document");
            LmcSearchResponse resp = (LmcSearchResponse) req.invoke(mUrl);
            @SuppressWarnings("unchecked")
            Iterator items = resp.getResults().listIterator();
            if (items.hasNext()) {
                String ids = "";
                while (items.hasNext()) {
                    LmcDocument doc = (LmcDocument) items.next();
                    if (!ids.equals("")) ids += ",";
                    ids += doc.getID();
                }
                LmcItemActionRequest actReq = new LmcItemActionRequest();
                actReq.setSession(mSession);
                actReq.setOp("delete");
                actReq.setMsgList(ids);
                actReq.invoke(mUrl);
            }
            LmcFolder[] list = folder.getSubFolders();
            if (list == null)
                return;
            for (int i = 0; i < list.length; i++) {
                LmcFolder f = list[i];
                if (f.getView() == null || !f.getView().equals("wiki"))
                    continue;
                deleteAllItemsInFolder(f, folderName);
            }
        }
        
        private void createDocument(LmcFolder where, File file) throws LmcSoapClientException, IOException, 
        SoapFaultException, ServiceException, SoapParseException {
            System.out.println("Creating file document " + file.getName() + " in folder " + where.getName());
            auth();
            LmcSaveDocumentRequest req = new LmcSaveDocumentRequest();
            req.setSession(mSession);

            URL url = new URL(mUrl);
            String domain = url.getHost();
            
            LmcDocument doc = new LmcDocument();
            doc.setFolder(where.getFolderID());
            String attachmentId = req.postAttachment(mUploadUrl, mSession, file, domain, 10000);
            doc.setAttachmentId(attachmentId);
            
            req.setDocument(doc);
            req.invoke(mUrl);
        }
        
        private void createWiki(LmcFolder where, File what) throws LmcSoapClientException, IOException, 
        SoapFaultException, ServiceException, SoapParseException {
            auth();
            String name = what.getName();
            if (name.toLowerCase().endsWith(".html") || name.toLowerCase().endsWith(".wiki"))
                name = name.substring(0, name.length() - 5);
            System.out.println("Creating wiki document " + name + " in folder " + where.getName());
            LmcWiki wiki = new LmcWiki();
            wiki.setWikiWord(name);
            wiki.setFolder(where.getFolderID());
            wiki.setContents(new String(ByteUtil.getContent(what), "utf-8"));
            
            LmcSaveWikiRequest req = new LmcSaveWikiRequest();
            req.setSession(mSession);
            req.setWiki(wiki);
            req.invoke(mUrl);
        }
        
        private void createItem(LmcFolder where, File what) throws LmcSoapClientException, IOException, 
        SoapFaultException, ServiceException, SoapParseException {
            // XXX use .wiki extension to distinguish wiki vs documents.
            if (what.getName().endsWith(".wiki") || what.getName().startsWith("_")) {
                createWiki(where, what);
            } else {
                createDocument(where, what);
            }
            /*
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(what.getName());
            // assume wiki when no extension.
            if (what.getName().indexOf('.') == -1)
                contentType = "text/html";
            if (contentType == null || !contentType.startsWith("text"))
                createDocument(where, what);
            else
                createWiki(where, what);
                */
        }
        private void populateFolders(LmcFolder where, File file)
        throws LmcSoapClientException, IOException, 
            SoapFaultException, ServiceException, SoapParseException {
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
                    LmcFolder sub = findFolder(where, f.getName());
                    if (sub == null)
                        sub = createFolder(where, f.getName());
                    populateFolders(sub, f);
                } else {
                    createItem(where, f);
                }
            }
        }

        protected void emptyNotebooks(String where) throws ServiceException {
            try {
                LmcFolder root = getRootFolder();
                LmcFolder f = findFolder(root, where);
                deleteAllItemsInFolder(f, "/");
                return;
            } catch (Exception e) {
                throw WikiServiceException.ERROR("emptyNotebooks", e);
            }
        }

        public void startImport(String where, File what) throws ServiceException {
            try {
                System.out.println("Initializing...");
                auth();

                if (where == null)
                    where = sDEFAULTFOLDER;

                emptyNotebooks(where);
                LmcFolder root = getRootFolder();
                LmcFolder f;
                if (where.equals("/"))
                    f = root;
                else {
                    f = findFolder(root, where);
                    if (f == null)
                        f = createFolder(root, where);
                }

                // start populating directories and files.
                populateFolders(f, what);
                return;
            } catch (Exception e) {
                throw WikiServiceException.ERROR("import", e);
            }
                
        }
    }
    
    public abstract void startImport(String folder, File dir) throws ServiceException, IOException;
    protected abstract void emptyNotebooks(String folder) throws ServiceException, IOException;
    protected abstract void setFolderPermission(Account account, String grantee, String name, String id) throws ServiceException;
    public abstract void setVerbose();
    
    public Account createWikiAccount() throws ServiceException {
        Account account = null;
        try {
        	account = mProv.get(AccountBy.name, mUsername);
        } catch (Exception e) {
        }
        
        if (account == null) {
            if (mPassword == null) {
                mPassword = sDEFAULTPASSWORD;
            }
            Map<String,Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraHideInGal, Provisioning.TRUE);
            attrs.put(Provisioning.A_zimbraIsSystemResource, Provisioning.TRUE);
            account = mProv.createAccount(mUsername, mPassword, attrs); 
        }
        return account;
    }
    
    private void initFolders(Account account, Entry entry) throws ServiceException {
        String grantee, name, id;
        Domain dom = null;
        if (entry instanceof Domain)
            dom = (Domain)entry;
        if (dom == null) {
            grantee = "pub";
            name = ACL.GUID_PUBLIC;
            id = ACL.GUID_PUBLIC;
        } else {
            grantee = "dom";
            name = dom.getName();
            id = dom.getId();
        }
        setFolderPermission(account, grantee, name, id);
    }
    
    public void initDefaultWiki() throws ServiceException {
        Config globalConfig = mProv.getConfig();
        initWiki(globalConfig);
    }
    
    public void initDomainWiki(String domain) throws ServiceException {
        if (domain == null) {
            throw WikiServiceException.ERROR("invalid argument - empty domain");
        }
        Domain dom = mProv.get(DomainBy.name, domain);
        if (dom == null) {
            throw WikiServiceException.ERROR("invalid domain: " + domain);
        }
        initDomainWiki(dom);
    }
    
    public void initDomainWiki(Domain dom) throws ServiceException {
        Config globalConfig = mProv.getConfig();
        String globalWikiAcct = globalConfig.getAttr(Provisioning.A_zimbraNotebookAccount, null);
        if (globalWikiAcct == null)
        	throw WikiServiceException.ERROR("must initialize global wiki before domain wiki");
        if (globalWikiAcct.equals(mUsername))
        	throw WikiServiceException.ERROR("domain wiki account must be different from global wiki account");
        initWiki(dom);
    }
    
    public void initWiki(Entry entry) throws ServiceException {
        String prevAcct = entry.getAttr(Provisioning.A_zimbraNotebookAccount);
        if (mUsername == null && prevAcct != null)
            mUsername = prevAcct;
        if (mUsername == null) {
            String errStr = (entry instanceof Config) ?
                    " in globalConfig" : " in domain "+((Domain)entry).getName(); 
            throw WikiServiceException.ERROR("empty LDAP domain attribute " +
                    Provisioning.A_zimbraNotebookAccount + errStr);
        }
        
        Account acct = createWikiAccount();
        initFolders(acct, entry);
        
        if (prevAcct == null || !prevAcct.equals(mUsername)) {
            ZimbraLog.wiki.info("updating default account from " + prevAcct + " to " + mUsername);
            HashMap<String,String> attrs = new HashMap<String,String>();
            attrs.put(Provisioning.A_zimbraNotebookAccount, mUsername);
            mProv.modifyAttrs(entry, attrs);
        }
    }
    
    /*
     * returns mbox based implementation.  used within the servlet container server.
     */
    public static WikiUtil getInstance(String user, String pass) {
        return new WikiMboxUtil(user, pass);
    }
    
    /*
     * returns soap based implementation.  used for command line based usage.
     */
    public static WikiUtil getInstance(Provisioning prov, String server, String user, String pass) throws ServiceException {
        return new WikiSoapUtil(prov, server, user, pass);
    }
    
    public static void main(String[] args) throws Exception {
        String defaultUsername = "user1";
        String defaultPassword = "test123";
        
        Zimbra.toolSetup();
        
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption("v", "verbose",  false, "verbose");
        options.addOption("s", "server",   true, "server name");
        options.addOption("u", "username", true, "username of the target account");
        options.addOption("p", "password", true, "password of the target account");
        
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
        
        String server, username, password;
        String dir = cl.getOptionValue("d");
        if (cl.hasOption("v")) 
            LmcSoapRequest.setDumpXML(true);
        server = cl.getOptionValue("s", LC.zimbra_zmprov_default_soap_server.value());
        username = cl.getOptionValue("u", defaultUsername);
        password = cl.getOptionValue("p", defaultPassword);
        
        SoapProvisioning sp = new SoapProvisioning();
        sp.soapSetURI("https://"+server+":"+LC.zimbra_admin_service_port.intValue()+ZimbraServlet.ADMIN_SERVICE_URI);
        sp.soapAdminAuthenticate(LC.zimbra_ldap_user.value(), LC.zimbra_ldap_password.value());

        WikiUtil prog = WikiUtil.getInstance(sp, server, username, password);
        prog.initDefaultWiki();
        prog.startImport("Template", new File(dir));
    }
}
