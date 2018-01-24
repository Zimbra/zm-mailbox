/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.html;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import com.zimbra.common.util.Constants;

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
        try {
            //get a factory
            SAXParserFactory spf = makeSAXParserFactory();
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

    public static SAXParserFactory makeSAXParserFactory() throws SAXNotRecognizedException, SAXNotSupportedException, ParserConfigurationException {
        //get a factory
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // XXE attack prevention
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature(Constants.DISALLOW_DOCTYPE_DECL, true);
        factory.setFeature(Constants.EXTERNAL_GENERAL_ENTITIES, false);
        factory.setFeature(Constants.EXTERNAL_PARAMETER_ENTITIES, false);
        factory.setFeature(Constants.LOAD_EXTERNAL_DTD, false);
        return factory;
    }
}
