package com.zimbra.cs.localconfig;

public class ConfigException extends Exception {
    public ConfigException(String key) {
        super(key);
    }
}
