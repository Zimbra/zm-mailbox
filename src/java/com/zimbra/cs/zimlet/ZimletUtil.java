/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zimlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.util.FileUtil;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.Element;

/**
 * 
 * @author jylee
 *
 */
public class ZimletUtil {
	
	public static final String ZIMLET_DEV_DIR = "_dev";
	public static final String ZIMLET_ALLOWED_DOMAINS = "allowedDomains";
	public static final String ZIMLET_DEFAULT_COS = "default";
	
	private static boolean sZimletsLoaded = false;
	private static Map sZimlets = new HashMap();
	private static Map sDevZimlets = new HashMap();
	private static Map sZimletHandlers = new HashMap();

	public static ZimletHandler getHandler(String name) {
		loadZimlets();
		Class zh = (Class) sZimletHandlers.get(name);
		if (zh == null) {
			ZimletFile zf = (ZimletFile) sZimlets.get(name);
			if (zf == null) {
				return null;
			}
			try {
				String clazz = zf.getZimletDescription().getServerExtensionClass();
				if (clazz != null) {
					URL[] urls = { zf.toURL() };
					URLClassLoader cl = new URLClassLoader(urls, ZimletUtil.class.getClassLoader());
					zh = cl.loadClass(clazz);
					ZimbraLog.zimlet.info("Loaded class "+zh.getName());
					sZimletHandlers.put(name, zh);
				}
			} catch (Exception e) {
				ZimbraLog.zimlet.info("Unable to load zimlet handler: "+e.getMessage());
				return null;
			}
		}
		try {
			return (ZimletHandler) zh.newInstance();
		} catch (Exception e) {
			ZimbraLog.zimlet.info("Unable to instantiate zimlet handler: "+e.getMessage());
		}
		return null;
	}
	
	public synchronized static void loadZimlets() {
		if (!sZimletsLoaded) {
			loadZimletsFromDir(sZimlets, LC.zimlet_directory.value());
			sZimletsLoaded = true;
		}
	}

	public synchronized static void loadDevZimlets() {
		loadZimletsFromDir(sDevZimlets, LC.zimlet_directory.value() + File.separator + ZIMLET_DEV_DIR);
	}

	public static void reloadZimlet(String zimlet) throws ZimletException {
		ZimletFile zf;
		try {
			zf = new ZimletFile(LC.zimlet_directory.value() + File.separator + zimlet);
		} catch (IOException ioe) {
			throw ZimletException.ZIMLET_HANDLER_ERROR("cannot load zimlet "+zimlet);
		}
		synchronized (sZimlets) {
			sZimlets.remove(zimlet);
			sZimlets.put(zimlet, zf);
		}
	}
	
	private static void loadZimletsFromDir(Map zimlets, String dir) {
        File zimletRootDir = new File(dir);
		if (zimletRootDir == null || !zimletRootDir.exists() || !zimletRootDir.isDirectory()) {
			return;
		}

        ZimbraLog.zimlet.info("Loading zimlets from " + zimletRootDir.getPath());
        
        synchronized (zimlets) {
        	zimlets.clear();
        	String[] zimletNames = zimletRootDir.list();
        	assert(zimletNames != null);
        	for (int i = 0; i < zimletNames.length; i++) {
        		try {
        			zimlets.put(zimletNames[i], new ZimletFile(zimletRootDir, zimletNames[i]));
        		} catch (IOException ioe) {
        			ZimbraLog.zimlet.info("error loading zimlet "+zimletNames[i]+": "+ioe.getMessage());
        		}
        	}
        }
	}
	
