package com.zimbra.common.account;

import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

public class ForgetPasswordEnums {
    public enum CodeConstants {
        EMAIL("email"),
        CODE("code"),
        EXPIRY_TIME("expiryTime"),
        RESEND_COUNT("resendCount"),
        SUSPENSION_TIME("suspensionTime"),
        ACCOUNT_ID("accountId");

        private static Map<String, CodeConstants> nameToCodeConstants = Maps.newHashMap();
        static {
            for (CodeConstants v : CodeConstants.values()) {
                nameToCodeConstants.put(v.toString(), v);
            }
        }

        private String name;

        private CodeConstants(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static CodeConstants fromString(String name) {
            return nameToCodeConstants.get(Strings.nullToEmpty(name));
        }
    }

}
