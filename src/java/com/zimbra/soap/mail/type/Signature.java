package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.SmimeConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class Signature {

    @XmlElement(name=SmimeConstants.E_SERIAL_NO, required=false)
    private String serialNumber;

    @XmlElement(name=SmimeConstants.E_ALGORITHM, required=false)
    private String algorithm;

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("serialNo", serialNumber)
            .add("algorithm", algorithm);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }

}
