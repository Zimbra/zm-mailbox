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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Zimlet;
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

        ZimbraLog.zimlet.debug("Loading zimlets from " + zimletRootDir.getPath());
        
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
		enableZimlet(zimletName, true);
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
	
	public static void enableZimlet(String zimlet, boolean enabled) throws ZimletException {
		Provisioning prov = Provisioning.getInstance();
		try {
			Zimlet z = prov.getZimlet(zimlet);
			z.setEnabled(enabled);
		} catch (Exception e) {
			throw ZimletException.CANNOT_ENABLE(zimlet, e.getCause().getMessage());
		}
	}
	
	public static void aclZimlet(String zimlet, String[] args) throws ZimletException {
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
	
	public static void listAcls(String zimlet) throws ZimletException {
		System.out.println("Listing COS entries for Zimlet "+zimlet+"...");
		Provisioning prov = Provisioning.getInstance();
		try {
			Iterator iter = prov.getAllCos().iterator();
			while (iter.hasNext()) {
				NamedEntry cos = (NamedEntry) iter.next();
				String[] zimlets = cos.getMultiAttr(Provisioning.A_zimbraZimletAvailableZimlets);
				for (int i = 0; i < zimlets.length; i++) {
					if (zimlets[i].equals(zimlet)) {
						System.out.println("\t"+cos.getName());
						break;
					}
				}
			}
		} catch (Exception e) {
			throw ZimletException.ZIMLET_HANDLER_ERROR("cannot list acls "+e.getMessage());
		}
	}
	
	public static void listInstalledZimletsOnHost() throws ZimletException {
		loadZimlets();
		ZimletFile[] zimlets = (ZimletFile[]) sZimlets.values().toArray(new ZimletFile[0]);
		Arrays.sort(zimlets);
		for (int i = 0; i < zimlets.length; i++) {
			System.out.println("\t"+zimlets[i].getName());
		}
	}
	
	public static void listInstalledZimletsInLdap() throws ZimletException {
		Provisioning prov = Provisioning.getInstance();
		try {
			Iterator iter = prov.listAllZimlets().iterator();
			while (iter.hasNext()) {
				String disabled = "";
				Zimlet z = (Zimlet) iter.next();
				if (!z.isEnabled()) {
					disabled = " (disabled)";
				}
				System.out.println("\t"+z.getName()+disabled);
			}
		} catch (Exception e) {
			throw ZimletException.ZIMLET_HANDLER_ERROR("cannot list installed zimlets in LDAP "+e.getMessage());
		}
	}
	
	public static void listZimletsInCos() throws ZimletException {
		Provisioning prov = Provisioning.getInstance();
		try {
			Iterator iter = prov.getAllCos().iterator();
			while (iter.hasNext()) {
				NamedEntry cos = (NamedEntry) iter.next();
				System.out.println("  "+cos.getName()+":");
				String[] zimlets = cos.getMultiAttr(Provisioning.A_zimbraZimletAvailableZimlets);
				Arrays.sort(zimlets);
				for (int i = 0; i < zimlets.length; i++) {
					System.out.println("\t"+zimlets[i]);
				}
			}
		} catch (Exception e) {
			throw ZimletException.ZIMLET_HANDLER_ERROR("cannot list zimlets in COS "+e.getMessage());
		}
	}
	
	public static void listAllZimlets() throws ZimletException {
		System.out.println("Installed Zimlet files on this host:");
		listInstalledZimletsOnHost();
		System.out.println("Installed Zimlets in LDAP:");
		listInstalledZimletsInLdap();
		System.out.println("Available Zimlets in COS:");
		listZimletsInCos();
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
	private static final int LIST_ZIMLETS = 12;
	private static final int ACL_ZIMLET = 13;
	private static final int LIST_ACLS = 14;
	private static final int DUMP_CONFIG = 15;
	private static final int INSTALL_CONFIG = 16;
	private static final int LDAP_DEPLOY = 17;
	private static final int DEPLOY_ZIMLET = 18;
	private static final int ENABLE_ZIMLET = 19;
	private static final int DISABLE_ZIMLET = 20;
	private static final int TEST = 99;
	
	private static final String INSTALL_CMD = "install";
	private static final String UNINSTALL_CMD = "uninstall";
	private static final String LIST_CMD = "listzimlets";
	private static final String ACL_CMD = "acl";
	private static final String LIST_ACLS_CMD = "listacls";
	private static final String DUMP_CONFIG_CMD = "dumpconfigtemplate";
	private static final String INSTALL_CONFIG_CMD = "configure";
	private static final String LDAP_DEPLOY_CMD = "ldapdeploy";
	private static final String DEPLOY_CMD = "deploy";
	private static final String ENABLE_CMD = "enable";
	private static final String DISABLE_CMD = "disable";
	private static final String TEST_CMD = "test";
	
	private static Map mCommands;
	
	private static void addCommand(String cmd, int cmdId) {
		mCommands.put(cmd, new Integer(cmdId));
	}
	
	private static void setup() {
		mCommands = new HashMap();
		addCommand(INSTALL_CMD, INSTALL_ZIMLET);
		addCommand(UNINSTALL_CMD, UNINSTALL_ZIMLET);
		addCommand(LIST_CMD, LIST_ZIMLETS);
		addCommand(ACL_CMD, ACL_ZIMLET);
		addCommand(LIST_ACLS_CMD, LIST_ACLS);
		addCommand(DUMP_CONFIG_CMD, DUMP_CONFIG);
		addCommand(INSTALL_CONFIG_CMD, INSTALL_CONFIG);
		addCommand(LDAP_DEPLOY_CMD, LDAP_DEPLOY);
		addCommand(DEPLOY_CMD, DEPLOY_ZIMLET);
		addCommand(ENABLE_CMD, ENABLE_ZIMLET);
		addCommand(DISABLE_CMD, DISABLE_ZIMLET);
		addCommand(TEST_CMD, TEST);
	}
	
	private static void usage() {
		System.out.println("zimlet: [command] [ zimlet.zip | config.xml | zimlet ]");
		System.out.println("\tdeploy {zimlet.zip} - install, ldapDeploy, grant ACL on default COS, then enable zimlet");
		System.out.println("\tinstall {zimlet.zip} - installs the zimlet files on this host");
		System.out.println("\tuninstall {zimlet} - uninstalls the zimlet files on this host");
		System.out.println("\tenable {zimlet} - enables the zimlet");
		System.out.println("\tdisable {zimlet} - disables the zimlet");
		System.out.println("\tldapDeploy {zimlet} - add the zimlet entry to the LDAP server");
		System.out.println("\tacl {zimlet} {cos1} grant|deny [{cos2} grant|deny...] - change the ACL for the zimlet on a COS");
		System.out.println("\tlistAcl {zimlet} - list ACLs for the Zimlet");
		System.out.println("\tlistZimlets - show status of all the Zimlets in the system.");
		System.out.println("\tdumpConfigTemplate {zimlet.zip} - dumps the configuration");
		System.out.println("\tconfigure {zimlet} {config.xml} - installs the configuration");
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
		if (args.length < 1) {
			usage();
		}
		
		String zimletRoot = LC.zimlet_directory.value();
		
		int cmd = lookupCmd(args[0]);
		try {
			switch (cmd) {
			case LIST_ZIMLETS:
				listAllZimlets();
				System.exit(0);
			case TEST:
				test();
				System.exit(0);
			}

			if (args.length < 2) {
				usage();
			}
			String zimlet = args[1];
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
			case ACL_ZIMLET:
				if (args.length < 3 || args.length % 2 != 0) {
					usage();
				}
				aclZimlet(zimlet, args);
				break;
			case LIST_ACLS:
				listAcls(zimlet);
				break;
			case ENABLE_ZIMLET:
				enableZimlet(zimlet, true);
				break;
			case DISABLE_ZIMLET:
				enableZimlet(zimlet, false);
				break;
			case DUMP_CONFIG:
				dumpConfig(zimlet);
				break;
			case INSTALL_CONFIG:
				installConfig(zimlet);
				break;
			default:
				usage();
				break;
			}
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}
	
    public static void main(String[] args) throws IOException {
        Zimbra.toolSetup();
        setup();
        dispatch(args);
    }
}
