/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.html;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.filters.DefaultFilter;
import org.cyberneko.html.filters.Writer;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;

public class HtmlDetag extends DefaultFilter {
	
//	private static final Map<String, String> startElementMap = new HashMap<String, String>();
//	private static final Map<String, String> endElementMap = new HashMap<String, String>();
//	
//	private static void addElementMap(String elementName, String startReplace) {
//		addElementMap(elementName, startReplace, null);
//	}
//	
//	private static void addElementMap(String elementName, String startReplace, String endReplace) {
//		if (startReplace != null) {
//			startElementMap.put(elementName, startReplace);
//		}
//		if (endReplace != null) {
//			endElementMap.put(elementName, endReplace);
//		}
//	}
//	
//	static {
//		addElementMap("br", "\n");
//		addElementMap("p", "\n");
//	}

	@Override
	public void comment(XMLString text, Augmentations augs) throws XNIException {
		//super.comment(text, augs);
	}

	@Override
	public void startElement(QName element, XMLAttributes attributes,
			Augmentations augs) throws XNIException {
		//super.startElement(element, attributes, augs);
	}
	
	@Override
	public void endElement(QName element, Augmentations augs)
			throws XNIException {
		//super.endElement(element, augs);
	}

	@Override
	public void emptyElement(QName element, XMLAttributes attributes,
			Augmentations augs) throws XNIException {
		//super.emptyElement(element, attributes, augs);
	}

	private static class UnescapeWriter extends Writer {
		
		private static final Map<String, Integer> entityMap = new HashMap<String, Integer>();
		static {
			entityMap.put("quot", 34);
			entityMap.put("amp", 38);
			entityMap.put("apos", 39);
			entityMap.put("frasl", 47);
			entityMap.put("lt", 60);
			entityMap.put("gt", 62);
			entityMap.put("ndash", 150);
			entityMap.put("mdash", 151);
			entityMap.put("nbsp", 160);
			entityMap.put("iexcl", 161);
			entityMap.put("cent", 162);
			entityMap.put("pound", 163);
			entityMap.put("curren", 164);
			entityMap.put("yen", 165);
			entityMap.put("brvbar", 166);
			entityMap.put("brkbar", 166);
			entityMap.put("sect", 167);
			entityMap.put("uml", 168);
			entityMap.put("die", 168);
			entityMap.put("copy", 169);
			entityMap.put("ordf", 170);
			entityMap.put("laquo", 171);
			entityMap.put("not", 172);
			entityMap.put("shy", 173);
			entityMap.put("reg", 174);
			entityMap.put("macr", 175);
			entityMap.put("hibar", 175);
			entityMap.put("deg", 176);
			entityMap.put("plusmn", 177);
			entityMap.put("sup2", 178);
			entityMap.put("sup3", 179);
			entityMap.put("acute", 180);
			entityMap.put("micro", 181);
			entityMap.put("para", 182);
			entityMap.put("middot", 183);
			entityMap.put("cedil", 184);
			entityMap.put("sup1", 185);
			entityMap.put("ordm", 186);
			entityMap.put("raquo", 187);
			entityMap.put("frac14", 188);
			entityMap.put("frac12", 189);
			entityMap.put("frac34", 190);
			entityMap.put("iquest", 191);
			entityMap.put("Agrave", 192);
			entityMap.put("Aacute", 193);
			entityMap.put("Acirc", 194);
			entityMap.put("Atilde", 195);
			entityMap.put("Auml", 196);
			entityMap.put("Aring", 197);
			entityMap.put("AElig", 198);
			entityMap.put("Ccedil", 199);
			entityMap.put("Egrave", 200);
			entityMap.put("Eacute", 201);
			entityMap.put("Ecirc", 202);
			entityMap.put("Euml", 203);
			entityMap.put("Igrave", 204);
			entityMap.put("Iacute", 205);
			entityMap.put("Icirc", 206);
			entityMap.put("Iuml", 207);
			entityMap.put("ETH", 208);
			entityMap.put("Ntilde", 209);
			entityMap.put("Ograve", 210);
			entityMap.put("Oacute", 211);
			entityMap.put("Ocirc", 212);
			entityMap.put("Otilde", 213);
			entityMap.put("Ouml", 214);
			entityMap.put("times", 215);
			entityMap.put("Oslash", 216);
			entityMap.put("Ugrave", 217);
			entityMap.put("Uacute", 218);
			entityMap.put("Ucirc", 219);
			entityMap.put("Uuml", 220);
			entityMap.put("Yacute", 221);
			entityMap.put("THORN", 222);
			entityMap.put("szlig", 223);
			entityMap.put("agrave", 224);
			entityMap.put("aacute", 225);
			entityMap.put("acirc", 226);
			entityMap.put("atilde", 227);
			entityMap.put("auml", 228);
			entityMap.put("aring", 229);
			entityMap.put("aelig", 230);
			entityMap.put("ccedil", 231);
			entityMap.put("egrave", 232);
			entityMap.put("eacute", 233);
			entityMap.put("ecirc", 234);
			entityMap.put("euml", 235);
			entityMap.put("igrave", 236);
			entityMap.put("iacute", 237);
			entityMap.put("icirc", 238);
			entityMap.put("iuml", 239);
			entityMap.put("eth", 240);
			entityMap.put("ntilde", 241);
			entityMap.put("ograve", 242);
			entityMap.put("oacute", 243);
			entityMap.put("ocirc", 244);
			entityMap.put("otilde", 245);
			entityMap.put("ouml", 246);
			entityMap.put("divide", 247);
			entityMap.put("oslash", 248);
			entityMap.put("ugrave", 249);
			entityMap.put("uacute", 250);
			entityMap.put("ucirc", 251);
			entityMap.put("uuml", 252);
			entityMap.put("yacute", 253);
			entityMap.put("thorn", 254);
			entityMap.put("yuml", 255);
		}

	    public UnescapeWriter(java.io.Writer writer, String encoding) {
	        super(writer, encoding);
	    }
	    
	    protected void printEntity(String name) {
	    	Integer num = entityMap.get(name);
	    	if (num != null) {
	    		fPrinter.print((char)num.intValue());
	    	} else {
	    		fPrinter.print("?");
	    	}
	    	fPrinter.flush();
	    }
	}

	public String detag(String html) {
		StringWriter out = new StringWriter();
        UnescapeWriter writer = new UnescapeWriter(out, "utf-8");
        XMLDocumentFilter[] filters = { this, writer };

        XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
        parser.setProperty("http://cyberneko.org/html/properties/names/elems", "match"); 
        parser.setFeature("http://cyberneko.org/html/features/balance-tags", false); 
        parser.setFeature("http://xml.org/sax/features/namespaces", false);
        
        XMLInputSource source = new XMLInputSource(null, null, null, new StringReader(html), null);
        try {
        	parser.parse(source);
        } catch (Exception x) {
        	ZimbraLog.misc.warn("Can't detag HTML [" + html + "]");
        }
        return out.toString(); //return whatever has been done
	}
 	
	public static void main(String[] args) throws IOException {
        String html = new String(ByteUtil.getContent(new File(args[0])));
        System.out.println(new HtmlDetag().detag(html));
	}
}
