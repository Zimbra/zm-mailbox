/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.wiki.PageCache;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.WikiPage;
import com.zimbra.cs.wiki.Wiki.WikiContext;
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
    	WikiContext ctxt = createWikiContext(context);
    	String template = null;
        String v = context.params.get(UserServlet.QP_VERSION);
        if (v == null) {
            // fully rendered pages are cached in <code>PageCache</code>.
            String key = sCache.generateKey(context.opContext.getAuthenticatedUser(), wiki);
            template = sCache.getPage(key);
            if (template == null) {
                WikiTemplate wt = getTemplate(context, wiki);
                template = wt.getComposedPage(ctxt, wiki, CHROME);
                sCache.addPage(key, template);
            }
        } else {
            WikiTemplate wt = getTemplate(context, wiki);
            template = wt.getComposedPage(ctxt, wiki, VERSION_CHROME);
        }
    	String url = UserServlet.getRestUrl(wiki);
		printWikiPage(context, template, wiki.getName(),url);
	}
    
    private void handleWikiHistory(Context context, WikiItem wiki) throws IOException, ServiceException {
    	WikiContext ctxt = createWikiContext(context);
    	String template = null;
       	WikiTemplate wt = getTemplate(context, wiki.getMailbox().getAccountId(), wiki.getFolderId(), VERSION);
       	template = wt.getComposedPage(ctxt, wiki, VERSION_CHROME);
    	String url = UserServlet.getRestUrl(wiki);
		printWikiPage(context, template, wiki.getName(),url);
	}
    
    private static final String TOC = "_Index";
    private static final String CHROME = "_Template";
    private static final String VERSION_CHROME = "_VersionTemplate";
    private static final String VERSION = "_VersionIndex";    
    
    private WikiTemplate getTemplate(Context context, WikiItem item) throws ServiceException {
    	return getTemplate(context, item.getMailbox().getAccountId(), item.getFolderId(), item.getWikiWord());
    }
    private WikiTemplate getTemplate(Context context, Folder folder, String name) throws ServiceException {
    	return getTemplate(context, folder.getMailbox().getAccountId(), folder.getId(), name);
    }
    private WikiTemplate getTemplate(Context context, String accountId, int folderId, String name) throws ServiceException {
        int ver = -1;
        String v = context.params.get(UserServlet.QP_VERSION);
        if (v != null)
            ver = Integer.parseInt(v);
        
    	WikiContext wctxt = createWikiContext(context);
    	Wiki wiki = Wiki.getInstance(wctxt, accountId, Integer.toString(folderId));
    	WikiPage page = wiki.lookupWikiRevision(wctxt, name, ver);
        if (page != null)
            return page.getTemplate(wctxt);
        
        return wiki.getTemplate(wctxt, name);
    }
    private WikiTemplate getDefaultTOC() {
    	return WikiTemplate.getDefaultTOC();
    }
    
    private WikiContext createWikiContext(Context context) {
    	return new WikiContext(context.opContext, 
    			context.cookieAuthHappened ? context.authTokenCookie : null,
    			context.getView(), context.locale);
    }
    private void handleWikiFolder(Context context, Folder folder) throws IOException, ServiceException {
    	WikiContext ctxt = createWikiContext(context);
    	String key = sCache.generateKey(context.opContext.getAuthenticatedUser(), folder);
    	String template = sCache.getPage(key);
    	if (template == null) {
        	WikiTemplate wt = getTemplate(context, folder, TOC);
        	
        	if (wt == null)
        		wt = getDefaultTOC();
        	
        	template = wt.getComposedPage(ctxt, folder, CHROME);
    		//sCache.addPage(key, template);
    	}
    	String url = UserServlet.getRestUrl(folder);
		printWikiPage(context, template, folder.getName(),url);
    }

	/**
	 * <b>Note:</b>
	 * This will be revisited when the client relies on the REST
	 * output for display/browsing functionality.
	 */
	private static void printWikiPage(Context context, String s, String title, String baseURL)
	throws IOException {
		context.resp.setContentType(WikiItem.WIKI_CONTENT_TYPE);
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
		out.println(s);
		out.println("</BODY>");
		out.println("</HTML>");
	}
    
	@Override
	public void formatCallback(Context context) throws UserServletException, ServiceException, IOException {
		if (!context.targetAccount.getBooleanAttr("zimbraFeatureNotebookEnabled", false))
			throw UserServletException.badRequest("Notebook is not enabled for user "+context.targetAccount.getName());
		//long t0 = System.currentTimeMillis();
        if (context.target instanceof Folder && context.itemId == null && !context.itemPath.endsWith("/")) {
        	context.resp.sendRedirect(context.req.getRequestURI() + "/");
        	return;
        }
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
        byte[] body = context.getPostBody();
        Mailbox mbox = folder.getMailbox();

        String creator = (context.authAccount == null ? null : context.authAccount.getName());
        ParsedDocument pd = new ParsedDocument(body, filename, WikiItem.WIKI_CONTENT_TYPE, System.currentTimeMillis(), creator);
        try {
            MailItem item = mbox.getItemByPath(context.opContext, filename, folder.getId());
            // XXX: should we just overwrite here instead?
            if (!(item instanceof WikiItem))
                throw new UserServletException(HttpServletResponse.SC_BAD_REQUEST, "cannot overwrite existing object at that path");

            mbox.addDocumentRevision(context.opContext, item.getId(), item.getType(), pd);
        } catch (NoSuchItemException nsie) {
            mbox.createDocument(context.opContext, folder.getId(), pd, MailItem.TYPE_WIKI);
        }

        // clear the wiki cache because we just went straight to the Mailbox
        Wiki.expireNotebook(folder);
	}
}
