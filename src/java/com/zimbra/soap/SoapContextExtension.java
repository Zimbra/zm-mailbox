/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.soap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.session.SoapSession;

public abstract class SoapContextExtension {
	
	
	private static List<SoapContextExtension> sExtensions = Collections.synchronizedList(new ArrayList<SoapContextExtension>());
	
	public static void register(String name, SoapContextExtension sce) {
		synchronized (sExtensions) {
			ZimbraLog.soap.info("Adding context extension: " + name);
			sExtensions.add(sce);
		}
	}
	
	public static void addExtensionHeaders(Element context, ZimbraSoapContext zsc, SoapSession session) throws ServiceException {
		SoapContextExtension[] exts = null;
		synchronized (sExtensions) {
			exts = new SoapContextExtension[sExtensions.size()];
			sExtensions.toArray(exts); //make a copy so that we keep lock on addExtensionHeader calls
		}
		for (SoapContextExtension sce : exts) {
			sce.addExtensionHeader(context, zsc, session);
		}
	}

	public abstract void addExtensionHeader(Element context, ZimbraSoapContext zsc,  SoapSession session) throws ServiceException;
}
