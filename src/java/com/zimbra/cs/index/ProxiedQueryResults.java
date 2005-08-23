/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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
 */
package com.zimbra.cs.index;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.util.ParseMailboxID;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;
import com.zimbra.soap.SoapTransport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



/**
 * @author tim
 *
 * Represents the results of a query made on a remote server.  This class takes the ServerID
 * and the query parameters and makes does smart chunking (buffering) of the results - making
 * SOAP requests to the remote Zimbra server as necessary.
 * 
 *  
 * TODO wish this could be a QueryOperation subclass instead of just a results subclass:
 * unfortunately right now the intersection and other operations assume you can get to the
 * actual hit objects (for doing Intersections and the like)....  Long-term-fixme...
 */
public class ProxiedQueryResults extends ZimbraQueryResultsImpl 
{
    // magic # for parameter checking
    public static final int SEARCH_ALL_MAILBOXES = 1234;
    
    // number of hits to request each time we make a round-trip to the remote server
    protected static final int BUFFER_CHUNK_SIZE = 3; 
    
    protected ArrayList /* ProxiedHit */ mHitBuffer;
    protected int mBufferStartOffset = 0;  // inclusive
    protected int mBufferEndOffset = 0; // not-inclusive
    protected int mIterOffset = 0; // globally, NOT an index into the buffer
    protected boolean mAtEndOfList = false;
    
    protected String mServer;
    protected String mAuthToken;
    protected SoapTransport mTransport = null;
    SearchParams mSearchParams;
    
    // mailbox specifier
    boolean isMultipleMailboxes = false;
    boolean isAllMailboxes = false;
    List /*ParseMailboxID*/ mMailboxes;

    /**
     * A search request in the current mailbox on a different server
     * 
     * @param encodedAuthToken (call ZimbraContext.getAuthToken().getEncoded() if necessary) 
     * @param server hostname of server
     * @param params
     */
    public ProxiedQueryResults(String encodedAuthToken, String server, SearchParams params) {
        super(params.getTypes(), params.getSortBy());
        
        this.mSearchParams = params;
        this.mAuthToken = encodedAuthToken;
        this.mServer = server;
    }
    
    /**
     * An admin-only request to search ALL mailboxes on the remote server
     * 
     * @param encodedAuthToken (call ZimbraContext.getAuthToken().getEncoded() if necessary) 
     * @param server
     * @param params
     * @param searchAllMailboxes -- must be set to SEARCH_ALL_MAILBOXES
     */
    public ProxiedQueryResults(String encodedAuthToken, String server, SearchParams params, int searchAllMailboxes) {
        super(params.getTypes(), params.getSortBy());
    
        assert(searchAllMailboxes == SEARCH_ALL_MAILBOXES);
        
        this.mSearchParams = params;
        this.mAuthToken = encodedAuthToken;
        this.mServer = server;
        this.isMultipleMailboxes = true;
        this.isAllMailboxes = true; 
    }
    
    /**
     * An admin-only request to search a set of mailboxes on the remote server
     * 
     * @param encodedAuthToken (call ZimbraContext.getAuthToken().getEncoded() if necessary) 
     * @param server
     * @param params
     */
    public ProxiedQueryResults(String encodedAuthToken, String server, SearchParams params, List /*ParseMailboxID*/ mailboxes)
    {
        super(params.getTypes(), params.getSortBy());
    
        this.mSearchParams = params;
        this.mAuthToken = encodedAuthToken;
        this.mServer = server;
        this.isMultipleMailboxes = true;
        
        this.mMailboxes = mailboxes; 
    }

    
    public void doneWithSearchResults() throws ServiceException {
        mTransport = null;
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

    public void resetIterator() throws ServiceException {
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

    public ZimbraHit peekNext() throws ServiceException 
    {
        if (mIterOffset >= mBufferEndOffset) {
            if (!bufferNextHits()) {
                return null;
            }
        }
        return (ZimbraHit)mHitBuffer.get(mIterOffset - mBufferStartOffset);
    }
    
    String getServer() { return mServer; }
    
    
    
    private SoapTransport getTransport() throws ServiceException {
        if (mTransport != null) {
            return mTransport;
        }
        Server server = Provisioning.getInstance().getServerByName(mServer);
        String url = URLUtil.getMailURL(server, ZimbraServlet.USER_SERVICE_URI);
        SoapTransport toRet = new SoapHttpTransport(url);
        toRet.setAuthToken(mAuthToken);
        return toRet;
    }
    
    /**
     * Always does a request -- caller is responsible for checking to see if this is necessary or not
     * 
     * @return
     * @throws ServiceException
     */
    private boolean bufferNextHits() throws ServiceException 
    {
        if (mAtEndOfList) {
            return false;
        }
        
        mBufferStartOffset = mIterOffset;
        mBufferEndOffset = mBufferStartOffset + BUFFER_CHUNK_SIZE;
        mHitBuffer = new ArrayList(BUFFER_CHUNK_SIZE);
        
        try {
            SoapTransport transp = getTransport();
            
            Element searchElt;
            if (isMultipleMailboxes) {
                searchElt = new Element.XMLElement(AdminService.SEARCH_MULTIPLE_MAILBOXES_REQUEST);
            } else {
                searchElt = new Element.XMLElement(MailService.SEARCH_REQUEST);
            }
            
            searchElt.addAttribute(MailService.A_SEARCH_TYPES, "message");
            searchElt.addAttribute(MailService.A_SORTBY, mSearchParams.getSortByStr());
            searchElt.addAttribute(MailService.A_QUERY_OFFSET, mBufferStartOffset);
            searchElt.addAttribute(MailService.A_QUERY_LIMIT, mBufferEndOffset-mBufferStartOffset);

            searchElt.addAttribute(MailService.E_QUERY, mSearchParams.getQueryStr(), Element.DISP_CONTENT);
            
            if (isMultipleMailboxes) {
                if (isAllMailboxes) {
                    Element mbxElt = searchElt.addElement(MailService.E_MAILBOX);
                    ParseMailboxID id = ParseMailboxID.serverAll(mServer);
                    mbxElt.addAttribute(MailService.A_ID, id.getString()); 
                } else {
                    for (Iterator iter = mMailboxes.iterator(); iter.hasNext();) {
                        ParseMailboxID id = (ParseMailboxID)iter.next();
                        
                        //                    assert(!id.isLocal());
                        //                    assert(id.getServer().equals(serverID));
                        
                        searchElt.addElement(MailService.E_MAILBOX).addAttribute(MailService.A_ID, id.getString());
                    }
                }
            }
            
            // call the remote server now!
            Element searchResp = transp.invokeWithoutSession(searchElt);
            
            
            int hitOffset = (int) searchResp.getAttributeLong(MailService.A_QUERY_OFFSET);
            boolean hasMore = searchResp.getAttributeBool(MailService.A_QUERY_MORE);
            
            assert(mBufferStartOffset == hitOffset);
            
            // put these hits into our buffer!
            int bufferIdx = 0;
            int stop = mBufferEndOffset - mBufferStartOffset;
            for (Iterator iter = searchResp.elementIterator(MailService.E_MSG); iter.hasNext() && bufferIdx < stop;) {
                Element e = (Element)iter.next();
                mHitBuffer.add(bufferIdx++, new ProxiedHit(this, e));
            }
            
            //
            // are we at the end of the line here?
            //
            if ((bufferIdx < stop) || (!hasMore)) {
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
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException ", e);
        } catch (SoapFaultException e) {
            throw ServiceException.FAILURE("SoapFaultException ", e);
        } 
    }
    
    
}
