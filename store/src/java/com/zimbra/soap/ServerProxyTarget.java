package com.zimbra.soap;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;

public class ServerProxyTarget extends ProxyTarget {

    private Server mServer;

    public ServerProxyTarget(String serverId, AuthToken authToken, HttpServletRequest req) throws ServiceException {
        super(authToken, req);
        Provisioning prov = Provisioning.getInstance();
        mServer = prov.get(Key.ServerBy.id, serverId);
        if (mServer == null) {
            throw AccountServiceException.NO_SUCH_SERVER(serverId);
        }
    }

    public ServerProxyTarget(Server server, AuthToken authToken, String url) {
        super(authToken, url);
        mServer = server;
    }

    public Server getServer() {
        return mServer;
    }

    @Override
    public boolean isTargetLocal() throws ServiceException {
        Server localServer = Provisioning.getInstance().getLocalServer();
        return mServer.getId().equals(localServer.getId());
    }

    @Override
    protected boolean isAdminRequest() {
        return !Provisioning.getInstance().isOfflineProxyServer(mServer) && super.isAdminRequest();
    }

    @Override
    protected String buildServiceUrl(String requestStr) throws ServiceException {
        return URLUtil.getServiceURL(mServer, requestStr, false);
    }

    @Override
    protected String buildAdminUrl(String requestStr) throws ServiceException {
        return URLUtil.getAdminURL(mServer, requestStr, true);
    }
}