	public static void listZimlet(Element elem, String zimlet) {
		loadZimlets();
		ZimletFile zim = (ZimletFile) sZimlets.get(zimlet);
		if (zim == null) {
			ZimbraLog.zimlet.info("cannot find zimlet "+zimlet);
			return;
		}
		try {
			Element entry = elem.addElement(AccountService.E_ZIMLET);
			zim.getZimletDescription().addToElement(entry);
			if (zim.hasZimletConfig()) {
				zim.getZimletConfig().addToElement(entry);
			}
		} catch (ZimletException ze) {
			ZimbraLog.zimlet.info("error loading zimlet "+zimlet+": "+ze.getMessage());
		} catch (IOException ioe) {
			ZimbraLog.zimlet.info("error loading zimlet "+zimlet+": "+ioe.getMessage());
		}
	}
	
	public static void listDevZimlets(Element elem) {
		try {
			loadDevZimlets();
			synchronized (sDevZimlets) {
	        	Iterator iter = sDevZimlets.values().iterator();
	        	while (iter.hasNext()) {
	        		ZimletFile zim = (ZimletFile) iter.next();
	        		Element entry = elem.addElement(AccountService.E_ZIMLET);
	    			zim.getZimletDescription().addToElement(entry);
	    			if (zim.hasZimletConfig()) {
	    				zim.getZimletConfig().addToElement(entry);
	    			}
	        	}
			}
		} catch (ZimletException ze) {
			ZimbraLog.zimlet.info("error loading dev zimlets: "+ze.getMessage());
		} catch (IOException ioe) {
			ZimbraLog.zimlet.info("error loading dev zimlets: "+ioe.getMessage());
		}
	}

	private static Map descToMap(ZimletDescription zd) throws ZimletException {
		Map attrs = new HashMap();
		attrs.put(Provisioning.A_zimbraZimletKeyword,         zd.getName());
		attrs.put(Provisioning.A_zimbraZimletVersion,         zd.getVersion());
		attrs.put(Provisioning.A_zimbraZimletDescription,     zd.getDescription());
		attrs.put(Provisioning.A_zimbraZimletIndexingEnabled, zd.getServerExtensionHasKeyword());
		//attrs.put(Provisioning.A_zimbraZimletStoreMatched,    zd.getStoreMatched());
		attrs.put(Provisioning.A_zimbraZimletHandlerClass,    zd.getServerExtensionClass());
		attrs.put(Provisioning.A_zimbraZimletServerIndexRegex, zd.getRegexString());
		//attrs.put(Provisioning.A_zimbraZimletContentObject,   zd.getContentObjectAsXML());
		//attrs.put(Provisioning.A_zimbraZimletPanelItem,       zd.getPanelItemAsXML());
		//attrs.put(Provisioning.A_zimbraZimletScript,          zd.getScripts());
		return attrs;
	}
	
	public static void deployZimlet(String zimletRoot, String zimlet) throws IOException, ZimletException {
		String zimletName = installZimlet(zimletRoot, zimlet);
		ldapDeploy(zimletRoot, zimletName);
		activateZimlet(zimletName, ZIMLET_DEFAULT_COS);
	}
	
	public static String installZimlet(String zimletRoot, String zimlet) throws IOException, ZimletException {
		ZimletFile zf = new ZimletFile(zimlet);
		ZimletDescription zd = zf.getZimletDescription();
		String zimletName = zd.getName();
		
		// install the files
		File zimletDir = new File(zimletRoot + File.separatorChar + zimletName);
		if (!zimletDir.exists()) {
			FileUtil.mkdirs(zimletDir);
		}

		Iterator files = zf.getAllEntries().entrySet().iterator();
		while (files.hasNext()) {
			Map.Entry f = (Map.Entry) files.next();
			ZimletFile.ZimletEntry entry = (ZimletFile.ZimletEntry) f.getValue();
			String fname = entry.getName();
			if (fname.endsWith("/") || fname.endsWith("\\")) {
				continue;
			} else {
				File file = new File(zimletDir, fname);
				file.getParentFile().mkdirs();
				writeFile(entry.getContents(), file);
			}
		}
		return zimletName;
	}
	
