package com.zimbra.cs.ephemeral;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.BooleanUtils;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * A wrapper class for results retrieved from EphemeralStore.
 * This class supports both single- and multi-valued keys.
 * If a key is known to have a single value, the get*Value() methods should be used.
 * If a key has more than one, get*Values() should be used.
 * @author iraykin
 *
 */
public class EphemeralResult {
    private EphemeralKey key;
    private String[] values;

	private final Pattern JSON_STRING_RE = Pattern.compile("^\"(.*)\"$");

    public EphemeralResult(EphemeralKey key, String value) {
        this.key = key;
        this.values = value == null ? null : normalizeZokValues(new String[]{ value });
    }

    public EphemeralResult(EphemeralKey key, String[] values) {
        this.key = key;
        this.values = normalizeZokValues(values);
    }

    public EphemeralResult(EphemeralKeyValuePair entry) {
        this.key = entry.getKey();
        this.values = entry == null ? null : normalizeZokValues(new String[]{ entry.getValue() });
    }

    public EphemeralResult(EphemeralKeyValuePair[] entries) throws ServiceException {
        if (entries != null && entries.length > 0) {
            this.key = entries[0].getKey();
            String[] values = new String[entries.length];
            for (int i = 0; i < entries.length; i++) {
                values[i] = entries[i].getValue();
            }
			this.values = normalizeZokValues(values);
        }
    }

    public EphemeralResult(EphemeralKey key, List<String> values) {
        this(key, values.toArray(new String[values.size()]));
		this.values = normalizeZokValues(this.values);
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
        try {
            return isEmpty() ? null : Integer.valueOf(values[0]);
        } catch (NumberFormatException e) {
            ZimbraLog.ephemeral.warn("value '%s' cannot be converted to an integer, returning null", values[0]);
            return null;
        }
    }

    public Integer getIntValue(Integer defaultValue) {
        try {
            return isEmpty() ? defaultValue : Integer.valueOf(values[0]);
        } catch (NumberFormatException e) {
            ZimbraLog.ephemeral.warn("value '%s' cannot be converted to an integer, returning default value", values[0]);
            return defaultValue;
        }
    }

    public Integer[] getIntValues() {
        try {
            return isEmpty() ? new Integer[0] : convertToInts(null);
        } catch (NumberFormatException e) {
            return new Integer[0];
        }
    }

    public Integer[] getIntValues(Integer defaultValue) {
        return isEmpty() ? new Integer[] {defaultValue} : convertToInts(defaultValue);
    }

    public Long getLongValue() {
        try {
            return isEmpty() ? null : Long.valueOf(values[0]);
        } catch (NumberFormatException e) {
            ZimbraLog.ephemeral.warn("value '%s' cannot be converted to a long, returning null", values[0]);
            return null;
        }
    }

    public Long getLongValue(Long defaultValue) {
        try {
            return isEmpty() ? defaultValue : Long.valueOf(values[0]);
        } catch (NumberFormatException e) {
            ZimbraLog.ephemeral.warn("value '%s' cannot be converted to a long, returning default value", values[0]);
            return defaultValue;
        }
    }

    public Long[] getLongValues() {
        return isEmpty() ? new Long[0] : convertToLongs(null);
    }

    public Long[] getLongValues(Long defaultValue) {
        return isEmpty() ? new Long[] {defaultValue} : convertToLongs(defaultValue);
    }

    public Boolean getBoolValue() {
        try {
            return isEmpty() ? null : stringToBool(values[0]);
        } catch (IllegalArgumentException e) {
            ZimbraLog.ephemeral.warn("value '%s' cannot be converted to a boolean, returning null", values[0]);
            return null;
        }
    }

    public Boolean getBoolValue(Boolean defaultValue) {
        try {
            return isEmpty() ? defaultValue : stringToBool(values[0]);
        } catch (IllegalArgumentException e) {
            ZimbraLog.ephemeral.warn("value '%s' cannot be converted to a boolean, returning default value", values[0]);
            return defaultValue;
        }
    }

    public Boolean[] getBoolValues() {
        return isEmpty() ? new Boolean[0] : convertToBools(null);
    }

    public Boolean[] getBoolValues(Boolean defaultValue) {
        return isEmpty() ? new Boolean[] {defaultValue} : convertToBools(defaultValue);
    }

    public boolean isMultiValued() {
        return values != null && values.length > 1;
    }

    public EphemeralKey getKey() {
        return key;
    }

    @Override
    public String toString() {
        return String.format("ephemeral attribute %s [%s]", key, values == null ? "unset" : values);
    }

    public static EphemeralResult emptyResult(EphemeralKey key) {
        return new EphemeralResult(key, new String[0]);
    }

    public boolean isEmpty() {
        return values == null || values.length == 0;
    }

    private Integer[] convertToInts(Integer defaultValue) {
        Integer[] integers = new Integer[values.length];
        for (int i = 0; i < values.length; i++ ) {
            try {
                integers[i] = Integer.valueOf(values[i]);
            } catch (NumberFormatException e) {
                ZimbraLog.ephemeral.warn("value '%s' cannot be converted to an integer, using default", values[i]);
                integers[i] = defaultValue;
            }
        }
        return integers;
    }

    private Long[] convertToLongs(Long defaultValue) {
        Long[] longs = new Long[values.length];
        for (int i = 0; i < values.length; i++ ) {
            try {
                longs[i] = Long.valueOf(values[i]);
            } catch (NumberFormatException e) {
                ZimbraLog.ephemeral.warn("value '%s' cannot be converted to a long, using default", values[i]);
                longs[i] = defaultValue;
            }
        }
        return longs;
    }

    private Boolean[] convertToBools(Boolean defaultValue) {
        Boolean[] bools = new Boolean[values.length];
        for (int i = 0; i < values.length; i++ ) {
            try {
                bools[i] = stringToBool(values[i]);
            } catch (IllegalArgumentException e) {
                ZimbraLog.ephemeral.warn("value '%s' cannot be converted to a boolean, using default", values[i]);
                bools[i] = defaultValue;
            }
        }
        return bools;
    }

    private Boolean stringToBool(String s) {
        return BooleanUtils.toBooleanObject(s.toLowerCase(), "true", "false", "null");
    }


	/** Replace extraneous quotes and other remnants of ZOK converting certain values to JSON.
	 *
	 * @param values the values to be set
	 * @return the clean values
	 */
	private String[] normalizeZokValues(final String[] values) {
		if (!LC.ssdb_zok_compat.booleanValue()) {
			return values;
		}

		String[] normalized = new String[values.length];

		for (int i = 0; i < values.length; i++) {
			final Matcher m = JSON_STRING_RE.matcher(values[i]);
			normalized[i] = m.matches() ? m.group(1) : values[i];
		}

		return normalized;
	}
}
