package com.zimbra.soap.mail.type;

import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

@XmlEnum
public enum RecoverAccountOperation {
    @XmlEnumValue("getRecoveryAccount") GET_RECOVERY_ACCOUNT("getRecoveryAccount"),
    @XmlEnumValue("sendRecoveryCode") SEND_RECOVERY_CODE("sendRecoveryCode");

    private static Map<String, RecoverAccountOperation> nameToRecoverAccountOperations = Maps.newHashMap();
    static {
        for (RecoverAccountOperation v : RecoverAccountOperation.values()) {
            nameToRecoverAccountOperations.put(v.toString(), v);
        }
    }

    private String name;

    private RecoverAccountOperation(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static RecoverAccountOperation fromString(String name) {
        return nameToRecoverAccountOperations.get(Strings.nullToEmpty(name));
    }
}
