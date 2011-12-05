package com.zimbra.qa.unittest.prov.soap;

import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport.HttpDebugListener;

public class SoapDebugListener implements HttpDebugListener {
    private boolean isOn = true;
    
    SoapDebugListener() {
    }
    
    @Override
    public void receiveSoapMessage(PostMethod postMethod, Element envelope) {
        if (!isOn) {
            return;
        }
        
        System.out.println();
        System.out.println("=== Response ===");
        System.out.println(envelope.prettyPrint());
    }

    @Override
    public void sendSoapMessage(PostMethod postMethod, Element envelope) {
        if (!isOn) {
            return;
        }
        
        System.out.println();
        System.out.println("=== Request ===");
        System.out.println(envelope.prettyPrint());
    }
}
