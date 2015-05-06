package com.zimbra.soap.account.message;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

@XmlRootElement(name=AccountConstants.E_ENABLE_TWO_FACTOR_AUTH_RESPONSE)
@XmlType(propOrder = {})
public class EnableTwoFactorAuthResponse {

    @XmlElement(name=AccountConstants.E_TWO_FACTOR_SECRET, type=String.class, required=false)
    private String secret;

    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AccountConstants.E_TWO_FACTOR_SCRATCH_CODES)
    @XmlElement(name=AccountConstants.E_TWO_FACTOR_SCRATCH_CODE, type=String.class)
    private List<String> scratchCodes;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public List<String> getScratchCodes() { return scratchCodes; }
    public void setScratchCodes(List<String> scratchCodes) { this.scratchCodes = scratchCodes; }
}
