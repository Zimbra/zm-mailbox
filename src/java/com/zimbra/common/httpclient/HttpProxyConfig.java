package com.zimbra.common.httpclient;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.httpclient.HostConfiguration;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

public class HttpProxyConfig {

//    public static void configProxy(HttpClient client, String uriStr, String proxyHost, int proxyPort, String proxyUser, String proxyPass) {
//        if (proxyHost != null && proxyHost.length() > 0 && proxyPort > 0) {
//            client.getHostConfiguration().setProxy(proxyHost, proxyPort);
//            if (proxyUser != null && proxyUser.length() > 0 && proxyPass != null && proxyPass.length() > 0) {
//                client.getState().setProxyCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(proxyUser, proxyPass));
//            }
//        } else {
//            HostConfiguration config = getProxyConfig(uriStr);
//            if (config != null)
//                client.getHostConfiguration().setProxy(config.getProxyHost(), config.getProxyPort());
//        }
//    }
    
    public static HostConfiguration getProxyConfig(String uriStr) {
        if (!LC.httpclient_use_system_proxy.booleanValue())
            return null;
        
        URI uri = null;
        try {
            uri = new URI(uriStr);
        } catch (URISyntaxException x) {
            ZimbraLog.misc.info(uriStr, x);
            return null;
        }
        
        try {
            if (InetAddress.getByName(uri.getHost()).isLoopbackAddress())
                return null;
        } catch (UnknownHostException x) {
            ZimbraLog.misc.info(uriStr, x);
            return null;
        }
        
        List<Proxy> proxies = ProxySelector.getDefault().select(uri);
        for (Proxy proxy : proxies) {
            switch (proxy.type()) {
            case DIRECT:
                return null;
            case HTTP:
                InetSocketAddress addr = (InetSocketAddress)proxy.address();
                ZimbraLog.misc.debug("URI %s to use HTTP proxy %s", uriStr, addr.toString());
                HostConfiguration config = new HostConfiguration();
                config.setProxy(addr.getHostName(), addr.getPort());
                return config;
            case SOCKS: //socks proxy can be handled at socket factory level
            default:
                continue;
            }
        }
        return null;
    }
}
