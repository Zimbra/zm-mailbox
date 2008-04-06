/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.zimlet;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.servlet.ZimbraServlet;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

public class ZimletResources
        extends ZimbraServlet {

    //
    // Constants
    //

    private static final String COMPRESSED_EXT = ".zgz";

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
        TYPES.put("plain", "text/plain");
    }

    //
    // Data
    //

    /**
     * <ul>
     * <li>Key: request uri
     * <li>Value: String buffer
     * </ul>
     */
    private Map<String, byte[]> cache = new HashMap<String, byte[]>();

    //
    // HttpServlet methods
    //

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        ZimbraLog.clearContext();

        String uri = req.getRequestURI();
        String contentType = getContentType(uri);
        String type = contentType.replaceAll("^.*/", "");
        boolean debug = req.getParameter(P_DEBUG) != null;

        String cacheId = getCacheId(req, type);

        if (ZimbraLog.zimlet.isDebugEnabled()) {
            ZimbraLog.zimlet.debug("DEBUG: uri=" + uri);
            ZimbraLog.zimlet.debug("DEBUG: cacheId=" + cacheId);
            ZimbraLog.zimlet.debug("DEBUG: contentType=" + contentType);
            ZimbraLog.zimlet.debug("DEBUG: type=" + type);
        }

        // generate buffer
        byte[] buffer = !debug ? cache.get(cacheId) : null;
        if (buffer == null) {
            StringWriter writer = new StringWriter();
            PrintWriter printer = new PrintWriter(writer);

            // zimlet messages
            if (type.equals(T_JAVASCRIPT)) {
                ServletConfig config = this.getServletConfig();
                ServletContext baseContext = config.getServletContext();
                ServletContext clientContext = baseContext.getContext("/zimbra/");
                RequestDispatcher dispatcher = clientContext.getRequestDispatcher("/res/");

                List<ZimletFile> files = getZimletFiles(req, type);
                for (ZimletFile file : files) {
                    if (!file.isResourceFile) {
                        continue;
                    }
                    HttpServletRequest wrappedReq = new RequestWrapper(req, "/res/" + file.zimletName);
                    HttpServletResponse wrappedResp = new ResponseWrapper(resp, printer);
                    dispatcher.include(wrappedReq, wrappedResp);
                }
            }

            // zimlet resources
            if (ZimbraLog.zimlet.isDebugEnabled()) ZimbraLog.zimlet.debug("DEBUG: generating buffer");
            generate(req, type, printer);
            String text = writer.toString();

            // minimize css
            if (type.equals(T_CSS) && !debug) {
                CssCompressor compressor = new CssCompressor(new StringReader(text));
                StringWriter out = new StringWriter();
                compressor.compress(out, 0);
                text = out.toString();
            }

            if (type.equals(T_JAVASCRIPT) && !debug) {
                // compress JS code
                JavaScriptCompressor compressor = new JavaScriptCompressor(new StringReader(text), new ErrorReporter() {

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

                    public EvaluatorException runtimeError(String message, String sourceName,
                                                           int line, String lineSource, int lineOffset) {
                        error(message, sourceName, line, lineSource, lineOffset);
                        return new EvaluatorException(message);
                    }
                });
                StringWriter out = new StringWriter();
                compressor.compress(out, 0, true, false, false, false);
                String mintext = out.toString();
                if (mintext == null) {
                    ZimbraLog.zimlet.debug("unable to minimize zimlet JS source");
                } else {
                    text = mintext;
                }
            }

            // compress
            buffer = text.getBytes("UTF-8");
            if (uri.endsWith(COMPRESSED_EXT)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream(buffer.length);
                OutputStream gzos = new GZIPOutputStream(bos);
                gzos.write(buffer);
                gzos.close();
                buffer = bos.toByteArray();
            }

            // store buffer
            if (!debug) {
                cache.put(cacheId, buffer);
            }
        } else {
            if (ZimbraLog.zimlet.isDebugEnabled()) ZimbraLog.zimlet.debug("DEBUG: using previous buffer");
        }

        // write buffer
        try {
            // We browser sniff so need to make sure any caches do the same.
            resp.addHeader("Vary", "User-Agent");

            // Bug 20626: We're no longer caching zimlets
            // Set to expire far in the past.
            resp.setHeader("Expires", "Tue, 24 Jan 2000 17:46:50 GMT");
            // Set standard HTTP/1.1 no-cache headers.
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            // Set standard HTTP/1.0 no-cache header.
            resp.setHeader("Pragma", "no-cache");

            resp.setContentType(contentType);
            resp.setContentLength(buffer.length);
            if (uri.endsWith(COMPRESSED_EXT)) {
                resp.setHeader("Content-Encoding", "gzip");
            }
        }
        catch (IllegalStateException e) {
            // ignore -- thrown if called from including JSP
            ZimbraLog.zimlet.debug("zimletres: " + cacheId);
            ZimbraLog.zimlet.debug("zimletres: " + e.getMessage());
        }

        try {
            // print files
            OutputStream out = resp.getOutputStream();
            out.write(buffer);
            out.flush();
        }
        catch (IllegalStateException e) {
            ZimbraLog.zimlet.debug("!!! illegal state: " + e.getMessage());
            // use writer if called from including JSP
            PrintWriter out = resp.getWriter();
            out.print(buffer);
            out.flush();
        }

    } // doGet(HttpServletRequest,HttpServletResponse)

    //
    // Private methods
    //

    private void generate(HttpServletRequest req, String type, PrintWriter out)
            throws IOException {
        boolean isCSS = type.equals(T_CSS);

        String commentStart = "/* ";
        String commentContinue = " * ";
        String commentEnd = " */";

        // create data buffers
        List<ZimletFile> files = getZimletFiles(req, type);
        for (ZimletFile file : files) {
            String filename = file.getAbsolutePath();
            if (file.isResourceFile) {
                continue;
            }

            out.println(commentStart);
            out.print(commentContinue);
            out.print("File: ");
            // NOTE: Show entire path for easy debugging, comments are stripped in prod mode
            out.println(filename.replaceAll("^.*/webapps/",""));
            out.println(commentEnd);
            out.println();

            // print file
            if (file.exists()) {
                printFile(out, file, file.zimletName, isCSS);
            } else {
                out.print(commentStart);
                out.print("Error: file doesn't exist " + filename.replaceAll("^.*/webapps/",""));
                out.println(commentEnd);
            }
            out.println();
        }
        out.flush();

    } // generate(HttpServletRequest,String):String

    private void printFile(PrintWriter out, File file,
                           String zimletName, boolean isCSS)
            throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
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
                            out.print("/service/zimlet/" + zimletName + "/");
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

    private String getCacheId(HttpServletRequest req, String type) {
        Set<String> zimletNames = (Set<String>) req.getAttribute(ZimletFilter.ALLOWED_ZIMLETS);

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

    private List<ZimletFile> getZimletFiles(HttpServletRequest req, String type) {
        List<ZimletFile> files = new LinkedList<ZimletFile>();
        Set<String> zimletNames = (Set<String>) req.getAttribute(ZimletFilter.ALLOWED_ZIMLETS);
        for (String zimletName : zimletNames) {
            // read zimlet manifest
			File basedir = new File(getServletContext().getRealPath("/zimlet"));
			File dir = new File(basedir, zimletName);
			if (!dir.exists()) {
				basedir = new File(basedir, "_dev");
				dir = new File(basedir, zimletName);
			}
            File file = new File(dir, zimletName + ".xml");


			Document document = parseDocument(file, zimletName);
            if (document == null) {
                continue;
            }

            // add properties files
            boolean isJavaScript = type.equals(T_JAVASCRIPT);
            if (isJavaScript) {
                files.add(new ZimletFile(zimletName, "/res/"+zimletName, true));
            }

            // add included files
            Element root = document.getDocumentElement();
            NodeList nodes = root.getElementsByTagName(isJavaScript ? "include" : "includeCSS");
            int nodeCount = nodes.getLength();
            for (int i = 0; i < nodeCount; i++) {
                Node node = nodes.item(i);
                String filename = getText(node).trim();

                // only add local files to list
                if (RE_REMOTE.matcher(filename).matches()) {
                    continue;
                }
                files.add(new ZimletFile(zimletName, dir.getAbsolutePath() + File.separator + filename));
            }
        }
        return files;
    }

    private Document parseDocument(File file, String zimletName) {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(file);
        }
        catch (Exception e) {
            ZimbraLog.zimlet.info("error loading " + zimletName + " manifest: " + e.getMessage());
        }
        return document;
    }

    private static String getText(Node node) {
        StringBuilder str = new StringBuilder();
        Node child = node.getFirstChild();
        while (child != null) {
            str.append(child.getNodeValue());
            child = child.getNextSibling();
        }
        return str.toString();
    }

    //
    // Classes
    //

    static class ZimletFile extends File {
        public String zimletName;
        public boolean isResourceFile;

        public ZimletFile(String zimletName, String filename) {
            super(filename);
            this.zimletName = zimletName;
            this.isResourceFile = false;
        }

        public ZimletFile(String zimletName, String filename, boolean isResourceFile) {
            super(filename);
            this.zimletName = zimletName;
            this.isResourceFile = isResourceFile;
        }
    }

    static class RequestWrapper extends HttpServletRequestWrapper {
        private String filename;

        public RequestWrapper(HttpServletRequest req, String filename) {
            super(req);
            this.filename = filename;
        }

        public String getRequestURI() {
            return filename + ".js";
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
