/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2018 Synacor, Inc.
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
package com.zimbra.cs.zimlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.service.admin.FlushCache;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.WebClientServiceUtil;
import com.zimbra.cs.zimlet.ZimletPresence.Presence;
import com.zimbra.soap.admin.type.CacheEntryType;

/**
 *
 * @author jylee
 *
 */
public class ZimletUtil {

    public static final String ZIMLET_BASE = "/service/zimlet";
    public static final String ZIMLET_DEV_DIR = "_dev";
    public static final String ZIMLET_ALLOWED_DOMAINS = "allowedDomains";
    public static final String ZIMLET_DEFAULT_COS = "default";
    public static final String PARAM_ZIMLET = "Zimlet";
    public static final String ZIMLET_NAME_REGEX = "^[\\w.-]+$";
    private static final String ZIMLET_CACHE_DIR = "/opt/zimbra/jetty/work/resource-cache/zimletres/latest";

    private static int P_MAX = Integer.MAX_VALUE;
    private static boolean sZimletsLoaded = false;
    private static Map<String,ZimletFile> sZimlets = new HashMap<String,ZimletFile>();

    private static Map<String, Class> sZimletHandlers = new HashMap<String, Class>();

    public static void migrateUserPrefIfNecessary(Account acct) throws ServiceException {
        if (!DebugConfig.enableMigrateUserZimletPrefs) {
            return;
        }

        Set<String> wanted = acct.getMultiAttrSet(Provisioning.A_zimbraPrefZimlets);
        Set<String> unwanted = acct.getMultiAttrSet(Provisioning.A_zimbraPrefDisabledZimlets);

        // needs upgrade only if wanted is not empty and unwanted is empty, because
        // if wanted is empty: means use whatever zimbraZimletAvailableZimlets/zimbraZimletDomainAvailableZimlets says
        // if unwanted is not empty: already migrated or user has change it.
        boolean needsMigrate = (!wanted.isEmpty()) && unwanted.isEmpty();

        if (!needsMigrate) {
            return;
        }

        ZimletPresence availZimlets = getAvailableZimlets(acct);
        Map<String, Object> attrs = new HashMap<String, Object>();
        StringBuilder disabledZimletNamesForLogging = new StringBuilder();

        for (String zimletName : availZimlets.getZimletNames()) {
            Presence presence = availZimlets.getPresence(zimletName);

            if (presence == Presence.enabled && !wanted.contains(zimletName)) {
                disabledZimletNamesForLogging.append(zimletName + ", ");
                StringUtil.addToMultiMap(attrs, Provisioning.A_zimbraPrefDisabledZimlets, zimletName);
            }
        }

        // in the odd case when someone has all available zimlets set in his zimbraPrefZimlets,
        // attrs will be empty => we've got nothing to do.
        if (attrs.isEmpty()) {
            ZimbraLog.account.info("Not migrating zimbraPrefDisabledZimlets for account " + acct.getName() + " because all zimlets are enabled");
            return;
        }

        ZimbraLog.account.info("Migrating zimbraPrefDisabledZimlets for account " + acct.getName() + " to " + disabledZimletNamesForLogging.toString());

        Provisioning prov = Provisioning.getInstance();
        prov.modifyAttrs(acct, attrs);
    }

    public static ZimletPresence getUserZimlets(Account acct) throws ServiceException {
        ZimletPresence userZimlets = getAvailableZimlets(acct);

        // userZimlets now contains all allowed zimlets for the user
        // process user pref, which overrides cos/domain enabled/disabled by default
        // user pref cannot override cos/domain mandatory zimlets

        String[] userPrefEnabledZimlets = acct.getMultiAttr(Provisioning.A_zimbraPrefZimlets);
        for (String zimletName : userPrefEnabledZimlets) {
            Presence presence = userZimlets.getPresence(zimletName);
            if (presence != null &&               // zimlet is allowed
                presence != Presence.mandatory && // can't override mandatory
                presence != Presence.enabled) {   // disabled by default, but user specifically enabled it
                userZimlets.put(zimletName, Presence.enabled);
            }
        }

        String[] userPrefDisabledZimlets = acct.getMultiAttr(Provisioning.A_zimbraPrefDisabledZimlets);
        for (String zimletName : userPrefDisabledZimlets) {
            Presence presence = userZimlets.getPresence(zimletName);
            if (presence != null &&               // zimlet is allowed
                presence != Presence.mandatory && // can't override mandatory
                presence != Presence.disabled) {  // enabled by default, but user specifically disabled it
                userZimlets.put(zimletName, Presence.disabled);
            }
        }

        return userZimlets;
    }

    public static ZimletPresence getAvailableZimlets(Account acct) throws ServiceException {
        ZimletPresence availZimlets = new ZimletPresence();

        // process domain settings first, because if domain and cos conflict we honor the cos setting
        Domain domain = Provisioning.getInstance().getDomain(acct);
        if (domain != null) {
            String[] domainZimlets = domain.getMultiAttr(Provisioning.A_zimbraZimletDomainAvailableZimlets);
            for (String zimletWithPrefix : domainZimlets) {
                if (!isZextrasZimlet(zimletWithPrefix)) {
                    availZimlets.put(zimletWithPrefix);
                }
            }
        }

        String[] acctCosZimlets = acct.getMultiAttr(Provisioning.A_zimbraZimletAvailableZimlets);
        for (String zimletWithPrefix : acctCosZimlets) {
            if (!isZextrasZimlet(zimletWithPrefix)) {
                availZimlets.put(zimletWithPrefix);
            }
        }

        return availZimlets;
    }
    
    /**
     * this method is also check whether zimletName is having prefix like +,- and !
     * and this method is used to find out whether zimletName is related to remove
     * ZEXTRAS list or not if zimletName is not in remove ZEXTRAS list then it
     * returns true else it returns false
     *
     * @param zimletName
     * @return boolean
     */
    private static boolean isZextrasZimlet(String zimletName) {
        if (!StringUtil.isNullOrEmpty(zimletName) && zimletName.length() > 1) {
            char prefix = zimletName.charAt(0);
            Presence presence = Presence.fromPrefix(prefix);

            if (presence != null) {
                zimletName = zimletName.substring(1);
            }

            if (AdminConstants.ZEXTRAS_PACKAGES_LIST.contains(zimletName)) {
                return true;
            }
        }
        return false;
    }

    public static ZimletPresence getAvailableZimlets(Cos cos) throws ServiceException {
        ZimletPresence availZimlets = new ZimletPresence();

        String[] acctCosZimlets = cos.getMultiAttr(Provisioning.A_zimbraZimletAvailableZimlets);
        for (String zimletWithPrefix : acctCosZimlets) {
            if (!isZextrasZimlet(zimletWithPrefix)) {
                availZimlets.put(zimletWithPrefix);
            }
        }

        return availZimlets;
    }

    public static String[] listZimletNames() {
        String[] zimlets = sZimlets.keySet().toArray(new String[0]);
        Arrays.sort(zimlets);
        return zimlets;
    }

    public static String[] listDevZimletNames() {
        String[] zimlets = loadDevZimlets().keySet().toArray(new String[0]);
        Arrays.sort(zimlets); // TODO: Should sort by zimlet priority.
        return zimlets;
    }

    public static List<Zimlet> orderZimletsByPriority(List<Zimlet> zimlets) {
        // create a sortable collection, sort, then return List<Zimlet> in the
        // sorted order.  version is not comparable in String format.
        List<Pair<Version,Zimlet>> plist = new ArrayList<Pair<Version,Zimlet>>();
        for (Zimlet z : zimlets) {
            String pstring = z.getPriority();
            if (pstring == null) {
                // no priority.  put it at the end of priority list
                pstring = Integer.toString(Integer.MAX_VALUE);
            }
            Version v = new Version(pstring);
            plist.add(new Pair<Version,Zimlet>(v,z));
        }
        Collections.sort(plist,
            new Comparator<Pair<Version,Zimlet>>() {
                @Override
                public int compare(Pair<Version,Zimlet> first,
                                    Pair<Version,Zimlet> second) {
                    return first.getFirst().compareTo(second.getFirst());
                }
            }
        );

        List<Zimlet> ret = new ArrayList<Zimlet>();
        for (Pair<Version,Zimlet> p : plist) {
            ret.add(p.getSecond());
        }
        return ret;
    }

    public static List<Zimlet> orderZimletsByPriority(String[] zimlets) {
        Provisioning prov = Provisioning.getInstance();
        List<Zimlet> zlist = new ArrayList<Zimlet>();
        for (int i = 0; i < zimlets.length; i++) {
            try {
                Zimlet z = prov.getZimlet(zimlets[i]);
                if (z != null) {
                    zlist.add(z);
                }
            } catch (ServiceException se) {
                // ignore error and continue on
                ZimbraLog.zimlet.warn("unable to get zimlet " + zimlets[i], se);
            }
        }
        return orderZimletsByPriority(zlist);
    }

