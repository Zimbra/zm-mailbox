/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.ldap.unboundid;

import javax.net.SocketFactory;

import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapServerConfig;

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
        connOpts.setAbandonOnTimeout(ldapConfig.isAbandonOnTimeout());

        return connOpts;
    }

}
