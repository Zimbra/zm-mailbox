package com.zimbra.qa.unittest.prov;

import org.junit.Rule;
import org.junit.rules.TestName;

public class ProvTest {
    
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

    protected String genGroupNameLocalPart(String suffix) {
        return genName(suffix);
    }
    
    protected String genGroupNameLocalPart() {
        return genGroupNameLocalPart(null);
    }
    
    protected String genCosName() {
        return "cos-" + genName();
    }
    
    protected String genDomainSegmentName() {
        return genName();
    }
}
