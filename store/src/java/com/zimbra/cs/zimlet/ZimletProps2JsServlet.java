/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.BufferStream;
import com.zimbra.common.util.Props2Js;
import com.zimbra.common.util.ZimbraLog;

/**
 * The equivalent of Props2JsServlet in ZimbraWebClient, handles .properties within deployed zimlets only.
 */
public class ZimletProps2JsServlet extends HttpServlet {

    private static final long serialVersionUID = -312514257671213969L;

    protected static final String COMPRESSED_EXT = ".zgz";

    protected static final String P_DEBUG = "debug";
    protected static final String P_BASENAME_PATTERNS = "basename-patterns";

    protected static final String A_FLUSH_CACHE = "flushCache"; /* i.e. FlushCache.FLUSH_CACHE */

    protected static final String A_REQUEST_URI = "request-uri";
    protected static final String A_BASENAME_PATTERNS = P_BASENAME_PATTERNS;
    protected static final String A_BASENAME_PATTERNS_LIST = A_BASENAME_PATTERNS + "-list";

    private static Map<Locale, Map<String, byte[]>> buffers =
            new HashMap<Locale, Map<String, byte[]>>();

    private String getDirPath(String dirname) {
        if (new File(dirname).isAbsolute()) {
            return dirname;
        }
        String basedir = this.getServletContext().getRealPath("/");
        if (!basedir.endsWith("/")) {
            basedir += "/";
        }
        return basedir + dirname;
    }

