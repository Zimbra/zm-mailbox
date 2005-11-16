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
 * Created on May 26, 2004
 */
package com.zimbra.soap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dom4j.DocumentException;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.zimbra.cs.util.ByteUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;

/**
 * @author schemers
 */
public class SoapTestHarness {

	public static final String NAMESPACE_STR = "urn:zimbraTestHarness";
	public static final Namespace NAMESPACE = Namespace.get("test", NAMESPACE_STR);
	public static final QName E_TESTS = QName.get("tests", NAMESPACE);
	public static final QName E_TEST = QName.get("test", NAMESPACE);
	public static final QName E_REQUEST = QName.get("request", NAMESPACE);
	public static final QName E_RESPONSE = QName.get("response", NAMESPACE);
	public static final QName E_PROPERTY = QName.get("property", NAMESPACE);
	public static final QName E_NAMESPACE = QName.get("namespace", NAMESPACE);    
	public static final QName E_SELECT = QName.get("select", NAMESPACE);
	public static final QName E_ECHO = QName.get("echo", NAMESPACE);
	
	//
    public static final String A_ATTR = "attr";
    public static final String A_NAME = "name";
    public static final String A_PREFIX = "prefix";
    public static final String A_URI = "uri";
	public static final String A_PATH = "path";
	public static final String A_VALUE = "value";
	public static final String A_SET = "set";
	public static final String A_REQUIRED = "required";
	public static final String A_DUMP = "dump";
	public static final String A_MATCH = "match";

	private static Pattern mPropPattern = Pattern.compile("(\\$\\{([^}]+)\\})");
	
	private SoapProtocol mSoapProto;
    private SoapProtocol mResponseProto;
	private SoapHttpTransport mTransport;
	private String mAuthToken;
    private String mSessionId;
    private long mCounter = 0;
	
	/** any props we have set */
	private HashMap mProps; 
	private boolean mDebug;
	
	/** <uri> */
	private String mUri;

	private int mTestNum;
	
	private Test mCurrent;
	private ArrayList mTests;

	public static class Test {
		/** the <test> element */
		public Element mTest;
		
		/** doc in the soap body */
		public  Element mDocRequest;
		/** doc in the soap body */
		public  Element mDocResponse;
		
		/** the soap request envelope */
		public Element mSoapRequest;
		
		/** the soap response envelope */
		public Element mSoapResponse;
		
		/** test number */
		public int mTestNum;
		
		/** total number of checks made on this test */
		public int mNumChecks;
		
		/** the number that failed */
		public int mNumCheckFails;

		/** request time in msecs */
		public long mTime;

		public void check(boolean ok, String message) {
			mNumChecks++;
			if (!ok)
				mNumCheckFails++;
		}
		
		public boolean isRequired() {
			return "true".equals(mTest.getAttribute(A_REQUIRED, null));
		}
		
		public boolean testFailed() {
			return mNumCheckFails > 0;
		}
		
		public String getDocReqName() {
			return mDocRequest.getName();
		}
		
		public String getDocRespName() {
			return mDocResponse.getName();
		}

		public String getStatus() {
			StringBuffer status = new StringBuffer();
		
			if (testFailed()) {
				status.append("FAIL ");
			} else {
				status.append("PASS ");
			}

			status.append(lpadz(mTestNum+"", 4));
			
			status.append(" ");

			int pass = mNumChecks - mNumCheckFails;
			status.append(lpad(pass + "/" + mNumChecks, 8));
			
			status.append(" ");
			
			status.append(lpad(mTime+"ms", 7));
			
			status.append("    ");
			
			status.append(rpad(getDocReqName(), 40));
		
			return status.toString();
		}

		/**
		 * @@return
		 */
		public boolean dumpTest() {
			return "true".equals(mTest.getAttribute(A_DUMP, null));
		}
	}

