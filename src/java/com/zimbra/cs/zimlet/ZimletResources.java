/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.zimlet;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.servlet.DiskCacheServlet;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class ZimletResources extends DiskCacheServlet {

    //
    // Constants
    //
    private static final String COMPRESSED_EXT = ".zgz";
    public static final String RESOURCE_PATH = "/res/";

    private static final String P_DEBUG = "debug";

    private static final String T_CSS = "css";
    private static final String T_JAVASCRIPT = "javascript";
    private static final String T_PLAIN = "plain";

    private static final Map<String, String> TYPES = new HashMap<String, String>();

    private static final Pattern RE_REMOTE = Pattern.compile("^((https?|ftps?)://|/)");
    private static final Pattern RE_CSS_URL = Pattern.compile("(url\\(['\"]?)([^'\"\\)]*)", Pattern.CASE_INSENSITIVE);

    static {
        TYPES.put("css", "text/css");
        TYPES.put("js", "text/javascript");
        TYPES.put("xsl", "application/xslt+xml");
        TYPES.put("plain", "text/plain");
    }

    public ZimletResources() {
        super("zimletres");
    }

    //
    // HttpServlet methods
    //

    public void service(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {
        if (flushCache(request))
            return;
        
        ZimbraLog.clearContext();

        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;
        
        String uri = req.getRequestURI();
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        @SuppressWarnings("unchecked")
        Set<String> zimletNames = (Set<String>) req.getAttribute(ZimletFilter.ALLOWED_ZIMLETS);

        if (!pathInfo.startsWith(RESOURCE_PATH)) {
            // handle requests for individual files included in zimlet in case dev=1 is set.
            ServletContext targetContext = getServletConfig().getServletContext().getContext("/zimlet");
            RequestDispatcher dispatcher = targetContext.getRequestDispatcher(pathInfo);
            dispatcher.forward(req, resp);
            return;
        }
        
        String contentType = getContentType(uri);
        String type = contentType.replaceAll("^.*/", "");
        boolean debug = req.getParameter(P_DEBUG) != null;

        String cacheId = getCacheId(req, type, zimletNames);

        if (ZimbraLog.zimlet.isDebugEnabled()) {
            ZimbraLog.zimlet.debug("DEBUG: uri=" + uri);
            ZimbraLog.zimlet.debug("DEBUG: cacheId=" + cacheId);
            ZimbraLog.zimlet.debug("DEBUG: contentType=" + contentType);
            ZimbraLog.zimlet.debug("DEBUG: type=" + type);
        }

        // generate buffer
        File file = !debug ? getCacheFile(cacheId) : null;
        String text = null;
        
        if (file == null || !file.exists()) {
            StringWriter writer = new StringWriter();
            PrintWriter printer = new PrintWriter(writer);

            // zimlet messages
            if (type.equals(T_JAVASCRIPT)) {
                String mailUrl = "/zimbra";
                try {
                    mailUrl = Provisioning.getInstance().getLocalServer().getMailURL();
                } catch (Exception e) {
                    ZimbraLog.zimlet.warn("can't get mailUrl", e);
                }
                ServletConfig config = this.getServletConfig();
                ServletContext baseContext = config.getServletContext();
                ServletContext clientContext = baseContext.getContext(mailUrl);
                RequestDispatcher dispatcher = clientContext.getRequestDispatcher(RESOURCE_PATH);

                for (String zimletName : zimletNames) {
                    RequestWrapper wrappedReq = new RequestWrapper(req, RESOURCE_PATH + zimletName);
                    ResponseWrapper wrappedResp = new ResponseWrapper(resp, printer);
                    wrappedReq.setParameter("debug", "1"); // bug 45922: avoid cached messages
                    dispatcher.include(wrappedReq, wrappedResp);
                }
            }

            // zimlet resources
            if (ZimbraLog.zimlet.isDebugEnabled()) ZimbraLog.zimlet.debug("DEBUG: generating buffer");
            generate(zimletNames, type, printer);
            text = writer.toString();

            // minimize css
            if (type.equals(T_CSS) && !debug) {
                CssCompressor compressor = new CssCompressor(new StringReader(text));
                StringWriter out = new StringWriter();
                compressor.compress(out, 0);
                text = out.toString();
            }

            if (type.equals(T_JAVASCRIPT) && !debug) {
                // compress JS code
				text = text.replaceAll("(^|\n)\\s*DBG\\.\\w+\\(.*\\);\\s*(\n|$)", "\n");
                JavaScriptCompressor compressor = new JavaScriptCompressor(
                    new StringReader(text), new ErrorReporter() {

                    public void warning(String message, String sourceName,
                        int line, String lineSource, int lineOffset) {
                        if (line < 0) {
                            ZimbraLog.zimlet.warn("\n" + message);
                        } else {
                            ZimbraLog.zimlet.warn("\n" + line + ':' + lineOffset + ':' + message);
                        }
                    }

                    public void error(String message, String sourceName,
                        int line, String lineSource, int lineOffset) {
                        if (line < 0) {
                            ZimbraLog.zimlet.error("\n" + message);
                        } else {
                            ZimbraLog.zimlet.error("\n" + line + ':' + lineOffset + ':' + message);
                        }
                    }

                    public EvaluatorException runtimeError(String message,
                        String sourceName, int line, String lineSource, int lineOffset) {
                        error(message, sourceName, line, lineSource, lineOffset);
                        return new EvaluatorException(message);
                    }
                });
                StringWriter out = new StringWriter();
                compressor.compress(out, 0, true, false, false, false);
                String mintext = out.toString();
                if (mintext == null) {
                    ZimbraLog.zimlet.info("unable to minimize zimlet JS source");
                } else {
                    text = mintext;
                }
            }

            // store buffer
            if (!debug) {
                file = createCacheFile(cacheId, type);
                if (ZimbraLog.zimlet.isDebugEnabled())
                    ZimbraLog.zimlet.debug("DEBUG: buffer file: " + file);
                copy(text, file);
                if (LC.zimbra_web_generate_gzip.booleanValue())
                    compress(file);
                putCacheFile(cacheId, file);
            }
        } else {
            if (ZimbraLog.zimlet.isDebugEnabled()) ZimbraLog.zimlet.debug("DEBUG: using previous buffer");
        }

        // write buffer
        boolean compress = !debug && uri.endsWith(COMPRESSED_EXT);
        try {
            // We browser sniff so need to make sure any caches do the same.
            resp.addHeader("Vary", "User-Agent");
            if (file == null || req.getProtocol().endsWith("1.0")) {
                // Bug 20626: We're no longer caching zimlets
                // Set to expire far in the past.
                resp.setHeader("Expires", "Tue, 24 Jan 2000 17:46:50 GMT");
                // Set standard HTTP/1.1 no-cache headers.
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                // Set standard HTTP/1.0 no-cache header.
                resp.setHeader("Pragma", "no-cache");

            } else {
                // force cache revalidation but allow client cache
                resp.setHeader("Cache-Control", "must-revalidate, max-age=0");
                if (file.lastModified() <= req.getDateHeader("If-Modified-Since")) {
                    resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }
                resp.setDateHeader("Last-Modified", file.lastModified());
            }
            resp.setContentType(contentType);
            resp.setContentLength(file != null ? (int)file.length() :
                text.getBytes("UTF-8").length);
        } catch (IllegalStateException e) {
            // ignore -- thrown if called from including JSP
            ZimbraLog.zimlet.debug("zimletres: " + cacheId);
            ZimbraLog.zimlet.debug("zimletres: " + e.getMessage());
        }
        if (file != null) {
            copy(file, resp, compress);
        } else {
            copy(text, resp, compress);
        }

    } // doGet(HttpServletRequest,HttpServletResponse)

    //
    // Private methods
    //

    private static MimetypesFileTypeMap sFileTypeMap = new MimetypesFileTypeMap();
    
    @SuppressWarnings("unused")
    private void printFile(HttpServletResponse resp, String zimletName,
        String file) throws IOException, ZimletException {
    	ZimletFile zf = ZimletUtil.getZimlet(zimletName);
    	if (zf == null) {
            ZimbraLog.zimlet.warn("zimlet file not found for: %s", zimletName);
    		resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    		return;
    	}
    	ZimletFile.ZimletEntry entry = zf.getEntry(file);
    	if (entry == null) {
            ZimbraLog.zimlet.warn("requested file not found for zimlet: %s (%s)",
                zimletName, file);
    		resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    		return;
    	}
    	resp.setStatus(HttpServletResponse.SC_OK);
    	resp.setHeader("Expires", "Tue, 24 Jan 2000 17:46:50 GMT");
    	resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    	resp.setHeader("Pragma", "no-cache");
        String contentType = null;
    	int dot = file.lastIndexOf('.');
    	if (dot > 0)
    	    contentType = TYPES.get(file.substring(dot + 1));
    	if (contentType == null)
    	    contentType = sFileTypeMap.getContentType(file);
        ZimbraLog.zimlet.debug("%s: %s", file, contentType);
    	if (contentType != null)
            resp.setContentType(contentType);
    	ByteUtil.copy(entry.getContentStream(), true, resp.getOutputStream(), false);
    }
    
    private void generate(Set<String> zimletNames, String type, PrintWriter out)
        throws IOException {
        boolean isCSS = type.equals(T_CSS);
        String commentStart = "/* ";
        String commentContinue = " * ";
        String commentEnd = " */";

        // create data buffers
        for (String zimlet : zimletNames) {
            ZimletFile file = ZimletUtil.getZimlet(zimlet);
            if (file == null) {
                ZimbraLog.zimlet.warn("error loading " + zimlet
                    + ": zimlet not found ");
                continue;
            }
            try {
                String[] files = isCSS ? file.getZimletDescription().getStyleSheets() :
                    file.getZimletDescription().getScripts();
                for (String f : files) {
                    // only add local files to list
                    if (RE_REMOTE.matcher(f).matches()) {
                        continue;
                    }
                    ZimletFile.ZimletEntry entry = file.getEntry(f);
                    if (entry == null)
                        continue;

                    out.println(commentStart);
                    out.print(commentContinue);
                    out.print("Zimlet: " + zimlet + "File: " + f);
                    out.println(commentEnd);
                    out.println();

                    printFile(out, new BufferedReader(new InputStreamReader(
                        entry.getContentStream())), zimlet, isCSS);
                    out.println();
                }
            } catch (ZimletException e) {
                ZimbraLog.zimlet.error("error loading " + zimlet + ": ", e);
                continue;
            }
        }
        out.flush();
    }

    private void printFile(PrintWriter out, BufferedReader in,
        String zimletName, boolean isCSS) throws IOException {
        String line;
        
        while ((line = in.readLine()) != null) {
            if (isCSS) {
                Matcher url = RE_CSS_URL.matcher(line);
                if (url.find()) {
                    int offset = 0;
                    do {
                        int start = url.start();
                        if (start > offset) {
                            out.print(line.substring(offset, start));
                        }
                        out.print(url.group(1));

                        String s = url.group(2);
                        Matcher remote = RE_REMOTE.matcher(s);
                        
                        if (!remote.find()) {
                            out.print(ZimletUtil.ZIMLET_BASE + "/" +
                                zimletName + "/");
                        }
                        out.print(s);
                        offset = url.end();
                    } while (url.find());
                    if (offset < line.length()) {
                        out.println(line.substring(offset));
                    }
                    continue;
                }
            }
            out.println(line);
        }
        in.close();
    }

    private static String getContentType(String uri) {
        int index = uri.indexOf('.');
        String type = T_PLAIN;
        if (index != -1) {
            type = uri.substring(index + 1);
            index = type.indexOf('.');
            if (index != -1) {
                type = type.substring(0, index);
            }
        }
        String contentType = TYPES.get(type);
        return contentType != null ? contentType : TYPES.get(T_PLAIN);
    }

    private String getCacheId(HttpServletRequest req, String type,
        Set<String> zimletNames) {
        StringBuilder str = new StringBuilder();

        str.append(getLocale(req).toString());
        str.append(":");
        str.append(type);
        if (req.getRequestURI().endsWith(COMPRESSED_EXT)) {
            str.append(COMPRESSED_EXT);
        }
        str.append(":");

        Iterator<String> iter = zimletNames.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            if (i > 0) {
                str.append(",");
            }
            str.append(iter.next());
        }
        return str.toString();
    }

    private static Locale getLocale(HttpServletRequest req) {
        String language = req.getParameter("language");
        if (language != null) {
            String country = req.getParameter("country");
            if (country != null) {
                String variant = req.getParameter("variant");
                if (variant != null) {
                    return new Locale(language, country, variant);
                }
                return new Locale(language, country);
            }
            return new Locale(language);
        }
        return req.getLocale();
    }

    //
    // Classes
    //

    static class RequestWrapper extends HttpServletRequestWrapper {
        private String filename;
        private Map<String,String> parameters;

        public RequestWrapper(HttpServletRequest req, String filename) {
            super(req);
            this.filename = filename;
        }

        public String getRequestURI() {
            return filename + ".js";
        }

        public void setParameter(String name, String value) {
            if (this.parameters == null) this.parameters = new HashMap<String,String>();
            this.parameters.put(name, value);
        }
        public String getParameter(String name) {
            String value = this.parameters != null ? this.parameters.get(name) : null;
            return value == null ? super.getParameter(name) : value;
        }
    }

    static class ResponseWrapper extends HttpServletResponseWrapper {
        private PrintWriter out;

        public ResponseWrapper(HttpServletResponse resp, PrintWriter out) {
            super(resp);
            this.out = out;
        }

        public void setHeader(String name, String value) { /* NOP */ }

        public void addHeader(String name, String value) { /* NOP */ }

        public ServletOutputStream getOutputStream() throws IOException {
            return new ServletStream(getWriter());
        }

        public PrintWriter getWriter() throws IOException {
            return out;
        }
    }

    static class ServletStream extends ServletOutputStream {
        private PrintWriter out;

        public ServletStream(PrintWriter out) {
            this.out = out;
        }

        public void write(int i) throws IOException {
            out.write(i);
        }

        public void print(boolean b) throws IOException {
            out.print(b);
        }

        public void print(char c) throws IOException {
            out.print(c);
        }

        public void print(float f) throws IOException {
            out.print(f);
        }

        public void print(int i) throws IOException {
            out.print(i);
        }

        public void print(long l) throws IOException {
            out.print(l);
        }

        public void print(String s) throws IOException {
            out.print(s);
        }

        public void println() throws IOException {
            out.println();
        }

        public void println(boolean b) throws IOException {
            out.println(b);
        }

        public void println(char c) throws IOException {
            out.println(c);
        }

        public void println(float f) throws IOException {
            out.println(f);
        }

        public void println(int i) throws IOException {
            out.println(i);
        }

        public void println(long l) throws IOException {
            out.println(l);
        }

        public void println(String s) throws IOException {
            out.println(s);
        }
    }

} // class ZimletResources
