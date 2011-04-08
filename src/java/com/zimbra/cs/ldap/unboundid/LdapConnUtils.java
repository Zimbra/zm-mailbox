package com.zimbra.cs.ldap.unboundid;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.PostConnectProcessor;
import com.unboundid.ldap.sdk.ServerSet;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.StartTLSPostConnectProcessor;

import com.zimbra.common.net.SocketFactories;
import com.zimbra.cs.ldap.LdapConfig;
import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapTODO.*;

public class LdapConnUtils {
    
    static LDAPConnectionPool createConnectionPool(
            LdapConfig config, LdapHost ldapHost) 
    throws LDAPException {
        ServerSet serverSet = ldapHost.getServerSet();
        BindRequest bindRequest = createBindRequest(config);
    
        PostConnectProcessor postConnectProcessor = null;
        if (ldapHost.getConnectionType() == LdapConnType.STARTTLS) {
            LdapTODO.TODO();
            SSLContext startTLSContext = null;
            postConnectProcessor = new StartTLSPostConnectProcessor(startTLSContext);
        }
        
        return new LDAPConnectionPool(serverSet, bindRequest, 
                config.getConnPoolInitSize(), config.getConnPoolMaxSize(),
                postConnectProcessor);
    }
    
    @TODO  // handle SASL
    private static BindRequest createBindRequest(LdapConfig config) throws LDAPException {
        String bindDN = config.getAdminBindDN();
        
        if (bindDN != null) {
            String bindPassword = config.getAdminBindPassword();
            return new SimpleBindRequest(bindDN, bindPassword);
        } else {
            return null;
        }
        
        /*
        if (bindDN != null) {
            return new SimpleBindRequest(bindDN, pw);
        } else if (saslMechanism != null) {
            return createSASLBindRequest();
        } else {
            return null;
        }
        */
    }

    @TODO
    static SocketFactory getSocketFactory(LdapConnType connType, boolean allowUntrustedCerts) {
        
        if (connType == LdapConnType.LDAPS && allowUntrustedCerts) {
            return SocketFactories.dummySSLSocketFactory(); // do we need to do this?  or is it handled by our SSL framework?
        } else {
            return null;  // use java default
        }
    }
    
}
