/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zimlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.servlet.DiskCacheServlet;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.WarningLevel;

@SuppressWarnings("serial")
public class ZimletResources extends DiskCacheServlet {

    //
    // Constants
    //
    private static final String COMPRESSED_EXT = ".zgz";
    private static final String RESOURCE_PATH = "/res/";

    private static final String P_DEBUG = "debug";

    private static final String T_CSS = "css";
    private static final String T_JAVASCRIPT = "javascript";
    private static final String T_PLAIN = "plain";

    private static final Map<String, String> TYPES = new HashMap<String, String>();

    private static final Pattern RE_REMOTE = Pattern.compile("^((https?|ftps?)://|/)");
    private static final Pattern RE_CSS_URL = Pattern.compile("(url\\(['\"]?)([^'\"\\)]*)", Pattern.CASE_INSENSITIVE);

    private boolean supportsGzip = true;

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

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String val = getServletConfig().getInitParameter("shouldSupportGzip");
        if (val != null) {
            this.supportsGzip = Boolean.valueOf(val);
        } else {
            this.supportsGzip = true;
        }
    }

    @Override
    public void service(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {
        if (flushCache(request)) {
            return;
        }

        ZimbraLog.clearContext();

        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;
        String zimbraXZimletCompatibleWith = req.getParameter("zimbraXZimletCompatibleWith");
        String uri = req.getRequestURI();
        String pathInfo = req.getPathInfo();

        if (pathInfo == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        @SuppressWarnings("unchecked")
        Set<String> zimletNames = (Set<String>) req.getAttribute(ZimletFilter.ALLOWED_ZIMLETS);
        Set<String> allZimletNames = (Set<String>) req.getAttribute(ZimletFilter.ALL_ZIMLETS);

        if (!pathInfo.startsWith(RESOURCE_PATH)) {
            // handle requests for individual files included in zimlet in case dev=1 is set.
            ServletContext targetContext = getServletConfig().getServletContext().getContext("/zimlet");
            if (targetContext == null) {
                throw new ServletException("Could not forward the request to zimlet webapp, possible misconfiguration.");
            }
            RequestDispatcher dispatcher = targetContext.getRequestDispatcher(pathInfo);
            dispatcher.forward(req, resp);
            return;
        }

        String contentType = getContentType(uri);
        String type = contentType.replaceAll("^.*/", "");
        boolean debug = req.getParameter(P_DEBUG) != null;
        boolean compress = !debug && supportsGzip && uri.endsWith(COMPRESSED_EXT);

        String cacheId = getCacheId(req, type, zimletNames, allZimletNames);

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
                ServletConfig config = this.getServletConfig();
                ServletContext baseContext = config.getServletContext();
                RequestDispatcher dispatcher = baseContext.getRequestDispatcher(RESOURCE_PATH);

                // NOTE: We have to return the messages for *all* of the zimlets,
                // NOTE: not just the enabled ones, because their names and
                // NOTE: descriptions can be translated.
                for (String zimletName : allZimletNames) {
                    RequestWrapper wrappedReq = new RequestWrapper(req, RESOURCE_PATH + zimletName);
                    ResponseWrapper wrappedResp = new ResponseWrapper(resp, printer);
                    wrappedReq.setParameter("debug", "1"); // bug 45922: avoid cached messages
                    dispatcher.include(wrappedReq, wrappedResp); //this will activate ZimletProps2JsServlet
                }
            }

            // zimlet resources
            if (ZimbraLog.zimlet.isDebugEnabled()) {
                ZimbraLog.zimlet.debug("DEBUG: generating buffer");
            }
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
                String mintext = null;
                if (zimbraXZimletCompatibleWith != null) {
                    mintext = compile(text);
                } else {
                    JavaScriptCompressor compressor = new JavaScriptCompressor(
                        new StringReader(text), new ErrorReporter() {

                            @Override
                            public void warning(String message, String sourceName, int line,
                                String lineSource, int lineOffset) {
                                if (line < 0) {
                                    ZimbraLog.zimlet.warn("\n" + message);
                                } else {
                                    ZimbraLog.zimlet
                                        .warn("\n" + line + ':' + lineOffset + ':' + message);
                                }
                            }

                            @Override
                            public void error(String message, String sourceName, int line,
                                String lineSource, int lineOffset) {
                                if (line < 0) {
                                    ZimbraLog.zimlet.error("\n" + message);
                                } else {
                                    ZimbraLog.zimlet
                                        .error("\n" + line + ':' + lineOffset + ':' + message);
                                }
                            }

                            @Override
                            public EvaluatorException runtimeError(String message,
                                String sourceName, int line, String lineSource, int lineOffset) {
                                error(message, sourceName, line, lineSource, lineOffset);
                                return new EvaluatorException(message);
                            }
                        });
                    StringWriter out = new StringWriter();
                    compressor.compress(out, 0, true, false, false, false);
                    mintext = out.toString();
                }
                if (mintext == null) {
                    ZimbraLog.zimlet.info("unable to minimize zimlet JS source");
                } else {
                    text = mintext;
                }
            }

            // store buffer
            if (!debug) {
                // NOTE: This assumes that the cacheId will *end* with the compressed
                // NOTE: extension. Therefore, make sure to keep getCacheId in sync.
                String uncompressedCacheId = compress ?
                    cacheId.substring(0, cacheId.length() - COMPRESSED_EXT.length()) : cacheId;

                // store uncompressed file in cache
                file = createCacheFile(uncompressedCacheId, type);
                if (ZimbraLog.zimlet.isDebugEnabled()) {
                    ZimbraLog.zimlet.debug("DEBUG: buffer file: " + file);
                }
                copy(text, file);
                putCacheFile(uncompressedCacheId, file);

                // store compressed file in cache
                if (compress) {
                    String compressedCacheId = cacheId;
                    File gzfile = createCacheFile(compressedCacheId, type+DiskCacheServlet.EXT_COMPRESSED);
                    if (ZimbraLog.zimlet.isDebugEnabled()) {
                        ZimbraLog.zimlet.debug("DEBUG: buffer file: " + gzfile);
                    }
                    file = compress(file, gzfile);
                    putCacheFile(compressedCacheId, file);
                }
            }
        } else {
            if (ZimbraLog.zimlet.isDebugEnabled()) {
                ZimbraLog.zimlet.debug("DEBUG: using previous buffer");
            }
        }

        // write buffer
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

            if (compress && file != null) {
                resp.setHeader("Content-Encoding", "gzip");
            }

            // NOTE: We can only be certain we know the final size if we are
            // NOTE: *not* compressing the output OR if we're just writing
            // NOTE: the contents of the generated file to the stream.
            if (!compress || file != null) {
                resp.setContentLength(file != null ? (int)file.length() : text.getBytes("UTF-8").length);
            }
        } catch (IllegalStateException e) {
            // ignore -- thrown if called from including JSP
            ZimbraLog.zimlet.debug("zimletres: " + cacheId);
            ZimbraLog.zimlet.debug("zimletres: " + e.getMessage());
        }
        if (file != null) {
            // NOTE: If we saved the buffer to a file and compression is
            // NOTE: enabled then the file has *already* been compressed
            // NOTE: and the Content-Encoding header has been added.
            copy(file, resp, false);
        } else {
            copy(text, resp, compress);
        }

    } // doGet(HttpServletRequest,HttpServletResponse)

    /**
     * @param code JavaScript source code to compile.
     * @return The compiled version of the code.
     */
    public static String compile(String code) {
        if (StringUtil.isNullOrEmpty(code)) {
            return null;
        }
        Compiler compiler = new Compiler();
        CompilerOptions options = new CompilerOptions();
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
        SourceFile extern = SourceFile.fromCode("externs.js", "");
        SourceFile input = SourceFile.fromCode("input.js", code);
        WarningLevel.QUIET.setOptionsForWarningLevel(options);
        compiler.compile(extern, input, options);
        return compiler.toSource();
    }

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
        if (dot > 0) {
            contentType = TYPES.get(file.substring(dot + 1));
        }
        if (contentType == null) {
            contentType = sFileTypeMap.getContentType(file);
        }
        ZimbraLog.zimlet.debug("%s: %s", file, contentType);
        if (contentType != null) {
            resp.setContentType(contentType);
        }
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
                    if (entry == null) {
                        continue;
                    }

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
        Set<String> zimletNames, Set<String> allZimletNames) {
        StringBuilder str = new StringBuilder();

        str.append(getLocale(req).toString());
        str.append(":");
        append(str, zimletNames);
        str.append(":");
        append(str, allZimletNames);
        str.append(":");
        str.append(type);
        // NOTE: Keep compressed extension at end of cacheId.
        if (req.getRequestURI().endsWith(COMPRESSED_EXT)) {
            str.append(COMPRESSED_EXT);
        }
        return str.toString();
    }

    private static void append(StringBuilder dest, Set<String> set) {
        Iterator<String> iter = set.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            if (i > 0) {
                dest.append(",");
            }
            dest.append(iter.next());
        }
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

        @Override
        public String getRequestURI() {
            return filename + ".js";
        }

        public void setParameter(String name, String value) {
            if (this.parameters == null) {
                this.parameters = new HashMap<String,String>();
            }
            this.parameters.put(name, value);
        }
        @Override
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

        @Override
        public void setHeader(String name, String value) { /* NOP */ }

        @Override
        public void addHeader(String name, String value) { /* NOP */ }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new ServletStream(getWriter());
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return out;
        }
    }

    static class ServletStream extends ServletOutputStream {
        private PrintWriter out;

        public ServletStream(PrintWriter out) {
            this.out = out;
        }

        @Override
        public void write(int i) throws IOException {
            out.write(i);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.print(new String(b, off, len, "UTF-8"));
        }

        @Override
        public void print(boolean b) throws IOException {
            out.print(b);
        }

        @Override
        public void print(char c) throws IOException {
            out.print(c);
        }

        @Override
        public void print(float f) throws IOException {
            out.print(f);
        }

        @Override
        public void print(int i) throws IOException {
            out.print(i);
        }

        @Override
        public void print(long l) throws IOException {
            out.print(l);
        }

        @Override
        public void print(String s) throws IOException {
            out.print(s);
        }

        @Override
        public void println() throws IOException {
            out.println();
        }

        @Override
        public void println(boolean b) throws IOException {
            out.println(b);
        }

        @Override
        public void println(char c) throws IOException {
            out.println(c);
        }

        @Override
        public void println(float f) throws IOException {
            out.println(f);
        }

        @Override
        public void println(int i) throws IOException {
            out.println(i);
        }

        @Override
        public void println(long l) throws IOException {
            out.println(l);
        }

        @Override
        public void println(String s) throws IOException {
            out.println(s);
        }

        @Override
        public boolean isReady() {
            //TODO: this isn't right, just stubbed for now so we can build
            return true;
        }

        @Override
        public void setWriteListener(WriteListener listener) {
            //TODO: this isn't right, just stubbed for now so we can build
        }
    }

} // class ZimletResources
