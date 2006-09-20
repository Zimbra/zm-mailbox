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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 10, 2004
 */
package com.zimbra.cs.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
//import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;

import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author tim
 */
public class UnitTests extends TestCase {
    
    public UnitTests() {
    }
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        Zimbra.toolSetup();
        
    }
    
    public void testSearch() throws ServiceException 
    { 
        

        runTestQuery(mMailboxId, "in:inbox", false, NO_EXPECTED_CHECK);
        
        
        assertTrue(runTestQuery(mMailboxId, "(linux or has:ssn) and before:1/1/2009 and -subject:\"zipped document\"", false, new QueryResult[]
                                                                                                                                     {
                                                                                  new QueryResult("Frequent system freezes after kernel bug"),
                                                                                  new QueryResult("Linux Desktop Info")
//                                                                                  ,new QueryResult(0, "numbers")
                                                                                                                                     }
                                                                          ));        

        assertTrue(runTestQuery(mMailboxId, "from:ross and not roland", true, new QueryResult[]
                                                                                     {
                                                                                             new QueryResult("meeting")
                                                                                      }
                                                                                     ));
                                                                                     
        
        /////////////////////////////////
        // BROKEN AND MUST BE FIXED:
        /////////////////////////////////
        assertTrue(runTestQuery(mMailboxId, "date:(01/01/2001 02/02/2002)", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "date:-1d date:(01/01/2001 02/02/2002)", false, NO_EXPECTED_CHECK));
        
        
        assertTrue(runTestQuery(mMailboxId, "in:(trash -junk)", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "date:(-1d or -2d)", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "date:\"-4d\"", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "date:-4d", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "date:\"+1d\"", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "date:+2w", false, NO_EXPECTED_CHECK));
        
        assertTrue(runTestQuery(mMailboxId, "not date:(1/1/2004 or 2/1/2004)", false, NO_EXPECTED_CHECK));
        
        
        /////////////////////////////////
        // Parser-only checks
        /////////////////////////////////
        assertTrue(runTestQuery(mMailboxId, "content:foo", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "content:\"foo bar\"", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "from:foo@bar.com", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "from:\"foo bar\"", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "to:foo@bar.com", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "to:\"foo bar\"", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "cc:foo@bar.com", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "cc:\"foo bar\"", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "subject:this_is_my_subject subject:\"this is_my_subject\"", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "in:inbox", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "has:attachment has:phone has:url", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "filename:foo filename:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "type:attachment type:text type:application type:word type:msword "+
                "type:excel type:xls type:ppt type:pdf type:ms-tnef type:image type:jpeg type:gif type:bmp "+
                "type:none type:any", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "is:(read unread flagged unflagged \"sent\" received replied unreplied forwarded unforwarded)", false, NO_EXPECTED_CHECK));

        // known broken:
//        assertTrue(runTestQuery(mM, "date:2001/01/13", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "date:+1d", false, NO_EXPECTED_CHECK));
        
//        assertTrue(runTestQuery(mM, "before:-1d before:(01/01/2001 2/2/2002)", false, NO_EXPECTED_CHECK));
        
        // broken:
//        assertTrue(runTestQuery(mM, "before:-1d before:(-1d 10d -100d 1w -10w -100h 1y -10y)", false, NO_EXPECTED_CHECK));

//        assertTrue(runTestQuery(mM, "after:-1d after:(01/01/2001 2/2/2002)", false, NO_EXPECTED_CHECK));

        // broken:
        //assertTrue(runTestQuery(mM, "after:-1d after:(01/01/2001 2001/01/02 +1d -1d +10d -100d +1w -10w -100h 1y -10y)", false, NO_EXPECTED_CHECK));
        
        assertTrue(runTestQuery(mMailboxId, "size:(1 20 300 1k <1k 10k >10k 100kb 34mb)", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "larger:(1 20 300 100kb 34mb)", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "smaller:(1 20 300 100kb 34mb)", false, NO_EXPECTED_CHECK));
        
        assertTrue(runTestQuery(mMailboxId, "author:foo author:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "title:foo title:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "keywords:foo keywords:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "company:foo company:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "metadata:foo metadata:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK));

        
        /////////////////////////////////
        // Search checks -- if you change the default data set these may start to fail!
        //                  just update the expected value list (order matters but case doesn't!)
        /////////////////////////////////
        assertTrue(runTestQuery(mMailboxId, "contributing to xmlbeans ", false, 
                new QueryResult[] { 
                new QueryResult("Contributing to XMLBeans"),
                new QueryResult("XmlBeans.jar size"),
                new QueryResult("XmlBeans project logo"),
        })
        );
        
//      assertTrue(runTestQuery(mM, "ski and not \"voice mail\"", false, 
//      new QueryResult[] { 
//      new QueryResult("Here are my ski pictures!")
//      })
//      );
        
        
		// skip this test -- it returns different results with or without verity, and that's annoying
        //assertTrue(runTestQuery(mM, "desktop -zipped", true, 
//		new QueryResult[] { 
//		new QueryResult("Linux Desktop Info")
//		}
//		));
        
        assertTrue(runTestQuery(mMailboxId, "before:1/1/2004 and source", false, new QueryResult[]
        {
                new QueryResult("Contributing to XMLBeans")
                ,new QueryResult("XmlBeans.jar size")
                ,new QueryResult("Failed to build with \"network downloads disabled\" error message.")
                ,new QueryResult("Failed to build with \"network downloads disabled\" error message.")
                ,new QueryResult("XMLBeans V1 binding style arch writeup")
                ,new QueryResult("Official release yet?")
                ,new QueryResult("cvs commit: xml-xmlbeans/v2/src/jam/org/apache/xmlbeans/impl/jam/internal/javadoc JDClassLoaderFactory.java")
                ,new QueryResult("XMLBeans/Java Web Start Issues")
                ,new QueryResult("XMLBeans/Java Web Start Issues")
                ,new QueryResult("builtin type conversions")
                ,new QueryResult("ArrayStoreException when using RMI")
                ,new QueryResult("Source Build Problem")
                ,new QueryResult("Finalizers")
                ,new QueryResult("Mapping XML type QName to Java Class name?")
                ,new QueryResult("xmlbeans javadocs?")
                ,new QueryResult("licenses for jaxb-api.jar and jax-qname.jar...?")
                ,new QueryResult("code to contribute: JAM")
                ,new QueryResult("STAX")
                ,new QueryResult("STAX")
                ,new QueryResult("code to contribute: JAM")
                ,new QueryResult("Getting the distribution onto a download site somewhere ...")
                ,new QueryResult("Getting an XMLBeans distribution onto a download site somewhere")
                ,new QueryResult("About me")
                ,new QueryResult("XmlBeans source code has been checked in ...")
                ,new QueryResult("XmlBeans source code has been checked in ...")
                ,new QueryResult("XmlBeans source code has been checked in ...")
                ,new QueryResult("XmlBeans source code has been checked in ...")
                ,new QueryResult("XmlBeans source code has been checked in ...")
                ,new QueryResult("Source Code")
                ,new QueryResult("Source Code")
                ,new QueryResult("Source Code")
        }
        ));
        
        assertTrue(runTestQuery(mMailboxId, "subject:linux", false, new QueryResult[]
                                                                           {
                new QueryResult("Linux Desktop Info")
                                                                           }
        ));
        
        assertTrue(runTestQuery(mMailboxId, "subject:\"code has\"", false, new QueryResult[]
                                                                                {
                new QueryResult("XmlBeans source code has been checked in ...")
                ,new QueryResult("XmlBeans source code has been checked in ...")
                ,new QueryResult("XmlBeans source code has been checked in ...")
                ,new QueryResult("XmlBeans source code has been checked in ...")
                ,new QueryResult("XmlBeans source code has been checked in ...")
//                ,new QueryResult("XmlBeans source code has been checked in ...")
//                ,new QueryResult("XmlBeans source code has been checked in ...")
                                                                                }
        ));        

        assertTrue(runTestQuery(mMailboxId, "(linux or has:ssn) and before:1/1/2009 and -subject:\"zipped document\"", false, new QueryResult[]
                                                                   {
                new QueryResult("Frequent system freezes after kernel bug"),
                new QueryResult("Linux Desktop Info")
//                ,new QueryResult(0, "numbers")
                                                                   }
        ));        
    
        assertTrue(runTestQuery(mMailboxId, "larger:1M", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "foo or not foo", false, NO_EXPECTED_CHECK));
		
        assertTrue(runTestQuery(mMailboxId, "(foo or not foo) and larger:1M", false, new QueryResult[]
                                                                                            {
                                     new QueryResult("Here are my ski pictures!")
                                     ,new QueryResult("AdminGuide...")
                                                                                            }
                             ));
        
        {
            Mailbox mbx = Mailbox.getMailboxById(1);
            String folderName = new String("/inbox");
            final Folder compFolder = mbx.getFolderByPath(null, folderName);
            //System.out.println("compTag is "+compTag);
            
            ResultValidator val = new ResultValidator() 
            {
                int cmpId = compFolder.getId();
                public void validate(ZimbraHit hit) throws ServiceException 
                {
                    MessageHit mh = (MessageHit)hit;
                    //                Date date = new Date(mh.getDateHeader());
                    //                assertTrue("Date out of range for " + mh.toString(), date.before(compDate));
                    //System.out.print("Tags for "+mh.toString()+": ");
                    int msgFolderId = mh.getFolderId();
//                    System.out.println("\tCompFolder = "+compFolder.toString()+" id="+cmpId);
//                    System.out.println("\tMessageFolder id="+msgFolderId);
                    assertTrue("Folder-Checking "+mh.toString()+" for "+compFolder.toString(), 
                            cmpId == msgFolderId);
                } 
            };
            runTestQuery(mMailboxId, "in:inbox", false, NO_EXPECTED_CHECK, val);
        }
        {
            final Date compDate = (new GregorianCalendar(2004, 1, 1)).getTime();
            
            ResultValidator val = new ResultValidator() 
            {
                public void validate(ZimbraHit hit) throws ServiceException 
                {
                    MessageHit mh = (MessageHit)hit;
                    Date date = new Date(mh.getDateHeader());
                    assertTrue("Date "+date+" out of range for " + mh.toString(), date.before(compDate));
                } 
            };
            
            runTestQuery(mMailboxId, "before:1/1/2004", false, NO_EXPECTED_CHECK, val);
        }
        
        
    }
    
    private static class QueryResult {
        public String mSubject;
        public QueryResult(String subject) {
            mSubject = subject;
        }
        public String toString() { 
            return mSubject;
        }
    }
    
    public static void MakeTestQuery(int mailboxId, String qstr, boolean conv)
    {
        try {
            QueryResult[] ret = RunQuery(mailboxId, qstr, conv, null);
            
            String qstr2 = qstr.replaceAll("\"", "\\\\\"");
            System.out.println("assertTrue(runTestQuery(mM, \""+
                    qstr2+"\", false, new QueryResult[]\n{");
            
            if (ret.length > 0) {
                System.out.println("\t\tnew QueryResult(0, \""+
                        ret[0].toString().replaceAll("\"","\\\\\"")+"\")");
            }
            
            for (int i = 1; i < ret.length; i++) {
                System.out.println("\t\t,new QueryResult(0, \""+
                        ret[i].toString().replaceAll("\"","\\\\\"")+"\")");
            }
            System.out.println("}\n));");
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // tag query
    public void notest11() throws ServiceException 
    {
        Mailbox mbx = Mailbox.getMailboxById(1);
        String tagName = new String("foo");
        final Tag compTag = mbx.getTagByName(tagName);
        //System.out.println("compTag is "+compTag);
        
        ResultValidator val = new ResultValidator() 
        {
            public void validate(ZimbraHit hit) throws ServiceException 
            {
                MessageHit mh = (MessageHit)hit;
                //                Date date = new Date(mh.getDateHeader());
                //                assertTrue("Date out of range for " + mh.toString(), date.before(compDate));
                //System.out.print("Tags for "+mh.toString()+": ");
                String mhStr = "";
                String compTagStr = "";
                try {
                    mhStr = mh.toString();
                    compTagStr = compTag.toString();
                } catch (NullPointerException e) {
                    System.out.println(e.toString());
                    e.printStackTrace();
                }
                assertTrue("Tag-Checking "+mhStr+" for "+compTagStr, 
                        mh.isTagged(compTag));
            } 
        };
        runTestQuery(mMailboxId, "tag:"+tagName, false, NO_EXPECTED_CHECK, val);
    }

    public static abstract class ResultValidator {
        public abstract void validate(ZimbraHit hit) throws ServiceException;
    }
    
    public static final QueryResult[] NO_EXPECTED_CHECK = {};
    
    public int mMailboxId = 2;
    
    public boolean runTestQuery(int mailboxId, String qstr, boolean conv, 
            QueryResult[] expected)
    {
        return runTestQuery(mailboxId, qstr, conv, expected, null);
    }
    
    
    public boolean runTestQuery(int mailboxId, String qstr, boolean conv, 
            QueryResult[] expected, ResultValidator validator)
    {
//        System.out.println("\n\nrunTestQuery("+mailboxId+","+qstr+")");
//        Zimbra.toolSetup("DEBUG");
        
        try {
            QueryResult[] ret = RunQuery(mailboxId, qstr, conv, validator);
            
//            for (int i = 0; i < ret.length; i++) {
//                System.out.println(ret[i].toString());
//            }

            if (expected != NO_EXPECTED_CHECK) {
                boolean error = true;
                String errStr = "Unexpected number of return values (expected "+expected.length+", returned "+ret.length+")";
                if (ret.length == expected.length) {
                    error = false;
                    for (int i = 0; i < ret.length; i++) {
                        String upperSub = ret[i].mSubject.toUpperCase();
                        if (upperSub.startsWith("RE:  ")) {
                            upperSub = upperSub.substring(5);
                        } else if (upperSub.startsWith("RE: ")) {
                                upperSub = upperSub.substring(4);
                        } else if (upperSub.startsWith("RE:")) {
                            upperSub = upperSub.substring(3);
                        } else if (upperSub.startsWith("FW:  ")) {
                            upperSub = upperSub.substring(5);
                        } else if (upperSub.startsWith("FW: ")) {
                            upperSub = upperSub.substring(4);
                        } else if (upperSub.startsWith("FW:")) {
                            upperSub = upperSub.substring(3); 
                        }
 
                        if (!upperSub.equals(expected[i].mSubject.toUpperCase())) {
                            System.out.println("UpperSub = "+upperSub+" sub="+ret[i].mSubject+" expected="+expected[i].mSubject.toUpperCase());
                            error = true;
                            errStr = 
                                "Expected return value doesn't match returned value at row " +
                                i;
                            //                            +
                            //                            " expected=\"" +
                            //                            expected[i].toString() +
                            //                            "\" returned=\"" +
                            //                            ret[i].toString()+
                            //                            "\"";
                            break;
                        }
                    }
                } 
                if (error) {
                    int ilen = Math.max(ret.length, expected.length);
                    for (int i = 0; i < ilen; i++) {
                        if (i < ret.length) {
                            errStr+="\n\t\t"+i+") ret=" + ret[i].toString();
                        } else {
                            errStr+="\n\t\t"+i+") ret=NO_MORE";
                        }
                        
                        if (i < expected.length) {
                            errStr+="\n\t\t"+i+") exp=" + expected[i].toString()+"\n";
                        } else {
                            errStr+="\n\t\t"+i+") exp=NO_MORE\n";
                        }
                    }
                    
                    fail("\n\tQuery=\""+qstr+"\" -- "+errStr);
                }
            }  
            
        } catch (IOException e) {
            e.printStackTrace();
            fail("Caught an IOException running test query "+qstr+" for mailbox "+mailboxId);
        } catch (ServiceException e) {
            e.printStackTrace();
            fail("Caught a service exception running test query "+qstr);
        } catch (ParseException e) {
            e.printStackTrace();
            fail("Caught a parse exception running test query "+qstr);
        }
        
//        System.out.println("DONE testQuery");
        
        return true;
    }
    
    public static QueryResult[] RunQuery(int mailboxId, String qstr, 
    		boolean conv, ResultValidator validator) throws IOException, MailServiceException, ParseException, ServiceException {
    	ArrayList<QueryResult> ret = new ArrayList<QueryResult>();

    	MailboxIndex searcher = Mailbox.getMailboxById(mailboxId).getMailboxIndex();

    	int groupBy = MailboxIndex.SEARCH_RETURN_MESSAGES;
    	if (conv) {
    		groupBy = MailboxIndex.SEARCH_RETURN_CONVERSATIONS;
    	}

    	byte types[] = new byte[1];
    	switch(groupBy) {
    	case MailboxIndex.SEARCH_RETURN_CONVERSATIONS:
    		types[0] = MailItem.TYPE_CONVERSATION;
    		break;
    	case MailboxIndex.SEARCH_RETURN_MESSAGES:
    		types[0] = MailItem.TYPE_MESSAGE;
    		break;
    	default:
    		types[0] = 0;
    	break;
    	}

    	Mailbox mbox = Mailbox.getMailboxById(mailboxId);

    	ZimbraQuery zq = new ZimbraQuery(qstr, null, null, mbox, 
    			types, SortBy.DATE_DESCENDING,
    			false, false, 100, true, Mailbox.SearchResultMode.NORMAL);

    	ZimbraQueryResults res = zq.execute();

    	try {

//  		long endTime = System.currentTimeMillis();

//  		compute numMessages the slow way, so we get a true count...for testing only!
    		if (true) {
    			int numMessages = 0;
    			{
    				ZimbraHit hit = res.getFirstHit();
    				while (hit != null) {
    					numMessages++;
    					hit=res.getNext();
    				}
    			}

    			System.out.println("Query: \""+qstr+"\" matched "+numMessages+" documents");
    		}
//  		System.out.println(numMessages + " total matching documents in " + (endTime-startTime) + " ms");

    		int HITS_PER_PAGE = 20;
    		if (conv) {
    			HITS_PER_PAGE = 20;
    		}

    		int totalShown = 0;

    		ZimbraHit hit = res.skipToHit(0);
    		while (hit != null) {
    			for (int i = 0; (hit != null) && (i < HITS_PER_PAGE); i++) {
    				if (conv) {
    					ConversationHit ch = (ConversationHit) hit;
    					Date date = new Date(ch.getHitDate());
    					System.out.println(ch.toString() + " " + date + " " + ch.getSubject() + "  (" + ch.getNumMessageHits() + ")");
//  					Collection mhs = ch.getMessageHits();
    					totalShown++;
    					ret.add(new QueryResult(ch.getSubject()));
    					if (validator != null) {
    						validator.validate(hit);
    					}
//  					for (Iterator iter = mhs.iterator(); iter.hasNext(); )
//  					{
//  					SimpleQueryResults.MessageHit mh = (SimpleQueryResults.MessageHit)iter.next();
//  					Date date1 = new Date(mh.getDateHeader());
//  					Date date2 = new Date(mh.getDate());

////					System.out.println("\t" + mh.toString() + " " + date1 + " " + mh.getSender() + " " + mh.getSubject());
//  					}
    				} else {
    					if (hit instanceof MessageHit) {
    						MessageHit mh = (MessageHit)hit;
//  						Date date = new Date(mh.getDateHeader());
    						//                            System.out.println(mh.toString() + " " + date + " " + mh.getSender() + " " + mh.getSubject());
    						totalShown++;
    						ret.add(new QueryResult(mh.getSubject()));
    						if (null != validator) {
    							validator.validate(hit);
    						}
    					}
    				}
    				hit = res.getNext();
    			}
    		}
//  		System.out.println(numMessages + " total matching documents in " + (endTime-startTime) + " ms");
//  		System.out.println("Showed a total of " + totalShown + (conv ? " Conversations" : " Messages"));
    	} finally {
    		res.doneWithSearchResults();
    	}
//  	} catch(Exception e) {
//  	e.printStackTrace();
//  	} finally {
//  	searcher.close();
//  	}
    	QueryResult[] retArray = new QueryResult[ret.size()];
    	return (QueryResult[])ret.toArray(retArray);

    }


    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public static void main(String[] args)
    {
        Zimbra.toolSetup("DEBUG");
        ZimbraLog.account.info("INFO TEST!");
        ZimbraLog.account.debug("DEBUG TEST!");

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//            String command = in.readLine();
            if (args.length > 0) {
                String command = args[0];
                if (command.equals("make")) {
                    while(true) {
                        System.out.print("Type Query>");
                        String query = in.readLine();
                        MakeTestQuery(1, query, false);
                    }
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        
        runTests();
        
        // hack: some system thread isn't cleaned up...just exit
        System.exit(1);
    }
    
    public static void runTests()
    {
        
        TestSuite suite = new TestSuite(UnitTests.class);
        TestResult results = new TestResult();
        suite.run(results);
//        System.out.println("Test Results: "+results.toString());

        if (!results.wasSuccessful()) {
            System.out.println("\n**************************");
            System.out.println("TEST FAILURES:");
            System.out.println("**************************");
        }

        if (results.failureCount() > 0) {
            Enumeration failures = results.failures();
            while(failures.hasMoreElements()) {
                TestFailure error = (TestFailure)failures.nextElement();
                System.out.println("--> Test Failure: " + error.trace() + error.thrownException());
                System.out.print("\n");
            }
        }

        if (results.errorCount() > 0) {
            Enumeration errors = results.errors();
            while(errors.hasMoreElements()) {
                TestFailure failure = (TestFailure)errors.nextElement();
                System.out.println("--> Test Error: " + failure.trace() + failure.thrownException() + " at ");
                failure.thrownException().printStackTrace();
                System.out.print("\n");
            }
        }
        
        if (results.wasSuccessful()) {
            System.out.println("\n**************************");
            System.out.println("Tests SUCCESSFUL!");
            System.out.println("**************************");
        }
        
    }
}
