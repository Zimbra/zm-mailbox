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

package com.zimbra.soap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.map.LazyMap;
import org.apache.log4j.PropertyConfigurator;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.TrustedNetwork;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.util.Zimbra;

/**
 * The soap service servlet
 */

public class SoapServlet extends ZimbraServlet {
    private static final long serialVersionUID = 38710345271877593L;

    private static final String PARAM_ENGINE_HANDLER = "engine.handler.";

    /** context name of auth token extracted from cookie */
    public static final String ZIMBRA_AUTH_TOKEN = "zimbra.authToken";    
    /** context name of servlet HTTP request */
    public static final String SERVLET_REQUEST = "servlet.request";
    /** If this is set, then this a RESUMED servlet request (Jetty Continuation) */
    public static final String IS_RESUMED_REQUEST = "zimbra.resumedRequest";

    // Used by sExtraServices
    private static Factory sListFactory = new Factory() {
        public Object create() {
            return new ArrayList<DocumentService>();
        }
    };
    
    /**
     * Keeps track of extra services added by extensions.
     */
    private static Map<String, List<DocumentService>> sExtraServices = LazyMap.decorate(new HashMap(), sListFactory);
    
    private static Log sLog = LogFactory.getLog(SoapServlet.class);
    private SoapEngine mEngine;

    @Override public void init() throws ServletException {
        // TODO we should have a ReloadConfig soap command that will reload
        // on demand, instead of modifying and waiting for some time.
        PropertyConfigurator.configureAndWatch(LC.zimbra_log4j_properties.value());

        String name = getServletName();
        ZimbraLog.soap.info("Servlet " + name + " starting up");
        super.init();

        mEngine = new SoapEngine();

        int i = 0;
        String cname;
        while ((cname = getInitParameter(PARAM_ENGINE_HANDLER+i)) != null) {
            loadHandler(cname);
            i++;
        }
        
        // See if any extra services were perviously added by extensions 
        synchronized (sExtraServices) {
            List<DocumentService> services = sExtraServices.get(getServletName());
            for (DocumentService service : services) {
                addService(service);
                i++;
            }
        }
        
        if (i == 0)
            throw new ServletException("Must specify at least one handler "+PARAM_ENGINE_HANDLER+i);

        try {
            Zimbra.startup();
        } catch (Throwable t) {
            ZimbraLog.soap.fatal("Unable to start servlet", t);
        	throw new UnavailableException(t.getMessage());
        }
    }

    @Override public void destroy() {
        String name = getServletName();
        ZimbraLog.soap.info("Servlet " + name + " shutting down");
        try {
            Zimbra.shutdown();
        } catch (ServiceException e) {
            // Log as error and ignore.
            ZimbraLog.soap.error("ServiceException while shutting down servlet " + name, e);
        } catch (RuntimeException e) {
            ZimbraLog.soap.error("Unchecked Exception while shutting down servlet " + name, e);
            throw e;
        }
        // FIXME: we might want to add mEngine.destroy()
        // to allow the mEngine to cleanup?
        mEngine = null;

        super.destroy();
    }

    private void loadHandler(String cname) throws ServletException {
        Class dispatcherClass;
        try {
            dispatcherClass = Class.forName(cname);
        } catch (ClassNotFoundException cnfe) {
            throw new ServletException("can't find handler initializer class " + cname, cnfe);
        } catch (Throwable t) {
            throw new ServletException("can't find handler initializer class " + cname, t);
        }

        Object dispatcher;

        try {
            dispatcher = dispatcherClass.newInstance();
        } catch (InstantiationException ie) {
            throw new ServletException("can't instantiate class " + cname, ie);
        } catch (IllegalAccessException iae) {
            throw new ServletException("can't instantiate class " + cname, iae);
        }

        if (!(dispatcher instanceof DocumentService)) {
            throw new ServletException("class not an instanceof HandlerInitializer: " + cname);
        }

        DocumentService hi = (DocumentService) dispatcher;
        addService(hi);
    }
    
    /**
     * Adds a service to the instance of <code>SoapServlet</code> with the given
     * name.  If the servlet has not been loaded, stores the service for later
     * initialization.
     */
    public static void addService(String servletName, DocumentService service) {
        synchronized (sExtraServices) {
            ZimbraServlet servlet = ZimbraServlet.getServlet(servletName);
            if (servlet != null) {
                ((SoapServlet) servlet).addService(service);
            } else {
                sLog.debug("addService(" + servletName + ", " +
                    StringUtil.getSimpleClassName(service) + "): servlet has not been initialized");
                List<DocumentService> services = sExtraServices.get(servletName);
                services.add(service);
            }
        }
    }
    
    private void addService(DocumentService service) {
        ZimbraLog.soap.info("Adding service " + StringUtil.getSimpleClassName(service) + " to " + getServletName());
        service.registerHandlers(mEngine.getDocumentDispatcher());
    }

    @Override public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ZimbraLog.clearContext();
        long startTime = ZimbraPerf.STOPWATCH_SOAP.start();  

        int len = req.getContentLength();
        byte[] buffer;
        boolean isResumed = true;

