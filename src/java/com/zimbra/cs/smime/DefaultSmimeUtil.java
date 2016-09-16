package com.zimbra.cs.smime;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;

public class DefaultSmimeUtil extends SmimeUtil{

    @Override
    public Map<X509Certificate, PrivateKey> extractPublicAndPrivateKey(String pkcs12, String password) throws ServiceException {
        operationUnsupported();
        return null;
    }

    @Override
    public String encryptPrivateKey(PrivateKey privateKey, String accountId) throws ServiceException {
        operationUnsupported();
        return null;
    }

    @Override
    public PrivateKey decryptPrivateKey(String encryptedkey, String accountId) throws ServiceException {
        operationUnsupported();
        return null;
    }

    @Override
    public X509Certificate getPublicCertificate(Account account) throws ServiceException {
        operationUnsupported();
        return null;
    }

    @Override
    public PrivateKey getPrivateKey(Account account) throws ServiceException {
        operationUnsupported();
        return null;
    }

    @Override
    public void signMessage(MimeMessage mime, X509Certificate publicCert, PrivateKey privKey) throws ServiceException {
        operationUnsupported();
    }

    @Override
    public Boolean verifySignedMessage(MimeMessage mime) throws ServiceException {
        operationUnsupported();
        return null;
    }

    @Override
    public void signAndEncryptMessage(MimeMessage mime, X509Certificate senderCert, PrivateKey senderPrivKey,
            X509Certificate recieverCert) throws ServiceException {
        operationUnsupported();
    }

    @Override
    public Boolean decryptAndVerifySignedMessage(MimeMessage mime, X509Certificate publicCert, PrivateKey privKey) throws ServiceException {
        operationUnsupported();
        return null;
    }

    private void operationUnsupported() throws ServiceException {
        throw ServiceException.FORBIDDEN("Operation not supported");
    }
}
