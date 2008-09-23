/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.dav;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class DomUtil {
	public static byte[] getBytes(Document doc) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writeDocumentToStream(doc, baos);
		byte[] msg = baos.toByteArray();
		return msg;
	}
	public static void writeDocumentToStream(Document doc, OutputStream out) throws IOException {
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setTrimText(false);
		format.setOmitEncoding(false);
		XMLWriter writer = new XMLWriter(out, format);
		writer.write(doc);
	}
}
