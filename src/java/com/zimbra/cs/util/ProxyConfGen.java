/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.util;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.ServerBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.service.ServiceException;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.extension.ExtensionDispatcherServlet;
import com.zimbra.cs.util.BuildInfo;

enum ProxyConfOverride {
    NONE,
    CONFIG,
    SERVER,
    LOCALCONFIG,
    CUSTOM;
};

enum ProxyConfValueType {
    INTEGER,
    LONG,
    STRING,
    BOOLEAN,
    ENABLER,
    TIME,
    CUSTOM;
};

@SuppressWarnings("serial")
class ProxyConfException extends Exception {
    public ProxyConfException (String msg) {
        super(msg);
    }

    public ProxyConfException (String msg, Throwable cause) {
        super(msg,cause);
    }
}

class ProxyConfVar
{
    public String                   mKeyword;
    public String                   mAttribute;
    public ProxyConfValueType       mValueType;
    public Object                   mDefault;
    public Object                   mValue;
    public ProxyConfOverride        mOverride;
    public String                   mDescription;

    protected static Log              mLog = LogFactory.getLog (ProxyConfGen.class);
    protected static Provisioning     mProv = Provisioning.getInstance();
    public static Entry             configSource = null;
    public static Entry             serverSource = null;

    public ProxyConfVar(String keyword, String attribute, Object defaultValue,
            ProxyConfValueType valueType, ProxyConfOverride overrideType,
            String description) {
        mKeyword = keyword;
        mAttribute = attribute;
        mValueType = valueType;
        mDefault = defaultValue;
        mOverride = overrideType;
        mValue = mDefault;
        mDescription = description;
    }

    public String confValue () throws ProxyConfException
    {
        return format(mValue);
    }

    public Object rawValue ()
    {
        return mValue;
    }

    public void write (PrintStream ps) throws ProxyConfException
    {
        ps.println ("  NGINX Keyword:         " + mKeyword);
        ps.println ("  Description:           " + mDescription);
        ps.println ("  Value Type:            " + mValueType.toString());
        ps.println ("  Controlling Attribute: " + ((mAttribute == null) ? "(none)" : mAttribute));
        ps.println ("  Default Value:         " + mDefault.toString());
        ps.println ("  Current Value:         " + mValue.toString());
        ps.println ("  Config Text:           " + format(mValue));
        ps.println ("");
    }

    /* Update internal value depending upon config source and data type */
    public void update () throws ServiceException, ProxyConfException
    {
        if (mOverride == ProxyConfOverride.NONE) {
            return;
        }

        if (mValueType == ProxyConfValueType.INTEGER) {
            updateInteger();
        } else if (mValueType == ProxyConfValueType.LONG) {
            updateLong();
        } else if (mValueType == ProxyConfValueType.STRING) {
            updateString();
        } else if (mValueType == ProxyConfValueType.BOOLEAN) {
            updateBoolean();
        } else if (mValueType == ProxyConfValueType.ENABLER) {
            updateEnabler();
        } else if (mValueType == ProxyConfValueType.TIME) {
            updateTime();
        } else if (mValueType == ProxyConfValueType.CUSTOM) {
           
            /* should always use override to define the custom update method */
            throw new ProxyConfException(
                    "the custom update of ProxyConfVar with key " + mKeyword
                            + " has to be implementated by override");
        }
    }

    public String format (Object o) throws ProxyConfException
    {
        if (mValueType == ProxyConfValueType.INTEGER) {
            return formatInteger(o);
        } else if (mValueType == ProxyConfValueType.LONG) {
            return formatLong(o);
        } else if (mValueType == ProxyConfValueType.STRING) {
            return formatString(o);
        } else if (mValueType == ProxyConfValueType.BOOLEAN) {
            return formatBoolean(o);
        } else if (mValueType == ProxyConfValueType.ENABLER) {
            return formatEnabler(o);
        } else if (mValueType == ProxyConfValueType.TIME) {
            return formatTime(o);
        } else /* if (mValueType == ProxyConfValueType.CUSTOM) */ {
            throw new ProxyConfException(
                    "the custom format of ProxyConfVar with key " + mKeyword
                            + " has to be implementated by override");
        }
    }

    public void updateString() {
        if (mOverride == ProxyConfOverride.CONFIG) {
            mValue = configSource.getAttr(mAttribute, (String) mDefault);
        } else if (mOverride == ProxyConfOverride.LOCALCONFIG) {
            mValue = lcValue(mAttribute, (String) mDefault);
        } else if (mOverride == ProxyConfOverride.SERVER) {
            mValue = serverSource.getAttr(mAttribute, (String) mDefault);
        }
    }

    public String formatString(Object o) {
        Formatter f = new Formatter();
        f.format("%s", o);
        return f.toString();
    }

    public void updateBoolean() {
        if (mOverride == ProxyConfOverride.CONFIG) {
            mValue = configSource
                    .getBooleanAttr(mAttribute, (Boolean) mDefault);
        } else if (mOverride == ProxyConfOverride.LOCALCONFIG) {
            mValue = Boolean.valueOf(lcValue(mAttribute, mDefault.toString()));
        } else if (mOverride == ProxyConfOverride.SERVER) {
            mValue = serverSource
                    .getBooleanAttr(mAttribute, (Boolean) mDefault);
        }
    }

    public String formatBoolean(Object o) {
        if ((Boolean) o)
            return "on";
        return "off";
    }

    public void updateEnabler() {
        updateBoolean();
    }

    public String formatEnabler(Object o) {
        if ((Boolean) o)
            return "";
        return "#";
    }

    public void updateTime() {
        if (mOverride == ProxyConfOverride.CONFIG) {
            mValue = new Long(configSource.getTimeInterval(mAttribute,
                    (Long) mDefault));
        } else if (mOverride == ProxyConfOverride.LOCALCONFIG) {
            mValue = new Long(DateUtil.getTimeInterval(
                    lcValue(mAttribute, mDefault.toString()),
                    ((Long) mDefault).longValue()));
        } else if (mOverride == ProxyConfOverride.SERVER) {
            mValue = new Long(serverSource.getTimeInterval(mAttribute,
                    (Long) mDefault));
        }
    }

    public String formatTime(Object o) {
        Formatter f = new Formatter();
        f.format("%dms", (Long) o);
        return f.toString();
    }

    public void updateInteger() {
        if (mOverride == ProxyConfOverride.CONFIG) {
            mValue = new Integer(configSource.getIntAttr(mAttribute,
                    (Integer) mDefault));
        } else if (mOverride == ProxyConfOverride.LOCALCONFIG) {
            mValue = Integer.valueOf(lcValue(mAttribute, mDefault.toString()));
        } else if (mOverride == ProxyConfOverride.SERVER) {
            mValue = new Integer(serverSource.getIntAttr(mAttribute,
                    (Integer) mDefault));
        }
    }

    public String formatInteger(Object o) {
        Formatter f = new Formatter();
        f.format("%d", (Integer) o);
        return f.toString();
    }

    public void updateLong() {
        if (mOverride == ProxyConfOverride.CONFIG) {
            mValue = new Long(configSource.getLongAttr(mAttribute,
                    (Long) mDefault));
        } else if (mOverride == ProxyConfOverride.LOCALCONFIG) {
            mValue = Long.valueOf(lcValue(mAttribute, mDefault.toString()));
        } else if (mOverride == ProxyConfOverride.SERVER) {
            mValue = new Long(serverSource.getLongAttr(mAttribute,
                    (Long) mDefault));
        }
    }

    public String formatLong(Object o) {
        Formatter f = new Formatter();
        Long l = (Long) o;

        if (l % (1024 * 1024) == 0)
            f.format("%dm", l / (1024 * 1024));
        else if (l % 1024 == 0)
            f.format("%dk", l / 1024);
        else
            f.format("%d", l);
        return f.toString();
    }

    private String lcValue(String key, String def) {
        String val = LC.get(key);

        return val == null || val.length() == 0 ? def : val;
    }
}

abstract class WebEnablerVar extends ProxyConfVar {
    
    public WebEnablerVar(String keyword, Object defaultValue,
            String description) {
        super(keyword, null, defaultValue, 
                ProxyConfValueType.ENABLER, ProxyConfOverride.CUSTOM, description);
    }
    
    private static String currentMailMode;
    
    static String getZimbraReverseProxyMailMode() {
        if (WebEnablerVar.currentMailMode == null) {
            WebEnablerVar.currentMailMode = 
                serverSource.getAttr(Provisioning.A_zimbraReverseProxyMailMode, "both");
        }
        return WebEnablerVar.currentMailMode;
    }
}

class HttpEnablerVar extends WebEnablerVar {
    
    public HttpEnablerVar() {
        super("web.http.enabled", true, 
                "Indicates whether HTTP Proxy will accept connections on HTTP " +
                "(true unless zimbraReverseProxyMailMode is 'https')");
    }
    
    @Override
    public void update() {
        String mailmode = getZimbraReverseProxyMailMode();
        if ("https".equalsIgnoreCase(mailmode)) {
             mValue = false;
        } else {
             mValue = true;
        }
    }   
}

class HttpsEnablerVar extends WebEnablerVar {
    public HttpsEnablerVar() {
        super("web.https.enabled", true, 
                "Indicates whether HTTP Proxy will accept connections on HTTPS " +
                "(true unless zimbraReverseProxyMailMode is 'http')");
    }
    @Override
    public void update() {
        String mailmode = getZimbraReverseProxyMailMode();
        if ("http".equalsIgnoreCase(mailmode)) {
            mValue = false;
        } else {
            mValue = true;
        }
    }   
}

abstract class IPModeEnablerVar extends ProxyConfVar {
    public IPModeEnablerVar(String keyword,
            Object defaultValue, 
             String description) {
        super(keyword, null, defaultValue, ProxyConfValueType.ENABLER, 
                ProxyConfOverride.CUSTOM, description);
        // TODO Auto-generated constructor stub
    }
    private static IPMode currentIPMode = IPMode.UNKNOWN;
    
    static enum IPMode {
        UNKNOWN,
        BOTH,
        IPV4_ONLY,
        IPV6_ONLY
    }
    
    static IPMode getZimbraIPMode()
    {
        if (currentIPMode == IPMode.UNKNOWN) {
            String res = ProxyConfVar.serverSource.getAttr(Provisioning.A_zimbraIPMode, "both");
            if (res.equalsIgnoreCase("both")) {
                currentIPMode = IPMode.BOTH;
            } else if (res.equalsIgnoreCase("ipv4")) {
                currentIPMode = IPMode.IPV4_ONLY;
            } else {
                currentIPMode = IPMode.IPV6_ONLY;
            }
        }
        
        return currentIPMode;
    }
}

class IPBothEnablerVar extends IPModeEnablerVar {

    public IPBothEnablerVar() {
        super("core.ipboth.enabled", true, "Both IPv4 and IPv6");
    }
    
    @Override
    public void update() {
        IPMode ipmode = getZimbraIPMode();
        mValue=(ipmode == IPMode.BOTH)?true:false;    
    }
}

class IPv4OnlyEnablerVar extends IPModeEnablerVar {

    public IPv4OnlyEnablerVar() {
        super("core.ipv4only.enabled", false, "IPv4 Only");
    }
    
    @Override
    public void update() {
        IPMode ipmode = getZimbraIPMode();
        mValue=(ipmode == IPMode.IPV4_ONLY)?true:false;    
    }
}

class IPv6OnlyEnablerVar extends IPModeEnablerVar {

    public IPv6OnlyEnablerVar() {
        super("core.ipv6only.enabled", false, "IPv6 Only");
    }
    
    @Override
    public void update() {
        IPMode ipmode = getZimbraIPMode();
        mValue=(ipmode == IPMode.IPV6_ONLY)?true:false;    
    }
}

