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
package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.io.InputStream;

import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.wiki.PageCache;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.Wiki.WikiContext;
import com.zimbra.cs.wiki.WikiTemplate;

public class WikiFormatter extends Formatter {

    public static class Format {};
    public static class Save {};
    static int sFormatLoad = Operation.setLoad(WikiFormatter.Format.class, 10);
    static int sSaveLoad = Operation.setLoad(WikiFormatter.Save.class, 10);
    int getFormatLoad() { return  sFormatLoad; }
    int getSaveLoad() { return sSaveLoad; }
    
	@Override
	public String getType() {
		return "wiki";
	}

	@Override
	public boolean canBeBlocked() {
		return true;
	}
	
    private void handleDocument(Context context, Document doc) throws IOException, ServiceException {
        context.resp.setContentType(doc.getContentType());
        InputStream is = doc.getRawDocument();
        ByteUtil.copy(is, true, context.resp.getOutputStream(), false);
    }
    
    private static PageCache sCache = new PageCache();
    
    private void handleWiki(Context context, WikiItem wiki) throws IOException, ServiceException {
    	WikiContext ctxt = createWikiContext(context);
    	// fully rendered pages are cached in <code>PageCache</code>.
    	String key = sCache.generateKey(ctxt, wiki);
    	String template = sCache.getPage(key);
    	if (template == null) {
    		WikiTemplate wt = getTemplate(context, wiki);
    		template = wt.getComposedPage(ctxt, wiki, CHROME);
    		sCache.addPage(key, template);
    	}
		printWikiPage(context, template);
	}

    private static final String TOC = "_Index";
    private static final String CHROME = "_Template";
    
    private WikiTemplate getTemplate(Context context, WikiItem item) throws IOException, ServiceException {
    	return getTemplate(context, item.getMailbox().getAccountId(), item.getFolderId(), item.getWikiWord());
    }
    private WikiTemplate getTemplate(Context context, Folder folder, String name) throws IOException, ServiceException {
    	return getTemplate(context, folder.getMailbox().getAccountId(), folder.getId(), name);
    }
    private WikiTemplate getTemplate(Context context, String accountId, int folderId, String name) throws IOException, ServiceException {
    	WikiContext wctxt = createWikiContext(context);
    	Wiki wiki = Wiki.getInstance(wctxt, accountId, Integer.toString(folderId));
    	return wiki.getTemplate(wctxt, name);
    }
    private WikiTemplate getDefaultTOC() {
    	return WikiTemplate.getDefaultTOC();
    }
    
    private WikiContext createWikiContext(Context context) {
    	return new WikiContext(context.opContext, 
    			context.cookieAuthHappened ? context.authTokenCookie : null,
    			context.getView());
    }
    private void handleWikiFolder(Context context, Folder folder) throws IOException, ServiceException {
    	WikiContext ctxt = createWikiContext(context);
    	String key = sCache.generateKey(ctxt, folder);
    	String template = sCache.getPage(key);
    	if (template == null) {
        	WikiTemplate wt = getTemplate(context, folder, TOC);
        	
        	if (wt == null)
        		wt = getDefaultTOC();
        	
        	template = wt.getComposedPage(ctxt, folder, CHROME);
    		sCache.addPage(key, template);
    	}
		printWikiPage(context, template);
    }

	/**
	 * <b>Note:</b>
	 * This will be revisited when the client relies on the REST
	 * output for display/browsing functionality.
	 */
	private static void printWikiPage(Context context, String s)
	throws IOException {
		context.resp.setContentType(WikiItem.WIKI_CONTENT_TYPE);
		javax.servlet.ServletOutputStream out = context.resp.getOutputStream();
		out.println("<HTML>");
		out.println("<HEAD>");
		/***
		// NOTE: This doesn't work because this servlet is at a different
		//       context path than the wiki.css file.
		String contextPath = context.req.getContextPath();
		out.print("<LINK rel='stylesheet' type='text/css' href='");
		out.print(contextPath);
		out.println("/css/wiki.css'>");
		/***/
		// REVISIT: Can we assume that the context path for the wiki.css
		//          file will be "/zimbra"?
		out.print("<LINK rel='stylesheet' type='text/css' href='/zimbra/css/wiki.css'>");
		/***/
		out.println("</HEAD>");
		out.println("<BODY style='margin:0px'>");
		out.println(s);
		out.println("</BODY>");
		out.println("</HTML>");
	}
    
	@Override
	public void formatCallback(Context context, MailItem item) throws UserServletException, ServiceException, IOException {
		//long t0 = System.currentTimeMillis();
        if (item instanceof Folder && context.itemId == null && !context.itemPath.endsWith("/")) {
        	context.resp.sendRedirect(context.req.getRequestURI() + "/");
        	return;
        }
        if (item instanceof WikiItem) {
            handleWiki(context, (WikiItem) item);
        } else if (item instanceof Document) {
            handleDocument(context, (Document) item);
        } else if (item instanceof Folder) {
            handleWikiFolder(context, (Folder) item);
        } else {
            throw UserServletException.notImplemented("can only handle Wiki messages and Documents");
        }
        //long t1 = System.currentTimeMillis() - t0;
        //ZimbraLog.wiki.info("Formatting " + item.getSubject() + " : " + t1 + "ms");
	}

	@Override
	public void saveCallback(byte[] body, Context context, Folder folder) throws UserServletException {
        throw UserServletException.notImplemented("saving documents via POST not yet supported.");
	}

}
