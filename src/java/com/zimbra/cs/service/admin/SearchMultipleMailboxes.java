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

/*
 * Created on Mar 28, 2005
 *
 */
package com.zimbra.cs.service.admin;

import java.util.Iterator;
import java.util.Map;

import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.index.ResultsPager;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.mail.Search;
import com.zimbra.cs.service.util.ParseMailboxID;
import com.zimbra.cs.util.CrossMailboxSearch;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;


/**
 * @author tim
 */
public class SearchMultipleMailboxes extends Search {
    //
    // We're an admin function -- but we can't directly inherit from AdminDocumentHandler
    // since we're already inheriting from Search...so override these funcs directly
    //
    public boolean needsAuth(Map context) { return true; }
    public boolean needsAdminAuth(Map context) { return true; }
    
    public Element handle(Element request, Map context) throws ServiceException {
        long startTime =  sWatch.start();
        try {
            ZimbraContext lc = getZimbraContext(context);
            String encodedAuthToken = getEncodedAuthToken(lc);

            SearchParams params = parseCommonParameters(request, lc);
            
            CrossMailboxSearch xmbsearch = getXMBSearch(request);
            ZimbraQueryResults results = xmbsearch.getSearchResults(encodedAuthToken, params);
            
            // TODO: log all the requested mailboxes? Is logging the query string too sensitive?
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "SearchMultipleMailboxes", "query", params.getQueryStr()}));

            Element retVal = null;

            try {
                Element response = lc.createElement(AdminService.SEARCH_MULTIPLE_MAILBOXES_RESPONSE);
                response.addAttribute(MailService.A_QUERY_OFFSET, Integer.toString(params.getOffset()));
                
                ResultsPager pager = ResultsPager.create(results, params);
                retVal = putHits(lc, response, pager, INCLUDE_MAILBOX_INFO, params);
            } finally {
                if (DONT_CACHE_RESULTS) {
                    results.doneWithSearchResults();
                }
            }
            
            if (mLog.isDebugEnabled()) {
                mLog.debug("Search Element Handler Finished in "+(System.currentTimeMillis()-startTime)+"ms");
            }
            
            return retVal;
            
        } finally {
            sWatch.stop(startTime);
        }
    }
    
    private static String getEncodedAuthToken(ZimbraContext lc)  throws ServiceException {
        try {
            return lc.getAuthToken().getEncoded();
        } catch (AuthTokenException e) {
            throw ServiceException.FAILURE("AuthTokenException ", e);
        }
    }

    
    /**
     * Get a set of ServerSearchTask 
     * @param request
     * @return
     * @throws ServiceException
     */
    private CrossMailboxSearch getXMBSearch(Element request) throws ServiceException 
    {
        CrossMailboxSearch xmb = new CrossMailboxSearch(true);
        
        for (Iterator iter = request.elementIterator(MailService.E_MAILBOX); iter.hasNext();) {
            Element cur = (Element) iter.next();

            String idStr = cur.getAttribute(MailService.A_ID);
            ParseMailboxID id = ParseMailboxID.parse(idStr);
            xmb.addMailboxToSearchList(id);
        }
        return xmb;
    }
}
