/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Mar 28, 2005
 */
package com.zimbra.cs.index;

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
 * Represents the results of a query made on a remote server.  This class takes the ServerID
 * and the query parameters and makes does smart chunking (buffering) of the results - making
 * SOAP requests to the remote Zimbra server as necessary.
 *
 * TODO wish this could be a QueryOperation subclass instead of just a results subclass:
 * unfortunately right now the intersection and other operations assume you can get to the
 * actual hit objects (for doing Intersections and the like)....  Long-term-fixme...
 */
public class ProxiedQueryResults extends ZimbraQueryResultsImpl 
{
    // magic # for parameter checking
    public static final int SEARCH_ALL_MAILBOXES = 1234;

    // minimum number of hits to request each time we make a round-trip to the remote server
    protected static final int MIN_BUFFER_CHUNK_SIZE = 25; 

    protected ArrayList<ProxiedHit> mHitBuffer;
    protected int mBufferStartOffset = 0;  // inclusive
    protected int mBufferEndOffset = 0; // not-inclusive
    protected int mIterOffset = 0; // globally, NOT an index into the buffer
    protected boolean mAtEndOfList = false;

    protected String mServer;
    protected AuthToken mAuthToken;
    protected String mTargetAcctId = null;
//    protected SoapTransport mTransport = null;
    protected SoapProtocol mResponseProto = null;

    private SearchParams mSearchParams;

    // mailbox specifier
    private boolean isMultipleMailboxes = false;
    private boolean isAllMailboxes = false;
    private List<ParseMailboxID> mMailboxes;
    
    // read timeout for the proxy SOAP request
    // -l mean use default SOAP http client timeout
    private long mTimeout = -1;
    
    private void setSearchParams(SearchParams params) {
        this.mSearchParams = (SearchParams)params.clone();
        mSearchParams.clearCursor();
        // when doing offset-paging, since we do a mergesort locally, the remote query must start
        // at offset 0 and page through all the results
        mSearchParams.setOffset(0); 
    }
    
    public void setTimeout(long timeout) {
        mTimeout = timeout;
    }

    /**
     * A search request in the current mailbox on a different server
     * 
     * @param encodedAuthToken (call ZimbraContext.getAuthToken().getEncoded() if necessary) 
     * @param server hostname of server
     * @param params
     */
    public ProxiedQueryResults(SoapProtocol respProto, AuthToken authToken, String targetAccountId, String server, SearchParams params, Mailbox.SearchResultMode mode) {
        super(params.getTypes(), params.getSortBy(), mode);

        setSearchParams(params);
        this.mAuthToken = authToken;
        this.mServer = server;
        this.mTargetAcctId = targetAccountId;
        this.mResponseProto = respProto;
    }

    /**
     * A search request in the current mailbox on a different server
     * 
     * @param encodedAuthToken (call ZimbraContext.getAuthToken().getEncoded() if necessary) 
     * @param server hostname of server
     * @param params
     */
    public ProxiedQueryResults(SoapProtocol respProto, AuthToken authToken, String server, SearchParams params, Mailbox.SearchResultMode mode) {
        super(params.getTypes(), params.getSortBy(), mode);

        setSearchParams(params);
        mSearchParams.setOffset(0); 
        this.mAuthToken = authToken;
        this.mServer = server;
        this.mResponseProto = respProto;
    }

    /**
     * An admin-only request to search ALL mailboxes on the remote server
     * 
     * @param encodedAuthToken (call ZimbraContext.getAuthToken().getEncoded() if necessary) 
     * @param server
     * @param params
     * @param searchAllMailboxes -- must be set to SEARCH_ALL_MAILBOXES
     */
    public ProxiedQueryResults(SoapProtocol respProto, AuthToken authToken, String server, SearchParams params, Mailbox.SearchResultMode mode, int searchAllMailboxes) {
        super(params.getTypes(), params.getSortBy(), mode);

        assert(searchAllMailboxes == SEARCH_ALL_MAILBOXES);

        setSearchParams(params);
        this.mAuthToken = authToken;
        this.mServer = server;
        this.isMultipleMailboxes = true;
        this.isAllMailboxes = true; 
        this.mResponseProto = respProto;
    }

