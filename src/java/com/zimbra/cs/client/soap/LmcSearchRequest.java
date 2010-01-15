/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2010 Zimbra, Inc.
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

package com.zimbra.cs.client.soap;

import java.util.Iterator;
import java.util.ArrayList;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;
import org.dom4j.QName;

import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;

public class LmcSearchRequest extends LmcSoapRequest {

    private String mLimit;

    private String mOffset;

    private String mTypes;

    private String mSortBy;

    private String mQuery;

    public void setLimit(String l) {
        mLimit = l;
    }

    public void setOffset(String o) {
        mOffset = o;
    }

    public void setTypes(String t) {
        mTypes = t;
    }

    public void setSortBy(String s) {
        mSortBy = s;
    }

    public void setQuery(String q) {
        mQuery = q;
    }

    public String getLimit() {
        return mLimit;
    }

    public String getOffset() {
        return mOffset;
    }

    public String getTypes() {
        return mTypes;
    }

    public String getSortBy() {
        return mSortBy;
    }

    public String getQuery() {
        return mQuery;
    }

    protected Element createQuery(QName elemName) {
        Element request = DocumentHelper.createElement(elemName);

        // add all the attributes of the SearchRequest element
        addAttrNotNull(request, MailConstants.A_QUERY_LIMIT, mLimit);
        addAttrNotNull(request, MailConstants.A_QUERY_OFFSET, mOffset);
        addAttrNotNull(request, MailConstants.A_SEARCH_TYPES, mTypes);
        addAttrNotNull(request, MailConstants.A_SORTBY, mSortBy);

        // add the query element
        DomUtil.add(request, MailConstants.E_QUERY, mQuery);

        return request;
    }

    protected Element getRequestXML() {
        return createQuery(MailConstants.SEARCH_REQUEST);
    }


    protected void parseResponse(LmcSearchResponse response,
                                 Element responseXML)
        throws ServiceException, LmcSoapClientException
    {
        // get the offset and more attributes from the <SearchResponse> element
        response.setOffset(DomUtil.getAttr(responseXML, MailConstants.A_QUERY_OFFSET));
        response.setMore(DomUtil.getAttr(responseXML, MailConstants.A_QUERY_MORE));

        /*
         * Iterate through the elements and put them in a generic ArrayList.  
         * XXX: Should validate that the correct types are returned.
         */
        ArrayList mailItems = new ArrayList();
        for (Iterator it = responseXML.elementIterator(); it.hasNext();) {
            Element e = (Element) it.next();

            // find out what element it is and go process that
            String elementType = e.getQName().getName();
            Object o;
            if (elementType.equals(MailConstants.E_CONV)) {
                o = parseConversation(e);
            } else if (elementType.equals(MailConstants.E_MSG)) {
                o = parseMessage(e);
            } else if (elementType.equals(MailConstants.E_MIMEPART)) {
                o = parseMimePart(e);
            } else if (elementType.equals(MailConstants.E_CONTACT)) {
                o = parseContact(e);
            } else if (elementType.equals(MailConstants.E_NOTE)) {
                o = parseNote(e);
            } else if (elementType.equals(MailConstants.E_DOC)) {
                o = parseDocument(e);
            } else if (elementType.equals(MailConstants.E_WIKIWORD)) {
                o = parseWiki(e);
            } else {
                // unknown element type as search result
                throw new LmcSoapClientException("Unexpected element type " + elementType);
            }

            // add the object to the ArrayList
            mailItems.add(o);
        }

        response.setResults(mailItems);
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
        throws ServiceException, LmcSoapClientException
    {
        LmcSearchResponse response = new LmcSearchResponse();
        parseResponse(response, responseXML);
        return response;
    }
}