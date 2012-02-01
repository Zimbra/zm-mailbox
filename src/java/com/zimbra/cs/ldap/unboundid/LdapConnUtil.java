/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ldap.unboundid;

import javax.net.SocketFactory;

import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.zimbra.cs.ldap.LdapServerConfig;
import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.LdapException;

public class LdapConnUtil {

    static SocketFactory getSocketFactory(LdapConnType connType, boolean allowUntrustedCerts) 
    throws LdapException {
        if (connType == LdapConnType.LDAPI) {
            return new UnixDomainSocketFactory();
        } else if (connType == LdapConnType.LDAPS) {
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
    
    static LDAPConnectionOptions getConnectionOptions(LdapServerConfig ldapConfig) {
        LDAPConnectionOptions connOpts = new LDAPConnectionOptions();
        
        connOpts.setUseSynchronousMode(true); // TODO: expose in LC?
        connOpts.setFollowReferrals(true);   // TODO: expose in LC?
        connOpts.setConnectTimeoutMillis(ldapConfig.getConnectTimeoutMillis());
        connOpts.setResponseTimeoutMillis(ldapConfig.getReadTimeoutMillis());

        return connOpts;
    }

}
