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
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class URLHandler extends ObjectHandler {

	// FIXME: this needs to be much more robust...
	// needed something for testing...
    private Pattern URL_PATTERN =
        Pattern.compile("((telnet:)|((https?|ftp|gopher|news|file):\\/\\/)|(www.[\\w\\.\\_\\-]+))[^\\s\\(\\)\\<\\>\\[\\]\\{\\}\\'\\\"]*");
	
	public void parse(String text, List matchedObjects, boolean firstMatchOnly)
			throws ObjectHandlerException {
        Matcher m = URL_PATTERN.matcher(text);
        MatchedObject mo;
        while (m.find()) {
            mo = new MatchedObject(this, text.substring(m.start(), m.end()));
            matchedObjects.add(mo);
            if (firstMatchOnly)
                return;
        }
	}
    
    public static void test(URLHandler h, String text) throws ObjectHandlerException {
        ArrayList list = new ArrayList();
        h.parse(text, list, true);
        System.out.println(text+" "+(list.size() >0));        
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
