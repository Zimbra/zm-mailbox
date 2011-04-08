package com.zimbra.cs.ldap.unboundid.notused;

import java.net.URL;

import static com.unboundid.util.Debug.debugException;
import static com.unboundid.util.StaticUtils.getExceptionMessage;
/*
import static com.unboundid.util.UtilityMessages.ERR_LDAP_TOOL_CANNOT_READ_BIND_PASSWORD;
import static com.unboundid.util.UtilityMessages.ERR_LDAP_TOOL_UNSUPPORTED_SASL_MECH;
import static com.unboundid.util.UtilityMessages.ERR_LDAP_TOOL_CANNOT_CREATE_KEY_MANAGER;
import static com.unboundid.util.UtilityMessages.ERR_LDAP_TOOL_CANNOT_CREATE_SSL_CONTEXT;
import static com.unboundid.util.UtilityMessages.ERR_LDAP_TOOL_CANNOT_CREATE_SSL_SOCKET_FACTORY;
import static com.unboundid.util.UtilityMessages.ERR_LDAP_TOOL_CANNOT_READ_KEY_STORE_PASSWORD;
import static com.unboundid.util.UtilityMessages.ERR_LDAP_TOOL_CANNOT_READ_TRUST_STORE_PASSWORD;
*/

import java.util.List;
import java.util.Map;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
// import static com.unboundid.util.UtilityMessages.ERR_LDAP_TOOL_START_TLS_FAILED;

import com.unboundid.ldap.sdk.ANONYMOUSBindRequest;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.CRAMMD5BindRequest;
import com.unboundid.ldap.sdk.DIGESTMD5BindRequest;
import com.unboundid.ldap.sdk.EXTERNALBindRequest;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.GSSAPIBindRequest;
import com.unboundid.ldap.sdk.GSSAPIBindRequestProperties;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import com.unboundid.ldap.sdk.PLAINBindRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.RoundRobinServerSet;
import com.unboundid.ldap.sdk.ServerSet;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.SingleServerSet;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.PromptTrustManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;

import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.LdapTODO;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.unboundid.LdapHost;

/*
 * origin: com.unboundid.util.LDAPCommandLineTool
 */
class Connection {

    // config parameters
    LdapHost       host;
    String         bindDN;
    String         bindPassword;
    
    LDAPConnection ldapConn;
    
    // The set of SASL options provided, if any.
    private Map<String,String> saslOptions;
    private SASLMech           saslMechanism;

    // Variables used when creating and authenticating connections.
    private BindRequest bindRequest;
    private ServerSet   serverSet;
    private SSLContext  startTLSContext;
    
