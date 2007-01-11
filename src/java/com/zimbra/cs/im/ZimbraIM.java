package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.interceptor.InterceptorManager;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;

public class ZimbraIM {
    
    private static boolean sRunning = false;
    
    public synchronized static void startup() throws ServiceException {
        try {
            System.setProperty("wildfireHome", "/opt/zimbra");
            
            ArrayList<String> domainStrs = new ArrayList<String>();
            
            String defaultDomain = Provisioning.getInstance().getConfig().getAttr(Provisioning.A_zimbraDefaultDomainName, null);
            if (defaultDomain != null) {
                ZimbraLog.im.info("Setting default XMPP domain to: "+defaultDomain);
                domainStrs.add(defaultDomain);
            } 
            List<Domain> domains = Provisioning.getInstance().getAllDomains();
            for (Domain d : domains) {
                domainStrs.add(d.getName());
            }
            
            // set the special msgs ClassLoader -- so that WF looks in our conf/msgs directory
            // for its localization .properties bundles.
            org.jivesoftware.util.LocaleUtils.sMsgsClassLoader = com.zimbra.cs.util.L10nUtil.getMsgClassLoader();             
            
            XMPPServer srv = new XMPPServer(domainStrs);
            InterceptorManager.getInstance().addInterceptor(new com.zimbra.cs.im.PacketInterceptor());
            
            sRunning = true;
        } catch (Exception e) { 
            ZimbraLog.system.warn("Could not start XMPP server: " + e.toString());
            e.printStackTrace();
        }
    }
    
    public synchronized static void shutdown() {
        XMPPServer.getInstance().stop();
        if (sRunning) {
            sRunning = false;
        }
    }

}