	public static void ldapDeploy(String zimletRoot, String zimlet) throws IOException, ZimletException {
		ZimletFile zf = new ZimletFile(zimletRoot + File.separator + zimlet);
		ZimletDescription zd = zf.getZimletDescription();
		String zimletName = zd.getName();
		Map attrs = descToMap(zd);
		
		if (zf.hasZimletConfig()) {
			ZimletConfig config = zf.getZimletConfig();
			attrs.put(Provisioning.A_zimbraZimletHandlerConfig, config.toXMLString());
		}
		
		// add zimlet entry to ldap
		Provisioning prov = Provisioning.getInstance();
		try {
			prov.createZimlet(zimletName, attrs);
		} catch (ServiceException se) {
			throw ZimletException.CANNOT_CREATE(zimletName, se.getCause().getMessage());
		}
	}
	
	private static void writeFile(byte[] src, File dest) throws IOException {
		dest.createNewFile();
		ByteArrayInputStream bais = new ByteArrayInputStream(src);
		FileOutputStream fos = new FileOutputStream(dest);
		ByteUtil.copy(bais, fos);
		fos.close();
		bais.close();
	}

	public static void uninstallZimlet(String zimlet) throws ZimletException {
		Provisioning prov = Provisioning.getInstance();
		try {
			prov.deleteZimlet(zimlet);
		} catch (ServiceException se) {
			throw ZimletException.CANNOT_DELETE(zimlet, se.getCause().getMessage());
		}
	}
	
	public static void upgradeZimlet(String zimlet) {
		
	}
	
	public static void activateZimlet(String zimlet, String cos) throws ZimletException {
		Provisioning prov = Provisioning.getInstance();
		try {
			prov.addZimletToCOS(zimlet, cos);
		} catch (Exception e) {
			throw ZimletException.CANNOT_ACTIVATE(zimlet, e.getCause().getMessage());
		}
	}
	
	public static void deactivateZimlet(String zimlet, String cos) throws ZimletException {
		Provisioning prov = Provisioning.getInstance();
		try {
			prov.removeZimletFromCOS(zimlet, cos);
		} catch (Exception e) {
			throw ZimletException.CANNOT_DEACTIVATE(zimlet, e.getCause().getMessage());
		}
	}
	
	public static void dumpConfig(String zimlet) throws IOException, ZimletException {
		ZimletFile zf = new ZimletFile(zimlet);
		String config = zf.getZimletConfigString();
		System.out.println(config);
	}
	
