package com.zimbra.qa.unittest.prov;

import org.junit.Rule;
import org.junit.rules.TestName;

import com.zimbra.cs.ldap.unboundid.InMemoryLdapServer;

public class ProvTest {
    
    public static class SkippedForInMemLdapServerException extends Exception {
        
        public static enum Reason {
            CONNECTION_POOL_HEALTH_CHECK("connection pool health check does not work for InMemoryDirectoryServer"),
            DN_SUBTREE_MATCH_FILTER("entryDN:dnSubtreeMatch filter is not supported by InMemoryDirectoryServer"),
            EXTERNAL_AUTH_STATUS_AUTH_FAILED("external auth STATUS_AUTH_FAILED status is not supported by InMemoryDirectoryServer"),
            EXTERNAL_AUTH_STATUS_COMMUNICATION_FAILURE("external auth STATUS_COMMUNICATION_FAILURE status is not supported by InMemoryDirectoryServer"),
            EXTERNAL_AUTH_STATUS_CONNECTION_REFUSED("external auth STATUS_CONNECTION_REFUSED status is not supported by InMemoryDirectoryServer"),
            EXTERNAL_AUTH_STATUS_UNKNOWN_HOST("external auth STATUS_UNKNOWN_HOST status is not supported by InMemoryDirectoryServer"),
            SUBORDINATES_OPERTIONAL_ATTRIBUTE("Subordinates operational attribute is not supported by InMemoryDirectoryServer");
            
            private String notes;
            private Reason(String notes) {
                this.notes = notes;
            }
        }
        
        public SkippedForInMemLdapServerException(Reason reason) {
            super("SkippedForInMemLdapServer: " + reason.name());
        }
    }
    
    protected void SKIP_FOR_INMEM_LDAP_SERVER(SkippedForInMemLdapServerException.Reason reason) 
    throws SkippedForInMemLdapServerException {
        if (InMemoryLdapServer.isOn()) {
            throw new SkippedForInMemLdapServerException(reason);
        }
    }

    @Rule
    public TestName TEST_NAME = new TestName();
    
    protected String getTestName() {
        return TEST_NAME.getMethodName();
    }
    
    private String genName(String suffix) {
        String name = suffix == null ? getTestName() : 
            getTestName() + "-" + suffix;
        return name.toLowerCase();
    }
    
    private String genName() {
        return genName(null);
    }
    
    protected String genAcctNameLocalPart(String suffix) {
        return genName(suffix);
    }
    
    protected String genAcctNameLocalPart() {
        return genAcctNameLocalPart(null);
    }
    
    protected String genDataSourceName(String suffix) {
        return "ds-" + genName(suffix);
    }
    
    protected String genDataSourceName() {
        return genDataSourceName(null);
    }
    
    protected String genIdentityName(String suffix) {
        return "iden-" + genName(suffix);
    }
    
    protected String genIdentityName() {
        return genIdentityName(null);
    }

    protected String genGroupNameLocalPart(String suffix) {
        return genName(suffix);
    }
    
    protected String genGroupNameLocalPart() {
        return genGroupNameLocalPart(null);
    }
    
    protected String genCosName(String suffix) {
        return "cos-" + genName(suffix);
    }
    
    protected String genCosName() {
        return genCosName(null);
    }
    
    protected String genDomainSegmentName(String suffix) {
        return genName(suffix);
    }
    
    protected String genDomainSegmentName() {
        return genDomainSegmentName(null);
    }
    
    protected String genServerName(String suffix) {
        return "server-" + genName(suffix);
    }
    
    protected String genServerName() {
        return genServerName(null);
    }
    
    protected String genSignatureName(String suffix) {
        return "sig-" + genName(suffix);
    }
    
    protected String genSignatureName() {
        return genSignatureName(null);
    }
    
    protected String genXMPPName(String suffix) {
        return "xmpp-" + genName(suffix);
    }
    
    protected String genXMPPName() {
        return genXMPPName(null);
    }  
    
    protected String genZimletName() {
        return "zimlet-" + genName();
    }

}
