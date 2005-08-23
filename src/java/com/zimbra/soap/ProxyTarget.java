/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on 2005. 3. 3.
 */
package com.zimbra.soap;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;

/**
 * @author jhahm
 */
public class ProxyTarget {

    private Server mServer;
    private String mAuthToken;
    private String mURL;

    public ProxyTarget(String serverId, String authToken, HttpServletRequest req) throws ServiceException {
        mServer = Provisioning.getInstance().getServerById(serverId);
        if (mServer == null)
            throw AccountServiceException.NO_SUCH_SERVER(serverId);

        mAuthToken = authToken;

        // Same URL as the incoming request, except hostname is that of target server.
        StringBuffer url = new StringBuffer(req.getScheme());
        url.append("://").append(mServer.getAttr(Provisioning.A_zimbraServiceHostname));
        url.append(':').append(req.getServerPort());
        url.append(req.getRequestURI());
        String qs = req.getQueryString();
        if (qs != null)
            url.append('?').append(qs);
        mURL = url.toString();
    }

    public boolean isTargetLocal() throws ServiceException {
    	Server localServer = Provisioning.getInstance().getLocalServer();
        return mServer.equals(localServer);
    }

    public Element dispatch(Element request) throws ServiceException {
        Element response = null;
        SoapHttpTransport transport = null;
        try {
        	transport = new SoapHttpTransport(mURL);
            transport.setAuthToken(mAuthToken);
        	response = transport.invokeWithoutSession(request);
        } catch (IOException e) {
            throw ServiceException.PROXY_ERROR(e);
        } catch (SoapFaultException e) {
            throw ServiceException.PROXY_ERROR(e);
        } finally {
            if (transport != null)
                transport.shutdown();
        }
    	return response;
    }
}
