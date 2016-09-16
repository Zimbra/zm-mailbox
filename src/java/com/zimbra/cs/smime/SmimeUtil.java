package com.zimbra.cs.smime;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.extension.ExtensionUtil;

public abstract class SmimeUtil {

    private static SmimeUtil sInstance;

    public static SmimeUtil getInstance() {
        if (sInstance == null) {
            synchronized (SmimeUtil.class) {
                if (sInstance != null) {
                    return sInstance;
                }

                String className = "com.zimbra.cs.smime.SmimeCryptoUtil";
                try {
                    sInstance = (SmimeUtil) ExtensionUtil.findClass(className).newInstance();
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    sInstance = new DefaultSmimeUtil();
                }
            }
        }
        return sInstance;
    }

    public abstract Map <X509Certificate, PrivateKey> extractPublicAndPrivateKey(String pkcs12, String password) throws ServiceException;

    public abstract String encryptPrivateKey(PrivateKey privateKey, String accountId) throws ServiceException;

    public abstract PrivateKey decryptPrivateKey(String encryptedkey, String accountId) throws ServiceException;

    public abstract X509Certificate getPublicCertificate(Account account) throws ServiceException;

    public abstract PrivateKey getPrivateKey(Account account) throws ServiceException;

    public abstract void signMessage(MimeMessage mime,  X509Certificate publicCert, PrivateKey privKey) throws ServiceException;

    public abstract Boolean verifySignedMessage(MimeMessage mime) throws ServiceException;

    public abstract void signAndEncryptMessage(MimeMessage mime, X509Certificate senderCert,
            PrivateKey senderPrivKey, X509Certificate recieverCert) throws ServiceException;

    public abstract Boolean decryptAndVerifySignedMessage(MimeMessage mime, X509Certificate publicCert, PrivateKey privKey) throws ServiceException;

}
