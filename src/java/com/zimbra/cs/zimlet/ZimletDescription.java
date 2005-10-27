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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.Element;

import org.dom4j.DocumentException;

/**
 * 
 * @author jylee
 *
 */
public class ZimletDescription {
	
	/* top level */
	public static final String ZIMLET_TAG_ZIMLET = "zimlet";

	/* first level */
	public static final String ZIMLET_TAG_NAME             = "name";
	public static final String ZIMLET_TAG_VERSION          = "version";
	public static final String ZIMLET_TAG_DESCRIPTION      = "description";
	public static final String ZIMLET_TAG_SCRIPT           = "script";
	public static final String ZIMLET_TAG_SERVER_EXTENSION = "serverExtension";
	public static final String ZIMLET_TAG_CONTENT_OBJECT   = "contentObject";
	public static final String ZIMLET_TAG_PANEL_ITEM       = "panelItem";
	public static final String ZIMLET_TAG_CONFIG           = "config";
	
	/* for serverExtension branch */
	public static final String ZIMLET_TAG_HAS_KEYWORD      = "hasKeyword";
	public static final String ZIMLET_TAG_MATCH_ON         = "matchOn";
	public static final String ZIMLET_TAG_EXTENSION_CLASS  = "extensionClass";
	
	/* config branch */
	public static final String ZIMLET_TAG_ATTR  = "attr";
	public static final String ZIMLET_ATTR_NAME = "name";
	
	/* skip parsing contentObject and panelItem branch as they are for client. */
	
	private Element mTopElement;
	
	private String mName;
	private String mVersion;
	private String mDescription;
	private String[] mScripts;
	private String mContentObject;
	private String mPanelItem;
	private Properties mConfig;
	private String mExtensionClass;
	private String mKeyword;
	private String mHasKeyword;
	private String mStoreMatched;
	
	private static final String TRUE_STR = "TRUE";
	private static final String FALSE_STR = "FALSE";
	
	public ZimletDescription(File desc) throws ZimletException {
		this(readFile(desc));
	}

	public ZimletDescription(String desc) throws ZimletException {
		try {
			mTopElement = Element.parseXML(desc);
			mConfig = new Properties();
			mHasKeyword = FALSE_STR;
			mStoreMatched = FALSE_STR;
			validate();
		} catch (DocumentException de) {
			throw ZimletException.INVALID_ZIMLET_DESCRIPTION("Cannot parse Zimlet description");
		}
	}

	private static String readFile(File f) throws ZimletException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ByteUtil.copy(new FileInputStream(f), baos);
			return baos.toString();
		} catch (IOException ie) {
			throw ZimletException.INVALID_ZIMLET_DESCRIPTION("Cannot find Zimlet description file");
		}
	}
	
	private void validate() throws ZimletException {
		if (mTopElement == null) {
			throw ZimletException.INVALID_ZIMLET_DESCRIPTION("Null DOM element");
		}
		if (!mTopElement.getName().equals(ZIMLET_TAG_ZIMLET)) {
			throw ZimletException.INVALID_ZIMLET_DESCRIPTION("Top level tag is not " + ZIMLET_TAG_ZIMLET);
		}
		List scriptList = new ArrayList();
		Iterator iter = mTopElement.listElements().iterator();
		while (iter.hasNext()) {
			Element elem = (Element) iter.next();
			if (elem.getName().equals(ZIMLET_TAG_CONTENT_OBJECT)) {
				mContentObject = elem.toString();
			} else if (elem.getName().equals(ZIMLET_TAG_PANEL_ITEM)) {
				mPanelItem = elem.toString();
			} else if (elem.getName().equals(ZIMLET_TAG_SERVER_EXTENSION)) {
				parseServerExtension(elem);
			} else if (elem.getName().equals(ZIMLET_TAG_CONFIG)) {
				parseConfig(elem);
			} else if (elem.getName().equals(ZIMLET_TAG_NAME)) {
				mName = elem.getText();
			} else if (elem.getName().equals(ZIMLET_TAG_DESCRIPTION)) {
				mDescription = elem.getText();
			} else if (elem.getName().equals(ZIMLET_TAG_VERSION)) {
				//parseVersion(elem);
				mVersion = elem.getText();
			} else if (elem.getName().equals(ZIMLET_TAG_SCRIPT)) {
				scriptList.add(elem.getText());
			}
		}
		if (!scriptList.isEmpty()) {
			mScripts = (String[]) scriptList.toArray(new String[0]);
		}
	}
	
	private void parseConfig(Element config) throws ZimletException {
		assert(config.getName().equals(ZIMLET_TAG_CONFIG));
		Iterator iter = config.listElements().iterator();
		while (iter.hasNext()) {
			Element elem = (Element) iter.next();
			if (elem.getName().equals(ZIMLET_TAG_ATTR)) {
				try {
					mConfig.setProperty(elem.getAttribute(ZIMLET_ATTR_NAME),
										elem.getText());
				} catch (ServiceException se) {
					throw ZimletException.INVALID_ZIMLET_DESCRIPTION("Missing name attribute in config for " + elem.getText());
				}
			}
		}
	}
	
	private void parseServerExtension(Element serverExt) throws ZimletException {
		assert(serverExt.getName().equals(ZIMLET_TAG_SERVER_EXTENSION));
		Iterator iter = serverExt.listElements().iterator();
		while (iter.hasNext()) {
			Element elem = (Element) iter.next();
			if (elem.getName().equals(ZIMLET_TAG_HAS_KEYWORD)) {
				mKeyword = elem.getText();
				mHasKeyword = TRUE_STR;
			} else if (elem.getName().equals(ZIMLET_TAG_EXTENSION_CLASS)) {
				mExtensionClass = elem.getText();
			}
		}
	}
	
	private void parseVersion(Element v) throws ZimletException {
		assert(v.getName().equals(ZIMLET_TAG_VERSION));
		String version = v.getText();
		int[] vers = new int[3];
		int start = 0, end;
		for (int i = 0; i < 3; i++) {
			end = version.indexOf('.', start);
			if (end == -1) {
				vers[i] = Integer.parseInt(version.substring(start));
				break;
			}
			vers[i] = Integer.parseInt(version.substring(start, end));
			start = end + 1;
		}
		mVersion = Integer.valueOf(vers[0] * 1000 * 1000 + vers[1] * 1000 + vers[2]).toString();
	}
	
	public String getZimletName() {
		assert(mTopElement != null);
		return mName;
	}
	
	public String getVersion() {
		assert(mTopElement != null);
		return mVersion;
	}
	
	public String getDescription() {
		assert(mTopElement != null);
		return mDescription;
	}
	
	public String getConfig(String key) {
		assert(mConfig != null);
		return mConfig.getProperty(key);
	}
	
	public String getContentObjectAsXML() {
		assert(mTopElement != null);
		return mContentObject;
	}
	
	public String getPanelItemAsXML() {
		assert(mTopElement != null);
		return mPanelItem;
	}
	
	public String getContentObjectAsJSON() {
		assert(mTopElement != null);
		return null;
	}
	
	public String getPanelItemAsJSON() {
		assert(mTopElement != null);
		return null;
	}
	
	public String[] getScripts() {
		return mScripts;
	}
	
	public String getServerExtensionClass() {
		return mExtensionClass;
	}
	
	public String getServerExtensionKeyword() {
		return mKeyword;
	}
	
	public String getServerExtensionHasKeyword() {
		return mHasKeyword;
	}
	
	public String getStoreMatched() {
		return mStoreMatched;
	}
}
