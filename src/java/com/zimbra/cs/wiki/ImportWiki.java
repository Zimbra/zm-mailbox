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
package com.zimbra.cs.wiki;

import java.io.File;
import java.io.IOException;
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
	
	private void createTree(LmcFolder parent, File dir) throws LmcSoapClientException, IOException, 
			SoapFaultException, ServiceException, SoapParseException {
		if (!dir.isDirectory()) {
			return;
		}
		File[] list = dir.listFiles();
		for (int i = 0; i < list.length; i++) {
			File f = list[i];
			if (!f.isDirectory()) {
				continue;
			}
			LmcFolder current = findFolder(parent, f);
			if (current == null) {
				System.out.println("Creating folder " + f.getName());
				LmcCreateFolderRequest req = new LmcCreateFolderRequest();
				req.setSession(mSession);
				req.setName(f.getName());
				req.setParentID(parent.getFolderID());
				LmcCreateFolderResponse resp = (LmcCreateFolderResponse) req.invoke(mUrl);
				current = resp.getFolder();
			}
			createTree(current, f);
		}
	}
	
	private void initFolders(File file) throws LmcSoapClientException, IOException, 
			SoapFaultException, ServiceException, SoapParseException {
        LmcGetFolderRequest req = new LmcGetFolderRequest();
        req.setSession(mSession);
        req.setFolderToGet(sFOLDERID);
        LmcGetFolderResponse resp = (LmcGetFolderResponse) req.invoke(mUrl);
        LmcFolder root = resp.getRootFolder();
		createTree(root, file);
	}

	private void deleteAllItemsInFolder(LmcFolder folder, String parent) throws LmcSoapClientException, IOException, 
			SoapFaultException, ServiceException, SoapParseException {
		String folderName = parent;
		if (folderName.length() == 0)
			folderName = "/";
		else if (folderName.equals("/"))
			folderName += folder.getName();
		else
			folderName += "/" + folder.getName();
		LmcSearchRequest req = new LmcSearchRequest();
		req.setSession(mSession);
		req.setQuery("in:"+folderName);
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
			deleteAllItemsInFolder(list[i], folderName);
		}
	}
	
	private void createItem(LmcFolder where, File what) throws LmcSoapClientException, IOException, 
	SoapFaultException, ServiceException, SoapParseException {
		System.out.println("Creating item " + what.getName() + " in folder " + where.getName());
		LmcWiki wiki = new LmcWiki();
		wiki.setWikiWord(what.getName());
		wiki.setFolder(where.getFolderID());
		wiki.setContents(new String(ByteUtil.getContent(what), "utf-8"));
		
		LmcSaveWikiRequest req = new LmcSaveWikiRequest();
		req.setSession(mSession);
		req.setWiki(wiki);
		req.invoke(mUrl);
	}
	
	private void populateFolders(LmcFolder where, File file) throws LmcSoapClientException, IOException, 
			SoapFaultException, ServiceException, SoapParseException {
		if (where == null) {
			throw new IllegalArgumentException("null folder");
		}
        File[] list = file.listFiles();
        for (int i = 0; i < list.length; i++) {
        	File f = list[i];
        	if (f.isDirectory()) {
        		populateFolders(findFolder(where, f), f);
        	} else {
        		createItem(where, f);
        	}
        }
	}
	
	public void startImport(File top) throws LmcSoapClientException, IOException, 
	SoapFaultException, ServiceException, SoapParseException {
		System.out.println("Initializing...");
		auth();
        initFolders(top);
        
        LmcGetFolderRequest req = new LmcGetFolderRequest();
        req.setSession(mSession);
        req.setFolderToGet(sFOLDERID);
        LmcGetFolderResponse resp = (LmcGetFolderResponse) req.invoke(mUrl);
        LmcFolder root = resp.getRootFolder();

        deleteAllItemsInFolder(root, "");
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
        prog.mUsername = cl.getOptionValue("u", ImportWiki.sUSERNAME);
        prog.mPassword = cl.getOptionValue("p", ImportWiki.sPASSWORD);
        prog.startImport(new File(dir));
	}

}
