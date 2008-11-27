package com.zimbra.common.util;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import com.zimbra.common.localconfig.LC;

public class SSLSocketFactoryManager {
	
	private static boolean isInitialzied = false;
	
	public synchronized static void init() {
		if (isInitialzied)
			return;
		
		ProtocolSocketFactory socketFactory = null;
		
        String className = LC.zimbra_class_sslprotocolsocketfactory.value();
        if (className != null && !className.equals("")) {
            try {
                socketFactory = (ProtocolSocketFactory)Class.forName(className).newInstance();
            } catch (Exception e) {
                ZimbraLog.security.error("could not instantiate ProtocolSocketFactory interface of class '%s'", className, e);
            }
        }
        
        if (socketFactory == null && LC.ssl_allow_untrusted_certs.booleanValue())
        	socketFactory = new EasySSLProtocolSocketFactory();
        
        if (socketFactory != null) {
        	Protocol https = new Protocol("https", socketFactory, 443);
        	Protocol.registerProtocol("https", https);
        }
	}
}
