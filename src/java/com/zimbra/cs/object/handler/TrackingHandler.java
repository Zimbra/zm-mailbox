/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 5, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.object.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.cs.object.MatchedObject;
import com.zimbra.cs.object.ObjectHandler;
import com.zimbra.cs.object.ObjectHandlerException;

/**
 * @author schemers
 *
 * TODO: currently just recognizing UPS 1Z* version
 * 
 *  UPS Tracking Numbers appear in the following formats:
 *
 *   1Z 999 999 99 9999 999 9
 *   9999 9999 9999
 *   T999 9999 999
 *
 * we are only looking for the first format right now...
 * 
 *  
 */
public class TrackingHandler extends ObjectHandler {

    private static final String UPS = "1[zZ]\\s?\\w{3}\\s?\\w{3}\\s?\\w{2}\\s?\\w{4}\\s?\\w{3}\\s?\\w{1}";
    private static final String FEDEX = "\\d{12}";
    
    private static final Pattern TRACKING = Pattern.compile("(\\b((" + UPS + ")|(" + FEDEX + "))\\b)");        

	public void parse(String text, List matchedObjects, boolean firstMatchOnly)
			throws ObjectHandlerException {
        Matcher t = TRACKING.matcher(text);
        MatchedObject mo;
        while (t.find()) {
            mo = new MatchedObject(this, text.substring(t.start(), t.end()));
            matchedObjects.add(mo);
            if (firstMatchOnly)
                return;
        }
	}
    
    public static void test(TrackingHandler h, String text, boolean expected) throws ObjectHandlerException {
        ArrayList list = new ArrayList();
        h.parse(text, list, true);
        boolean actual  = list.size() > 0;
        if (expected != actual)
            System.out.println("["+text+"] ************** expected="+expected+" actual="+actual);
        else
            System.out.println("["+text+ "] expected="+expected);
    }

    public static void main(String args[]) throws ObjectHandlerException {
        ArrayList list = new ArrayList();
        TrackingHandler th = new TrackingHandler();
        test(th, "792806493666", true);
        test(th, "'792806493666'", true);        
        test(th, "\"792806493666\"", true);        
        test(th, "7928064936669", false);        
        test(th, "ups: 1Z11X1400217079799", true);
        test(th, "ups: 1Z11X14002170797999", false);        
        test(th, "ups: z1Z11X1400217079799", false);
        test(th, "ups(1Z11X1400217079799)", true);
        test(th, "The tracking number is: 792806493888.", true); 
    }
}
