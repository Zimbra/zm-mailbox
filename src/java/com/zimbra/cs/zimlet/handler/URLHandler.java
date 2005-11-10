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
package com.zimbra.cs.zimlet.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.cs.zimlet.ZimletHandler;
import com.zimbra.cs.object.ObjectHandlerException;

/**
 * @author schemers
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class URLHandler implements ZimletHandler {

	// FIXME: this needs to be much more robust...
	// needed something for testing...
    private Pattern URL_PATTERN =
        Pattern.compile("((telnet:)|((https?|ftp|gopher|news|file):\\/\\/)|(www.[\\w\\.\\_\\-]+))[^\\s\\(\\)\\<\\>\\[\\]\\{\\}\\'\\\"]*");
	
	public String[] match(String text) {
        Matcher m = URL_PATTERN.matcher(text);
        List l = new ArrayList();
        while (m.find()) {
            l.add(text.substring(m.start(), m.end()));
        }
        return (String[]) l.toArray(new String[0]);
	}
    
    public static void test(URLHandler h, String text) throws ObjectHandlerException {
        String[] array = h.match(text);
        System.out.println(text+" "+(array.length >0));        
    }
    
    // TOOD: move these to a unit test
    public static void main(String args[]) throws ObjectHandlerException {
        URLHandler h = new URLHandler();
        test(h,"(http://itproductguidebeta.infoworld.com/article/04/09/23/HNsdchiupdate_1.html?BACKUP%20AND%20RECOVERY) is interesting");
        test(h," http://www.hula-project.org/index.php/Screenshots for screenshots");
        test(h,"http://rsvp.conferencemgr.com/RSVP.asp?Q=3KRV7&E=matt@example.zimbra.com");
        test(h," http://www.google.com/news");
        test(h," www.google.com/news");
        test(h," here is a url: www.google.com.");
        test(h," here is a url: http://www.google.com.");
        test(h," http://www.stanford.edu/~quanah/directory/email/");
        test(h," http://software.newsforge.com/article.pl?sid=05/03/22/204244");
        test(h,"\"http://software.newsforge.com/article.pl?sid=05/03/22/204244\"");
        test(h," (http://news.bbc.co.uk/2/hi/business/4167633.stm)");
        test(h," http://www.perforce.com/perforce/doc.042/manuals/p4guide/04_details.html#1098239");
        test(h," When presented with a URL like \"www.usps.com/webtools\", the client now only");
        test(h," highlights \"www.usps.com\".");
        test(h," highlights foo bar ");
        test(h," http is kewl ");        
    }
}
