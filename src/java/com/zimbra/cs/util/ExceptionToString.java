/*
 * Created on Sep 16, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.util;

import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * @author tim
 *
 * Dead-Simple: does what it says - writes data about the exception to a string.
 */
public final class ExceptionToString {
    public static final String ToString(Exception e) {
    	StringWriter sw = new StringWriter();
    	PrintWriter pw = new PrintWriter(sw);
    	e.printStackTrace(pw);
    	pw.close();
    	return sw.toString();
    }
    
}
