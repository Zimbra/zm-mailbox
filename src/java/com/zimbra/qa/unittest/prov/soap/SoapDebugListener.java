package com.zimbra.qa.unittest.prov.soap;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport.HttpDebugListener;

public class SoapDebugListener implements HttpDebugListener {
    
    private enum Level {
        OFF,
        HEADER,
        BODY,
        ALL;
        
        private static boolean needsHeader(Level level) {
            return level == Level.ALL || level == Level.HEADER;
        }
        
        private static boolean needsBody(Level level) {
            return level == Level.ALL || level == Level.BODY;
        }
    }
    
    private static Level level = Level.BODY;
    
    SoapDebugListener() {
    }
    
    @Override
    public void receiveSoapMessage(PostMethod postMethod, Element envelope) {
        if (level == Level.OFF) {
            return;
        }
        
        System.out.println();
        System.out.println("=== Response ===");
        
        if (Level.needsHeader(level)) {
            Header[] headers = postMethod.getResponseHeaders();
            for (Header header : headers) {
                System.out.println(header.toString().trim()); // trim the ending crlf
            }
            System.out.println();
        }
        
        if (Level.needsBody(level)) {
            System.out.println(envelope.prettyPrint());
        }
    }

    @Override
    public void sendSoapMessage(PostMethod postMethod, Element envelope) {
        if (level == Level.OFF) {
            return;
        }
        
        System.out.println();
        System.out.println("=== Request ===");
        
        if (Level.needsHeader(level)) {
            try {
                URI uri = postMethod.getURI();
                System.out.println(uri.toString());
            } catch (URIException e) {
                e.printStackTrace();
            }
            
            Header[] headers = postMethod.getRequestHeaders();
            for (Header header : headers) {
                System.out.println(header.toString().trim()); // trim the ending crlf
            }
            System.out.println();
        }
        
        if (Level.needsBody(level)) {
            System.out.println(envelope.prettyPrint());
        }
    }
}