class Pop3GreetingVar extends ProxyConfVar {

    public Pop3GreetingVar() {
        super("mail.pop3.greeting", "zimbraReverseProxyPop3ExposeVersionOnBanner", "",
                ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, 
                "Proxy IMAP banner message (contains build version if " +
                "zimbraReverseProxyImapExposeVersionOnBanner is true)");
    }
    
    @Override
    public void update() {
        if (serverSource.getBooleanAttr("zimbraReverseProxyPop3ExposeVersionOnBanner", false)) {
            mValue = "+OK " + "Zimbra " + BuildInfo.VERSION + " POP3 ready";
        } else {
            mValue = "";
        }
    }
}

class ImapGreetingVar extends ProxyConfVar {

    public ImapGreetingVar() {
        super("mail.imap.greeting", "zimbraReverseProxyImapExposeVersionOnBanner", "",
                ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, 
                "Proxy IMAP banner message (contains build version if " +
                "zimbraReverseProxyImapExposeVersionOnBanner is true)");
    }
    
    @Override
    public void update() {
        if (serverSource.getBooleanAttr("zimbraReverseProxyImapExposeVersionOnBanner",false)) {
            mValue = "* OK " + "Zimbra " + BuildInfo.VERSION + " IMAP4 ready";
        } else {
            mValue = "";
        }
    }
}

class SaslHostFromIPVar extends ProxyConfVar {
    
    public SaslHostFromIPVar() {
        super("mail.sasl_host_from_ip", "krb5_service_principal_from_interface_address",
                false, ProxyConfValueType.BOOLEAN, ProxyConfOverride.LOCALCONFIG,
                "Whether to use incoming interface IP address to determine service " +
                "principal name (if true, IP address is reverse mapped to DNS name, " +
                "else host name of proxy is used)");
    }
    
    @Override
    public void update() {
        if (LC.krb5_service_principal_from_interface_address.booleanValue()) {
            mValue = true;
        }
        else {
            mValue = false;
        }
    }
}

class MemcacheServersVar extends ProxyConfVar {

    public MemcacheServersVar() {
        super("memcache.:servers", null, null, ProxyConfValueType.CUSTOM,
                ProxyConfOverride.CUSTOM, 
                "List of known memcache servers (i.e. servers having proxy service enabled)");
    }
    
   
    @Override
    public void update() throws ServiceException {
        ArrayList<String> servers = new ArrayList<String>();

        /* $(zmprov gamcs) */
        List<Server> mcs = mProv.getAllServers(Provisioning.SERVICE_MEMCACHED);
        for (Server mc : mcs) {
            String serverName = mc.getAttr(
                    Provisioning.A_zimbraServiceHostname, "");
            int serverPort = mc.getIntAttr(
                    Provisioning.A_zimbraMemcachedBindPort, 11211);
            Formatter f = new Formatter();
            f.format("%s:%d", serverName, serverPort);

            servers.add(f.toString());
        }

        mValue = servers;
    }

    @Override
    public String format(Object o) {
        @SuppressWarnings("unchecked")
        ArrayList<String> servers = (ArrayList<String>) o;
        StringBuilder conf = new StringBuilder();
        for (String s : servers) {
            conf.append("  servers   ");
            conf.append(s);
            conf.append(";\n");
        }
        return conf.toString();
    }
}

class LookupHandlersVar extends ProxyConfVar {
    
    public LookupHandlersVar(String key, String desc) {
        super(key, null, null, ProxyConfValueType.CUSTOM,
                ProxyConfOverride.CUSTOM, desc);
    }
    
    @Override
    public void update() throws ServiceException {
        
        ArrayList<String> servers = new ArrayList<String>();

        // $(zmprov garpu) 
        List<Server> allServers = mProv.getAllServers();
        int REVERSE_PROXY_PORT = 7072;
        for (Server s: allServers) {
            String sn = s.getAttr(Provisioning.A_zimbraServiceHostname,"");
            boolean isTarget = s.getBooleanAttr(Provisioning.A_zimbraReverseProxyLookupTarget, false);
            if (isTarget) {
                Formatter f = new Formatter();
                f.format("%s:%d", sn, REVERSE_PROXY_PORT);
                servers.add(f.toString());
                mLog.debug("Route Lookup: Added server " + sn);
            }
        }

        mValue = servers;
    }
}

@Deprecated
/**
 * only for back compatibility
 */
class RouteHandlersVar extends LookupHandlersVar {
    public RouteHandlersVar() {
        super("web.:routehandlers",
              "List of web route lookup handlers (i.e. servers " +
              "for which zimbraReverseProxyLookupTarget is true)");
    }
    
    @Override
    public String format(Object o) {
        
        String REVERSE_PROXY_PATH = ExtensionDispatcherServlet.EXTENSION_PATH + "/nginx-lookup";
        @SuppressWarnings("unchecked")
        ArrayList<String> servers = (ArrayList<String>) o;
        String conf = "";
        for (String s: servers) {
            conf = conf + "    zmroutehandlers   " + s + REVERSE_PROXY_PATH + ";" + "\n";
        }
        return conf; 
    }
}

@Deprecated
/**
 * only for back compatibility
 */
class AuthHttpHandlersVar extends LookupHandlersVar {

    public AuthHttpHandlersVar() {
        super("mail.:auth_http",
              "List of mail route lookup handlers (i.e. servers " +
              "for which zimbraReverseProxyLookupTarget is true)");
    }
    
    @Override
    public String format(Object o) {

        String REVERSE_PROXY_PATH = ExtensionDispatcherServlet.EXTENSION_PATH + "/nginx-lookup";
        @SuppressWarnings("unchecked")
        ArrayList<String> servers = (ArrayList<String>) o;
        String conf = "";
        for (String s: servers) {
            conf = conf + "    auth_http   " + s + REVERSE_PROXY_PATH + ";" + "\n";
        }
        return conf;
    }
}

/**
 * The variable for nginx "upstream" servers block
 * @author jiankuan
 *
 */
abstract class ServersVar extends ProxyConfVar {
    /**
     * The port attribute name
     */
    private String mPortAttrName;
    
    public ServersVar(String key, String portAttrName, String description) {
        super(key, null, null, ProxyConfValueType.CUSTOM, ProxyConfOverride.CUSTOM, description);
        this.mPortAttrName = portAttrName;
    }
    
    @Override
    public void update() throws ServiceException {
        ArrayList<String> servers = new ArrayList<String>();
        String portName = configSource.getAttr(mPortAttrName, "");
        /* $(zmprov garpb) */
        List<Server> us = mProv.getAllServers();

        for (Server u : us) {
            boolean isTarget = u.getBooleanAttr(
                    Provisioning.A_zimbraReverseProxyLookupTarget, false);
            if (isTarget) {
                String mode = u.getAttr(Provisioning.A_zimbraMailMode, "");
                String serverName = u.getAttr(
                        Provisioning.A_zimbraServiceHostname, "");

                if (mode.equalsIgnoreCase(Provisioning.MailMode.http.toString())
                        || mode.equalsIgnoreCase(Provisioning.MailMode.mixed
                                .toString())
                        || mode.equalsIgnoreCase(Provisioning.MailMode.both
                                .toString())) {
                    int serverPort = u.getIntAttr(portName, 0);
                    int timeout = u.getIntAttr(
                            Provisioning.A_zimbraMailProxyReconnectTimeout, 60);
                    int maxFails = u.getIntAttr("zimbraMailProxyMaxFails", 1);
                    if (maxFails != 1) {
                        servers.add(String.format("%s:%d fail_timeout=%ds max_fails=%d", serverName, serverPort,
                                timeout, maxFails));
                    } else  {
                        servers.add(String.format("%s:%d fail_timeout=%ds", serverName, serverPort,
                                timeout));
                    }
                    mLog.info("Added server to HTTP upstream: " + serverName);
                } else {
                    mLog.warn("Upstream: Ignoring server: " + serverName
                            + " ,because its mail mode is: " + (mode.equals("")?"EMPTY":mode));
                }
            }
        }

        mValue = servers;
    }
    
    @Override
    public String format(Object o) {
        @SuppressWarnings("unchecked")
        ArrayList<String> servers = (ArrayList<String>) o;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < servers.size(); i++) {
            String s = servers.get(i);
            if (i == 0) {
                sb.append(String.format("server    %s;\n", s));
            } else {
                sb.append(String.format("        server    %s;\n", s));
            }
        }
        return sb.toString();
    }
}

class WebUpstreamServersVar extends ServersVar {
    
    public WebUpstreamServersVar() {
        super("web.upstream.:servers", Provisioning.A_zimbraReverseProxyHttpPortAttribute,
                "List of upstream HTTP servers used by Web Proxy (i.e. servers " +
                "for which zimbraReverseProxyLookupTarget is true, and whose " +
                "mail mode is http|mixed|both)");
    }
}

class WebSSLUpstreamServersVar extends ServersVar {
    
    public WebSSLUpstreamServersVar() {
        super("web.ssl.upstream.:servers", Provisioning.A_zimbraReverseProxyHttpSSLPortAttribute,
                "List of upstream HTTPS servers used by Web Proxy (i.e. servers " +
                "for which zimbraReverseProxyLookupTarget is true, and whose " +
                "mail mode is https|mixed|both)");
    }
}

class WebAdminUpstreamServersVar extends ServersVar {
    
    public WebAdminUpstreamServersVar() {
        super("web.admin.upstream.:servers", Provisioning.A_zimbraReverseProxyAdminPortAttribute,
                "List of upstream admin console servers used by Web Proxy (i.e. servers " +
                "for which zimbraReverseProxyLookupTarget is true");
    }
}

class ImapCapaVar extends ProxyConfVar {
    
    public ImapCapaVar() {
        super("mail.imapcapa", null, getDefaultImapCapabilities(),
                ProxyConfValueType.CUSTOM, ProxyConfOverride.CUSTOM,
                "IMAP Capability List");
    }

    static ArrayList<String> getDefaultImapCapabilities () {
        ArrayList<String> imapCapabilities = new ArrayList<String> ();
        imapCapabilities.add("IMAP4rev1");
        imapCapabilities.add("ID");
        imapCapabilities.add("LITERAL+");
        imapCapabilities.add("SASL-IR");
        imapCapabilities.add("IDLE");
        imapCapabilities.add("NAMESPACE");
        return imapCapabilities;
    }
    
    @Override
    public void update() {

        ArrayList<String> capabilities = new ArrayList<String>();
        String[] capabilityNames = 
            serverSource.getMultiAttr("zimbraReverseProxyImapEnabledCapability");
        for (String c:capabilityNames)
        {
            capabilities.add(c);
        }
        if (capabilities.size() > 0) {
            mValue = capabilities;
        } else {
            mValue = mDefault;
        }
    }

    @Override
    public String format(Object o) {

        @SuppressWarnings("unchecked")
        ArrayList<String> capabilities = (ArrayList<String>) o;
        StringBuilder capa = new StringBuilder();
        for (String c : capabilities) {
            capa.append(" \"");
            capa.append(c);
            capa.append("\"");
        }
        return capa.toString();
    }
}

class Pop3CapaVar extends ProxyConfVar {
    
    public Pop3CapaVar() {
        super("mail.pop3capa", null, getDefaultPop3Capabilities(),
                ProxyConfValueType.CUSTOM, ProxyConfOverride.CUSTOM,
                "POP3 Capability List");
    }

    public static ArrayList<String> getDefaultPop3Capabilities() {
        ArrayList<String> pop3Capabilities = new ArrayList<String>();
        pop3Capabilities.add("TOP");
        pop3Capabilities.add("USER");
        pop3Capabilities.add("UIDL");
        pop3Capabilities.add("EXPIRE 31 USER");
        return pop3Capabilities;
    }

