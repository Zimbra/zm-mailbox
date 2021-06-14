/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.account.Key;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.extension.ExtensionDispatcherServlet;
import com.zimbra.cs.service.SharedFileServletContext;

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
    public static final String UNKNOWN_HEADER_NAME = "X-Zimbra-Unknown-Header";
    public static final Pattern RE_HEADER = Pattern.compile("^([^:]+):\\s+(.*)$");

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
        ps.println ("  Default Value:         " + ((mDefault == null) ? "(none)" : mDefault.toString()));
        ps.println ("  Current Value:         " + ((mValue == null) ? "(none)" : mValue.toString()));
        ps.println ("  Config Text:           " + ((mValue == null) ? "(none)" : format(mValue)));
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

    boolean isValidUpstream(Server server, String serverName) {
        boolean isTarget = server.getBooleanAttr(
                Provisioning.A_zimbraReverseProxyLookupTarget, false);
        if(!isTarget) {
            return false;
        }

        String mode = server.getAttr(Provisioning.A_zimbraMailMode, "");
        if (mode.equalsIgnoreCase(Provisioning.MailMode.http.toString())
                || mode.equalsIgnoreCase(Provisioning.MailMode.mixed
                        .toString())
                || mode.equalsIgnoreCase(Provisioning.MailMode.both
                        .toString())
                || mode.equalsIgnoreCase(Provisioning.MailMode.redirect
                        .toString())
                || mode.equalsIgnoreCase(Provisioning.MailMode.https
                        .toString())) {
            return true;
        } else {
            mLog.warn("Upstream: Ignoring server: " + serverName
                    + " ,because its mail mode is: " + (mode.equals("")?"EMPTY":mode));
            return false;
        }
    }

    String generateServerDirective(Server server, String serverName, String portName) {
        int serverPort = server.getIntAttr(portName, 0);
        return generateServerDirective(server, serverName, serverPort);
    }

    String generateServerDirective(Server server, String serverName, int serverPort) {
        int timeout = server.getIntAttr(
                Provisioning.A_zimbraMailProxyReconnectTimeout, 60);
        String version = server.getAttr(Provisioning.A_zimbraServerVersion, "");
        int maxFails = server.getIntAttr(Provisioning.A_zimbraMailProxyMaxFails, 1);
        if (maxFails != 1 && version != "") {
            return String.format("%s:%d fail_timeout=%ds max_fails=%d version=%s", serverName, serverPort,
                    timeout, maxFails, version);
        } else if (maxFails != 1) {
        	return String.format("%s:%d fail_timeout=%ds max_fails=%d", serverName, serverPort,
                    timeout, maxFails);
        } else if (version != "") {
        	return String.format("%s:%d fail_timeout=%ds version=%s", serverName, serverPort,
                    timeout, version);
        } else {
            return String.format("%s:%d fail_timeout=%ds", serverName, serverPort,
                    timeout);
        }
    }

    public static final class KeyValue {
        public final String key;
        public final String value;

        public KeyValue(String value) {
            this(ProxyConfVar.UNKNOWN_HEADER_NAME, value);
        }

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}

abstract class WebEnablerVar extends ProxyConfVar {

    public WebEnablerVar(String keyword, Object defaultValue,
            String description) {
        super(keyword, null, defaultValue,
                ProxyConfValueType.ENABLER, ProxyConfOverride.CUSTOM, description);
    }

