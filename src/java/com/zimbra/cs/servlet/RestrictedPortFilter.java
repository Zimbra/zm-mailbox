package com.zimbra.cs.servlet;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import com.zimbra.common.util.RemoteIP;
import com.zimbra.common.util.RemoteIP.TrustedIPs;
import com.zimbra.common.util.ZimbraLog;

public class RestrictedPortFilter implements Filter {

    private static final String PARAM_RESTRICTED_PORTS = "restricted.ports";
    private Set<Integer> restrictedPorts = new HashSet<Integer>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String portsParam = filterConfig.getInitParameter(PARAM_RESTRICTED_PORTS);
        if (!Strings.isNullOrEmpty(portsParam)) {
            for (String port: portsParam.split(",")) {
                try {
                    restrictedPorts.add(Integer.parseInt(port));
                    ZimbraLog.filter.debug("adding port %s to restricted ports", port);
                } catch (NumberFormatException e) {
                    ZimbraLog.filter.warn("invalid restricted port value: %s", port);
                }
            }
        }
    }


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            HttpServletResponse httpResp = (HttpServletResponse) response;
            TrustedIPs trustedIPs = ZimbraServlet.getTrustedIPs();
            RemoteIP remoteIp = new RemoteIP(httpReq, trustedIPs);
            Integer serverPort = request.getServerPort();
            String clientIp = remoteIp.getClientIP();
            if (restrictedPorts.contains(serverPort)) {
                if (!trustedIPs.isIpTrusted(clientIp)) {
                    httpResp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    ZimbraLog.security.warn("attempted request on port %d from non-trusted IP %s", serverPort, clientIp);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        ZimbraLog.filter.info("Destroying restricted port filter.");
    }
}