    @Override
    public void update() {

        ArrayList<String> capabilities = new ArrayList<String>();
        String[] capabilityNames = serverSource
                .getMultiAttr("zimbraReverseProxyPop3EnabledCapability");
        for (String c : capabilityNames) {
            capabilities.add(c);
        }
        if (capabilities.size() > 0) {
            mValue = capabilities;
        } else {
            mValue = mDefault;
        }
    }
    
    @Override
    public String format(Object o) {

        @SuppressWarnings("unchecked")
        ArrayList<String> capabilities = (ArrayList<String>) o;
        StringBuilder capa = new StringBuilder();
        for (String c : capabilities) {
            capa.append(" \"");
            capa.append(c);
            capa.append("\"");
        }
        return capa.toString();
    }
}

class ZMLookupHandlerVar extends ProxyConfVar{
    public ZMLookupHandlerVar() {
        super("zmlookup.:handlers",
              "zimbraReverseProxyLookupTarget",
              new ArrayList<String>(),
              ProxyConfValueType.CUSTOM,
              ProxyConfOverride.CUSTOM,
              "List of nginx lookup handlers (i.e. servers for which" +
              " zimbraReverseProxyLookupTarget is true)");
    }
    
    @Override
    public void update() throws ServiceException {
        ArrayList<String> servers = new ArrayList<String>();

        List<Server> allServers = mProv.getAllServers();
        int REVERSE_PROXY_PORT = 7072;
        for (Server s : allServers)
        {
            String sn = s.getAttr(Provisioning.A_zimbraServiceHostname,"");
            boolean isTarget = s.getBooleanAttr(Provisioning.A_zimbraReverseProxyLookupTarget, false);
            if (isTarget) {
                Formatter f = new Formatter();
                f.format("%s:%d", sn, REVERSE_PROXY_PORT);
                servers.add(f.toString());
                mLog.debug("Route Lookup: Added server " + sn);
            }
        }

        mValue = servers;
    }
    
    @Override
    public String format(Object o) throws ProxyConfException {
        String REVERSE_PROXY_PATH = ExtensionDispatcherServlet.EXTENSION_PATH + "/nginx-lookup";
        @SuppressWarnings("unchecked")
        ArrayList<String> servers = (ArrayList<String>) o;
        if (servers.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String s: servers) {
            sb.append(s + REVERSE_PROXY_PATH);
            sb.append(' ');
        }
        sb.setLength(sb.length() - 1); //trim the last space
        return sb.toString();
    }
}

class ZMSSOCertAuthDefaultEnablerVar extends ProxyConfVar {
    public ZMSSOCertAuthDefaultEnablerVar() {
        super("web.sso.certauth.default.enabled",
              null,
              null,
              ProxyConfValueType.ENABLER,
              ProxyConfOverride.CUSTOM,
              "whether to turn on certauth in global/server level");
    }
    
    @Override
    public void update() throws ServiceException {
        String certMode = 
            serverSource.getAttr(Provisioning.A_zimbraReverseProxyClientCertMode, "off");
       if (certMode.equals("on") || certMode.equals("optional")) {
           mValue = true;
       } else {
           // ... we may add more condition if more sso auth method is introduced
           mValue = false;
       }
    }
}

class ClientCertAuthDefaultCAVar extends ProxyConfVar {
    public ClientCertAuthDefaultCAVar() {
        super("ssl.clientcertca.default",
              "zimbraReverseProxyClientCertCA", 
              ProxyConfGen.getDefaultClientCertCaPath(), 
              ProxyConfValueType.STRING, 
              ProxyConfOverride.CUSTOM, 
              "CA certificate for authenticating client certificates in nginx proxy (https only)");
    }
    
    @Override
    public void update() throws ServiceException {
        
        mValue = mDefault; //must be the value of getDefaultClientCertCaPath
    }
}

class SSORedirectEnablerVar extends ProxyConfVar {
    public SSORedirectEnablerVar() {
        super("web.sso.redirect.enabled.default",
              "zimbraWebClientLoginURL", 
              false, 
              ProxyConfValueType.ENABLER, 
              ProxyConfOverride.CUSTOM, 
              "whether to redirect from common http & https to https sso");
    }
    
    @Override
    public void update() throws ServiceException {
        String webClientLoginURL = serverSource.getAttr(mAttribute, true);
        if (webClientLoginURL == null ||
            ProxyConfUtil.isEmptyString(webClientLoginURL)) {
            mValue = false;
        } else {
            mValue = true;
        }
    }
}

class ZMSSOEnablerVar extends ProxyConfVar {
    public ZMSSOEnablerVar() {
        super("web.sso.enabled",
              "zimbraReverseProxyClientCertMode", 
              false, 
              ProxyConfValueType.ENABLER, 
              ProxyConfOverride.CUSTOM, 
              "whether enable sso for domain level");
    }
    
    @Override
    public void update() throws ServiceException {
        if (ProxyConfGen.isDomainClientCertVerifyEnabled()) {
            mValue = true;
        } else {
            mValue = false;
        }
    }
}

class ZMSSODefaultEnablerVar extends ProxyConfVar {
    public ZMSSODefaultEnablerVar() {
        super("web.sso.enabled",
              "zimbraReverseProxyClientCertMode", 
              false, 
              ProxyConfValueType.ENABLER, 
              ProxyConfOverride.CUSTOM, 
              "whether enable sso for global/server level");
    }
    
    @Override
    public void update() throws ServiceException {
        if (ProxyConfGen.isClientCertVerifyEnabled()) {
            mValue = true;
        } else {
            mValue = false;
        }
    }
}

class ErrorPagesVar extends ProxyConfVar {
    
    static final String[] ERRORS = {"502", "504"};
    
    public ErrorPagesVar() {
        super("web.:errorPages",
              "zimbraReverseProxyErrorHandlerURL", 
              "", 
              ProxyConfValueType.STRING, 
              ProxyConfOverride.SERVER, 
              "the error page statements");
    }
    
    @Override
    public String format(Object o) throws ProxyConfException {

        String errURL = (String)o;
        StringBuilder sb = new StringBuilder();
        if(errURL.length() == 0) {
            for(String err: ErrorPagesVar.ERRORS) {
                sb.append("error_page " + err + " /zmerror_upstream_" + err + ".html;\n");
            }
        } else {
            for(String err: ErrorPagesVar.ERRORS) {
                sb.append("error_page " + err + " " + errURL + "?err=" + err + "&up=$upstream_addr;\n");
            }
        }
        return sb.toString();
    }
}

/**
 * Provide the timeout value which accepts an offset
 * @author jiankuan
 *
 */
class TimeoutVar extends ProxyConfVar {
    private int offset;

    public TimeoutVar(String keyword, String attribute, Object defaultValue,
            ProxyConfOverride overrideType, int offset,
            String description) {
        super(keyword, attribute, defaultValue, ProxyConfValueType.INTEGER,
                overrideType, description);
        this.offset = offset;
    }
    
    @Override
    public void update() throws ServiceException, ProxyConfException {
        super.update();
        mValue = new Integer(((Integer)mValue).intValue() + offset);
    }
}

/**
 * a wrapper class that convert a ProxyConfVar which
 * contains the time in milliseconds to seconds. This
 * is useful when the default timeout unit used by 
 * Provisioning API is "ms" but nginx uses "s".
 * @author jiankuan
 *
 */
class TimeInSecVarWrapper extends ProxyConfVar {
    private ProxyConfVar mVar;
    
    public TimeInSecVarWrapper (ProxyConfVar var) {
        super(null, null, null, null, null, null);
        
        if (var.mValueType != ProxyConfValueType.TIME) {
            throw new RuntimeException("Only Proxy Conf Var with TIME" +
            		" type can be used in this wrapper");
        }
        
        mVar = var;
    }
    
    @Override
    public void update() throws ServiceException, ProxyConfException {
        mVar.update();
        mVar.mValue = ((Long)mVar.mValue).longValue() / 1000;
    }
    
    public String format(Object o) throws ProxyConfException {
        return mVar.mValue.toString();
    }
}

/**
 * Provide the value of "proxy_pass" for web proxy.
 * @author jiankuan
 *
 */
class WebProxyUpstreamTargetVar extends ProxyConfVar {
    public WebProxyUpstreamTargetVar() {
        super("web.upstream.schema", "zimbraReverseProxySSLToUpstreamEnabled", false, ProxyConfValueType.BOOLEAN,
                ProxyConfOverride.SERVER, "The target of proxy_pass for web proxy");
    }
    
    @Override
    public String format(Object o) throws ProxyConfException {
        Boolean value = (Boolean)o;
        if(value == false) {
            return "http://" + ProxyConfGen.ZIMBRA_UPSTREAM_NAME;
        } else {
            return "https://" + ProxyConfGen.ZIMBRA_SSL_UPSTREAM_NAME;
        }
    }
}

/**
 * A simple class of Triple<VirtualHostName, VirtualIPAddress, DomainName>. Uses
 * this only for convenient and HashMap can't guarantee order
 * @author jiankuan
 */
class DomainAttrItem {
    public String domainName;
    public String virtualHostname;
    public String virtualIPAddress;
    public String sslCertificate;
    public String sslPrivateKey;
    public Boolean useDomainServerCert;
    public Boolean useDomainClientCert;
    public String clientCertMode;
    public String clientCertCa;

    public DomainAttrItem(String dn, String vhn, String vip, String scrt, String spk, 
            String ccm, String cca) {
        this.domainName = dn;
        this.virtualHostname = vhn;
        this.virtualIPAddress = vip;
        this.sslCertificate = scrt;
        this.sslPrivateKey = spk;
        this.clientCertMode = ccm;
        this.clientCertCa = cca;
    }
}

/** The visit of LdapProvisioning can't throw the exception out.
 *  Therefore uses this special item to indicate exception.
 * @author jiankuan
 *
 */
class DomainAttrExceptionItem extends DomainAttrItem {
    public DomainAttrExceptionItem(ProxyConfException e) {
        super(null, null, null, null, null, null, null);
        this.exception = e;
    }

    public ProxyConfException exception;
}

public class ProxyConfGen
{
    private static final int DEFAULT_SERVER_NAME_HASH_MAX_SIZE = 512;
    private static Log mLog = LogFactory.getLog (ProxyConfGen.class);
    private static Options mOptions = new Options();
    private static boolean mDryRun = false;
    private static String mWorkingDir = "/opt/zimbra";
    private static String mTemplateDir = mWorkingDir + "/conf/nginx/templates";
    private static String mConfDir = mWorkingDir + "/conf";
    private static String mIncDir = "nginx/includes";
    private static String mDomainSSLDir = mConfDir + File.separator + "domaincerts";
    private static String mSSLCrtExt = ".crt";
    private static String mSSLKeyExt = ".key";
    private static String mSSLClientCertCaExt = ".client.ca.crt";
    private static String mDefaultSSLCrt = mConfDir + File.separator + "nginx.crt";
    private static String mDefaultSSLKey = mConfDir + File.separator + "nginx.key";
    private static String mDefaultSSLClientCertCa = mConfDir + File.separator + "nginx.client.ca.crt";
    private static String mConfIncludesDir = mConfDir + File.separator + mIncDir;
    private static String mConfPrefix = "nginx.conf";
    private static String mTemplatePrefix = mConfPrefix;
    private static String mTemplateSuffix = ".template";
    private static Provisioning mProv = null;
    private static String mHost = null;
    private static Server mServer = null;
    private static boolean mGenConfPerVhn = false;
    private static Map<String, ProxyConfVar> mConfVars = new HashMap<String, ProxyConfVar>();
    private static Map<String, String> mVars = new HashMap<String, String>();
    static List<DomainAttrItem> mDomainReverseProxyAttrs;
    
