/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.html;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Makes xhtml and svg content safe for display
 * @author jpowers
 *
 */
public class XHtmlDefang extends AbstractDefang{

    @Override
    public void defang(InputStream is, boolean neuterImages, Writer out)
            throws IOException {
        InputSource inputSource = new InputSource(is);
        defang(inputSource, neuterImages, out);
    }

    @Override
    public void defang(Reader reader, boolean neuterImages, Writer out)
            throws IOException {
        InputSource inputSource = new InputSource(reader);
        defang(inputSource, neuterImages, out);
    }

    protected void defang(InputSource is, boolean neuterImages, Writer out)
            throws IOException {
        //get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            //get a new instance of parser            
            SAXParser sp = spf.newSAXParser();
            
            XHtmlDocumentHandler handler = new XHtmlDocumentHandler(out);
            //parse the file and also register this class for call backs
            sp.parse(is, handler);

        }catch(SAXException se) {
            se.printStackTrace();
        }catch(ParserConfigurationException pce) {
            pce.printStackTrace();
        }catch (IOException ie) {
            ie.printStackTrace();
        }
        
        
    }


    
}