	public static void installConfig(String config) throws IOException, ZimletException {
		ZimletConfig zc = new ZimletConfig(new File(config));
		String zimletName = zc.getName();
		String configString = zc.toXMLString();
		Provisioning prov = Provisioning.getInstance();
		try {
			prov.updateZimletConfig(zimletName, configString);
			String allowedDomains = zc.getConfigValue(ZIMLET_ALLOWED_DOMAINS);
			if (allowedDomains != null) {
				prov.addAllowedDomains(allowedDomains, "default");  // XXX to default cos for now
			}
		} catch (Exception e) {
			throw ZimletException.INVALID_ZIMLET_CONFIG("cannot update Zimlet config for "+zimletName+" : "+e.getMessage());
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
	
	private static final int INSTALL_ZIMLET = 10;
	private static final int UNINSTALL_ZIMLET = 11;
	private static final int UPGRADE_ZIMLET = 12;
	private static final int ACTIVATE_ZIMLET = 13;
	private static final int DEACTIVATE_ZIMLET = 14;
	private static final int DUMP_CONFIG = 15;
	private static final int INSTALL_CONFIG = 16;
	private static final int LDAP_DEPLOY = 17;
	private static final int DEPLOY_ZIMLET = 18;
	private static final int TEST = 99;
	
	private static final String INSTALL_CMD = "install";
	private static final String UNINSTALL_CMD = "uninstall";
	private static final String UPGRADE_CMD = "upgrade";
	private static final String ACTIVATE_CMD = "activate";
	private static final String DEACTIVATE_CMD = "deactivate";
	private static final String DUMP_CONFIG_CMD = "config";
	private static final String INSTALL_CONFIG_CMD = "configure";
	private static final String LDAP_DEPLOY_CMD = "ldap-deploy";
	private static final String DEPLOY_CMD = "deploy";
	private static final String TEST_CMD = "test";
	
	private static Map mCommands;
	
	private static void setup() {
		mCommands = new HashMap();
		mCommands.put(INSTALL_CMD, new Integer(INSTALL_ZIMLET));
		mCommands.put(UNINSTALL_CMD, new Integer(UNINSTALL_ZIMLET));
		mCommands.put(UPGRADE_CMD, new Integer(UPGRADE_ZIMLET));
		mCommands.put(ACTIVATE_CMD, new Integer(ACTIVATE_ZIMLET));
		mCommands.put(DEACTIVATE_CMD, new Integer(DEACTIVATE_ZIMLET));
		mCommands.put(DUMP_CONFIG_CMD, new Integer(DUMP_CONFIG));
		mCommands.put(INSTALL_CONFIG_CMD, new Integer(INSTALL_CONFIG));
		mCommands.put(LDAP_DEPLOY_CMD, new Integer(LDAP_DEPLOY));
		mCommands.put(DEPLOY_CMD, new Integer(DEPLOY_ZIMLET));
		mCommands.put(TEST_CMD, new Integer(TEST));
	}
	
	private static void usage() {
		System.out.println("zimlet: [command] [ zimlet.zip | config.xml | zimlet ]");
		System.out.println("\tdeploy - install, ldap-deploy, then activate on default COS");
		System.out.println("\tinstall - installs the zimlet files on this host");
		System.out.println("\tuninstall - uninstalls the zimlet files on this host");
		System.out.println("\tldap-deploy - add the zimlet entry to the LDAP server");
		System.out.println("\tactivate - activates the zimlet on a COS");
		System.out.println("\tdeactivate - deactivates the zimlet from a COS");
		System.out.println("\tupgrade - upgrades the zimlet");
		System.out.println("\tconfig - dumps the configuration");
		System.out.println("\tconfigure - installs the configuration");
		System.exit(1);
	}
	
	private static int lookupCmd(String cmd) {
		Integer i = (Integer) mCommands.get(cmd.toLowerCase());
		if (i == null) {
			usage();
		}
		return i.intValue();
	}
	
	private static void dispatch(String[] args) {
		if (args.length < 2) {
			usage();
		}
		
		String zimletRoot = LC.zimlet_directory.value();
		String zimlet = args[1];
		
		int cmd = lookupCmd(args[0]);
		try {
			switch (cmd) {
			case DEPLOY_ZIMLET:
				deployZimlet(zimletRoot, zimlet);
				break;
			case INSTALL_ZIMLET:
				installZimlet(zimletRoot, zimlet);
				break;
			case UNINSTALL_ZIMLET:
				uninstallZimlet(zimlet);
				break;
			case LDAP_DEPLOY:
				ldapDeploy(zimletRoot, zimlet);
				break;
			case UPGRADE_ZIMLET:
				upgradeZimlet(zimlet);
				break;
			case ACTIVATE_ZIMLET:
				if (args.length < 3) {
					usage();
				}
				activateZimlet(zimlet, args[2]);
				break;
			case DEACTIVATE_ZIMLET:
				if (args.length < 3) {
					usage();
				}
				deactivateZimlet(zimlet, args[2]);
				break;
			case DUMP_CONFIG:
				dumpConfig(zimlet);
				break;
			case INSTALL_CONFIG:
				installConfig(zimlet);
				break;
			case TEST:
				test();
				break;
			default:
				System.out.println("Unknown command " + args[0]);
				break;
			}
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
	}
	
    public static void main(String[] args) throws IOException {
        Zimbra.toolSetup();
        setup();
        dispatch(args);
    }
}