    static final String ZIMBRA_UPSTREAM_NAME = "zimbra";
    static final String ZIMBRA_SSL_UPSTREAM_NAME = "zimbra_ssl";
    static final String ZIMBRA_ADMIN_CONSOLE_UPSTREAM_NAME = "zimbra_admin";
     

    /** the pattern for custom header cmd, such as "!{explode domain} */
    private static Pattern cmdPattern = Pattern.compile("(.*)\\!\\{([^\\}]+)\\}(.*)", Pattern.DOTALL);

    static
    {
        mOptions.addOption("h", "help", false, "show this usage text");
        mOptions.addOption("v", "verbose", false, "be verbose");

        mOptions.addOption("w", "workdir", true, "Proxy Working Directory (defaults to /opt/zimbra)");
        mOptions.addOption("t", "templatedir", true, "Proxy Template Directory (defaults to $workdir/conf/nginx/templates)");
        mOptions.addOption("n", "dry-run", false, "Do not write any configuration, just show which files would be written");
        mOptions.addOption("d", "defaults", false, "Print default variable map");
        mOptions.addOption("D", "definitions", false, "Print variable map Definitions after loading LDAP configuration (and processing overrides)");
        mOptions.addOption("p", "prefix", true, "Config File prefix (defaults to nginx.conf)");
        mOptions.addOption("P", "template-prefix", true, "Template File prefix (defaults to $prefix)");
        mOptions.addOption("i", "include-dir", true, "Directory Path (relative to $workdir/conf), where included configuration files will be written. Defaults to nginx/includes");
        mOptions.addOption("s", "server", true, "If provided, this should be the name of a valid server object. Configuration will be generated based on server attributes. Otherwise, if not provided, Configuration will be generated based on Global configuration values");

        Option cOpt = new Option("c","config",true,"Override a config variable. Argument format must be name=value. For list of names, run with -d or -D");
        cOpt.setArgs(Option.UNLIMITED_VALUES);
        mOptions.addOption(cOpt);
    }

    private static void usage(String errmsg)
    {
        if (errmsg != null) {
            System.out.println(errmsg);
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ProxyConfGen [options] ",
            "where [options] are one of:", mOptions,
            "ProxyConfGen generates the NGINX Proxy configuration files");
    }

    private static CommandLine parseArgs(String args[])
    {
        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(mOptions, args);
        } catch (ParseException pe) {
            usage(pe.getMessage());
            return cl;
        }

