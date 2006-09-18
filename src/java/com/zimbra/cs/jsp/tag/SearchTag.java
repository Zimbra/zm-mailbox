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
package com.zimbra.cs.jsp.tag;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import com.zimbra.cs.jsp.bean.ZSearchHitBean;
import com.zimbra.cs.jsp.bean.ZSearchResultBean;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZSearchParams;
import com.zimbra.cs.zclient.ZSearchResult;
import com.zimbra.cs.zclient.ZMailbox.SearchSortBy;

public class SearchTag extends ZimbraSimpleTag {
    
    private String mVar;
    private int mLimit = Integer.MAX_VALUE;
    private int mOffset;
    private String mConvId = null;
    private String mTypes = ZSearchParams.TYPE_CONVERSATION;
    private String mQuery = "in:inbox";
    private SearchSortBy mSortBy = SearchSortBy.dateDesc;
    
    public void setVar(String var) { this.mVar = var; }

    public void setTypes(String types) { this.mTypes = types; }

    public void setQuery(String query) {
        if (query == null || query.equals("")) query = "in:inbox";
            this.mQuery = query;
    }    

    public void setSort(String sortBy) throws ServiceException { this.mSortBy = SearchSortBy.fromString(sortBy); }
    
    public void setConv(String convId) { this.mConvId = convId; }

    public void setLimit(int limit) { this.mLimit = limit; }

    public void setOffset(int offset) { this.mOffset = offset; }
    
    public void doTag() throws JspException, IOException {
        JspContext jctxt = getJspContext();
        try {
            ZMailbox mbox = getMailbox();

            ZSearchParams params = new ZSearchParams(mQuery);
            params.setOffset(mOffset);
            params.setLimit(mLimit);
            params.setSortBy(mSortBy);
            params.setTypes(mTypes);

            ZSearchResult searchResults = mConvId == null ? mbox.search(params) : mbox.searchConversation(mConvId, params);
            int prevOff = mLimit > mOffset ? 0 : mOffset - mLimit;
            int nextOff = mOffset + mLimit;
            
            jctxt.setAttribute(mVar, new ZSearchResultBean(searchResults, prevOff, nextOff),  PageContext.PAGE_SCOPE);
            
        } catch (ServiceException e) {
            getJspContext().getOut().write(e.toString());
        }
    }
}
