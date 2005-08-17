package com.zimbra.cs.stats;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * @author anandp
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AccumulatorLayout extends Layout {

    public AccumulatorLayout() {
    }

    public String format(LoggingEvent event) {
        String msg = event.getRenderedMessage();
        StringBuffer sb = new StringBuffer(msg.length() + 32);
        long secondsSinceEpoch = System.currentTimeMillis() / 1000;
        sb.append(secondsSinceEpoch).append(",").append(msg).append("\n");
        return sb.toString();
    }

    public boolean ignoresThrowable() {
        return true;
    }

    public void activateOptions() {
    }
}
