package com.zimbra.cs.service.authenticator;

import java.security.Principal;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;


public class ClientCertAuthenticator extends SSOAuthenticator {

    public ClientCertAuthenticator(HttpServletRequest req, HttpServletResponse resp) {
        super(req, resp);
    }
    
    public String getAuthMethod() {
        return "ClientCert";
    }

    @Override
    public boolean sentResponse() {
        return false;
    }
    
    @Override
    public ZimbraPrincipal authenticate() throws ServiceException {
        
        Principal principal = getPrincipal();
        Account acct = getAccountByPrincipal(principal);
        
        return new ZimbraPrincipal(principal.getName(), acct);
    }
    
    private Principal getPrincipal() throws ServiceException {
        X509Certificate[] certs = (X509Certificate[])req.getAttribute("javax.servlet.request.X509Certificate");
            
        if (certs==null || certs.length==0 || certs[0]==null) {
            throw AuthFailedServiceException.AUTH_FAILED("no client certificate", (Throwable)null);
        }
        
        validateClientCert(certs);
        
        Principal principal = certs[0].getSubjectDN();
        
        if (principal == null) {
            throw AuthFailedServiceException.AUTH_FAILED("missing subject in cert", (Throwable)null);
        }
        
        return principal;
    }
    
    private Account getAccountByPrincipal(Principal principal) throws ServiceException {
        String x509SubjectDN = principal.getName();
        if (x509SubjectDN == null) {
            throw AuthFailedServiceException.AUTH_FAILED("missing name in principal", (Throwable)null);
        }
        
        try {
            LdapName dn = new LdapName(x509SubjectDN);
            List<Rdn> rdns = dn.getRdns();
            
            for (Rdn rdn : rdns) {
                String type = rdn.getType();
                
                // recognize only email address for now
                if ("EMAILADDRESS".equals(type)) {
                    Object value = rdn.getValue();
                    if (value != null) {
                        String email = value.toString();
                        Account acct = Provisioning.getInstance().get(AccountBy.name, email);
                        if (acct != null) {
                            return acct;
                        } else {
                            ZimbraLog.account.debug("account not found: " + email);
                        }
                    }
                }
            }
        } catch (InvalidNameException e) {
            throw AuthFailedServiceException.AUTH_FAILED("invalid X509 subject: " + x509SubjectDN, e);
        }
        
        return null;
    }
    
    private String getSubjectDNForLogging(X509Certificate cert) {
        String subjectDn = null;
        Principal principal = cert.getSubjectDN();
        if (principal != null) {
            subjectDn = principal.getName();
        }
        
        if (subjectDn == null) {
            subjectDn = "";
        }
        
        return subjectDn;
    }
    
    private void validateClientCert(X509Certificate[] certs) throws ServiceException {
        // CertificateValidator.validateClientCert(certs);
        for (X509Certificate cert : certs) {
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException e) {
                throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "client certificate expired", e);
            } catch (CertificateNotYetValidException e) {
                throw AuthFailedServiceException.AUTH_FAILED(getSubjectDNForLogging(cert), "client certificate not yet valid", e);
            }
        }
    }

}
