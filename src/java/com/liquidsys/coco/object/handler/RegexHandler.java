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
 * Generic object handler that gets its regex from the handler config.
 * 
 */
public class RegexHandler extends ObjectHandler {

    private Pattern mPattern;
	  
	public void parse(String text, List matchedObjects, boolean firstMatchOnly)
			throws ObjectHandlerException {
	    if (mPattern == null) {
	        mPattern = Pattern.compile(getHandlerConfig());
	    }
        Matcher m = mPattern.matcher(text);
        MatchedObject mo;
        while (m.find()) {
            mo = new MatchedObject(this, text.substring(m.start(), m.end()));
            matchedObjects.add(mo);
            if (firstMatchOnly)
                return;
        }
	}
}
