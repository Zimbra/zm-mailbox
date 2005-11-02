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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.zimbra.soap.Element;

/**
 * 
 * @author jylee
 *
 */
public class ZimletDescription extends ZimletMeta {
	
	public static final String ZIMLET_REGEX_EXTENSION_CLASS = "com.zimbra.cs.zimlet.handler.RegexHandler";
	
	private String mDescription;
	private List   mScripts;
	private String mContentObject;
	private String mPanelItem;
	private String mExtensionClass;
	private String mKeyword;
	private String mHasKeyword;
	private String mStoreMatched;
	private String mRegexString;
	
	private static final String TRUE_STR = "TRUE";
	private static final String FALSE_STR = "FALSE";
	
	public ZimletDescription(File desc) throws ZimletException {
		super(desc);
	}

	public ZimletDescription(String desc) throws ZimletException {
		super(desc);
	}
	
	protected void initialize() {
		mScripts = new ArrayList();
		mHasKeyword = FALSE_STR;
		mStoreMatched = FALSE_STR;
	}

	protected void validateElement(Element elem) throws ZimletException {
		if (elem.getName().equals(ZIMLET_TAG_CONTENT_OBJECT)) {
			mContentObject = elem.toString();
		} else if (elem.getName().equals(ZIMLET_TAG_PANEL_ITEM)) {
			mPanelItem = elem.toString();
		} else if (elem.getName().equals(ZIMLET_TAG_SERVER_EXTENSION)) {
			parseServerExtension(elem);
		} else if (elem.getName().equals(ZIMLET_TAG_DESCRIPTION)) {
			mDescription = elem.getText();
		} else if (elem.getName().equals(ZIMLET_TAG_SCRIPT)) {
			mScripts.add(elem.getText());
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
			} else if (elem.getName().equals(ZIMLET_TAG_REGEX)) {
				mExtensionClass = ZIMLET_REGEX_EXTENSION_CLASS;
				mRegexString = elem.getText();
			}
		}
	}
	
	public String getDescription() {
		assert(mTopElement != null);
		return mDescription;
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
		if (mScripts.isEmpty()) {
			return null;
		}
		return (String[]) mScripts.toArray(new String[0]);
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
	
	public String getRegexString() {
		return mRegexString;
	}
}
