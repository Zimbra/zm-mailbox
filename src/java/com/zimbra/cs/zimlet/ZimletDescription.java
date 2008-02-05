/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zimlet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.soap.Element;

/**
 * 
 * @author jylee
 *
 */
public class ZimletDescription extends ZimletMeta {
	
	public static final String ZIMLET_REGEX_EXTENSION_CLASS = "com.zimbra.cs.zimlet.handler.RegexHandler";
	
	private List<String> mScripts;
	private String mContentObject;
	private String mPanelItem;
	private String mExtensionClass;
	private String mKeyword;
	private String mRegexString;
	private List<String> mTargets;
	
	public ZimletDescription(File desc) throws ZimletException {
		super(desc);
	}

	public ZimletDescription(String desc) throws ZimletException {
		super(desc);
	}
	
	protected void initialize() {
		mScripts = new ArrayList<String>();
		mTargets = new ArrayList<String>();
	}

	protected void validateElement(Element elem) throws ZimletException {
		if (elem.getName().equals(ZIMLET_TAG_CONTENT_OBJECT)) {
			mContentObject = elem.toString();
		} else if (elem.getName().equals(ZIMLET_TAG_PANEL_ITEM)) {
			mPanelItem = elem.toString();
		} else if (elem.getName().equals(ZIMLET_TAG_SERVER_EXTENSION)) {
			parseServerExtension(elem);
		} else if (elem.getName().equals(ZIMLET_TAG_SCRIPT)) {
			parseResource(elem);
		} else if (elem.getName().equals(ZIMLET_TAG_TARGET)) {
			mTargets.add(elem.getText());
		}
	}

	private void parseResource(Element resource) throws ZimletException {
		mScripts.add(resource.getText());
	}
	
	private void parseServerExtension(Element serverExt) throws ZimletException {
		assert(serverExt.getName().equals(ZIMLET_TAG_SERVER_EXTENSION));
		String val = serverExt.getAttribute(ZIMLET_ATTR_HAS_KEYWORD, "");
		if (val.length() > 0) {
			mKeyword = val;
		}
		val = serverExt.getAttribute(ZIMLET_ATTR_EXTENSION_CLASS, "");
		if (val.length() > 0) {
			mExtensionClass = val;
		}
		val = serverExt.getAttribute(ZIMLET_ATTR_REGEX, "");
		if (val.length() > 0) {
			mExtensionClass = ZIMLET_REGEX_EXTENSION_CLASS;
			mRegexString = val;
		}
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
		return mScripts.toArray(new String[0]);
	}
	
	public String getServerExtensionClass() {
		return mExtensionClass;
	}
	
	public String getServerExtensionKeyword() {
		return mKeyword;
	}
	
	public boolean getServerExtensionHasKeyword() {
		return (mKeyword != null);
	}
	
	public String getRegexString() {
		return mRegexString;
	}
	
	public boolean checkTarget(String target) {
		return (mTargets.contains(target));
	}
	
	public List<String> getTargets() {
		return mTargets;
	}
}