    Connection(LdapHost host, String bindDN, String bindPassword) {
        this.host = host;
        this.bindDN = bindDN;
        this.bindPassword = bindPassword;
        
        try {
            ldapConn = getConnection();
        } catch (LDAPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @TODO
    private SSLUtil createSSLUtil() throws LDAPException {
        LdapTODO.TODO();
        return null;
    }
    
    /*
    private SSLUtil createSSLUtil() throws LDAPException {
        if (useSSL.isPresent() || useStartTLS.isPresent()) {
            KeyManager keyManager = null;
            if (keyStorePath.isPresent()) {
                char[] pw = null;
                if (keyStorePassword.isPresent()) {
                    pw = keyStorePassword.getValue().toCharArray();
                } else if (keyStorePasswordFile.isPresent()) {
                    try {
                        pw = keyStorePasswordFile.getNonBlankFileLines().get(0).
                                toCharArray();
                    } catch (Exception e) {
                        debugException(e);
                        throw new LDAPException(ResultCode.LOCAL_ERROR,
                                ERR_LDAP_TOOL_CANNOT_READ_KEY_STORE_PASSWORD.get(
                                getExceptionMessage(e)), e);
                    }
                }
    
                try {
                    keyManager = new KeyStoreKeyManager(keyStorePath.getValue(), pw,
                            keyStoreFormat.getValue(), certificateNickname.getValue());
                } catch (Exception e) {
                    debugException(e);
                    throw new LDAPException(ResultCode.LOCAL_ERROR,
                            ERR_LDAP_TOOL_CANNOT_CREATE_KEY_MANAGER.get(
                            getExceptionMessage(e)), e);
                }
            }

            TrustManager trustManager;
            if (trustAll.isPresent()) {
                trustManager = new TrustAllTrustManager(false);
            } else if (trustStorePath.isPresent()) {
                char[] pw = null;
                if (trustStorePassword.isPresent()) {
                    pw = trustStorePassword.getValue().toCharArray();
                } else if (trustStorePasswordFile.isPresent()) {
                    try {
                      pw = trustStorePasswordFile.getNonBlankFileLines().get(0).
                                toCharArray();
                    } catch (Exception e) {
                        debugException(e);
                        throw new LDAPException(ResultCode.LOCAL_ERROR,
                                ERR_LDAP_TOOL_CANNOT_READ_TRUST_STORE_PASSWORD.get(
                                getExceptionMessage(e)), e);
                    }
                }
            
                trustManager = new TrustStoreTrustManager(trustStorePath.getValue(), pw,
                        trustStoreFormat.getValue(), true);
            } else {
                trustManager = promptTrustManager.get();
                if (trustManager == null) {
                    final PromptTrustManager m = new PromptTrustManager();
                    promptTrustManager.compareAndSet(null, m);
                    trustManager = promptTrustManager.get();
                }
            }

            return new SSLUtil(keyManager, trustManager);
        } else {
            return null;
        }
    }
    */
    
    private LDAPConnectionOptions getConnectionOptions() {
        return new LDAPConnectionOptions();
    }
    
    public ServerSet createServerSet() throws LDAPException
    {
        final SSLUtil sslUtil = createSSLUtil();
    
        SocketFactory socketFactory = null;
        
        // TODO
        /*
        if (connectionType == ConnectionType.LDAPS) {
            try {
                socketFactory = sslUtil.createSSLSocketFactory();
            } catch (Exception e) {
                debugException(e);
                throw new LDAPException(ResultCode.LOCAL_ERROR,
                    ERR_LDAP_TOOL_CANNOT_CREATE_SSL_SOCKET_FACTORY.get(
                         getExceptionMessage(e)), e);
            }
        } else if (connectionType == ConnectionType.STARTTLS) {
            try {
                startTLSContext = sslUtil.createSSLContext();
            } catch (Exception e) {
                debugException(e);
                throw new LDAPException(ResultCode.LOCAL_ERROR,
                    ERR_LDAP_TOOL_CANNOT_CREATE_SSL_CONTEXT.get(
                         getExceptionMessage(e)), e);
            }
        }
        */
        
        if (host.getUrls().size() == 1) {
            LDAPURL url = host.getUrls().get(0);
            return new SingleServerSet(url.getHost(), url.getPort(),
                    socketFactory, getConnectionOptions());
        } else {
            int numUrls = host.getUrls().size();
            
            final String[] hosts = new String[numUrls];
            final int[]    ports = new int[numUrls];
            
            for (int i=0; i < numUrls; i++) {
                LDAPURL url = host.getUrls().get(i);
                hosts[i] = url.getHost();
                ports[i] = url.getPort();
            }
            
            return new RoundRobinServerSet(hosts, ports, socketFactory,
                    getConnectionOptions());
        }
    }
    
    @TODO
    private BindRequest createSASLBindRequest() {
        LdapTODO.TODO();
        return null;
    }
    
    /*
    private BindRequest createSASLBindRequest() {
        if (saslMechanism == SASLMech.SASL_MECH_ANONYMOUS) {
            return new ANONYMOUSBindRequest(saslOptions.get(SASL_OPTION_TRACE));
        } else if (saslMechanism == SASLMech.SASL_MECH_CRAM_MD5) {
            return new CRAMMD5BindRequest(saslOptions.get(SASL_OPTION_AUTH_ID), pw);
        } else if (saslMechanism == SASLMech.SASL_MECH_DIGEST_MD5) {
            return new DIGESTMD5BindRequest(saslOptions.get(SASL_OPTION_AUTH_ID),
                    saslOptions.get(SASL_OPTION_AUTHZ_ID), pw,
                    saslOptions.get(SASL_OPTION_REALM));
        } else if (saslMechanism == SASLMech.SASL_MECH_EXTERNAL) {
            return new EXTERNALBindRequest();
        } else if (saslMechanism == SASLMech.SASL_MECH_GSSAPI) {
            final GSSAPIBindRequestProperties gssapiProperties =
                new GSSAPIBindRequestProperties(saslOptions.get(SASL_OPTION_AUTH_ID), pw);
            gssapiProperties.setAuthorizationID(saslOptions.get(SASL_OPTION_AUTHZ_ID));
            gssapiProperties.setRealm(saslOptions.get(SASL_OPTION_REALM));
            gssapiProperties.setKDCAddress(saslOptions.get(SASL_OPTION_KDC_ADDRESS));

            final String protocol = saslOptions.get(SASL_OPTION_PROTOCOL);
            if (protocol != null) {
                gssapiProperties.setServicePrincipalProtocol(protocol);
            }

            final String debugStr = saslOptions.get(SASL_OPTION_DEBUG);
            if ((debugStr != null) && debugStr.equalsIgnoreCase("true")) {
                gssapiProperties.setEnableGSSAPIDebugging(true);
            }

            return new GSSAPIBindRequest(gssapiProperties);
        } else if (saslMechanism == SASLMech.SASL_MECH_PLAIN) {
            return new PLAINBindRequest(saslOptions.get(SASL_OPTION_AUTH_ID),
                    saslOptions.get(SASL_OPTION_AUTHZ_ID), pw);
        } else {
            throw new LDAPException(ResultCode.NOT_SUPPORTED,
                    ERR_LDAP_TOOL_UNSUPPORTED_SASL_MECH.get(saslMechanism));
        } 
    }
    */
    
    /**
     * Creates the bind request to use to authenticate to the server.
     *
     * @return  The bind request to use to authenticate to the server, or
     *          {@code null} if no bind should be performed.
     *
     * @throws  LDAPException  If a problem occurs while creating the bind
     *                         request.
     */
    private BindRequest createBindRequest() throws LDAPException {
        final String pw = bindPassword;

        if (bindDN != null) {
            return new SimpleBindRequest(bindDN, pw);
        } else if (saslMechanism != null) {
            return createSASLBindRequest();
        } else {
            return null;
        }
    }
   
    public final LDAPConnection getConnection() throws LDAPException {
        if (serverSet == null) {
            serverSet   = createServerSet();
            bindRequest = createBindRequest();
        }
    
        final LDAPConnection connection = serverSet.getConnection();
    
        if (host.getConnectionType() == LdapConnType.STARTTLS) {
            try {
                final ExtendedResult extendedResult =
                    connection.processExtendedOperation(new StartTLSExtendedRequest(startTLSContext));
                if (! extendedResult.getResultCode().equals(ResultCode.SUCCESS)) {
                    throw new LDAPException(extendedResult.getResultCode(),
                            "StartTLS negotiation failed: " + extendedResult.getDiagnosticMessage());
                }
            } catch (LDAPException le) {
                debugException(le);
                connection.close();
                throw le;
            }
        }
    
        try {
            if (bindRequest != null) {
                connection.bind(bindRequest);
            }
        } catch (LDAPException le) {
            debugException(le);
            connection.close();
            throw le;
        }
    
        return connection;
    }

}
