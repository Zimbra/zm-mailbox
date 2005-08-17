package com.zimbra.soap;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.util.LiquidPerf;
import com.liquidsys.coco.service.util.ThreadLocalData;
import com.liquidsys.coco.servlet.LiquidServlet;
import com.liquidsys.coco.stats.StopWatch;
import com.liquidsys.coco.util.Liquid;
import com.liquidsys.coco.util.LiquidLog;

/**
 * The soap service servlet
 */

public class SoapServlet extends LiquidServlet {

    private static final String PARAM_ENGINE_HANDLER = "engine.handler.";

    /** context name of auth token extracted from cookie */
    public static final String LIQUID_AUTH_TOKEN = "liquid.authToken";    
    /** context name of servlet HTTP request */
    public static final String SERVLET_REQUEST = "servlet.request";
    
    private SoapEngine mEngine;

    public void init() throws ServletException {
        String name = getServletName();
        LiquidLog.soap.info("Servlet " + name + " starting up");
        super.init();

        mEngine = new SoapEngine();

        int i=0;
        String cname;
        while ((cname = getInitParameter(PARAM_ENGINE_HANDLER+i)) != null) {
            loadHandler(cname);
            i++;
        }
        if (i==0)
            throw new ServletException("Must specify at least one handler "+PARAM_ENGINE_HANDLER+i);

        try {
            Liquid.startup();
        } catch (Throwable t) {
            LiquidLog.soap.fatal("Unable to start servlet", t);
        	throw new UnavailableException(t.getMessage());
        }
    }

    public void destroy() {
        String name = getServletName();
        LiquidLog.soap.info("Servlet " + name + " shutting down");
        try {
            Liquid.shutdown();
        } catch (ServiceException e) {
            // Log as error and ignore.
        	LiquidLog.soap.error("Exception while shutting down servlet " + name, e);
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
            throw new ServletException("can't find handler initializer class "+cname,
                                       cnfe);
        } catch (Throwable t) {
            throw new ServletException("can't find handler initializer class " + cname, t);
        }

        Object dispatcher;

        try {
            dispatcher = dispatcherClass.newInstance();
        } catch (InstantiationException ie) {
            throw new ServletException("can't instantiate class "+cname,
                                       ie);
        } catch (IllegalAccessException iae) {
            throw new ServletException("can't instantiate class "+cname,
                                       iae);
        }

        if (!(dispatcher instanceof DocumentService)) {
            throw new ServletException(
                   "class not an instanceof HandlerInitializer: "+cname);
        }

        DocumentService hi = (DocumentService)dispatcher;
        hi.registerHandlers(mEngine.getDocumentDispatcher());
    }
    
    private static StopWatch sSoapStopWatch = StopWatch.getInstance("Soap");
 
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        if (LiquidLog.perf.isDebugEnabled()) {
            ThreadLocalData.reset();
        }
        
        int len = req.getContentLength();
        
        byte[] buffer;
        
        if (len == -1) {
            buffer = readUntilEOF(req.getInputStream());
        } else {
            buffer = new byte[len];
            readFully(req.getInputStream(), buffer, 0, len);
        }
        if (LiquidLog.soap.isDebugEnabled()) {
            LiquidLog.soap.debug("SOAP request:\n" + new String(buffer, "utf8"));
        }
        
        String realHost = req.getParameter("host");
        
        if (realHost != null) {
            String realPort = req.getParameter("port");	
            proxyPost(req, resp, realHost, realPort, buffer);
        } else {
            long startTime = sSoapStopWatch.start();
            
            HashMap context = new HashMap();
            context.put(SERVLET_REQUEST, req);
            context.put(SoapEngine.REQUEST_IP, req.getRemoteAddr());            
            //checkAuthToken(req.getCookies(), context);

            Element envelope = null;
            try {
                envelope = mEngine.dispatch(req.getRequestURI(), buffer, context);
            } catch (Throwable e) {
                if (e instanceof OutOfMemoryError) {
                    Liquid.halt("handler exception", e);
                }
                LiquidLog.soap.warn("handler exception", e);
                Element fault = SoapProtocol.Soap12.soapFault(ServiceException.FAILURE(e.toString(), e));
                envelope = SoapProtocol.Soap12.soapEnvelope(fault);
            }

            SoapProtocol soapProto = SoapProtocol.determineProtocol(envelope);
            int statusCode = soapProto.hasFault(envelope) ?
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_OK;
            
            byte[] soapBytes = envelope.toUTF8();
            if (LiquidLog.soap.isDebugEnabled()) {
                LiquidLog.soap.debug("SOAP response: \n" + new String(soapBytes, "utf8"));
            }
            
            resp.setContentType(soapProto.getContentType());
            resp.setBufferSize(soapBytes.length + 2048);
            resp.setContentLength(soapBytes.length);
            resp.setStatus(statusCode);
            resp.getOutputStream().write(soapBytes);

            sSoapStopWatch.stop(startTime);
            
            // If perf logging is enabled, track server response times
            if (LiquidLog.perf.isDebugEnabled()) {
                long responseTime = System.currentTimeMillis() - startTime;
                String responseName = soapProto.getBodyElement(envelope).getName();
                LiquidPerf.writeResponseStats(responseName, responseTime,
                    ThreadLocalData.getDbTime(), ThreadLocalData.getStatementCount());
            }
        }
    }
}
