package com.zimbra.soap;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.httpclient.URLUtil;

public class IpProxyTarget extends ProxyTarget {

    private String ip;

    public IpProxyTarget(String targetIp, AuthToken authToken, HttpServletRequest req) throws ServiceException {
        super(authToken, req);
        this.ip = targetIp;
    }
    
    public IpProxyTarget(String targetIp, AuthToken authToken, String url) {
        super(authToken, url);
        this.ip = targetIp;
    }

    public String getIp() {
        return ip;
    }

    @Override
    public boolean isTargetLocal() {
        return Provisioning.isMyIpAddress(ip);
    }

    @Override
    protected String buildServiceUrl(String requestStr) throws ServiceException {
        return URLUtil.getServiceURL(ip, requestPort, requestStr, false);
    }

    @Override
    protected String buildAdminUrl(String requestStr) throws ServiceException {
        return URLUtil.getAdminURL(ip, requestPort, requestStr, true);
    }
}
