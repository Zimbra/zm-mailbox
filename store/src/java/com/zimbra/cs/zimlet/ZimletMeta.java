/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.soap.XmlParseException;
import com.zimbra.common.soap.ZimletConstants;

/**
 * Parses the Zimlet description files, <zimlet>.xml and config.xml.
 * 
 * @author jylee
 *
 */
public abstract class ZimletMeta {
	protected Element mTopElement;
	
	protected String mName;
	protected Version mVersion;
	protected String mDescription;
	protected boolean mIsExtension;

	protected String mRawXML;
	protected String mGeneratedXML;
	protected String mZimbraXZimletCompatibleSemVer;
	
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
		} catch (XmlParseException de) {
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
		if (!name.equals(ZimletConstants.ZIMLET_TAG_ZIMLET) && !name.equals(ZimletConstants.ZIMLET_TAG_CONFIG)) {
			throw ZimletException.INVALID_ZIMLET_DESCRIPTION("Top level tag not recognized " + name);
		}
		
		mName = mTopElement.getAttribute(ZimletConstants.ZIMLET_ATTR_NAME, "");
		mVersion = new Version(mTopElement.getAttribute(ZimletConstants.ZIMLET_ATTR_VERSION, ""));
		mDescription = mTopElement.getAttribute(ZimletConstants.ZIMLET_ATTR_DESCRIPTION, "");
		mZimbraXZimletCompatibleSemVer = mTopElement.getAttribute(ZimletConstants.ZIMLET_ATTR_ZIMBRAX_SEMVER, "");
		try {
			mIsExtension = mTopElement.getAttributeBool(ZimletConstants.ZIMLET_ATTR_EXTENSION, false);
		} catch (Exception se) {
			mIsExtension = false;
		}

		@SuppressWarnings("unchecked")
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

    public String getZimbraXCompatibleSemVer() {
        assert (mTopElement != null);
        return mZimbraXZimletCompatibleSemVer;
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
			return W3cDomUtil.parseXML(mGeneratedXML, f).toString();
		} catch (XmlParseException e) {
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
			Element newElem = W3cDomUtil.parseXML(toXMLString(), elem.getFactory());
			elem.addElement(newElem);
		} catch (XmlParseException de) {
			throw ZimletException.ZIMLET_HANDLER_ERROR("cannot parse the dom tree");
		}
	}
	
	protected abstract void initialize() throws ZimletException;
	protected abstract void validateElement(Element e) throws ZimletException;
}
