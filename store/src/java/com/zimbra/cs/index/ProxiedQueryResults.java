/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.index;

import com.google.common.base.MoreObjects;
import com.zimbra.common.account.Key;
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
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ProxyTarget;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    private boolean singleShotRemoteRequest = false;

    /**
     * read timeout for the proxy SOAP request. -1 mean use default SOAP http client timeout.
     */
    private long mTimeout = -1;

    private List<QueryInfo> queryInfo = new ArrayList<QueryInfo>();
    private boolean relevanceSortSupported = true;

    /**
     * A search request in the current mailbox on a different server.
     *
     * The query string for this search is the string passed in the queryString
     * parameter, *not* the query string in the params.
     *
     * @param server hostname of server
     * @param queryString queryString to use for this search
     */
    public ProxiedQueryResults(SoapProtocol respProto, AuthToken authToken, String targetAccountId, String server,
            SearchParams params, String queryString, SearchParams.Fetch fetch) {
        super(params.getTypes(), params.getSortBy(), fetch);
        setSearchParams(params, queryString);
        this.authToken = authToken;
        this.server = server;
        this.targetAcctId = targetAccountId;
        this.responseProto = respProto;
    }

    /**
     * A search request in the current mailbox on a different server.
     *
     * @param encodedAuthToken (call ZimbraContext.getAuthToken().getEncoded() if necessary)
     * @param server hostname of server
     */
    public ProxiedQueryResults(SoapProtocol respProto, AuthToken authToken, String server, SearchParams params,
            SearchParams.Fetch fetch) {
        super(params.getTypes(), params.getSortBy(), fetch);
        setSearchParams(params);
        this.authToken = authToken;
        this.server = server;
        this.responseProto = respProto;
    }

    private void setSearchParams(SearchParams params) {
        searchParams = (SearchParams) params.clone();
        if ((searchParams.getCursor() != null) && (searchParams.getLimit() > 0) && (searchParams.getLimit() < 500)) {
            SortBy sb = getSortBy();
            // Using a fairly restrictive match in order to reduce scope of "singleShot" change
            if (sb != null && ((SortBy.NAME_LOCALIZED_ASC.equals(sb)) || (SortBy.NAME_LOCALIZED_DESC.equals(sb)) ||
                (SortBy.NAME_ASC.equals(sb)) || (SortBy.NAME_DESC.equals(sb)))) {
                singleShotRemoteRequest = true;
                if (searchParams.getLimit() > 0) {
                    // Go 1 beyond what we need so that 'more' setting will be correct.
                    searchParams.setLimit(searchParams.getLimit() + 1);
                }
            }
        }
        if (!singleShotRemoteRequest) {
            searchParams.setCursor(null);
            // when doing offset-paging, since we do a mergesort locally, the remote query must start
            // at offset 0 and page through all the results
            searchParams.setOffset(0);
        }
    }

    private void setSearchParams(SearchParams params, String queryString) {
        setSearchParams(params);
        searchParams.setQueryString(queryString);
    }

    public void setTimeout(long timeout) {
        mTimeout = timeout;
    }

    @Override
    public long getCursorOffset() {
        return -1;
    }

    @Override
    public void close() {
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
            url = URLUtil.getAdminURL(Provisioning.getInstance().get(Key.ServerBy.name, server));
        } catch (ServiceException ex) {
            url = server;
        }
        return MoreObjects.toStringHelper(this).add("url", url).add("acctId", targetAcctId).toString();
    }

    /**
     * Always does a request -- caller is responsible for checking to see if this is necessary or not
     */
    private boolean bufferNextHits() throws ServiceException {
        if (atEndOfList || searchParams.getHopCount() > ZimbraSoapContext.MAX_HOP_COUNT) {
            return false;
        }

        bufferStartOffset = iterOffset;

        int chunkSizeToUse;
        if (singleShotRemoteRequest) {
            chunkSizeToUse = searchParams.getLimit();
        } else {
            chunkSizeToUse = searchParams.getLimit() * 2;
            if (chunkSizeToUse < MIN_BUFFER_CHUNK_SIZE) {
                chunkSizeToUse = MIN_BUFFER_CHUNK_SIZE;
            }
            if (chunkSizeToUse > 500) {
                chunkSizeToUse = 500;
            }
        }

        bufferEndOffset = bufferStartOffset + chunkSizeToUse;
        hitBuffer = new ArrayList<ProxiedHit>(chunkSizeToUse);

        Element searchElt = Element.create(responseProto, MailConstants.SEARCH_REQUEST);

        searchParams.setOffset(bufferStartOffset);
        searchParams.setLimit(chunkSizeToUse);
        searchParams.encodeParams(searchElt);
        if (singleShotRemoteRequest && (searchParams.getCursor() != null)) {
            Element cursorElt = searchElt.addElement(MailConstants.E_CURSOR);
            cursorElt.addAttribute(MailConstants.A_ID, searchParams.getCursor().getItemId().getId());
            if (searchParams.getCursor().getSortValue() != null) {
                cursorElt.addAttribute(MailConstants.A_SORTVAL, searchParams.getCursor().getSortValue());
            }
            if (searchParams.getCursor().getEndSortValue() != null) {
                cursorElt.addAttribute(MailConstants.A_ENDSORTVAL, searchParams.getCursor().getEndSortValue());
            }
        }

        // call the remote server now!
        Server targetServer = Provisioning.getInstance().get(Key.ServerBy.name, server);
        String baseurl = null;
        try {
            baseurl = URLUtil.getSoapURL(targetServer, false);
        } catch (ServiceException e) {
        }
        if (baseurl == null) {
            baseurl = URLUtil.getAdminURL(targetServer, AdminConstants.ADMIN_SERVICE_URI, true);
        }
        ProxyTarget proxy = new ProxyTarget(targetServer, authToken, baseurl + MailConstants.SEARCH_REQUEST.getName());
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

        int hitOffset;
        if (singleShotRemoteRequest) {
            hitOffset = (int) searchResp.getAttributeLong(MailConstants.A_QUERY_OFFSET, bufferStartOffset);
        } else {
            hitOffset = (int) searchResp.getAttributeLong(MailConstants.A_QUERY_OFFSET);
        }
        boolean hasMore = searchResp.getAttributeBool(MailConstants.A_QUERY_MORE);

        relevanceSortSupported = searchResp.getAttributeBool(MailConstants.A_RELEVANCE_SORT_SUPPORTED, true);

        assert(bufferStartOffset == hitOffset);

        SortBy sb = getSortBy();
        // put these hits into our buffer!
        int bufferIdx = 0;
        int stop = bufferEndOffset - bufferStartOffset;
        for (Iterator<Element> iter = searchResp.elementIterator(); iter.hasNext() && bufferIdx < stop; ) {
            Element el = iter.next();
            if (el.getName().equalsIgnoreCase(MailConstants.E_INFO)) {
                for (Element info : el.listElements()) {
                    queryInfo.add(new ProxiedQueryInfo(info));
                }
            } else {
                if (sb != null && ((SortBy.NAME_LOCALIZED_ASC.equals(sb)) || (SortBy.NAME_LOCALIZED_DESC.equals(sb)))) {
                    hitBuffer.add(bufferIdx++,
                            new ProxiedContactHit(this, el, el.getAttribute(MailConstants.A_FILE_AS_STR)));
                } else {
                    hitBuffer.add(bufferIdx++, new ProxiedHit(this, el, el.getAttribute(MailConstants.A_SORT_FIELD)));
                }
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
        if (singleShotRemoteRequest) {
            atEndOfList = true;
        }

        assert(bufferStartOffset <= iterOffset);

        // OK, we were successful if we managed to buffer the current hit
        return (bufferEndOffset > iterOffset);
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return queryInfo;
    }

    /**
     * The results of the proxied search are added to the hit array in the same order as in the proxied search
     * response - so there should be no need to sort the results again.
     */
    @Override
    public boolean isPreSorted() {
        return true;
    }

    @Override
    public boolean isRelevanceSortSupported() {
        return relevanceSortSupported;
    }
}
