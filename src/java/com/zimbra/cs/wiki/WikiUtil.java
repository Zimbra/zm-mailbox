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
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.client.*;
import com.zimbra.cs.client.soap.*;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
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

public class WikiUtil {
	
	private static final String sUSERNAME = "user1";
	private static final String sPASSWORD = "test123";
	
	private static final String sDEFAULTPASSWORD = "zimbra";
	
	private static final String sDEFAULTFOLDER = "Notebook";
	private static final String sDEFAULTTEMPLATEFOLDER = "Template";
	
	private static final String sDEFAULTVIEW = "wiki";
	
	private LmcSession mSession;
	
	private String mUrl;
	private String mUploadUrl;
	private String mUsername;
	private String mPassword;
	
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
	
	private void initFolders(String grantee, String name) throws LmcSoapClientException, IOException, 
	SoapFaultException, ServiceException, SoapParseException {
		System.out.println("Initializing folders ");
		
        LmcFolder root = getRootFolder();
        LmcFolder f = findFolder(root, sDEFAULTFOLDER);
        setFolderAccess(f, "rwid", grantee, name, true);
        
        f = findFolder(root, sDEFAULTTEMPLATEFOLDER);
        if (f == null)
        	f = createFolder(root, sDEFAULTTEMPLATEFOLDER);
        
        setFolderAccess(f, "r", "pub", ACL.GUID_PUBLIC, true);
	}

	private boolean purgeFolder(LmcFolder folder) throws LmcSoapClientException, IOException, 
			SoapFaultException, ServiceException, SoapParseException {
		if (folder == null)
			return true;
		auth();
		if (folder.getView() == null ||
			!folder.getView().equals(sDEFAULTVIEW) ||
			folder.getName().equals(sDEFAULTFOLDER) ||
			folder.getName().equals(sDEFAULTTEMPLATEFOLDER)) {
			return false;
		}
		LmcFolderActionRequest req = new LmcFolderActionRequest();
		req.setSession(mSession);
		req.setFolderList(folder.getFolderID());
		req.setOp("delete");
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
		if (name.toLowerCase().endsWith(".html"))
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
		String contentType = URLConnection.getFileNameMap().getContentTypeFor(what.getName());
		// assume wiki when no extension.
		if (what.getName().indexOf('.') == -1)
			contentType = "text/html";
		if (contentType == null || !contentType.startsWith("text"))
			createDocument(where, what);
		else
			createWiki(where, what);
	}
	
