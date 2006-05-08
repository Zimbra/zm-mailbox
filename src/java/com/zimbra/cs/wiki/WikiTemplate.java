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
import com.zimbra.cs.util.ZimbraLog;

public class WikiTemplate {
	
	public WikiTemplate(WikiItem item) throws ServiceException {
		this(new String(item.getMessageContent()));
		touch();
	}
	
	public WikiTemplate(String item) {
		mTemplate = item;
		mTokens = new ArrayList<Token>();
		mDocument = null;
		mModifiedTime = 0;
	}
	
	public static WikiTemplate findTemplate(Context ctxt, String name)
	throws IOException,ServiceException {
    	WikiTemplateStore ts = WikiTemplateStore.getInstance(ctxt.item);
    	WikiTemplate template;
		if (name.startsWith("_")) {
			// XXX this is how templates are named.
			template = ts.getTemplate(ctxt.octxt, name);
		} else {
			template = ts.getTemplate(ctxt.octxt, name, false);
		}
		return template;
	}
	
	public String toString(OperationContext octxt, MailItem item)
	throws ServiceException, IOException {
		return toString(new Context(octxt, item));
	}
	
	public String toString(Context ctxt) throws ServiceException, IOException {
		if (!mParsed) {
			parse();
		}
		StringBuffer buf = new StringBuffer();
		for (Token tok : mTokens) {
			ctxt.token = tok;
			buf.append(apply(ctxt));
		}
		touch();
		return buf.toString();
	}
	
	public Token getToken(int i) {
		return mTokens.get(i);
	}

	public long getModifiedTime() {
		return mModifiedTime;
	}
	
	public void setDocument(String doc) {
		mDocument = doc;
		touch();
	}
	
	public String getDocument(OperationContext octxt, MailItem item, String chrome)
	throws ServiceException, IOException {
		return getDocument(new Context(octxt, item), chrome);
	}
	
	public String getDocument(Context ctxt, String chrome)
	throws ServiceException, IOException {
		WikiTemplateStore ts = WikiTemplateStore.getInstance(ctxt.item);
		WikiTemplate chromeTemplate = ts.getTemplate(ctxt.octxt, chrome);
		if (mDocument == null || chromeTemplate.isExpired(ctxt)) {
			ZimbraLog.wiki.debug("generating new document " + ctxt.item.getSubject());
			String doc = chromeTemplate.toString(ctxt);
			setDocument(doc);
		}
		return mDocument;
	}
	
	public boolean isExpired(Context ctxt) throws ServiceException, IOException {
		for (Token tok : mTokens) {
			if (tok.getType() == Token.TokenType.TEXT)
				continue;
			ctxt.token = tok;
			Wiklet w = findWiklet(tok);
			if (w != null && w.isExpired(this, ctxt)) {
				ZimbraLog.wiki.debug("failed validation " + ctxt.item.getSubject() + " because " + tok.getValue());
				return true;
			}
		}
		return false;
	}
	
	public void parse() {
		if (!mParsed)
			Token.parse(mTemplate, mTokens);
		mParsed = true;
	}
	
	private String apply(Context ctxt) throws ServiceException, IOException {
		if (ctxt.token.getType() == Token.TokenType.TEXT)
			return ctxt.token.getValue();
		Wiklet w = findWiklet(ctxt.token);
		if (w != null) {
			String ret = w.apply(ctxt);
			return ret;
		}
		return "";
	}
	
	private Wiklet findWiklet(Token tok) {
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
		return w;
	}
	
	private void touch() {
		mModifiedTime = System.currentTimeMillis();
	}
	
	private long    mModifiedTime;
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
		public enum TokenType { TEXT, WIKLET, WIKILINK }
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
		private String mData;
		
		public TokenType getType() {
			return mType;
		}
		
		public String getValue() {
			return mVal;
		}
		
		public String getData() {
			return mData;
		}
		
