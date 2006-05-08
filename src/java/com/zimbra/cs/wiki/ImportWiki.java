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
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.cs.client.*;
import com.zimbra.cs.client.soap.*;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapParseException;

public class ImportWiki {
	
	private static final String sURL = "http://localhost:7070/service/soap";
	
	// XXX this is for the default Chrome hack.  should use Notebook folder id 12.
	private static final String sFOLDERID = "1";
	private static final String sUSERNAME = "user1";
	private static final String sPASSWORD = "test123";
	
	private LmcSession mSession;
	
	private String mUrl;
	private String mUploadUrl;
	private String mUsername;
	private String mPassword;
	
	private void auth() throws LmcSoapClientException, IOException, 
			SoapFaultException, ServiceException, SoapParseException {
		LmcAuthRequest auth = new LmcAuthRequest();
		auth.setUsername(mUsername);
		auth.setPassword(mPassword);
		LmcAuthResponse resp = (LmcAuthResponse) auth.invoke(mUrl);
		mSession = resp.getSession();
	}
	
	private LmcFolder findFolder(LmcFolder root, File dir) {
		if (root == null)
			return null;
		LmcFolder[] list = root.getSubFolders();
		if (list == null)
			return null;
		for (int i = 0; i < list.length; i++) {
			if (list[i].getName().equals(dir.getName())) {
				return list[i];
			}
		}
		return null;
	}

	private LmcFolder createFolder(LmcFolder parent, File dir) throws LmcSoapClientException, IOException, 
			SoapFaultException, ServiceException, SoapParseException {
		System.out.println("Creating folder " + dir.getName());
		LmcCreateFolderRequest req = new LmcCreateFolderRequest();
		req.setSession(mSession);
		req.setName(dir.getName());
		req.setParentID(parent.getFolderID());
		req.setName(dir.getName());
		req.setView("wiki");
		LmcCreateFolderResponse resp = (LmcCreateFolderResponse) req.invoke(mUrl);
		return resp.getFolder();
	}
	
	private boolean purgeFolder(LmcFolder folder) throws LmcSoapClientException, IOException, 
			SoapFaultException, ServiceException, SoapParseException {
		if (folder.getView() == null ||
			!folder.getView().equals("wiki") ||
			folder.getName().equals("Notebook")) {
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
        		LmcFolder sub = findFolder(where, f);
        		if (sub == null)
        			sub = createFolder(where, f);
        		populateFolders(sub, f);
        	} else {
        		createItem(where, f);
        	}
        }
	}
	
	public void startImport(File top) throws LmcSoapClientException, IOException, 
	SoapFaultException, ServiceException, SoapParseException {
		System.out.println("Initializing...");
		auth();

		// get current folder hierarchy.
        LmcGetFolderRequest req = new LmcGetFolderRequest();
        req.setSession(mSession);
        req.setFolderToGet(sFOLDERID);
        LmcGetFolderResponse resp = (LmcGetFolderResponse) req.invoke(mUrl);
        LmcFolder root = resp.getRootFolder();

        // delete all the items in wiki folders and then rmdir all wiki folders.
        deleteAllItemsInFolder(root, "");

		// get new folder hierarchy.
        req = new LmcGetFolderRequest();
        req.setSession(mSession);
        req.setFolderToGet(sFOLDERID);
        resp = (LmcGetFolderResponse) req.invoke(mUrl);
        root = resp.getRootFolder();

        // start populating directories and files.
        populateFolders(root, top);
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
        
        String dir = cl.getOptionValue("d");
        ImportWiki prog = new ImportWiki();
        if (cl.hasOption("v")) 
        	LmcSoapRequest.setDumpXML(true);
        prog.mUrl = cl.getOptionValue("s", ImportWiki.sURL);
        prog.mUploadUrl = prog.mUrl.substring(0, prog.mUrl.length() - 4) + "upload";
        prog.mUsername = cl.getOptionValue("u", ImportWiki.sUSERNAME);
        prog.mPassword = cl.getOptionValue("p", ImportWiki.sPASSWORD);
        prog.startImport(new File(dir));
	}

}
