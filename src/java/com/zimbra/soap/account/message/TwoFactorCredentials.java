package com.zimbra.soap.account.message;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;

import com.zimbra.common.soap.AccountConstants;

public class TwoFactorCredentials {

    @XmlElement(name=AccountConstants.E_TWO_FACTOR_SECRET)
    private String sharedSecret;

    @XmlElementWrapper(name=AccountConstants.E_TWO_FACTOR_SCRATCH_CODES)
    @XmlElements({
        @XmlElement(name=AccountConstants.E_TWO_FACTOR_SCRATCH_CODE, type=String.class)
    })
    private List<String> scratchCodes;

    public String getSharedSecret() {return sharedSecret; }
    public void setSharedSecret(String secret) {this.sharedSecret = secret; }

    public List<String> getScratchCodes() {return scratchCodes; }
    public void setScratchCodes(List<String> codes) { scratchCodes = codes; }
}