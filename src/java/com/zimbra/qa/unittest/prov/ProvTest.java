package com.zimbra.qa.unittest.prov;

import org.junit.Rule;
import org.junit.rules.TestName;

import com.zimbra.cs.ldap.unboundid.InMemoryLdapServer;

public class ProvTest {
    
    public enum SkipTestReason {
        // reasons for skipping for real ldap server
        LONG_TEST("test takes too long to run"),
        
        // reasons for skipping for inmem ldap server
        CONNECTION_POOL_HEALTH_CHECK("connection pool health check does not work for InMemoryDirectoryServer"),
        DN_SUBTREE_MATCH_FILTER("entryDN:dnSubtreeMatch filter is not supported by InMemoryDirectoryServer"),
        EXTERNAL_AUTH_STATUS_AUTH_FAILED("external auth STATUS_AUTH_FAILED status is not supported by InMemoryDirectoryServer"),
        EXTERNAL_AUTH_STATUS_COMMUNICATION_FAILURE("external auth STATUS_COMMUNICATION_FAILURE status is not supported by InMemoryDirectoryServer"),
        EXTERNAL_AUTH_STATUS_CONNECTION_REFUSED("external auth STATUS_CONNECTION_REFUSED status is not supported by InMemoryDirectoryServer"),
        EXTERNAL_AUTH_STATUS_UNKNOWN_HOST("external auth STATUS_UNKNOWN_HOST status is not supported by InMemoryDirectoryServer"),
        SUBORDINATES_OPERTIONAL_ATTRIBUTE("Subordinates operational attribute is not supported by InMemoryDirectoryServer");
        
        private String notes;
        private SkipTestReason(String notes) {
            this.notes = notes;
        }
    }
    
    public static class SkippedForInMemLdapServerException extends Exception {
        public SkippedForInMemLdapServerException(SkipTestReason reason) {
            super("Skipped for inmem ldap server: " + reason.name());
        }
    }
    
    public static class SkippedForRealLdapServerException extends Exception {
        public SkippedForRealLdapServerException(SkipTestReason reason) {
            super("Skipped for real ldap server: " + reason.name());
        }
    };
    
    protected void SKIP_FOR_INMEM_LDAP_SERVER(SkipTestReason reason) 
    throws SkippedForInMemLdapServerException {
        if (InMemoryLdapServer.isOn()) {
            throw new SkippedForInMemLdapServerException(reason);
        }
    }
    
    protected void SKIP_FOR_REAL_LDAP_SERVER(SkipTestReason reason) 
    throws SkippedForRealLdapServerException {
        if (!InMemoryLdapServer.isOn()) {
            throw new SkippedForRealLdapServerException(reason);
        }
    }
    
    public static class Sequencer {
        private int next = 1;
        
        public int next() {
            return next++;
        }
    }

    @Rule
    public TestName TEST_NAME = new TestName();
    
    protected String getTestName() {
        return TEST_NAME.getMethodName();
    }
    
    private String genName(String suffix, Sequencer seq) {
        String name = suffix == null ? getTestName() : 
            getTestName() + "-" + suffix;
        
        if (seq != null) {
            name = name + "-" + seq.next();
        }
        return name.toLowerCase();
    }
    
    private String genName(String suffix) {
        return genName(suffix, null);
    }
    
    private String genName() {
        return genName(null);
    }
    
    protected String genAcctNameLocalPart(String suffix) {
        return "acct-" + genName(suffix);
    }
    
    protected String genAcctNameLocalPart() {
        return genAcctNameLocalPart(null);
    }
    
    protected String genCalendarResourceNameLocalPart(String suffix) {
        return "cr-" + genName(suffix);
    }
    
    protected String genCalendarResourceNameLocalPart() {
        return genCalendarResourceNameLocalPart(null);
    }
    
    protected String genDataSourceName(String suffix, Sequencer seq) {
        return "ds-" + genName(suffix, seq);
    }
    
    protected String genDataSourceName(String suffix) {
        return genDataSourceName(suffix, null);
    }
    
    protected String genDataSourceName(Sequencer seq) {
        return genDataSourceName(null, seq);
    }
    
    protected String genDataSourceName() {
        return genDataSourceName(null, null);
    }
    
    protected String genIdentityName(String suffix, Sequencer seq) {
        return "iden-" + genName(suffix, seq);
    }
    
    protected String genIdentityName(String suffix) {
        return genIdentityName(suffix, null);
    }
    
    protected String genIdentityName(Sequencer seq) {
        return genIdentityName(null, seq);
    }
    
    protected String genIdentityName() {
        return genIdentityName(null, null);
    }

    protected String genGroupNameLocalPart(String suffix) {
        return "group-" + genName(suffix);
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
    
    protected String genDomainName(String baseDomainName) {
        return genDomainSegmentName() + "." + baseDomainName;
    }
    
    protected String genServerName(String suffix) {
        return "server-" + genName(suffix);
    }
    
    protected String genServerName() {
        return genServerName(null);
    }
    
    protected String genSignatureName(String suffix, Sequencer seq) {
        return "sig-" + genName(suffix, seq);
    }
    
    protected String genSignatureName(String suffix) {
        return genSignatureName(suffix, null);
    }
    
    protected String genSignatureName(Sequencer seq) {
        return genSignatureName(null, seq);
    }
    
    protected String genSignatureName() {
        return genSignatureName(null, null);
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