		public void setData(String str) {
			mData = str;
		}
	}
	
	public static class Context {
		public Context(OperationContext oc, MailItem it) {
			octxt = oc; item = it; content = null;
		}
		public OperationContext octxt;
		public MailItem item;
		public Token token;
		public String content;
	}
	public static abstract class Wiklet {
		public abstract String getName();
		public abstract String getPattern();
		public abstract String apply(Context ctxt) throws ServiceException,IOException;
		public abstract boolean isExpired(WikiTemplate template, Context ctxt) throws ServiceException,IOException;
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
					if (!s.equals(getPattern()))
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
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return true;  // always generate fresh TOC.
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
		public String generateList(Context ctxt, int style) throws ServiceException {
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
        	for (Folder f : subfolders) {
    	    	buf.append("<");
        		buf.append(sTAGS[sINNER][style]);
        		buf.append(" class='_pageLink'>");
        		buf.append(createLink(f.getName() + "/"));
        		buf.append("</");
        		buf.append(sTAGS[sINNER][style]);
        		buf.append(">");
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
		public String apply(Context ctxt) throws ServiceException {
			Map<String,String> params = parseParam(ctxt.token.getValue());
			String format = params.get(sFORMAT);
			if (format == null) {
				format = sLIST;
			}
			if (format.equals(sTEMPLATE)) {
				
			}
			
			return generateList(ctxt, format.equals(sSIMPLE) ? sTAGSIMPLE : sTAGLIST);
		}
	}
	public static class BreadcrumbsWiklet extends Wiklet {
		public static final String sPAGE = "page";
		public static final String sFORMAT = "format";
		public static final String sBODYTEMPLATE = "bodyTemplate";
		public static final String sITEMTEMPLATE = "itemTemplate";
		public static final String sSEPARATOR = "separator";
		
		public static final String sSIMPLE   = "simple";
		public static final String sTEMPLATE = "template";
		
		public static final String sDEFAULTBODYTEMPLATE = "_BREADCRUMB_BODY_TEMPLATE_";
		public static final String sDEFAULTITEMTEMPLATE = "_BREADCRUMB_ITEM_TEMPLATE_";
		
		private List<MailItem> mList;
		
		public String getName() {
			return "Breadcrumbs";
		}
		public String getPattern() {
			return "BREADCRUMBS";
		}
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		private Folder getFolder(Context ctxt, MailItem item) throws ServiceException {
			Mailbox mbox = item.getMailbox();
			return mbox.getFolderById(ctxt.octxt, item.getFolderId());
		}
		private void getBreadcrumbs(Context ctxt) throws ServiceException {
			mList = new ArrayList<MailItem>();
			mList.add(ctxt.item);
			Folder f = getFolder(ctxt, ctxt.item);
			while (f.getId() != 1) {
				mList.add(0, f);
				f = getFolder(ctxt, f);
			}
		}
		public String apply(Context ctxt) throws ServiceException, IOException {
			getBreadcrumbs(ctxt);
			Map<String,String> params = parseParam(ctxt.token.getValue());
			String format = params.get(sFORMAT);
			if (format == null || format.equals(sSIMPLE)) {
				StringBuffer buf = new StringBuffer();
				buf.append("<span class='_breadcrumbs_simple'>");
				for (MailItem item : mList) {
					String name;
					if (item instanceof Folder)
						name = ((Folder)item).getName();
					else
						name = item.getSubject();
					buf.append("<span class='_pageLink'>");
					buf.append("[[").append(name).append("]]");
					buf.append("</span>");
				}
				buf.append("</span>");
				return buf.toString();
			} else if (format.equals(sTEMPLATE)) {
				String bt = params.get(sBODYTEMPLATE);
				String it = params.get(sITEMTEMPLATE);
				if (bt == null)
					bt = sDEFAULTBODYTEMPLATE;
				if (it == null)
					it = sDEFAULTITEMTEMPLATE;
				return handleTemplates(ctxt, bt, it);
			} else {
				return reportError("format " + format + " not recognized");
			}
		}
		private String handleTemplates(Context ctxt,
										String bodyTemplate, 
										String itemTemplate)
		throws ServiceException, IOException {
			StringBuffer buf = new StringBuffer();
			for (MailItem item : mList) {
				WikiTemplate t = WikiTemplate.findTemplate(ctxt, itemTemplate);
				buf.append(t.toString(ctxt.octxt, item));
			}
			Context newCtxt = new Context(ctxt.octxt, ctxt.item);
			newCtxt.content = buf.toString();
			WikiTemplate body = WikiTemplate.findTemplate(newCtxt, bodyTemplate);

			return body.toString(newCtxt);
		}
	}
	public static class IconWiklet extends Wiklet {
		public String getName() {
			return "Icon";
		}
		public String getPattern() {
			return "ICON";
		}
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		public String apply(Context ctxt) {
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
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		public String apply(Context ctxt) {
			if (ctxt.item instanceof Document)
				return ((Document)ctxt.item).getFilename();
			else if (ctxt.item instanceof Folder)
				return ((Folder)ctxt.item).getName();
			else
				return ctxt.item.getSubject();
		}
	}
	public static class FragmentWiklet extends Wiklet {
		public String getName() {
			return "Fragment";
		}
		public String getPattern() {
			return "FRAGMENT";
		}
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		public String apply(Context ctxt) {
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
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		public String apply(Context ctxt) throws ServiceException {
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
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		public String apply(Context ctxt) throws ServiceException {
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
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		public String apply(Context ctxt) throws ServiceException {
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
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		public String apply(Context ctxt) throws ServiceException {
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
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		public String apply(Context ctxt) throws ServiceException {
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
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		public String apply(Context ctxt) throws ServiceException {
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
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		public String apply(Context ctxt) {
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
		public boolean isExpired(WikiTemplate template, Context ctxt) throws ServiceException, IOException {
			if (ctxt.content == null) {
				WikiItem wiki = (WikiItem) ctxt.item;
				WikiTemplate t = WikiTemplate.findTemplate(ctxt, wiki.getWikiWord());
				return t.isExpired(ctxt);
			}
			return false;
		}
		public String apply(Context ctxt) throws ServiceException, IOException {
			if (ctxt.content != null)
				return ctxt.content;
			WikiItem wiki = (WikiItem) ctxt.item;
			WikiTemplate template = WikiTemplate.findTemplate(ctxt, wiki.getWikiWord());
			return template.toString(ctxt);
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
		public boolean isExpired(WikiTemplate template, Context ctxt) throws ServiceException, IOException {
			Map<String,String> params = parseParam(ctxt.token.getValue());
			String page = params.get(sPAGE);
			if (page == null) {
				page = params.keySet().iterator().next();
			}
			WikiTemplate includedTemplate = WikiTemplate.findTemplate(ctxt, page);
			return includedTemplate.getModifiedTime() > template.getModifiedTime();
		}
		public String apply(Context ctxt) throws ServiceException, IOException {
			Map<String,String> params = parseParam(ctxt.token.getValue());
			String page = params.get(sPAGE);
			if (page == null) {
				page = params.keySet().iterator().next();
			}
			//ZimbraLog.wiki.info("including " + page);
			WikiTemplate template = WikiTemplate.findTemplate(ctxt, page);
			return template.toString(ctxt);
		}
	}
	public static class WikilinkWiklet extends Wiklet {
		public String getName() {
			return "Wikilink";
		}
		public String getPattern() {
			return "WIKILINK";
		}
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		public String apply(Context ctxt) {
			String text = ctxt.token.getValue();
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
