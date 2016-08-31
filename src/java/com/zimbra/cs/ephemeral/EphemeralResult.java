package com.zimbra.cs.ephemeral;

import java.util.List;

import com.zimbra.common.service.ServiceException;

/**
 * A wrapper class for results retrieved from @EphemeralStore.
 * This class supports both single- and multi-valued keys.
 * If a key is known to have a single value, the get*Value() methods should be used.
 * If a key has more than one, get*Values() should be used.
 * @author iraykin
 *
 */
public class EphemeralResult {
    private String key;
    private String[] values;

    public EphemeralResult(String key, String value) {
        this.key = key;
        this.values = value == null ? null : new String[] {value};
    }

    public EphemeralResult(String key, String[] values) {
        this.key = key;
        this.values = values;
    }

    public EphemeralResult(EphemeralKeyValuePair entry) {
        this.key = entry.getKey();
        this.values = entry == null ? null : new String[] { entry.getValue() };
    }

    public EphemeralResult(EphemeralKeyValuePair[] entries) throws ServiceException {
        if (entries != null && entries.length > 0) {
            this.key = entries[0].getKey();
            this.values = new String[entries.length];
            for (int i = 0; i < entries.length; i++) {
                this.values[i] = entries[i].getValue();
            }
        }
    }

    public EphemeralResult(String name, List<String> values) {
        this(name, values.toArray(new String[values.size()]));
    }

    public String getValue() {
        return isEmpty() ? null : values[0];
    }

    public String getValue(String defaultValue) {
        return isEmpty() ? defaultValue : values[0];
    }

    public String[] getValues(String defaultValue) {
        return values == null || values.length == 0 ? new String[] {defaultValue}: values;
    }

    public String[] getValues() {
        return values == null || values.length == 0 ? new String[0]: values;
    }

    public Integer getIntValue() {
        return isEmpty() ? null : Integer.valueOf(values[0]);
    }

    public Integer getIntValue(Integer defaultValue) {
        return isEmpty() ? defaultValue : Integer.valueOf(values[0]);
    }

    public Integer[] getIntValues() {
        return isEmpty() ? new Integer[0] : convertToInts();
    }

    public Integer[] getIntValues(Integer defaultValue) {
        return isEmpty() ? new Integer[] {defaultValue} : convertToInts();
    }

    public Long getLongValue() {
        return isEmpty() ? null : Long.valueOf(values[0]);
    }

    public Long getLongValue(Long defaultValue) {
        return isEmpty() ? defaultValue : Long.valueOf(values[0]);
    }

    public Long[] getLongValues() {
        return isEmpty() ? new Long[0] : convertToLongs();
    }

    public Long[] getLongValues(Long defaultValue) {
        return isEmpty() ? new Long[] {defaultValue} : convertToLongs();
    }

    public Boolean getBoolValue() {
        return isEmpty() ? null : Boolean.valueOf(values[0]);
    }

    public Boolean getBoolValue(Boolean defaultValue) {
        return isEmpty() ? defaultValue : Boolean.valueOf(values[0]);
    }

    public Boolean[] getBoolValues() {
        return isEmpty() ? new Boolean[0] : convertToBools();
    }

    public Boolean[] getBoolValues(Boolean defaultValue) {
        return isEmpty() ? new Boolean[] {defaultValue} : convertToBools();
    }

    public boolean isMultiValued() {
        return values != null && values.length > 1;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return String.format("ephemeral attribute %s [%s]", key, values == null ? "unset" : values);
    }

    public static EphemeralResult emptyResult(String key) {
        return new EphemeralResult(key, new String[0]);
    }

    public boolean isEmpty() {
        return values == null || values.length == 0;
    }

    private Integer[] convertToInts() {
        Integer[] integers = new Integer[values.length];
        for (int i = 0; i < values.length; i++ ) {
            integers[i] = Integer.valueOf(values[i]);
        }
        return integers;
    }

    private Long[] convertToLongs() {
        Long[] longs = new Long[values.length];
        for (int i = 0; i < values.length; i++ ) {
            longs[i] = Long.valueOf(values[i]);
        }
        return longs;
    }

    private Boolean[] convertToBools() {
        Boolean[] bools = new Boolean[values.length];
        for (int i = 0; i < values.length; i++ ) {
            bools[i] = Boolean.valueOf(values[i]);
        }
        return bools;
    }
}
