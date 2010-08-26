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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.mail.Part;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.html.HtmlDefang;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mime.ParsedDocument;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.wiki.PageCache;
import com.zimbra.cs.wiki.WikiPage;
import com.zimbra.cs.wiki.WikiTemplate;

public class WikiFormatter extends Formatter {

	@Override
	public String getType() {
		return "wiki";
	}

	@Override
	public boolean canBeBlocked() {
		return true;
	}
	
    private static PageCache sCache = new PageCache();
    
    public static void expireCacheItem(MailItem item) {
    	try {
    		String key = sCache.generateKey(item.getAccount(), item);
    		sCache.removePage(key);
    	} catch (ServiceException e) {
    		ZimbraLog.wiki.info("unable to expire item from cache", e);
    	}
    }
    
    public static void expireCache() {
    	sCache = new PageCache();
    }
    
    private void handleWiki(Context context, WikiItem wiki) throws IOException, ServiceException {
    	WikiPage.WikiContext ctxt = createWikiContext(context);
    	String template = null;
        String v = context.params.get(UserServlet.QP_VERSION);
        if (v == null) {
            // fully rendered pages are cached in <code>PageCache</code>.
            String key = sCache.generateKey(context.opContext.getAuthenticatedUser(), wiki);
            template = sCache.getPage(key);
            if (template == null) {
                WikiTemplate wt = getTemplate(context, wiki);
                template = wt.getComposedPage(ctxt, wiki, CHROME);
                //sCache.addPage(key, template);
            }
        } else {
            WikiTemplate wt = getTemplate(context, wiki);
            template = wt.getComposedPage(ctxt, wiki, VERSION_CHROME);
        }
    	String url = UserServlet.getRestUrl(wiki);
		printWikiPage(context, template, wiki.getName(), url, neuterHtmlTags(wiki.getAccount()));
	}
    
    private void handleWikiHistory(Context context, WikiItem wiki) throws IOException, ServiceException {
    	WikiPage.WikiContext ctxt = createWikiContext(context);
    	String template = null;
		WikiPage chromePage = WikiPage.findTemplate(ctxt, wiki.getMailbox().getAccountId(), VERSION);
       	WikiTemplate wt = chromePage.getTemplate(ctxt);
       	template = wt.getComposedPage(ctxt, wiki, VERSION_CHROME);
    	String url = UserServlet.getRestUrl(wiki);
		printWikiPage(context, template, wiki.getName(), url, neuterHtmlTags(wiki.getAccount()));
	}
    
    private static final String TOC = "_Index";
    private static final String CHROME = "_Template";
    private static final String VERSION_CHROME = "_VersionTemplate";
    private static final String VERSION = "_VersionIndex";    
    
    private WikiTemplate getTemplate(Context context, WikiItem item) throws ServiceException {
    	return getTemplate(context, item.getMailbox().getAccountId(), item.getFolderId(), item.getWikiWord());
    }
    private WikiTemplate getTemplate(Context context, String accountId, int folderId, String name) throws ServiceException {
        int ver = -1;
        String v = context.params.get(UserServlet.QP_VERSION);
        if (v != null)
            ver = Integer.parseInt(v);
        
    	WikiPage.WikiContext wctxt = createWikiContext(context);
        MailItem item = context.targetMailbox.getItemByPath(context.opContext, name, folderId);
        int id = item.getId();
        if (ver != -1)
        	item = context.targetMailbox.getItemRevision(context.opContext, id, MailItem.TYPE_DOCUMENT, ver);
    	WikiPage page = WikiPage.create((Document)item);
    	return page.getTemplate(wctxt);
    }
    private WikiTemplate getDefaultTOC() {
    	return WikiTemplate.getDefaultTOC();
    }
    
    private WikiPage.WikiContext createWikiContext(Context context) {
    	return new WikiPage.WikiContext(context.opContext, 
    			context.cookieAuthHappened ? context.authToken : null,
    			context.getView(), context.locale);
    }
    private void handleWikiFolder(Context context, Folder folder) throws IOException, ServiceException {
    	WikiPage.WikiContext ctxt = createWikiContext(context);
    	String key = sCache.generateKey(context.opContext.getAuthenticatedUser(), folder);
    	String template = sCache.getPage(key);
    	if (template == null) {
    		WikiPage chromePage = WikiPage.findTemplate(ctxt, folder.getAccount().getId(), TOC);
        	WikiTemplate wt = null;
        	if (chromePage == null)
        		wt = getDefaultTOC();
        	else
        		wt = chromePage.getTemplate(ctxt);
        	
        	template = wt.getComposedPage(ctxt, folder, CHROME);
    		//sCache.addPage(key, template);
    	}
    	String url = UserServlet.getRestUrl(folder);
		printWikiPage(context, template, folder.getName(), url, neuterHtmlTags(folder.getAccount()));
    }

    private boolean neuterHtmlTags(Account acct) throws ServiceException {
    	if (acct != null) {
    		return acct.getBooleanAttr(Provisioning.A_zimbraNotebookSanitizeHtml, true);
    	}
    	return true;
    }
    