    @Override
    public void service(ServletRequest req, ServletResponse resp)
            throws IOException, ServletException {
        if (flushCache(req)) {
            return;
        }
        super.service(req, resp);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws
            IOException, ServletException {
        // get request info
        boolean debug = req.getParameter(P_DEBUG) != null;
        Locale locale = getLocale(req);
        Map<String, byte[]> localeBuffers;
        String uri = getRequestURI(req);
        ZimbraLog.zimlet.debug("ZimletProps2JsServlet uri: %s", uri);
        ZimbraLog.zimlet.debug("ZimletProps2JsServlet base context path: %s",
                this.getServletConfig().getServletContext().getContextPath());

        synchronized (buffers) {
            localeBuffers = buffers.get(locale);
            if (localeBuffers == null) {
                localeBuffers = Collections.synchronizedMap(new HashMap<String,
                        byte[]>());
                buffers.put(locale, localeBuffers);
            }
        }

        // get byte buffer
        byte[] buffer = debug ? null : localeBuffers.get(uri);

        if (buffer == null) {
            buffer = getBuffer(req, locale, uri);
            // do not need to compress JS because Prop2Js has been optimized
            if (uri.endsWith(COMPRESSED_EXT)) {
                // gzip response
                ByteArrayOutputStream bos = new ByteArrayOutputStream(buffer.length / 2);
                OutputStream gzos = new GZIPOutputStream(bos);

                gzos.write(buffer);
                gzos.close();
                buffer = bos.toByteArray();
            }
            if (!LC.zimbra_minimize_resources.booleanValue()) {
                localeBuffers.put(uri, buffer);
            }
        }

        // generate output
        OutputStream out = resp.getOutputStream();
        try {
            if (uri.endsWith(COMPRESSED_EXT)) {
                resp.setHeader("Content-Encoding", "gzip");
            }
            resp.setContentType("application/x-javascript");
        } catch (Exception e) {
            ZimbraLog.zimlet.error(e.getMessage());
        }
        out.write(buffer);
        out.flush();
    }

    protected boolean flushCache(ServletRequest req) {
        Boolean flushCache = (Boolean) req.getAttribute(A_FLUSH_CACHE);
        if (flushCache != null && flushCache.booleanValue()) {
            int oldSize = buffers.size();
            buffers.clear();
            int newSize = buffers.size();
            ZimbraLog.zimlet.debug("flushed uistrings cache: %d entries > %d entries", oldSize, newSize);
            return true;
        }
        return false;
    }

    protected String getRequestURI(HttpServletRequest req) {
        String uri = (String) req.getAttribute(A_REQUEST_URI);
        if (uri == null) {
            uri = req.getRequestURI();
        }
        return uri;
    }

    protected List<String> getBasenamePatternsList(HttpServletRequest req) {
        List<String> list = new LinkedList<String>();
        String patterns = (String) req.getAttribute(A_BASENAME_PATTERNS);

        if (patterns == null) {
            patterns = this.getInitParameter(P_BASENAME_PATTERNS);
        }
        if (patterns == null) {
            patterns = "WEB-INF/classes/${dir}/${name}";
        }
        list.add(patterns);
        return list;
    }

    protected Locale getLocale(HttpServletRequest req) {
        String language = req.getParameter("language");
        String locid = req.getParameter("locid");
        if (language != null) {
            String country = req.getParameter("country");
            if (country != null) {
                return new Locale(language, country);
            }
            return new Locale(language);
        }
        else if (locid != null) {
            String[] parts = locid.split("_");
            if (parts.length > 1) {
                return new Locale(parts[0], parts[1]);
            } else {
                return new Locale(parts[0]);
            }
        }
        return req.getLocale();
    }

    protected byte[] getBuffer(HttpServletRequest req,
            Locale locale, String uri) throws IOException {
        BufferStream bos = new BufferStream(24 * 1024);
        DataOutputStream out = new DataOutputStream(bos);
        out.writeBytes("// Locale: " + Props2Js.getCommentSafeString(locale.toString()) + '\n');

        // tokenize the list of patterns
        List<String> patternsList = this.getBasenamePatternsList(req);
        List<List<String>> basenamePatterns = new LinkedList<List<String>>();
        for (String patterns : patternsList) {
            StringTokenizer tokenizer = new StringTokenizer(patterns, ",");
            List<String> basenamesList = new LinkedList<String>();
            basenamePatterns.add(basenamesList);
            while (tokenizer.hasMoreTokens()) {
                String pattern = tokenizer.nextToken().trim();
                basenamesList.add(pattern);
            }
        }

        // This gets the base directory for the resource bundle
        // basename. For example, if the URI is:
        //
        //   .../messages/I18nMsg.js
        //
        // then the basedir is "/messages/" and if the URI is:
        //
        //   .../keys/ZmKeys.js
        //
        // then the basedir is "/keys/".
        //
        // NOTE: The <url-pattern>s in the web.xml file restricts
        //       which URLs map to this servlet so there's no risk
        //       that the basedir will be other than what we expect.
        int lastSlash = uri.lastIndexOf('/');
        int prevSlash = uri.substring(0, lastSlash).lastIndexOf('/');
        String basedir = uri.substring(prevSlash, lastSlash + 1);
        String dirname = this.getDirPath("");
        String filenames = uri.substring(uri.lastIndexOf('/') + 1);
        String classnames = filenames.substring(0, filenames.indexOf('.'));
        StringTokenizer tokenizer = new StringTokenizer(classnames, ",");

        if (ZimbraLog.zimlet.isDebugEnabled()) {
            for (List<String> basenames : basenamePatterns) {
                ZimbraLog.zimlet.debug("!!! basenames: %s", basenames);
            }
            ZimbraLog.zimlet.debug("!!! basedir: %s", basedir);
        }
        while (tokenizer.hasMoreTokens()) {
            String classname = tokenizer.nextToken();
            if (ZimbraLog.zimlet.isDebugEnabled()) {
                ZimbraLog.zimlet.debug("!!! classname: %s", classname);
            }
            load(req, out, locale, basenamePatterns, basedir, dirname, classname);
        }
        return bos.toByteArray();
    }

    protected void load(HttpServletRequest req, DataOutputStream out,
            Locale locale, List<List<String>> basenamePatterns,
            String basedir, String dirname, String classname) throws IOException {
        String basename = basedir + classname;

        out.writeBytes("// Basename: " + Props2Js.getCommentSafeString(basename) + '\n');
        for (List<String> basenames : basenamePatterns) {
            try {
                ClassLoader parentLoader = this.getClass().getClassLoader();
                PropsLoader loader = new PropsLoader(parentLoader, basenames,
                        basedir, dirname, classname);

                // load path list, but not actual properties to prevent caching
                ResourceBundle.getBundle(basename, locale, loader, new ResourceBundle.Control()
                {
                    @Override
                    public List<Locale> getCandidateLocales(String baseName, Locale locale)
                    {
                        if (baseName == null) {
                            throw new NullPointerException();
                        }
                        if (locale.equals(new Locale("zh", "HK")) || locale.equals(new Locale("zh", "CN")) || locale.equals(new Locale("zh", "TW")))
                        {
                            return Arrays.asList(
                                    locale,
                                    Locale.ROOT);
                        }
                        return super.getCandidateLocales(baseName, locale);
                    }
                });
                for (File file : loader.getFiles()) {
                    Props2Js.convert(out, file, classname);
                }
            } catch (MissingResourceException e) {
                out.writeBytes("// properties for " + classname + " not found\n");
            } catch (IOException e) {
                out.writeBytes("// properties error for " + classname +
                        " - see server log\n");
                ZimbraLog.zimlet.error(e.getMessage());
            }
        }
    }

    public static class PropsLoader extends ClassLoader {
        private List<File> files;
        private List<String> patterns;
        private String dir;
        private String dirname;
        private String name;

        private static Pattern RE_LOCALE = Pattern.compile(".*(_[a-z]{2}(_[A-Z]{2})?)\\.properties");
        private static Pattern RE_SYSPROP = Pattern.compile("\\$\\{(.*?)\\}");

        public PropsLoader(ClassLoader parent, List<String> patterns,
                String basedir, String dirname, String classname) {
            super(parent);
            this.patterns = patterns;
            this.dir = basedir.replaceAll("/[^/]+$", "").replaceAll("^.*/", "");
            this.dirname = dirname;
            this.name = classname;
            this.files = new LinkedList<File>();
        }

        public List<File> getFiles() {
            return files;
        }

        @Override
        public InputStream getResourceAsStream(String rname) {
            String filename = rname.replaceAll("^.*/", "");
            Matcher matcher = RE_LOCALE.matcher(filename);
            String locale = matcher.matches() ? matcher.group(1) : "";
            String ext = rname.replaceAll("^[^\\.]*", "");
            for (String basename : this.patterns) {
                basename = basename.replaceAll("\\$\\{dir\\}", this.dir);
                basename = basename.replaceAll("\\$\\{name\\}", this.name);
                basename = replaceSystemProps(basename);
                basename += locale + ext;
                File file = new File(this.dirname + basename);
                if (!file.exists()) {
                    file = new File(basename);
                }
                if (file.exists()) {
                    files.add(file);
                    return new ByteArrayInputStream(new byte[0]);
                }
            }
            return super.getResourceAsStream(rname);
        }

        @Override
        public URL getResource(String rname) {
            String filename = rname.replaceAll("^.*/", "");
            Matcher matcher = RE_LOCALE.matcher(filename);
            String locale = matcher.matches() ? matcher.group(1) : "";
            String ext = rname.replaceAll("^[^\\.]*", "");
            for (String basename : this.patterns) {
                basename = basename.replaceAll("\\$\\{dir\\}", this.dir);
                basename = basename.replaceAll("\\$\\{name\\}", this.name);
                basename = replaceSystemProps(basename);
                basename += locale + ext;
                File file = new File(this.dirname + basename);
                if (!file.exists()) {
                    file = new File(basename);
                }
                if (file.exists()) {
                    files.add(file);
                    try {
                        return file.toURI().toURL();
                    } catch (MalformedURLException e) {
                        ZimbraLog.zimlet.debug("MalformedURLException:" + e);
                    }
                }
            }
            return super.getResource(rname);
        }

        private static String replaceSystemProps(String s) {
            Matcher matcher = RE_SYSPROP.matcher(s);
            if (!matcher.find()) {
                return s;
            }
            StringBuilder str = new StringBuilder();
            int index = 0;
            do {
                str.append(s.substring(index, matcher.start()));
                String pname = matcher.group(1);
                String pvalue = System.getProperty(pname);
                str.append(pvalue != null ? pvalue : matcher.group(0));
                index = matcher.end();
            } while (matcher.find());
            str.append(s.substring(index));
            return str.toString();
        }
    }
}