    /**
     * An admin-only request to search a set of mailboxes on the remote server
     * 
     * @param encodedAuthToken (call ZimbraContext.getAuthToken().getEncoded() if necessary) 
     * @param server
     * @param params
     */
    public ProxiedQueryResults(SoapProtocol respProto, AuthToken authToken, String server, SearchParams params, Mailbox.SearchResultMode mode, List<ParseMailboxID> mailboxes)
    {
        super(params.getTypes(), params.getSortBy(), mode);

        setSearchParams(params);
        this.mAuthToken = authToken;
        this.mServer = server;
        this.isMultipleMailboxes = true;
        this.mResponseProto = respProto;

        this.mMailboxes = mailboxes; 
    }


    public void doneWithSearchResults() {
//        mTransport = null;
    }

    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        mIterOffset = hitNo;

        if (mIterOffset < mBufferStartOffset || mIterOffset > mBufferEndOffset) {

            // special-case: if we are already at the end of the list AND they want to
            // go even further, then we can just drop out
            if (mIterOffset > mBufferEndOffset && mAtEndOfList) { 
                return null; // no hit
            }

            // not in current buffer...clear the buffer
            mBufferStartOffset = mIterOffset;
            mBufferEndOffset = mIterOffset;
            mHitBuffer = null;
            mAtEndOfList = false;
        } 

