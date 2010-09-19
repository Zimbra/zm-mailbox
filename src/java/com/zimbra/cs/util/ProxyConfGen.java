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

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.service.ServiceException;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.account.ldap.LdapProvisioning;
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

@SuppressWarnings("unchecked")
class ProxyConfVar
{
    public String                   mKeyword;
    public String                   mAttribute;
    public ProxyConfValueType       mValueType;
    public Object                   mDefault;
    public Object                   mValue;
    public ProxyConfOverride        mOverride;
    public String                   mDescription;

    private static Log              mLog = LogFactory.getLog (ProxyConfGen.class);
    private static Provisioning     mProv = Provisioning.getInstance();
    public static Entry             configSource = null;
    public static Entry             serverSource = null;

    public ProxyConfVar (String keyword, String attribute, Object defaultValue, ProxyConfValueType valueType, ProxyConfOverride overrideType, String description)
    {
        mKeyword    = keyword;
        mAttribute  = attribute;
        mValueType  = valueType;
        mDefault    = defaultValue;
        mOverride   = overrideType;
        mValue      = mDefault;
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
    public void update () throws ServiceException
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
            /* web.http.enabled and web.https.enabled are special ENABLER that need CUSTOM override */
            if ("web.http.enabled".equalsIgnoreCase(mKeyword))
            {
                /* if mailmode is https (only), then http needs to be disabled */
                
                String mailmode = serverSource.getAttr(Provisioning.A_zimbraReverseProxyMailMode,"both");
                if ("https".equalsIgnoreCase(mailmode)) {
                     mValue = false;
                } else {
                     mValue = true;
                }
            }
            else if ("web.https.enabled".equalsIgnoreCase(mKeyword))
            {
                 /* if mailmode is http (only), then https needs to be disabled */
                 String mailmode = serverSource.getAttr(Provisioning.A_zimbraReverseProxyMailMode,"both");
                 if ("http".equalsIgnoreCase(mailmode)) {
                     mValue = false;
                 } else {
                     mValue = true;
                 }
            }
        } else if (mValueType == ProxyConfValueType.TIME) {
            updateTime();
        } else if (mValueType == ProxyConfValueType.CUSTOM) {
           
            if ("mail.pop3.greeting".equalsIgnoreCase(mKeyword))
            {
                if (serverSource.getBooleanAttr("zimbraReverseProxyPop3ExposeVersionOnBanner",false)) {
                    mValue = "+OK " + "Zimbra " + BuildInfo.VERSION + " POP3 ready";
                } else {
                    mValue = "";
                }
            }
            else if ("mail.imap.greeting".equalsIgnoreCase(mKeyword))
            {
                if (serverSource.getBooleanAttr("zimbraReverseProxyImapExposeVersionOnBanner",false)) {
                    mValue = "* OK " + "Zimbra " + BuildInfo.VERSION + " IMAP4 ready";
                } else {
                    mValue = "";
                }
            }

            else if ("mail.sasl_host_from_ip".equalsIgnoreCase(mKeyword))
            {
                if (LC.krb5_service_principal_from_interface_address.booleanValue()) {
                    mValue = true;
                }
                else {
                    mValue = false;
                }
            }

            else if ("memcache.:servers".equalsIgnoreCase(mKeyword))
            {
                ArrayList<String> servers = new ArrayList<String>();

                /* $(zmprov gamcs) */
                List<Server> mcs = mProv.getAllServers(Provisioning.SERVICE_MEMCACHED);
                for (Server mc : mcs)
                {
                    String serverName = mc.getAttr(Provisioning.A_zimbraServiceHostname,"");
                    int serverPort = mc.getIntAttr(Provisioning.A_zimbraMemcachedBindPort,11211); 
                    Formatter f = new Formatter();
                    f.format("%s:%d", serverName, serverPort);

                    servers.add(f.toString());
                }

                mValue = servers;
            }

            else if ("mail.:auth_http".equalsIgnoreCase(mKeyword) || "web.:routehandlers".equalsIgnoreCase(mKeyword))
            {
                ArrayList<String> servers = new ArrayList<String>();

                /* $(zmprov garpu) */
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
            else if ("web.upstream.:servers".equalsIgnoreCase(mKeyword))
            {
                ArrayList<String> servers = new ArrayList<String>();
                /* $(zmprov garpb) */
                List<Server> us = mProv.getAllServers();

                for (Server u : us)
                {
                    boolean isTarget = u.getBooleanAttr(Provisioning.A_zimbraReverseProxyLookupTarget, false);
                    if (isTarget)
                    {
                        String mode = u.getAttr(Provisioning.A_zimbraMailMode, "");
                        String serverName = u.getAttr(Provisioning.A_zimbraServiceHostname, "");

                        if (mode.equalsIgnoreCase(Provisioning.MailMode.http.toString()) ||
                            mode.equalsIgnoreCase(Provisioning.MailMode.mixed.toString()) ||
                            mode.equalsIgnoreCase(Provisioning.MailMode.both.toString())
                        ) {
                            int serverPort = u.getIntAttr(Provisioning.A_zimbraMailPort,0);
                            Formatter f = new Formatter();
                            f.format("%s:%d", serverName, serverPort);
                            servers.add(f.toString());
                            mLog.info("Added server to HTTP upstream: " + serverName);
                        } else {
                            mLog.warn("Upstream: Ignoring server:" + serverName + " because its mail mode is:" + mode);
                        }

                    }
                }

                mValue = servers;
            }
            else if ("mail.imapcapa".equalsIgnoreCase(mKeyword))
            {
                ArrayList<String> capabilities = new ArrayList<String>();
                String[] capabilityNames = serverSource.getMultiAttr("zimbraReverseProxyImapEnabledCapability");
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
            else if ("mail.pop3capa".equalsIgnoreCase(mKeyword))
            {
                ArrayList<String> capabilities = new ArrayList<String>();
                String[] capabilityNames = serverSource.getMultiAttr("zimbraReverseProxyPop3EnabledCapability");
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
            if ("memcache.:servers".equalsIgnoreCase(mKeyword))
            {
                ArrayList<String> servers = (ArrayList<String>) o;
                String conf = "";
                for (String s: servers)
                {
                    conf = conf + "  servers   " + s + ";" + "\n";
                }
                return conf;
            }
            else if ("mail.:auth_http".equalsIgnoreCase(mKeyword))
            {
                String REVERSE_PROXY_PATH = ExtensionDispatcherServlet.EXTENSION_PATH + "/nginx-lookup";
                ArrayList<String> servers = (ArrayList<String>) o;
                String conf = "";
                for (String s: servers)
                {
                    conf = conf + "    auth_http   " + s + REVERSE_PROXY_PATH + ";" + "\n";
                }
                return conf;
            }
            else if ("web.:routehandlers".equalsIgnoreCase(mKeyword))
            {
                String REVERSE_PROXY_PATH = ExtensionDispatcherServlet.EXTENSION_PATH + "/nginx-lookup";
                ArrayList<String> servers = (ArrayList<String>) o;
                String conf = "";
                for (String s: servers)
                {
                    conf = conf + "    zmroutehandlers   " + s + REVERSE_PROXY_PATH + ";" + "\n";
                }
                return conf;
            }
            else if ("web.upstream.:servers".equalsIgnoreCase(mKeyword))
            {
                ArrayList<String> servers = (ArrayList<String>) o;
                String conf = "";
                for (String s: servers)
                {
                    conf = conf + "    server   " + s + ";" + "\n";
                }
                return conf;
            }
            else if ("mail.pop3.greeting".equalsIgnoreCase(mKeyword))
            {
                return formatString(o);
            }
            else if ("mail.imap.greeting".equalsIgnoreCase(mKeyword))
            {
                return formatString(o);
            }
            else if ("mail.sasl_host_from_ip".equalsIgnoreCase(mKeyword))
            {
                return formatBoolean(o);
            }
            else if ("mail.imapcapa".equalsIgnoreCase(mKeyword))
            {
                ArrayList<String> capabilities = (ArrayList<String>) o;
                String capa = "";
                for (String c: capabilities)
                {
                    capa = capa + " " + "\"" + c + "\"";
                }
                return capa;
            }
            else if ("mail.pop3capa".equalsIgnoreCase(mKeyword))
            {
                ArrayList<String> capabilities = (ArrayList<String>) o;
                String capa = "";
                for (String c: capabilities)
                {
                    capa = capa + " " + "\"" + c + "\"";
                }
                return capa;
            }
            else
            {
                throw new ProxyConfException ("Unhandled keyword: " + mKeyword);
            }
        }
    }

    public void updateString ()
    {
        if (mOverride == ProxyConfOverride.CONFIG) {
            mValue = configSource.getAttr(mAttribute,(String)mDefault);
        } else if (mOverride == ProxyConfOverride.LOCALCONFIG) {
            mValue = lcValue(mAttribute,(String)mDefault);
        } else if (mOverride == ProxyConfOverride.SERVER) {
            mValue = serverSource.getAttr(mAttribute,(String)mDefault);
        }
    }

    public String formatString (Object o)
    {
        Formatter f = new Formatter();
        f.format("%s", o);
        return f.toString();
    }

    public void updateBoolean ()
    {
        if (mOverride == ProxyConfOverride.CONFIG) {
            mValue = configSource.getBooleanAttr(mAttribute,(Boolean)mDefault);
        } else if (mOverride == ProxyConfOverride.LOCALCONFIG) {
            mValue = Boolean.valueOf(lcValue(mAttribute,mDefault.toString()));
        } else if (mOverride == ProxyConfOverride.SERVER) {
            mValue = serverSource.getBooleanAttr(mAttribute,(Boolean)mDefault);
        }
    }

    public String formatBoolean (Object o)
    {
        if ((Boolean)o)
            return "on";
        return "off";
    }

    public void updateEnabler ()
    {
        updateBoolean();
    }

    public String formatEnabler (Object o)
    {
        if ((Boolean)o)
            return "";
        return "#";
    }

    public void updateTime ()
    {
        if (mOverride == ProxyConfOverride.CONFIG) {
            mValue = new Long(configSource.getTimeInterval(mAttribute,(Long)mDefault));
        } else if (mOverride == ProxyConfOverride.LOCALCONFIG) {
            mValue = new Long(DateUtil.getTimeInterval(lcValue(mAttribute,
                mDefault.toString()), ((Long)mDefault).longValue()));
        } else if (mOverride == ProxyConfOverride.SERVER) {
            mValue = new Long(serverSource.getTimeInterval(mAttribute,(Long)mDefault));
        }
    }

    public String formatTime (Object o)
    {
        Formatter f = new Formatter();
        f.format("%dms", (Long)o);
        return f.toString();
    }

    public void updateInteger ()
    {
        if (mOverride == ProxyConfOverride.CONFIG) {
            mValue = new Integer(configSource.getIntAttr(mAttribute,(Integer)mDefault));
        } else if (mOverride == ProxyConfOverride.LOCALCONFIG) {
            mValue = Integer.valueOf(lcValue(mAttribute,mDefault.toString()));
        } else if (mOverride == ProxyConfOverride.SERVER) {
            mValue = new Integer(serverSource.getIntAttr(mAttribute,(Integer)mDefault));
        }
    }

    public String formatInteger (Object o)
    {
        Formatter f = new Formatter();
        f.format("%d", (Integer)o);
        return f.toString();
    }

    public void updateLong ()
    {
        if (mOverride == ProxyConfOverride.CONFIG) {
            mValue = new Long(configSource.getLongAttr(mAttribute,(Long)mDefault));
        } else if (mOverride == ProxyConfOverride.LOCALCONFIG) {
            mValue = Long.valueOf(lcValue(mAttribute,mDefault.toString()));
        } else if (mOverride == ProxyConfOverride.SERVER) {
            mValue = new Long(serverSource.getLongAttr(mAttribute,(Long)mDefault));
        }
    }

    public String formatLong (Object o)
    {
        Formatter f = new Formatter();
        Long l = (Long)o;
        
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

/**
 * A simple class of Pair<VirtualHostName, DomainName>. Uses
 * this only for convenient and HashMap can't guarantee order
 * 
 * @author jiankuan
 *
 */
class DnVhnMap {
    public String domainName;
    public String virtualHostname;
    public DnVhnMap(String dn, String vhn) {
        this.domainName = dn;
        this.virtualHostname = vhn;
    }
}

public class ProxyConfGen
{
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
    private static String mDefaultSSLCrt = mConfDir + File.separator + "nginx.crt";
    private static String mDefaultSSLKey = mConfDir + File.separator + "nginx.key";
    private static String mConfIncludesDir = mConfDir + File.separator + mIncDir;
    private static String mConfPrefix = "nginx.conf";
    private static String mTemplatePrefix = mConfPrefix;
    private static String mTemplateSuffix = ".template";
    private static Provisioning mProv = null;
    private static String mHost = null;
    private static Server mServer = null;
    private static Map<String, ProxyConfVar> mConfVars = new HashMap<String, ProxyConfVar>();
    private static Map<String, String> mVars = new HashMap<String, String>();
    private static List<DnVhnMap> mQualifiedVhns;

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
     * Retrieve the cert/key pair of each reverse proxy domain from LDAP. Only
     * the domains with non-null virtual host name, cert and key will be
     * retrieved.
     * 
     * @return a list of domain name.
     * @throws ServiceException
     *             this method can work only when LDAP is available
     * @author Jiankuan
     */
    private static List<DnVhnMap> loadReverseProxyVhns()
            throws ServiceException {

        if (!(mProv instanceof LdapProvisioning))
            throw ServiceException.INVALID_REQUEST(
                "The method can work only when LDAP is available", null);

        final Set<String> attrsNeeded = new HashSet<String>();
        attrsNeeded.add(Provisioning.A_zimbraVirtualHostname);
        attrsNeeded.add(Provisioning.A_zimbraSSLCertificate);
        attrsNeeded.add(Provisioning.A_zimbraSSLPrivateKey);

        final List<DnVhnMap> result = new ArrayList<DnVhnMap>();

        // visit domains
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            public void visit(NamedEntry entry) throws ServiceException {
                String domainName = entry
                    .getAttr(Provisioning.A_zimbraDomainName);
                String[] virtualHostnames = entry
                    .getMultiAttr(Provisioning.A_zimbraVirtualHostname);
                String certificate = entry
                    .getAttr(Provisioning.A_zimbraSSLCertificate);
                String privateKey = entry
                    .getAttr(Provisioning.A_zimbraSSLPrivateKey);
                if (virtualHostnames.length == 0 || certificate == null
                            || privateKey == null) {
                    return; // ignore the items that don't have virtual host
                            // name, cert or key. Those domains will use the
                            // config
                }

                for(String vhn: virtualHostnames) {
                    result.add(new DnVhnMap(domainName, vhn));
                }
            }
        };

        mProv.getAllDomains(visitor,
            attrsNeeded.toArray(new String[attrsNeeded.size()]));
        return result;
    }

    /* Guess how to find a server object -- taken from ProvUtil::guessServerBy */
    public static ServerBy guessServerBy(String value) {
        if (Provisioning.isUUID(value))
            return ServerBy.id;
        return ServerBy.name;
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

    public static String getCoreConf () {
        return mConfPrefix;
    }

    public static String getCoreConfTemplate () {
        return mTemplatePrefix + mTemplateSuffix;
    }

    public static String getMainConf () {
        return mConfPrefix + ".main";
    }

    public static String getMainConfTemplate () {
        return mTemplatePrefix + ".main" + mTemplateSuffix;
    }

    public static String getMemcacheConf () {
        return mConfPrefix + ".memcache";
    }

    public static String getMemcacheConfTemplate () {
        return mTemplatePrefix + ".memcache" + mTemplateSuffix;
    }

    public static String getMailConf () {
        return mConfPrefix + ".mail";
    }

    public static String getMailConfTemplate () {
        return mTemplatePrefix + ".mail" + mTemplateSuffix;
    }

    public static String getMailImapConf () {
        return mConfPrefix + ".mail.imap";
    }

    public static String getMailImapConfTemplate () {
        return mTemplatePrefix + ".mail.imap" + mTemplateSuffix;
    }

    public static String getMailImapSConf () {
        return mConfPrefix + ".mail.imaps";
    }

    public static String getMailImapSConfTemplate () {
        return mTemplatePrefix + ".mail.imaps" + mTemplateSuffix;
    }

    public static String getMailPop3Conf () {
        return mConfPrefix + ".mail.pop3";
    }

    public static String getMailPop3ConfTemplate () {
        return mTemplatePrefix + ".mail.pop3" + mTemplateSuffix;
    }

    public static String getMailPop3SConf () {
        return mConfPrefix + ".mail.pop3s";
    }

    public static String getMailPop3SConfTemplate () {
        return mTemplatePrefix + ".mail.pop3s" + mTemplateSuffix;
    }

    public static String getWebConf () {
        return mConfPrefix + ".web";
    }

    public static String getWebConfTemplate () {
        return mTemplatePrefix + ".web" + mTemplateSuffix;
    }

    public static String getWebHttpConf () {
        return mConfPrefix + ".web.http";
    }

    public static String getWebHttpConfTemplate () {
        return mTemplatePrefix + ".web.http" + mTemplateSuffix;
    }

    public static String getWebHttpSConf () {
        return mConfPrefix + ".web.https";
    }

    public static String getWebHttpSConfTemplate () {
        return mTemplatePrefix + ".web.https" + mTemplateSuffix;
    }

    public static String getWebHttpModeConf (String mode) {
        return mConfPrefix + ".web.http.mode-" + mode;
    }

    public static String getWebHttpModeConfTemplate (String mode) {
        return mTemplatePrefix + ".web.http.mode-" + mode + mTemplateSuffix;
    }

    public static String getWebHttpSModeConf (String mode) {
        return mConfPrefix + ".web.https.mode-" + mode;
    }

    public static String getWebHttpSModeConfTemplate (String mode) {
        return mTemplatePrefix + ".web.https.mode-" + mode + mTemplateSuffix;
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
            Matcher cmdMatcher = cmdPattern.matcher(line);
            if(cmdMatcher.matches()) {
                //the command is found
                String[] cmd_arg = cmdMatcher.group(2).split("[ \t]+", 2);
                //command selection can be extracted if more commands are introduced
                if(cmd_arg.length == 2 && 
                        cmd_arg[0].compareTo("explode") == 0 && 
                        cmd_arg[1].compareTo("vhn_ssl") == 0) {
                    expandTemplateExplodeSSLConfigsForAllVhns(r, w);
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
     * Enumerate all domain names and apply them into the var replacement
     * @author Jiankuan
     */
    private static void expandTemplateExplodeSSLConfigsForAllVhns(
        BufferedReader temp, BufferedWriter conf) throws IOException {
        int size = mQualifiedVhns.size();
        List<String> cache = null;
        if (size > 0) {
            mVars.put("server_name.enabled", ""); // enable 'server_name'
                                                  // command
            Iterator<DnVhnMap> it = mQualifiedVhns.iterator();
            DnVhnMap item = it.next();
            mVars.put("vhn", item.virtualHostname);
            mVars.put("ssl.crt", mDomainSSLDir + File.separator +
                item.domainName + mSSLCrtExt);
            mVars.put("ssl.key", mDomainSSLDir + File.separator +
                item.domainName + mSSLKeyExt);
            cache = expandTemplateAndCache(temp, conf);
            conf.newLine();
            while (it.hasNext()) {
                item = it.next();
                mVars.put("vhn", item.virtualHostname);
                mVars.put("ssl.crt", mDomainSSLDir + File.separator +
                    item.domainName + mSSLCrtExt);
                mVars.put("ssl.key", mDomainSSLDir + File.separator +
                    item.domainName + mSSLKeyExt);
                expandTempateFromCache(cache, conf);
            }
        }

        // always write the default configs
        mVars.put("server_name.enabled", "#"); // comment out "server_name"
                                               // command
        mVars.put("vhn", "default");
        mVars.put("ssl.crt", mDefaultSSLCrt);
        mVars.put("ssl.key", mDefaultSSLKey);
        if (cache == null) {
            expandTemplateSimple(temp, conf);
        } else {
            expandTempateFromCache(cache, conf);
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

    public static ArrayList<String> getDefaultImapCapabilities ()
    {
        ArrayList<String> imapCapabilities = new ArrayList<String> ();
        imapCapabilities.add("IMAP4rev1");
        imapCapabilities.add("ID");
        imapCapabilities.add("LITERAL+");
        imapCapabilities.add("SASL-IR");
        imapCapabilities.add("IDLE");
        imapCapabilities.add("NAMESPACE");
        return imapCapabilities;
    }

    public static ArrayList<String> getDefaultPop3Capabilities ()
    {
        ArrayList<String> pop3Capabilities = new ArrayList<String> ();
        pop3Capabilities.add("TOP");
        pop3Capabilities.add("USER");
        pop3Capabilities.add("UIDL");
        pop3Capabilities.add("EXPIRE 31 USER");
        return pop3Capabilities;
    }

    public static void buildDefaultVars ()
    {
        mConfVars.put("core.workdir", new ProxyConfVar("core.workdir", null, mWorkingDir, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "Working Directory for NGINX worker processes"));
        mConfVars.put("core.includes", new ProxyConfVar("core.includes", null, mIncDir, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "Include directory (relative to ${core.workdir}/conf)"));
        mConfVars.put("core.cprefix", new ProxyConfVar("core.cprefix", null, mConfPrefix, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "Common config file prefix"));
        mConfVars.put("core.tprefix", new ProxyConfVar("core.tprefix", null, mTemplatePrefix, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "Common template file prefix"));
        mConfVars.put("ssl.crt.default", new ProxyConfVar("ssl.crt.default", null, mDefaultSSLCrt, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "default nginx certificate file path"));
        mConfVars.put("ssl.key.default", new ProxyConfVar("ssl.key.default", null, mDefaultSSLKey, ProxyConfValueType.STRING, ProxyConfOverride.NONE, "default nginx private key file path"));
        mConfVars.put("main.user", new ProxyConfVar("main.user", null, "zimbra", ProxyConfValueType.STRING, ProxyConfOverride.NONE, "The user as which the worker processes will run"));
        mConfVars.put("main.group", new ProxyConfVar("main.group", null, "zimbra", ProxyConfValueType.STRING, ProxyConfOverride.NONE, "The group as which the worker processes will run"));
        mConfVars.put("main.workers", new ProxyConfVar("main.workers", "zimbraReverseProxyWorkerProcesses", new Integer(4), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER, "Number of worker processes"));
        mConfVars.put("main.pidfile", new ProxyConfVar("main.pidfile", null, "log/nginx.pid", ProxyConfValueType.STRING, ProxyConfOverride.NONE, "PID file path (relative to ${core.workdir})"));
        mConfVars.put("main.logfile", new ProxyConfVar("main.logfile", null, "log/nginx.log", ProxyConfValueType.STRING, ProxyConfOverride.NONE, "Log file path (relative to ${core.workdir})"));
        mConfVars.put("main.loglevel", new ProxyConfVar("main.loglevel", "zimbraReverseProxyLogLevel", "info", ProxyConfValueType.STRING, ProxyConfOverride.SERVER, "Log level - can be debug|info|notice|warn|error|crit"));
        mConfVars.put("main.connections", new ProxyConfVar("main.connections", "zimbraReverseProxyWorkerConnections", new Integer(10240), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER, "Maximum number of simultaneous connections per worker process"));
        mConfVars.put("main.krb5keytab", new ProxyConfVar("main.krb5keytab", "krb5_keytab", "/opt/zimbra/conf/krb5.keytab", ProxyConfValueType.STRING, ProxyConfOverride.LOCALCONFIG, "Path to kerberos keytab file used for GSSAPI authentication"));
        mConfVars.put("memcache.:servers", new ProxyConfVar("memcache.:servers", null, new ArrayList<String>(), ProxyConfValueType.CUSTOM, ProxyConfOverride.CUSTOM, "List of known memcache servers (i.e. servers having imapproxy service enabled)"));
        mConfVars.put("memcache.timeout", new ProxyConfVar("memcache.timeout", "zimbraReverseProxyCacheFetchTimeout", new Long(3000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG, "Time (ms) given to a cache-fetch operation to complete"));
        mConfVars.put("memcache.reconnect", new ProxyConfVar("memcache.reconnect", "zimbraReverseProxyCacheReconnectInterval", new Long(60000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG, "Time (ms) after which NGINX will attempt to re-establish a broken connection to a memcache server"));
        mConfVars.put("memcache.ttl", new ProxyConfVar("memcache.ttl", "zimbraReverseProxyCacheEntryTTL", new Long(3600000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG, "Time interval (ms) for which cached entries remain in memcache"));
        mConfVars.put("memcache.unqual", new ProxyConfVar("memcache.unqual", null, false, ProxyConfValueType.BOOLEAN, ProxyConfOverride.NONE, "Deprecated - always set to false"));
        mConfVars.put("mail.ctimeout", new ProxyConfVar("mail.ctimeout", "zimbraReverseProxyConnectTimeout", new Long(120000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "Time interval (ms) after which a POP/IMAP proxy connection to a remote host will give up"));
        mConfVars.put("mail.timeout", new ProxyConfVar("mail.timeout", "zimbraReverseProxyInactivityTimeout", new Long(3600000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER, "Time interval (ms) after which, if a POP/IMAP connection is inactive, it will be automatically disconnected"));
        mConfVars.put("mail.passerrors", new ProxyConfVar("mail.passerrors", "zimbraReverseProxyPassErrors", true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.SERVER, "Indicates whether mail proxy will pass any protocol specific errors from the upstream server back to the downstream client"));
        mConfVars.put("mail.:auth_http", new ProxyConfVar("mail.:auth_http", "zimbraReverseProxyLookupTarget", new ArrayList<String>(), ProxyConfValueType.CUSTOM, ProxyConfOverride.CUSTOM, "List of mail route lookup handlers (i.e. servers for which zimbraReverseProxyLookupTarget is true)"));
        mConfVars.put("mail.auth_http_timeout", new ProxyConfVar("mail.auth_http_timeout", "zimbraReverseProxyRouteLookupTimeout", new Long(15000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER,"Time interval (ms) given to mail route lookup handler to respond to route lookup request (after this time elapses, Proxy fails over to next handler, or fails the request if there are no more lookup handlers)"));
        mConfVars.put("mail.auth_http_timeout_cache", new ProxyConfVar("mail.auth_http_timeout_cache", "zimbraReverseProxyRouteLookupTimeoutCache", new Long(60000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER,"Time interval (ms) given to mail route lookup handler to cache a failed response to route a previous lookup request (after this time elapses, Proxy retries this host)"));
        mConfVars.put("mail.authwait", new ProxyConfVar("mail.authwait", "zimbraReverseProxyAuthWaitInterval", new Long(10000), ProxyConfValueType.TIME, ProxyConfOverride.CONFIG, "Time delay (ms) after which an incorrect POP/IMAP login attempt will be rejected"));
        mConfVars.put("mail.pop3capa", new ProxyConfVar("mail.pop3capa", "zimbraReverseProxyPop3EnabledCapability", getDefaultPop3Capabilities(), ProxyConfValueType.CUSTOM, ProxyConfOverride.CUSTOM, "POP3 Capability List"));
        mConfVars.put("mail.imapcapa", new ProxyConfVar("mail.imapcapa", "zimbraReverseProxyImapEnabledCapability", getDefaultImapCapabilities(), ProxyConfValueType.CUSTOM, ProxyConfOverride.CUSTOM, "IMAP Capability List"));
        mConfVars.put("mail.imapid", new ProxyConfVar("mail.imapid", null, "\"NAME\" \"Zimbra\" \"VERSION\" \"" + BuildInfo.VERSION + "\" \"RELEASE\" \"" + BuildInfo.RELEASE + "\"", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "NGINX response to IMAP ID command"));
        mConfVars.put("mail.dpasswd", new ProxyConfVar("mail.dpasswd", "ldap_nginx_password", "zmnginx", ProxyConfValueType.STRING, ProxyConfOverride.LOCALCONFIG, "Password for master credentials used by NGINX to log in to upstream for GSSAPI authentication"));
        mConfVars.put("mail.defaultrealm", new ProxyConfVar("mail.defaultrealm", "zimbraReverseProxyDefaultRealm", "", ProxyConfValueType.STRING, ProxyConfOverride.SERVER, "Default SASL realm used in case Kerberos principal does not contain realm information"));
        mConfVars.put("mail.sasl_host_from_ip", new ProxyConfVar("mail.sasl_host_from_ip", "krb5_service_principal_from_interface_address", false, ProxyConfValueType.CUSTOM, ProxyConfOverride.LOCALCONFIG, "Whether to use incoming interface IP address to determine service principal name (if true, IP address is reverse mapped to DNS name, else host name of proxy is used)"));
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
        mConfVars.put("mail.imap.greeting", new ProxyConfVar("mail.imap.greeting", "zimbraReverseProxyPop3ExposeVersionOnBanner", "", ProxyConfValueType.CUSTOM, ProxyConfOverride.CONFIG,"Proxy IMAP banner message (contains build version if zimbraReverseProxyImapExposeVersionOnBanner is true)"));
        mConfVars.put("mail.pop3.greeting", new ProxyConfVar("mail.pop3.greeting", "zimbraReverseProxyPop3ExposeVersionOnBanner", "", ProxyConfValueType.CUSTOM, ProxyConfOverride.CONFIG,"Proxy POP3 banner message (contains build version if zimbraReverseProxyPop3ExposeVersionOnBanner is true)"));
        mConfVars.put("mail.enabled", new ProxyConfVar("mail.enabled", "zimbraReverseProxyMailEnabled", true, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER,"Indicates whether Mail Proxy is enabled"));
        mConfVars.put("web.mailmode", new ProxyConfVar("web.mailmode", Provisioning.A_zimbraReverseProxyMailMode, "both", ProxyConfValueType.STRING, ProxyConfOverride.SERVER,"Reverse Proxy Mail Mode - can be http|https|both|redirect|mixed"));
        mConfVars.put("web.upstream.name", new ProxyConfVar("web.upstream.name", null, "zimbra", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG,"Symbolic name for HTTP upstream cluster"));
        mConfVars.put("web.upstream.:servers", new ProxyConfVar("web.upstream.:servers", "zimbraReverseProxyLookupTarget", new ArrayList<String>(), ProxyConfValueType.CUSTOM, ProxyConfOverride.CONFIG,"List of upstream HTTP servers used by Web Proxy (i.e. servers for which zimbraReverseProxyLookupTarget is true, and whose mail mode is http|mixed|both)"));
        mConfVars.put("web.:routehandlers", new ProxyConfVar("web.:routehandlers", "zimbraReverseProxyLookupTarget", new ArrayList<String>(), ProxyConfValueType.CUSTOM, ProxyConfOverride.CUSTOM,"List of web route lookup handlers (i.e. servers for which zimbraReverseProxyLookupTarget is true)"));
        mConfVars.put("web.routetimeout", new ProxyConfVar("web.routetimeout", "zimbraReverseProxyRouteLookupTimeout", new Long(15000), ProxyConfValueType.TIME, ProxyConfOverride.SERVER,"Time interval (ms) given to web route lookup handler to respond to route lookup request (after this time elapses, Proxy fails over to next handler, or fails the request if there are no more lookup handlers)"));
        mConfVars.put("web.uploadmax", new ProxyConfVar("web.uploadmax", "zimbraFileUploadMaxSize", new Long(10485760), ProxyConfValueType.LONG, ProxyConfOverride.SERVER,"Maximum accepted client request body size (indicated by Content-Length) - if content length exceeds this limit, then request fails with HTTP 413"));
        mConfVars.put("web.http.port", new ProxyConfVar("web.http.port", Provisioning.A_zimbraMailProxyPort, new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Web Proxy HTTP Port"));
        mConfVars.put("web.http.maxbody", new ProxyConfVar("web.http.maxbody", "zimbraFileUploadMaxSize", new Long(10485760), ProxyConfValueType.LONG, ProxyConfOverride.SERVER,"Maximum accepted client request body size (indicated by Content-Length) - if content length exceeds this limit, then request fails with HTTP 413"));
        mConfVars.put("web.https.port", new ProxyConfVar("web.https.port", Provisioning.A_zimbraMailSSLProxyPort, new Integer(0), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Web Proxy HTTPS Port"));
        mConfVars.put("web.https.maxbody", new ProxyConfVar("web.https.maxbody", "zimbraFileUploadMaxSize", new Long(10485760), ProxyConfValueType.LONG, ProxyConfOverride.SERVER,"Maximum accepted client request body size (indicated by Content-Length) - if content length exceeds this limit, then request fails with HTTP 413"));
        mConfVars.put("web.ssl.preferserverciphers", new ProxyConfVar("web.ssl.preferserverciphers", null, true, ProxyConfValueType.BOOLEAN, ProxyConfOverride.CONFIG,"Requires protocols SSLv3 and TLSv1 server ciphers be preferred over the client's ciphers"));
        mConfVars.put("web.ssl.ciphers", new ProxyConfVar("web.ssl.ciphers", "zimbraReverseProxySSLCiphers", "!SSLv2:!MD5:HIGH", ProxyConfValueType.STRING, ProxyConfOverride.CONFIG, "Permitted ciphers for mail proxy"));
        mConfVars.put("web.http.uport", new ProxyConfVar("web.http.uport", Provisioning.A_zimbraMailPort, new Integer(80), ProxyConfValueType.INTEGER, ProxyConfOverride.SERVER,"Web upstream server port"));
        mConfVars.put("web.enabled", new ProxyConfVar("web.enabled", "zimbraReverseProxyHttpEnabled", false, ProxyConfValueType.ENABLER, ProxyConfOverride.SERVER, "Indicates whether HTTP proxying is enabled"));
        mConfVars.put("web.http.enabled", new ProxyConfVar("web.http.enabled", null, true, ProxyConfValueType.ENABLER, ProxyConfOverride.CUSTOM,"Indicates whether HTTP Proxy will accept connections on HTTP (true unless zimbraReverseProxyMailMode is 'https')"));
        mConfVars.put("web.https.enabled", new ProxyConfVar("web.https.enabled", null, true, ProxyConfValueType.ENABLER, ProxyConfOverride.CUSTOM,"Indicates whether HTTP Proxy will accept connections on HTTPS (true unless zimbraReverseProxyMailMode is 'http')"));
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
        ArrayList<String> webUpstreamServers, mailRouteHandlers;

        webEnabled = (Boolean)mConfVars.get("web.enabled").rawValue();
        mailEnabled = (Boolean)mConfVars.get("mail.enabled").rawValue();

        webUpstreamServers = (ArrayList<String>) mConfVars.get("web.upstream.:servers").rawValue();
        mailRouteHandlers = (ArrayList<String>) mConfVars.get("mail.:auth_http").rawValue();

        if (mailEnabled && (mailRouteHandlers.size() == 0)) {
            mLog.info("Mail is enabled but there are no route lookup handlers (Config will not be written)");
            validConf = false;
        }

        if (webEnabled && (webUpstreamServers.size() == 0)) {
            mLog.info("Web is enabled but there are no upstream servers (Config will not be written)");
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

        mProv = Provisioning.getInstance();
        ProxyConfVar.configSource = mProv.getConfig();
        ProxyConfVar.serverSource = ProxyConfVar.configSource;

        if (cl.hasOption('v')) {
            CliUtil.toolSetup("DEBUG");
        } else {
            CliUtil.toolSetup("INFO");
        }

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

        /* upgrade the variable map from the config in force */
        mLog.debug("Updating Default Variable Map");
        updateDefaultVars();

        mLog.debug("Processing Config Overrides");
        overrideDefaultVars(cl);
        
        mLog.debug("Loading virtual host names and domain names");
        mQualifiedVhns = loadReverseProxyVhns();
        
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
            expandTemplate(new File(mTemplateDir,getCoreConfTemplate()), new File(mConfDir,getCoreConf())); /* Only core nginx conf goes to mConfDir, rest to mConfIncludesDir */
            expandTemplate(new File(mTemplateDir,getMainConfTemplate()), new File(mConfIncludesDir,getMainConf()));
            expandTemplate(new File(mTemplateDir,getMemcacheConfTemplate()), new File(mConfIncludesDir,getMemcacheConf()));
            expandTemplate(new File(mTemplateDir,getMailConfTemplate()), new File(mConfIncludesDir,getMailConf()));
            expandTemplate(new File(mTemplateDir,getMailImapConfTemplate()), new File(mConfIncludesDir,getMailImapConf()));
            expandTemplate(new File(mTemplateDir,getMailImapSConfTemplate()), new File(mConfIncludesDir,getMailImapSConf()));
            expandTemplate(new File(mTemplateDir,getMailPop3ConfTemplate()), new File(mConfIncludesDir,getMailPop3Conf()));
            expandTemplate(new File(mTemplateDir,getMailPop3SConfTemplate()), new File(mConfIncludesDir,getMailPop3SConf()));
            expandTemplate(new File(mTemplateDir,getWebConfTemplate()), new File(mConfIncludesDir,getWebConf()));
            expandTemplate(new File(mTemplateDir,getWebHttpConfTemplate()), new File(mConfIncludesDir,getWebHttpConf()));
            expandTemplate(new File(mTemplateDir,getWebHttpSConfTemplate()), new File(mConfIncludesDir,getWebHttpSConf()));
            expandTemplate(new File(mTemplateDir,getWebHttpModeConfTemplate("http")), new File(mConfIncludesDir,getWebHttpModeConf("http")));
            expandTemplate(new File(mTemplateDir,getWebHttpModeConfTemplate("https")), new File(mConfIncludesDir,getWebHttpModeConf("https")));
            expandTemplate(new File(mTemplateDir,getWebHttpModeConfTemplate("both")), new File(mConfIncludesDir,getWebHttpModeConf("both")));
            expandTemplate(new File(mTemplateDir,getWebHttpModeConfTemplate("redirect")), new File(mConfIncludesDir,getWebHttpModeConf("redirect")));
            expandTemplate(new File(mTemplateDir,getWebHttpModeConfTemplate("mixed")), new File(mConfIncludesDir,getWebHttpModeConf("mixed")));
            expandTemplate(new File(mTemplateDir,getWebHttpSModeConfTemplate("http")), new File(mConfIncludesDir,getWebHttpSModeConf("http")));
            expandTemplate(new File(mTemplateDir,getWebHttpSModeConfTemplate("https")), new File(mConfIncludesDir,getWebHttpSModeConf("https")));
            expandTemplate(new File(mTemplateDir,getWebHttpSModeConfTemplate("both")), new File(mConfIncludesDir,getWebHttpSModeConf("both")));
            expandTemplate(new File(mTemplateDir,getWebHttpSModeConfTemplate("redirect")), new File(mConfIncludesDir,getWebHttpSModeConf("redirect")));
            expandTemplate(new File(mTemplateDir,getWebHttpSModeConfTemplate("mixed")), new File(mConfIncludesDir,getWebHttpSModeConf("mixed")));
        } catch (ProxyConfException pe) {
            mLog.error("Error while expanding templates: " + pe.getMessage());
            exitCode = 1;
        } catch (SecurityException se) {
            mLog.error("Error while expanding templates: " + se.getMessage());
            exitCode = 1;
        }
        return (exitCode);
    }

    public static void main(String[] args) throws ServiceException,
        ProxyConfException {
        int exitCode = createConf(args);
        System.exit(exitCode);
    }
}
