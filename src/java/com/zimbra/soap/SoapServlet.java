/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.ProtocolException;
import org.apache.log4j.PropertyConfigurator;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.BufferStream;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.RemoteIP;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.ZimbraServletOutputStream;
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
    /** context name of servlet context */
    public static final String SERVLET_CONTEXT = "servlet.context";
    /** context name of servlet HTTP request */
    public static final String SERVLET_REQUEST = "servlet.request";
    /** context name of servlet HTTP response */
    public static final String SERVLET_RESPONSE = "servlet.response";
    /** If this is set, then this a RESUMED servlet request (Jetty Continuation) */
    public static final String IS_RESUMED_REQUEST = "zimbra.resumedRequest";

    // Used by sExtraServices
    private static class ArrayListFactory
    implements Function<String, List<DocumentService>> {
        public List<DocumentService> apply(String from) {
            return new ArrayList<DocumentService>();
        }
    };
    
    /**
     * Keeps track of extra services added by extensions.
     */
    private static Map<String, List<DocumentService>> sExtraServices =
        new MapMaker().makeComputingMap(new ArrayListFactory());
    
    private static Log sLog = LogFactory.getLog(SoapServlet.class);
    private SoapEngine mEngine;

    @Override public void init() throws ServletException {
        // TODO we should have a ReloadConfig soap command that will reload
        // on demand, instead of modifying and waiting for some time.
        long watch = LC.zimbra_log4j_properties_watch.longValue();
        
        if (watch > 0)
            PropertyConfigurator.configureAndWatch(LC.zimbra_log4j_properties.value(), watch);
        else
            PropertyConfigurator.configure(LC.zimbra_log4j_properties.value());

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
        
        mEngine.getDocumentDispatcher().clearSoapWhiteList();
        
        if (i == 0)
            throw new ServletException("Must specify at least one handler "+PARAM_ENGINE_HANDLER+i);

        try {
            Zimbra.startup();
        } catch (OutOfMemoryError e) {
            Zimbra.halt("out of memory", e);
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
        Class<?> dispatcherClass = null;
        try {
            dispatcherClass = Class.forName(cname);
        } catch (ClassNotFoundException cnfe) {
            throw new ServletException("can't find handler initializer class " + cname, cnfe);
        } catch (OutOfMemoryError e) {
            Zimbra.halt("out of memory", e);
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
        
        try {
            doWork(req, resp);
        } finally {
            ZimbraLog.clearContext();
            ZimbraPerf.STOPWATCH_SOAP.stop(startTime);
        }
    }
    
    private void doWork(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
                BufferStream bs = new BufferStream(len, maxSize, maxSize);
                int in = (int)bs.readFrom(req.getInputStream(), len >= 0 ? len :
                    Integer.MAX_VALUE);
                
                if (len > 0 && in < len)
                    throw new EOFException("SOAP content truncated " + in + "!=" + len);
                success = in <= maxSize;
                buffer = bs.toByteArray();
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
                sendResponse(req, resp, envelope);
                return;
            }
            
            req.setAttribute("com.zimbra.request.buffer", buffer);
        }

        HashMap<String, Object> context = new HashMap<String, Object>();
        context.put(SERVLET_CONTEXT, getServletContext());
        context.put(SERVLET_REQUEST, req);
        context.put(SERVLET_RESPONSE, resp);
        
        // setup IPs in the context and add to logging context
        RemoteIP remoteIp = new RemoteIP(req, ZimbraServlet.getTrustedIPs());
        context.put(SoapEngine.SOAP_REQUEST_IP, remoteIp.getClientIP());
        context.put(SoapEngine.ORIG_REQUEST_IP, remoteIp.getOrigIP());
        context.put(SoapEngine.REQUEST_IP, remoteIp.getRequestIP()); 
        remoteIp.addToLoggingContext();
        
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
        sendResponse(req, resp, envelope);
    }
    
    private int soapResponseBufferSize() {
        String val = LC.soap_response_buffer_size.value();
        if (val == null || val.length() == 0)
            return -1; // will be using jetty default
        else
            return LC.soap_response_buffer_size.intValue();
    }
    
    private void sendResponse(HttpServletRequest req, HttpServletResponse resp, Element envelope)
    throws IOException {
        SoapProtocol soapProto = SoapProtocol.determineProtocol(envelope);
        int statusCode = soapProto.hasFault(envelope) ?
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_OK;
        
        boolean chunkingDisabled = LC.soap_response_chunked_transfer_encoding_disabled.booleanValue();

        if (!chunkingDisabled) {
            // disable chunking if proto < HTTP 1.1
            String proto = req.getProtocol();
            try {
                HttpVersion httpVer = HttpVersion.parse(proto);
                chunkingDisabled = httpVer.lessEquals(HttpVersion.HTTP_1_0);
            } catch (ProtocolException e) {
                ZimbraLog.soap.warn("cannot parse http version in request: " + proto + ", http chunked transfer encoding disabled", e);
                chunkingDisabled = true;
            }
        }
        
        // use jetty default if the LC key is not set
        int responseBufferSize = soapResponseBufferSize();
        if (responseBufferSize != -1) 
            resp.setBufferSize(responseBufferSize);
        
        resp.setContentType(soapProto.getContentType());  
        resp.setStatus(statusCode);
        
        if (chunkingDisabled) {
            /*
             * serialize the envelope to a byte array and send the response with Content-Length header.
             */
            byte[] soapBytes = envelope.toUTF8();
            resp.setContentLength(soapBytes.length);
            resp.getOutputStream().write(soapBytes);
        } else {
            /*
             * Let jetty chunk the response if applicable.
             */
            ZimbraServletOutputStream out = new ZimbraServletOutputStream(resp.getOutputStream());
            envelope.output(out);
            out.flush();
        }
    }
}

