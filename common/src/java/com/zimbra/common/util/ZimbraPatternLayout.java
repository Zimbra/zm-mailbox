package com.zimbra.common.util;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;

@Plugin(name = "ZimbraPatternLayout", category = "Converter")
@ConverterKeys({ "z" })
public class ZimbraPatternLayout extends LogEventPatternConverter {

    protected ZimbraPatternLayout(String name, String style) {
        super(name, style);
    }
    
    public static ZimbraPatternLayout newInstance(String[] options)
    {
        return new ZimbraPatternLayout("z", Thread.currentThread().getName());
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        toAppendTo.append(ZimbraLog.getContextString() == null ? "" : ZimbraLog.getContextString());
    }

}