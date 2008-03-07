/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox.SearchResultMode;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.index.ResultValidator.QueryResult;
import com.zimbra.cs.index.ResultValidator.ExpectedCalendarItemHit;
import com.zimbra.cs.index.ResultValidator.ExpectedMessageHit;
import com.zimbra.cs.index.ResultValidator.ExpectedHitValidator;
import com.zimbra.qa.unittest.TestUtil;



/**
 * @author tim
 */
public class TestSearch extends TestCase {
    
    public void testSearch() throws ServiceException
    {
        {
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mMailboxId);
            
            // force deferred index items to be caught-up
            try {
                byte[] types = new byte[1];
                types[0] = MailItem.TYPE_MESSAGE;
                mbox.search(new OperationContext(mbox), "abc", types, SortBy.DATE_ASCENDING, 1);
            } catch (Exception e) {
                e.printStackTrace();
                fail("Caught exception running initial text search: "+e);
            }
                
            assertTrue(ZimbraQuery.unitTests(mbox));
        }
        long startTime = System.currentTimeMillis();

        runTestQuery(mMailboxId, "is:unread is:remote", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING);
        
        if (true) {
        runTestQuery(mMailboxId, "in:inbox", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING);
        
        assertTrue(runTestQuery(mMailboxId, "appt-start:<1/1/2006 recurring", false, 
            getTypes(MailItem.TYPE_APPOINTMENT, MailItem.TYPE_MESSAGE),SortBy.DATE_DESCENDING, NO_EXPECTED_CHECK, 
            new ExpectedHitValidator(
                new ExpectedMessageHit("Recurring Meeting Test"), 
                new ExpectedCalendarItemHit("040000008200E00074C5B7101A82E00800000000F0BC424F5761C5010000000000000000100000002C10E79C9140CF41B7E18816DF121033")
                )));
        
        assertTrue(runTestQuery(mMailboxId, "appt-start:>1/1/2006 recurring", false, 
            getTypes(MailItem.TYPE_APPOINTMENT, MailItem.TYPE_MESSAGE),SortBy.DATE_DESCENDING, NO_EXPECTED_CHECK, 
            new ExpectedHitValidator(
                new ExpectedMessageHit("Recurring Meeting Test")
            )
        )); 
        
        
        assertTrue(runTestQuery(mMailboxId, "(linux or has:ssn) and before:1/1/2009 and -subject:\"zipped document\"", false, new QueryResult[]
                                                                                                                                              {
                                                                                           new QueryResult("Frequent system freezes after kernel bug"),
                                                                                           new QueryResult("Linux Desktop Info")
                                                                                                                                              }, SortBy.DATE_DESCENDING
                                                                                   ));
     

        assertTrue(runTestQuery(mMailboxId, "(linux or has:ssn) and before:1/1/2009 and -subject:\"zipped document\"", false, new QueryResult[]
                                                                                                                                              {
                                                                                           new QueryResult("Frequent system freezes after kernel bug"),
                                                                                           new QueryResult("Linux Desktop Info")
//                                                                                           ,new QueryResult(0, "numbers")
                                                                                                                                              }, SortBy.DATE_DESCENDING
                                                                                   ));
        
        assertTrue(runTestQuery(mMailboxId, "(linux or has:ssn) and before:1/1/2009 and -subject:\"zipped document\"", false, new QueryResult[]
                                                                                                                                     {
                                                                                  new QueryResult("Frequent system freezes after kernel bug"),
                                                                                  new QueryResult("Linux Desktop Info")
//                                                                                  ,new QueryResult(0, "numbers")
                                                                                                                                     }, SortBy.DATE_DESCENDING
                                                                          ));

        assertTrue(runTestQuery(mMailboxId, "from:ross and not roland", true, new QueryResult[]
                                                                                     {
                                                                                             new QueryResult("meeting")
                                                                                      }, SortBy.DATE_DESCENDING
                                                                                     ));


        /////////////////////////////////
        // BROKEN AND MUST BE FIXED:
        /////////////////////////////////
        assertTrue(runTestQuery(mMailboxId, "date:(01/01/2001 02/02/2002)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "date:-1d date:(01/01/2001 02/02/2002)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));


        assertTrue(runTestQuery(mMailboxId, "in:(trash -junk)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "date:(-1d or -2d)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "date:\"-4d\"", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "date:-4d", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "date:\"+1d\"", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "date:+2w", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));

        assertTrue(runTestQuery(mMailboxId, "not date:(1/1/2004 or 2/1/2004)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));


        /////////////////////////////////
        // Parser-only checks
        /////////////////////////////////
        assertTrue(runTestQuery(mMailboxId, "content:foo", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "content:\"foo bar\"", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "from:foo@bar.com", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "from:\"foo bar\"", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "to:foo@bar.com", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "to:\"foo bar\"", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "cc:foo@bar.com", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "cc:\"foo bar\"", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "subject:this_is_my_subject subject:\"this is_my_subject\"", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "in:inbox", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "has:attachment has:phone has:url", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "filename:foo filename:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "type:attachment type:text type:application type:word type:msword "+
                "type:excel type:xls type:ppt type:pdf type:ms-tnef type:image type:jpeg type:gif type:bmp "+
                "type:none type:any", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "is:(read unread flagged unflagged \"sent\" received replied unreplied forwarded unforwarded)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));

        // known broken:
//        assertTrue(runTestQuery(mM, "date:2001/01/13", false, NO_EXPECTED_CHECK));
        assertTrue(runTestQuery(mMailboxId, "date:+1d", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));

//        assertTrue(runTestQuery(mM, "before:-1d before:(01/01/2001 2/2/2002)", false, NO_EXPECTED_CHECK));

        // broken:
//        assertTrue(runTestQuery(mM, "before:-1d before:(-1d 10d -100d 1w -10w -100h 1y -10y)", false, NO_EXPECTED_CHECK));

//        assertTrue(runTestQuery(mM, "after:-1d after:(01/01/2001 2/2/2002)", false, NO_EXPECTED_CHECK));

        // broken:
        //assertTrue(runTestQuery(mM, "after:-1d after:(01/01/2001 2001/01/02 +1d -1d +10d -100d +1w -10w -100h 1y -10y)", false, NO_EXPECTED_CHECK));

        assertTrue(runTestQuery(mMailboxId, "size:(1 20 300 1k <1k 10k >10k 100kb 34mb)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "larger:(1 20 300 100kb 34mb)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "smaller:(1 20 300 100kb 34mb)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));

        assertTrue(runTestQuery(mMailboxId, "author:foo author:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "title:foo title:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "keywords:foo keywords:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "company:foo company:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "foo sort:score and bar", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));

        /////////////////////////////////
        // Search checks -- if you change the default data set these may start to fail!
        //                  just update the expected value list (order matters but case doesn't!)
        /////////////////////////////////

        // test A and (B or C) -- where B and C cannot be combined)
        assertTrue(runTestQuery(mMailboxId, "in:inbox ((after:1/1/2006 and welcome) or from:ross)", false, new QueryResult[]
        {
            new QueryResult("Welcome to the Zimbra Collaboration Suite source!"),
            new QueryResult("Here are my ski pictures!"),
            new QueryResult(""),
            new QueryResult("Linux Desktop Info"),
            new QueryResult("meeting")
        }, SortBy.DATE_DESCENDING
        ));

        assertTrue(runTestQuery(mMailboxId, "contributing to xmlbeans ", false,
                new QueryResult[] {
                new QueryResult("Contributing to XMLBeans"),
                new QueryResult("XmlBeans.jar size"),
                new QueryResult("XmlBeans project logo"),
        }, SortBy.DATE_DESCENDING
        ));

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
        }, SortBy.DATE_DESCENDING
        ));

        assertTrue(runTestQuery(mMailboxId, "subject:linux", false, new QueryResult[]
                                                                           {
                new QueryResult("Linux Desktop Info")
                                                                           }, SortBy.DATE_DESCENDING
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
                                                                                }, SortBy.DATE_DESCENDING
        ));

        assertTrue(runTestQuery(mMailboxId, "(linux or has:ssn) and before:1/1/2009 and -subject:\"zipped document\"", false, new QueryResult[]
                                                                   {
                new QueryResult("Frequent system freezes after kernel bug"),
                new QueryResult("Linux Desktop Info")
//                ,new QueryResult(0, "numbers")
                                                                   }, SortBy.DATE_DESCENDING
        ));

        assertTrue(runTestQuery(mMailboxId, "larger:1M", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "foo or not foo", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));

        assertTrue(runTestQuery(mMailboxId, "(foo or not foo) and larger:1M", false, new QueryResult[]
                                                                                            {
                                     new QueryResult("Here are my ski pictures!")
                                     ,new QueryResult("AdminGuide...")
                                                                                            }, SortBy.DATE_DESCENDING
                             ));

        {
            ResultValidator val = new ResultValidator()
            {
                int cmpId = Mailbox.ID_FOLDER_INBOX;
                public void validate(ZimbraHit hit) throws ServiceException
                {
                    MessageHit mh = (MessageHit)hit;
                    //                Date date = new Date(mh.getDateHeader());
                    //                assertTrue("Date out of range for " + mh.toString(), date.before(compDate));
                    //System.out.print("Tags for "+mh.toString()+": ");
                    int msgFolderId = mh.getFolderId();
//                    System.out.println("\tCompFolder = "+compFolder.toString()+" id="+cmpId);
//                    System.out.println("\tMessageFolder id="+msgFolderId);
                    assertTrue("Folder-Checking "+mh.toString()+" for INBOX",
                            cmpId == msgFolderId);
                }
            };
            runTestQuery(mMailboxId, "in:inbox", false, getMessageTypes(false), SortBy.DATE_DESCENDING, NO_EXPECTED_CHECK, val);
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

            runTestQuery(mMailboxId, "before:1/1/2004", false, getMessageTypes(false), SortBy.DATE_DESCENDING, NO_EXPECTED_CHECK, val);
        }

        assertTrue(runTestQuery(mMailboxId, "metadata:foo metadata:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "sort:score metadata:foo metadata:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "metadata:foo sort:score and not metadata:foo sort:score and metadata:(\"foo\" \"foo bar\" gub)", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "metadata:foo sort:score metadata:(\"foo\" \"foo bar\" gub) sort:score", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "item:({1,2,3} or {4,5,6})", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
        assertTrue(runTestQuery(mMailboxId, "item:({1,2,3}) is:unread", false, NO_EXPECTED_CHECK, SortBy.DATE_DESCENDING));
    }
        
        long endTime = System.currentTimeMillis();
        long time = endTime - startTime;
        System.out.println("UnitTests completed in: "+time+"ms");
    }

    public static void makeTestQuery(int mailboxId, String qstr, boolean conv)
    {
        try {
            QueryResult[] ret = runQuery(mailboxId, qstr, conv, getMessageTypes(conv), MailboxIndex.SortBy.DATE_DESCENDING, null);

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
        Mailbox mbx = MailboxManager.getInstance().getMailboxById(1);
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
                Assert.assertTrue("Tag-Checking "+mhStr+" for "+compTagStr, mh.isTagged(compTag));
            }
        };
        runTestQuery(mMailboxId, "tag:"+tagName, false, getMessageTypes(false), SortBy.DATE_DESCENDING, NO_EXPECTED_CHECK, val);
    }

    public static final QueryResult[] NO_EXPECTED_CHECK = {};

    static final byte[] getTypes(Byte... in) {
        Set<Byte> types = new HashSet<Byte>();
        for (Byte b : in) {
            types.add(b);
        }
        byte[] toRet = new byte[types.size()];
        int i = 0;
        for (Byte b : types) 
            toRet[i++] = b;
        return toRet;
    }
    
    static final byte[] getMessageTypes(boolean conv) {
        byte types[] = new byte[1];
        if (conv) 
            types[0] = MailItem.TYPE_CONVERSATION;
        else
            types[0] = MailItem.TYPE_MESSAGE;
        return types;
    }

    boolean runTestQuery(int mailboxId, String qstr, boolean conv, QueryResult[] expected, SortBy sort) {
        byte[] types = getMessageTypes(conv);
        return runTestQuery(mailboxId, qstr, conv, types, SortBy.DATE_DESCENDING, expected, null);
    }
    
    boolean runTestQuery(int mailboxId, String qstr, boolean conv, byte[] types, SortBy sort,
                                QueryResult[] expected, ResultValidator validator)
    {
        try {
            if ((expected != null && expected != NO_EXPECTED_CHECK) && validator != null) 
                throw new IllegalArgumentException("Only one of subject validator or expected may be passed!");
            
            if (expected != null && expected != NO_EXPECTED_CHECK)
                validator = new ExpectedHitValidator((ResultValidator.ExpectedHit[])expected);
            
            runQuery(mailboxId, qstr, conv, types, sort, validator);
//            checkExpected(qstr, ret, expected);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Caught an IOException running test query "+qstr+" for mailbox "+mailboxId);
        } catch (ServiceException e) {
            e.printStackTrace();
            fail("Caught a service exception running test query "+qstr);
        } catch (ParseException e) {
            e.printStackTrace();
            fail("Caught a parse exception running test query "+qstr);
        }
        return true;
    }
    
    /**
     * 
     * @param mailboxId
     * @param qstr
     * @param conv
     * @param types
     * @param sort TODO
     * @param validator
     * @return
     * @throws IOException
     * @throws MailServiceException
     * @throws ParseException
     * @throws ServiceException
     */
    static QueryResult[] runQuery(int mailboxId, String qstr, 
        boolean conv, byte[] types, SortBy sort, ResultValidator validator) throws IOException, 
        MailServiceException, ParseException, ServiceException {
        
        ArrayList<QueryResult> ret = new ArrayList<QueryResult>();

        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mailboxId);

        SearchParams params = new SearchParams();
        params.setQueryStr(qstr);
        params.setTypes(types);
        params.setSortBy(SortBy.DATE_DESCENDING);
        params.setOffset(0);
        params.setLimit(100);
        params.setPrefetch(true);
        params.setMode(SearchResultMode.NORMAL);
        ZimbraQuery zq = new ZimbraQuery(mbox, params);
        ZimbraQueryResults res = zq.execute(null, SoapProtocol.Soap12);

        try {
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

            final int HITS_PER_PAGE = 20;
            
            int totalShown = 0;

            ZimbraHit hit = res.skipToHit(0);
            while (hit != null) {
                for (int i = 0; (hit != null) && (i < HITS_PER_PAGE); i++) {
                    if (conv) {
                        ConversationHit ch = (ConversationHit) hit;
                        Date date = new Date(ch.getHitDate());
                        System.out.println(ch.toString() + " " + date + " " + ch.getSubject() + "  (" + ch.getNumMessageHits() + ")");
                        totalShown++;
                        ret.add(new QueryResult(ch.getSubject()));
                        if (validator != null) { 
                            try {
                                validator.validate(hit);
                            } catch (ServiceException e) {
                                StringBuilder toRet = new StringBuilder("\nReceived Hits:\n");
                                for (QueryResult q : ret) {
                                    toRet.append('\t').append(q.toString()).append('\n');
                                }
                                throw ServiceException.FAILURE(toRet.append(e.getMessage()).toString(), null);
                            }
                        }
                    } else {
                        if (hit instanceof MessageHit) {
                            MessageHit mh = (MessageHit)hit;
                            totalShown++;
                            ret.add(new QueryResult(mh.getSubject()));
                            if (null != validator) { 
                                try {
                                    validator.validate(hit);
                                } catch (ServiceException e) {
                                    StringBuilder toRet = new StringBuilder("\nReceived Hits:\n");
                                    for (QueryResult q : ret) {
                                        toRet.append('\t').append(q.toString()).append('\n');
                                    }
                                    throw ServiceException.FAILURE(toRet.append(e.getMessage()).toString(), null);
                                }
                            }
                        } else {
                            totalShown++;
                            ret.add(new QueryResult(hit.getClass().getName()));
                        }
                    }
                    hit = res.getNext();
                }
            }
            
            if (validator != null && !validator.numReceived(ret.size())) {
                String errStr = "Unexpected number of return values (expected "+validator.numExpected()+", returned "+ret.size()+")";
                int ilen = Math.max(ret.size(), validator.numExpected());
                for (int i = 0; i < ilen; i++) {
                    if (i < ret.size()) {
                        errStr+="\n\t\t"+i+") ret=" + ret.get(i).toString();
                    } else {
                        errStr+="\n\t\t"+i+") ret=NO_MORE";
                    }
                    
                    if (i < validator.numExpected()) {
                        errStr+="\n\t\t"+i+") exp=" + validator.getExpected(i).toString()+"\n";
                    } else {
                        errStr+="\n\t\t"+i+") exp=NO_MORE\n";
                    }
                }
                
                fail("\n\tQuery=\""+qstr+"\" -- "+errStr);
            }
        } finally {
            res.doneWithSearchResults();
        }
        QueryResult[] retArray = new QueryResult[ret.size()];
        return (QueryResult[])ret.toArray(retArray);

    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public static void runTests()
    {
        TestSuite suite = new TestSuite(TestSearch.class);
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
    
    public int mMailboxId;

    public TestSearch() {
    }
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        Mailbox mbox = TestUtil.getMailbox("user1");
        mMailboxId = mbox.getId();
    }

    public static void main(String[] args)
    {
        MailboxIndex.startup();
        RedoLogProvider redoLog = RedoLogProvider.getInstance();
        try {
            redoLog.startup();
        } catch (ServiceException e) { e.printStackTrace(); }
        
        CliUtil.toolSetup("DEBUG");
        ZimbraLog.account.info("INFO TEST!");
        ZimbraLog.account.debug("DEBUG TEST!");

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            if (args.length > 0) {
                String command = args[0];
                if (command.equals("make")) {
                    while(true) {
                        System.out.print("Type Query>");
                        String query = in.readLine();
                        makeTestQuery(1, query, false);
                    }
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

        runTests();
        MailboxIndex.shutdown();
        // hack: some system thread isn't cleaned up...just exit
        System.exit(1);
    }

}
