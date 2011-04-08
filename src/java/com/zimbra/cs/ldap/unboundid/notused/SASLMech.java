package com.zimbra.cs.ldap.unboundid.notused;

enum SASLMech {
    
    SASL_MECH_ANONYMOUS("anonymous"),
    SASL_MECH_CRAM_MD5("cram-md5"),
    SASL_MECH_DIGEST_MD5("digest-md5"),
    SASL_MECH_EXTERNAL("external"),
    SASL_MECH_GSSAPI("gssapi"),
    SASL_MECH_PLAIN("plain");
    
    private String mech;
    
    private SASLMech(String mech) {
        this.mech = mech;
    }
}
