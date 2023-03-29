package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.service.util.SecretKey;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.Map;

public class GenerateSecretKey extends AdminDocumentHandler {
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        String randomString = SecretKey.generateRandomString();
        Provisioning.getInstance().getConfig().setSecretKeyForMailRecall(randomString);
        flushCache();

        Element response = zsc.createElement(AdminConstants.GENERATE_SECRET_KEY_RESPONSE);
        return response;
    }

    private void flushCache() {
        try {
            SoapProvisioning sp = new SoapProvisioning();
            sp.soapSetURI(SoapProvisioning.getLocalConfigURI());
            sp.soapZimbraAdminAuthenticate();
            sp.flushCache("config", null, true);
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("Encountered exception during FlushCache after creating Secret Key", e);
        }
    }

}

