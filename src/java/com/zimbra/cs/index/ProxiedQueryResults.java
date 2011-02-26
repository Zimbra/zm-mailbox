/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.index;

import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.util.ParseMailboxID;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ProxyTarget;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.QName;

/**
 * Represents the results of a query made on a remote server. This class takes
 * the ServerID and the query parameters and makes does smart chunking
 * (buffering) of the results - making SOAP requests to the remote Zimbra server
 * as necessary.
 * <p>
 * TODO wish this could be a {@link QueryOperation} subclass instead of just a
 * results subclass: unfortunately right now the intersection and other
 * operations assume you can get to the actual hit objects (for doing
 * Intersections and the like)....  Long-term-fixme...
 *
 * @since Mar 28, 2005
 */
public final class ProxiedQueryResults extends ZimbraQueryResultsImpl {
    /**
     * magic # for parameter checking.
     */
    public static final int SEARCH_ALL_MAILBOXES = 1234;

    /**
     * minimum number of hits to request each time we make a round-trip to the remote server.
     */
    protected static final int MIN_BUFFER_CHUNK_SIZE = 25;

    protected ArrayList<ProxiedHit> hitBuffer;
    protected int bufferStartOffset = 0;  // inclusive
    protected int bufferEndOffset = 0; // not-inclusive
    protected int iterOffset = 0; // globally, NOT an index into the buffer
    protected boolean atEndOfList = false;

    protected String server;
    protected AuthToken authToken;
    protected String targetAcctId = null;
    protected SoapProtocol responseProto = null;

    private SearchParams searchParams;

    // mailbox specifier
    private boolean isMultipleMailboxes = false;
    private boolean isAllMailboxes = false;
    private List<ParseMailboxID> mailboxes;

    /**
     * read timeout for the proxy SOAP request. -1 mean use default SOAP http
     * client timeout.
     */
    private long mTimeout = -1;

    private List<QueryInfo> queryInfo = new ArrayList<QueryInfo>();

    /**
     * A search request in the current mailbox on a different server.
     *
     * The query string for this search is the string passed in the queryString
     * parameter, *not* the query string in the params.
     *
     * @param respProto
     * @param authToken
     * @param targetAccountId
     * @param server hostname of server
     * @param params
     * @param queryString queryString to use for this search
     * @param mode
     */
    public ProxiedQueryResults(SoapProtocol respProto, AuthToken authToken, String targetAccountId, String server,
            SearchParams params, String queryString, Mailbox.SearchResultMode mode) {
        super(params.getTypes(), params.getSortBy(), mode);
        setSearchParams(params, queryString);
        this.authToken = authToken;
        this.server = server;
        this.targetAcctId = targetAccountId;
        this.responseProto = respProto;
    }

    /**
     * A search request in the current mailbox on a different server
     *
     * @param encodedAuthToken (call ZimbraContext.getAuthToken().getEncoded() if necessary)
     * @param server hostname of server
     * @param params
     */
    public ProxiedQueryResults(SoapProtocol respProto, AuthToken authToken, String server, SearchParams params,
            Mailbox.SearchResultMode mode) {
        super(params.getTypes(), params.getSortBy(), mode);
        setSearchParams(params);
        searchParams.setOffset(0);
        this.authToken = authToken;
        this.server = server;
        this.responseProto = respProto;
    }

    /**
     * An admin-only request to search ALL mailboxes on the remote server
     *
     * @param authToken (call ZimbraContext.getAuthToken().getEncoded() if necessary)
     * @param server server ID
     * @param params search query parameters
     * @param searchAllMailboxes must be set to SEARCH_ALL_MAILBOXES
     */
    public ProxiedQueryResults(SoapProtocol respProto, AuthToken authToken, String server, SearchParams params,
            Mailbox.SearchResultMode mode, int searchAllMailboxes) {
        super(params.getTypes(), params.getSortBy(), mode);
        assert(searchAllMailboxes == SEARCH_ALL_MAILBOXES);
        setSearchParams(params);
        this.authToken = authToken;
        this.server = server;
        this.isMultipleMailboxes = true;
        this.isAllMailboxes = true;
        this.responseProto = respProto;
    }

