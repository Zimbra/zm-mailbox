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
import java.text.DateFormat;
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
import com.zimbra.cs.service.wiki.WikiServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.ZimbraLog;

public class WikiTemplate {
	
	public WikiTemplate(WikiItem item) throws IOException,ServiceException {
		this(new String(ByteUtil.getContent(item.getRawMessage(), 0)));
	}
	
	public WikiTemplate(String item) {
		mTemplate = item;
		mTokens = new ArrayList<Token>();
	}
	
	public String toString(OperationContext octxt, MailItem item) throws ServiceException, IOException {
		return toString(new Context(octxt, item));
	}
	
	public String toString(Context ctxt) throws ServiceException, IOException {
		if (!mParsed) {
			parse(ctxt);
		}
		return mDocument;
	}
	
	public Token getToken(int i) {
		return mTokens.get(i);
	}
	
	private void parse(Context ctxt) throws ServiceException, IOException {
		Token.parse(mTemplate, mTokens);
		StringBuffer buf = new StringBuffer();
		for (Token tok : mTokens) {
			buf.append(apply(ctxt, tok));
		}
		mDocument = buf.toString();
		mParsed = true;
	}
	
	private String apply(Context ctxt, Token tok) throws ServiceException, IOException {
		if (tok.getType() == Token.TokenType.TEXT)
			return tok.getValue();
		Wiklet w;
		String tokenStr = tok.getValue();
		if (tok.getType() == Token.TokenType.WIKILINK) {
			w = sWIKLETS.get("WIKILINK");
		} else {
			String firstTok;
			int index = tokenStr.indexOf(' ');
			if (index != -1) {
				firstTok = tokenStr.substring(0, index);
			} else {
				firstTok = tokenStr;
			}
			w = sWIKLETS.get(firstTok);
		}
		if (w != null) {
			//long t0 = System.currentTimeMillis();
			String ret = w.apply(ctxt, tokenStr);
			//long t1 = System.currentTimeMillis() - t0;
	        //com.zimbra.cs.util.ZimbraLog.wiki.info("Applying Wiklet " + w.getName() + " : " + t1 + "ms");
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
		addWiklet(new CreatorWiklet());
		addWiklet(new ModifierWiklet());
		addWiklet(new CreateDateWiklet());
		addWiklet(new CreateTimeWiklet());
		addWiklet(new ModifyDateWiklet());
		addWiklet(new ModifyTimeWiklet());
		addWiklet(new VersionWiklet());
		addWiklet(new ContentWiklet());
		addWiklet(new IncludeWiklet());
		addWiklet(new WikilinkWiklet());
	}
	
	private static void addWiklet(Wiklet w) {
		sWIKLETS.put(w.getPattern(), w);
	}
	
	public static class Token {
		public enum TokenType { TEXT, WIKLET, WIKILINK };
		public static void parse(String str, List<Token> tokens) throws IllegalArgumentException {
			int end = 0;
			if (str.startsWith("{{")) {
				end = str.indexOf("}}");
				if (end == -1)
					throw new IllegalArgumentException("parse error");
				tokens.add(new Token(str.substring(2, end), TokenType.WIKLET));
				end += 2;
			} else if (str.startsWith("[[")) {
				end = str.indexOf("]]");
				if (end == -1)
					throw new IllegalArgumentException("parse error");
				tokens.add(new Token(str.substring(2, end), TokenType.WIKILINK));
				end += 2;
			} else {
				int lastPos = str.length() - 1;
				while (end < lastPos) {
					if (str.charAt(end) == '{' && str.charAt(end+1) == '{' ||
						str.charAt(end) == '[' && str.charAt(end+1) == '[') {
						break;
					}
					end++;
				}
				if (end == lastPos)
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
	public static abstract class Wiklet {
		public abstract String getName();
		public abstract String getPattern();
		public abstract String apply(Context ctxt, String text) throws ServiceException,IOException;
		public Map<String,String> parseParam(String text) {
			Map<String,String> map = new HashMap<String,String>();
			int start = 0;
			int end = text.indexOf(' ');
			while (true) {
				int equal = text.indexOf('=', start);
				if (end == -1)
					end = text.length();
				if (equal == -1 || equal > end) {
					String s = text.substring(start,end);
					map.put(s, s);
				} else {
					String val;
					if (text.charAt(equal+1) == '"' && text.charAt(end-1) == '"')
						val = text.substring(equal+2,end-1);
					else
						val = text.substring(equal+1,end);
					map.put(text.substring(start,equal), val);
				}
				if (end == text.length())
					break;
				start = end + 1;
				end = text.indexOf(' ', start);
			}
			return map;
		}
		public String reportError(String errorMsg) {
			String msg = "Error handling wiklet " + getName() + ": " + errorMsg;
			ZimbraLog.wiki.error(msg);
			return msg;
		}
	}
	public static class TocWiklet extends Wiklet {
		public static final String sFORMAT = "format";
		public static final String sBODYTEMPLATE = "bodyTemplate";
		public static final String sITEMTEMPLATE = "itemTemplate";

		public static final String sSIMPLE   = "simple";
		public static final String sLIST     = "list";
		public static final String sTEMPLATE = "template";

		public static final String[][] sTAGS =
		{
			{ "_toc_list", "_toc_simple" },
			{ "ul",        "span" },
			{ "li",        "span" }
		};
		
		public static final int sTAGLIST   = 0;
		public static final int sTAGSIMPLE = 1;
		
		public static final int sCLASS = 0;
		public static final int sOUTER = 1;
		public static final int sINNER = 2;
		
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
		public String generateList(Context ctxt, String text, int style) throws ServiceException {
			Folder folder;
			if (ctxt.item instanceof Folder)
				folder = (Folder) ctxt.item;
			else
				folder = ctxt.item.getMailbox().getFolderById(ctxt.octxt, ctxt.item.getFolderId());
	    	StringBuffer buf = new StringBuffer();
	    	buf.append("<");
	    	buf.append(sTAGS[sOUTER][style]);
	    	buf.append(" class='");
	    	buf.append(sTAGS[sCLASS][style]);
	    	buf.append("'>");
	    	List<Folder> subfolders = folder.getSubfolders(ctxt.octxt);
	    	if (subfolders != null) {
	        	for (Folder f : subfolders) {
	    	    	buf.append("<");
	        		buf.append(sTAGS[sINNER][style]);
	        		buf.append(" class='_pageLink'>");
	        		buf.append(createLink(f.getName() + "/"));
	        		buf.append("</");
	        		buf.append(sTAGS[sINNER][style]);
	        		buf.append(">");
	        	}
	    	}
	    	Mailbox mbox = ctxt.item.getMailbox();
            for (Document doc : mbox.getWikiList(ctxt.octxt, folder.getId())) {
            	if (shouldSkipThis(doc))
            		continue;
            	buf.append("<");
        		buf.append(sTAGS[sINNER][style]);
            	buf.append(" class='_pageLink'>");
            	String name;
            	if (doc instanceof WikiItem)
            		name = ((WikiItem)doc).getWikiWord();
            	else
            		name = doc.getFilename();
            	buf.append(createLink(name));
        		buf.append("</");
        		buf.append(sTAGS[sINNER][style]);
        		buf.append(">");
            }

    		buf.append("</");
    		buf.append(sTAGS[sOUTER][style]);
    		buf.append(">");
	        return buf.toString();
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			Map<String,String> params = parseParam(text);
			String format = params.get(sFORMAT);
			if (format == null) {
				format = sLIST;
			}
			if (format.equals(sTEMPLATE)) {
				
			}
			
			return generateList(ctxt, text, format.equals(sSIMPLE) ? sTAGSIMPLE : sTAGLIST);
		}
	}
	public static class BreadcrumbsWiklet extends Wiklet {
		public String getName() {
			return "Breadcrumbs";
		}
		public String getPattern() {
			return "BREADCRUMBS";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			return "Breakcrumbs here.";
		}
	}
	public static class IconWiklet extends Wiklet {
		public String getName() {
			return "Icon";
		}
		public String getPattern() {
			return "ICON";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			return "<div class='ImgNotebook_pageIcon'></div>";
		}
	}
	public static class NameWiklet extends Wiklet {
		public String getName() {
			return "Name";
		}
		public String getPattern() {
			return "NAME";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			Document doc = (Document) ctxt.item;
			return doc.getFilename();
		}
	}
	public static class FragmentWiklet extends Wiklet {
		public String getName() {
			return "Fragment";
		}
		public String getPattern() {
			return "FRAGMENT";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			Document doc = (Document) ctxt.item;
			return doc.getFragment();
		}
	}
	public static class CreatorWiklet extends Wiklet {
		public String getName() {
			return "Creator";
		}
		public String getPattern() {
			return "Creator";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			Document doc = (Document) ctxt.item;
			return doc.getRevision(1).getCreator();
		}
	}
	public static class ModifierWiklet extends Wiklet {
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
	public static class CreateDateWiklet extends Wiklet {
		public String getName() {
			return "Create Date";
		}
		public String getPattern() {
			return "CREATEDATE";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			Document doc = (Document) ctxt.item;
			Date modifyDate = new Date(doc.getRevision(1).getRevDate());
			return DateFormat.getDateInstance(DateFormat.MEDIUM).format(modifyDate);
		}
	}
	public static class CreateTimeWiklet extends Wiklet {
		public String getName() {
			return "Create Time";
		}
		public String getPattern() {
			return "CREATETIME";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			Document doc = (Document) ctxt.item;
			Date modifyDate = new Date(doc.getRevision(1).getRevDate());
			return DateFormat.getTimeInstance(DateFormat.MEDIUM).format(modifyDate);
		}
	}
	public static class ModifyDateWiklet extends Wiklet {
		public String getName() {
			return "Modified Date";
		}
		public String getPattern() {
			return "MODIFYDATE";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			Document doc = (Document) ctxt.item;
			Date modifyDate = new Date(doc.getLastRevision().getRevDate());
			return DateFormat.getDateInstance(DateFormat.MEDIUM).format(modifyDate);
		}
	}
	public static class ModifyTimeWiklet extends Wiklet {
		public String getName() {
			return "Modified Time";
		}
		public String getPattern() {
			return "MODIFYTIME";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			Document doc = (Document) ctxt.item;
			Date modifyDate = new Date(doc.getLastRevision().getRevDate());
			return DateFormat.getTimeInstance(DateFormat.MEDIUM).format(modifyDate);
		}
	}
	public static class VersionWiklet extends Wiklet {
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
	public static class ContentWiklet extends Wiklet {
		public String getName() {
			return "Content";
		}
		public String getPattern() {
			return "CONTENT";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			Document doc = (Document) ctxt.item;
			try {
				return new String(ByteUtil.getContent(doc.getRawDocument(), 0), "UTF-8");
			} catch (IOException ioe) {
				throw WikiServiceException.CANNOT_READ(doc.getSubject());
			}
		}
	}
	public static class IncludeWiklet extends Wiklet {
		public static final String sPAGE = "page";
		
		public String getName() {
			return "Include";
		}
		public String getPattern() {
			return "INCLUDE";
		}
		public String getPage(Context ctxt, String page) throws ServiceException, IOException {
			WikiTemplate template;
			if (page.startsWith("_")) {
				// XXX this is how templates are named.
		    	WikiTemplateStore wiki = WikiTemplateStore.getInstance(ctxt.item);
		    	template = wiki.getTemplate(ctxt.octxt, page);
			} else {
				Wiki wiki = Wiki.getInstance(ctxt.item.getMailbox().getAccount().getName(), ctxt.item.getFolderId());
				WikiWord ww = wiki.lookupWiki(page);
				if (ww == null) {
					return reportError("cannot find page " + page);
				}
				Document doc = ww.getWikiItem(ctxt.octxt);
				if (!(doc instanceof WikiItem)) {
					return reportError("included page \"" + page + "\" is not of type Wiki");
				}
				template = new WikiTemplate((WikiItem)doc);
			}
			
	    	return template.toString(ctxt);
		}
		public String apply(Context ctxt, String text) throws ServiceException, IOException {
			Map<String,String> params = parseParam(text);
			String page = params.get(sPAGE);
			if (page == null) {
				return reportError("missing name attribute");
			}
			return getPage(ctxt, page);
		}
	}
	public static class WikilinkWiklet extends Wiklet {
		public String getName() {
			return "Wikilink";
		}
		public String getPattern() {
			return "WIKILINK";
		}
		public String apply(Context ctxt, String text) throws ServiceException {
			String link = text;
			String title = text;
			int pos = text.indexOf('|');
			if (pos != -1) {
				link = text.substring(0, pos);
				title = text.substring(pos+1);
			} else {
				pos = text.indexOf("][");
				if (pos != -1) {
					link = text.substring(0, pos);
					title = text.substring(pos+2);
				}
			}
			StringBuffer buf = new StringBuffer();
			buf.append("<a href='").append(link).append("'>").append(title).append("</a>");
			return buf.toString();
		}
	}
}