        return cl;
    }
    
    /**
     * Retrieve all the necessary domain level reverse proxy attrs, like
     * virtualHostname, ssl certificate, ...
     * 
     * @return a list of <code>DomainAttrItem</code>
     * @throws ServiceException
     *             this method can work only when LDAP is available
     * @author Jiankuan
     */
    private static List<DomainAttrItem> loadDomainReverseProxyAttrs()
            throws ServiceException {

        if (!mGenConfPerVhn) {
            return Collections.emptyList();
        }
        if (!(mProv instanceof LdapProv))
            throw ServiceException.INVALID_REQUEST(
                "The method can work only when LDAP is available", null);

        final Set<String> attrsNeeded = new HashSet<String>();
        attrsNeeded.add(Provisioning.A_zimbraVirtualHostname);
        attrsNeeded.add(Provisioning.A_zimbraVirtualIPAddress);
        attrsNeeded.add(Provisioning.A_zimbraSSLCertificate);
        attrsNeeded.add(Provisioning.A_zimbraSSLPrivateKey);
        attrsNeeded.add(Provisioning.A_zimbraReverseProxyClientCertMode);
        attrsNeeded.add(Provisioning.A_zimbraReverseProxyClientCertCA);
        attrsNeeded.add(Provisioning.A_zimbraWebClientLoginURL);

        final List<DomainAttrItem> result = new ArrayList<DomainAttrItem>();


        // visit domains
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            public void visit(NamedEntry entry) throws ServiceException {
                String domainName = entry
                    .getAttr(Provisioning.A_zimbraDomainName);
                String[] virtualHostnames = entry
                    .getMultiAttr(Provisioning.A_zimbraVirtualHostname);
                String[] virtualIPAddresses = entry
                    .getMultiAttr(Provisioning.A_zimbraVirtualIPAddress);
                String certificate = entry
                    .getAttr(Provisioning.A_zimbraSSLCertificate);
                String privateKey = entry
                    .getAttr(Provisioning.A_zimbraSSLPrivateKey);
                String clientCertMode = entry
                    .getAttr(Provisioning.A_zimbraReverseProxyClientCertMode);
                String clientCertCA = entry
                    .getAttr(Provisioning.A_zimbraReverseProxyClientCertCA);

                // no need to check whether clientCertMode or clientCertCA == null,

                if (virtualHostnames.length == 0 || ( certificate == null &&
                                privateKey == null && clientCertMode == null && clientCertCA == null ) ) {

                    return; // ignore the items that don't have virtual host
                            // name, cert or key. Those domains will use the
                            // config
                }
                boolean lookupVIP = true; // lookup virutal IP from DNS or /etc/hosts
                if (virtualIPAddresses.length > 0) {
                    if (virtualIPAddresses.length != virtualHostnames.length) {
                        result.add(new DomainAttrExceptionItem(
                                new ProxyConfException("The configurations of zimbraVirtualHostname and " +
                                                       "zimbraVirtualIPAddress are mismatched", null)));
                        return;
                    }
                    lookupVIP = false;
                }

                //Here assume virtualHostnames and virtualIPAddresses are
                //same in number
                int i = 0;

                for( ; i < virtualHostnames.length; i++) {
                    //bug 66892, only lookup IP when zimbraVirtualIPAddress is unset
                    String vip = null;
                    if (lookupVIP) {
                        vip = null;
                    } else {
                        vip = virtualIPAddresses[i];
                    }
                    
                    if (!ProxyConfUtil.isEmptyString(clientCertCA)){
                        createDomainSSLDirIfNotExists();
                    }
                    result.add(new DomainAttrItem(domainName,
                            virtualHostnames[i], vip, certificate, privateKey, 
                            clientCertMode, clientCertCA));
                }
            }
        };

        mProv.getAllDomains(visitor,
            attrsNeeded.toArray(new String[attrsNeeded.size()]));

        return result;
    }
    
    /**
     * Load all the client cert ca content
     * @return
     */
    private static String loadAllClientCertCA() {
        // to avoid redundancy CA if some domains share the same CA
        HashSet<String> caSet = new HashSet<String>(); 
        String globalCA = ProxyConfVar.serverSource.getAttr(Provisioning.A_zimbraReverseProxyClientCertCA, "");
        if (!ProxyConfUtil.isEmptyString(globalCA)) {
            caSet.add(globalCA);
        }
        
        for (DomainAttrItem item : mDomainReverseProxyAttrs) {
            if (!ProxyConfUtil.isEmptyString(item.clientCertCa)) {
                caSet.add(item.clientCertCa);
            }
        }
        
        StringBuilder sb = new StringBuilder();
        String separator = System.getProperty("line.separator");
        for (String ca: caSet) {
            sb.append(ca);
            sb.append(separator);
        }
        if (sb.length() > separator.length()) {
            sb.setLength(sb.length() - separator.length()); // trim the last separator
        }
        return sb.toString();
    }

    public static void createDomainSSLDirIfNotExists( ){
        File domainSSLDir = new File( mDomainSSLDir );
        if( !domainSSLDir.exists() ){
          domainSSLDir.mkdirs();
        }
    }

    /* Guess how to find a server object -- taken from ProvUtil::guessServerBy */
    public static Key.ServerBy guessServerBy(String value) {
        if (Provisioning.isUUID(value))
            return Key.ServerBy.id;
        return Key.ServerBy.name;
    }

    public static Server getServer (String key)
        throws ProxyConfException
    {
        Server s = null;

        try {
            s = mProv.get(guessServerBy(key),key);
            if (s == null) {
                throw new ProxyConfException ("Cannot find server: " + key);
            }
        } catch (ServiceException se) {
            throw new ProxyConfException ("Error getting server: " + se.getMessage());
        }

        return s;
    }

    private static String getCoreConf () {
        return mConfPrefix;
    }

    private static String getCoreConfTemplate () {
        return mTemplatePrefix + mTemplateSuffix;
    }
    
    private static String getConfFileName(String name) {
        return mConfPrefix + "." + name;
    }
    
    private static String getConfTemplateFileName(String name) {
        return mTemplatePrefix + "." + name + mTemplateSuffix;
    }

    private static String getWebHttpModeConf (String mode) {
        return mConfPrefix + ".web.http.mode-" + mode;
    }

    private static String getWebHttpModeConfTemplate (String mode) {
        return mTemplatePrefix + ".web.http.mode-" + mode + mTemplateSuffix;
    }

    private static String getWebHttpSModeConf (String mode) {
        return mConfPrefix + ".web.https.mode-" + mode;
    }

    public static String getWebHttpSModeConfTemplate (String mode) {
        return mTemplatePrefix + ".web.https.mode-" + mode + mTemplateSuffix;
    }

    public static String getClientCertCaPathByDomain(String domainName ){

        return mDomainSSLDir + File.separator + domainName + mSSLClientCertCaExt;
    }
    
    public static String getDefaultClientCertCaPath() {
        return mDefaultSSLClientCertCa;
    }

    public static void expandTemplate (File tFile, File wFile)
        throws ProxyConfException
    {
        BufferedReader r = null;
        BufferedWriter w = null;
        try {
            String tf = tFile.getAbsolutePath();
            String wf = wFile.getAbsolutePath();

            mLog.info("Expanding template:" + tf + " to file:" + wf);

            if (mDryRun) {
                return;
            }

            if (!tFile.exists()) {
                throw new ProxyConfException("Template file " + tf + " does not exist");
            }
            r = new BufferedReader(new FileReader(tf));
            w = new BufferedWriter(new FileWriter(wf));

            String line;
            
            //for the first line of template, check the custom header command
            r.mark(100); //assume the first line won't beyond 100
            line = r.readLine();
            
            //only for back compability
            if(line.equalsIgnoreCase("!{explode vhn_vip_ssl}")) {
                expandTemplateExplodeSSLConfigsForAllVhnsAndVIPs(r, w);
                return;
            }
            Matcher cmdMatcher = cmdPattern.matcher(line);
            if(cmdMatcher.matches()) {
                //the command is found
                String[] cmd_arg = cmdMatcher.group(2).split("[ \t]+", 2);
                //command selection can be extracted if more commands are introduced
                if(cmd_arg.length == 2 && 
                   cmd_arg[0].compareTo("explode") == 0) {
                    
                    if(!mGenConfPerVhn) { // explode only when GenConfPerVhn is enabled
                        return;
                    }
                    
                    if(cmd_arg[1].startsWith("domain(") &&cmd_arg[1].endsWith(")")) {
                        //extract the args in "domain(arg1, arg2, ...)
                        String arglist = cmd_arg[1].substring("domain(".length(), cmd_arg[1].length() - 1);
                        String[] args;
                        if(arglist.equals("")) {
                            args = new String[0];
                        } else {
                            args = arglist.split(",( |\t)*");
                        }
                        expandTemplateByExplodeDomain(r, w, args);
                    } else {
                        throw new ProxyConfException("Illegal custom header command: " + cmdMatcher.group(2));
                    }
                } else {
                    throw new ProxyConfException("Illegal custom header command: " + cmdMatcher.group(2));
                }
            } else {
                r.reset(); //reset to read the first line
                expandTemplateSimple(r, w);
            }
           
        } catch (IOException ie) {
                throw new ProxyConfException("Cannot expand template file: "
                    + ie.getMessage());

        } catch (SecurityException se) {
            throw new ProxyConfException("Cannot expand template: "
                + se.getMessage());
        }finally {
            try {
                if (w != null)
                    w.close();
                if (r != null)
                    r.close();
            } catch (IOException e) {
                throw new ProxyConfException("Cannot expand template file: " +
                    e.getMessage());
            }
        }
    }
    
    /**
     * Enumerate all domains, if the required attrs are valid, generate the
     * "server" block according to the template.
     * @author Jiankuan
     * @throws ProxyConfException
     * @deprecated use expandTemplateByExplodeDomain instead
     */
    private static void expandTemplateExplodeSSLConfigsForAllVhnsAndVIPs(
        BufferedReader temp, BufferedWriter conf) throws IOException, ProxyConfException {
        int size = mDomainReverseProxyAttrs.size();
        List<String> cache = null;

        if (size > 0) {
            Iterator<DomainAttrItem> it = mDomainReverseProxyAttrs.iterator();
            DomainAttrItem item = it.next();
            fillVarsWithDomainAttrs(item);
            cache = expandTemplateAndCache(temp, conf);
            conf.newLine();

            while (it.hasNext()) {
                item = it.next();
                fillVarsWithDomainAttrs(item);
                expandTempateFromCache(cache, conf);
                conf.newLine();
            }
        }
    }
    
    /**
     * Enumerate all virtual host names and virtual ip addresses and 
     * apply them into the var replacement.<br/>
     * explode domain command has this format:<br/>
     * <code>!{explode domain(arg1, arg2, ...)}</code><br/>
     * The args indicate the required attrs to generate a server block
     * , which now supports:
     * <ul>
     * <li>vhn: zimbraVirtualHostname must not be empty</li>
     * <li>sso: zimbraClientCertMode must not be empty or "off"</li>
     * </ul>
     * @author Jiankuan
     * @throws ProxyConfException 
     */
    private static void expandTemplateByExplodeDomain(
        BufferedReader temp, BufferedWriter conf, String[] requiredAttrs) throws IOException, ProxyConfException {
        int size = mDomainReverseProxyAttrs.size();
        List<String> cache = null;

        if (size > 0) {
            Iterator<DomainAttrItem> it = mDomainReverseProxyAttrs.iterator();
            DomainAttrItem item;
            while(cache == null && it.hasNext()) {
                item = it.next();
                if (item instanceof DomainAttrExceptionItem) {
                    throw ((DomainAttrExceptionItem)item).exception;
                }

                if (!isRequiredAttrsValid(item, requiredAttrs)) {
                    continue;
                }
                fillVarsWithDomainAttrs(item);
                cache = expandTemplateAndCache(temp, conf);
                conf.newLine();
            }

            while (it.hasNext()) {
                item = it.next();
                if (item instanceof DomainAttrExceptionItem) {
                    throw ((DomainAttrExceptionItem)item).exception;
                }
                
                if (!isRequiredAttrsValid(item, requiredAttrs)) {
                    continue;
                }
                fillVarsWithDomainAttrs(item);
                expandTempateFromCache(cache, conf);
                conf.newLine();
            }
        }
    }
    
    private static boolean isRequiredAttrsValid(DomainAttrItem item, String[] requiredAttrs) {
        for(String attr: requiredAttrs) {
            if (attr.equals("vhn")) {
                //check virtual hostname
                if (item.virtualHostname == null || item.virtualHostname.equals("")) {
                    return false;
                }
            } else if (attr.equals("sso")) {
                if (item.clientCertMode == null ||
                    item.clientCertMode.equals("") ||
                    item.clientCertMode.equals("off")) {
                    return false;
                }
            } else {
                //... check other attrs
            }
        }
        return true;
    }

    private static void fillVarsWithDomainAttrs(DomainAttrItem item)
            throws UnknownHostException, ProxyConfException {
        
        String defaultVal = null;
        mVars.put("vhn", item.virtualHostname);
        
        //resolve the virtual host name
        InetAddress vip = null;
        try {
            if (item.virtualIPAddress == null) {
                vip = InetAddress.getByName(item.virtualHostname);
            } else {
                vip = InetAddress.getByName(item.virtualIPAddress);
            }
        } catch (UnknownHostException e) {
            throw new ProxyConfException("virtual host name \"" + item.virtualHostname + "\" is not resolvable", e);
        }
        
        if (IPModeEnablerVar.getZimbraIPMode() != IPModeEnablerVar.IPMode.BOTH) {
            if (IPModeEnablerVar.getZimbraIPMode() == IPModeEnablerVar.IPMode.IPV4_ONLY &&
                    vip instanceof Inet6Address) {
                String msg = item.virtualIPAddress +
                        " is an IPv6 address but zimbraIPMode is 'ipv4'";
                mLog.error(msg);
                throw new ProxyConfException(msg);
            }

            if (IPModeEnablerVar.getZimbraIPMode() == IPModeEnablerVar.IPMode.IPV6_ONLY &&
                    vip instanceof Inet4Address) {
                String msg = item.virtualIPAddress +
                        " is an IPv4 address but zimbraIPMode is 'ipv6'";
                mLog.error(msg);
                throw new ProxyConfException(msg);
            }
        }

        if (vip instanceof Inet6Address) {
            //ipv6 address has to be enclosed with [ ]
            mVars.put("vip", "[" + vip.getHostAddress() + "]");
        } else {
            mVars.put("vip", vip.getHostAddress());
        }


        if ( item.sslCertificate != null ){
            mVars.put("ssl.crt", mDomainSSLDir + File.separator +
            item.domainName + mSSLCrtExt);
        }
        else{
            defaultVal = mVars.get("ssl.crt.default");
            mVars.put("ssl.crt", defaultVal);
        }

        if ( item.sslPrivateKey != null ){
            mVars.put("ssl.key", mDomainSSLDir + File.separator +
                    item.domainName + mSSLKeyExt);
        }
        else{
            defaultVal = mVars.get("ssl.key.default");
            mVars.put("ssl.key", defaultVal);
        }

        if ( item.clientCertMode != null ){
            mVars.put("ssl.clientcertmode", item.clientCertMode );
            if ( item.clientCertMode.equals("on") || item.clientCertMode.equals("optional")) {
                mVars.put("web.sso.certauth.enabled", "");
            } else {
                mVars.put("web.sso.certauth.enabled", "#");
            }
        }
        else {
            defaultVal = mVars.get("ssl.clientcertmode.default");
            mVars.put("ssl.clientcertmode", defaultVal );
        }

        if ( item.clientCertCa != null ){
            String clientCertCaPath = getClientCertCaPathByDomain(item.domainName);
            mVars.put("ssl.clientcertca", clientCertCaPath);
            //DnVhnVIPItem.clientCertCa stores the CA cert's content, other than path
            //if it is not null or "", loadReverseProxyVhnAndVIP() will save its content .
            //into clientCertCaPath before coming here
        }
        else{
            defaultVal = mVars.get("ssl.clientcertca.default");
            mVars.put("ssl.clientcertca", defaultVal);
        }
    }
    
    /**
     * Read from template file and translate the contents to conf.
     * The template will be cached and returned
     */
    private static List<String> expandTemplateAndCache(BufferedReader temp,
        BufferedWriter conf) throws IOException {
        String line;
        ArrayList<String> cache = new ArrayList<String>(50);
        while ((line = temp.readLine()) != null) {
            if (!line.startsWith("#"))
                cache.add(line); // cache only non-comment lines
            line = StringUtil.fillTemplate(line, mVars);
            conf.write(line);
            conf.newLine();
        }
        return cache;
    }

    /**
     * Read from template file and translate the contents to conf
     */
    private static void expandTemplateSimple(BufferedReader temp,
        BufferedWriter conf) throws IOException {
        String line;
        while ((line = temp.readLine()) != null) {
            line = StringUtil.fillTemplate(line, mVars);
            conf.write(line);
            conf.newLine();
        }
    }

    /**
     * Read from cache that holding template file's content and translate to
     * conf
     */
    private static void expandTempateFromCache(List<String> cache,
        BufferedWriter conf) throws IOException {
        for (String line : cache) {
            line = StringUtil.fillTemplate(line, mVars);
            conf.write(line);
            conf.newLine();
        }
    }

    /* Print the variable map */
    public static void displayVariables () throws ProxyConfException
    {
        SortedSet <String> sk = new TreeSet <String> (mVars.keySet());

        for (String k : sk) {
            mConfVars.get(k).write(System.out);
        }
    }

    public static void buildDefaultVars ()
    {
        mConfVars.put("core.workdir", new ProxyConfVar("core.workdir", null, mWorkingDir, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "Working Directory for NGINX worker processes"));
        mConfVars.put("core.includes", new ProxyConfVar("core.includes", null, mConfIncludesDir, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "Include directory (relative to ${core.workdir}/conf)"));
        mConfVars.put("core.cprefix", new ProxyConfVar("core.cprefix", null, mConfPrefix, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "Common config file prefix"));
        mConfVars.put("core.tprefix", new ProxyConfVar("core.tprefix", null, mTemplatePrefix, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "Common template file prefix"));
        mConfVars.put("core.ipv4only.enabled", new IPv4OnlyEnablerVar());
        mConfVars.put("core.ipv6only.enabled", new IPv6OnlyEnablerVar());
        mConfVars.put("core.ipboth.enabled", new IPBothEnablerVar());
        mConfVars.put("ssl.crt.default", new ProxyConfVar("ssl.crt.default", null, mDefaultSSLCrt, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "default nginx certificate file path"));
        mConfVars.put("ssl.key.default", new ProxyConfVar("ssl.key.default", null, mDefaultSSLKey, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "default nginx private key file path"));
        mConfVars.put("ssl.clientcertmode.default", new ProxyConfVar("ssl.clientcertmode.default", "zimbraReverseProxyClientCertMode", "off", ProxyConfValueType.STRING, ProxyConfOverride.SERVER,"enable authentication via X.509 Client Certificate in nginx proxy (https only)"));
        mConfVars.put("ssl.clientcertca.default", new ClientCertAuthDefaultCAVar());
        mConfVars.put("ssl.clientcertdepth.default", new ProxyConfVar("ssl.clientcertdepth.default", "zimbraReverseProxyClientCertDepth", new Integer(10), ProxyConfValueType.INTEGER, ProxyConfOverride.NONE,"indicate how depth the verification will load the ca chain. This is useful when client crt is signed by multiple intermediate ca"));
        mConfVars.put("main.user", new ProxyConfVar("main.user", null, ZIMBRA_UPSTREAM_NAME, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "The user as which the worker processes will run"));
        mConfVars.put("main.group", new ProxyConfVar("main.group", null, ZIMBRA_UPSTREAM_NAME, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "The group as which the worker processes will run"));
        mConfVars.put("main.workers", new ProxyConfVar("main.workers", "zimbraReverseProxyWorkerProcesses", new Integer(4), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER, "Number of worker processes"));
        mConfVars.put("main.pidfile", new ProxyConfVar("main.pidfile", null, mWorkingDir + "/log/nginx.pid", ProxyConfValueType.STRING, ProxyConfOverride.NONE, "PID file path (relative to ${core.workdir})"));
        mConfVars.put("main.logfile", new ProxyConfVar("main.logfile", null, mWorkingDir + "/log/nginx.log", ProxyConfValueType.STRING, ProxyConfOverride.NONE, "Log file path (relative to ${core.workdir})"));
        mConfVars.put("main.loglevel", new ProxyConfVar("main.loglevel", "zimbraReverseProxyLogLevel", "info", ProxyConfValueType.STRING, ProxyConfOverride.SERVER, "Log level - can be debug|info|notice|warn|error|crit"));
        mConfVars.put("main.connections", new ProxyConfVar("main.connections", "zimbraReverseProxyWorkerConnections", new Integer(10240), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER, "Maximum number of simultaneous connections per worker process"));
        mConfVars.put("main.krb5keytab", new ProxyConfVar("main.krb5keytab", "krb5_keytab", "/opt/zimbra/conf/krb5.keytab", ProxyConfValueType.STRING, ProxyConfOverride.LOCALCONFIG, "Path to kerberos keytab file used for GSSAPI authentication"));
        mConfVars.put("memcache.:servers", new MemcacheServersVar());
        mConfVars.put("memcache.timeout", new ProxyConfVar("memcache.timeout", "zimbraReverseProxyCacheFetchTimeout", new Long(3000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG, "Time (ms) given to a cache-fetch operation to complete"));
        mConfVars.put("memcache.reconnect", new ProxyConfVar("memcache.reconnect", "zimbraReverseProxyCacheReconnectInterval", new Long(60000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG, "Time (ms) after which NGINX will attempt to re-establish a broken connection to a memcache server"));
        /* deprecated */ mConfVars.put("memcache.unqual", new ProxyConfVar("memcache.unqual", null, false, ProxyConfValueType.BOOLEAN, ProxyConfOverride.NONE, "Deprecated - always set to false"));
        mConfVars.put("memcache.ttl", new ProxyConfVar("memcache.ttl", "zimbraReverseProxyCacheEntryTTL", new Long(3600000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG, "Time interval (ms) for which cached entries remain in memcache"));
        mConfVars.put("mail.ctimeout", new ProxyConfVar("mail.ctimeout", "zimbraReverseProxyConnectTimeout", new Long(120000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "Time interval (ms) after which a POP/IMAP proxy connection to a remote host will give up"));
        /* deprecated */mConfVars.put("mail.timeout", new ProxyConfVar("mail.timeout", "zimbraReverseProxyInactivityTimeout", new Long(3600000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "Time interval (ms) after which, if a POP/IMAP connection is inactive, it will be automatically disconnected"));
        mConfVars.put("mail.pop3.timeout", new ProxyConfVar("mail.pop3.timeout", "pop3_max_idle_time", 60, ProxyConfValueType.INTEGER, ProxyConfOverride.LOCALCONFIG, "pop3 network timeout before authentication"));
        mConfVars.put("mail.pop3.proxytimeout", new ProxyConfVar("mail.pop3.proxytimeout", "pop3_max_idle_time", 60, ProxyConfValueType.INTEGER, ProxyConfOverride.LOCALCONFIG, "pop3 network timeout after authentication"));
        mConfVars.put("mail.imap.timeout", new ProxyConfVar("mail.imap.timeout", "imap_max_idle_time", 60, ProxyConfValueType.INTEGER, ProxyConfOverride.LOCALCONFIG, "imap network timeout before authentication"));
        mConfVars.put("mail.imap.proxytimeout", new TimeoutVar("mail.imap.proxytimeout", "imap_authenticated_max_idle_time", 1800, ProxyConfOverride.LOCALCONFIG, 300, "imap network timeout after authentication"));
        mConfVars.put("mail.passerrors", new ProxyConfVar("mail.passerrors", "zimbraReverseProxyPassErrors", true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.SERVER, "Indicates whether mail proxy will pass any protocol specific errors from the upstream server back to the downstream client"));
        /* deprecated */ mConfVars.put("mail.:auth_http", new AuthHttpHandlersVar());
        mConfVars.put("mail.auth_http_timeout", new ProxyConfVar("mail.auth_http_timeout", "zimbraReverseProxyRouteLookupTimeout", new Long(15000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER,"Time interval (ms) given to mail route lookup handler to respond to route lookup request (after this time elapses, Proxy fails over to next handler, or fails the request if there are no more lookup handlers)"));
        /* deprecated */ mConfVars.put("mail.auth_http_timeout_cache", new ProxyConfVar("mail.auth_http_timeout_cache", "zimbraReverseProxyRouteLookupTimeoutCache", new Long(60000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER,"Time interval (ms) given to mail route lookup handler to cache a failed response to route a previous lookup request (after this time elapses, Proxy retries this host)"));
        mConfVars.put("mail.authwait", new ProxyConfVar("mail.authwait", "zimbraReverseProxyAuthWaitInterval", new Long(10000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG, "Time delay (ms) after which an incorrect POP/IMAP login attempt will be rejected"));
        mConfVars.put("mail.pop3capa", new Pop3CapaVar());
        mConfVars.put("mail.imapcapa", new ImapCapaVar());
        mConfVars.put("mail.imapid", new ProxyConfVar("mail.imapid", null, "\"NAME\" \"ZCS\" \"VERSION\" \"" + BuildInfo.VERSION + "\" \"RELEASE\" \"" + BuildInfo.RELEASE + "\"", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "NGINX response to IMAP ID command"));
        /* deprecated */ mConfVars.put("mail.dpasswd", new ProxyConfVar("mail.dpasswd", "ldap_nginx_password", "zmnginx", ProxyConfValueType.STRING, ProxyConfOverride.LOCALCONFIG, "Password for master credentials used by NGINX to log in to upstream for GSSAPI authentication"));
        mConfVars.put("mail.defaultrealm", new ProxyConfVar("mail.defaultrealm", "zimbraReverseProxyDefaultRealm", "", ProxyConfValueType.STRING, ProxyConfOverride.SERVER, "Default SASL realm used in case Kerberos principal does not contain realm information"));
        mConfVars.put("mail.sasl_host_from_ip", new SaslHostFromIPVar());
        mConfVars.put("mail.saslapp", new ProxyConfVar("mail.saslapp", null, "nginx", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "Application name used by NGINX to initialize SASL authentication"));
        mConfVars.put("mail.ipmax", new ProxyConfVar("mail.ipmax", "zimbraReverseProxyIPLoginLimit", new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.CONFIG,"IP Login Limit (Throttle) - 0 means infinity"));
        mConfVars.put("mail.ipttl", new ProxyConfVar("mail.ipttl", "zimbraReverseProxyIPLoginLimitTime", new Long(3600000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG,"Time interval (ms) after which IP Login Counter is reset"));
        mConfVars.put("mail.iprej", new ProxyConfVar("mail.iprej", "zimbraReverseProxyIpThrottleMsg", "Login rejected from this IP", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Rejection message for IP throttle"));
        mConfVars.put("mail.usermax", new ProxyConfVar("mail.usermax", "zimbraReverseProxyUserLoginLimit", new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.CONFIG,"User Login Limit (Throttle) - 0 means infinity"));
        mConfVars.put("mail.userttl", new ProxyConfVar("mail.userttl", "zimbraReverseProxyUserLoginLimitTime", new Long(3600000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG,"Time interval (ms) after which User Login Counter is reset"));
        mConfVars.put("mail.userrej", new ProxyConfVar("mail.userrej", "zimbraReverseProxyUserThrottleMsg", "Login rejected for this user", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Rejection message for User throttle"));
        mConfVars.put("mail.upstream.pop3xoip", new ProxyConfVar("mail.upstream.pop3xoip", "zimbraReverseProxySendPop3Xoip", true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.CONFIG,"Whether NGINX issues the POP3 XOIP command to the upstream server prior to logging in (audit purpose)"));
        mConfVars.put("mail.upstream.imapid", new ProxyConfVar("mail.upstream.imapid", "zimbraReverseProxySendImapId", true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.CONFIG,"Whether NGINX issues the IMAP ID command to the upstream server prior to logging in (audit purpose)"));
        mConfVars.put("mail.ssl.preferserverciphers", new ProxyConfVar("mail.ssl.preferserverciphers", null, true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.CONFIG,"Requires protocols SSLv3 and TLSv1 server ciphers be preferred over the client's ciphers"));
        mConfVars.put("mail.ssl.ciphers", new ProxyConfVar("mail.ssl.ciphers", "zimbraReverseProxySSLCiphers", "!SSLv2:!MD5:HIGH", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Permitted ciphers for mail proxy"));
        /* deprecated */ mConfVars.put("mail.ssl.cert", new ProxyConfVar("mail.ssl.cert", null, "/opt/zimbra/conf/nginx.crt", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Mail Proxy SSL certificate file"));
        /* deprecated */ mConfVars.put("mail.ssl.key", new ProxyConfVar("mail.ssl.key", null, "/opt/zimbra/conf/nginx.key", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Mail Proxy SSL certificate key"));
        mConfVars.put("mail.imap.authplain.enabled", new ProxyConfVar("mail.imap.authplain.enabled", "zimbraReverseProxyImapSaslPlainEnabled", true, ProxyConfValueType.ENABLER, ProxyConfOverride.CONFIG,"Whether SASL PLAIN is enabled for IMAP"));
        mConfVars.put("mail.imap.authgssapi.enabled", new ProxyConfVar("mail.imap.authgssapi.enabled", "zimbraReverseProxyImapSaslGssapiEnabled", false, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Whether SASL GSSAPI is enabled for IMAP"));
        mConfVars.put("mail.pop3.authplain.enabled", new ProxyConfVar("mail.pop3.authplain.enabled", "zimbraReverseProxyPop3SaslPlainEnabled", true, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Whether SASL PLAIN is enabled for POP3"));
        mConfVars.put("mail.pop3.authgssapi.enabled", new ProxyConfVar("mail.pop3.authgssapi.enabled", "zimbraReverseProxyPop3SaslGssapiEnabled", false, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Whether SASL GSSAPI is enabled for POP3"));
        mConfVars.put("mail.imap.literalauth", new ProxyConfVar("mail.imap.literalauth", null, true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.CONFIG,"Whether NGINX uses literal strings for user name/password when logging in to upstream IMAP server - if false, NGINX uses quoted strings"));
        mConfVars.put("mail.imap.port", new ProxyConfVar("mail.imap.port", Provisioning.A_zimbraImapProxyBindPort, new Integer(143), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Mail Proxy IMAP Port"));
        mConfVars.put("mail.imap.tls", new ProxyConfVar("mail.imap.tls", "zimbraReverseProxyImapStartTlsMode", "only", ProxyConfValueType.STRING, ProxyConfOverride.SERVER,"TLS support for IMAP - can be on|off|only - on indicates TLS support present, off indicates TLS support absent, only indicates TLS is enforced on unsecure channel"));
        mConfVars.put("mail.imaps.port", new ProxyConfVar("mail.imaps.port", Provisioning.A_zimbraImapSSLProxyBindPort, new Integer(993), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Mail Proxy IMAPS Port"));
        mConfVars.put("mail.pop3.port", new ProxyConfVar("mail.pop3.port", Provisioning.A_zimbraPop3ProxyBindPort, new Integer(110), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Mail Proxy POP3 Port"));
        mConfVars.put("mail.pop3.tls", new ProxyConfVar("mail.pop3.tls", "zimbraReverseProxyPop3StartTlsMode", "only", ProxyConfValueType.STRING, ProxyConfOverride.SERVER,"TLS support for POP3 - can be on|off|only - on indicates TLS support present, off indicates TLS support absent, only indicates TLS is enforced on unsecure channel"));
        mConfVars.put("mail.pop3s.port", new ProxyConfVar("mail.pop3s.port", Provisioning.A_zimbraPop3SSLProxyBindPort, new Integer(995), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Mail Proxy POP3S Port"));
        mConfVars.put("mail.imap.greeting", new ImapGreetingVar());
        mConfVars.put("mail.pop3.greeting", new Pop3GreetingVar());
        mConfVars.put("mail.enabled", new ProxyConfVar("mail.enabled", "zimbraReverseProxyMailEnabled", true, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Indicates whether Mail Proxy is enabled"));
        mConfVars.put("mail.proxy.ssl", new ProxyConfVar("mail.proxy.ssl", "zimbraReverseProxySSLToUpstreamEnabled", false, ProxyConfValueType.BOOLEAN, ProxyConfOverride.SERVER, "Indicates whether using SSL to connect to upstream mail server"));
        mConfVars.put("web.mailmode", new ProxyConfVar("web.mailmode", Provisioning.A_zimbraReverseProxyMailMode, "both", ProxyConfValueType.STRING, ProxyConfOverride.SERVER,"Reverse Proxy Mail Mode - can be http|https|both|redirect|mixed"));
        mConfVars.put("web.upstream.name", new ProxyConfVar("web.upstream.name", null, ZIMBRA_UPSTREAM_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Symbolic name for HTTP upstream cluster"));
        mConfVars.put("web.ssl.upstream.name", new ProxyConfVar("web.ssl.upstream.name", null, ZIMBRA_SSL_UPSTREAM_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Symbolic name for HTTPS upstream cluster"));
        mConfVars.put("web.upstream.:servers", new WebUpstreamServersVar());
        mConfVars.put("web.server_names.size", new ProxyConfVar("web.server_names.size", null, DEFAULT_SERVER_NAME_HASH_MAX_SIZE, ProxyConfValueType.INTEGER, ProxyConfOverride.NONE, "the server names hash max size, needed to be increased if too many virtual host names are added"));
        mConfVars.put("web.ssl.upstream.:servers", new WebSSLUpstreamServersVar());
        /* deprecated */ mConfVars.put("web.:routehandlers", new RouteHandlersVar());
        /* deprecated */ mConfVars.put("web.routetimeout", new ProxyConfVar("web.routetimeout", "zimbraReverseProxyRouteLookupTimeout", new Long(15000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER,"Time interval (ms) given to web route lookup handler to respond to route lookup request (after this time elapses, Proxy fails over to next handler, or fails the request if there are no more lookup handlers)"));
        mConfVars.put("web.uploadmax", new ProxyConfVar("web.uploadmax", "zimbraFileUploadMaxSize", new Long(10485760), ProxyConfValueType.LONG, ProxyConfOverride.SERVER,"Maximum accepted client request body size (indicated by Content-Length) - if content length exceeds this limit, then request fails with HTTP 413"));
        mConfVars.put("web.:error_pages", new ErrorPagesVar());
        mConfVars.put("web.http.port", new ProxyConfVar("web.http.port", Provisioning.A_zimbraMailProxyPort, new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Web Proxy HTTP Port"));
        mConfVars.put("web.http.maxbody", new ProxyConfVar("web.http.maxbody", "zimbraFileUploadMaxSize", new Long(10485760), ProxyConfValueType.LONG, ProxyConfOverride.SERVER,"Maximum accepted client request body size (indicated by Content-Length) - if content length exceeds this limit, then request fails with HTTP 413"));
        mConfVars.put("web.https.port", new ProxyConfVar("web.https.port", Provisioning.A_zimbraMailSSLProxyPort, new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Web Proxy HTTPS Port"));
        mConfVars.put("web.https.maxbody", new ProxyConfVar("web.https.maxbody", "zimbraFileUploadMaxSize", new Long(10485760), ProxyConfValueType.LONG, ProxyConfOverride.SERVER,"Maximum accepted client request body size (indicated by Content-Length) - if content length exceeds this limit, then request fails with HTTP 413"));
        mConfVars.put("web.ssl.preferserverciphers", new ProxyConfVar("web.ssl.preferserverciphers", null, true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.CONFIG,"Requires protocols SSLv3 and TLSv1 server ciphers be preferred over the client's ciphers"));
        /* deprecated */ mConfVars.put("web.ssl.cert", new ProxyConfVar("web.ssl.cert", null, "/opt/zimbra/conf/nginx.crt", ProxyConfValueType.STRING, ProxyConfOverride.NONE,"Web Proxy SSL certificate path"));
        /* deprecated */ mConfVars.put("web.ssl.key", new ProxyConfVar("web.ssl.key", null, "/opt/zimbra/conf/nginx.key", ProxyConfValueType.STRING, ProxyConfOverride.NONE,"Web Proxy SSL certificate key"));
        mConfVars.put("web.ssl.ciphers", new ProxyConfVar("web.ssl.ciphers", "zimbraReverseProxySSLCiphers", "!SSLv2:!MD5:HIGH", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "Permitted ciphers for mail proxy"));
        mConfVars.put("web.http.uport", new ProxyConfVar("web.http.uport", Provisioning.A_zimbraMailPort, new Integer(80), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Web upstream server port"));
        mConfVars.put("web.upstream.read.timeout", new TimeInSecVarWrapper(new ProxyConfVar("web.upstream.read.timeout", "zimbraReverseProxyUpstreamReadTimeout", new Long(60), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "upstream read timeout")));
        mConfVars.put("web.upstream.send.timeout", new TimeInSecVarWrapper(new ProxyConfVar("web.upstream.send.timeout", "zimbraReverseProxyUpstreamSendTimeout", new Long(60), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "upstream send timeout")));
        mConfVars.put("web.upstream.polling.timeout", new TimeInSecVarWrapper(new ProxyConfVar("web.upstream.polling.timeout", "zimbraReverseProxyUpstreamPollingTimeout", new Long(3600), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "the response timeout for Microsoft Active Sync polling")));
        mConfVars.put("web.enabled", new ProxyConfVar("web.enabled", "zimbraReverseProxyHttpEnabled", false, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER, "Indicates whether HTTP proxying is enabled"));
        mConfVars.put("web.http.enabled", new HttpEnablerVar());
        mConfVars.put("web.https.enabled", new HttpsEnablerVar());
        mConfVars.put("web.upstream.target", new WebProxyUpstreamTargetVar());
        mConfVars.put("zmlookup.:handlers", new ZMLookupHandlerVar());
        mConfVars.put("zmlookup.timeout", new ProxyConfVar("zmlookup.timeout", "zimbraReverseProxyRouteLookupTimeout", new Long(15000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "Time interval (ms) given to lookup handler to respond to route lookup request (after this time elapses, Proxy fails over to next handler, or fails the request if there are no more lookup handlers)"));
        mConfVars.put("zmlookup.retryinterval", new ProxyConfVar("zmlookup.retryinterval", "zimbraReverseProxyRouteLookupTimeoutCache", new Long(60000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER,"Time interval (ms) given to lookup handler to cache a failed response to route a previous lookup request (after this time elapses, Proxy retries this host)"));
        mConfVars.put("zmlookup.dpasswd", new ProxyConfVar("zmlookup.dpasswd", "ldap_nginx_password", "zmnginx", ProxyConfValueType.STRING, ProxyConfOverride.LOCALCONFIG, "Password for master credentials used by NGINX to log in to upstream for GSSAPI authentication"));
        mConfVars.put("zmlookup.caching", new ProxyConfVar("zmlookup.caching", null, true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.NONE, "Whether to turn on nginx lookup caching"));
        mConfVars.put("zmlookup.unqual", new ProxyConfVar("zmlookup.unqual", null, false, ProxyConfValueType.BOOLEAN, ProxyConfOverride.NONE, "Deprecated - always set to false"));
        mConfVars.put("web.sso.certauth.port", new ProxyConfVar("web.sso.certauth.port", Provisioning.A_zimbraMailSSLProxyClientCertPort, new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"reverse proxy client cert auth port"));
        mConfVars.put("web.sso.certauth.default.enabled", new ZMSSOCertAuthDefaultEnablerVar());
        mConfVars.put("web.sso.enabled", new ZMSSOEnablerVar());
        mConfVars.put("web.sso.default.enabled", new ZMSSODefaultEnablerVar());
        mConfVars.put("web.admin.default.enabled", new ProxyConfVar("web.amdin.default.enabled", "zimbraReverseProxyAdminEnabled", new Boolean(false), ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER, "Inidicate whether admin console proxy is enabled"));
        mConfVars.put("web.admin.port", new ProxyConfVar("web.admin.port", "zimbraAdminProxyPort", new Integer(9071), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER, "Admin console proxy port"));
        mConfVars.put("web.admin.uport", new ProxyConfVar("web.admin.uport", "zimbraAdminPort", new Integer(7071), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER, "Admin console upstream port"));
        mConfVars.put("web.admin.upstream.name", new ProxyConfVar("web.admin.upstream.name", null, ZIMBRA_ADMIN_CONSOLE_UPSTREAM_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "Symbolic name for admin console upstream cluster"));
        mConfVars.put("web.admin.upstream.:servers", new WebAdminUpstreamServersVar());
    }

    /* update the default variable map from the active configuration */
    public static void updateDefaultVars ()
        throws ServiceException, ProxyConfException
    {
        Set<String> keys = mConfVars.keySet();
        for (String key: keys) {
            mConfVars.get(key).update();
            mVars.put(key,mConfVars.get(key).confValue());
        }
    }

    public static void overrideDefaultVars (CommandLine cl)
    {
        String[] overrides = cl.getOptionValues('c');

        if (overrides != null) {
            for (String o : overrides) {
                mLog.debug("Processing config override " + o);
                int e = o.indexOf ("=");
                if (e <= 0) {
                    mLog.info("Ignoring config override " + o + " because it is not of the form name=value");
                } else {
                    String k = o.substring(0,e);
                    String v = o.substring(e+1);

                    if (mVars.containsKey(k)) {
                        mLog.info("Overriding config variable " + k + " with " + v);
                        mVars.put(k,v);
                    } else {
                        mLog.info("Ignoring non-existent config variable " + k);
                    }
                }
            }
        }
    }

    /* Indicate whether configuration is valid, taking into consideration "essential" configuration values */
    @SuppressWarnings("unchecked")
    public static boolean isWorkableConf ()
    {
        boolean webEnabled, mailEnabled, validConf = true;
        ArrayList<String> webUpstreamServers, zmLookupHandlers;

        webEnabled = (Boolean)mConfVars.get("web.enabled").rawValue();
        mailEnabled = (Boolean)mConfVars.get("mail.enabled").rawValue();

        webUpstreamServers = (ArrayList<String>) mConfVars.get("web.upstream.:servers").rawValue();
        zmLookupHandlers = (ArrayList<String>) mConfVars.get("zmlookup.:handlers").rawValue();
        //mailRouteHandlers = (ArrayList<String>) mConfVars.get("mail.:auth_http").rawValue();

//        if (mailEnabled && (mailRouteHandlers.size() == 0)) {
//            mLog.info("Mail is enabled but there are no route lookup handlers (Config will not be written)");
//            validConf = false;
//        }

        if (webEnabled && (webUpstreamServers.size() == 0)) {
            mLog.info("Web is enabled but there are no upstream servers (Config will not be written)");
            validConf = false;
        }
        
        if ((webEnabled || mailEnabled) && (zmLookupHandlers.size() == 0)) {
            mLog.info("Proxy is enabled but there are no lookup hanlders (Config will not be written)");
            validConf = false;
        }

        return validConf;
    }

    public static int createConf(String[] args) throws ServiceException,
        ProxyConfException {
        int exitCode = 0;
        CommandLine cl = parseArgs(args);

        if (cl == null) {
            exitCode = 1;
            return(exitCode);
        }

        if (cl.hasOption('v')) { //BUG 51624, must initialize log4j first
            CliUtil.toolSetup("DEBUG");
        } else {
            CliUtil.toolSetup("INFO");
        }
        
        mProv = Provisioning.getInstance();
        ProxyConfVar.configSource = mProv.getConfig();
        ProxyConfVar.serverSource = ProxyConfVar.configSource;

        if (cl.hasOption('h')) {
            usage(null);
            exitCode = 0;
            return(exitCode);
        }

        if (cl.hasOption('n')) {
            mDryRun = true;
        }

        if (cl.hasOption('w')) {
            mWorkingDir = cl.getOptionValue('w');
            mConfDir = mWorkingDir + "/conf";
            mTemplateDir = mWorkingDir + "/conf/nginx/templates";
            mConfIncludesDir = mConfDir + "/" + mIncDir;
        }

        if (cl.hasOption('i')) {
            mIncDir = cl.getOptionValue('i');
            mConfIncludesDir = mConfDir + "/" + mIncDir;
        }

        if (cl.hasOption('t')) {
            mTemplateDir = cl.getOptionValue('t');
        }

        mLog.debug("Working Directory: " + mWorkingDir);
        mLog.debug("Template Directory: " + mTemplateDir);
        mLog.debug("Config Includes Directory: " + mConfIncludesDir);

        if (cl.hasOption('p')) {
            mConfPrefix = cl.getOptionValue('p');
            mTemplatePrefix = mConfPrefix;
        }

        if (cl.hasOption('P')) {
            mTemplatePrefix = cl.getOptionValue('P');
        }

        mLog.debug("Config File Prefix: " + mConfPrefix);
        mLog.debug("Template File Prefix: " + mTemplatePrefix);

        /* set up the default variable map */
        mLog.debug("Building Default Variable Map");
        buildDefaultVars();

        if (cl.hasOption('d')) {
            displayVariables();
            exitCode = 0;
            return(exitCode);
        }


        /* If a server object has been provided, then use that */
        if (cl.hasOption('s')) {
            mHost = cl.getOptionValue('s');
            mLog.info("Loading server object: " + mHost);
            try {
                mServer = getServer (mHost);
                ProxyConfVar.serverSource = mServer;
            } catch (ProxyConfException pe) {
                mLog.error("Cannot load server object. Make sure the server specified with -s exists");
                exitCode = 1;
                return(exitCode);
            }
        }

        mGenConfPerVhn = mServer.getBooleanAttr("zimbraReverseProxyGenConfigPerVirtualHostname", false);

        /* upgrade the variable map from the config in force */
        mLog.debug("Loading Attrs in Domain Level");
        mDomainReverseProxyAttrs = loadDomainReverseProxyAttrs();

        //bug 69648, manually set the server name hash size when too many server names are added
        int size = DEFAULT_SERVER_NAME_HASH_MAX_SIZE;
        if (mDomainReverseProxyAttrs.size() > DEFAULT_SERVER_NAME_HASH_MAX_SIZE) {
            size = mDomainReverseProxyAttrs.size() + 100; // add a little more than needed
        }

        mConfVars.get("web.server_names.size").mValue = size;
        
        mLog.debug("Updating Default Variable Map");
        updateDefaultVars();

        mLog.debug("Processing Config Overrides");
        overrideDefaultVars(cl);
        
        String clientCA = loadAllClientCertCA();
        writeClientCAtoFile(clientCA);
       
        if (cl.hasOption('D')) {
            displayVariables();
            exitCode = 0;
            return(exitCode);
        }

        if (!isWorkableConf()) {
            mLog.error("Configuration is not valid because no route lookup handlers exist, or because no HTTP upstream servers were found");
            mLog.error("Please ensure that the output of 'zmprov garpu' and 'zmprov garpb' returns at least one entry");
            exitCode = 1;
            return(exitCode);
        }

        exitCode = 0;

        try {
            File confDir = new File(mConfDir,"");
            String confPath = confDir.getAbsolutePath();
            if (!confDir.canRead()) {
                throw new ProxyConfException ("Cannot read configuration directory " + confPath);
            }
            if (!confDir.canWrite()) {
                throw new ProxyConfException ("Cannot write to configuration directory " + confPath);
            }
            if (!confDir.exists()) {
                throw new ProxyConfException ("Configuration directory " + confDir.getAbsolutePath() + " does not exist");
            }
            
            expandTemplate(new File(mTemplateDir, getCoreConfTemplate()), new File(mConfDir,getCoreConf())); /* Only core nginx conf goes to mConfDir, rest to mConfIncludesDir */
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("main")), new File(mConfIncludesDir, getConfFileName("main")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("memcache")), new File(mConfIncludesDir, getConfFileName("memcache")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("zmlookup")), new File(mConfIncludesDir, getConfFileName("zmlookup")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("mail")), new File(mConfIncludesDir, getConfFileName("mail")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("mail.imap")), new File(mConfIncludesDir, getConfFileName("mail.imap")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("mail.imap.default")), new File(mConfIncludesDir, getConfFileName("mail.imap.default")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("mail.imaps")), new File(mConfIncludesDir, getConfFileName("mail.imaps")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("mail.imaps.default")), new File(mConfIncludesDir, getConfFileName("mail.imaps.default")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("mail.pop3")), new File(mConfIncludesDir, getConfFileName("mail.pop3")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("mail.pop3.default")), new File(mConfIncludesDir, getConfFileName("mail.pop3.default")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("mail.pop3s")), new File(mConfIncludesDir, getConfFileName("mail.pop3s")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("mail.pop3s.default")), new File(mConfIncludesDir, getConfFileName("mail.pop3s.default")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("web")), new File(mConfIncludesDir,getConfFileName("web")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("web.http")), new File(mConfIncludesDir, getConfFileName("web.http")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("web.http.default")), new File(mConfIncludesDir, getConfFileName("web.http.default")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("web.https")), new File(mConfIncludesDir, getConfFileName("web.https")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("web.https.default")), new File(mConfIncludesDir, getConfFileName("web.https.default")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("web.sso")), new File(mConfIncludesDir, getConfFileName("web.sso")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("web.sso.default")), new File(mConfIncludesDir, getConfFileName("web.sso.default")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("web.admin")), new File(mConfIncludesDir, getConfFileName("web.admin")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("web.admin.default")), new File(mConfIncludesDir, getConfFileName("web.admin.default")));
            expandTemplate(new File(mTemplateDir, getWebHttpModeConfTemplate("http")), new File(mConfIncludesDir, getWebHttpModeConf("http")));
            expandTemplate(new File(mTemplateDir, getWebHttpModeConfTemplate("https")), new File(mConfIncludesDir, getWebHttpModeConf("https")));
            expandTemplate(new File(mTemplateDir, getWebHttpModeConfTemplate("both")), new File(mConfIncludesDir, getWebHttpModeConf("both")));
            expandTemplate(new File(mTemplateDir, getWebHttpModeConfTemplate("redirect")), new File(mConfIncludesDir, getWebHttpModeConf("redirect")));
            expandTemplate(new File(mTemplateDir, getWebHttpModeConfTemplate("mixed")), new File(mConfIncludesDir, getWebHttpModeConf("mixed")));
            expandTemplate(new File(mTemplateDir, getWebHttpSModeConfTemplate("http")), new File(mConfIncludesDir, getWebHttpSModeConf("http")));
            expandTemplate(new File(mTemplateDir, getWebHttpSModeConfTemplate("https")), new File(mConfIncludesDir, getWebHttpSModeConf("https")));
            expandTemplate(new File(mTemplateDir, getWebHttpSModeConfTemplate("both")), new File(mConfIncludesDir, getWebHttpSModeConf("both")));
            expandTemplate(new File(mTemplateDir, getWebHttpSModeConfTemplate("redirect")), new File(mConfIncludesDir, getWebHttpSModeConf("redirect")));
            expandTemplate(new File(mTemplateDir, getWebHttpSModeConfTemplate("mixed")), new File(mConfIncludesDir, getWebHttpSModeConf("mixed")));
        } catch (ProxyConfException pe) {
            mLog.error("Error while expanding templates: " + pe.getMessage());
            appendConfGenResultToConf("__CONF_GEN_ERROR__:" + pe.getMessage());
            exitCode = 1;
        } catch (SecurityException se) {
            mLog.error("Error while expanding templates: " + se.getMessage());
            appendConfGenResultToConf("__CONF_GEN_ERROR__:" + se.getMessage());
            exitCode = 1;
        }
        if (exitCode != 1) {
            mLog.info("Proxy configuration files are generated successfully");
            appendConfGenResultToConf("__SUCCESS__");
        } else {
            mLog.info("Proxy configuration files generation is interrupted by errors");
        }
        
        return (exitCode);
    }

    /**
     * bug 66072#c3, always append the conf generation result
     * to <zimbr home>/conf/nginx.conf. In this way, zmnginxctl
     * restart can detect the problem.
     * @param text
     */
    private static void appendConfGenResultToConf(String text) {
        File conf = new File(mConfDir, getCoreConf());
        if (!conf.exists()) {
            return;
        }

        FileWriter writer;
        try {
            writer = new FileWriter(conf, true);
            writer.write("\n#" + text + "\n");
            writer.close();
        } catch (IOException e) {
            //do nothing
        }
    }

    private static void writeClientCAtoFile(String clientCA)
            throws ServiceException {
        int exitCode;
        ProxyConfVar clientCAEnabledVar = null;

        if (ProxyConfUtil.isEmptyString(clientCA)) {
            clientCAEnabledVar = new ProxyConfVar(
                    "ssl.clientcertca.enabled", null, false, 
                    ProxyConfValueType.ENABLER, 
                    ProxyConfOverride.CUSTOM, "is there valid client ca cert");
            
            if(isClientCertVerifyEnabled() || isDomainClientCertVerifyEnabled()) {
                mLog.error("Client certificate verification is enabled but no client cert ca is provided");
                exitCode = 1;
                System.exit(exitCode);
            }
            
        } else {
            clientCAEnabledVar = new ProxyConfVar(
                    "ssl.clientcertca.enabled", null, true, 
                    ProxyConfValueType.ENABLER, 
                    ProxyConfOverride.CUSTOM, "is there valid client ca cert");
             mLog.debug("Write Client CA file");
             ProxyConfUtil.writeContentToFile(clientCA, getDefaultClientCertCaPath());
        }
        mConfVars.put("ssl.clientcertca.enabled", clientCAEnabledVar);
        try {
            mVars.put("ssl.clientcertca.enabled", clientCAEnabledVar.confValue());
        } catch (ProxyConfException e) {
            mLog.error("ProxyConfException during format ssl.clientcertca.enabled", e);
            System.exit(1);
        }
    }
    
    /**
     * check whether client cert verify is enabled in server level
     * @return
     */
    static boolean isClientCertVerifyEnabled() {
        String globalMode = ProxyConfVar.serverSource.getAttr(
                Provisioning.A_zimbraReverseProxyClientCertMode, "off");
        
        if (globalMode.equals("on") ||
            globalMode.equals("optional")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * check whether client cert verify is enabled in domain level
     * @return
     */
    static boolean isDomainClientCertVerifyEnabled() {
        for (DomainAttrItem item: mDomainReverseProxyAttrs) {
            if (item.clientCertMode != null &&
                (item.clientCertMode.equals("on") ||
                 item.clientCertMode.equals("optional"))) {
                return true;
            }
        }
        
        return false;
    }

    public static void main(String[] args) throws ServiceException, ProxyConfException {
        int exitCode = createConf(args);
        System.exit(exitCode);
    }
}


class ProxyConfUtil{

    public static void writeContentToFile( String content, String filePath )
        throws ServiceException {

        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));

            bw.write(content);
            bw.flush();
            bw.close();

        }catch( IOException e ){
            throw ServiceException.FAILURE("Cannot write the content (" + content + ") to " + filePath, e);
        }
    }

    public static boolean isEmptyString( String target ){
        return (target == null) || (target.trim().equalsIgnoreCase(""));
    }

}
