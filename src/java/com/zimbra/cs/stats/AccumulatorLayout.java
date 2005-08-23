/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

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
