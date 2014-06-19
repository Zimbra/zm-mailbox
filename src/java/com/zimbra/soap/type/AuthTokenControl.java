package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.HeaderConstants;

@XmlAccessorType(XmlAccessType.NONE)
public final class AuthTokenControl {

    /**
     * @zm-api-field-tag voidOnExpired
     * @zm-api-field-description if set to true, expired authToken in the header will be ignored
     */
    @XmlAttribute(name=HeaderConstants.A_VOID_ON_EXPIRED/* voidOnExpired */, required=false)
    private Boolean voidOnExpired;

    public AuthTokenControl() {
        voidOnExpired = false;
    }

    public AuthTokenControl(boolean voidExpired) {
        voidOnExpired = voidExpired;
    }

    public void setVoidOnExpired(boolean voidExpired) {
        voidOnExpired = voidExpired;
    }

    public boolean isVoidOnExpired() {
        return voidOnExpired;
    }
}
