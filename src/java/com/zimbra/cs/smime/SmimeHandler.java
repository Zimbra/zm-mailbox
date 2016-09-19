package com.zimbra.cs.smime;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.extension.ExtensionUtil;

public abstract class SmimeHandler {

    private static SmimeHandler sInstance;

    public static SmimeHandler getInstance() throws ServiceException {
        if (sInstance == null) {
            synchronized (SmimeHandler.class) {
                if (sInstance != null) {
                    return sInstance;
                }

                String className = "com.zimbra.cs.smime.SmimeCryptoUtil";
                try {
                    sInstance = (SmimeHandler) ExtensionUtil.findClass(className).newInstance();
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    throw ServiceException.FORBIDDEN("Operation not supported");
                }
            }
        }
        return sInstance;
    }

    public abstract Boolean verifySignedMessage(MimeMessage mime) throws ServiceException;

    public abstract Boolean decryptAndVerifySignedMessage(MimeMessage mime, X509Certificate publicCert,
            PrivateKey privKey) throws ServiceException;

}
