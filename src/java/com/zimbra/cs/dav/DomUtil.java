/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.zimbra.common.util.ZimbraLog;

public class DomUtil {
	public static void writeDocumentToStream(Document doc, OutputStream out) throws IOException {
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setTrimText(false);
		format.setOmitEncoding(false);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XMLWriter writer = new XMLWriter(baos, format);
		writer.write(doc);
		byte[] msg = baos.toByteArray();
		if (ZimbraLog.dav.isDebugEnabled())
			ZimbraLog.dav.debug(new String(msg, "UTF-8"));
		out.write(msg);
	}
}
