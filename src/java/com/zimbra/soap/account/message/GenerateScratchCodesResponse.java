package com.zimbra.soap.account.message;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_GENERATE_SCRATCH_CODES_RESPONSE)
public class GenerateScratchCodesResponse {
    @XmlElementWrapper(name=AccountConstants.E_TWO_FACTOR_SCRATCH_CODES)
    @XmlElements({
        @XmlElement(name=AccountConstants.E_TWO_FACTOR_SCRATCH_CODE, type=String.class)
    })
    private List<String> scratchCodes;

    public List<String> getScratchCodes() {return scratchCodes; }
    public void setScratchCodes(List<String> codes) { scratchCodes = codes; }
}