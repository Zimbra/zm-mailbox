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