        // now either in current buffer, or current buffer is empty -- getNext will do the rest
        return getNext();
    }

    public void resetIterator() {
        mIterOffset = 0;
        mAtEndOfList = false;
    }

    public ZimbraHit getNext() throws ServiceException {
        ZimbraHit retVal = peekNext();
        if (retVal != null) {
            mIterOffset++;
        }
        return retVal;
    }

    public ZimbraHit peekNext() throws ServiceException {
        if (mIterOffset >= mBufferEndOffset) {
            if (!bufferNextHits()) {
                return null;
            }
        }
        return mHitBuffer.get(mIterOffset - mBufferStartOffset);
    }

    String getServer() { return mServer; }


    @Override public String toString() {
        String url;
        try {
            Server server = Provisioning.getInstance().get(ServerBy.name, mServer);
            url = URLUtil.getAdminURL(server);
        } catch (ServiceException ex) {
            url = mServer; 
        }
        
        return "ProxiedQueryResults(url="+url+", acctId="+mTargetAcctId+")";
    }

    /**
     * Always does a request -- caller is responsible for checking to see if this is necessary or not
     * 
     * @return
     * @throws ServiceException
     */
    private boolean bufferNextHits() throws ServiceException {
        if (mAtEndOfList || mSearchParams.getHopCount() > ZimbraSoapContext.MAX_HOP_COUNT)
            return false;

        mBufferStartOffset = mIterOffset;

        int chunkSizeToUse = mSearchParams.getLimit() * 2;
        if (chunkSizeToUse < MIN_BUFFER_CHUNK_SIZE)
            chunkSizeToUse = MIN_BUFFER_CHUNK_SIZE;
        if (chunkSizeToUse > 500)
            chunkSizeToUse = 500;

        mBufferEndOffset = mBufferStartOffset + chunkSizeToUse;
        mHitBuffer = new ArrayList<ProxiedHit>(chunkSizeToUse);

        QName qnrequest = (isMultipleMailboxes ? AdminConstants.SEARCH_MULTIPLE_MAILBOXES_REQUEST : MailConstants.SEARCH_REQUEST);
        Element searchElt = Element.create(mResponseProto, qnrequest);

        mSearchParams.setOffset(mBufferStartOffset);
        mSearchParams.setLimit(chunkSizeToUse);
        mSearchParams.encodeParams(searchElt);

        if (isMultipleMailboxes) {
            if (isAllMailboxes) {
                Element mbxElt = searchElt.addElement(MailConstants.E_MAILBOX);
                ParseMailboxID id = ParseMailboxID.serverAll(mServer);
                mbxElt.addAttribute(MailConstants.A_ID, id.getString());
            } else {
                for (ParseMailboxID id : mMailboxes) {
                    // assert(!id.isLocal());
                    // assert(id.getServer().equals(serverID));
                    searchElt.addElement(MailConstants.E_MAILBOX).addAttribute(MailConstants.A_NAME, id.getEmailAddress());
                }
            }
        }

        // call the remote server now!
        Server server = Provisioning.getInstance().get(ServerBy.name, mServer);
        String baseurl = null;
        if (!isMultipleMailboxes) {
            try {
                baseurl = URLUtil.getSoapURL(server, false);
            } catch (ServiceException e) { }
        }
        if (baseurl == null)
            baseurl = URLUtil.getAdminURL(server, AdminConstants.ADMIN_SERVICE_URI, true);
        ProxyTarget proxy = new ProxyTarget(server, mAuthToken, baseurl + qnrequest.getName());
        if (mTimeout != -1)
            proxy.setTimeouts(mTimeout);

        ZimbraSoapContext zscProxy, zscInbound = mSearchParams.getRequestContext();
        if (zscInbound != null)
            zscProxy = new ZimbraSoapContext(zscInbound, mTargetAcctId);
        else
            zscProxy = new ZimbraSoapContext(mAuthToken, mTargetAcctId, mResponseProto, mResponseProto, mSearchParams.getHopCount() + 1);

        long start = System.currentTimeMillis();
        Element searchResp = null;
        try {
            searchResp = DocumentHandler.proxyWithNotification(searchElt, proxy, zscProxy, zscInbound);
        } catch (SoapFaultException sfe) {
            ZimbraLog.index.warn("Unable to ("+sfe.toString()+") fetch search results from remote server " + proxy);
            mAtEndOfList = true;
            mBufferEndOffset = mIterOffset;
            return false;
        } catch (ServiceException e) {
            if (ServiceException.PROXY_ERROR.equals(e.getCode())) {
                ZimbraLog.index.warn("Unable to ("+e.toString()+") fetch search results from remote server " + proxy);
                mAtEndOfList = true;
                mBufferEndOffset = mIterOffset;
                return false;
            }
            throw e;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            ZimbraLog.index.debug("Remote query took " + elapsed + "ms; URL=" + proxy.toString() + "; QUERY=" + searchElt.toString());
        }

        int hitOffset = (int) searchResp.getAttributeLong(MailConstants.A_QUERY_OFFSET);
        boolean hasMore = searchResp.getAttributeBool(MailConstants.A_QUERY_MORE);

        assert(mBufferStartOffset == hitOffset);

        // put these hits into our buffer!
        int bufferIdx = 0;
        int stop = mBufferEndOffset - mBufferStartOffset;
        for (Iterator<Element> iter = searchResp.elementIterator(); iter.hasNext() && bufferIdx < stop; ) {
            Element e = iter.next();
            if (e.getName().equalsIgnoreCase(MailConstants.E_INFO)) {
                for (Element info : e.listElements())  
                    mInfo.add(new ProxiedQueryInfo(info));
            } else {
                mHitBuffer.add(bufferIdx++, new ProxiedHit(this, e));
            }
        }

        //
        // are we at the end of the line here?
        //
        if (bufferIdx < stop || !hasMore) {
            // update the buffer-end-pointer 
            mBufferEndOffset = mBufferStartOffset + bufferIdx;

            if (hasMore) {
                assert(!hasMore); // if bufferIdx < stop then !hasMore should be set...server bug!
            }
            this.mAtEndOfList = true;
        } else {
            assert(mBufferEndOffset == mBufferStartOffset+bufferIdx);
        }

        assert(mBufferStartOffset <= mIterOffset);

        // OK, we were successful if we managed to buffer the current hit
        return (mBufferEndOffset > mIterOffset);

//      catch (ServiceException e) {
//      if (!isMultipleMailboxes) {
//      //throw ServiceException.FAILURE("ServiceException ", e);
//      e.setArgument(ServiceException.PROXIED_FROM_ACCT, mTargetAcctId);
//      }
//      throw e;
//      }
    }

    private List<QueryInfo> mInfo = new ArrayList<QueryInfo>();
    public List<QueryInfo> getResultInfo() { return mInfo; }

    public int estimateResultSize() { return 0; }
}