	public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(SoapTestHarness.class.getName(), options); 
        System.exit(1);
    }
    
	public static void main(String args[]) 
    throws HarnessException, SoapFaultException, IOException, DocumentException {
		SoapTestHarness harness = new SoapTestHarness();
		harness.runTests(args);
	}

	public SoapTestHarness() {
		mProps = new HashMap();
        mSoapProto = SoapProtocol.Soap12;
        mResponseProto = SoapProtocol.Soap12;
		mTests = new ArrayList();
	}
	
	public void runTests(String args[]) 
	throws HarnessException, SoapFaultException, IOException, DocumentException {

        Zimbra.toolSetup();
		
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        
        options.addOption("h", "help", false, "print usage");
        options.addOption("d", "debug", false, "debug");
        
        Option fileOpt = new Option("f", "file", true, "input document");
        fileOpt.setArgName("request-document");
        fileOpt.setRequired(true);
        options.addOption(fileOpt);

        CommandLine cl = null;
        boolean err = false;
        try {
            cl = parser.parse(options, args);
        } catch (ParseException pe) {
            System.err.println("error: "+pe.getMessage());
            err = true;
        }
        
        if (err || cl.hasOption("help"))
            usage(options);

        mDebug = cl.hasOption("d");
        String file = cl.getOptionValue("f");

        String docStr = new String(ByteUtil.getContent(new File(file)), "utf-8");
        doTests(Element.parseXML(docStr));
        
        //Element request = doc.getRootElement();
        //Element response = trans.invoke(request, isRaw);
        //System.out.println(DomUtil.toString(response, true));

        if (mTransport != null)
	        mTransport.shutdown();
	}
	
	private void setProperty(String name, String value) throws HarnessException {
		mProps.put(name, value);
		if (name.equals("uri")) {
			if (value == null)
				throw new HarnessException("need value for uri");
			mUri = value;
			mTransport = new SoapHttpTransport(mUri);
		} else if (name.equals("authToken")) {
			if (value == null)
				throw new HarnessException("need value for authToken");
			mAuthToken = value;
        } else if (name.equals("sessionId")) {
            if (value != null)
                mSessionId = value;
        } else if (name.equals("protocol")) {
            mResponseProto = "js".equalsIgnoreCase(value) ? SoapProtocol.SoapJS : SoapProtocol.Soap12;
		}
	}
	
	private void doEcho(Element e) {
		System.out.println(e.getTextTrim());
	}

    private void checkGlobals(Element e) throws HarnessException {
        if (e.getQName().equals(E_ECHO)) {
            doEcho(e);
	    } else if (e.getQName().equals(E_PROPERTY)) {
	        doProperty(e);
	    } else if (e.getQName().equals(E_NAMESPACE)) {
	        doNamespace(e);
	    }
    }
	
	private void doProperty(Element property) throws HarnessException
	{
		String name = property.getAttribute(A_NAME, null);
		String value = property.getAttribute(A_VALUE, null);
		if (name == null)
			throw new HarnessException("<property> tag missing name");
		if (value == null)
			throw new HarnessException("<property> tag missing value");
		setProperty(name, value);
		//System.out.println(name + "=" + value);
	}

    
    private void doNamespace(Element property) throws HarnessException
    {
        String prefix = property.getAttribute(A_PREFIX, null);
        String uri = property.getAttribute(A_URI, null);
        if (prefix == null)
            throw new HarnessException("<namespace> tag missing prefix");
        if (uri == null)
            throw new HarnessException("<namespace> tag missing uri");
        getURIs(SoapProtocol.Soap12).put(prefix, uri);
        getURIs(SoapProtocol.SoapJS).put(prefix, uri);
    }
    
	private void doTests(Element root) throws HarnessException, SoapFaultException, IOException
	{
		if (!root.getQName().equals(E_TESTS))
			throw new HarnessException("root document node must be " + E_TESTS.getQualifiedName());
	
		for (Iterator it = root.elementIterator(); it.hasNext(); ) {
			Element e = (Element) it.next();
//            e = expandProps(e.createCopy());
            e = expandProps(e);
			if (e.getQName().equals(E_TEST)) {
				if (!doTest(e))
					break;
			} else {
				checkGlobals(e);
			}
		}
		
		for (Iterator it = mTests.iterator(); it.hasNext(); ) {
			Test t = (Test) it.next();
			System.out.println(t.getStatus());
		}
	}
	
	
	private boolean doTest(Element test) throws SoapFaultException, IOException, HarnessException {
		mCurrent = new Test();
		mTests.add(mCurrent);
		mCurrent.mTestNum = ++mTestNum;
		mCurrent.mTest = test;
		
		for (Iterator it = test.elementIterator(); it.hasNext(); ) {
			Element e = (Element) it.next();
			if (e.getQName().equals(E_REQUEST)) {
				doRequest(e);
			} else if (e.getQName().equals(E_RESPONSE)) {	
				doResponse(e);
			} else {
				checkGlobals(e);				
			}
		}
		
		//System.out.println(mCurrent.getStatus());
		//System.out.println("  Response: " + mDocResponse.getQualifiedName());
		if (mDebug || mCurrent.testFailed() || mCurrent.dumpTest()) {
			System.out.println("----");
			System.out.println(mCurrent.mSoapRequest.prettyPrint());
			System.out.println("----");
			System.out.println(mCurrent.mSoapResponse.prettyPrint());
			System.out.println("----");
		}		
		
		if (!mCurrent.isRequired())
			return true;
		else 
			return !mCurrent.testFailed();

		//return !mCurrent.isRequired() || !mCurrent.testFailed();
	}

	private String expandAllProps(String text) throws HarnessException {
		Matcher matcher = mPropPattern.matcher(text);
		StringBuffer sb = new StringBuffer();
		//System.out.println("("+text+")");
		while (matcher.find()) {
            String name = matcher.group(2);
			String replace = (String) mProps.get(name);
            if (replace == null) {
                if (name.equals("TIME")) {
                    replace = System.currentTimeMillis()+"";
                } else if (name.equals("COUNTER")) {
                    replace = ++mCounter +"";
                }
            }
            
			if (replace != null) {
				matcher.appendReplacement(sb, replace);
			} else {
                 throw new HarnessException("unknown property: "+matcher.group(1));
				//matcher.appendReplacement(sb, matcher.group(1));
			}
		}
		matcher.appendTail(sb);
		text = sb.toString();
		//System.out.println("("+text+")");
		return text;
	}
	
	private Element expandProps(Element doc) throws HarnessException {
		for (Iterator it = doc.elementIterator(); it.hasNext(); ) {
			Element e = (Element) it.next();
			expandProps(e);
		}
		for (Iterator it = doc.attributeIterator(); it.hasNext();) {
			Element.Attribute attr = (Element.Attribute) it.next();
			String text = attr.getValue();
			if (text.indexOf("${") != -1)
				attr.setValue(expandAllProps(text));
		}
		String text = doc.getText();
		if (text.indexOf("${") != -1)
			doc.setText(expandAllProps(text));
		return doc;
	}

	static String lpad(String s, int width) {
		return pad(s, width, false, true);
	}
	
	static String rpad(String s, int width) {
		return pad(s, width, false, false);
	}
	
	static String lpadz(String s, int width) {
		return pad(s, width, true, true);
	}
	
	static String rpadz(String s, int width) {
		return pad(s, width, true, false);
	}
	
	/**
	 * @@param testNum
	 * @@return
	 */
	private static String pad(String s, int width, boolean withZero, boolean left) {
		int needed = width - s.length();
		
		if (needed <= 0 )
			return s;
		
		StringBuffer sb = new StringBuffer(width);

		if (left) {
			while (needed-- > 0) {
				sb.append(withZero ? '0' : ' ');
			}
			sb.append(s);
		} else {
			sb.append(s);
			while (needed-- > 0) {
				sb.append(withZero ? '0' : ' ');
			}
		}
		return sb.toString();
	}

	private void doRequest(Element request) throws SoapFaultException, IOException {
//        mCurrent.mDocRequest = ((Element) request.elementIterator().next()).createCopy();
        mCurrent.mDocRequest = (Element) request.elementIterator().next();
        mCurrent.mDocRequest.detach();
		if (mAuthToken == null)
			mCurrent.mSoapRequest = mSoapProto.soapEnvelope(mCurrent.mDocRequest);
		else {
			mCurrent.mSoapRequest = mSoapProto.soapEnvelope(mCurrent.mDocRequest, ZimbraContext.toCtxt(mSoapProto, mAuthToken, mSessionId));
            if (mResponseProto == SoapProtocol.SoapJS) {
                Element context = mCurrent.mSoapRequest.getOptionalElement(mSoapProto.getHeaderQName()).getOptionalElement(ZimbraContext.CONTEXT);
                if (context != null)
                    context.addElement(ZimbraContext.E_FORMAT).addAttribute(ZimbraContext.A_TYPE, ZimbraContext.TYPE_JAVASCRIPT);
            }
//            try {
//                Element ctxt = mCurrent.mSoapRequest.getElement(SoapProtocol.Soap12.mHeaderQName).getElement(ZimbraContext.CONTEXT);
//                ctxt.addElement(ZimbraContext.E_CHANGE)
//                    .addAttribute(ZimbraContext.A_TYPE, ZimbraContext.CHANGE_CREATED)
//                    .addAttribute(ZimbraContext.A_CHANGE_ID, 190);
//            } catch (ServiceException e) { }
        }

		long start = System.currentTimeMillis();
		mCurrent.mSoapResponse = mTransport.invokeRaw(mCurrent.mSoapRequest);
		long finish = System.currentTimeMillis();

        mCurrent.mTime = finish - start;
		mCurrent.mDocResponse = mResponseProto.getBodyElement(mCurrent.mSoapResponse);
	}

    private static Map mURIs = null;
    private static Map mJSURIs = null;

    private static Map getURIs(SoapProtocol proto) {
        if (mURIs == null) {
            mURIs = new HashMap();
            mURIs.put("zimbra", "urn:zimbra");
            mURIs.put("acct", "urn:zimbraAccount");
            mURIs.put("mail", "urn:zimbraMail");
            mURIs.put("admin", "urn:zimbraAdmin");
            mURIs.put("soap", "http://www.w3.org/2003/05/soap-envelope");
            mURIs.put("soap12", "http://www.w3.org/2003/05/soap-envelope");            
            mURIs.put("soap11", "http://schemas.xmlsoap.org/soap/envelope/");

            mJSURIs = new HashMap();
            mJSURIs.put("zimbra", "urn:zimbra");
            mJSURIs.put("acct", "urn:zimbraAccount");
            mJSURIs.put("mail", "urn:zimbraMail");
            mJSURIs.put("admin", "urn:zimbraAdmin");
            mJSURIs.put("soap", "urn:zimbraSoap");
            mJSURIs.put("soapJS", "urn:zimbraSoap");            
        }
        return proto == SoapProtocol.SoapJS ? mJSURIs : mURIs;
    }
    
	/*
    XPath xpath = response.createXPath(path);
    Map uris = new HashMap();
    uris.put("acct", "urn:zimbraAccount");
    xpath.setNamespaceURIs(uris);
    xpath.selectSingleNode(response);
    System.out.println(xpath.selectSingleNode(response));
	*/

	private void doSelect(Element context, Element parent) 
	throws SoapFaultException, IOException, HarnessException {

		String path = parent.getAttribute(A_PATH, null);
        String attr = parent.getAttribute(A_ATTR, null);
        String match = parent.getAttribute(A_MATCH, null);

        Element se;
        if (path != null) {
            // FIXME: hacky!
            org.dom4j.Element d4context = context.toXML();
            org.dom4j.XPath xpath = d4context.createXPath(path);
            xpath.setNamespaceURIs(getURIs(mResponseProto));
            org.dom4j.Node node = xpath.selectSingleNode(d4context);
            //System.out.println("path = " + path + " node = " + node);
            if (!(node instanceof org.dom4j.Element)) {
            	mCurrent.check(false, "select failed: " + path);
                return;
            } else {
                se = Element.convertDOM((org.dom4j.Element) node);
            	mCurrent.check(true, "select ok: " + path);
            }
        } else {
            se = context;
        }
			
        String value;
		if (attr != null) {
            value = se.getAttribute(attr, null);
        } else {
            value = se.getText();
        }

		if (match != null) {
			boolean ok = Pattern.matches(match, value);
			mCurrent.check(ok, "match "+ (ok ? "ok" : "failed") + " (" + match + ")" + " (" + value + ")");
		}
		
		//System.out.println(se.getText());
		String property = parent.getAttribute(A_SET, null);
		if (property != null) {
		    //System.out.println(property+" "+value);
		    setProperty(property, value);
		}

		for (Iterator it = parent.elementIterator(); it.hasNext(); ) {
		    Element e = (Element) it.next();
		    if (e.getQName().equals(E_SELECT)) {
		        doSelect(se, e);
		    } else {
		    	checkGlobals(e);
		    }
		}
	}
	
	private void doResponse(Element test) throws SoapFaultException, IOException, HarnessException {
		for (Iterator it = test.elementIterator(); it.hasNext(); ) {
			Element e = (Element) it.next();
			if (e.getQName().equals(E_SELECT)) {
				doSelect(mCurrent.mDocResponse, e);
			} else {
		    	checkGlobals(e);
			}
		}
	}
	
	public static class HarnessException extends Exception {
		public HarnessException(String message) {
			super(message);
		}
		
		public HarnessException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
