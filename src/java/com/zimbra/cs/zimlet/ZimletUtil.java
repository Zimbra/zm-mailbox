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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.extension.ZimbraExtension;
import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.object.ObjectType;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.FileUtil;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

/**
 * 
 * @author jylee
 *
 */
public class ZimletUtil {
	
	private static List sClassLoaders = new ArrayList();
	
	public static synchronized void loadAll() {
        File zimletRootDir = new File(LC.zimlet_directory.value());
		if (zimletRootDir == null) {
			ZimbraLog.zimlet.info(LC.zimbra_extensions_directory.key() + " is null, no extensions loaded");
			return;
		}
        ZimbraLog.zimlet.info("Loading extensions from " + zimletRootDir.getPath());

        Iterator iter;

        try {
    		List zimlets = Provisioning.getInstance().getObjectTypes();
            iter = zimlets.iterator();
        } catch (ServiceException se) {
            ZimbraLog.zimlet.info("Cannot get ObjectTypes from Provisioning while loading Zimlets");
        	return;
        }
        
        while (iter.hasNext()) {
        	ObjectType zimlet = (ObjectType) iter.next();
			String name = zimlet.getType();
			File zimletDir = new File(zimletRootDir, name);
			try {
				ZimletClassLoader cl = new ZimletClassLoader(zimletDir, name, ZimletUtil.class.getClassLoader());
				sClassLoaders.add(cl);
			} catch (MalformedURLException mue) {
				ZimbraLog.zimlet.info("Unable to load Zimlet extension: " + name);
			}
		}
	}

	private static List sInitializedExtensions = new ArrayList();
	
	public static synchronized void initAll() {
		ZimbraLog.zimlet.info("Initializing extensions");
		for (Iterator clIter = sClassLoaders.iterator(); clIter.hasNext();) {
			ZimletClassLoader zcl = (ZimletClassLoader)clIter.next();
			List classes = zcl.getExtensionClassNames();
			for (Iterator nameIter = classes.iterator(); nameIter.hasNext();) {
				String name = (String)nameIter.next();
				Class clz;
				try {
					clz = zcl.loadClass(name);
					ZimbraExtension ext = (ZimbraExtension)clz.newInstance();
					try {
						ext.init();
						ZimbraLog.zimlet.info("Initialized extension: " + name + "@" + zcl);
						sInitializedExtensions.add(ext);
					} catch (Throwable t) { 
						ZimbraLog.zimlet.warn("exception in " + name + ".init()", t);
					}
				} catch (InstantiationException e) {
					ZimbraLog.zimlet.warn("exception occurred initializing extension " + name, e);
				} catch (IllegalAccessException e) {
					ZimbraLog.zimlet.warn("exception occurred initializing extension " + name, e);
				} catch (ClassNotFoundException e) {
					ZimbraLog.zimlet.warn("exception occurred initializing extension " + name, e);
				}
				
			}
		}
	}

	public static synchronized void destroyAll() {
		ZimbraLog.zimlet.info("Destroying extensions");
		for (ListIterator iter = sInitializedExtensions.listIterator(sInitializedExtensions.size());
			iter.hasPrevious();)
		{
			ZimbraExtension ext = (ZimbraExtension)iter.previous();
			try {
				ext.destroy();
				ZimbraLog.zimlet.info("Destroyed extension: " + 
						ext.getClass().getName() + "@" + ext.getClass().getClassLoader());
				iter.remove();
			} catch (Throwable t) {
				ZimbraLog.zimlet.warn("exception in " + ext.getClass().getName() + ".destroy()", t);
			}
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
		attrs.put(Provisioning.A_zimbraZimletContentObject,   zd.getContentObjectAsXML());
		attrs.put(Provisioning.A_zimbraZimletPanelItem,       zd.getPanelItemAsXML());
		attrs.put(Provisioning.A_zimbraZimletScript,          zd.getScripts());
		return attrs;
	}
	
	public static void installZimlet(String zimletRoot, String zimlet) throws IOException, ZimletException {
		ZimletFile zf = new ZimletFile(zimlet);
		ZimletDescription zd = new ZimletDescription(zf.getZimletDescription());
		String zimletName = zd.getName();
		
		Map attrs = descToMap(zd);
		
		String configStr = zf.getZimletConfig();
		if (configStr != null) {
			ZimletConfig config = new ZimletConfig(configStr);
			attrs.put(Provisioning.A_zimbraZimletHandlerConfig, config.toXMLString());
		}
		
		// add zimlet entry to ldap
		Provisioning prov = Provisioning.getInstance();
		try {
			prov.createZimlet(zimletName, attrs);
		} catch (ServiceException se) {
			throw ZimletException.CANNOT_CREATE(zimletName, se.getCause().getMessage());
		}

		// install the files
		File zimletDir = new File(zimletRoot + File.separatorChar + zimletName);
		FileUtil.mkdirs(zimletDir);

		String[] files = zf.getAllEntryNames();
		for (int i = 0; i < files.length; i++) {
			String f = files[i];
			if (f.endsWith("/") || f.endsWith("\\")) {
				FileUtil.mkdirs(new File(zimletDir, f));
			} else {
				writeFile(zf.getEntryContent(f), new File(zimletDir, f));
			}
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
		String config = zf.getZimletConfig();
		System.out.println(config);
	}
	
	public static void installConfig(String config) throws IOException, ZimletException {
		ZimletConfig zc = new ZimletConfig(new File(config));
		String zimletName = zc.getName();
		String configString = zc.toXMLString();
		Provisioning prov = Provisioning.getInstance();
		try {
			prov.updateZimletConfig(zimletName, configString);
		} catch (Exception e) {
			throw ZimletException.INVALID_ZIMLET_CONFIG("cannot update Zimlet config for "+zimletName);
		}
	}
	
	private static final String ZCSROOT = "c:/opt/zimbra/tomcat";
	
	private static final int INSTALL_ZIMLET = 10;
	private static final int UNINSTALL_ZIMLET = 11;
	private static final int UPGRADE_ZIMLET = 12;
	private static final int ACTIVATE_ZIMLET = 13;
	private static final int DEACTIVATE_ZIMLET = 14;
	private static final int DUMP_CONFIG = 15;
	private static final int INSTALL_CONFIG = 16;
	
	private static final String INSTALL_CMD = "install";
	private static final String UNINSTALL_CMD = "uninstall";
	private static final String UPGRADE_CMD = "upgrade";
	private static final String ACTIVATE_CMD = "activate";
	private static final String DEACTIVATE_CMD = "deactivate";
	private static final String DUMP_CONFIG_CMD = "config";
	private static final String INSTALL_CONFIG_CMD = "configure";
	
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
	}
	
	private static void usage() {
		System.out.println("zimlet: [command] [ zimlet.zip | config.xml ]");
		System.out.println("\tinstall - installs the zimlet");
		System.out.println("\tuninstall - uninstalls the zimlet");
		System.out.println("\tactivate - activates the zimlet");
		System.out.println("\tdeactivate - deactivates the zimlet");
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
		
		String zimletRoot = ZCSROOT + File.separatorChar + "zimlet";
		String zimlet = args[1];
		
		int cmd = lookupCmd(args[0]);
		try {
			switch (cmd) {
			case INSTALL_ZIMLET:
				installZimlet(zimletRoot, zimlet);
				break;
			case UNINSTALL_ZIMLET:
				uninstallZimlet(zimlet);
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
