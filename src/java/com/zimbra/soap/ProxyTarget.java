/*
 * Created on 2005. 3. 3.
 */
package com.zimbra.soap;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.liquidsys.coco.account.AccountServiceException;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.account.Server;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;

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
        url.append("://").append(mServer.getAttr(Provisioning.A_liquidServiceHostname));
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
