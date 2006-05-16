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

import javax.servlet.http.HttpServletRequest;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.cs.wiki.Wiki.WikiUrl;

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
    	return ts.getTemplate(ctxt.octxt, name);
	}
	
	public String toString(OperationContext octxt, HttpServletRequest req, MailItem item)
	throws ServiceException, IOException {
		return toString(new Context(octxt, req, item));
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
	
	public String getDocument(OperationContext octxt, HttpServletRequest req, MailItem item, String chrome)
	throws ServiceException, IOException {
		return getDocument(new Context(octxt, req, item), chrome);
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
			Wiklet w = Wiklet.get(tok);
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
		Wiklet w = Wiklet.get(ctxt.token);
		if (w != null) {
			String ret = w.apply(ctxt);
			return ret;
		}
		return "";
	}
	
	private void touch() {
		mModifiedTime = System.currentTimeMillis();
	}
	
	private long    mModifiedTime;
	private boolean mParsed;
	
	private List<Token> mTokens;
	private String mTemplate;
	private String mDocument;
	
	public static class Token {
		public static final String sWIKLETTAG = "wiklet";
		public static final String sCLASSATTR = "class";
		
		public enum TokenType { TEXT, WIKLET, WIKILINK }
		public static void parse(String str, List<Token> tokens) throws IllegalArgumentException {
			Token.parse(str, 0, tokens);
		}
		public static void parse(String str, int pos, List<Token> tokens) throws IllegalArgumentException {
			int end = pos;
			if (str.startsWith("{{", pos)) {
				end = str.indexOf("}}", pos);
				if (end == -1)
					throw new IllegalArgumentException("parse error: unmatched {{");
				tokens.add(new Token(str.substring(pos+2, end), TokenType.WIKLET));
				end += 2;
			} else if (str.startsWith("[[", pos)) {
				end = str.indexOf("]]", pos);
				if (end == -1)
					throw new IllegalArgumentException("parse error: unmatched [[");
				tokens.add(new Token(str.substring(pos+2, end), TokenType.WIKILINK));
				end += 2;
			} else if (str.startsWith("<wiklet", pos)) {
				int padding = 2;
				end = str.indexOf(">", pos);
				if (str.charAt(end-1) == '/') {
					end = end - 1;
				} else {
					int endSection = str.indexOf("</wiklet>", end);
					padding = endSection - end + 9;
				}
				if (end == -1)
					throw new IllegalArgumentException("parse error: unmatched <wiklet");
				tokens.add(new Token(str.substring(pos+1, end), TokenType.WIKLET));
				end += padding;
			} else {
				int lastPos = str.length() - 1;
				while (end < lastPos) {
					if (str.startsWith("{{", end) ||
						str.startsWith("[[", end) ||
						str.startsWith("<wiklet", end)) {
						break;
					}
					end++;
				}
				if (end == lastPos)
					end = str.length();
				tokens.add(new Token(str.substring(pos, end), TokenType.TEXT));
			}
			if (end == -1 || end == str.length())
				return;
			
			Token.parse(str, end, tokens);
		}
		
		public Token(String text, TokenType type) {
			mVal = text;
			mType = type;
		}
		
		private TokenType mType;
		private String mVal;
		private String mData;
		private Map<String,String> mParams;
		
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
		
		public Map<String,String> parseParam() {
			return parseParam(mVal);
		}
		
		public Map<String,String> parseParam(String text) {
			if (mParams != null)
				return mParams;
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
					else if (text.charAt(equal+1) == '\'' && text.charAt(end-1) == '\'')
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
			mParams = map;
			return map;
		}
	}
	
	public static class Context {
		public Context(OperationContext oc, MailItem it) {
			octxt = oc; item = it; content = null;
		}
		public Context(OperationContext oc, HttpServletRequest request, MailItem it) {
			octxt = oc; req = request; item = it; content = null;
		}
		public OperationContext octxt;
		public HttpServletRequest req;
		public MailItem item;
		public Token token;
		public String content;
	}
	public static abstract class Wiklet {
		public abstract String getName();
		public abstract String getPattern();
		public abstract String apply(Context ctxt) throws ServiceException,IOException;
		public abstract boolean isExpired(WikiTemplate template, Context ctxt) throws ServiceException,IOException;
		public String reportError(String errorMsg) {
			String msg = "Error handling wiklet " + getName() + ": " + errorMsg;
			ZimbraLog.wiki.error(msg);
			return msg;
		}
		protected String handleTemplates(Context ctxt,
											List<MailItem> list,
											String bodyTemplate, 
											String itemTemplate)
		throws ServiceException, IOException {
			StringBuffer buf = new StringBuffer();
			for (MailItem item : list) {
				WikiTemplate t = WikiTemplate.findTemplate(ctxt, itemTemplate);
				buf.append(t.toString(ctxt.octxt, ctxt.req, item));
			}
			Context newCtxt = new Context(ctxt.octxt, ctxt.item);
			newCtxt.content = buf.toString();
			WikiTemplate body = WikiTemplate.findTemplate(newCtxt, bodyTemplate);

			return body.toString(newCtxt);
		}
		
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
			addWiklet(new InlineWiklet());
			addWiklet(new WikilinkWiklet());
		}
		
		private static void addWiklet(Wiklet w) {
			sWIKLETS.put(w.getPattern(), w);
		}
		public static Wiklet get(Token tok) {
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
				if (firstTok.equals(Token.sWIKLETTAG)) {
					Map<String,String> params = tok.parseParam();
					String cls = params.get(Token.sCLASSATTR);
					if (cls == null) {
						// this is really a parse error.
						return null;
					}
					if (cls.equals("link"))
						w = sWIKLETS.get("WIKILINK");
					else
						w = sWIKLETS.get(cls.toUpperCase());
				} else {
					w = sWIKLETS.get(firstTok);
				}
			}
			return w;
		}
		
		public static Wiklet get(String name) {
			return sWIKLETS.get(name);
		}
	}
	public static class TocWiklet extends Wiklet {
		public static final String sFORMAT = "format";
		public static final String sBODYTEMPLATE = "bodyTemplate";
		public static final String sITEMTEMPLATE = "itemTemplate";

		public static final String sDEFAULTBODYTEMPLATE = "_TocBodyTemplate";
		public static final String sDEFAULTITEMTEMPLATE = "_TocItemTemplate";
		
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
	    private boolean shouldSkipThis(Document doc) {
	    	// XXX skip the non visible items.
    		if (doc.getFilename().startsWith("_"))
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
		public String applyTemplates(Context ctxt, Map<String,String> params) throws ServiceException, IOException {
			List<MailItem> list = new ArrayList<MailItem>();
			Folder folder;
			if (ctxt.item instanceof Folder)
				folder = (Folder) ctxt.item;
			else
				folder = ctxt.item.getMailbox().getFolderById(ctxt.octxt, ctxt.item.getFolderId());
	    	list.addAll(folder.getSubfolders(ctxt.octxt));
	    	
	    	Mailbox mbox = ctxt.item.getMailbox();
	    	list.addAll(mbox.getWikiList(ctxt.octxt, folder.getId()));

			String bt = params.get(sBODYTEMPLATE);
			String it = params.get(sITEMTEMPLATE);
			if (bt == null)
				bt = sDEFAULTBODYTEMPLATE;
			if (it == null)
				it = sDEFAULTITEMTEMPLATE;
			return handleTemplates(ctxt, list, bt, it);
		}
		
		public String apply(Context ctxt) throws ServiceException, IOException {
			Map<String,String> params = ctxt.token.parseParam();
			String format = params.get(sFORMAT);
			if (format == null) {
				format = sLIST;
			}
			if (format.equals(sTEMPLATE)) {
				return applyTemplates(ctxt, params);
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
		
		public static final String sDEFAULTBODYTEMPLATE = "_PathBodyTemplate";
		public static final String sDEFAULTITEMTEMPLATE = "_PathItemTemplate";
		
		public String getName() {
			return "Breadcrumbs";
		}
		public String getPattern() {
			return "PATH";
		}
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		private Folder getFolder(Context ctxt, MailItem item) throws ServiceException {
			Mailbox mbox = item.getMailbox();
			return mbox.getFolderById(ctxt.octxt, item.getFolderId());
		}
		private List<MailItem> getBreadcrumbs(Context ctxt) throws ServiceException {
			List<MailItem> list = new ArrayList<MailItem>();
			list.add(ctxt.item);
			Folder f = getFolder(ctxt, ctxt.item);
			while (f.getId() != 1) {
				list.add(0, f);
				f = getFolder(ctxt, f);
			}
			return list;
		}
		public String apply(Context ctxt) throws ServiceException, IOException {
			List<MailItem> list = getBreadcrumbs(ctxt);
			Map<String,String> params = ctxt.token.parseParam();
			String format = params.get(sFORMAT);
			if (format == null || format.equals(sSIMPLE)) {
				StringBuffer buf = new StringBuffer();
				buf.append("<span class='_breadcrumbs_simple'>");
				StringBuffer path = new StringBuffer();
				path.append("/");
				for (MailItem item : list) {
					String name;
					if (item instanceof Folder)
						name = ((Folder)item).getName();
					else
						name = item.getSubject();
					path.append(name);
					buf.append("<span class='_pageLink'>");
					buf.append("[[").append(name).append("][").append(path).append("]]");
					buf.append("</span>");
					path.append("/");
				}
				buf.append("</span>");
				return new WikiTemplate(buf.toString()).toString(ctxt);
			} else if (format.equals(sTEMPLATE)) {
				String bt = params.get(sBODYTEMPLATE);
				String it = params.get(sITEMTEMPLATE);
				if (bt == null)
					bt = sDEFAULTBODYTEMPLATE;
				if (it == null)
					it = sDEFAULTITEMTEMPLATE;
				return handleTemplates(ctxt, list, bt, it);
			} else {
				return reportError("format " + format + " not recognized");
			}
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
			if (!(ctxt.item instanceof Document)) 
				return "";
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
			if (!(ctxt.item instanceof Document)) 
				return "";
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
			if (!(ctxt.item instanceof Document)) 
				return "";
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
			Date createDate;
			if (ctxt.item instanceof Document) {
				Document doc = (Document) ctxt.item;
				createDate = new Date(doc.getLastRevision().getRevDate());
			} else
				createDate = new Date(ctxt.item.getDate());
			return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(createDate);
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
			if (!(ctxt.item instanceof Document)) 
				return "";
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
			Date modifyDate;
			if (ctxt.item instanceof Document) {
				Document doc = (Document) ctxt.item;
				modifyDate = new Date(doc.getLastRevision().getRevDate());
			} else
				modifyDate = new Date(ctxt.item.getDate());
			return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(modifyDate);
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
			if (!(ctxt.item instanceof Document)) 
				return "";
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
			if (!(ctxt.item instanceof Document)) 
				return "1";
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
	public static class InlineWiklet extends IncludeWiklet {
		public String getPattern() {
			return "INLINE";
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
		private WikiTemplate findPage(Context ctxt) throws ServiceException, IOException {
			Map<String,String> params = ctxt.token.parseParam();
			String page = params.get(sPAGE);
			if (page == null) {
				page = params.keySet().iterator().next();
			}
			return WikiTemplate.findTemplate(ctxt, page);
		}
		public boolean isExpired(WikiTemplate template, Context ctxt) throws ServiceException, IOException {
			WikiTemplate includedTemplate = findPage(ctxt);
			return includedTemplate.getModifiedTime() > template.getModifiedTime();
		}
		public String apply(Context ctxt) throws ServiceException, IOException {
			WikiTemplate template = findPage(ctxt);
			return template.toString(ctxt);
		}
	}
	public static class WikilinkWiklet extends Wiklet {
		private static final String URL_PREFIX = "/home/";
		private static final String PAGENAME = "pagename";
		private static final String TEXT = "text";
		
		public String getName() {
			return "Wikilink";
		}
		public String getPattern() {
			return "WIKILINK";
		}
		public boolean isExpired(WikiTemplate template, Context ctxt) {
			return false;
		}
		public String apply(Context ctxt) throws ServiceException, IOException {
			String link, title;
			if (ctxt.token.getType() == Token.TokenType.WIKILINK) {
				String text = ctxt.token.getValue();
				if (text.startsWith("http://")) {
					link = text;
					title = text;
				} else if (text.startsWith("<wiklet")) {
					WikiTemplate template = new WikiTemplate(text);
					link = template.toString(ctxt);
					title = link;
				} else {
					link = text;
					title = text;
					int pos = text.indexOf('|');
					if (pos != -1) {
						link = text.substring(0, pos);
						title = text.substring(pos+1);
					} else {
						pos = text.indexOf("][");
						if (pos != -1) {
							title = text.substring(0, pos);
							link = text.substring(pos+2);
						}
					}
				}
			} else {
				Map<String,String> params = ctxt.token.parseParam();
				link = params.get(PAGENAME);
				title = params.get(TEXT);
				if (title == null)
					title = link;
			}
			WikiUrl wurl = new WikiUrl(link, ctxt.item.getFolderId());
			StringBuffer buf = new StringBuffer();
			buf.append("<a href='");
			if (wurl.isRemote())
				buf.append(URL_PREFIX).append(wurl.getToken(1)).append('/').append(wurl.getToken(2));
			else if (wurl.isAbsolute())
				buf.append(URL_PREFIX).append(ctxt.item.getMailbox().getAccount().getUid()).append(wurl.getToken(1));
			else
				buf.append(link);
			buf.append("'>").append(title).append("</a>");
			return buf.toString();
		}
	}
}