    /**
     * An admin-only request to search a set of mailboxes on the remote server
     *
     * @param encodedAuthToken (call ZimbraContext.getAuthToken().getEncoded() if necessary)
     * @param server
     * @param params
     */
    public ProxiedQueryResults(SoapProtocol respProto, AuthToken authToken, String server, SearchParams params,
            Mailbox.SearchResultMode mode, List<ParseMailboxID> mailboxes) {
        super(params.getTypes(), params.getSortBy(), mode);
        setSearchParams(params);
        this.authToken = authToken;
        this.server = server;
        this.isMultipleMailboxes = true;
        this.responseProto = respProto;
        this.mailboxes = mailboxes;
    }

    private void setSearchParams(SearchParams params) {
        searchParams = (SearchParams) params.clone();
        searchParams.clearCursor();
        // when doing offset-paging, since we do a mergesort locally, the remote query must start
        // at offset 0 and page through all the results
        searchParams.setOffset(0);
    }

    private void setSearchParams(SearchParams params, String queryString) {
        setSearchParams(params);
        searchParams.setQueryStr(queryString);
    }

    public void setTimeout(long timeout) {
        mTimeout = timeout;
    }

    @Override
    public long getTotalHitCount() {
        return -1;
    }

    @Override
    public void doneWithSearchResults() {
    }

    @Override
    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        iterOffset = hitNo;

        if (iterOffset < bufferStartOffset || iterOffset > bufferEndOffset) {

            // special-case: if we are already at the end of the list AND they want to
            // go even further, then we can just drop out
            if (iterOffset > bufferEndOffset && atEndOfList) {
                return null; // no hit
            }

            // not in current buffer...clear the buffer
            bufferStartOffset = iterOffset;
            bufferEndOffset = iterOffset;
            hitBuffer = null;
            atEndOfList = false;
        }

