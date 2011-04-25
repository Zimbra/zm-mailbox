package com.zimbra.cs.ldap.unboundid;

import javax.net.SocketFactory;

import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.zimbra.cs.ldap.LdapConfig;
import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapTODO.TODO;

public class LdapConnUtil {

    static SocketFactory getSocketFactory(LdapConnType connType, boolean allowUntrustedCerts) throws LdapException {
        
        if (connType == LdapConnType.LDAPS) {
            return LdapSSLUtil.createSSLSocketFactory(allowUntrustedCerts);
        } else {
            // return null for all other cases to use the java default SocketFactory.
            // STARTTLS will use a plain socket first, then upgrade the plain connection 
            // to TLS.  It will be handled via a SSLContext with either a 
            // StartTLSPostConnectProcessor(when using connection pool) or a
            // StartTLSExtendedOperation(when not using connection pool)
            return null;
        }
    }
    
    static LDAPConnectionOptions getConnectionOptions(LdapConfig ldapConfig) {
        LDAPConnectionOptions connOpts = new LDAPConnectionOptions();
        
        connOpts.setUseSynchronousMode(true); // TODO: expose in LC?
        connOpts.setFollowReferrals(true);   // TODO: expose in LC?
        connOpts.setConnectTimeoutMillis(ldapConfig.getConnectTimeoutMillis());
        connOpts.setResponseTimeoutMillis(ldapConfig.getReadTimeoutMillis());
        
        return connOpts;
    }

}
