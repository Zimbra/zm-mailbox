package com.zimbra.common.util;

import org.apache.commons.lang.ObjectUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;

@Plugin(name = "ZimbraPattern2Layout", category = "Converter")
@ConverterKeys({ "z" })
public class ZimbraPattern2Layout extends LogEventPatternConverter {

    protected ZimbraPattern2Layout(String name, String style) {
        super(name, style);
    }
    
    public static ZimbraPattern2Layout newInstance(String[] options)
    {
        return new ZimbraPattern2Layout("z", Thread.currentThread().getName());
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        toAppendTo.append(ZimbraLog.getContextString());
    }

}