	private void populateFolders(LmcFolder where, File file) throws LmcSoapClientException, IOException, 
			SoapFaultException, ServiceException, SoapParseException {
		if (where == null) {
			throw new IllegalArgumentException("null folder");
		}
        File[] list = file.listFiles();
        for (int i = 0; i < list.length; i++) {
        	File f = list[i];
        	
			// skip files and directories that start with "."
       		if (f.getName().startsWith(".")) { continue;	}

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
	
	public void startImportSoap(String where, File top) throws LmcSoapClientException, IOException, 
	SoapFaultException, ServiceException, SoapParseException {
		System.out.println("Initializing...");
		auth();

		if (where == null)
			where = sDEFAULTFOLDER;
		
        emptyNotebooks(where, true);
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
        populateFolders(f, top);
	}
	
	private void populateFolders(OperationContext octxt, Mailbox mbox, Folder where, File file) throws ServiceException, IOException {
		File[] list = file.listFiles();
		if (list == null)
			return;
		for (int i = 0; i < list.length; i++) {
			File f = list[i];
			
			// skip files and directories that start with "."
       		if (f.getName().startsWith(".")) { continue;	}

        	if (f.isDirectory()) {
        		Folder sub = mbox.createFolder(octxt, f.getName(), where.getId(), MailItem.getTypeForName(sDEFAULTVIEW), null);
        		populateFolders(octxt, mbox, sub, f);
        	} else {
        		byte type = 0;
        		byte[] contents = ByteUtil.getContent(f);
        		String name = f.getName();
        		String contentType;
        		
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
        		mbox.createDocument(octxt, where.getId(), name, contentType, mbox.getAccount().getName(), contents, null, type);
        	}
		}
	}
	
	/**
	 * Import a set of files to the user's default folder ("Notebook") using
	 * direct Mailbox API instead of SOAP.  Mailbox API should only be used within
	 * tomcat.  If ran outside tomcat, use SOAP to communicate to tomcat.
	 * 
	 * @param top
	 * @throws ServiceException
	 * @throws IOException
	 */
	public void startImport(File top) throws ServiceException, IOException {
		startImport(top, false);
	}
	
	public void startImport(File top, boolean useSoap) throws ServiceException, IOException {
		startImport(sDEFAULTFOLDER, top, useSoap);
	}
	
	public void startImport(String where, File what) throws ServiceException, IOException {
		startImport(where, what, false);
	}
	
	public void startImport(String where, File what, boolean useSoap) throws ServiceException, IOException {
		if (useSoap)
			try {
				startImportSoap(where, what);
				return;
			} catch (Exception e) {
				throw WikiServiceException.ERROR("import", e);
			}
			
		emptyNotebooks(where, useSoap);
		Provisioning prov = Provisioning.getInstance();
		Account acct = prov.get(AccountBy.name, mUsername);
		Mailbox mbox = Mailbox.getMailboxByAccount(acct);
		OperationContext octxt = new OperationContext(acct);
		
		Folder f = null;
		try {
			f = mbox.getFolderByPath(octxt, where);
		} catch (ServiceException se) {
			f = mbox.createFolder(octxt, where, Mailbox.ID_FOLDER_USER_ROOT, MailItem.TYPE_WIKI, null);
		}
		populateFolders(octxt, mbox, f, what);
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
	
	private void emptyNotebooks(String where, boolean useSoap) throws ServiceException {
		if (useSoap)
			try {
				LmcFolder root = getRootFolder();
				LmcFolder f = findFolder(root, where);
				deleteAllItemsInFolder(f, "/");
				return;
			} catch (Exception e) {
				throw WikiServiceException.ERROR("emptyNotebooks", e);
			}
		Provisioning prov = Provisioning.getInstance();
		Account acct = prov.get(AccountBy.name, mUsername);
		Mailbox mbox = Mailbox.getMailboxByAccount(acct);
		OperationContext octxt = new OperationContext(acct);
		
		try {
			Folder f = mbox.getFolderByPath(octxt, where);
			deleteItems(octxt, mbox, f);
		} catch (ServiceException se) {
			
		}
	}
	
	public Account createWikiAccount() throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Account account = prov.get(AccountBy.name, mUsername);
		if (account == null) {
			if (mPassword == null) {
				mPassword = sDEFAULTPASSWORD;
			}
			Map<String,Object> attrs = new HashMap<String, Object>();
            attrs.put(Provisioning.A_zimbraHideInGal, Provisioning.TRUE);
			account = prov.createAccount(mUsername, mPassword, attrs); 
		}
		return account;
	}
	
	private void initFolders(Account account, boolean useSoap, Domain dom) throws ServiceException {
		String grantee, name;
		if (dom == null) {
			grantee = "pub";
			name = ACL.GUID_PUBLIC;
		} else {
			grantee = "dom";
			name = dom.getName();
		}
		if (useSoap)
			try {
				initFolders(grantee, name);
				return;
			} catch (Exception e) {
				throw WikiServiceException.ERROR("initFolders", e);
			}
		Mailbox mbox = Mailbox.getMailboxByAccount(account);
		OperationContext octxt = new OperationContext(account);
		Folder template;
		try {
			template = mbox.getFolderByPath(octxt, sDEFAULTTEMPLATEFOLDER);
		} catch (ServiceException se) {
			template = mbox.createFolder(octxt, 
					sDEFAULTTEMPLATEFOLDER, 
					Mailbox.ID_FOLDER_USER_ROOT, 
					MailItem.getTypeForName(sDEFAULTVIEW), 
					null);
		}
        mbox.grantAccess(octxt, 
        		Mailbox.ID_FOLDER_NOTEBOOK, 
        		name, 
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
	
	public void initDefaultWiki() throws ServiceException {
		initDefaultWiki(false);
	}
	
	public void initDefaultWiki(boolean useSoap) throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Config globalConfig = prov.getConfig();
		String prevAcct = globalConfig.getAttr(Provisioning.A_zimbraNotebookAccount);
		if (mUsername == null && prevAcct != null)
			mUsername = prevAcct;
		if (prevAcct != null && !prevAcct.equals(mUsername)) {
			ZimbraLog.wiki.info("updating default account from " + prevAcct + " to " + mUsername);
		}
		
		Account acct = createWikiAccount();
		initFolders(acct, useSoap, null);
		
		HashMap<String,String> attrs = new HashMap<String,String>();
		attrs.put(Provisioning.A_zimbraNotebookAccount, mUsername);
		prov.modifyAttrs(globalConfig, attrs);
	}
	
	public void initDomainWiki(String domain) throws ServiceException {
		initDomainWiki(domain, false);
	}
	
	public void initDomainWiki(String domain, boolean useSoap) throws ServiceException {
		if (domain == null) {
			throw WikiServiceException.ERROR("invalid argument - empty domain");
		}
		Provisioning prov = Provisioning.getInstance();
		Domain dom = prov.get(DomainBy.name, domain);
		if (dom == null) {
			throw WikiServiceException.ERROR("invalid domain: " + domain);
		}
		initDomainWiki(dom, useSoap);
	}
	
	public void initDomainWiki(Domain dom) throws ServiceException {
		initDomainWiki(dom, false);
	}
	
	public void initDomainWiki(Domain dom, boolean useSoap) throws ServiceException {
		String prevAcct = dom.getAttr(Provisioning.A_zimbraNotebookAccount);
		if (prevAcct != null && !prevAcct.equals(mUsername)) {
			ZimbraLog.wiki.info("updating default account from " + prevAcct + " to " + mUsername);
		}
		
		Account acct = createWikiAccount();
		initFolders(acct, useSoap, dom);
		
		HashMap<String,String> attrs = new HashMap<String,String>();
		attrs.put(Provisioning.A_zimbraNotebookAccount, mUsername);
		Provisioning.getInstance().modifyAttrs(dom, attrs);
	}
	
	public WikiUtil(String user, String pass) throws ServiceException {
		this(null, user, pass);
	}
	
	public WikiUtil(String soapUrl, String user, String pass) throws ServiceException {

		mUrl = soapUrl;

		if (mUrl == null) {
			Server s = Provisioning.getInstance().getLocalServer();
			mUrl = URLUtil.getMailURL(s, ZimbraServlet.USER_SERVICE_URI, false);
			mUploadUrl = URLUtil.getMailURL(s, "/service/upload", false);
		} else {
			int end = mUrl.length() - 1;
			if (mUrl.charAt(end) == '/')
				end--;
			int index = mUrl.lastIndexOf('/', end);
			mUploadUrl = mUrl.substring(0, index) + "/upload";
		}
		
		mUsername = user;
		mPassword = pass;
	}
	
	private WikiUtil() {
	}
	
	public void setVerbose() {
		LmcSoapRequest.setDumpXML(true);
	}
	
	public static void main(String[] args) throws Exception {
        Zimbra.toolSetup();
        
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption("v", "verbose",  false, "verbose");
        options.addOption("s", "server",   true, "server URL");
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
        
        String url, username, password;
        String dir = cl.getOptionValue("d");
        if (cl.hasOption("v")) 
        	LmcSoapRequest.setDumpXML(true);
        url = cl.getOptionValue("s", null);
        username = cl.getOptionValue("u", WikiUtil.sUSERNAME);
        password = cl.getOptionValue("p", WikiUtil.sPASSWORD);
        WikiUtil prog = new WikiUtil(url, username, password);
        prog.initDefaultWiki(true);
        prog.startImport(new File(dir), true);
	}
}