	/**
	 * <b>Note:</b>
	 * This will be revisited when the client relies on the REST
	 * output for display/browsing functionality.
	 */
	private static void printWikiPage(Context context, String s, String title, String baseURL, boolean neuter)
	throws IOException {
        String disp = context.req.getParameter(UserServlet.QP_DISP);
        disp = (disp == null || disp.toLowerCase().startsWith("i") ) ? Part.INLINE : Part.ATTACHMENT;
		context.resp.setContentType(WikiItem.WIKI_CONTENT_TYPE);
		if (disp.equals(Part.ATTACHMENT)) {
			String cd = disp + "; filename=" + HttpUtil.encodeFilename(context.req, title);
			context.resp.addHeader("Content-Disposition", cd);
		}
		PrintWriter out = context.resp.getWriter();
		out.println("<HTML>");
		out.println("<HEAD>");
		out.println("<TITLE>");
		out.println(title);
		out.println("</TITLE>");		 
		out.println("<base href='"+baseURL+"'/>");
		/***
		// NOTE: This doesn't work because this servlet is at a different
		//       context path than the wiki.css file.
		String contextPath = context.req.getContextPath();
		out.print("<LINK rel='stylesheet' type='text/css' href='");
		out.print(contextPath);
		out.println("/css/wiki.css'>");
		/***/
		String defaultFontFamily = context.targetAccount.getAttr("zimbraPrefHtmlEditorDefaultFontFamily");
		String defaultFontColor = context.targetAccount.getAttr("zimbraPrefHtmlEditorDefaultFontColor");
		String defaultFontSize = context.targetAccount.getAttr("zimbraPrefHtmlEditorDefaultFontSize");
		
		// REVISIT: Can we assume that the context path for the wiki.css
		//          file will be "/zimbra"?
		out.print("<LINK rel='stylesheet' type='text/css' href='/zimbra/css/wiki.css'>");
		
		if(defaultFontFamily !=null){
		out.print("<style>");
		out.print("body, table{ ");
		out.print("font-family:"+defaultFontFamily+";");
		out.print("color:"+defaultFontColor+";");
		out.print("font-size:"+defaultFontSize+";");
		out.print("}");
		out.print("</style>");
		}
		/***/
		out.println("</HEAD>");
		out.println("<BODY style='margin:0px'>");
		if (neuter && !disp.equals(Part.ATTACHMENT))
			out.println(HtmlDefang.defang(s, false));
		else
			out.println(s);
		out.println("</BODY>");
		out.println("</HTML>");
	}
    
	@Override
	public void formatCallback(Context context) throws UserServletException, ServiceException, IOException {
		//long t0 = System.currentTimeMillis();
        String view = context.params.get(UserServlet.QP_VIEW);        
        if (view!=null && view.compareTo(UserServlet.QP_HISTORY) == 0 && context.target instanceof  WikiItem) {
        	handleWikiHistory(context, (WikiItem) context.target);
        } else if (context.target instanceof WikiItem) {
            handleWiki(context, (WikiItem) context.target);
        } else if (context.target instanceof Folder) {
            handleWikiFolder(context, (Folder) context.target);
        } else {
            throw UserServletException.notImplemented("can only handle Wiki messages and Documents");
        }
        //long t1 = System.currentTimeMillis() - t0;
        //ZimbraLog.wiki.info("Formatting " + item.getSubject() + " : " + t1 + "ms");
	}

	@Override
    public boolean supportsSave() {
        return true;
    }

    @Override
	public void saveCallback(Context context, String contentType, Folder folder, String filename)
    throws UserServletException, ServiceException, IOException {
    	if (filename == null)
            throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "filename cannot be empty");
    		
        String creator = (context.authAccount == null ? null : context.authAccount.getName());
        InputStream is = context.getRequestInputStream();
        Mailbox mbox = folder.getMailbox();
        ParsedDocument pd = new ParsedDocument(is, filename, WikiItem.WIKI_CONTENT_TYPE, System.currentTimeMillis(), creator, null);
        MailItem item = null;
        
        try {
            MailItem orig = mbox.getItemByPath(context.opContext, filename, folder.getId());
            // XXX: should we just overwrite here instead?
            if (!(orig instanceof WikiItem))
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "cannot overwrite existing object at that path");

            item = mbox.addDocumentRevision(context.opContext, orig.getId(), pd);
        } catch (NoSuchItemException nsie) {
            item = mbox.createDocument(context.opContext, folder.getId(), pd, MailItem.TYPE_WIKI);
        } finally {
        	is.close();
        }

        if (item != null) {
        	context.resp.addHeader("X-Zimbra-ItemId", item.getId() + "");
        	context.resp.addHeader("X-Zimbra-Version", item.getVersion() + "");
        	context.resp.addHeader("X-Zimbra-Modified", item.getChangeDate() + "");
        	context.resp.addHeader("X-Zimbra-Change", item.getModifiedSequence() + "");
        	context.resp.addHeader("X-Zimbra-Revision", item.getSavedSequence() + "");
        }
        WikiFormatter.expireCacheItem(folder);
	}
}
