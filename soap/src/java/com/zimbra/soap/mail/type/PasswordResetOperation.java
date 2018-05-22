package com.zimbra.soap.mail.type;

import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

@XmlEnum
public enum PasswordResetOperation {
    @XmlEnumValue("getRecoveryEmail")
    GET_RECOVERY_EMAIL("getRecoveryEmail"), @XmlEnumValue("sendRecoveryCode")
    SEND_RECOVERY_CODE("sendRecoveryCode");

    private static Map<String, PasswordResetOperation> nameToPasswordRestOperations = Maps.newHashMap();
    static {
        for (PasswordResetOperation v : PasswordResetOperation.values()) {
            nameToPasswordRestOperations.put(v.toString(), v);
        }
    }

    private String name;

    private PasswordResetOperation(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static PasswordResetOperation fromString(String name) {
        return nameToPasswordRestOperations.get(Strings.nullToEmpty(name));
    }
}
