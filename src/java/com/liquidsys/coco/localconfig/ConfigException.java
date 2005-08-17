package com.liquidsys.coco.localconfig;

public class ConfigException extends Exception {
    public ConfigException(String key) {
        super(key);
    }
}
