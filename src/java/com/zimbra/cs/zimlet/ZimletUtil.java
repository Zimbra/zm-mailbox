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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.cs.account.Cos;
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
	
	public static final String ZIMLET_BASE = "/service/zimlet";
	public static final String ZIMLET_DEV_DIR = "_dev";
	public static final String ZIMLET_ALLOWED_DOMAINS = "allowedDomains";
	public static final String ZIMLET_DEFAULT_COS = "default";
	
	private static boolean sZimletsLoaded = false;
	private static Map<String,ZimletFile> sZimlets = new HashMap<String,ZimletFile>();
	private static Map<String,ZimletFile> sDevZimlets = new HashMap<String,ZimletFile>();
	private static Map<String,Class> sZimletHandlers = new HashMap<String,Class>();

	public static String[] listZimletNames() {
		String[] zimlets = sZimlets.keySet().toArray(new String[0]);
		Arrays.sort(zimlets);
		return zimlets;
	}
	
	public static String[] listDevZimletNames() {
		String[] zimlets = sDevZimlets.keySet().toArray(new String[0]);
		Arrays.sort(zimlets);
		return zimlets;
	}
	
	public static ZimletConfig getZimletConfig(String zimlet) {
		loadZimlets();
		loadDevZimlets();
		ZimletFile zf;
		zf = sZimlets.get(zimlet);
		if (zf == null) {
			zf = sDevZimlets.get(zimlet);
		}
		if (zf == null) {
			return null;
		}
		try {
			return zf.getZimletConfig();
		} catch (Exception e) {
			ZimbraLog.zimlet.info("error loading zimlet "+zimlet, e);
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
	
	/**
	 * 
	 * Load all the installed Zimlets.
	 *
	 */
	public synchronized static void loadZimlets() {
		if (!sZimletsLoaded) {
			loadZimletsFromDir(sZimlets, LC.zimlet_directory.value());
			sZimletsLoaded = true;
		}
	}

	/**
	 * 
	 * Load all the Zimlets in the dev test directory.
	 *
	 */
	public synchronized static void loadDevZimlets() {
		loadZimletsFromDir(sDevZimlets, LC.zimlet_directory.value() + File.separator + ZIMLET_DEV_DIR);
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
			throw ZimletException.ZIMLET_HANDLER_ERROR("cannot load zimlet "+zimlet);
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
        		try {
        			zimlets.put(zimletNames[i], new ZimletFile(zimletRootDir, zimletNames[i]));
        		} catch (IOException ioe) {
        			ZimbraLog.zimlet.info("error loading zimlet "+zimletNames[i]+": "+ioe.getMessage());
        		} catch (ZimletException ze) {
        			ZimbraLog.zimlet.info("error loading zimlet "+zimletNames[i]+": "+ze.getMessage());
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
	public static void listZimlet(Element elem, String zimlet) {
		loadZimlets();
		ZimletFile zim = (ZimletFile) sZimlets.get(zimlet);
		if (zim == null) {
			ZimbraLog.zimlet.info("cannot find zimlet "+zimlet);
			return;
		}
		try {
			String zimletBase = ZIMLET_BASE + "/" + zim.getName() + "/";
			Element entry = elem.addElement(AccountService.E_ZIMLET);
			Element zimletContext = entry.addElement(AccountService.E_ZIMLET_CONTEXT);
			zimletContext.addAttribute(AccountService.A_ZIMLET_BASE_URL, zimletBase);
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
	
	/**
	 * 
	 * List the description of all the dev test Zimlets as Element.
	 * 
	 * @param elem - Parent element node
	 */
	public static void listDevZimlets(Element elem) {
		try {
			loadDevZimlets();
			synchronized (sDevZimlets) {
	        	Iterator iter = sDevZimlets.values().iterator();
	        	while (iter.hasNext()) {
	        		ZimletFile zim = (ZimletFile) iter.next();
	    			String zimletBase = ZIMLET_BASE + "/" + ZIMLET_DEV_DIR + "/" + zim.getName() + "/";
	    			Element entry = elem.addElement(AccountService.E_ZIMLET);
	    			Element zimletContext = entry.addElement(AccountService.E_ZIMLET_CONTEXT);
	    			zimletContext.addAttribute(AccountService.A_ZIMLET_BASE_URL, zimletBase);
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
		Map<String,String> attrs = new HashMap<String,String>();
		attrs.put(Provisioning.A_zimbraZimletKeyword,         zd.getName());
		attrs.put(Provisioning.A_zimbraZimletVersion,         zd.getVersion().toString());
		attrs.put(Provisioning.A_zimbraZimletDescription,     zd.getDescription());
		attrs.put(Provisioning.A_zimbraZimletHandlerClass,    zd.getServerExtensionClass());
		attrs.put(Provisioning.A_zimbraZimletServerIndexRegex, zd.getRegexString());
		//attrs.put(Provisioning.A_zimbraZimletContentObject,   zd.getContentObjectAsXML());
		//attrs.put(Provisioning.A_zimbraZimletPanelItem,       zd.getPanelItemAsXML());
		//attrs.put(Provisioning.A_zimbraZimletScript,          zd.getScripts());
		return attrs;
	}
	
	private static String getZimletDir() {
		return LC.zimlet_directory.value();
	}

	/**
	 * 
	 * Deploys the specified Zimlets.  The following actions are taken.
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
	public static void deployZimlet(ZimletFile zf) throws IOException, ZimletException {
		Provisioning prov = Provisioning.getInstance();
		String zimletName = zf.getZimletName();
		ZimletDescription zd = zf.getZimletDescription();
		try {
			// check if the zimlet already exists in LDAP.
			Zimlet z = prov.getZimlet(zimletName);
			Version ver = new Version(z.getAttr(Provisioning.A_zimbraZimletVersion));
			if (zd.getVersion().equals(ver)) {
				ZimbraLog.zimlet.info("Zimlet " + zimletName + " already installed in LDAP.");
				return;
			} else if (zd.getVersion().compareTo(ver) < 0) {
				ZimbraLog.zimlet.info("Zimlet " + zimletName + " being installed is of an older version.");
				return;
			}
			// upgrade
			ZimbraLog.zimlet.info("Upgrading Zimlet " + zimletName + " to " +zd.getVersion().toString());
			try {
				prov.deleteZimlet(z.getName());
				installZimlet(zf);
				ldapDeploy(zimletName);
				if (zf.hasZimletConfig()) {
					installConfig(zf.getZimletConfig());
				}
				return;
			} catch (ServiceException se) {
				ZimbraLog.zimlet.info("Upgrade failed: "+se.getMessage());
				if (se.getCause() != null) {
					throw ZimletException.CANNOT_CREATE(zimletName, se.getCause().getMessage());
				}
				throw ZimletException.CANNOT_CREATE(zimletName, se.getMessage());
			}
		} catch (ServiceException se) {
			// zimlet was not found in LDAP.  continue with deploy.
		}
		
		// install files
		installZimlet(zf);
		
		// deploy in LDAP
		ldapDeploy(zimletName);
		
		if (zf.hasZimletConfig()) {
			installConfig(zf.getZimletConfig());
		}
		if (!zd.isExtension()) {
			activateZimlet(zimletName, ZIMLET_DEFAULT_COS);
		}
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
	public static void installZimlet(ZimletFile zf) throws IOException, ZimletException {
		ZimletDescription zd = zf.getZimletDescription();
		String zimletName = zd.getName();
		ZimbraLog.zimlet.info("Installing Zimlet " + zimletName + " on this host.");
		String zimletRoot = getZimletDir();
		
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
	}
	
	/**
	 * 
	 * Deploy the Zimlet to LDAP.
	 * 
	 * @param zimlet
	 * @throws IOException
	 * @throws ZimletException
	 */
	public static void ldapDeploy(String zimlet) throws IOException, ZimletException {
		String zimletRoot = getZimletDir();
		ZimletFile zf = new ZimletFile(zimletRoot + File.separator + zimlet);
		ldapDeploy(zf);
	}
	
	public static void ldapDeploy(ZimletFile zf) throws IOException, ZimletException {
		ZimletDescription zd = zf.getZimletDescription();
		String zimletName = zd.getName();
		Map attrs = descToMap(zd);
		
		ZimbraLog.zimlet.info("Deploying Zimlet " + zimletName + " in LDAP.");

		// add zimlet entry to ldap
		Provisioning prov = Provisioning.getInstance();
		try {
			Zimlet zim = prov.createZimlet(zimletName, attrs);
			if (zd.isExtension()) {
				zim.setExtension(true);
			}
		} catch (ServiceException se) {
			if (se.getCause() != null) {
				throw ZimletException.CANNOT_CREATE(zimletName, se.getCause().getMessage());
			}
			throw ZimletException.CANNOT_CREATE(zimletName, se.getMessage());
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

	/**
	 * 
	 * Delete the Zimlet from LDAP.
	 * 
	 * @param zimlet
	 * @throws ZimletException
	 */
	public static void uninstallZimlet(String zimlet) throws ZimletException {
		ZimbraLog.zimlet.info("Uninstalling Zimlet " + zimlet + " from LDAP.");
		Provisioning prov = Provisioning.getInstance();
		try {
			@SuppressWarnings({"unchecked"})
			List<Cos> cos = prov.getAllCos();
			for (Cos c : cos) {
				try {
					deactivateZimlet(zimlet, c.getName());
				} catch (Exception e) {}
			}
			prov.deleteZimlet(zimlet);
		} catch (ServiceException se) {
			throw ZimletException.CANNOT_DELETE(zimlet, se.getCause().getMessage());
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
	public static void activateZimlet(String zimlet, String cos) throws ZimletException {
		ZimbraLog.zimlet.info("Adding Zimlet " + zimlet + " to COS " + cos);
		Provisioning prov = Provisioning.getInstance();
		try {
			prov.addZimletToCOS(zimlet, cos);
		} catch (Exception e) {
			throw ZimletException.CANNOT_ACTIVATE(zimlet, e.getCause().getMessage());
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
	public static void deactivateZimlet(String zimlet, String cos) throws ZimletException {
		ZimbraLog.zimlet.info("Removing Zimlet " + zimlet + " from COS " + cos);
		Provisioning prov = Provisioning.getInstance();
		try {
			prov.removeZimletFromCOS(zimlet, cos);
		} catch (Exception e) {
			throw ZimletException.CANNOT_DEACTIVATE(zimlet, e.getCause().getMessage());
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
			z.setEnabled(enabled);
		} catch (Exception e) {
			throw ZimletException.CANNOT_ENABLE(zimlet, e.getCause().getMessage());
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
	
	/**
	 * 
	 * List all the COS the Zimlet is available to.
	 * 
	 * @param zimlet
	 * @throws ZimletException
	 */
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
	
	/**
	 * 
	 * Print all the Zimlets installed on this host.
	 * 
	 * @throws ZimletException
	 */
	public static void listInstalledZimletsOnHost(boolean everything) throws ZimletException {
		loadZimlets();
		ZimletFile[] zimlets = (ZimletFile[]) sZimlets.values().toArray(new ZimletFile[0]);
		Arrays.sort(zimlets);
		for (int i = 0; i < zimlets.length; i++) {
			ZimletDescription zd;
			try {
				String extra = "";
				zd = zimlets[i].getZimletDescription();
				boolean isExtension = (zd != null && zd.isExtension());
				if (!everything && isExtension) {
					continue;
				}
				if (isExtension) {
					extra += " (ext)";
				}
				System.out.println("\t"+zimlets[i].getName()+extra);
			} catch (IOException ioe) {
				ZimbraLog.zimlet.info("error reading zimlet : "+ioe.getMessage());
			}
		}
	}
	
	/**
	 * 
	 * Print all the Zimlets on LDAP.
	 * 
	 * @throws ZimletException
	 */
	public static void listInstalledZimletsInLdap(boolean everything) throws ZimletException {
		Provisioning prov = Provisioning.getInstance();
		try {
			Iterator iter = prov.listAllZimlets().iterator();
			while (iter.hasNext()) {
				String extra = "";
				Zimlet z = (Zimlet) iter.next();
				boolean isExtension = z.isExtension();
				if (!everything && isExtension) {
					continue;
				}
				if (!z.isEnabled()) {
					extra += " (disabled)";
				}
				if (isExtension) {
					extra += " (ext)";
				}
				System.out.println("\t"+z.getName()+extra);
			}
		} catch (Exception e) {
			throw ZimletException.ZIMLET_HANDLER_ERROR("cannot list installed zimlets in LDAP "+e.getMessage());
		}
	}
	
	/**
	 * 
	 * Print the Zimlet COS ACL for all the Zimlets.
	 * 
	 * @throws ZimletException
	 */
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
	
	/**
	 * 
	 * Print all the Zimlets installed on the host, on LDAP, and COS ACL.
	 * 
	 * @throws ZimletException
	 */
	public static void listAllZimlets(boolean everything) throws ZimletException {
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
	public static void installConfig(ZimletConfig zc) throws IOException, ZimletException {
		String zimletName = zc.getName();
		ZimbraLog.zimlet.info("Installing Zimlet config for " + zimletName);
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
	
	public static void installConfig(String config) throws IOException, ZimletException {
		installConfig(new ZimletConfig(new File(config)));
	}
	
	public static void listPriority() {
		Provisioning prov = Provisioning.getInstance();
		try {
			System.out.println("Pri\tZimlet");
			Iterator iter = prov.listAllZimlets().iterator();
			while (iter.hasNext()) {
				Zimlet z = (Zimlet) iter.next();
				if (z.getPriority() >= 0) {
					System.out.println(z.getPriority() + "\t" + z.getName());
				}
			}
		} catch (ServiceException se) {
			ZimbraLog.zimlet.info("error reading zimlets : "+se.getMessage());
		}
	}
	
	public static void setPriority(String zimlet, int priority) {
		Provisioning prov = Provisioning.getInstance();
		try {
			Zimlet z = prov.getZimlet(zimlet);
			z.setPriority(priority);
		} catch (ServiceException se) {
			ZimbraLog.zimlet.info("error setting zimlet priority : "+se.getMessage());
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
	private static final int LIST_PRIORITY = 21;
	private static final int SET_PRIORITY = 22;
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
	private static final String TEST_CMD = "test";
	
	private static Map<String,Integer> mCommands;
	
	private static void addCommand(String cmd, int cmdId) {
		mCommands.put(cmd, new Integer(cmdId));
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
		addCommand(TEST_CMD, TEST);
	}
	
	private static void usage() {
		System.out.println("zimlet: [command] [ zimlet.zip | config.xml | zimlet ]");
		System.out.println("\tdeploy {zimlet.zip} - install, ldapDeploy, grant ACL on default COS, then enable zimlet");
		System.out.println("\tundeploy {zimlet} - remove the zimlet entry from the system");
		System.out.println("\tinstall {zimlet.zip} - installs the zimlet files on this host");
		System.out.println("\tldapDeploy {zimlet} - add the zimlet entry to the system");
		System.out.println("\tenable {zimlet} - enables the zimlet");
		System.out.println("\tdisable {zimlet} - disables the zimlet");
		System.out.println("\tacl {zimlet} {cos1} grant|deny [{cos2} grant|deny...] - change the ACL for the zimlet on a COS");
		System.out.println("\tlistAcl {zimlet} - list ACLs for the Zimlet");
		System.out.println("\tlistZimlets - show status of all the Zimlets in the system.");
		System.out.println("\tdumpConfigTemplate {zimlet.zip} - dumps the configuration");
		System.out.println("\tconfigure {zimlet} {config.xml} - installs the configuration");
		System.out.println("\tlistPriority - show the current Zimlet priorities (0 high, 9 low)");
		System.out.println("\tsetPriority {zimlet} {priority} - set Zimlet priority");
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
		int cmd = lookupCmd(args[argPos++]);
		try {
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
				deployZimlet(new ZimletFile(zimlet));
				break;
			case INSTALL_ZIMLET:
				installZimlet(new ZimletFile(zimlet));
				break;
			case UNINSTALL_ZIMLET:
				uninstallZimlet(zimlet);
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
    		Zimbra.toolSetup("WARN");
    	} else {
    		Zimbra.toolSetup();
    	}
        setup();
        dispatch(args);
    }
}
