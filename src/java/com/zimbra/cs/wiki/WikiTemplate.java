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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.wiki.WikiTemplate.Token.TokenType;

public class WikiTemplate {
	
	public WikiTemplate(WikiItem item) throws IOException,ServiceException {
		this(new String(ByteUtil.getContent(item.getRawMessage(), 0)));
	}
	
	public WikiTemplate(String item) {
		mTemplate = item;
		mTokens = new ArrayList<Token>();
	}
	
	public String toString(OperationContext octxt, MailItem item) throws ServiceException {
		Context ctxt = new Context(octxt, item);
		if (!mParsed) {
			parse(ctxt);
		}
		return mDocument;
	}
	
	public Token getToken(int i) {
		return mTokens.get(i);
	}
	
	private void parse(Context ctxt) throws ServiceException {
		Token.parse(mTemplate, mTokens);
		StringBuffer buf = new StringBuffer();
		for (Token tok : mTokens) {
			buf.append(apply(ctxt, tok));
		}
		mDocument = buf.toString();
		mParsed = true;
	}
	
	private String apply(Context ctxt, Token tok) throws ServiceException {
		if (tok.getType() == TokenType.TEXT)
			return tok.getValue();
		String tokenStr = tok.getValue();
		String firstTok;
		int index = tokenStr.indexOf(' ');
		if (index != -1) {
			firstTok = tokenStr.substring(0, index);
		} else {
			firstTok = tokenStr;
		}
		Wiklet w = sWIKLETS.get(firstTok);
		if (w != null) {
			String ret = w.apply(ctxt, tokenStr);
			List<Token> tokens = new ArrayList<Token>();
			Token.parse(ret, tokens);
			StringBuffer buf = new StringBuffer();
			for (Token t : tokens) {
				buf.append(apply(ctxt, t));
			}
			return buf.toString();
		}
		return "";
	}
	
	private boolean mParsed;
	
	private List<Token> mTokens;
	private String mTemplate;
	private String mDocument;
	
	private static Map<String,Wiklet> sWIKLETS;
	
	static {
		sWIKLETS = new HashMap<String,Wiklet>();
		addWiklet(new TocWiklet());
		addWiklet(new BreadcrumbsWiklet());
		addWiklet(new IconWiklet());
		addWiklet(new NameWiklet());
		addWiklet(new ModifierWiklet());
		addWiklet(new ModifyDateWiklet());
		addWiklet(new VersionWiklet());
	}
	
	private static void addWiklet(Wiklet w) {
		sWIKLETS.put(w.getPattern(), w);
	}
	
	public static class Token {
		public enum TokenType { TEXT, WIKLET };
		public static void parse(String str, List<Token> tokens) throws IllegalArgumentException {
			int end;
			if (str.startsWith("{{")) {
				end = str.indexOf("}}");
				if (end == -1)
					throw new IllegalArgumentException("parse error");
				tokens.add(new Token(str.substring(2, end), TokenType.WIKLET));
				end += 2;
			} else {
				end = str.indexOf("{{");
				if (end == -1)
					end = str.length();
				tokens.add(new Token(str.substring(0, end), TokenType.TEXT));
			}
			if (end != -1 && end != str.length()) {
				Token.parse(str.substring(end), tokens);
			}
		}
		
		public Token(String text, TokenType type) {
			mVal = text;
			mType = type;
		}
		
		private TokenType mType;
		private String mVal;
		
		public TokenType getType() {
			return mType;
		}
		
		public String getValue() {
			return mVal;
		}
	}
	
	public static class Context {
		public Context(OperationContext oc, MailItem it) {
			octxt = oc; item = it;
		}
		public OperationContext octxt;
		public MailItem item;
		
	}
	public static interface Wiklet {
		public String getName();
		public String getPattern();
		public String apply(Context ctxt, String text) throws ServiceException;
	}
	public static class TocWiklet implements Wiklet {
		public static String format = "format";
		public static String bodyTemplate = "bodyTemplate";
		public static String itemTemplate = "itemTemplate";
		
		public String getName() {
			return "Table of contents";
		}
		public String getPattern() {
			return "TOC";
		}
	    private static final String TOC = "_INDEX_";
	    private boolean shouldSkipThis(Document doc) {
	    	// XXX skip the non visible items.
    		if (doc.getFilename().equals(TOC))
    			return true;
	    	return false;
	    }
	    private String createLink(String name) {
	    	StringBuffer buf = new StringBuffer();
	    	buf.append("<a href=\"");
	    	buf.append(name);
	    	buf.append("\">");
	    	buf.append(name);
	    	buf.append("</a>");
	    	return buf.toString();
	    }
		public String apply(Context ctxt, String text) throws ServiceException {
			Folder folder = (Folder) ctxt.item;
	    	StringBuffer buf = new StringBuffer();
	    	buf.append("<ul>");
	    	List<Folder> subfolders = folder.getSubfolders(ctxt.octxt);
	    	if (subfolders != null) {
	        	for (Folder f : subfolders) {
	        		buf.append("<li>");
	        		buf.append(createLink(f.getName() + "/"));
	        		buf.append("</li>");
	        	}
	    	}
	    	Mailbox mbox = ctxt.item.getMailbox();
            for (Document doc : mbox.getWikiList(ctxt.octxt, folder.getId())) {
            	if (shouldSkipThis(doc))
            		continue;
            	buf.append("<li>");
            	String name;
            	if (doc instanceof WikiItem)
            		name = ((WikiItem)doc).getWikiWord();
            	else
            		name = doc.getFilename();
            	buf.append(createLink(name));
            	buf.append("</li>");
            }

	    	buf.append("</ul>");
	        return buf.toString();
		}
	}
	public static class BreadcrumbsWiklet implements Wiklet {
		public String getName() {
			return "Breadcrumbs";
		}
		public String getPattern() {
			return "BREADCRUMBS";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			return "";
		}
	}
	public static class IconWiklet implements Wiklet {
		public String getName() {
			return "Icon";
		}
		public String getPattern() {
			return "ICON";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			return "";
		}
	}
	public static class NameWiklet implements Wiklet {
		public String getName() {
			return "name";
		}
		public String getPattern() {
			return "NAME";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			Document doc = (Document) ctxt.item;
			return doc.getFilename();
		}
	}
	public static class ModifierWiklet implements Wiklet {
		public String getName() {
			return "Modifier";
		}
		public String getPattern() {
			return "MODIFIER";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			Document doc = (Document) ctxt.item;
			return doc.getLastRevision().getCreator();
		}
	}
	public static class ModifyDateWiklet implements Wiklet {
		public String getName() {
			return "Modified Date";
		}
		public String getPattern() {
			return "MODIFYDATE";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			Document doc = (Document) ctxt.item;
			return new Date(doc.getLastRevision().getRevDate()).toString();
		}
	}
	public static class VersionWiklet implements Wiklet {
		public String getName() {
			return "Version";
		}
		public String getPattern() {
			return "VERSION";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			Document doc = (Document) ctxt.item;
			return Integer.toString(doc.getVersion());
		}
	}
}
