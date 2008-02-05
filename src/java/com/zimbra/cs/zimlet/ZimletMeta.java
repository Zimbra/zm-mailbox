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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.dom4j.DocumentException;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.Element;

/**
 * Parses the Zimlet description files, <zimlet>.xml and config.xml.
 * 
 * @author jylee
 *
 */
public abstract class ZimletMeta {
	/* top level */
	public static final String ZIMLET_TAG_ZIMLET = "zimlet";

	/* first level */
	public static final String ZIMLET_ATTR_VERSION         = "version";
	public static final String ZIMLET_ATTR_DESCRIPTION     = "description";
	public static final String ZIMLET_ATTR_NAME            = "name";
	public static final String ZIMLET_ATTR_EXTENSION       = "extension";

	public static final String ZIMLET_TAG_SCRIPT           = "include";
	public static final String ZIMLET_TAG_CONTENT_OBJECT   = "contentObject";
	public static final String ZIMLET_TAG_PANEL_ITEM       = "panelItem";
	
	/* for serverExtension branch */
	public static final String ZIMLET_TAG_SERVER_EXTENSION = "serverExtension";
	public static final String ZIMLET_ATTR_HAS_KEYWORD     = "hasKeyword";
	public static final String ZIMLET_ATTR_MATCH_ON        = "matchOn";
	public static final String ZIMLET_ATTR_EXTENSION_CLASS = "extensionClass";
	public static final String ZIMLET_ATTR_REGEX           = "regex";
	
	/* config description file */
	public static final String ZIMLET_TAG_CONFIG           = "zimletConfig";
	
	public static final String ZIMLET_TAG_GLOBAL           = "global";
	public static final String ZIMLET_TAG_HOST             = "host";
	public static final String ZIMLET_TAG_PROPERTY         = "property";

	public static final String ZIMLET_TAG_TARGET		   = "target";
	protected Element mTopElement;
	
	protected String mName;
	protected Version mVersion;
	protected String mDescription;
	protected boolean mIsExtension;

	protected String mRawXML;
	protected String mGeneratedXML;
	
	protected ZimletMeta() {
		// empty
	}
	
	public ZimletMeta(File f) throws ZimletException {
		this(readFile(f));
	}

	public ZimletMeta(String meta) throws ZimletException {
		initialize();
		if (meta == null) {
			return;
		}
		try {
			mTopElement = Element.parseXML(meta);
			mRawXML = meta;
		} catch (DocumentException de) {
			throw ZimletException.INVALID_ZIMLET_DESCRIPTION("Cannot parse Zimlet description: "+de.getMessage());
		}

		validate();
	}

	private static String readFile(File f) throws ZimletException {
		try {
			return new String(ByteUtil.getContent(new FileInputStream(f), -1));
		} catch (IOException ie) {
			throw ZimletException.INVALID_ZIMLET_DESCRIPTION("Cannot find Zimlet description file: " + f.getName());
		}
	}
	
	protected void validate() throws ZimletException {
		if (mTopElement == null) {
			throw ZimletException.INVALID_ZIMLET_DESCRIPTION("Null DOM element");
		}
		String name = mTopElement.getName();
		if (!name.equals(ZIMLET_TAG_ZIMLET) && !name.equals(ZIMLET_TAG_CONFIG)) {
			throw ZimletException.INVALID_ZIMLET_DESCRIPTION("Top level tag not recognized " + name);
		}
		
		mName = mTopElement.getAttribute(ZIMLET_ATTR_NAME, "");
		mVersion = new Version(mTopElement.getAttribute(ZIMLET_ATTR_VERSION, ""));
		mDescription = mTopElement.getAttribute(ZIMLET_ATTR_DESCRIPTION, "");
		try {
			mIsExtension = mTopElement.getAttributeBool(ZIMLET_ATTR_EXTENSION, false);
		} catch (Exception se) {
			mIsExtension = false;
		}

		Iterator iter = mTopElement.listElements().iterator();
		while (iter.hasNext()) {
			validateElement((Element) iter.next());
		}
	}
	
	public String getName() {
		assert(mTopElement != null);
		return mName;
	}
	
	public Version getVersion() {
		assert(mTopElement != null);
		return mVersion;
	}
	
	public String getDescription() {
		assert(mTopElement != null);
		return mDescription;
	}
	
	public boolean isExtension() {
		assert(mTopElement != null);
		return mIsExtension;
	}

	/*
	 * returns JSON representation of the parsed DOM tree.
	 */
	public String toJSONString() {
		return toString(Element.JSONElement.mFactory);
	}

	/*
	 * returns XML representation of the parsed DOM tree.
	 */
	public String toXMLString() {
		return toString(Element.XMLElement.mFactory);
	}
	
	public String getRawXML() {
		return mRawXML;
	}
	
	/*
	 * returns either XML or JSON representation of parsed and possibly modified DOM tree.
	 */
	public String toString(Element.ElementFactory f) {
		try {
			if (mGeneratedXML == null) {
				mGeneratedXML = mTopElement.toString();
			}
			return Element.parseXML(mGeneratedXML, f).toString();
		} catch (Exception e) {
			ZimbraLog.zimlet.warn("error parsing the Zimlet file "+mName);
		}
		return "";
	}
	
	/*
	 * attaches the DOM tree underneath the Element passed in.
	 */
	public void addToElement(Element elem) throws ZimletException {
		try {
			// TODO: cache parsed structure or result or both.
			Element newElem = Element.parseXML(toXMLString(), elem.getFactory());
			elem.addElement(newElem);
		} catch (DocumentException de) {
			throw ZimletException.ZIMLET_HANDLER_ERROR("cannot parse the dom tree");
		}
	}
	
	protected abstract void initialize() throws ZimletException;
	protected abstract void validateElement(Element e) throws ZimletException;
}
