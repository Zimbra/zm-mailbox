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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Feb 10, 2005
 *
 */
package com.zimbra.cs.convert;

import java.io.IOException;
import java.util.Map;

import com.zimbra.cs.localconfig.LC;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TransformationStub {

    private static TransformationStub mInstance;
    
    public static synchronized TransformationStub getInstance() {
        if (mInstance == null) {
            String className = LC.get("convertd_stub_name");
            if ("".equals(className)) {
                mInstance = new TransformationStub();
            } else {
                className = "com.zimbra.cs.convert." + className;
                try {
                    mInstance = (TransformationStub) Class.forName(className).newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException("unable to instantiate " + className, e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("not allowed to create " + className, e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(className + " not found", e);
                }
            }
        }
        return mInstance;
    }
    
    /**
     * Converts the document into HTML/images.
     * 
     * @param doc the source document to convert
     * @param baseURL the base URL to which all hyperlinks in the resultant HTML file are relative
     * @return a URI, usually a file path to the main converted HTML file
     * @throws IOException
     * @throws ConversionException
     */
    public String convert(AttachmentInfo doc, String baseURL)
        throws IOException, ConversionException {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Extracts text from the source stream.
     * 
     * @param in the source stream.
     * @param options the options for text extraction
     * @return the extracted text
     * @throws IOException
     * @throws ConversionException
     */
    public DocumentText extract(AttachmentInfo doc, Map options)
        throws IOException, ConversionException {
        throw new UnsupportedOperationException();
    }
    
    public void init() {
        
    }
    
    public void destroy() {
        
    }
}
