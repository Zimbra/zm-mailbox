package com.zimbra.znative;

import java.util.List;

public class ProxyInfo {
    private final Type type;
    private final String host;
    private final int port;
    private final String user;
    private final String pass;

    public enum Type {
        NONE, AUTO_CONFIG_URL, FTP, HTTP, HTTPS, SOCKS, UNKNOWN
    }

    static {
        Util.loadLibrary();
    }

    public static native boolean isSupported();
    public static native ProxyInfo[] getProxyInfo(String uri);

    ProxyInfo(int type, String host, int port, String user, String pass) {
        this.type = Type.values()[type];
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
    }

    public Type getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public String toString() {
        return String.format("{type=%s,host=%s,port=%d,user=%s,pass=%s}",
                             type, host, port, user, pass);
    }
    
    public static void main(String[] args) {
        String url = args.length > 0 ? args[0] : "http://www.news.com";
        System.out.printf("Proxy information for %s :\n", url);
        ProxyInfo[] proxies = getProxyInfo(url);
        for (int i = 0; i < proxies.length; i++) {
            System.out.printf("proxy[%d] = %s\n", i, proxies[i]);
        }
    }
}