    public static List<Zimlet> orderZimletsByPriority() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        List<Zimlet> allzimlets = prov.listAllZimlets();
        return orderZimletsByPriority(allzimlets);
    }

    public static void updateZimletConfig(String zimlet, String config) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Zimlet zim = prov.getZimlet(zimlet);
        if (zim == null) {
            throw AccountServiceException.NO_SUCH_ZIMLET(zimlet);
        }
        Map<String, String> map = new HashMap<String, String>();
        map.put(Provisioning.A_zimbraZimletHandlerConfig, config);
        prov.modifyAttrs(zim, map);
    }

    public static ZimletConfig getZimletConfig(String zimlet) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Zimlet z = prov.getZimlet(zimlet);
        if (z == null) {
            throw AccountServiceException.NO_SUCH_ZIMLET(zimlet);
        }
        String cf = z.getAttr(Provisioning.A_zimbraZimletHandlerConfig);
        if (cf == null) {
            return null;
        }
        try {
            return new ZimletConfig(cf);
        } catch (ZimletException e) {
            ZimbraLog.zimlet.warn("Unable to load zimlet config for "+z.getName(), e);
        }
        return null;
    }

    /**
     * Loads all the Zimlets, locates the server side ZimletHandler for each Zimlets,
     * loads the class and instantiate the object, then returns the instance.
     *
     * @param name of the Zimlet
     * @return ZimletHandler object
     */
    public static ZimletHandler getHandler(String name) {
        loadZimlets();
        Class zh = sZimletHandlers.get(name);
        if (zh == null) {
            ZimletFile zf = sZimlets.get(name);
            if (zf == null) {
                return null;
            }
            URLClassLoader cl = null;
            try {
                String clazz = zf.getZimletDescription().getServerExtensionClass();
                if (clazz != null) {
                    URL[] urls = { zf.toURL() };
                    cl = new URLClassLoader(urls, ZimletUtil.class.getClassLoader());
                    zh = cl.loadClass(clazz);
                    ZimbraLog.zimlet.info("Loaded class "+zh.getName());
                    sZimletHandlers.put(name, zh);
                }
            } catch (Exception e) {
                ZimbraLog.zimlet.warn("Unable to load zimlet handler for %s", name, e);
                return null;
            } finally {
                if (cl != null) {
                    try {
                        cl.close();
                    } catch (IOException e) {
                        ZimbraLog.zimlet.warn("failed to close URLClassLoader", e);
                    }
                }
            }
        }
        try {
            if (zh != null) {
                return (ZimletHandler) zh.newInstance();
            }
        } catch (Exception e) {
            ZimbraLog.zimlet.warn("Unable to instantiate zimlet handler for "+name, e);
        }
        return null;
    }

    public static ZimletFile getZimlet(String zimlet) {
        ZimletFile zf = null;
        loadZimlets();
        zf = sZimlets.get(zimlet);
        if (zf != null) {
            return zf;
        }
        return loadDevZimlets().get(zimlet);
    }

    /**
     *
     * Load all the installed Zimlets.
     *
     */
    public synchronized static Map<String,ZimletFile> loadZimlets() {
        if (!sZimletsLoaded) {
            loadZimletsFromDir(sZimlets, LC.zimlet_directory.value());
            sZimletsLoaded = true;
        }
        // NOTE: Was added for consistency with loadDevZimlets
        return sZimlets;
    }

    /**
     *
     * Load all the Zimlets in the dev test directory.
     *
     */
    public static Map<String,ZimletFile> loadDevZimlets() {
        Map<String,ZimletFile> zimletMap = new HashMap<String,ZimletFile>();
        loadZimletsFromDir(zimletMap, LC.zimlet_directory.value() + File.separator + ZIMLET_DEV_DIR);
        return zimletMap;
    }

    /**
     *
     * Throw away the cached Zimlet, and reloads from the file system.
     *
     * @param zimlet
     * @throws ZimletException
     */
    public static void reloadZimlet(String zimlet) throws ZimletException {
        ZimletFile zf;
        try {
            zf = new ZimletFile(LC.zimlet_directory.value() + File.separator + zimlet);
        } catch (IOException ioe) {
            ZimbraLog.zimlet.warn(ioe.getMessage());
            return;
        }
        synchronized (sZimlets) {
            sZimlets.remove(zimlet);
            sZimlets.put(zimlet, zf);
        }
    }

    /**
     *
     * Load all the Zimlets found in the directory.
     *
     * @param zimlets - Zimlet cache
     * @param dir - directory
     */
    private static void loadZimletsFromDir(Map<String,ZimletFile> zimlets, String dir) {
        File zimletRootDir = new File(dir);
        if (zimletRootDir == null || !zimletRootDir.exists() || !zimletRootDir.isDirectory()) {
            return;
        }

        ZimbraLog.zimlet.debug("Loading zimlets from " + zimletRootDir.getPath());

        synchronized (zimlets) {
            zimlets.clear();
            String[] zimletNames = zimletRootDir.list();
            assert(zimletNames != null);
            for (int i = 0; i < zimletNames.length; i++) {
                if (zimletNames[i].equals(ZIMLET_DEV_DIR)) {
                    continue;
                }
                try {
                    if (!isZextrasZimlet(zimletNames[i])) {
                        zimlets.put(zimletNames[i], new ZimletFile(zimletRootDir, zimletNames[i]));
                    }
                } catch (Exception e) {
                    ZimbraLog.zimlet.warn("error loading zimlet "+zimletNames[i], e);
                }
            }
        }
    }

    /**
     *
     * List the Zimlet description as XML or JSON Element.
     *
     * @param elem - Parent Element node
     * @param zimlet
     */
    public static void listZimlet(Element elem, Zimlet zimlet, int priority, Presence presence) {
        loadZimlets();
        ZimletFile zf = sZimlets.get(zimlet.getName());
        if (zf == null) {
            ZimbraLog.zimlet.warn("cannot find zimlet "+zimlet.getName());
            return;
        }
        String zimletBase = ZIMLET_BASE + "/" + zimlet.getName() + "/";
        Element entry = elem.addElement(AccountConstants.E_ZIMLET);
        Element zimletContext = entry.addElement(AccountConstants.E_ZIMLET_CONTEXT);
        zimletContext.addAttribute(AccountConstants.A_ZIMLET_BASE_URL, zimletBase);
        if (priority >= 0) {
            zimletContext.addAttribute(AccountConstants.A_ZIMLET_PRIORITY, priority);
        }

        if (presence == null) {
            presence = Presence.enabled;
        }
        zimletContext.addAttribute(AccountConstants.A_ZIMLET_PRESENCE, presence.toString());

        try {
            zf.getZimletDescription().addToElement(entry);
            String config = zimlet.getHandlerConfig();
            if (config != null) {
                entry.addElement(W3cDomUtil.parseXML(config, elem.getFactory()));
            }
        } catch (Exception e) {
            ZimbraLog.zimlet.warn("error loading zimlet "+zimlet, e);
        }
    }

    /**
     *
     * List the description of all the dev test Zimlets as Element.
     *
     * @param elem - Parent element node
     */
    public static void listDevZimlets(Element elem) {
        for (ZimletFile zim : loadDevZimlets().values()) {
            String zimletBase = ZIMLET_BASE + "/" + ZIMLET_DEV_DIR + "/" + zim.getZimletName() + "/";
            Element entry = elem.addElement(AccountConstants.E_ZIMLET);
            Element zimletContext = entry.addElement(AccountConstants.E_ZIMLET_CONTEXT);
            zimletContext.addAttribute(AccountConstants.A_ZIMLET_BASE_URL, zimletBase);

            // dev zimlets are all enabled
            zimletContext.addAttribute(AccountConstants.A_ZIMLET_PRESENCE, Presence.enabled.toString());

            try {
                zim.getZimletDescription().addToElement(entry);
                if (zim.hasZimletConfig()) {
                    zim.getZimletConfig().addToElement(entry);
                }
            } catch (Exception e) {
                ZimbraLog.zimlet.warn("error loading dev zimlet: "+zim.getName(), e);
            }
        }
    }

    private static Map<String,Object> descToMap(ZimletDescription zd) throws ZimletException {
        Map<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletKeyword,         zd.getServerExtensionKeyword());
        attrs.put(Provisioning.A_zimbraZimletVersion,         zd.getVersion().toString());
        attrs.put(Provisioning.A_zimbraZimletDescription,     zd.getDescription());
        if (!StringUtil.isNullOrEmpty(zd.getZimbraXCompatibleSemVer())) {
            attrs.put(Provisioning.A_zimbraXZimletCompatibleSemVer,zd.getZimbraXCompatibleSemVer());
        }
        attrs.put(Provisioning.A_zimbraZimletHandlerClass,    zd.getServerExtensionClass());
        attrs.put(Provisioning.A_zimbraZimletServerIndexRegex, zd.getRegexString());
        return attrs;
    }

    public static File getZimletRootDir(String zimletName) throws ZimletException {
        if(zimletName.matches(ZIMLET_NAME_REGEX)) {
            return new File(LC.zimlet_directory.value(), zimletName);
        } else {
            throw ZimletException.INVALID_ZIMLET_NAME();
        }
    }

    public static void flushCache() throws ZimletException {
        sZimletsLoaded = false;
        try {
            Provisioning.getInstance().flushCache(CacheEntryType.zimlet, null);
        } catch (ServiceException e) {
            throw ZimletException.CANNOT_FLUSH_CACHE(e);
        }
    }

    public static void flushDiskCache(Map<String, Object> context) throws ServiceException {
        if (WebClientServiceUtil.isServerInSplitMode()) {
            List<Server> allServers = Provisioning.getInstance().getAllServers();
            for (Server server : allServers) {
                if (server.hasMailClientService()) {
                    FlushCache.flushAllZimlets(context);
                } else {
                    WebClientServiceUtil.sendFlushZimletRequestToUiNode(server);
                }
            }
        }
    }

    public static void flushAllZimletsCache() {
        try {
            File file = new File(ZIMLET_CACHE_DIR);
            FileUtil.deleteDirContents(file);
        } catch (IOException e) {
            ZimbraLog.zimlet.warn("failed to flush zimlet cache", e);
        }
    }

    public static interface DeployListener {
        public void markFinished(Server s);
        public void markFailed(Server s, Exception e);
    }

    public static void deployZimletLocally(ZimletFile zf, DeployListener listener) throws IOException, ZimletException, ServiceException {
        Server localServer = Provisioning.getInstance().getLocalServer();
        deployZimletLocally(zf);
        if (listener != null) {
            listener.markFinished(localServer);
        }
    }

    enum Action { INSTALL, UPGRADE, REPAIR };

    /**
     * Deploys the specified Zimlet on local server, be it service node or ui node. The following actions are taken.
     * 1.  Install the Zimlet files on the machine.
     * 2.  Check the LDAP for the Zimlet entry.  If the entry already exists, stop.
     * 3.  Install the LDAP entry for the Zimlet.
     * 4.  Install Zimlet config.
     * 5.  Activate the zimlet on default COS.
     * 6.  Enable the Zimlet.
     *
     * @param zimlet
     * @throws IOException
     * @throws ZimletException
     */
    public static void deployZimletLocally(ZimletFile zf) throws IOException, ZimletException, ServiceException {
        Provisioning prov = Provisioning.getInstance();
        String zimletName = zf.getZimletName();
        ZimletDescription zd = zf.getZimletDescription();
        Zimlet z;
        Action action = Action.INSTALL;
        String priority = null;
        boolean enable = true;

        // check if the zimlet already exists in LDAP.
        z = prov.getZimlet(zimletName);

        if (z != null) {
            Version ver = new Version(z.getAttr(Provisioning.A_zimbraZimletVersion));
            if (zd.getVersion().compareTo(ver) < 0) {
                ZimbraLog.zimlet.info("Zimlet " + zimletName + " being installed is of an older version.");
            }
            if (zd.getVersion().compareTo(ver) == 0) {
                action = Action.REPAIR;
            } else {
                action = Action.UPGRADE;
            }
            // save priority
            priority = z.getPriority();
            enable = z.isEnabled();
        }

        // update LDAP
        z = ldapDeploy(zf);

        // install files
        installZimletLocally(zf);

        if (action == Action.REPAIR) {
            return;
        }

        // upgrade
        ZimbraLog.zimlet.info("Upgrading Zimlet " + zimletName + " to " +zd.getVersion().toString());

        // set the priority
        if (priority == null) {
            setPriority(zimletName, P_MAX);
        }

        // install the config
        if (zf.hasZimletConfig()) {
            ZimletConfig zc = getZimletConfig(zimletName);
            if (zc != null) {
                Version zimletConfigVersion = new Version (zf.getZimletConfig().getVersion().toString());
                Version zimletConfigVersionInLDAP = new Version (zc.getVersion().toString());
                if (zimletConfigVersion.compareTo(zimletConfigVersionInLDAP)> 0) {
                    installConfig(zf.getZimletConfig());
                }
            }
            else {
                installConfig(zf.getZimletConfig());
            }
        }

        // activate
        if (!zd.isExtension()) {
            activateZimlet(zimletName, ZIMLET_DEFAULT_COS);
        }

        if (!enable) {
            // it was an upgrade of previously disabled zimlet.  leave it alone.
            return;
        }

        // enable
        enableZimlet(zimletName);
    }

    /**
     *
     * Install the Zimlet file on this machine.
     *
     * @param zimlet
     * @return
     * @throws IOException
     * @throws ZimletException
     */
    public static void installZimletLocally(ZimletFile zf) throws IOException, ZimletException {
        ZimletDescription zd = zf.getZimletDescription();
        String zimletName = zd.getName();
        ZimbraLog.zimlet.info("Installing Zimlet " + zimletName + " on this host.");

        // location for the jar files
        File libDir = new File(LC.mailboxd_directory.value() + File.separator +
                                    "webapps" + File.separator +
                                    "zimlet" + File.separator +
                                    "WEB-INF" + File.separator +
                                    "lib");
        // location for the rest of the files
        File zimlet = getZimletRootDir(zimletName);
        String zimletsRootPath = new File(LC.zimlet_directory.value()).getCanonicalPath();
        zimlet.getParentFile().mkdirs();
        if (zimlet.exists()) {
            deleteFile(zimlet);
        }

        for (ZimletFile.ZimletEntry entry : zf.getAllEntries()) {
            String fname = entry.getName();
            File file;
            if (fname.endsWith(".jar")) {
                file = new File(libDir, fname);
                if(!file.getCanonicalPath().startsWith(libDir.getCanonicalPath())) {
                    ZimbraLog.zimlet.error(String.format("Zimlet %s has an invalid file path %s", zimletName, fname));
                    throw ZimletException.CANNOT_DEPLOY(zimletName, "Invalid file path " + fname, null);
                }
            } else {
                file = new File(zimlet, fname);
                if(!file.getCanonicalPath().startsWith(zimletsRootPath)) {
                    ZimbraLog.zimlet.error(String.format("Zimlet %s has an invalid file path %s", zimletName, fname));
                    throw ZimletException.CANNOT_DEPLOY(zimletName, "Invalid file path " + fname, null);
                }
            }

            file.getParentFile().mkdirs();
            writeFile(entry.getContents(), file);
        }

        flushCache();
    }

    /**
     *
     * Deploy the Zimlet to LDAP.
     *
     * @param zimlet
     * @throws IOException
     * @throws ZimletException
     */
    public static void ldapDeploy(String zimlet) throws ServiceException, IOException, ZimletException {
        String zimletRoot = LC.zimlet_directory.value();
        ZimletFile zf = new ZimletFile(zimletRoot + File.separator + zimlet);
        ldapDeploy(zf);
    }

    public static Zimlet ldapDeploy(ZimletFile zf) throws ServiceException, IOException, ZimletException {
        ZimletDescription zd = zf.getZimletDescription();
        String zimletName = zd.getName();
        Map<String,Object> attrs = descToMap(zd);
        List <String> targets = zd.getTargets();
        if (targets != null && targets.size() > 0) {
            attrs.put(Provisioning.A_zimbraZimletTarget, targets);
        }

        String disableZimletUndeploy = zd.getDisableUIUndeploy();
        if (disableZimletUndeploy != null && disableZimletUndeploy.equalsIgnoreCase("true")) {
            attrs.put(Provisioning.A_zimbraAdminExtDisableUIUndeploy, ProvisioningConstants.TRUE);
        }

        if (zd.isExtension()) {
            attrs.put(Provisioning.A_zimbraZimletIsExtension, ProvisioningConstants.TRUE);
        } else {
            attrs.put(Provisioning.A_zimbraZimletIsExtension, ProvisioningConstants.FALSE);
        }

        ZimbraLog.zimlet.info("Deploying Zimlet " + zimletName + " in LDAP.");

        // add zimlet entry to ldap
        Provisioning prov = Provisioning.getInstance();
        Zimlet zim = prov.getZimlet(zimletName);
        if (zim == null) {
            zim = prov.createZimlet(zimletName, attrs);
        } else {
            prov.modifyAttrs(zim, attrs);
        }
        return zim;
    }

    private static void writeFile(byte[] src, File dest) throws IOException {
        dest.createNewFile();
        ByteArrayInputStream bais = new ByteArrayInputStream(src);
        FileOutputStream fos = new FileOutputStream(dest);
        ByteUtil.copy(bais, true, fos, true);
    }

    private static void deleteFile(File f) {
        if (f.isDirectory()) {
            for (File sub : f.listFiles()) {
                deleteFile(sub);
            }
        }
        f.delete();
    }

    /**
     *
     * Delete the Zimlet from LDAP and remove the associated Zimlet files.
     *
     * @param zimlet
     * @throws ZimletException
     */
    public static void undeployZimletLocally(String zimlet) throws ServiceException {
        ZimbraLog.zimlet.info("Uninstalling Zimlet " + zimlet + " from LDAP.");
        Provisioning prov = Provisioning.getInstance();
        Zimlet z = prov.getZimlet(zimlet);
        if (z != null) {
            List<Cos> cos = prov.getAllCos();
            for (Cos c : cos) {
                try {
                    deactivateZimlet(zimlet, c.getName());
                } catch (Exception e) {
                    ZimbraLog.zimlet.warn("Error deactiving Zimlet " + zimlet + " in LDAP.", e);
                }
            }
            try {
                prov.deleteZimlet(zimlet);
            } catch (ServiceException se) {
                z = prov.getZimlet(zimlet);
                if (z != null) {
                    ZimbraLog.zimlet.warn("Error deleting Zimlet " + zimlet + " in LDAP.", se);
                }
            }
        }

        ZimletFile zf = sZimlets.get(zimlet);
        if (zf != null) {
            sZimlets.remove(zimlet);
        }
        ZimbraLog.zimlet.info("undeploying zimlet %s", zimlet);
        try {
            File zimletDir = ZimletUtil.getZimletRootDir(zimlet);
            FileUtil.deleteDir(zimletDir);
            ZimbraLog.zimlet.info("zimlet directory %s is deleted", zimletDir.getName());
        } catch (IOException e) {
            throw ServiceException.FAILURE("error occurred when deleting zimlet directory", e);
        } catch (ZimletException e) {
            throw ServiceException.FAILURE("error occurred when deleting zimlet directory", e);
        }
    }

    /**
     *
     * Add the Zimlet to specified COS.
     *
     * @param zimlet
     * @param cos
     * @throws ZimletException
     */
    public static void activateZimlet(String zimlet, String cos) throws ServiceException, ZimletException {
        ZimbraLog.zimlet.info("Adding Zimlet " + zimlet + " to COS " + cos);
        Provisioning prov = Provisioning.getInstance();
        Cos c = prov.get(Key.CosBy.name, cos);
        if (c == null) {
            throw ZimletException.CANNOT_ACTIVATE("no such cos " + cos, null);
        }
        Map<String,Object> attrs = new HashMap<String, Object>();
        attrs.put("+"+Provisioning.A_zimbraZimletAvailableZimlets, zimlet);
        prov.modifyAttrs(c, attrs);
        ZimletConfig zc = getZimletConfig(zimlet);
        if (zc == null) {
            return;
        }
        String allowedDomains = zc.getConfigValue(ZIMLET_ALLOWED_DOMAINS);
        if (allowedDomains != null) {
            addAllowedDomains(allowedDomains, cos);
        }
    }

    /**
     *
     * Remove the Zimlet from COS.
     *
     * @param zimlet
     * @param cos
     * @throws ZimletException
     */
    public static void deactivateZimlet(String zimlet, String cos) throws ServiceException, ZimletException {
        ZimbraLog.zimlet.info("Removing Zimlet " + zimlet + " from COS " + cos);
        Provisioning prov = Provisioning.getInstance();
        Cos c = prov.get(Key.CosBy.name, cos);
        if (c == null) {
            throw ZimletException.CANNOT_DEACTIVATE("no such cos " + cos, null);
        }
        Map<String,Object> attrs = new HashMap<String, Object>();
        attrs.put("-"+Provisioning.A_zimbraZimletAvailableZimlets, zimlet);
        prov.modifyAttrs(c, attrs);
        ZimletConfig zc = getZimletConfig(zimlet);
        if (zc == null) {
            return;
        }
        String domains = zc.getConfigValue(ZIMLET_ALLOWED_DOMAINS);
        if (domains == null) {
            return;
        }
        String[] domainArray = domains.toLowerCase().split(",");
        Set<String> domainsToRemove = new HashSet<String>();
        for (String d : domainArray) {
            domainsToRemove.add(d);
        }
        String[] zimlets = getAvailableZimlets(c).getZimletNamesAsArray();
        for (String z : zimlets) {
            if (z.equals(zimlet)) {
                continue;
            }
            zc = getZimletConfig(z);
            if (zc == null) {
                continue;
            }
            domains = zc.getConfigValue(ZIMLET_ALLOWED_DOMAINS);
            if (domains == null) {
                continue;
            }
            domainArray = domains.toLowerCase().split(",");
            for (String d : domainArray) {
                domainsToRemove.remove(d);
            }
        }
        if (!domainsToRemove.isEmpty()) {
            removeAllowedDomains(domainsToRemove, cos);
        }
    }

    /**
     *
     * Change the enabled status of the Zimlet.
     *
     * @param zimlet
     * @param enabled
     * @throws ZimletException
     */
    public static void setZimletEnable(String zimlet, boolean enabled) throws ZimletException {
        Provisioning prov = Provisioning.getInstance();
        try {
            Zimlet z = prov.getZimlet(zimlet);
            if (z == null) {
                throw AccountServiceException.NO_SUCH_ZIMLET(zimlet);
            }
            Map<String,String> attr = new HashMap<String,String>();
            attr.put(Provisioning.A_zimbraZimletEnabled, enabled ? ProvisioningConstants.TRUE : ProvisioningConstants.FALSE);
            prov.modifyAttrs(z, attr);
        } catch (Exception e) {
            if (enabled) {
                throw ZimletException.CANNOT_ENABLE(zimlet, e);
            } else {
                throw ZimletException.CANNOT_DISABLE(zimlet, e);
            }
        }
    }

    /**
     *
     * Enable the Zimlet.  Only the enabled Zimlets are available to the users.
     *
     * @param zimlet
     * @throws ZimletException
     */
    public static void enableZimlet(String zimlet) throws ZimletException {
        ZimbraLog.zimlet.info("Enabling Zimlet " + zimlet);
        setZimletEnable(zimlet, true);
    }

    /**
     *
     * Disable the Zimlet.  Disabled Zimlets are not available to the users.
     *
     * @param zimlet
     * @throws ZimletException
     */
    public static void disableZimlet(String zimlet) throws ZimletException {
        ZimbraLog.zimlet.info("Disabling Zimlet " + zimlet);
        setZimletEnable(zimlet, false);
    }

    /**
     *
     * Change the Zimlet COS ACL.
     *
     * @param zimlet
     * @param args
     * @throws ZimletException
     */
    public static void aclZimlet(String zimlet, String[] args) throws ServiceException, ZimletException {
        for (int i = 2; i < args.length; i+=2) {
            String cos = args[i];
            String action = args[i+1].toLowerCase();
            if (action.equals("grant")) {
                activateZimlet(zimlet, cos);
            } else if (action.equals("deny")) {
                deactivateZimlet(zimlet, cos);
            } else {
                throw ZimletException.ZIMLET_HANDLER_ERROR("invalid acl command "+args[i+1]);
            }
        }
    }

    /**
     *
     * List all the COS the Zimlet is available to.
     *
     * @param zimlet
     * @throws ZimletException
     */
    public static void listAcls(String zimlet) throws ServiceException, ZimletException {
        System.out.println("Listing COS entries for Zimlet "+zimlet+"...");
        Provisioning prov = Provisioning.getInstance();
        for (Cos cos : prov.getAllCos()) {
            String[] zimlets = getAvailableZimlets(cos).getZimletNamesAsArray();
            for (int i = 0; i < zimlets.length; i++) {
                if (zimlets[i].equals(zimlet)) {
                    System.out.println("\t"+cos.getName());
                    break;
                }
            }
        }
    }

    /**
     *
     * Print all the Zimlets installed on this host.
     *
     * @throws ZimletException
     */
    public static void listInstalledZimletsOnHost(boolean everything) {
        loadZimlets();
        ZimletFile[] zimlets = sZimlets.values().toArray(new ZimletFile[0]);
        Arrays.sort(zimlets);
        for (int i = 0; i < zimlets.length; i++) {
            ZimletDescription zd;
            System.out.print("\t"+zimlets[i].getZimletName());
            try {
                zd = zimlets[i].getZimletDescription();
                boolean isExtension = (zd != null && zd.isExtension());
                if (isExtension && everything) {
                    System.out.print(" (ext)");
                }
            } catch (Exception e) {
                ZimbraLog.zimlet.warn("error reading zimlet : "+zimlets[i].getName(), e);
            }
            System.out.println();
        }
    }

    /**
     *
     * Print all the Zimlets on LDAP.
     *
     * @throws ZimletException
     */
    public static void listInstalledZimletsInLdap(boolean everything) throws ServiceException, ZimletException {
        Provisioning prov = Provisioning.getInstance();
        for (Zimlet z : prov.listAllZimlets()) {
            boolean isExtension = z.isExtension();
            if (!everything && isExtension) {
                continue;
            }
            System.out.print("\t"+z.getName());
            if (!z.isEnabled()) {
                System.out.print(" (disabled)");
            }
            if (isExtension) {
                System.out.print(" (ext)");
            }
            System.out.println();
        }
    }

    /**
     *
     * Print the Zimlet COS ACL for all the Zimlets.
     *
     * @throws ZimletException
     */
    public static void listZimletsInCos() throws ServiceException, ZimletException {
        Provisioning prov = Provisioning.getInstance();
        for (Cos cos : prov.getAllCos()) {
            System.out.println("  "+cos.getName()+":");
            String[] zimlets = getAvailableZimlets(cos).getZimletNamesAsArray();
            Arrays.sort(zimlets);
            for (int i = 0; i < zimlets.length; i++) {
                System.out.println("\t"+zimlets[i]);
            }
        }
    }

    /**
     *
     * Print all the Zimlets installed on the host, on LDAP, and COS ACL.
     *
     * @throws ZimletException
     */
    public static void listAllZimlets(boolean everything) throws ServiceException, ZimletException {
        System.out.println("Installed Zimlet files on this host:");
        listInstalledZimletsOnHost(everything);
        System.out.println("Installed Zimlets in LDAP:");
        listInstalledZimletsInLdap(everything);
        System.out.println("Available Zimlets in COS:");
        listZimletsInCos();
    }

    /**
     *
     * Dump the config template for the Zimlet.
     *
     * @param zimlet
     * @throws IOException
     * @throws ZimletException
     */
    public static void dumpConfig(String zimlet) throws IOException, ZimletException {
        ZimletFile zf = new ZimletFile(zimlet);
        String config = zf.getZimletConfigString();
        System.out.println(config);
    }

    /**
     *
     * Install the Zimlet configuration.
     *
     * @param config
     * @throws IOException
     * @throws ZimletException
     */
    private static void installConfig(ZimletConfig zc) throws ServiceException, IOException, ZimletException {
        String zimletName = zc.getName();
        ZimbraLog.zimlet.info("Installing Zimlet config for " + zimletName);
        String configString = zc.toXMLString();
        updateZimletConfig(zimletName, configString);
        String allowedDomains = zc.getConfigValue(ZIMLET_ALLOWED_DOMAINS);
        if (allowedDomains != null) {
            addAllowedDomains(allowedDomains, "default");  // XXX to default cos for now
        }
    }

    public static void installConfig(String config) throws ServiceException, IOException, ZimletException {
        installConfig(new ZimletConfig(config));
    }

    public static void addAllowedDomains(String domains, String cosName) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Cos cos = prov.get(Key.CosBy.name, cosName);
        Set<String> domainSet = cos.getMultiAttrSet(Provisioning.A_zimbraProxyAllowedDomains);
        String[] domainArray = domains.toLowerCase().split(",");
        for (int i = 0; i < domainArray.length; i++) {
            domainSet.add(domainArray[i]);
        }
        Map<String, String[]> newlist = new HashMap<String, String[]>();
        newlist.put(Provisioning.A_zimbraProxyAllowedDomains, domainSet.toArray(new String[0]));
        prov.modifyAttrs(cos, newlist);
    }

    public static void removeAllowedDomains(Set<String> domains, String cosName) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Cos cos = prov.get(Key.CosBy.name, cosName);
        Set<String> domainSet = cos.getMultiAttrSet(Provisioning.A_zimbraProxyAllowedDomains);
        String[] domainArray = domains.toArray(new String[0]);
        for (int i = 0; i < domainArray.length; i++) {
            domainSet.remove(domainArray[i]);
        }
        Map<String, String[]> newlist = new HashMap<String, String[]>();
        newlist.put(Provisioning.A_zimbraProxyAllowedDomains, domainSet.toArray(new String[0]));
        prov.modifyAttrs(cos, newlist);
    }

    public static void listPriority() throws ServiceException {
        List<Zimlet> plist = orderZimletsByPriority();
        System.out.println("Pri\tZimlet");
        for (int i = 0; i < plist.size(); i++) {
            System.out.println(i + "\t" + plist.get(i).getName());
        }
    }

    public static void setPriority(String zimlet, int priority) throws ServiceException {
        List<Zimlet> plist = orderZimletsByPriority();
        Provisioning prov = Provisioning.getInstance();
        Zimlet z = prov.getZimlet(zimlet);
        if (z == null) {
            throw AccountServiceException.NO_SUCH_ZIMLET(zimlet);
        }
        setPriority(z, priority, plist);
    }


    private static void setPriority(Zimlet zimlet, String priority) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Map<String,String> attr = new HashMap<String,String>();
        attr.put(Provisioning.A_zimbraZimletPriority, priority);
        prov.modifyAttrs(zimlet, attr);
    }

    public static void setPriority(Zimlet z, int priority, List<Zimlet> plist) throws ServiceException {
        // remove self first
        // XXX LdapEntry.equals() is not implemented
        for (Zimlet zim : plist) {
            if (zim.compareTo(z) == 0) {
                plist.remove(zim);
                break;
            }
        }

        if (priority == P_MAX) {
            priority = plist.size();
        }
        Version newPriority;
        if (priority == 0) {
            newPriority = new Version("0");
            setPriority(z, newPriority.toString());
            plist.add(0, z);
            if (plist.size() > 1) {
                // make sure the previous p0 zimlet is now p1.
                Zimlet p0zimlet = plist.get(1);
                setPriority(p0zimlet, 1, plist);
            }
        } else {
            // take the priority of previous zimlet
            Zimlet oneAbove = plist.get(priority-1);
            String pString = oneAbove.getPriority();
            if (pString == null) {
                // priority is mandatory now, but it could be from old version
                // when we didn't have priorities.
                pString = Integer.toString(priority);
            }
            newPriority = new Version(pString);
            if (priority < plist.size()) {
                // increment, while staying before the next zimlet
                Zimlet oneBelow = plist.get(priority);
                pString = oneBelow.getPriority();
                if (pString == null) {
                    pString = Integer.toString(priority+2);
                }
                Version nextPriority = new Version(pString);
                if (newPriority.compareTo(nextPriority) < 0) {
                    newPriority.increment(nextPriority);
                } else {
                    // it really is an error because priorities of two zimlets
                    // shouldn't be the same.  bump the next one down
                    newPriority.increment();
                    setPriority(z, newPriority.toString());
                    plist.add(priority, z);
                    setPriority(oneBelow, priority + 1, plist);
                    return;
                }
            } else {
                // simply increment from the previous priority
                newPriority.increment();
            }
            setPriority(z, newPriority.toString());
        }

        try {
            flushCache();
        }
        catch (Exception e) {
            Throwable t = e.getCause();
            if (t instanceof ServiceException) {
                throw (ServiceException)t;
            }
            if (e instanceof IOException) {
                t = e;
            }
            throw ServiceException.FAILURE("unable to set priority", t);
        }
    }

    /**
     * deploy zimlet to a node by providing adminURL and uploadURL or deploy to all remote nodes
     */
    private static void deployZimletRemotely(String zimletFile, String adminURL, String uploadURL, boolean synchronous)
            throws ServiceException, IOException {
        File zf = new File(zimletFile);
        if (adminURL != null && uploadURL != null) {
            ZimletSoapUtil soapUtil = new ZimletSoapUtil(adminURL, uploadURL, null, null);
            soapUtil.deployZimletOnServer(zf.getName(), ByteUtil.getContent(zf), true);
        } else {
            //deploy to all servers except local
            ZimletSoapUtil soapUtil = null;
            List<Server> allServers = Provisioning.getInstance().getAllServers();
            for (Server server : allServers) {
                if (!server.isLocalServer() && canDeployZimlet(server)) {
                    if (soapUtil == null) {
                        soapUtil = new ZimletSoapUtil();
                        soapUtil.mSynchronous = synchronous;
                    }
                    soapUtil.deployZimletRemotely(server, zf.getName(), ByteUtil.getContent(zf), null, true);
                }
            }
        }
    }

    private static void undeployZimletRemotely(String zimlet) throws ServiceException, IOException {
        ZimletSoapUtil soapUtil = null;
        List<Server> allServers = Provisioning.getInstance().getAllServers();
        for (Server server : allServers) {
            if (!server.isLocalServer() && canDeployZimlet(server)) {
                if (soapUtil == null) {
                    soapUtil = new ZimletSoapUtil();
                }
                soapUtil.undeployZimletRemotely(server, zimlet);
            }
        }
    }

    private static boolean canDeployZimlet(Server server) {
        return server.hasMailboxService() || server.hasMailClientService() ||
            server.hasZimletService() || server.hasAdminClientService() ||
            server.hasWebClientService();
    }

    public static void showInfo(String zimlet) throws ServiceException, ZimletException, IOException {
        Provisioning prov = Provisioning.getInstance();
        Zimlet z = prov.getZimlet(zimlet);
        if (z == null) {
            throw AccountServiceException.NO_SUCH_ZIMLET(zimlet);
        }
        List<Zimlet> plist = orderZimletsByPriority();
        int pri = 0;
        for (Zimlet zp : plist) {
            if (zp.compareTo(z) == 0) {
                break;
            }
            pri++;
        }
        System.out.println("Zimlet "+z.getName());
        System.out.println("         Version: "+z.getAttr(Provisioning.A_zimbraZimletVersion));
        System.out.println("     Description: "+z.getDescription());
        System.out.println("        Priority: "+pri);
        System.out.println("         Enabled: "+(z.isEnabled() ? "true" : "false"));
        System.out.println("Indexing Enabled: "+(z.isIndexingEnabled() ? "true" : "false"));
        if (z.isExtension()) {
            System.out.println("       Extension: true");
        }
        String cosList = null;
        for (Cos cos : prov.getAllCos()) {
            for (String zc : getAvailableZimlets(cos).getZimletNamesAsArray()) {
                if (zc.compareTo(zimlet) != 0) {
                    continue;
                }
                if (cosList == null) {
                    cosList = cos.getName();
                } else {
                    cosList = cosList + ", " + cos.getName();
                }
                break;
            }
        }
        System.out.println("Activated in COS: "+cosList);
        System.out.println("          Config: "+z.getHandlerConfig());
        ZimletFile zf = getZimlet(zimlet);
        if (zf == null) {
            System.out.println("*** Zimlet file is missing on this machine");
        } else {
            ZimletDescription desc = zf.getZimletDescription();
            String val = desc.getRegexString();
            if (val != null) {
                System.out.println("           RegEx: "+val);
            }
            val = desc.getContentObjectAsXML();
            if (val != null) {
                System.out.println("  Content Object: "+val);
            }
            val = desc.getPanelItemAsXML();
            if (val != null) {
                System.out.println("      Panel Item: "+val);
            }
            val = null;
            for (String script : desc.getScripts()) {
                if (val == null) {
                    val = script;
                } else {
                    val = val + ", " + script;
                }
            }
            if (val != null) {
                System.out.println("         Scripts: "+val);
            }
            val = null;
            for (String css : desc.getStyleSheets()) {
                if (val == null) {
                    val = css;
                } else {
                    val = val + ", " + css;
                }
            }
            if (val != null) {
                System.out.println("             CSS: "+val);
            }
            val = null;
            for (String target : desc.getTargets()) {
                if (val == null) {
                    val = target;
                } else {
                    val = val + ", " + target;
                }
            }
            if (val != null) {
                System.out.println("         Targets: "+val);
            }
        }
    }

    public static void createZip(String dirName, String descFile) throws IOException {
        File dir = new File(dirName);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IOException("directory does not exist: "+dirName);
        }
        String target = descFile;
        boolean found = false;
        for (String f : dir.list()) {
            if (target != null) {
                if (target.compareTo(f) == 0) {
                    found = true;
                    break;
                }
            } else if (f.endsWith(".xml") && f.substring(0, f.length()-4).compareTo(dir.getName()) == 0) {
                target = f;
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IOException("Zimlet description not found, or not named correctly.");
        }
        String manifest = "Manifest-Version: 1.0\nZimlet-Description-File: "+target+"\n";
        JarOutputStream out = new JarOutputStream(new FileOutputStream(target.substring(0, target.length()-4)+".zip"),
                new Manifest(new ByteArrayInputStream(manifest.getBytes("UTF-8"))));
        for (File f : dir.listFiles()) {
            addZipEntry(out, f, null);
        }
        out.close();
    }

    private static void addZipEntry(ZipOutputStream out, File file, String path) throws IOException {
        String name = (path == null) ? file.getName() : path + "/" + file.getName();
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                addZipEntry(out, f, name);
            }
            return;
        }
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(file.length());
        entry.setCompressedSize(file.length());
        entry.setCrc(computeCRC32(file));
        out.putNextEntry(entry);
        ByteUtil.copy(new FileInputStream(file), true, out, false);
        out.closeEntry();
    }

    private static long computeCRC32(File file) throws IOException {
        byte buf[] = new byte[32 * 1024];
        CRC32 crc = new CRC32();
        crc.reset();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            int bytesRead;
            while ((bytesRead = fis.read(buf)) != -1) {
                crc.update(buf, 0, bytesRead);
            }
            return crc.getValue();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {}
            }
        }
    }

    private static void test() {
        String ZIMLET_URL = "^/service/zimlet/([^/\\?]+)[/\\?]?.*$";
        String t1 = "/service/zimlet/po";
        String t2 = "/service/zimlet/foo?123";
        Pattern mPattern = Pattern.compile(ZIMLET_URL);
        Matcher matcher = mPattern.matcher(t1);
        if (matcher.matches()) {
            System.out.println( matcher.group(1) );
        }
        matcher = mPattern.matcher(t2);
        if (matcher.matches()) {
            System.out.println( matcher.group(1) );
        }
    }

    public static class ZimletSoapUtil {
        private String mUsername;
        private String mPassword;
        private String mAttachmentId;
        private String mAdminURL;
        private String mUploadURL;
        private ZAuthToken mAuth;
        private SoapHttpTransport mTransport;
        private boolean mSynchronous;
        private String mStatus;

        public ZimletSoapUtil() throws ServiceException {
            initZimletSoapUtil(null, null);
        }

        public ZimletSoapUtil(String adminURL, String uploadURL, String username, String password) throws ServiceException {
            mAdminURL = adminURL;
            mUploadURL = uploadURL;
            initZimletSoapUtil(username, password);
        }

        private void initZimletSoapUtil(String username, String password) throws ServiceException {
            mUsername = username != null ? username : LC.zimbra_ldap_user.value();
            mPassword = password != null ? password : LC.zimbra_ldap_password.value();
            mAuth = null;
            String adminURL = mAdminURL;
            if (adminURL == null) {
                String server = LC.zimbra_zmprov_default_soap_server.value();
                adminURL = URLUtil.getAdminURL(server);
            }
            SoapProvisioning sp = new SoapProvisioning();
            sp.soapSetURI(adminURL);
            sp.soapAdminAuthenticate(mUsername, mPassword);
        }

        public ZimletSoapUtil(ZAuthToken auth) {
            mAuth = auth;
        }

        public void deployZimletRemotely(Server server, String zimlet, byte[] data, DeployListener listener,
                boolean flushCache) throws ServiceException, IOException {
            if (server.hasMailClientService()) {
                ZimbraLog.zimlet.info("Deploying on service node %s", server.getName());
                deployZimletOnServiceNode(zimlet, data, server, listener, flushCache);
            } else {
                ZimbraLog.zimlet.info("Deploying on ui node %s", server.getName());
                deployZimletOnUiNode(zimlet, data, server, listener, flushCache);
            }
        }

        public void undeployZimletRemotely(Server server, String zimlet) throws ServiceException, IOException {
            ZimbraLog.zimlet.info("Undeploying on %s", server.getName());
            if (server.hasMailClientService()) {
                undeployZimletOnServiceNode(server, zimlet);
            } else {
                undeployZimletOnUiNode(server, zimlet);
            }
        }

        public void deployZimletOnServer(String zimlet, byte[] data, boolean flushCache) throws ServiceException {
            mTransport = null;
            try {
                mTransport = new SoapHttpTransport(mAdminURL);
                auth();
                mTransport.setAuthToken(mAuth);
                URL url = new URL(mUploadURL);
                mAttachmentId = postAttachment(mUploadURL, zimlet, data, url.getHost());

                soapDeployZimlet(flushCache);
                if (mSynchronous) {
                    ZimbraLog.zimlet.info("Deploy status: %s", mStatus);
                } else {
                    ZimbraLog.zimlet.info("Deploy initiated. Check the server's mailbox.log for the status.");
                }
            } catch (Exception e) {
                ZimbraLog.zimlet.info("deploy failed on %s", mAdminURL, e);
                if (e instanceof ServiceException) {
                    throw (ServiceException)e;
                } else {
                    throw ServiceException.FAILURE("Unable to deploy Zimlet " + zimlet + " on " + mAdminURL, e);
                }
            } finally {
                if (mTransport != null) {
                    mTransport.shutdown();
                }
            }
        }

        public void deployZimletOnServiceNode(String zimlet, byte[] data, Server server, DeployListener listener,
                boolean flushCache) throws ServiceException {
            mTransport = null;
            try {
                String adminUrl = URLUtil.getAdminURL(server, AdminConstants.ADMIN_SERVICE_URI);
                mTransport = new SoapHttpTransport(adminUrl);
                // auth if necessary
                if (mAuth == null) {
                    auth();
                }
                mTransport.setAuthToken(mAuth);

                // upload
                String uploadUrl = URLUtil.getAdminURL(server, "/service/upload?fmt=raw");
                ZimbraLog.zimlet.info("post to server %s, data size %d", server.getName(), data.length);
                mAttachmentId = postAttachment(uploadUrl, zimlet, data, server.getName());

                // deploy
                soapDeployZimlet(flushCache);
                if (mSynchronous) {
                    ZimbraLog.zimlet.info("Deploy status: %s", mStatus);
                } else {
                    ZimbraLog.zimlet.info("Deploy initiated. Check the server %s's mailbox.log for the status.",
                        server.getName());
                }
                if (listener != null) {
                    listener.markFinished(server);
                }
            } catch (Exception e) {
                ZimbraLog.zimlet.info("deploy failed on service node %s", server.getName(), e);
                if (listener != null) {
                    listener.markFailed(server, e);
                } else if (e instanceof ServiceException) {
                    throw (ServiceException)e;
                } else {
                    throw ServiceException.FAILURE("Unable to deploy Zimlet " + zimlet + " on " + server.getName(), e);
                }
            } finally {
                if (mTransport != null) {
                    mTransport.shutdown();
                }
            }
        }

        void deployZimletOnUiNode(String zimlet, byte[] data, Server server, DeployListener listener, boolean flushCache)
                throws ServiceException {
            try {
                // auth if necessary
                if (mAuth == null) {
                    auth();
                }
                WebClientServiceUtil.sendDeployZimletRequestToUiNode(server, zimlet, mAuth.getValue(), data);
                if (mSynchronous) {
                    ZimbraLog.zimlet.info("Deploy status: %s", mStatus);
                } else {
                    ZimbraLog.zimlet.info("Deploy initiated. Check the server %s's mailbox.log for the status.",
                        server.getName());
                }
                if (listener != null) {
                    listener.markFinished(server);
                }
            } catch (Exception e) {
                ZimbraLog.zimlet.info("deploy failed on ui node %s", server.getName(), e);
                if (listener != null) {
                    listener.markFailed(server, e);
                } else if (e instanceof ServiceException) {
                    throw (ServiceException) e;
                } else {
                    throw ServiceException.FAILURE("Unable to deploy Zimlet " + zimlet + " on " + server.getName(), e);
                }
            }
        }

        private void soapDeployZimlet(boolean flushCache) throws ServiceException, IOException, HttpException {
            XMLElement req = new XMLElement(AdminConstants.DEPLOY_ZIMLET_REQUEST);
            req.addAttribute(AdminConstants.A_ACTION, AdminConstants.A_DEPLOYLOCAL);
            req.addAttribute(AdminConstants.A_FLUSH, flushCache);
            if (mSynchronous) {
                req.addAttribute(AdminConstants.A_SYNCHRONOUS, mSynchronous);
            }
            req.addElement(MailConstants.E_CONTENT).addAttribute(MailConstants.A_ATTACHMENT_ID, mAttachmentId);
            Element res = mTransport.invoke(req);
            if (mSynchronous) {
                mStatus = res.getElement(AdminConstants.E_PROGRESS).getAttribute(AdminConstants.A_STATUS, "");
            }
        }

        public void undeployZimletOnServiceNode(Server server, String zimlet) throws ServiceException {
            mTransport = null;
            try {
                String adminUrl = URLUtil.getAdminURL(server, AdminConstants.ADMIN_SERVICE_URI);
                mTransport = new SoapHttpTransport(adminUrl);

                // auth if necessary
                if (mAuth == null) {
                    auth();
                }
                mTransport.setAuthToken(mAuth);
                XMLElement req = new XMLElement(AdminConstants.UNDEPLOY_ZIMLET_REQUEST);
                req.addAttribute(AdminConstants.A_ACTION, AdminConstants.A_DEPLOYLOCAL);
                req.addAttribute(AdminConstants.A_NAME, zimlet);
                mTransport.invoke(req);
                ZimbraLog.zimlet.info("Undeploy initiated. Check the server %s's mailbox.log for the status.",
                        server.getName());
            } catch (Exception e) {
                if (e instanceof ServiceException) {
                    throw (ServiceException)e;
                } else {
                    throw ServiceException.FAILURE("Unable to undeploy Zimlet " + zimlet + " on " + server.getName(), e);
                }
            } finally {
                if (mTransport != null) {
                    mTransport.shutdown();
                }
            }
        }

        public void undeployZimletOnUiNode(Server server, String zimlet) throws ServiceException {
            try {
                // auth if necessary
                if (mAuth == null) {
                    auth();
                }
                WebClientServiceUtil.sendUndeployZimletRequestToUiNode(server, zimlet, mAuth.getValue());
            } catch (Exception e) {
                ZimbraLog.zimlet.warn("undeployment failed on ui node %s", server.getName(), e);
                if (e instanceof ServiceException) {
                    throw (ServiceException) e;
                } else {
                    throw ServiceException.FAILURE("Unable to undeploy Zimlet " + zimlet + " on " + server.getName(), e);
                }
            }
        }

        private String postAttachment(String uploadURL, String name, byte[] data, String domain)
                throws IOException, ZimletException {
            String aid = null;

            // no need/point to use ZimbraHttpConnectionManager because all callsites
            // of this methods are from command line, our idle connection reaper is
            // only started in the server anyway.  After the CLI exits all connections
            // will be released.
            HttpClientBuilder clientBuilder = HttpClientBuilder.create(); // CLI only, don't need conn mgr
            Map<String, String> cookieMap = mAuth.cookieMap(true);
            if (cookieMap != null) {
                BasicCookieStore cookieStore = new BasicCookieStore();
                for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                    BasicClientCookie cookie = new BasicClientCookie(ck.getKey(), ck.getValue());
                    cookie.setDomain(domain);
                    cookie.setPath("/");
                    cookie.setSecure(false);
                    cookie.setExpiryDate(null);
                    cookieStore.addCookie(cookie);

                }
                clientBuilder.setDefaultCookieStore(cookieStore);

                RequestConfig reqConfig = RequestConfig.custom().
                    setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();
                clientBuilder.setDefaultRequestConfig(reqConfig);
            }

            HttpPost post = new HttpPost(uploadURL);
            SocketConfig config = SocketConfig.custom().setSoTimeout(
                (int) TimeUnit.MILLISECONDS.convert(LC.zimlet_deploy_timeout.intValue(),
                TimeUnit.SECONDS)).build();
            clientBuilder.setDefaultSocketConfig(config);

            int statusCode = -1;
            try {
                String contentType = URLConnection.getFileNameMap().getContentTypeFor(name);
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.addBinaryBody("upfile", data, ContentType.create(contentType), name);
                HttpEntity httpEntity = builder.build();
                post.setEntity(httpEntity);
                HttpClient client = clientBuilder.build();

                HttpResponse httpResp = HttpClientUtil.executeMethod(client, post);
                statusCode = httpResp.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    String response = EntityUtils.toString(httpResp.getEntity());

                    // "raw" response should be of the format
                    //   200,'null','aac04dac-b3c8-4c26-b9c2-c2534f1d6ba1:79d2722f-d4c7-4304-9961-e7bcb146fc32'
                    String[] responseParts = response.split(",", 3);
                    if (responseParts.length == 3) {
                        aid = responseParts[2].trim();
                        if (aid.startsWith("'") || aid.startsWith("\"")) {
                            aid = aid.substring(1);
                        }
                        if (aid.endsWith("'") || aid.endsWith("\"")) {
                            aid = aid.substring(0, aid.length() - 1);
                        }
                    }
                    if (aid == null) {
                        throw ZimletException.CANNOT_DEPLOY("Attachment post failed, unexpected response: " + response, null);
                    }
                } else {
                    throw ZimletException.CANNOT_DEPLOY("Attachment post failed, status=" + statusCode, null);
                }
            } catch (HttpException e) {
                throw new IOException("unexpected error during postAttachment operation: " + e.getMessage());
            } finally {
                post.releaseConnection();
            }

            return aid;
        }

        private void auth() throws ServiceException, IOException {
            XMLElement req = new XMLElement(AdminConstants.AUTH_REQUEST);
            req.addElement(AdminConstants.E_NAME).setText(mUsername);
            req.addElement(AdminConstants.E_PASSWORD).setText(mPassword);
            if(mTransport == null) {
                String adminUrl = URLUtil.getAdminURL(LC.zimbra_zmprov_default_soap_server.value());
                mTransport = new SoapHttpTransport(adminUrl);
            }

            Element resp = mTransport.invoke(req);
            mAuth = new ZAuthToken(resp.getElement(AccountConstants.E_AUTH_TOKEN), true);
        }

    }

    private static final int INSTALL_ZIMLET = 10;
    private static final int UNINSTALL_ZIMLET = 11;
    private static final int LIST_ZIMLETS = 12;
    private static final int ACL_ZIMLET = 13;
    private static final int LIST_ACLS = 14;
    private static final int DUMP_CONFIG = 15;
    private static final int INSTALL_CONFIG = 16;
    private static final int LDAP_DEPLOY = 17;
    private static final int DEPLOY_ZIMLET = 18;
    private static final int ENABLE_ZIMLET = 19;
    private static final int DISABLE_ZIMLET = 20;
    private static final int LIST_PRIORITY = 21;
    private static final int SET_PRIORITY = 22;
    private static final int INFO = 23;
    private static final int CREATE_ZIP = 24;
    private static final int TEST = 99;

    private static final String INSTALL_CMD = "install";
    private static final String UNINSTALL_CMD = "uninstall";
    private static final String UNDEPLOY_CMD = "undeploy";
    private static final String LIST_CMD = "listzimlets";
    private static final String ACL_CMD = "acl";
    private static final String LIST_ACLS_CMD = "listacls";
    private static final String DUMP_CONFIG_CMD = "getconfigtemplate";
    private static final String INSTALL_CONFIG_CMD = "configure";
    private static final String LDAP_DEPLOY_CMD = "ldapdeploy";
    private static final String DEPLOY_CMD = "deploy";
    private static final String ENABLE_CMD = "enable";
    private static final String DISABLE_CMD = "disable";
    private static final String LIST_PRIORITY_CMD = "listpriority";
    private static final String SET_PRIORITY_CMD = "setpriority";
    private static final String INFO_CMD = "info";
    private static final String CREATE_ZIP_CMD = "createzip";
    private static final String TEST_CMD = "test";

    private static Map<String,Integer> mCommands;

    private static void addCommand(String cmd, int cmdId) {
        mCommands.put(cmd, Integer.valueOf(cmdId));
    }

    private static void setup() {
        mCommands = new HashMap<String,Integer>();
        addCommand(DEPLOY_CMD, DEPLOY_ZIMLET);
        addCommand(UNDEPLOY_CMD, UNINSTALL_ZIMLET);
        addCommand(INSTALL_CMD, INSTALL_ZIMLET);
        addCommand(UNINSTALL_CMD, UNINSTALL_ZIMLET);
        addCommand(LIST_CMD, LIST_ZIMLETS);
        addCommand(ACL_CMD, ACL_ZIMLET);
        addCommand(LIST_ACLS_CMD, LIST_ACLS);
        addCommand(DUMP_CONFIG_CMD, DUMP_CONFIG);
        addCommand(INSTALL_CONFIG_CMD, INSTALL_CONFIG);
        addCommand(LDAP_DEPLOY_CMD, LDAP_DEPLOY);
        addCommand(ENABLE_CMD, ENABLE_ZIMLET);
        addCommand(DISABLE_CMD, DISABLE_ZIMLET);
        addCommand(LIST_PRIORITY_CMD, LIST_PRIORITY);
        addCommand(SET_PRIORITY_CMD, SET_PRIORITY);
        addCommand(INFO_CMD, INFO);
        addCommand(CREATE_ZIP_CMD, CREATE_ZIP);
        addCommand(TEST_CMD, TEST);
    }

    private static void usage() {
        System.out.println("Usage: zmzimletctl [-l] [-a <admin url> -u <upload url>] [command] [ zimlet.zip | config.xml | zimlet ]");
        System.out.println("\tdeploy {zimlet.zip} - install, ldapDeploy, grant ACL on default COS, then enable Zimlet");
        System.out.println("\tundeploy {zimlet} - remove the Zimlet from the system");
        System.out.println("\tinstall {zimlet.zip} - installs the Zimlet files on this host");
        System.out.println("\tldapDeploy {zimlet} - add the Zimlet entry to the system");
        System.out.println("\tenable {zimlet} - enables the Zimlet");
        System.out.println("\tdisable {zimlet} - disables the Zimlet");
        System.out.println("\tacl {zimlet} {cos1} grant|deny [{cos2} grant|deny...] - change the ACL for the Zimlet on a COS");
        System.out.println("\tlistAcls {zimlet} - list ACLs for the Zimlet");
        System.out.println("\tlistZimlets - show status of all the Zimlets in the system.");
        System.out.println("\tgetConfigTemplate {zimlet.zip} - dumps the configuration");
        System.out.println("\tconfigure {config.xml} - installs the configuration");
        System.out.println("\tlistPriority - show the current Zimlet priorities (0 high, 9 low)");
        System.out.println("\tsetPriority {zimlet} {priority} - set Zimlet priority");
        System.out.println("\tinfo {zimlet} - show information about zimlet");
        System.out.println("\tcreateZip {zimlet directory} [description-file] - creates zimlet.zip from the contents in the directory");
        System.exit(1);
    }

    private static int lookupCmd(String cmd) {
        Integer i = mCommands.get(cmd.toLowerCase());
        if (i == null) {
            usage();
        }
        return i.intValue();
    }

    private static void dispatch(String[] args) {
        boolean isLocalInstall = false;
        try {
            if (args[argPos].equals("-l")) {
                isLocalInstall = true;
                argPos++;
            }
            String adminURL = null;
            String uploadURL = null;
            if (args[argPos].equals("-a")) {
                adminURL = args[++argPos];
                argPos++;
            }
            if (args[argPos].equals("-u")) {
                uploadURL = args[++argPos];
                argPos++;
            }
            if (argPos >= args.length) {
                usage();
            }

            int cmd = lookupCmd(args[argPos++]);
            switch (cmd) {
            case LIST_ZIMLETS:
                boolean everything = false;
                if (args.length > argPos && args[argPos].equals("all")) {
                    everything = true;
                }
                listAllZimlets(everything);
                System.exit(0);
            case LIST_PRIORITY:
                listPriority();
                System.exit(0);
            case TEST:
                test();
                System.exit(0);
            }

            if (args.length < argPos+1) {
                usage();
            }
            String zimlet = args[argPos++];
            switch (cmd) {
            case DEPLOY_ZIMLET:
                deployZimletLocally(new ZimletFile(zimlet));
                if (!isLocalInstall) {
                    boolean synchronous = false;
                    if (args.length > argPos && args[argPos].equals("sync")) {
                        synchronous = true;
                    }
                    deployZimletRemotely(zimlet, adminURL, uploadURL, synchronous);
                }
                break;
            case INSTALL_ZIMLET:
                installZimletLocally(new ZimletFile(zimlet));
                break;
            case UNINSTALL_ZIMLET:
                undeployZimletLocally(zimlet);
                if (!isLocalInstall) {
                    undeployZimletRemotely(zimlet);
                }
                break;
            case LDAP_DEPLOY:
                ldapDeploy(zimlet);
                break;
            case ACL_ZIMLET:
                if (args.length < (argPos+2) || args.length % 2 != 0) {
                    usage();
                }
                aclZimlet(zimlet, args);
                break;
            case SET_PRIORITY:
                if (args.length < (argPos+1)) {
                    usage();
                }
                setPriority(zimlet, Integer.parseInt(args[argPos]));
                listPriority();
                break;
            case LIST_ACLS:
                listAcls(zimlet);
                break;
            case ENABLE_ZIMLET:
                enableZimlet(zimlet);
                break;
            case DISABLE_ZIMLET:
                disableZimlet(zimlet);
                break;
            case DUMP_CONFIG:
                dumpConfig(zimlet);
                break;
            case INSTALL_CONFIG:
                installConfig(new String(ByteUtil.getContent(new File(zimlet))));
                break;
            case INFO:
                showInfo(zimlet);
                break;
            case CREATE_ZIP:
                createZip(zimlet, (args.length == argPos) ? null : args[argPos]);
                break;
            default:
                usage();
                break;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            usage();
        } catch (Exception e) {
            e.printStackTrace();
            if (sQuietMode) {
                ZimbraLog.zimlet.error("Error " + e.getMessage());
            } else {
                ZimbraLog.zimlet.error("Error", e);
            }
            System.exit(1);
        }
    }

    private static boolean sQuietMode = false;
    private static int argPos = 0;

    private static void getOpt(String[] args) {
        if (args.length < 1) {
            usage();
        }

        int index = 0;
        while (index < args.length) {
            if (args[index].equals("-q")) {
                sQuietMode = true;
            } else {
                break;
            }
            index++;
        }
        argPos = index;
    }

    public static void main(String[] args) throws IOException {

        getOpt(args);
        if (sQuietMode) {
            CliUtil.toolSetup("WARN");
        } else {
            CliUtil.toolSetup();
        }
        SoapTransport.setDefaultUserAgent("zmzimletctl", BuildInfo.VERSION);
        setup();
        dispatch(args);
    }
}