        // now either in current buffer, or current buffer is empty -- getNext will do the rest
        return getNext();
    }

    @Override
    public void resetIterator() {
        iterOffset = 0;
        atEndOfList = false;
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        ZimbraHit retVal = peekNext();
        if (retVal != null) {
            iterOffset++;
        }
        return retVal;
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        if (iterOffset >= bufferEndOffset) {
            if (!bufferNextHits()) {
                return null;
            }
        }
        return hitBuffer.get(iterOffset - bufferStartOffset);
    }

    String getServer() {
        return server;
    }

    @Override
    public String toString() {
        String url;
        try {
            url = URLUtil.getAdminURL(Provisioning.getInstance().get(ServerBy.name, server));
        } catch (ServiceException ex) {
            url = server;
        }
        return Objects.toStringHelper(this).add("url", url).add("acctId", targetAcctId).toString();
    }

    /**
     * Always does a request -- caller is responsible for checking to see if this is necessary or not
     */
    private boolean bufferNextHits() throws ServiceException {
        if (atEndOfList || searchParams.getHopCount() > ZimbraSoapContext.MAX_HOP_COUNT) {
            return false;
        }

        bufferStartOffset = iterOffset;

        int chunkSizeToUse = searchParams.getLimit() * 2;
        if (chunkSizeToUse < MIN_BUFFER_CHUNK_SIZE) {
            chunkSizeToUse = MIN_BUFFER_CHUNK_SIZE;
        }
        if (chunkSizeToUse > 500) {
            chunkSizeToUse = 500;
        }

        bufferEndOffset = bufferStartOffset + chunkSizeToUse;
        hitBuffer = new ArrayList<ProxiedHit>(chunkSizeToUse);

        QName qnrequest = (isMultipleMailboxes ?
                AdminConstants.SEARCH_MULTIPLE_MAILBOXES_REQUEST : MailConstants.SEARCH_REQUEST);
        Element searchElt = Element.create(responseProto, qnrequest);

        searchParams.setOffset(bufferStartOffset);
        searchParams.setLimit(chunkSizeToUse);
        searchParams.encodeParams(searchElt);

        if (isMultipleMailboxes) {
            if (isAllMailboxes) {
                Element mbxElt = searchElt.addElement(MailConstants.E_MAILBOX);
                ParseMailboxID id = ParseMailboxID.serverAll(server);
                mbxElt.addAttribute(MailConstants.A_ID, id.getString());
            } else {
                for (ParseMailboxID id : mailboxes) {
                    Element mboxEl = searchElt.addElement(MailConstants.E_MAILBOX);
                    if (id.getAccount() != null) {
                        mboxEl.addAttribute(MailConstants.A_NAME, id.getAccount().getName());
                    } else {
                        mboxEl.addAttribute(MailConstants.A_ID, id.getMailboxId());
                    }
                }
            }
        }

        // call the remote server now!
        Server targetServer = Provisioning.getInstance().get(ServerBy.name, server);
        String baseurl = null;
        if (!isMultipleMailboxes) {
            try {
                baseurl = URLUtil.getSoapURL(targetServer, false);
            } catch (ServiceException e) {
            }
        }
        if (baseurl == null) {
            baseurl = URLUtil.getAdminURL(targetServer, AdminConstants.ADMIN_SERVICE_URI, true);
        }
        ProxyTarget proxy = new ProxyTarget(targetServer, authToken, baseurl + qnrequest.getName());
        if (mTimeout != -1) {
            proxy.setTimeouts(mTimeout);
        }

        ZimbraSoapContext zscInbound = searchParams.getRequestContext();
        ZimbraSoapContext zscProxy;
        if (zscInbound != null) {
            zscProxy = new ZimbraSoapContext(zscInbound, targetAcctId);
        } else {
            zscProxy = new ZimbraSoapContext(authToken, targetAcctId,
                    responseProto, responseProto, searchParams.getHopCount() + 1);
        }

        long start = System.currentTimeMillis();
        Element searchResp = null;
        try {
            searchResp = DocumentHandler.proxyWithNotification(
                    searchElt, proxy, zscProxy, zscInbound);
        } catch (SoapFaultException sfe) {
            ZimbraLog.index.warn("Unable to (" + sfe + ") fetch search results from remote server " + proxy);
            atEndOfList = true;
            bufferEndOffset = iterOffset;
            return false;
        } catch (ServiceException e) {
            if (ServiceException.PROXY_ERROR.equals(e.getCode())) {
                ZimbraLog.index.warn("Unable to (" + e + ") fetch search results from remote server " + proxy);
                atEndOfList = true;
                bufferEndOffset = iterOffset;
                return false;
            }
            throw e;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            ZimbraLog.index.debug("Remote query took " + elapsed + "ms; URL=" + proxy.toString() + "; QUERY=" + searchElt.toString());
        }

        int hitOffset = (int) searchResp.getAttributeLong(MailConstants.A_QUERY_OFFSET);
        boolean hasMore = searchResp.getAttributeBool(MailConstants.A_QUERY_MORE);

        assert(bufferStartOffset == hitOffset);

        // put these hits into our buffer!
        int bufferIdx = 0;
        int stop = bufferEndOffset - bufferStartOffset;
        for (Iterator<Element> iter = searchResp.elementIterator();
            iter.hasNext() && bufferIdx < stop; ) {

            Element el = iter.next();
            if (el.getName().equalsIgnoreCase(MailConstants.E_INFO)) {
                for (Element info : el.listElements()) {
                    queryInfo.add(new ProxiedQueryInfo(info));
                }
            } else {
                hitBuffer.add(bufferIdx++, new ProxiedHit(this, el));
            }
        }

        // are we at the end of the line here?
        if (bufferIdx < stop || !hasMore) {
            // update the buffer-end-pointer
            bufferEndOffset = bufferStartOffset + bufferIdx;

            if (hasMore) {
                assert(!hasMore); // if bufferIdx < stop then !hasMore should be set...server bug!
            }
            atEndOfList = true;
        } else {
            assert(bufferEndOffset == bufferStartOffset+bufferIdx);
        }

        assert(bufferStartOffset <= iterOffset);

        // OK, we were successful if we managed to buffer the current hit
        return (bufferEndOffset > iterOffset);
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return queryInfo;
    }

}