        // resuming from a Jetty Continuation does *not* reset the HttpRequest's input stream -
        // therefore we store the read buffer in the Continuation, and use the stored buffer
        // if we're resuming
        buffer = (byte[])req.getAttribute("com.zimbra.request.buffer");
        if (buffer == null) {
            isResumed = false;
            
            // Look up max request size
            int maxSize = 0;
            try {
                maxSize = Provisioning.getInstance().getLocalServer().getIntAttr(Provisioning.A_zimbraSoapRequestMaxSize, 0);
            } catch (ServiceException e) {
                ZimbraLog.soap.warn("Unable to look up %s.  Not limiting the request size.", Provisioning.A_zimbraSoapRequestMaxSize, e);
            }
            if (maxSize <= 0) {
                maxSize = Integer.MAX_VALUE;
            }
            
            // Read the request
            boolean success;
            if (len > maxSize) {
                success = false;
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                success = readRequest(req.getInputStream(), baos, maxSize, len);
                buffer = baos.toByteArray();
            }
            
            // Handle requests that exceed the size limit
            if (!success) {
                String sizeString = (len < 0 ? "" : " size " + len);
                String msg = String.format("Request%s exceeded limit of %d bytes set for %s.",
                    sizeString, maxSize, Provisioning.A_zimbraSoapRequestMaxSize);
                ServiceException e = ServiceException.INVALID_REQUEST(msg, null);
                ZimbraLog.soap.warn(null, e);
                Element fault = SoapProtocol.Soap12.soapFault(e);
                Element envelope = SoapProtocol.Soap12.soapEnvelope(fault);
                sendResponse(resp, envelope);
                ZimbraLog.clearContext();
                return;
            }
            
            req.setAttribute("com.zimbra.request.buffer", buffer);
        }

        HashMap<String, Object> context = new HashMap<String, Object>();
        context.put(SERVLET_REQUEST, req);
        
        // Set the requester IP.  If the request was made by the HTML client,
        // set it to the value of the X-Originating-IP header.
        String remoteAddr = req.getRemoteAddr();
        context.put(SoapEngine.SOAP_REQUEST_IP, remoteAddr); 
        
        String origIp = req.getHeader(SoapHttpTransport.X_ORIGINATING_IP);
        if (TrustedNetwork.isIpTrusted(remoteAddr)) {
            context.put(SoapEngine.ORIG_REQUEST_IP, origIp);
            remoteAddr = origIp;
        } 
        context.put(SoapEngine.REQUEST_IP, remoteAddr); 
        
        //checkAuthToken(req.getCookies(), context);
        if (isResumed) 
            context.put(IS_RESUMED_REQUEST, "1");

        Element envelope = null;
        try {
            envelope = mEngine.dispatch(req.getRequestURI(), buffer, context);
        } catch (Throwable e) {
            if (e instanceof OutOfMemoryError) {
                Zimbra.halt("handler exception", e);
            }

            if (ZimbraLog.soap.isDebugEnabled()) {
                Boolean logged = (Boolean)context.get(SoapEngine.SOAP_REQUEST_LOGGED);
                if (logged == null || !logged)
                    ZimbraLog.soap.debug("SOAP request:\n" + new String(buffer, "utf8"));
            }

            // don't interfere with Jetty Continuations -- pass the exception right up
            if (e.getClass().getName().equals("org.mortbay.jetty.RetryRequest"))
                throw ((RuntimeException)e);

            ZimbraLog.soap.warn("handler exception", e);
            Element fault = SoapProtocol.Soap12.soapFault(ServiceException.FAILURE(e.toString(), e));
            envelope = SoapProtocol.Soap12.soapEnvelope(fault);
        }

        if (ZimbraLog.soap.isDebugEnabled()) {
            ZimbraLog.soap.debug("SOAP response: \n" + envelope.prettyPrint());
        }
        sendResponse(resp, envelope);
        
        ZimbraLog.clearContext();
        ZimbraPerf.STOPWATCH_SOAP.stop(startTime);
    }
    
    private void sendResponse(HttpServletResponse resp, Element envelope)
    throws IOException {
        SoapProtocol soapProto = SoapProtocol.determineProtocol(envelope);
        int statusCode = soapProto.hasFault(envelope) ?
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_OK;
        byte[] soapBytes = envelope.toUTF8();
        resp.setContentType(soapProto.getContentType());
        resp.setBufferSize(soapBytes.length + 2048);
        resp.setContentLength(soapBytes.length);
        resp.setStatus(statusCode);
        resp.getOutputStream().write(soapBytes);
    }

    /**
     * Reads from the <tt>InputStream</tt> and writes to the <tt>ByteArrayOutputStream</tt> until
     * either EOF or the maximum number of bytes have been read.
     * @param sizeHint the buffer size used for each read from the <tt>InputStream</tt>
     * 
     * @return <tt>true</tt> if successful, or <tt>false</tt> if the number of bytes read
     * exceeded <tt>maxBytes</tt> 
     */
    private boolean readRequest(InputStream input, ByteArrayOutputStream baos, int maxBytes, int sizeHint)
    throws IOException {
        if (sizeHint <= 0) {
            sizeHint = 2048;
        }
        byte[] buffer = new byte[sizeHint];

        int numRead = 0;
        while ((numRead = input.read(buffer)) > 0) {
            baos.write(buffer, 0, numRead);
            if (baos.size() > maxBytes) {
                return false;
            }
        }
        return true;
    }
}

