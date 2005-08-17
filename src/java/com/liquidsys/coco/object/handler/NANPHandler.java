/*
 * Created on May 5, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.liquidsys.coco.object.handler;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.liquidsys.coco.object.MatchedObject;
import com.liquidsys.coco.object.ObjectHandler;
import com.liquidsys.coco.object.ObjectHandlerException;

/**
 * @author schemers
 *
 * NANP rules:
 * 
 *  
 */
public class NANPHandler extends ObjectHandler {

	// FIXME: this needs to be much more robust...
	// needed something for testing...
    private Pattern NANP_NUMBER_PATTERN = Pattern.compile(
    "((?:\\(?\\d{3}\\)?[-.\\s]?)|[^\\d])\\d{3}[-.\\s]\\d{4}");
    ///((?:\(?\d{3}\)?[-.\s]?)|[^\d])\d{3}[-.\s]\d{4}/}   
    
    /**
     * first digit of area code or exchange must be 2-9
     * second and third digits can't both be '1' and '1'
     * @return
     */
    private boolean invalid(StringBuffer match, int offset) {
    	return (match.charAt(offset) == '0' ||
    			match.charAt(offset) == '1' ||
				(match.charAt(offset+1) == '1' &&
				match.charAt(offset+2) == '1'));
    }
    
	public void parse(String text, List matchedObjects, boolean firstMatchOnly)
			throws ObjectHandlerException {
        Matcher m = NANP_NUMBER_PATTERN.matcher(text);
        MatchedObject mo;
        while (m.find()) {
        	String match = text.substring(m.start(), m.end());
        	StringBuffer digits = new StringBuffer(10);
        	int numDigits = 0;
        	for (int i=0; i < match.length(); i++) {
        		char c = match.charAt(i);
        		if (Character.isDigit(c))
        			digits.append(c);
        	}
        	if (invalid(digits, 0) ||
        		((digits.length() == 10 && invalid(digits, 3))))
        		continue;
            mo = new MatchedObject(this, match);
            matchedObjects.add(mo);
            if (firstMatchOnly)
                return;
        }
	}
}
