package com.zimbra.cs.im;

import java.io.IOException;
import java.util.Map;

import org.xmpp.packet.JID;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.cs.im.interop.Interop;
import com.zimbra.cs.im.interop.InteropRegistrationProvider;

public class InteropRegistrationProviderImpl implements InteropRegistrationProvider {

    public Map<String, String> getIMGatewayRegistration(JID userJID, Interop.ServiceName service) throws IOException {
        try {
            IMPersona persona = IMRouter.getInstance().findPersona(null, IMAddr.fromJID(userJID));
            return persona.getIMGatewayRegistration(service);
        } catch (ServiceException e) {
            throw new IOException("Caught IOException when trying to fetch persona: "+e.toString()+" "+SystemUtil.getStackTrace(e));
        }
    }

    public void putIMGatewayRegistration(JID userJID, Interop.ServiceName service, Map<String, String> data) throws IOException {
        try {
            IMPersona persona = IMRouter.getInstance().findPersona(null, IMAddr.fromJID(userJID));
            persona.setIMGatewayRegistration(service, data);
        } catch (ServiceException e) {
            throw new IOException("Caught IOException when trying to fetch persona: "+e.toString()+" "+SystemUtil.getStackTrace(e));
        }
    }

}
