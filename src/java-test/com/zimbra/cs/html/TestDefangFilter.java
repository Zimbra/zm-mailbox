package com.zimbra.cs.html;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;

/**
 * Tired of regressions in the defang filter. Unit test based on fixes I found in bugzilla over the years for different
 * problems to make sure they still work
 * @author jpowers
 *
 */
public class TestDefangFilter {
    private static final String EMAIL_BASE_DIR = "./data/unittest/email/";
    
    /**
     * Check to makes sure ftp:// urls are passed through...
     * @throws Exception
     */
    @Test
    public void testBug37098() throws Exception {
        String fileName = "bug_37098.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        
        String result = HtmlDefang.defang(htmlStream, true);
        // Make sure it didn't delete ftp://
        Assert.assertTrue(result.contains("ftp://ftp.perftech.com/hidden/aaeon/cpupins.jpg"));
        
    }

    /**
     * Tests to make sure target="_blank" is added to anythign with an href
     * @throws Exception
     */
    @Test
    public void testBug46948() throws Exception {
        String fileName = "bug_46948.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        
        String result = HtmlDefang.defang(htmlStream, true);
        // Make sure each area tag has a target
        int index = result.indexOf("<area");
        while(index >= 0){
            int closingIndex = result.indexOf(">", index);
            int targetIndex =  result.indexOf("target=", index);
            // Make sure we got a target
            Assert.assertTrue(targetIndex != -1);
            // make sure its before the closing tag
            Assert.assertTrue(targetIndex < closingIndex);
            index = result.indexOf("<area", index+1);
        }
    }
    
    /**
     * Check to make sure we don't defang a url because we don't like the end of it.
     * @throws Exception
     */
    @Test
    public void testBug49452() throws Exception {
        String fileName = "bug_49452.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        
        String result = HtmlDefang.defang(htmlStream, true);
        // make sure the link is still there 
        // There should be a bunch of data after this link, but there's a few \n that seem to break it up.
        Assert.assertTrue(result.contains("https://www.plus1staging.net/plus1staging.net/companyAuthorization.jsp"));
    }
    
    /**
     * Checks to make sure the base url is prepended to any of the relative links
     * @throws Exception
     */
    @Test
    public void testBug11464() throws Exception {
        String fileName = "bug_11464.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        
        String result = HtmlDefang.defang(htmlStream, true);
        
        // Make sure this has been replaced
        Assert.assertTrue(!result.contains("src=\"_media/zimbra_logo.gif\""));
    }

    
    /**
     * Utility method that gets the html body part from a mime message and returns its input stream
     * @param fileName The name of the email file to load from the unit test data dir
     * @return The input stream for the html body if successful
     * @throws Exception
     */
    private InputStream getHtmlBody(String fileName) throws Exception {
        // Get an input stream of a test pdf to test with
        InputStream inputStream = new FileInputStream(EMAIL_BASE_DIR + fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteUtil.copy(inputStream, true, baos, true);
        
        ParsedMessage msg = new ParsedMessage(baos.toByteArray(), false);
        Set<MPartInfo> bodyparts = Mime.getBody(msg.getMessageParts(), true);

        InputStream htmlStream = null;
        for(MPartInfo body: bodyparts) {
            if(body.getContentType().contains("html")){
                htmlStream=  body.getMimePart().getInputStream();
            }
        }
        return htmlStream;
    }
    
    /**
     * Utility method that gets the html body part from a mime message and returns its input stream
     * @param fileName The name of the email file to load from the unit test data dir
     * @return The input stream for the html body if successful
     * @throws Exception
     */
    private InputStream getHtmlPart(String fileName, int partNum) throws Exception {
        // Get an input stream of a test pdf to test with
        InputStream inputStream = new FileInputStream(EMAIL_BASE_DIR + fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteUtil.copy(inputStream, true, baos, true);
        
        ParsedMessage msg = new ParsedMessage(baos.toByteArray(), false);
        List<MPartInfo> parts = msg.getMessageParts();//Mime.getBody(msg.getMessageParts(), true);

        InputStream htmlStream = null;
        for(MPartInfo body: parts) {
               if(body.getPartNum() == partNum){
                htmlStream=  body.getMimePart().getInputStream();
               }
        }
        return htmlStream;
    }
    
    /**
     * Tests to make sure we allow just image names to come through
     * @throws Exception
     */
    @Test
    public void testBug60769() throws Exception {
        String fileName = "bug_60769.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        
        String result = HtmlDefang.defang(htmlStream, true);
                
        Assert.assertTrue(!result.contains("dfsrc=\"image001.gif\""));
        Assert.assertTrue(result.contains("src=\"image001.gif\""));
    }
    
    /**
     * Tests to make sure we can handle inline image data embeded with a data: protocol 
     * without tying up the system
     * @throws Exception
     */
    @Test
    public void testBug62605() throws Exception {
        String fileName = "bug_62605.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        long startTime = System.currentTimeMillis();
        String result = HtmlDefang.defang(htmlStream, true);
        long endTime = System.currentTimeMillis();
        
        // Make sure this takes less than one second
        Assert.assertTrue("Possible slowness in a regex", (endTime - startTime) < 1000);
        // Make sure this has been replaced
        Assert.assertTrue(result.contains("src=\"data:"));
    }
    
    /**
     * Makes sure we don't defang inline images
     * @throws Exception
     */
    @Test
    public void testBug62632() throws Exception {
        String fileName = "bug_62632.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        
        String result = HtmlDefang.defang(htmlStream, true);
                
        // Mare sure dfsrc isn't in there
        Assert.assertTrue(!result.contains("dfsrc=\"data:"));
        // and make sure we still have the src link..
        Assert.assertTrue(result.contains("src=\"data:"));
    }
    
    /**
     * Makes sure we don't defang inline images
     * @throws Exception
     */
    @Test
    public void testBug63150() throws Exception {
        String fileName = "bug_63150.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        
        String result = HtmlDefang.defang(htmlStream, true);
                
        // Check to make sure the link needed is still in there.
        Assert.assertTrue(result.contains("BillingInfoDisplayCmd?bi_URL"));
    }
    
    /**
     * Makes sure we don't defang input button images
     * @throws Exception
     */
    @Test
    public void testBug62346() throws Exception {
        String fileName = "bug_62346.txt";
        InputStream htmlStream = getHtmlPart(fileName, 2);
        Assert.assertNotNull(htmlStream);
        
        String result = HtmlDefang.defang(htmlStream, false);
                
        // Check to make sure the link needed is still in there.
        Assert.assertTrue(result.contains("https://secure.sslpost.com/static/images/open_document.png"));
    }
    /**
     * Test to make sure there aren't NPE's when there isn't an src in an img tag
     * @throws Exception
     */
    @Test
    public void testBug64188() throws Exception {
        String fileName = "bug_64188.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        Assert.assertNotNull(htmlStream);
        
        String result = HtmlDefang.defang(htmlStream, true);
         // just make sure we made it here, as this was NPEing out..
        Assert.assertNotNull(result);
       
    }
}