    static String getZimbraReverseProxyMailMode() {
        return serverSource.getAttr(Provisioning.A_zimbraReverseProxyMailMode, "both");
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

class ProxyFairShmVar extends ProxyConfVar {

    public ProxyFairShmVar() {
        super("upstream.fair.shm.size", Provisioning.A_zimbraReverseProxyUpstreamFairShmSize, "",
                ProxyConfValueType.CUSTOM, ProxyConfOverride.CONFIG,
                "Controls the 'upstream_fair_shm_size' configuration in the proxy configuration file: nginx.conf.web.template.");
    }

    @Override
    public void update() {
        String setting = serverSource.getAttr(Provisioning.A_zimbraReverseProxyUpstreamFairShmSize, "32");

        try {
            if (Integer.parseInt(setting) < 32)
            {
                setting = "32";
            }
        }
        catch (NumberFormatException e)
        {
            mLog.info("Value provided in 'zimbraReverseProxyUpstreamFairShmSize': " + setting + " is invalid. Falling back to default value of 32.");
            setting = "32";
        }

        mValue = setting;
    }

    @Override
    public String format(Object o) {
       return "upstream_fair_shm_size " + mValue + "k;";
    }
}

class Pop3GreetingVar extends ProxyConfVar {

    public Pop3GreetingVar() {
        super("mail.pop3.greeting", Provisioning.A_zimbraReverseProxyPop3ExposeVersionOnBanner, "",
                ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,
                "Proxy IMAP banner message (contains build version if " +
                Provisioning.A_zimbraReverseProxyImapExposeVersionOnBanner + " is true)");
    }

    @Override
    public void update() {
        if (serverSource.getBooleanAttr(Provisioning.A_zimbraReverseProxyPop3ExposeVersionOnBanner, false)) {
            mValue = "+OK " + "Zimbra " + BuildInfo.VERSION + " POP3 ready";
        } else {
            mValue = "";
        }
    }
}

class ImapGreetingVar extends ProxyConfVar {

    public ImapGreetingVar() {
        super("mail.imap.greeting", Provisioning.A_zimbraReverseProxyImapExposeVersionOnBanner, "",
                ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,
                "Proxy IMAP banner message (contains build version if " +
                Provisioning.A_zimbraReverseProxyImapExposeVersionOnBanner + " is true)");
    }

    @Override
    public void update() {
        if (serverSource.getBooleanAttr(Provisioning.A_zimbraReverseProxyImapExposeVersionOnBanner, false)) {
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

class WebUpstreamClientServersVar extends ProxyConfVar {

    public WebUpstreamClientServersVar() {
        super("web.upstream.webclient.:servers", null, null, ProxyConfValueType.CUSTOM,
                ProxyConfOverride.CUSTOM,
                "List of upstream HTTP webclient servers used by Web Proxy");
    }

    @Override
    public void update() throws ServiceException, ProxyConfException {
        ArrayList<String> directives = new ArrayList<String>();
        String portName = configSource.getAttr(Provisioning.A_zimbraReverseProxyHttpPortAttribute, "");

        List<Server> webclientservers = mProv.getAllWebClientServers();
        for (Server server : webclientservers) {
            String serverName = server.getAttr(
                    Provisioning.A_zimbraServiceHostname, "");

            if (isValidUpstream(server, serverName)) {
                directives.add(generateServerDirective(server, serverName, portName));
                mLog.debug("Added server to HTTP webclient upstream: " + serverName);
            }
        }
        mValue = directives;
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

class ZMWebAvailableVar extends ProxyConfVar {

    public ZMWebAvailableVar() {
        super("web.available", null, false,
                ProxyConfValueType.ENABLER, ProxyConfOverride.CUSTOM,
                "Indicates whether there are available web client servers or not");
    }

    @Override
    public void update() throws ServiceException, ProxyConfException {
        WebUpstreamClientServersVar lhVar = new WebUpstreamClientServersVar();
        lhVar.update();
        @SuppressWarnings("unchecked")
        ArrayList<String> servers = (ArrayList<String>) lhVar.mValue;
        if (servers.isEmpty()) {
            mValue = false;
        }
        else {
            mValue = true;
        }
    }
}

class WebSSLUpstreamClientServersVar extends ProxyConfVar {

    public WebSSLUpstreamClientServersVar() {
        super("web.ssl.upstream.webclient.:servers", null, null, ProxyConfValueType.CUSTOM,
                ProxyConfOverride.CUSTOM,
                "List of upstream HTTPS webclient servers used by Web Proxy");
    }

    @Override
    public void update() throws ServiceException, ProxyConfException {
        ArrayList<String> directives = new ArrayList<String>();
        String portName = configSource.getAttr(Provisioning.A_zimbraReverseProxyHttpSSLPortAttribute, "");

        List<Server> webclientservers = mProv.getAllWebClientServers();
        for (Server server : webclientservers) {
            String serverName = server.getAttr(
                    Provisioning.A_zimbraServiceHostname, "");

            if (isValidUpstream(server, serverName)) {
                directives.add(generateServerDirective(server, serverName, portName));
                mLog.debug("Added server to HTTPS webclient upstream: " + serverName);
            }
        }
        mValue = directives;
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

class WebAdminUpstreamAdminClientServersVar extends ProxyConfVar {

    public WebAdminUpstreamAdminClientServersVar() {
        super("web.admin.upstream.:servers", null, null, ProxyConfValueType.CUSTOM,
                ProxyConfOverride.CUSTOM,
                "List of upstream HTTPS Admin client servers used by Web Proxy");
    }

    @Override
    public void update() throws ServiceException, ProxyConfException {
        ArrayList<String> directives = new ArrayList<String>();
        String portName = configSource.getAttr(Provisioning.A_zimbraReverseProxyAdminPortAttribute, "");

        List<Server> adminclientservers = mProv.getAllAdminClientServers();
        for (Server server : adminclientservers) {
            String serverName = server.getAttr(
                    Provisioning.A_zimbraServiceHostname, "");

            if (isValidUpstream(server, serverName)) {
                directives.add(generateServerDirective(server, serverName, portName));
                mLog.debug("Added server to HTTPS Admin client upstream: " + serverName);
            }
        }
        mValue = directives;
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

class MemcacheServersVar extends ProxyConfVar {

    public MemcacheServersVar() {
        super("memcache.:servers", null, null, ProxyConfValueType.CUSTOM,
                ProxyConfOverride.CUSTOM,
                "List of known memcache servers (i.e. servers having memcached service enabled)");
    }


    @Override
    public void update() throws ServiceException, ProxyConfException {
        ArrayList<String> servers = new ArrayList<String>();

        /* $(zmprov gamcs) */
        List<Server> mcs = mProv.getAllServers(Provisioning.SERVICE_MEMCACHED);
        for (Server mc : mcs) {
            String serverName = mc.getAttr(
                    Provisioning.A_zimbraServiceHostname, "");
            int serverPort = mc.getIntAttr(
                    Provisioning.A_zimbraMemcachedBindPort, 11211);
            try {
                InetAddress ip = ProxyConfUtil.getLookupTargetIPbyIPMode(serverName);

                Formatter f = new Formatter();
                if (ip instanceof Inet4Address) {
                    f.format("%s:%d", ip.getHostAddress(), serverPort);
                } else {
                    f.format("[%s]:%d", ip.getHostAddress(), serverPort);
                }

                servers.add(f.toString());
                f.close();
            }
            catch (ProxyConfException pce) {
                mLog.error("Error resolving memcached host name: '" + serverName + "'", pce);
            }
        }
        if (servers.isEmpty()) {
            throw new ProxyConfException ("No available memcached servers could be contacted");
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

/**
 * The variable for nginx "upstream" servers block
 * @author jiankuan
 *
 */
abstract class ServersVar extends ProxyConfVar {
    /**
     * The port attribute name
     */
    private final String mPortAttrName;

    public ServersVar(String key, String portAttrName, String description) {
        super(key, null, null, ProxyConfValueType.CUSTOM, ProxyConfOverride.CUSTOM, description);
        this.mPortAttrName = portAttrName;
    }

    @Override
    public abstract void update() throws ServiceException;

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

class ReverseProxyIPThrottleWhitelist extends ProxyConfVar {

	public ReverseProxyIPThrottleWhitelist() {
        super("mail.whitelistip.:servers", null, null, ProxyConfValueType.CUSTOM,
                ProxyConfOverride.CUSTOM,
			    "List of Client IP addresses immune to IP Throttling");
    }

    @Override
    public void update() throws ServiceException {
    	ArrayList<String> directives = new ArrayList<String>();
        String[] ips = serverSource.getMultiAttr(Provisioning.A_zimbraReverseProxyIPThrottleWhitelist);
		Boolean first = true;
    	for (String ip : ips) {
			directives.add(ip);
			mLog.debug("Added %s IP Throttle whitelist", ip);
    	}
    	mValue = directives;
    }

	@Override
    public String format(Object o) {
        @SuppressWarnings("unchecked")
        ArrayList<String> servers = (ArrayList<String>) o;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < servers.size(); i++) {
            String s = servers.get(i);
            if (i == 0) {
                sb.append(String.format("mail_whitelist_ip    %s;\n", s));
            } else {
                sb.append(String.format("    mail_whitelist_ip    %s;\n", s));
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

    @Override
    public void update() throws ServiceException {
    	ArrayList<String> directives = new ArrayList<String>();
    	String portName = configSource.getAttr(Provisioning.A_zimbraReverseProxyHttpPortAttribute, "");

    	List<Server> mailclientservers = mProv.getAllMailClientServers();
    	for (Server server : mailclientservers) {
    		String serverName = server.getAttr(
    				Provisioning.A_zimbraServiceHostname, "");

    		if (isValidUpstream(server, serverName)) {
    			directives.add(generateServerDirective(server, serverName, portName));
    			mLog.debug("Added server to HTTP mailstore upstream: " + serverName);
    		}
    	}
    	mValue = directives;
    }
}

class WebUpstreamZxServersVar extends ServersVar {

    public WebUpstreamZxServersVar() {
        super("web.upstream.zx.:servers", null,
              "List of upstream HTTP servers towards zx port used by Web Proxy (i.e. servers " +
                "for which zimbraReverseProxyLookupTarget is true, and whose " +
                "mail mode is http|https|mixed|both)");
    }

    @Override
    public void update() throws ServiceException {
        ArrayList<String> directives = new ArrayList<String>();

        List<Server> mailclientservers = mProv.getAllMailClientServers();
        for (Server server : mailclientservers) {
            String serverName = server.getAttr(
              Provisioning.A_zimbraServiceHostname, "");

            if (isValidUpstream(server, serverName)) {
                directives.add(generateServerDirective(server, serverName, ProxyConfGen.ZIMBRA_UPSTREAM_ZX_PORT));
                mLog.debug("Added server to HTTP zx upstream: " + serverName);
            }
        }
        mValue = directives;
    }
}

class WebSslUpstreamZxServersVar extends ServersVar {

    public WebSslUpstreamZxServersVar() {
        super("web.ssl.upstream.zx.:servers", null,
              "List of upstream HTTPS servers towards zx ssl port used by Web Proxy (i.e. servers " +
                "for which zimbraReverseProxyLookupTarget is true, and whose " +
                "mail mode is http|https|mixed|both)");
    }

    @Override
    public void update() throws ServiceException {
        ArrayList<String> directives = new ArrayList<String>();

        List<Server> mailclientservers = mProv.getAllMailClientServers();
        for (Server server : mailclientservers) {
            String serverName = server.getAttr(
              Provisioning.A_zimbraServiceHostname, "");

            if (isValidUpstream(server, serverName)) {
                directives.add(generateServerDirective(server, serverName, ProxyConfGen.ZIMBRA_UPSTREAM_SSL_ZX_PORT));
                mLog.debug("Added server to HTTPS zx ssl upstream: " + serverName);
            }
        }
        mValue = directives;
    }
}

class WebSSLUpstreamServersVar extends ServersVar {

    public WebSSLUpstreamServersVar() {
    	super("web.ssl.upstream.:servers", Provisioning.A_zimbraReverseProxyHttpSSLPortAttribute,
    			"List of upstream HTTPS servers used by Web Proxy (i.e. servers " +
    			"for which zimbraReverseProxyLookupTarget is true, and whose " +
    			"mail mode is https|mixed|both)");
    }

    @Override
    public void update() throws ServiceException {
    	ArrayList<String> directives = new ArrayList<String>();
    	String portName = configSource.getAttr(Provisioning.A_zimbraReverseProxyHttpSSLPortAttribute, "");

    	List<Server> mailclientservers = mProv.getAllMailClientServers();
    	for (Server server : mailclientservers) {
    		String serverName = server.getAttr(
    				Provisioning.A_zimbraServiceHostname, "");

    		if (isValidUpstream(server, serverName)) {
    			directives.add(generateServerDirective(server, serverName, portName));
    			mLog.debug("Added server to HTTPS mailstore upstream: " + serverName);
    		}
    	}
    	mValue = directives;
    }
}

class WebAdminUpstreamServersVar extends ServersVar {
	public WebAdminUpstreamServersVar() {
		super("web.admin.upstream.:servers", Provisioning.A_zimbraReverseProxyAdminPortAttribute,
				"List of upstream admin console servers used by Web Proxy (i.e. servers " +
				"for which zimbraReverseProxyLookupTarget is true");
	}

	@Override
	public void update() throws ServiceException {
		ArrayList<String> directives = new ArrayList<String>();
		String portName = configSource.getAttr(Provisioning.A_zimbraReverseProxyAdminPortAttribute, "");

		List<Server> mailclientservers = mProv.getAllMailClientServers();
		for (Server server : mailclientservers) {
			String serverName = server.getAttr(
					Provisioning.A_zimbraServiceHostname, "");

			if (isValidUpstream(server, serverName)) {
				directives.add(generateServerDirective(server, serverName, portName));
				mLog.debug("Added server to HTTPS Admin mailstore upstream: " + serverName);
			}
		}
		mValue = directives;
	}
}

class WebEwsUpstreamServersVar extends ServersVar {

	public WebEwsUpstreamServersVar() {
		super("web.upstream.ewsserver.:servers", Provisioning.A_zimbraReverseProxyUpstreamEwsServers,
				"List of upstream EWS servers used by Web Proxy");
	}

    @Override
    public void update() throws ServiceException {
        ArrayList<String> directives = new ArrayList<String>();
        String portName = configSource.getAttr(Provisioning.A_zimbraReverseProxyHttpPortAttribute, "");
        String[] upstreams = serverSource.getMultiAttr(Provisioning.A_zimbraReverseProxyUpstreamEwsServers);

        if (upstreams.length > 0) {
            for (String serverName: upstreams) {
                Server server = mProv.getServerByName(serverName);
                if (isValidUpstream(server, serverName)) {
                    directives.add(generateServerDirective(server, serverName, portName));
                    mLog.debug("Added EWS server to HTTP upstream: " + serverName);
                }
            }
        }
        mValue = directives;
    }
}

class WebEwsSSLUpstreamServersVar extends ServersVar {

    public WebEwsSSLUpstreamServersVar() {
    	super("web.ssl.upstream.ewsserver.:servers",Provisioning.A_zimbraReverseProxyUpstreamEwsServers,
				"List of upstream EWS servers used by Web Proxy");
    }

    @Override
    public void update() throws ServiceException {
        ArrayList<String> directives = new ArrayList<String>();
        String portName = configSource.getAttr(Provisioning.A_zimbraReverseProxyHttpSSLPortAttribute, "");
        String[] upstreams = serverSource.getMultiAttr(Provisioning.A_zimbraReverseProxyUpstreamEwsServers);

        if (upstreams.length > 0) {
            for (String serverName: upstreams) {
                Server server = mProv.getServerByName(serverName);
                if (isValidUpstream(server, serverName)) {
                    directives.add(generateServerDirective(server, serverName, portName));
                    mLog.debug("Added EWS server to HTTPS upstream: " + serverName);
                }
            }
        }
        mValue = directives;
    }
}

class WebLoginUpstreamServersVar extends ServersVar {

	public WebLoginUpstreamServersVar() {
		super("web.upstream.loginserver.:servers", Provisioning.A_zimbraReverseProxyUpstreamLoginServers,
				"List of upstream Login servers used by Web Proxy");
	}

    @Override
    public void update() throws ServiceException {
        ArrayList<String> directives = new ArrayList<String>();
        String portName = configSource.getAttr(Provisioning.A_zimbraReverseProxyHttpPortAttribute, "");
        String[] upstreams = serverSource.getMultiAttr(Provisioning.A_zimbraReverseProxyUpstreamLoginServers);

        if (upstreams.length > 0) {
            for (String serverName: upstreams) {
                Server server = mProv.getServerByName(serverName);
                if (isValidUpstream(server, serverName)) {
                    directives.add(generateServerDirective(server, serverName, portName));
                    mLog.debug("Added Login server to HTTP upstream: " + serverName);
                }
            }
        }
        mValue = directives;
    }
}

class WebLoginSSLUpstreamServersVar extends ServersVar {

    public WebLoginSSLUpstreamServersVar() {
    	super("web.ssl.upstream.loginserver.:servers",Provisioning.A_zimbraReverseProxyUpstreamLoginServers,
				"List of upstream Login servers used by Web Proxy");
    }

    @Override
    public void update() throws ServiceException {
        ArrayList<String> directives = new ArrayList<String>();
        String portName = configSource.getAttr(Provisioning.A_zimbraReverseProxyHttpSSLPortAttribute, "");
        String[] upstreams = serverSource.getMultiAttr(Provisioning.A_zimbraReverseProxyUpstreamLoginServers);

        if (upstreams.length > 0) {
            for (String serverName: upstreams) {
                Server server = mProv.getServerByName(serverName);
                if (isValidUpstream(server, serverName)) {
                    directives.add(generateServerDirective(server, serverName, portName));
                    mLog.debug("Added Login server to HTTPS upstream: " + serverName);
                }
            }
        }
        mValue = directives;
    }
}

class OnlyOfficeDocServiceServersVar extends ProxyConfVar {
    private final String DEFAULT_DOC_HOST = "zimbra.com";

    public OnlyOfficeDocServiceServersVar() {

        super("web.upstream.onlyoffice.docservice", null, null, ProxyConfValueType.STRING, ProxyConfOverride.CUSTOM,
                "List of upstream HTTPS servers towards docservice port used by Web Proxy ");
    }

    @Override
    public void update() throws ServiceException {
        StringBuffer sb = new StringBuffer();
        Set<String> upstreamsCreatedForZimbraId = new HashSet<String>();
        List<Server> mailclientservers = mProv.getAllMailClientServers();
        for (Server server : mailclientservers) {
            String serverName = server.getAttr(Provisioning.A_zimbraServiceHostname, "");
            String zimbraId = server.getAttr(Provisioning.A_zimbraId, "");
            boolean hasOnlyOfficeServer = server.hasOnlyOfficeService();
            String docServerHost = server.getAttr(Provisioning.A_zimbraDocumentServerHost, "");
            mLog.debug(String.format(
                    " Setting Docservice upstream for servername : %s , zimbraId : %s , docServerHost in config : %s ",
                    serverName, zimbraId, docServerHost));
            // if docServerHost is present, get the zimbraId of that host
            if (docServerHost != null && docServerHost.trim().length() > 0
                    && !docServerHost.trim().equals(DEFAULT_DOC_HOST)
                    && !serverName.trim().equals(docServerHost.trim())) {
                mLog.info(String.format(" Setting Docservice upstream, Document Server set to : %s in config",
                        docServerHost));
                Server docServer = mProv.get(Key.ServerBy.name, docServerHost);
                zimbraId = docServer.getId();
                serverName = docServerHost;
                hasOnlyOfficeServer = docServer.hasOnlyOfficeService();
            }

            if (!upstreamsCreatedForZimbraId.contains(zimbraId) && hasOnlyOfficeServer) {
                sb.append(generateUpstreamBlock(zimbraId, serverName));
                sb.append("\n");
                upstreamsCreatedForZimbraId.add(zimbraId);
                mLog.debug("Added docservice upstream for onlyoffice server " + serverName);
            }
        }
        mValue = sb.toString();
    }

    public String generateUpstreamBlock(String zimbraId, String serverName) {
        int timeout = 0;
        int maxFails = 0;
        // form the docservice upstream block
        String encodedServerZimbraId = SharedFileServletContext.fetchEncoded(zimbraId);
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("upstream docservice_%s {\n", encodedServerZimbraId));
        sb.append(String.format(String.format("\tserver\t %s:%d fail_timeout=%ds max_fails=%d;\n", serverName,
                ProxyConfGen.ZIMBRA_UPSTREAM_ONLYOFFICE_DOCSERVICE_PORT, timeout, maxFails)));
        sb.append(String.format("}\n", encodedServerZimbraId));

        return sb.toString();
    }
}

class OnlyOfficeSpellCheckerServersVar extends ProxyConfVar {
    private final String DEFAULT_DOC_HOST = "zimbra.com";

    public OnlyOfficeSpellCheckerServersVar() {

        super("web.upstream.onlyoffice.spellchecker", null, null, ProxyConfValueType.STRING, ProxyConfOverride.CUSTOM,
                "List of upstream HTTPS servers towards spellchecker port used by Web Proxy ");
    }

    @Override
    public void update() throws ServiceException {
        StringBuffer sb = new StringBuffer();
        Set<String> upstreamsCreatedForZimbraId = new HashSet<String>();
        // get the docserver hostname configured
        // if it is not set use the mailbox server name
        List<Server> mailClientServers = mProv.getAllMailClientServers();
        for (Server server : mailClientServers) {
            String serverName = server.getAttr(Provisioning.A_zimbraServiceHostname, "");
            String zimbraId = server.getAttr(Provisioning.A_zimbraId, "");
            boolean hasOnlyOfficeServer = server.hasOnlyOfficeService();
            String docServerHost = server.getAttr(Provisioning.A_zimbraDocumentServerHost, "");
            // if docServerHost is present, get the zimbraId of that host
            if (docServerHost != null && docServerHost.trim().length() > 0
                    && !docServerHost.trim().equals(DEFAULT_DOC_HOST)
                    && !serverName.trim().equals(docServerHost.trim())) {
                Server docServer = mProv.get(Key.ServerBy.name, docServerHost);
                zimbraId = docServer.getId();
                serverName = docServerHost;
                hasOnlyOfficeServer = docServer.hasOnlyOfficeService();
            }
            if (!upstreamsCreatedForZimbraId.contains(zimbraId) && hasOnlyOfficeServer) {
                sb.append(generateUpstreamBlock(zimbraId, serverName));
                sb.append("\n");
                upstreamsCreatedForZimbraId.add(zimbraId);
                mLog.debug("Added spellchecker upstream for onlyoffice server " + serverName);
            }
        }

        mValue = sb.toString();
    }

    public String generateUpstreamBlock(String zimbraId, String serverName) {
        // form the spellchecker upstream block
        String encodedServerZimbraId = SharedFileServletContext.fetchEncoded(zimbraId);
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("upstream spellchecker_%s {\n", encodedServerZimbraId));
        sb.append(String.format(String.format("\tserver\t %s:%d;\n", serverName,
                ProxyConfGen.ZIMBRA_UPSTREAM_ONLYOFFICE_SPELLCHECKER_PORT)));
        sb.append(String.format("}\n", encodedServerZimbraId));

        return sb.toString();
    }
}

class AddHeadersVar extends ProxyConfVar {
    private final ArrayList<String> rhdr;
    private final String key;
    private KeyValue[] headers;
    private int i;

    public AddHeadersVar(String key, ArrayList<String> rhdr, String description) {
        super(key, null, null, ProxyConfValueType.CUSTOM, ProxyConfOverride.CUSTOM, description);
        this.rhdr = rhdr;
        this.key = key;
    }

    @Override
    public void update() throws ServiceException {
        ArrayList<KeyValue> directives = new ArrayList<KeyValue>();
        headers = new KeyValue[rhdr.size()];
        i = 0;

        for (String hdr: rhdr) {
            Matcher matcher = RE_HEADER.matcher(hdr);
            if (matcher.matches()) {
                headers[i] = new KeyValue(matcher.group(1), matcher.group(2));
            } else {
                headers[i] = new KeyValue(hdr);
            }
            directives.add(headers[i]);
            i++;
        }
        mValue = directives;
    }

    @Override
    public String format(Object o) {
        @SuppressWarnings("unchecked")
        ArrayList<KeyValue> rsphdr = (ArrayList<KeyValue>) o;
        StringBuilder sb = new StringBuilder();
        for (i = 0; i < rsphdr.size(); i++) {
            KeyValue header = rsphdr.get(i);
            mLog.debug("Adding directive add_header " + header.key + " " + header.value);
            if (i == 0) {
                sb.append(String.format("add_header %s %s;", header.key, header.value));
            } else {
                sb.append(String.format("\n    add_header %s %s;", header.key, header.value));
            }
        }
        return sb.toString();
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
            serverSource.getMultiAttr(Provisioning.A_zimbraReverseProxyImapEnabledCapability);
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
                .getMultiAttr(Provisioning.A_zimbraReverseProxyPop3EnabledCapability);
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

/**
 * ListenAddressesVar
 * This class is intended to produce strings that are embedded inside the 'nginx.conf.web.https.default' template
 * It is placed inside the strict server_name enforcing server block.
 */
class ListenAddressesVar extends ProxyConfVar {

    public ListenAddressesVar(Set<String> addresses) {
        super("listen.:addresses",
                null, // this is a fake attribute
                addresses,
                ProxyConfValueType.CUSTOM,
                ProxyConfOverride.CUSTOM,
                "List of ip addresses nginx needs to listen to catch all unknown server names");
    }

    @Override
    public String format(Object o) throws ProxyConfException {
       Set<String> addresses = (Set<String>)o;
        if (addresses.size() == 0) {
            return "${web.strict.servername}";
        }
        StringBuilder sb = new StringBuilder();
        for (String addr: addresses) {
            sb.append(String.format("${web.strict.servername}    listen                  %s:${web.https.port} default_server;\n", addr));
        }
        sb.setLength(sb.length() - 1); //trim the last newline
        return sb.toString();
    }
}

class ZMLookupHandlerVar extends ProxyConfVar{
    public ZMLookupHandlerVar() {
        super("zmlookup.:handlers",
              Provisioning.A_zimbraReverseProxyLookupTarget,
              new ArrayList<String>(),
              ProxyConfValueType.CUSTOM,
              ProxyConfOverride.CUSTOM,
              "List of nginx lookup handlers (i.e. servers for which" +
              " zimbraReverseProxyLookupTarget is true)");
    }

    @Override
    public void update() throws ServiceException, ProxyConfException {
        ArrayList<String> servers = new ArrayList<String>();
        int numFailedHandlers = 0;

        String[] handlerNames = serverSource.getMultiAttr(Provisioning.A_zimbraReverseProxyAvailableLookupTargets);
        if (handlerNames.length > 0) {
            for (String handlerName: handlerNames) {
                Server s = mProv.getServerByName(handlerName);
                if (s != null) {
                    String sn = s.getAttr(Provisioning.A_zimbraServiceHostname, "");
                    int port = s.getIntAttr(Provisioning.A_zimbraExtensionBindPort, 7072);
                    String proto = "http://";
                    int major = s.getIntAttr(Provisioning.A_zimbraServerVersionMajor, 0);
                    int minor = s.getIntAttr(Provisioning.A_zimbraServerVersionMinor, 0);
                    if ((major == 8 && minor >= 7) || (major > 8)) {
                        proto = "https://";
                    }
                    boolean isTarget = s.getBooleanAttr(Provisioning.A_zimbraReverseProxyLookupTarget, false);
                    if (isTarget) {
                        try {
                            InetAddress ip = ProxyConfUtil.getLookupTargetIPbyIPMode(sn);
                            Formatter f = new Formatter();
                            if (ip instanceof Inet4Address) {
                                f.format("%s%s:%d", proto, ip.getHostAddress(), port);
                            } else {
                                f.format("%s[%s]:%d", proto, ip.getHostAddress(), port);
                            }
                            servers.add(f.toString());
                            f.close();
                            mLog.debug("Route Lookup: Added server " + ip);
                        }
                        catch (ProxyConfException pce) {
                            numFailedHandlers++;
                            mLog.error("Error resolving service host name: '" + sn + "'", pce);
                        }
                    }
                }
                else {
                    mLog.warn("Invalid value found in 'zimbraReverseProxyAvailableLookupTargets': " +
                              handlerName +
                              "\nPlease correct and run zmproxyconfgen again");
                }
            }
        } else {
            List<Server> allServers = mProv.getAllServers();

            for (Server s : allServers)
            {
                String sn = s.getAttr(Provisioning.A_zimbraServiceHostname, "");
                int port = s.getIntAttr(Provisioning.A_zimbraExtensionBindPort, 7072);
                String proto = "http://";
                int major = s.getIntAttr(Provisioning.A_zimbraServerVersionMajor, 0);
                int minor = s.getIntAttr(Provisioning.A_zimbraServerVersionMinor, 0);
                if ((major == 8 && minor >= 7) || (major > 8)) {
                    proto = "https://";
                }
                boolean isTarget = s.getBooleanAttr(Provisioning.A_zimbraReverseProxyLookupTarget, false);
                if (isTarget) {
                    try {
                        InetAddress ip = ProxyConfUtil.getLookupTargetIPbyIPMode(sn);
                        Formatter f = new Formatter();
                        if (ip instanceof Inet4Address) {
                            f.format("%s%s:%d", proto, ip.getHostAddress(), port);
                        } else {
                            f.format("%s[%s]:%d", proto, ip.getHostAddress(), port);
                        }
                        servers.add(f.toString());
                        f.close();
                        mLog.debug("Route Lookup: Added server " + ip);
                    }
                    catch (ProxyConfException pce) {
                        numFailedHandlers++;
                        mLog.error("Error resolving service host name: '" + sn + "'", pce);
                    }
                }
            }
        }
        if (servers.isEmpty()) {
            if (numFailedHandlers > 0) {
                throw new ProxyConfException ("No available nginx lookup handlers could be contacted");
            }
            else {
                mLog.warn("No available nginx lookup handlers could be found");
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

class ZMLookupAvailableVar extends ProxyConfVar {

    public ZMLookupAvailableVar() {
        super("lookup.available", null, false,
                ProxyConfValueType.ENABLER, ProxyConfOverride.CUSTOM,
                "Indicates whether there are available lookup handlers or not");
    }

    @Override
    public void update() throws ServiceException, ProxyConfException {
        ZMLookupHandlerVar lhVar = new ZMLookupHandlerVar();
        lhVar.update();
        @SuppressWarnings("unchecked")
        ArrayList<String> servers = (ArrayList<String>) lhVar.mValue;
        if (servers.isEmpty()) {
            mValue = false;
        }
        else {
            mValue = true;
        }
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
              Provisioning.A_zimbraReverseProxyClientCertCA,
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
              Provisioning.A_zimbraWebClientLoginURL,
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
              Provisioning.A_zimbraReverseProxyClientCertMode,
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
              Provisioning.A_zimbraReverseProxyClientCertMode,
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
              Provisioning.A_zimbraReverseProxyErrorHandlerURL,
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
    private final int offset;

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
    protected ProxyConfVar mVar;

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

    @Override
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
        super("web.upstream.schema", Provisioning.A_zimbraReverseProxySSLToUpstreamEnabled, true, ProxyConfValueType.BOOLEAN,
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
 * Provide the value of "proxy_pass" for web proxy.
 * @author jiankuan
 *
 */
class WebProxyUpstreamClientTargetVar extends ProxyConfVar {
    public WebProxyUpstreamClientTargetVar() {
        super("web.upstream.schema", Provisioning.A_zimbraReverseProxySSLToUpstreamEnabled, true, ProxyConfValueType.BOOLEAN,
                ProxyConfOverride.SERVER, "The target of proxy_pass for web client proxy");
    }

    @Override
    public String format(Object o) throws ProxyConfException {
        Boolean value = (Boolean)o;
        if(value == false) {
            return "http://" + ProxyConfGen.ZIMBRA_UPSTREAM_WEBCLIENT_NAME;
        } else {
            return "https://" + ProxyConfGen.ZIMBRA_SSL_UPSTREAM_WEBCLIENT_NAME;
        }
    }
}

class WebProxyUpstreamLoginTargetVar extends ProxyConfVar {
    public WebProxyUpstreamLoginTargetVar() {
        super("web.upstream.schema", Provisioning.A_zimbraReverseProxySSLToUpstreamEnabled, true, ProxyConfValueType.BOOLEAN,
                ProxyConfOverride.SERVER, "The login target of proxy_pass for web proxy");
    }

    @Override
    public String format(Object o) throws ProxyConfException {
        Boolean value = (Boolean)o;
        if(value == false) {
            return "http://" + ProxyConfGen.ZIMBRA_UPSTREAM_LOGIN_NAME;
        } else {
            return "https://" + ProxyConfGen.ZIMBRA_SSL_UPSTREAM_LOGIN_NAME;
        }
    }
}

class WebProxyUpstreamEwsTargetVar extends ProxyConfVar {
    public WebProxyUpstreamEwsTargetVar() {
        super("web.upstream.schema", Provisioning.A_zimbraReverseProxySSLToUpstreamEnabled, true, ProxyConfValueType.BOOLEAN,
                ProxyConfOverride.SERVER, "The ews target of proxy_pass for web proxy");
    }

    @Override
    public String format(Object o) throws ProxyConfException {
        Boolean value = (Boolean)o;
        if(value == false) {
            return "http://" + ProxyConfGen.ZIMBRA_UPSTREAM_EWS_NAME;
        } else {
            return "https://" + ProxyConfGen.ZIMBRA_SSL_UPSTREAM_EWS_NAME;
        }
    }
}

class XmppBoshProxyUpstreamProtoVar extends ProxyConfVar {
    public XmppBoshProxyUpstreamProtoVar() {
        super("xmpp.upstream.schema", Provisioning.A_zimbraReverseProxyXmppBoshSSL, true, ProxyConfValueType.BOOLEAN,
                ProxyConfOverride.SERVER, "The XMPP target of proxy_pass for web proxy");
    }

    @Override
    public String format(Object o) throws ProxyConfException {
        if((Boolean)o == false) {
            return "http";
        } else {
            return "https";
        }
    }
}

class WebProxyUpstreamZxTargetVar extends ProxyConfVar {
    public WebProxyUpstreamZxTargetVar() {
        super("web.upstream.schema", Provisioning.A_zimbraReverseProxySSLToUpstreamEnabled, true, ProxyConfValueType.BOOLEAN,
              ProxyConfOverride.SERVER, "The target for zx paths");
    }

    @Override
    public String format(Object o) throws ProxyConfException {
        Boolean value = (Boolean)o;
        if(value == false) {
            return "http://" + ProxyConfGen.ZIMBRA_UPSTREAM_ZX_NAME;
        } else {
            return "https://" + ProxyConfGen.ZIMBRA_SSL_UPSTREAM_ZX_NAME;
        }
    }
}

class WebSSLSessionCacheSizeVar extends ProxyConfVar {

    public WebSSLSessionCacheSizeVar() {
        super("ssl.session.cachesize", Provisioning.A_zimbraReverseProxySSLSessionCacheSize, "10m",
                ProxyConfValueType.STRING, ProxyConfOverride.SERVER,
                "SSL session cache size for the proxy");
    }

    @Override
    public String format(Object o) {
        @SuppressWarnings("unchecked")
        String sslSessionCacheSize = (String)o;
        StringBuilder sslsessioncache = new StringBuilder();
        sslsessioncache.append("shared:SSL:");
        sslsessioncache.append(sslSessionCacheSize);

        return sslsessioncache.toString();
    }
}

/**
 *
 * @author zimbra
 *
 */
class EwsEnablerVar extends WebEnablerVar {

    public EwsEnablerVar() {
        super("web.ews.upstream.disable", "#",
                "Indicates whether EWS upstream servers blob in nginx.conf.web should be populated " +
                "(false unless zimbraReverseProxyUpstreamEwsServers is populated)");
    }


    @Override
    public String format(Object o)  {
    	String[] upstreams = serverSource.getMultiAttr(Provisioning.A_zimbraReverseProxyUpstreamEwsServers);
        if (upstreams.length  == 0) {
            return "#";
        } else {
            return "";
        }
    }
}

/**
 *
 * @author zimbra
 *
 */
class LoginEnablerVar extends WebEnablerVar {

    public LoginEnablerVar() {
        super("web.login.upstream.disable", "#",
                "Indicates whether upstream Login servers blob in nginx.conf.web should be populated " +
                "(false unless zimbraReverseProxyUpstreamLoginServers is populated)");
    }


    @Override
    public String format(Object o)  {
    	String[] upstreams = serverSource.getMultiAttr(Provisioning.A_zimbraReverseProxyUpstreamLoginServers);
        if (upstreams.length  == 0) {
            return "#";
        } else {
            return "";
        }
    }
}

class WebXmppBoshEnablerVar extends ProxyConfVar {

    public WebXmppBoshEnablerVar() {
        super("web.xmpp.bosh.upstream.disable",
              Provisioning.A_zimbraReverseProxyXmppBoshEnabled,
              false,
              ProxyConfValueType.ENABLER,
              ProxyConfOverride.CUSTOM,
              "whether to populate the location block for XMPP over BOSH requests to /http-bind path");
    }

    @Override
    public void update() throws ServiceException {
        String xmppEnabled = serverSource.getAttr(Provisioning.A_zimbraReverseProxyXmppBoshEnabled, true);
        String XmppBoshLocalBindURL = serverSource.getAttr(Provisioning.A_zimbraReverseProxyXmppBoshLocalHttpBindURL, true);
        String XmppBoshHostname = serverSource.getAttr(Provisioning.A_zimbraReverseProxyXmppBoshHostname, true);
        int XmppBoshPort = serverSource.getIntAttr(Provisioning.A_zimbraReverseProxyXmppBoshPort, 0);

        if (XmppBoshLocalBindURL == null || ProxyConfUtil.isEmptyString(XmppBoshLocalBindURL) ||
            XmppBoshHostname == null || ProxyConfUtil.isEmptyString(XmppBoshHostname) ||
            XmppBoshPort == 0) {
            mLog.debug("web.xmpp.bosh.upstream.disable is false because one of the required attrs is unset");
            mValue = false;
        } else {
            if (xmppEnabled.equals("TRUE")) {
                mValue = true;
            } else {
                mValue = false;
            }
        }
    }
}

/**
 * Simplified container object for a server
 * @author Davide Baldo
 */
class ServerAttrItem {
    public String zimbraId;
    public String hostname;
    public String[] services;

    public ServerAttrItem(String zimbraId, String hostname, String[] services) {
        this.zimbraId = zimbraId;
        this.hostname = hostname;
        this.services = services;
    }

    public boolean hasService(String service)
    {
        for( String current : services ) {
            if( service.equals(current) ) return true;
        }
        return false;
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
    public String[] rspHeaders;

    public DomainAttrItem(String dn, String vhn, String vip, String scrt, String spk,
            String ccm, String cca, String[] rhdr) {
        this.domainName = dn;
        this.virtualHostname = vhn;
        this.virtualIPAddress = vip;
        this.sslCertificate = scrt;
        this.sslPrivateKey = spk;
        this.clientCertMode = ccm;
        this.clientCertCa = cca;
        this.rspHeaders = rhdr;
    }
}

/** The visit of LdapProvisioning can't throw the exception out.
 *  Therefore uses this special item to indicate exception.
 * @author jiankuan
 *
 */
class DomainAttrExceptionItem extends DomainAttrItem {
    public DomainAttrExceptionItem(ProxyConfException e) {
        super(null, null, null, null, null, null, null, null);
        this.exception = e;
    }

    public ProxyConfException exception;
}

/**
 * Provide the value of "ssl_protocols" for web proxy.
 */
class WebSSLProtocolsVar extends ProxyConfVar {

    public WebSSLProtocolsVar() {
        super("web.ssl.protocols", null, getEnabledSSLProtocols(),
                ProxyConfValueType.CUSTOM, ProxyConfOverride.CUSTOM,
                "SSL Protocols enabled for the web proxy");
    }

    static ArrayList<String> getEnabledSSLProtocols () {
        ArrayList<String> sslProtocols = new ArrayList<String> ();
        sslProtocols.add("TLSv1");
        sslProtocols.add("TLSv1.1");
        sslProtocols.add("TLSv1.2");
        return sslProtocols;
    }

    @Override
    public void update() {

        ArrayList<String> sslProtocols = new ArrayList<String>();
        String[] sslProtocolsEnabled =
            serverSource.getMultiAttr(Provisioning.A_zimbraReverseProxySSLProtocols);
        for (String c:sslProtocolsEnabled)
        {
            sslProtocols.add(c);
        }
        if (sslProtocols.size() > 0) {
            mValue = sslProtocols;
        } else {
            mValue = mDefault;
        }
    }

    @Override
    public String format(Object o) {

        @SuppressWarnings("unchecked")
        ArrayList<String> sslProtocols = (ArrayList<String>) o;
        StringBuilder sslproto = new StringBuilder();
        for (String c : sslProtocols) {
            sslproto.append(" ");
            sslproto.append(c);
        }
        return sslproto.toString();
    }
}

/**
 * Provide the value of "ssl_protocols" for mail proxy.
 */
class MailSSLProtocolsVar extends ProxyConfVar {

    public MailSSLProtocolsVar() {
        super("web.ssl.protocols", null, getEnabledSSLProtocols(),
                ProxyConfValueType.CUSTOM, ProxyConfOverride.CUSTOM,
                "SSL Protocols enabled for the mail proxy");
    }

    static ArrayList<String> getEnabledSSLProtocols () {
        ArrayList<String> sslProtocols = new ArrayList<String> ();
        sslProtocols.add("TLSv1");
        sslProtocols.add("TLSv1.1");
        sslProtocols.add("TLSv1.2");
        return sslProtocols;
    }

    @Override
    public void update() {

        ArrayList<String> sslProtocols = new ArrayList<String>();
        String[] sslProtocolsEnabled =
            serverSource.getMultiAttr(Provisioning.A_zimbraReverseProxySSLProtocols);
        for (String c:sslProtocolsEnabled)
        {
            sslProtocols.add(c);
        }
        if (sslProtocols.size() > 0) {
            mValue = sslProtocols;
        } else {
            mValue = mDefault;
        }
    }

    @Override
    public String format(Object o) {

        @SuppressWarnings("unchecked")
        ArrayList<String> sslProtocols = (ArrayList<String>) o;
        StringBuilder sslproto = new StringBuilder();
        for (String c : sslProtocols) {
            sslproto.append(" ");
            sslproto.append(c);
        }
        return sslproto.toString();
    }
}

class WebSSLDhparamEnablerVar extends WebEnablerVar {

    private ProxyConfVar webSslDhParamFile;

    public WebSSLDhparamEnablerVar(ProxyConfVar webSslDhParamFile) {
        super("web.ssl.dhparam.enabled", false,
                "Indicates whether ssl_dhparam directive should be added or not");
	this.webSslDhParamFile = webSslDhParamFile;
    }

    @Override
    public void update() {
	String dhparam = (String)webSslDhParamFile.rawValue();
        if (dhparam == null || ProxyConfUtil.isEmptyString(dhparam) || !(new File(dhparam).exists())) {
            mValue = false;
        } else {
            mValue = true;
        }
    }
}

class WebStrictServerName extends WebEnablerVar {

    public WebStrictServerName() {
        super("web.strict.servername", "#",
                "Indicates whether the default server block is generated returning a default HTTP response to all unknown hostnames");
    }

    @Override
    public String format(Object o)  {
        if (isStrictEnforcementEnabled()) {
            return "";
        } else {
            return "#";
        }
    }

    public boolean isStrictEnforcementEnabled() {
        boolean enforcementEnabled = serverSource.getBooleanAttr(Provisioning.A_zimbraReverseProxyStrictServerNameEnabled, false);
        mLog.info(String.format("Strict server name enforcement enabled? %s", enforcementEnabled));
        return enforcementEnabled;
    }
}

public class ProxyConfGen
{
    private static final int DEFAULT_SERVERS_NAME_HASH_MAX_SIZE = 512;
    private static final int DEFAULT_SERVERS_NAME_HASH_BUCKET_SIZE = 64;
    private static Log mLog = LogFactory.getLog (ProxyConfGen.class);
    private static Options mOptions = new Options();
    private static boolean mDryRun = false;
    private static boolean mEnforceDNSResolution = false;
    private static String mWorkingDir = "/opt/zimbra";
    private static String mTemplateDir = mWorkingDir + "/conf/nginx/templates";
    private static String mConfDir = mWorkingDir + "/conf";
    private static String mResolverfile = mConfDir + "/nginx/resolvers.conf";
    private static String mIncDir = "nginx/includes";
    private static String mDomainSSLDir = mConfDir + File.separator + "domaincerts";
    private static String mSSLCrtExt = ".crt";
    private static String mSSLKeyExt = ".key";
    private static String mSSLClientCertCaExt = ".client.ca.crt";
    private static String mDefaultSSLCrt = mConfDir + File.separator + "nginx.crt";
    private static String mDefaultSSLKey = mConfDir + File.separator + "nginx.key";
    private static String mDefaultSSLClientCertCa = mConfDir + File.separator + "nginx.client.ca.crt";
    private static String mDefaultDhParamFile = mConfDir + File.separator + "dhparam.pem";
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
    private static Map<String, ProxyConfVar> mDomainConfVars = new HashMap<String, ProxyConfVar>();
    static List<DomainAttrItem> mDomainReverseProxyAttrs;
    static List<ServerAttrItem> mServerAttrs;
    static Set<String> mListenAddresses = new HashSet<String>();

    static final String ZIMBRA_UPSTREAM_NAME = "zimbra";
    static final String ZIMBRA_UPSTREAM_WEBCLIENT_NAME = "zimbra_webclient";
    static final String ZIMBRA_SSL_UPSTREAM_NAME = "zimbra_ssl";
    static final String ZIMBRA_SSL_UPSTREAM_WEBCLIENT_NAME = "zimbra_ssl_webclient";
    static final String ZIMBRA_ADMIN_CONSOLE_UPSTREAM_NAME = "zimbra_admin";
    static final String ZIMBRA_ADMIN_CONSOLE_CLIENT_UPSTREAM_NAME = "zimbra_adminclient";
    static final String ZIMBRA_UPSTREAM_EWS_NAME = "zimbra_ews";
    static final String ZIMBRA_SSL_UPSTREAM_EWS_NAME = "zimbra_ews_ssl";
    static final String ZIMBRA_UPSTREAM_LOGIN_NAME = "zimbra_login";
    static final String ZIMBRA_SSL_UPSTREAM_LOGIN_NAME = "zimbra_login_ssl";
    static final String ZIMBRA_UPSTREAM_ZX_NAME = "zx";
    static final String ZIMBRA_SSL_UPSTREAM_ZX_NAME = "zx_ssl";
    static final int    ZIMBRA_UPSTREAM_ZX_PORT = 8742;
    static final int    ZIMBRA_UPSTREAM_SSL_ZX_PORT = 8743;
    static final int    ZIMBRA_UPSTREAM_ONLYOFFICE_DOCSERVICE_PORT = 7084;
    static final int    ZIMBRA_UPSTREAM_ONLYOFFICE_SPELLCHECKER_PORT = 7085;

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
        mOptions.addOption("D", "definitions", false, "Print variable map Definitions after loading LDAP configuration (and processing overrides). -D requires -s upstream server. If \"-s upstream server\" is not specified, it just dumps the default varaible map");
        mOptions.addOption("p", "prefix", true, "Config File prefix (defaults to nginx.conf)");
        mOptions.addOption("P", "template-prefix", true, "Template File prefix (defaults to $prefix)");
        mOptions.addOption("i", "include-dir", true, "Directory Path (relative to $workdir/conf), where included configuration files will be written. Defaults to nginx/includes");
        mOptions.addOption("s", "server", true, "If provided, this should be the name of a valid server object. Configuration will be generated based on server attributes. Otherwise, if not provided, Configuration will be generated based on Global configuration values");
        mOptions.addOption("f", "force-dns-resolution", false, "Force configuration generation to stop if DNS resolution failure of any hostnames is detected. Defaults to 'false'");

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
            cl = parser.parse(mOptions, args, false);
        } catch (ParseException pe) {
            usage(pe.getMessage());
            return cl;
        }

        return cl;
    }

    /**
     * Retrieve all server and store only few attrs
     *
     * @return a list of <code>ServerAttrItem</code>
     * @throws ServiceException
     *              this method can work only when LDAP is available
     * @author Davide Baldo
     */

    private static List<ServerAttrItem> loadServerAttrs() throws ServiceException {
        if (!(mProv instanceof LdapProv))
            throw ServiceException.INVALID_REQUEST(
                    "The method can work only when LDAP is available", null);

        final List<ServerAttrItem> serverAttrItems = new ArrayList<ServerAttrItem>();
        for (Server server : mProv.getAllServers()) {
            String zimbraId = server
                    .getAttr(Provisioning.A_zimbraId);
            String serviceHostname = server
                    .getAttr(Provisioning.A_zimbraServiceHostname);
            String[] services = server
                    .getMultiAttr(Provisioning.A_zimbraServiceEnabled);

            serverAttrItems.add(new ServerAttrItem(zimbraId, serviceHostname, services));
        }

        return serverAttrItems;
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
        attrsNeeded.add(Provisioning.A_zimbraReverseProxyResponseHeaders);

        final List<DomainAttrItem> result = new ArrayList<DomainAttrItem>();

        // visit domains
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            @Override
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
                String[] rspHeaders = entry
                    .getMultiAttr(Provisioning.A_zimbraReverseProxyResponseHeaders);

                // no need to check whether clientCertMode or clientCertCA == null,

                if (virtualHostnames.length == 0 || ( certificate == null &&
                                privateKey == null && clientCertMode == null && clientCertCA == null ) ) {

                    return; // ignore the items that don't have virtual host
                            // name, cert or key. Those domains will use the
                            // config
                }
                boolean lookupVIP = true; // lookup virutal IP from DNS or /etc/hosts
                if (virtualIPAddresses.length > 0) {
                    for (String ipAddress: virtualIPAddresses) {
                        mListenAddresses.add(ipAddress);
                    }

                    lookupVIP = false;
                }

                int i = 0;

                for( ; i < virtualHostnames.length; i++) {
                    //bug 66892, only lookup IP when zimbraVirtualIPAddress is unset
                    String vip = null;
                    if (lookupVIP) {
                        vip = null;
                    } else {
                        if (virtualIPAddresses.length == virtualHostnames.length) {
                            vip = virtualIPAddresses[i];
                        } else {
                            vip = virtualIPAddresses[0];
                        }
                    }

                    if (!ProxyConfUtil.isEmptyString(clientCertCA)){
                        createDomainSSLDirIfNotExists();
                    }
                    result.add(new DomainAttrItem(domainName,
                            virtualHostnames[i], vip, certificate, privateKey,
                            clientCertMode, clientCertCA, rspHeaders));
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

            if (mDryRun) {
                mLog.info("Would expand template:" + tf + " to file:" + wf);
                return;
            }

            mLog.info("Expanding template:" + tf + " to file:" + wf);

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

                    if( cmd_arg[1].startsWith("server(") && cmd_arg[1].endsWith(")")) {
                        String serviceName = cmd_arg[1].substring("server(".length(), cmd_arg[1].length() - 1);
                        if( serviceName.isEmpty() ) {
                            throw new ProxyConfException("Missing service parameter in custom header command: " + cmdMatcher.group(2));
                        }
                        expandTemplateByExplodeServer(r, w, serviceName);
                    }
                    else
                    {
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
    @Deprecated
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

    /**
     * Iterate all server of type `serviceName` and populate server_id and server_hostname when exploding
     * with !{explode server(docs)}
     * initially created for zimbra-docs.
     *
     * @param temp Reader of the file which will be exploded per server
     * @param conf Target buffer where generated configuration will be written
     * @param serviceName Filter only servers which contains `serviceName`
     * @throws IOException
     * @author Davide Baldo
     */
    private static void expandTemplateByExplodeServer(
            BufferedReader temp, BufferedWriter conf, String serviceName ) throws IOException {
        List<ServerAttrItem> filteredServers = new ArrayList<>();
        for( ServerAttrItem serverAttrItem : mServerAttrs ) {
            if (serverAttrItem.hasService(serviceName)) {
                filteredServers.add(serverAttrItem);
            }
        }
        if( !filteredServers.isEmpty() ) {
            ArrayList<String> cache = new ArrayList<String>(50);
            String line;
            while ((line = temp.readLine()) != null) {
                if (!line.startsWith("#"))
                    cache.add(line); // cache only non-comment lines
            }

            for (ServerAttrItem server : filteredServers) {
                mVars.put("server_id", server.zimbraId);
                mVars.put("server_hostname", server.hostname);
                expandTempateFromCache(cache, conf);
                mVars.remove("server_id");
                mVars.remove("server_hostname");
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
        int i = 0;

        //resolve the virtual host name
        InetAddress vip = null;
        try {
            if (item.virtualIPAddress == null) {
                vip = InetAddress.getByName(item.virtualHostname);
            } else {
                vip = InetAddress.getByName(item.virtualIPAddress);
            }
        } catch (UnknownHostException e) {
            if (mEnforceDNSResolution) {
                throw new ProxyConfException("virtual host name \"" + item.virtualHostname + "\" is not resolvable", e);
            } else {
                mLog.warn("virtual host name \"" + item.virtualHostname + "\" is not resolvable");
            }
        }

        if (IPModeEnablerVar.getZimbraIPMode() != IPModeEnablerVar.IPMode.BOTH) {
            if (IPModeEnablerVar.getZimbraIPMode() == IPModeEnablerVar.IPMode.IPV4_ONLY &&
                    vip instanceof Inet6Address) {
                String msg = vip.getHostAddress() +
                        " is an IPv6 address but zimbraIPMode is 'ipv4'";
                mLog.error(msg);
                throw new ProxyConfException(msg);
            }

            if (IPModeEnablerVar.getZimbraIPMode() == IPModeEnablerVar.IPMode.IPV6_ONLY &&
                    vip instanceof Inet4Address) {
                String msg = vip.getHostAddress() +
                        " is an IPv4 address but zimbraIPMode is 'ipv6'";
                mLog.error(msg);
                throw new ProxyConfException(msg);
            }
        }

        boolean sni = ProxyConfVar.serverSource.getBooleanAttr(Provisioning.A_zimbraReverseProxySNIEnabled, false);
        if (vip instanceof Inet6Address) {
            //ipv6 address has to be enclosed with [ ]
            if (sni || vip == null) {
                mVars.put("vip", "[::]:");
            } else {
                mVars.put("vip", "[" + vip.getHostAddress() + "]:");
            }
        } else {
            if (sni || vip == null) {
                mVars.put("vip", "");
            } else {
                mVars.put("vip", vip.getHostAddress() + ":");
            }
        }

        //Get the response headers list for this domain
        ArrayList<String> rhdr = new ArrayList<String>();
        for(i = 0; i < item.rspHeaders.length; i++) {
            rhdr.add(item.rspHeaders[i]);
        }
        mDomainConfVars.put("web.add.headers.vhost", new AddHeadersVar("web.add.headers.vhost", rhdr,
                "add_header directive for vhost web proxy"));

        mLog.debug("Updating Default Domain Variable Map");
        try {
            updateDefaultDomainVars();
        } catch (ProxyConfException pe) {
            handleException(pe);
        } catch (ServiceException se) {
            handleException(se);
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

    /* Print the default variable map */
    public static void displayDefaultVariables () throws ProxyConfException
    {
        for (ProxyConfVar var : mConfVars.values()) {
            if (var instanceof TimeInSecVarWrapper) {
                var = ((TimeInSecVarWrapper) var).mVar;
            }
            var.write(System.out);
        }
    }

    /* Print the variable map */
    public static void displayVariables () throws ProxyConfException
    {
        SortedSet <String> sk = new TreeSet <String> (mVars.keySet());

        for (String k : sk) {
            ProxyConfVar var = mConfVars.get(k);
            if (var instanceof TimeInSecVarWrapper)
                var = ((TimeInSecVarWrapper) var).mVar;
            var.write(System.out);
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
        mConfVars.put("ssl.clientcertmode.default", new ProxyConfVar("ssl.clientcertmode.default", Provisioning.A_zimbraReverseProxyClientCertMode, "off", ProxyConfValueType.STRING, ProxyConfOverride.SERVER,"enable authentication via X.509 Client Certificate in nginx proxy (https only)"));
        mConfVars.put("ssl.clientcertca.default", new ClientCertAuthDefaultCAVar());
        mConfVars.put("ssl.clientcertdepth.default", new ProxyConfVar("ssl.clientcertdepth.default", "zimbraReverseProxyClientCertDepth", new Integer(10), ProxyConfValueType.INTEGER, ProxyConfOverride.NONE,"indicate how depth the verification will load the ca chain. This is useful when client crt is signed by multiple intermediate ca"));
        mConfVars.put("main.user", new ProxyConfVar("main.user", null, ZIMBRA_UPSTREAM_NAME, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "The user as which the worker processes will run"));
        mConfVars.put("main.group", new ProxyConfVar("main.group", null, ZIMBRA_UPSTREAM_NAME, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "The group as which the worker processes will run"));
        mConfVars.put("main.workers", new ProxyConfVar("main.workers", Provisioning.A_zimbraReverseProxyWorkerProcesses, new Integer(4), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER, "Number of worker processes"));
        mConfVars.put("main.pidfile", new ProxyConfVar("main.pidfile", null, mWorkingDir + "/log/nginx.pid", ProxyConfValueType.STRING, ProxyConfOverride.NONE, "PID file path (relative to ${core.workdir})"));
        mConfVars.put("main.logfile", new ProxyConfVar("main.logfile", null, mWorkingDir + "/log/nginx.log", ProxyConfValueType.STRING, ProxyConfOverride.NONE, "Log file path (relative to ${core.workdir})"));
        mConfVars.put("main.loglevel", new ProxyConfVar("main.loglevel", Provisioning.A_zimbraReverseProxyLogLevel, "info", ProxyConfValueType.STRING, ProxyConfOverride.SERVER, "Log level - can be debug|info|notice|warn|error|crit"));
        mConfVars.put("main.connections", new ProxyConfVar("main.connections", Provisioning.A_zimbraReverseProxyWorkerConnections, new Integer(10240), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER, "Maximum number of simultaneous connections per worker process"));
        mConfVars.put("main.krb5keytab", new ProxyConfVar("main.krb5keytab", "krb5_keytab", "/opt/zimbra/conf/krb5.keytab", ProxyConfValueType.STRING, ProxyConfOverride.LOCALCONFIG, "Path to kerberos keytab file used for GSSAPI authentication"));
        mConfVars.put("memcache.:servers", new MemcacheServersVar());
        mConfVars.put("memcache.timeout", new ProxyConfVar("memcache.timeout", Provisioning.A_zimbraReverseProxyCacheFetchTimeout, new Long(3000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG, "Time (ms) given to a cache-fetch operation to complete"));
        mConfVars.put("memcache.reconnect", new ProxyConfVar("memcache.reconnect", Provisioning.A_zimbraReverseProxyCacheReconnectInterval, new Long(60000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG, "Time (ms) after which NGINX will attempt to re-establish a broken connection to a memcache server"));
        mConfVars.put("memcache.ttl", new ProxyConfVar("memcache.ttl", Provisioning.A_zimbraReverseProxyCacheEntryTTL, new Long(3600000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG, "Time interval (ms) for which cached entries remain in memcache"));
        mConfVars.put("mail.ctimeout", new ProxyConfVar("mail.ctimeout", Provisioning.A_zimbraReverseProxyConnectTimeout, new Long(120000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "Time interval (ms) after which a POP/IMAP proxy connection to a remote host will give up"));
        mConfVars.put("mail.pop3.timeout", new ProxyConfVar("mail.pop3.timeout", "pop3_max_idle_time", 60, ProxyConfValueType.INTEGER, ProxyConfOverride.LOCALCONFIG, "pop3 network timeout before authentication"));
        mConfVars.put("mail.pop3.proxytimeout", new ProxyConfVar("mail.pop3.proxytimeout", "pop3_max_idle_time", 60, ProxyConfValueType.INTEGER, ProxyConfOverride.LOCALCONFIG, "pop3 network timeout after authentication"));
        mConfVars.put("mail.imap.timeout", new ProxyConfVar("mail.imap.timeout", "imap_max_idle_time", 60, ProxyConfValueType.INTEGER, ProxyConfOverride.LOCALCONFIG, "imap network timeout before authentication"));
        mConfVars.put("mail.imap.proxytimeout", new TimeoutVar("mail.imap.proxytimeout", "imap_authenticated_max_idle_time", 1800, ProxyConfOverride.LOCALCONFIG, 300, "imap network timeout after authentication"));
        mConfVars.put("mail.passerrors", new ProxyConfVar("mail.passerrors", Provisioning.A_zimbraReverseProxyPassErrors, true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.SERVER, "Indicates whether mail proxy will pass any protocol specific errors from the upstream server back to the downstream client"));
        mConfVars.put("mail.auth_http_timeout", new ProxyConfVar("mail.auth_http_timeout", Provisioning.A_zimbraReverseProxyRouteLookupTimeout, new Long(15000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER,"Time interval (ms) given to mail route lookup handler to respond to route lookup request (after this time elapses, Proxy fails over to next handler, or fails the request if there are no more lookup handlers)"));
        mConfVars.put("mail.authwait", new ProxyConfVar("mail.authwait", Provisioning.A_zimbraReverseProxyAuthWaitInterval, new Long(10000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG, "Time delay (ms) after which an incorrect POP/IMAP login attempt will be rejected"));
        mConfVars.put("mail.pop3capa", new Pop3CapaVar());
        mConfVars.put("mail.imapcapa", new ImapCapaVar());
        mConfVars.put("mail.imapid", new ProxyConfVar("mail.imapid", null, "\"NAME\" \"Zimbra\" \"VERSION\" \"" + BuildInfo.VERSION + "\" \"RELEASE\" \"" + BuildInfo.RELEASE + "\"", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "NGINX response to IMAP ID command"));
        mConfVars.put("mail.defaultrealm", new ProxyConfVar("mail.defaultrealm", Provisioning.A_zimbraReverseProxyDefaultRealm, "", ProxyConfValueType.STRING, ProxyConfOverride.SERVER, "Default SASL realm used in case Kerberos principal does not contain realm information"));
        mConfVars.put("mail.sasl_host_from_ip", new SaslHostFromIPVar());
        mConfVars.put("mail.saslapp", new ProxyConfVar("mail.saslapp", null, "nginx", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "Application name used by NGINX to initialize SASL authentication"));
        mConfVars.put("mail.ipmax", new ProxyConfVar("mail.ipmax", Provisioning.A_zimbraReverseProxyIPLoginLimit, new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.CONFIG,"IP Login Limit (Throttle) - 0 means infinity"));
        mConfVars.put("mail.ipttl", new ProxyConfVar("mail.ipttl", Provisioning.A_zimbraReverseProxyIPLoginLimitTime, new Long(3600000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG,"Time interval (ms) after which IP Login Counter is reset"));
        mConfVars.put("mail.imapmax", new ProxyConfVar("mail.imapmax", Provisioning.A_zimbraReverseProxyIPLoginImapLimit, new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.CONFIG,"IMAP Login Limit (Throttle) - 0 means infinity"));
        mConfVars.put("mail.imapttl", new ProxyConfVar("mail.imapttl", Provisioning.A_zimbraReverseProxyIPLoginImapLimitTime, new Long(3600000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG,"Time interval (ms) after which IMAP Login Counter is reset"));
        mConfVars.put("mail.pop3max", new ProxyConfVar("mail.pop3max", Provisioning.A_zimbraReverseProxyIPLoginPop3Limit, new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.CONFIG,"POP3 Login Limit (Throttle) - 0 means infinity"));
        mConfVars.put("mail.pop3ttl", new ProxyConfVar("mail.pop3ttl", Provisioning.A_zimbraReverseProxyIPLoginPop3LimitTime, new Long(3600000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG,"Time interval (ms) after which POP3 Login Counter is reset"));
        mConfVars.put("mail.iprej", new ProxyConfVar("mail.iprej", Provisioning.A_zimbraReverseProxyIpThrottleMsg, "Login rejected from this IP", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Rejection message for IP throttle"));
        mConfVars.put("mail.usermax", new ProxyConfVar("mail.usermax", Provisioning.A_zimbraReverseProxyUserLoginLimit, new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.CONFIG,"User Login Limit (Throttle) - 0 means infinity"));
        mConfVars.put("mail.userttl", new ProxyConfVar("mail.userttl", Provisioning.A_zimbraReverseProxyUserLoginLimitTime, new Long(3600000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG,"Time interval (ms) after which User Login Counter is reset"));
        mConfVars.put("mail.userrej", new ProxyConfVar("mail.userrej", Provisioning.A_zimbraReverseProxyUserThrottleMsg, "Login rejected for this user", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Rejection message for User throttle"));
        mConfVars.put("mail.upstream.pop3xoip", new ProxyConfVar("mail.upstream.pop3xoip", Provisioning.A_zimbraReverseProxySendPop3Xoip, true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.CONFIG,"Whether NGINX issues the POP3 XOIP command to the upstream server prior to logging in (audit purpose)"));
        mConfVars.put("mail.upstream.imapid", new ProxyConfVar("mail.upstream.imapid", Provisioning.A_zimbraReverseProxySendImapId, true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.CONFIG,"Whether NGINX issues the IMAP ID command to the upstream server prior to logging in (audit purpose)"));
        mConfVars.put("mail.ssl.protocols", new MailSSLProtocolsVar());
        mConfVars.put("mail.ssl.preferserverciphers", new ProxyConfVar("mail.ssl.preferserverciphers", null, true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.CONFIG,"Requires TLS protocol server ciphers be preferred over the client's ciphers"));
        mConfVars.put("mail.ssl.ciphers", new ProxyConfVar("mail.ssl.ciphers", Provisioning.A_zimbraReverseProxySSLCiphers, "ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-DSS-AES128-GCM-SHA256:kEDH+AESGCM:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA256:"
                + "ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES256-SHA:ECDHE-ECDSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-DSS-AES128-SHA256:DHE-RSA-AES256-SHA256:DHE-DSS-AES256-SHA:"
                + "DHE-RSA-AES256-SHA:AES128-GCM-SHA256:AES256-GCM-SHA384:AES128:AES256:HIGH:!aNULL:!eNULL:!EXPORT:!DES:!MD5:!PSK:!RC4", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Permitted ciphers for mail proxy"));
        mConfVars.put("mail.ssl.ecdh.curve", new ProxyConfVar("mail.ssl.ecdh.curve", Provisioning.A_zimbraReverseProxySSLECDHCurve, "prime256v1", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"SSL ECDH cipher curve for mail proxy"));
        mConfVars.put("mail.imap.authplain.enabled", new ProxyConfVar("mail.imap.authplain.enabled", Provisioning.A_zimbraReverseProxyImapSaslPlainEnabled, true, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Whether SASL PLAIN is enabled for IMAP"));
        mConfVars.put("mail.imap.authgssapi.enabled", new ProxyConfVar("mail.imap.authgssapi.enabled", Provisioning.A_zimbraReverseProxyImapSaslGssapiEnabled, false, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Whether SASL GSSAPI is enabled for IMAP"));
        mConfVars.put("mail.pop3.authplain.enabled", new ProxyConfVar("mail.pop3.authplain.enabled", Provisioning.A_zimbraReverseProxyPop3SaslPlainEnabled, true, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Whether SASL PLAIN is enabled for POP3"));
        mConfVars.put("mail.pop3.authgssapi.enabled", new ProxyConfVar("mail.pop3.authgssapi.enabled", Provisioning.A_zimbraReverseProxyPop3SaslGssapiEnabled, false, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Whether SASL GSSAPI is enabled for POP3"));
        mConfVars.put("mail.imap.literalauth", new ProxyConfVar("mail.imap.literalauth", null, true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.CONFIG,"Whether NGINX uses literal strings for user name/password when logging in to upstream IMAP server - if false, NGINX uses quoted strings"));
        mConfVars.put("mail.imap.port", new ProxyConfVar("mail.imap.port", Provisioning.A_zimbraImapProxyBindPort, new Integer(143), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Mail Proxy IMAP Port"));
        mConfVars.put("mail.imap.tls", new ProxyConfVar("mail.imap.tls", Provisioning.A_zimbraReverseProxyImapStartTlsMode, "only", ProxyConfValueType.STRING, ProxyConfOverride.SERVER,"TLS support for IMAP - can be on|off|only - on indicates TLS support present, off indicates TLS support absent, only indicates TLS is enforced on unsecure channel"));
        mConfVars.put("mail.imaps.port", new ProxyConfVar("mail.imaps.port", Provisioning.A_zimbraImapSSLProxyBindPort, new Integer(993), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Mail Proxy IMAPS Port"));
        mConfVars.put("mail.pop3.port", new ProxyConfVar("mail.pop3.port", Provisioning.A_zimbraPop3ProxyBindPort, new Integer(110), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Mail Proxy POP3 Port"));
        mConfVars.put("mail.pop3.tls", new ProxyConfVar("mail.pop3.tls", Provisioning.A_zimbraReverseProxyPop3StartTlsMode, "only", ProxyConfValueType.STRING, ProxyConfOverride.SERVER,"TLS support for POP3 - can be on|off|only - on indicates TLS support present, off indicates TLS support absent, only indicates TLS is enforced on unsecure channel"));
        mConfVars.put("mail.pop3s.port", new ProxyConfVar("mail.pop3s.port", Provisioning.A_zimbraPop3SSLProxyBindPort, new Integer(995), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Mail Proxy POP3S Port"));
        mConfVars.put("mail.imap.greeting", new ImapGreetingVar());
        mConfVars.put("mail.pop3.greeting", new Pop3GreetingVar());
        mConfVars.put("mail.enabled", new ProxyConfVar("mail.enabled", Provisioning.A_zimbraReverseProxyMailEnabled, true, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Indicates whether Mail Proxy is enabled"));
        mConfVars.put("mail.imap.enabled", new ProxyConfVar("mail.imap.enabled", Provisioning.A_zimbraReverseProxyMailImapEnabled, true, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Indicates whether Imap Mail Proxy is enabled"));
        mConfVars.put("mail.imaps.enabled", new ProxyConfVar("mail.imaps.enabled", Provisioning.A_zimbraReverseProxyMailImapsEnabled, true, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Indicates whether Imaps Mail Proxy is enabled"));
        mConfVars.put("mail.pop3.enabled", new ProxyConfVar("mail.pop3.enabled", Provisioning.A_zimbraReverseProxyMailPop3Enabled, true, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Indicates whether Pop Mail Proxy is enabled"));
        mConfVars.put("mail.pop3s.enabled", new ProxyConfVar("mail.pop3s.enabled", Provisioning.A_zimbraReverseProxyMailPop3sEnabled, true, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Indicates whether Pops Mail Proxy is enabled"));
        mConfVars.put("mail.proxy.ssl", new ProxyConfVar("mail.proxy.ssl", Provisioning.A_zimbraReverseProxySSLToUpstreamEnabled, true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.SERVER, "Indicates whether using SSL to connect to upstream mail server"));
        mConfVars.put("mail.whitelistip.:servers", new ReverseProxyIPThrottleWhitelist());
        mConfVars.put("mail.whitelist.ttl", new TimeInSecVarWrapper(new ProxyConfVar("mail.whitelist.ttl", Provisioning.A_zimbraReverseProxyIPThrottleWhitelistTime, new Long(300000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG,"Time-to-live, in seconds, of the list of servers for which IP throttling is disabled")));
        mConfVars.put("web.logfile", new ProxyConfVar("web.logfile", null, mWorkingDir + "/log/nginx.access.log", ProxyConfValueType.STRING, ProxyConfOverride.NONE, "Access log file path (relative to ${core.workdir})"));
        mConfVars.put("web.mailmode", new ProxyConfVar("web.mailmode", Provisioning.A_zimbraReverseProxyMailMode, "both", ProxyConfValueType.STRING, ProxyConfOverride.SERVER,"Reverse Proxy Mail Mode - can be http|https|both|redirect|mixed"));
        mConfVars.put("web.server_name.default", new ProxyConfVar("web.server_name.default", "zimbra_server_hostname", "localhost", ProxyConfValueType.STRING, ProxyConfOverride.LOCALCONFIG, "The server name for default server config"));
        mConfVars.put("web.upstream.name", new ProxyConfVar("web.upstream.name", null, ZIMBRA_UPSTREAM_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Symbolic name for HTTP upstream cluster"));
        mConfVars.put("web.upstream.webclient.name", new ProxyConfVar("web.upstream.webclient.name", null, ZIMBRA_UPSTREAM_WEBCLIENT_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Symbolic name for HTTP upstream webclient cluster"));
        mConfVars.put("web.ssl.upstream.name", new ProxyConfVar("web.ssl.upstream.name", null, ZIMBRA_SSL_UPSTREAM_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Symbolic name for HTTPS upstream cluster"));
        mConfVars.put("web.ssl.upstream.webclient.name", new ProxyConfVar("web.ssl.upstream.webclient.name", null, ZIMBRA_SSL_UPSTREAM_WEBCLIENT_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Symbolic name for HTTPS upstream webclient cluster"));
        mConfVars.put("web.upstream.:servers", new WebUpstreamServersVar());
        mConfVars.put("web.upstream.webclient.:servers", new WebUpstreamClientServersVar());
        mConfVars.put("web.server_names.max_size", new ProxyConfVar("web.server_names.max_size", "proxy_server_names_hash_max_size", DEFAULT_SERVERS_NAME_HASH_MAX_SIZE, ProxyConfValueType.INTEGER, ProxyConfOverride.LOCALCONFIG, "the server names hash max size, needed to be increased if too many virtual host names are added"));
        mConfVars.put("web.server_names.bucket_size", new ProxyConfVar("web.server_names.bucket_size", "proxy_server_names_hash_bucket_size", DEFAULT_SERVERS_NAME_HASH_BUCKET_SIZE, ProxyConfValueType.INTEGER, ProxyConfOverride.LOCALCONFIG, "the server names hash bucket size, needed to be increased if too many virtual host names are added"));
        mConfVars.put("web.ssl.upstream.:servers", new WebSSLUpstreamServersVar());
        mConfVars.put("web.ssl.upstream.webclient.:servers", new WebSSLUpstreamClientServersVar());
        mConfVars.put("web.uploadmax", new ProxyConfVar("web.uploadmax", Provisioning.A_zimbraFileUploadMaxSize, new Long(10485760), ProxyConfValueType.LONG, ProxyConfOverride.SERVER,"Maximum accepted client request body size (indicated by Content-Length) - if content length exceeds this limit, then request fails with HTTP 413"));
        mConfVars.put("web.:error_pages", new ErrorPagesVar());
        mConfVars.put("web.http.port", new ProxyConfVar("web.http.port", Provisioning.A_zimbraMailProxyPort, new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Web Proxy HTTP Port"));
        mConfVars.put("web.http.maxbody", new ProxyConfVar("web.http.maxbody", Provisioning.A_zimbraFileUploadMaxSize, new Long(10485760), ProxyConfValueType.LONG, ProxyConfOverride.SERVER,"Maximum accepted client request body size (indicated by Content-Length) - if content length exceeds this limit, then request fails with HTTP 413"));
        mConfVars.put("web.https.port", new ProxyConfVar("web.https.port", Provisioning.A_zimbraMailSSLProxyPort, new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Web Proxy HTTPS Port"));
        mConfVars.put("web.https.maxbody", new ProxyConfVar("web.https.maxbody", Provisioning.A_zimbraFileUploadMaxSize, new Long(10485760), ProxyConfValueType.LONG, ProxyConfOverride.SERVER,"Maximum accepted client request body size (indicated by Content-Length) - if content length exceeds this limit, then request fails with HTTP 413"));
        mConfVars.put("web.ssl.protocols", new WebSSLProtocolsVar());
        mConfVars.put("web.ssl.preferserverciphers", new ProxyConfVar("web.ssl.preferserverciphers", null, true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.CONFIG,"Requires TLS protocol server ciphers be preferred over the client's ciphers"));
        mConfVars.put("web.ssl.ciphers", new ProxyConfVar("web.ssl.ciphers", Provisioning.A_zimbraReverseProxySSLCiphers, "ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-DSS-AES128-GCM-SHA256:kEDH+AESGCM:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA256:"
                + "ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES256-SHA:ECDHE-ECDSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-DSS-AES128-SHA256:DHE-RSA-AES256-SHA256:DHE-DSS-AES256-SHA:"
                + "DHE-RSA-AES256-SHA:AES128-GCM-SHA256:AES256-GCM-SHA384:AES128:AES256:HIGH:!aNULL:!eNULL:!EXPORT:!DES:!MD5:!PSK:!RC4", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Permitted ciphers for web proxy"));
        mConfVars.put("web.ssl.ecdh.curve", new ProxyConfVar("web.ssl.ecdh.curve", Provisioning.A_zimbraReverseProxySSLECDHCurve, "prime256v1", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"SSL ECDH cipher curve for web proxy"));
        mConfVars.put("web.http.uport", new ProxyConfVar("web.http.uport", Provisioning.A_zimbraMailPort, new Integer(80), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Web upstream server port"));
        mConfVars.put("web.upstream.connect.timeout", new ProxyConfVar("web.upstream.connect.timeout", Provisioning.A_zimbraReverseProxyUpstreamConnectTimeout, new Integer(25), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER, "upstream connect timeout"));
        mConfVars.put("web.upstream.read.timeout", new TimeInSecVarWrapper(new ProxyConfVar("web.upstream.read.timeout", Provisioning.A_zimbraReverseProxyUpstreamReadTimeout, new Long(60), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "upstream read timeout")));
        mConfVars.put("web.upstream.send.timeout", new TimeInSecVarWrapper(new ProxyConfVar("web.upstream.send.timeout", Provisioning.A_zimbraReverseProxyUpstreamSendTimeout, new Long(60), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "upstream send timeout")));
        mConfVars.put("web.upstream.polling.timeout", new TimeInSecVarWrapper(new ProxyConfVar("web.upstream.polling.timeout", Provisioning.A_zimbraReverseProxyUpstreamPollingTimeout, new Long(3600), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "the response timeout for Microsoft Active Sync polling")));
        mConfVars.put("web.enabled", new ProxyConfVar("web.enabled", Provisioning.A_zimbraReverseProxyHttpEnabled, false, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER, "Indicates whether HTTP proxying is enabled"));
        mConfVars.put("web.upstream.exactversioncheck", new ProxyConfVar("web.upstream.exactversioncheck", Provisioning.A_zimbraReverseProxyExactServerVersionCheck, "on", ProxyConfValueType.STRING, ProxyConfOverride.SERVER, "Indicates whether nginx will match exact server version against the version received in the client request"));
        mConfVars.put("web.http.enabled", new HttpEnablerVar());
        mConfVars.put("web.https.enabled", new HttpsEnablerVar());
        mConfVars.put("web.upstream.target", new WebProxyUpstreamTargetVar());
        mConfVars.put("web.upstream.webclient.target", new WebProxyUpstreamClientTargetVar());
        mConfVars.put("lookup.available", new ZMLookupAvailableVar());
        mConfVars.put("web.available", new ZMWebAvailableVar());
        mConfVars.put("zmlookup.:handlers", new ZMLookupHandlerVar());
        mConfVars.put("zmlookup.timeout", new ProxyConfVar("zmlookup.timeout", Provisioning.A_zimbraReverseProxyRouteLookupTimeout, new Long(15000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "Time interval (ms) given to lookup handler to respond to route lookup request (after this time elapses, Proxy fails over to next handler, or fails the request if there are no more lookup handlers)"));
        mConfVars.put("zmlookup.retryinterval", new ProxyConfVar("zmlookup.retryinterval", Provisioning.A_zimbraReverseProxyRouteLookupTimeoutCache, new Long(60000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER,"Time interval (ms) given to lookup handler to cache a failed response to route a previous lookup request (after this time elapses, Proxy retries this host)"));
        mConfVars.put("zmlookup.dpasswd", new ProxyConfVar("zmlookup.dpasswd", "ldap_nginx_password", "zmnginx", ProxyConfValueType.STRING, ProxyConfOverride.LOCALCONFIG, "Password for master credentials used by NGINX to log in to upstream for GSSAPI authentication"));
        mConfVars.put("zmlookup.caching", new ProxyConfVar("zmlookup.caching", Provisioning.A_zimbraReverseProxyZmlookupCachingEnabled, true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.SERVER, "Whether to turn on nginx lookup caching"));
        mConfVars.put("zmprefix.url", new ProxyConfVar("zmprefix.url", Provisioning.A_zimbraMailURL, "/", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "http URL prefix for where the zimbra app resides on upstream server"));
        mConfVars.put("web.sso.certauth.port", new ProxyConfVar("web.sso.certauth.port", Provisioning.A_zimbraMailSSLProxyClientCertPort, new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"reverse proxy client cert auth port"));
        mConfVars.put("web.sso.certauth.default.enabled", new ZMSSOCertAuthDefaultEnablerVar());
        mConfVars.put("web.sso.enabled", new ZMSSOEnablerVar());
        mConfVars.put("web.sso.default.enabled", new ZMSSODefaultEnablerVar());
        mConfVars.put("web.admin.default.enabled", new ProxyConfVar("web.admin.default.enabled", Provisioning.A_zimbraReverseProxyAdminEnabled, new Boolean(false), ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER, "Inidicate whether admin console proxy is enabled"));
        mConfVars.put("web.admin.port", new ProxyConfVar("web.admin.port", Provisioning.A_zimbraAdminProxyPort, new Integer(9071), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER, "Admin console proxy port"));
        mConfVars.put("web.admin.uport", new ProxyConfVar("web.admin.uport", Provisioning.A_zimbraAdminPort, new Integer(7071), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER, "Admin console upstream port"));
        mConfVars.put("web.admin.upstream.name", new ProxyConfVar("web.admin.upstream.name", null, ZIMBRA_ADMIN_CONSOLE_UPSTREAM_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "Symbolic name for admin console upstream cluster"));
        mConfVars.put("web.admin.upstream.adminclient.name", new ProxyConfVar("web.admin.upstream.adminclient.name", null, ZIMBRA_ADMIN_CONSOLE_CLIENT_UPSTREAM_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "Symbolic name for admin client console upstream cluster"));
        mConfVars.put("web.admin.upstream.:servers", new WebAdminUpstreamServersVar());
        mConfVars.put("web.admin.upstream.adminclient.:servers", new WebAdminUpstreamAdminClientServersVar());
        mConfVars.put("web.upstream.noop.timeout", new TimeoutVar("web.upstream.noop.timeout", "zimbra_noop_max_timeout", 1200, ProxyConfOverride.LOCALCONFIG, 20, "the response timeout for NoOpRequest"));
        mConfVars.put("web.upstream.waitset.timeout", new TimeoutVar("web.upstream.waitset.timeout", "zimbra_waitset_max_request_timeout", 1200, ProxyConfOverride.LOCALCONFIG, 20, "the response timeout for WaitSetRequest"));
        mConfVars.put("main.accept_mutex", new ProxyConfVar("main.accept_mutex", Provisioning.A_zimbraReverseProxyAcceptMutex, "on", ProxyConfValueType.STRING, ProxyConfOverride.SERVER, "accept_mutex flag for NGINX - can be on|off - on indicates regular distribution, off gets better distribution of client connections between workers"));
        mConfVars.put("web.ews.upstream.disable", new EwsEnablerVar());
        mConfVars.put("web.upstream.ewsserver.:servers", new WebEwsUpstreamServersVar());
        mConfVars.put("web.ssl.upstream.ewsserver.:servers", new WebEwsSSLUpstreamServersVar());
        mConfVars.put("web.ews.upstream.name", new ProxyConfVar("web.ews.upstream.name", null, ZIMBRA_UPSTREAM_EWS_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "Symbolic name for ews upstream server cluster"));
        mConfVars.put("web.ssl.ews.upstream.name", new ProxyConfVar("web.ssl.ews.upstream.name", null, ZIMBRA_SSL_UPSTREAM_EWS_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "Symbolic name for https ews upstream server cluster"));
        mConfVars.put("web.login.upstream.disable", new LoginEnablerVar());
        mConfVars.put("web.upstream.loginserver.:servers", new WebLoginUpstreamServersVar());
        mConfVars.put("web.ssl.upstream.loginserver.:servers", new WebLoginSSLUpstreamServersVar());
        mConfVars.put("web.login.upstream.name", new ProxyConfVar("web.login.upstream.name", null, ZIMBRA_UPSTREAM_LOGIN_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "Symbolic name for upstream login server cluster"));
        mConfVars.put("web.ssl.login.upstream.name", new ProxyConfVar("web.ssl.login.upstream.name", null, ZIMBRA_SSL_UPSTREAM_LOGIN_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "Symbolic name for https upstream login server cluster"));
        mConfVars.put("web.login.upstream.url", new ProxyConfVar("web.login.upstream.url", Provisioning.A_zimbraMailURL, "/", ProxyConfValueType.STRING, ProxyConfOverride.SERVER, "Zimbra Login URL"));
        mConfVars.put("web.upstream.login.target", new WebProxyUpstreamLoginTargetVar());
        mConfVars.put("web.upstream.ews.target", new WebProxyUpstreamEwsTargetVar());
        mConfVars.put("ssl.session.timeout", new TimeInSecVarWrapper(new ProxyConfVar("ssl.session.timeout", Provisioning.A_zimbraReverseProxySSLSessionTimeout, new Long(600), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "SSL session timeout value for the proxy in secs")));
        mConfVars.put("ssl.session.cachesize", new WebSSLSessionCacheSizeVar());
        mConfVars.put("web.xmpp.upstream.proto", new XmppBoshProxyUpstreamProtoVar());
        mConfVars.put("web.xmpp.bosh.upstream.disable", new WebXmppBoshEnablerVar());
        mConfVars.put("web.xmpp.bosh.enabled", new ProxyConfVar("web.xmpp.bosh.enabled", Provisioning.A_zimbraReverseProxyXmppBoshEnabled, true, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Indicates whether XMPP/Bosh Reverse Proxy is enabled"));
        mConfVars.put("web.xmpp.local.bind.url", new ProxyConfVar("web.xmpp.local.bind.url", Provisioning.A_zimbraReverseProxyXmppBoshLocalHttpBindURL, "/http-bind", ProxyConfValueType.STRING, ProxyConfOverride.SERVER, "Local HTTP-BIND URL prefix where ZWC sends XMPP over BOSH requests"));
        mConfVars.put("web.xmpp.remote.bind.url", new ProxyConfVar("web.xmpp.remote.bind.url", Provisioning.A_zimbraReverseProxyXmppBoshRemoteHttpBindURL, "", ProxyConfValueType.STRING, ProxyConfOverride.SERVER, "Remote HTTP-BIND URL prefix for an external XMPP server where XMPP over BOSH requests need to be proxied"));
        mConfVars.put("web.xmpp.bosh.hostname", new ProxyConfVar("web.xmpp.bosh.hostname", Provisioning.A_zimbraReverseProxyXmppBoshHostname, "", ProxyConfValueType.STRING, ProxyConfOverride.SERVER, "Hostname of the external XMPP server where XMPP over BOSH requests need to be proxied"));
        mConfVars.put("web.xmpp.bosh.port", new ProxyConfVar("web.xmpp.bosh.port", Provisioning.A_zimbraReverseProxyXmppBoshPort, new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER, "Port number of the external XMPP server where XMPP over BOSH requests need to be proxied"));
        mConfVars.put("web.xmpp.bosh.timeout", new TimeInSecVarWrapper(new ProxyConfVar("web.xmpp.bosh.timeout", Provisioning.A_zimbraReverseProxyXmppBoshTimeout, new Long(60), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "the response timeout for an external XMPP/BOSH server")));
        mConfVars.put("web.xmpp.bosh.use_ssl", new ProxyConfVar("web.xmpp.bosh.use_ssl", Provisioning.A_zimbraReverseProxyXmppBoshSSL, true, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Indicates whether XMPP/Bosh uses SSL"));
        ProxyConfVar webSslDhParamFile = new ProxyConfVar("web.ssl.dhparam.file", null, mDefaultDhParamFile, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "Filename with DH parameters for EDH ciphers to be used by the proxy");
        mConfVars.put("web.ssl.dhparam.enabled", new WebSSLDhparamEnablerVar(webSslDhParamFile));
        mConfVars.put("web.ssl.dhparam.file", webSslDhParamFile);
        mConfVars.put("upstream.fair.shm.size", new ProxyFairShmVar());
        mConfVars.put("web.strict.servername", new WebStrictServerName());
        mConfVars.put("web.upstream.zx", new WebProxyUpstreamZxTargetVar());
        mConfVars.put("web.upstream.zx.name", new ProxyConfVar("web.upstream.zx.name", null, ZIMBRA_UPSTREAM_ZX_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Symbolic name for HTTP zx upstream"));
        mConfVars.put("web.ssl.upstream.zx.name", new ProxyConfVar("web.ssl.upstream.zx.name", null, ZIMBRA_SSL_UPSTREAM_ZX_NAME, ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Symbolic name for HTTPS zx upstream"));
        mConfVars.put("web.upstream.zx.:servers", new WebUpstreamZxServersVar());
        mConfVars.put("web.ssl.upstream.zx.:servers", new WebSslUpstreamZxServersVar());

        mConfVars.put("web.upstream.onlyoffice.docservice", new OnlyOfficeDocServiceServersVar());
        mConfVars.put("web.upstream.onlyoffice.spellchecker", new OnlyOfficeSpellCheckerServersVar());

        //Get the response headers list from globalconfig
        String[] rspHeaders = ProxyConfVar.configSource.getMultiAttr(Provisioning.A_zimbraReverseProxyResponseHeaders);
        ArrayList<String> rhdr = new ArrayList<String>();
        for(int i = 0; i < rspHeaders.length; i++) {
            rhdr.add(rspHeaders[i]);
        }
        mConfVars.put("web.add.headers.default", new AddHeadersVar("web.add.headers.default", rhdr,
                "add_header directive for default web proxy"));
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

    /* update the default domain variable map from the active configuration */
    public static void updateDefaultDomainVars ()
        throws ServiceException, ProxyConfException
    {
        Set<String> keys = mDomainConfVars.keySet();
        for (String key: keys) {
            mDomainConfVars.get(key).update();
            mVars.put(key,mDomainConfVars.get(key).confValue());
        }
    }

    public static void updateListenAddresses () throws ProxyConfException
    {
        mVars.put("listen.:addresses", new ListenAddressesVar(mListenAddresses).confValue());
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
        ArrayList<String> webUpstreamServers, webUpstreamClientServers, zmLookupHandlers;
        ArrayList<String> webSSLUpstreamServers, webSSLUpstreamClientServers;

        webEnabled = (Boolean)mConfVars.get("web.enabled").rawValue();
        mailEnabled = (Boolean)mConfVars.get("mail.enabled").rawValue();

        webUpstreamServers = (ArrayList<String>) mConfVars.get("web.upstream.:servers").rawValue();
        webUpstreamClientServers = (ArrayList<String>) mConfVars.get("web.upstream.webclient.:servers").rawValue();
        webSSLUpstreamServers = (ArrayList<String>) mConfVars.get("web.ssl.upstream.:servers").rawValue();
        webSSLUpstreamClientServers = (ArrayList<String>) mConfVars.get("web.ssl.upstream.webclient.:servers").rawValue();
        zmLookupHandlers = (ArrayList<String>) mConfVars.get("zmlookup.:handlers").rawValue();

        if (webEnabled && (webUpstreamServers.size() == 0 || webUpstreamClientServers.size() == 0)) {
            mLog.info("Web is enabled but there are no HTTP upstream webclient/mailclient servers");
            validConf = false;
        }

        if (webEnabled && (webSSLUpstreamServers.size() == 0 || webSSLUpstreamClientServers.size() == 0)) {
            mLog.info("Web is enabled but there are no HTTPS upstream webclient/mailclient servers");
            validConf = false;
        }

        if ((webEnabled || mailEnabled) && (zmLookupHandlers.size() == 0)) {
            mLog.info("Proxy is enabled but there are no lookup handlers");
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
        ProxyConfVar.serverSource = mProv.getLocalServer();

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
            displayDefaultVariables();
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

        if (cl.hasOption('f')) {
            mEnforceDNSResolution = true;
        } else {
            mEnforceDNSResolution = false;
        }

        mGenConfPerVhn = ProxyConfVar.serverSource.getBooleanAttr(Provisioning.A_zimbraReverseProxyGenConfigPerVirtualHostname, false);

        try {
        /* upgrade the variable map from the config in force */
            mLog.debug("Loading Attrs in Domain Level");
            mDomainReverseProxyAttrs = loadDomainReverseProxyAttrs();
            mServerAttrs = loadServerAttrs();
            updateListenAddresses();

            mLog.debug("Updating Default Variable Map");
            updateDefaultVars();

            mLog.debug("Processing Config Overrides");
            overrideDefaultVars(cl);

            String clientCA = loadAllClientCertCA();
            writeClientCAtoFile(clientCA);
        } catch (ProxyConfException pe) {
            handleException(pe);
            exitCode = 1;
        } catch (ServiceException se) {
            handleException(se);
            exitCode = 1;
        }

        if (exitCode > 0) {
            mLog.info("Proxy configuration files generation is interrupted by errors");
            return exitCode;
        }
        if (cl.hasOption('D')) {
            displayVariables();
            exitCode = 0;
            return(exitCode);
        }

        if (cl.getArgs().length > 0) {
            usage(null);
            exitCode = 0;
            return(exitCode);
        }

        if (!isWorkableConf()) {
            mLog.warn("Configuration is not valid because no route lookup handlers exist, or because no HTTP/HTTPS upstream servers were found");
            mLog.warn("Please ensure that the output of 'zmprov garpu/garpb' returns at least one entry");
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
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("docs.common")), new File(mConfIncludesDir, getConfFileName("docs.common")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("docs.upstream")), new File(mConfIncludesDir, getConfFileName("docs.upstream")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("onlyoffice.common")), new File(mConfIncludesDir, getConfFileName("onlyoffice.common")));
            expandTemplate(new File(mTemplateDir, getConfTemplateFileName("onlyoffice.upstream")), new File(mConfIncludesDir, getConfFileName("onlyoffice.upstream")));
        } catch (ProxyConfException pe) {
            handleException(pe);
            exitCode = 1;
        } catch (SecurityException se) {
            handleException(se);
            exitCode = 1;
        }
        if (!mDryRun) {
            if (exitCode == 0) {
                mLog.info("Proxy configuration files are generated successfully");
                appendConfGenResultToConf("__SUCCESS__");
            } else {
                mLog.info("Proxy configuration files generation is interrupted by errors");
            }
        }
        return (exitCode);
    }

    private static void handleException(Exception e) {
         mLog.error("Error while expanding templates: " + e.getMessage());
         appendConfGenResultToConf("__CONF_GEN_ERROR__:" + e.getMessage());
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

    public static InetAddress getLookupTargetIPbyIPMode(String hostname) throws ProxyConfException {

        InetAddress[] ips;
        try {
            ips = InetAddress.getAllByName(hostname);
        } catch (UnknownHostException e) {
            throw new ProxyConfException("the lookup target " + hostname
                    + " can't be resolved");
        }
        IPModeEnablerVar.IPMode mode = IPModeEnablerVar.getZimbraIPMode();

        if (mode == IPModeEnablerVar.IPMode.IPV4_ONLY) {
            for (InetAddress ip : ips) {
                if (ip instanceof Inet4Address) {
                    return ip;
                }
            }
            throw new ProxyConfException(
                    "Can't find valid lookup target IPv4 address when zimbra IP mode is IPv4 only");
        } else if (mode == IPModeEnablerVar.IPMode.IPV6_ONLY) {
            for (InetAddress ip : ips) {
                if (ip instanceof Inet6Address) {
                    return ip;
                }
            }
            throw new ProxyConfException(
                    "Can't find valid lookup target IPv6 address when zimbra IP mode is IPv6 only");
        } else {
            for (InetAddress ip : ips) {
                if (ip instanceof Inet4Address) {
                    return ip;
                }
            }
            return ips[0]; // try to return an IPv4, but if there is none,
                           // simply return the first IPv6
        }
    }
}
