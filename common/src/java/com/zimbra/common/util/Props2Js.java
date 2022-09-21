/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.util;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class converts <code>ResourceBundle</code> and <code>Properties</code>
 * objects to native JavaScript. The class iterates over the keys and
 * generates a line of JavaScript for each value. For example, if the
 * properties file "/path/Messages.properties" contains the following:
 * <pre>
 * one = One
 * two : Two\
 * Two
 * three = Three\
 * 		Three\
 * 		Three
 * </pre>
 * the generated JavaScript would look like this:
 * <pre>
 * function Messages() {}
 *
 * Messages.one = "One";
 * Messages.two = "TwoTwo";
 * Messages.three = "ThreeThreeThree";
 * </pre>
 *
 * @author Andy Clark
 */
public class Props2Js {
    private static Pattern VARNAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private Props2Js() {}

    public static void convert(OutputStream ostream, ResourceBundle bundle,
        String classname) throws IOException {
        DataOutputStream out = getOutputStream(ostream);
        Matcher matcher = VARNAME.matcher("");
        
        printHead(out, classname);
        for (Enumeration<String> keys = bundle.getKeys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            
            printEntry(out, matcher, key, bundle.getString(key));
        }
		printTail(out);
    }

    public static void convert(OutputStream ostream, Properties props,
        String classname) throws IOException {
        DataOutputStream out = getOutputStream(ostream);
        Matcher matcher = VARNAME.matcher("");
        
        printHead(out, classname);
        for (Enumeration<Object> keys = props.keys(); keys.hasMoreElements();) {
            String key = (String)keys.nextElement();
            
            printEntry(out, matcher, key, props.getProperty(key));
        }
		printTail(out);
    }

    public static void convert(OutputStream ostream, File file,
        String classname) throws IOException {
        // print values immediately rather than store them in a hash
        class PropertyPrinter extends Properties {
            DataOutputStream out;
            Matcher matcher = VARNAME.matcher("");
            
            PropertyPrinter(DataOutputStream out) { this.out = out; }

            public Object put(Object key, Object val) {
                try {
                    printEntry(out, matcher, (String)key, (String)val);
                } catch (IOException e) {
                }
                return null;
            }
        }
        
        DataOutputStream out = getOutputStream(ostream);
        FileInputStream in = new FileInputStream(file);
        PropertyPrinter pp = new PropertyPrinter(out);
        
        printHead(out, classname);
        pp.load(in);
        in.close();
		printTail(out);
    }

    private static void printHead(DataOutputStream out, String classname) throws
        IOException {
        out.writeBytes("if (!window['"+classname+"']) { ");
        out.writeBytes("window['"+classname+"'] = {};");
        out.writeBytes(" }\n");
        out.writeBytes("a=window['"+classname+"'];\n");
    }
    
	private static void printTail(DataOutputStream out) throws
		IOException {
		out.writeBytes("delete a;");
	}
	
    private static void printEntry(DataOutputStream out, Matcher matcher,
        String key, String val) throws IOException {
        matcher.reset(key);
        if (matcher.matches()) {
            out.writeBytes("a.");
            out.writeBytes(key);
            out.writeBytes("=\"");
            Props2Js.printEscaped(out, val);
            out.writeBytes("\";\n");
        } else {
            out.writeBytes("a[\"");
            Props2Js.printEscaped(out, key);
            out.writeBytes("\"]=\"");
            Props2Js.printEscaped(out, val);
            out.writeBytes("\";\n");
        }
    }

    private static void printEscaped(DataOutputStream out, String s)
    throws IOException {
        int length = s.length();
        
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            switch (c) {
            case '\t': out.writeBytes("\\t"); break;
            case '\n': out.writeBytes("\\n"); break;
            case '\r': out.writeBytes("\\r"); break;
            case '\\': out.writeBytes("\\\\"); break;
            case '"': out.writeBytes("\\\""); break;
            default: {
                if (c < 32 || c > 127) {
                    String cs = Integer.toString(c, 16);
                    out.writeBytes("\\u");
                    int cslen = cs.length();
                    for (int j = cslen; j < 4; j++) {
                        out.writeByte('0');
                    }
                    out.writeBytes(cs);
                } else {
                    out.writeByte(c);
                }
            }
            }
        }
    } // printEscaped(PrintStream,String)

    public static String getCommentSafeString(String st) {
        return st.replaceAll("<", "") //make sure you can't start a "script" tag within the comment cuz genius IE supposedly exectutes it
        .replaceAll("\n", ""); //make sure no newline can be injected to start a malicious script too
    }

    public static void main(String[] argv) throws Exception {
        // data
        String basename = null;
        Locale locale = null;
        File ifile = null;
        File ofile = null;
        String classname = null;

        // process arguments
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.startsWith("-")) {
                String option = arg.substring(1);
                if (option.equals("b")) {
                    basename = argv[++i];
                    continue;
                }
                if (option.equals("l")) {
                    locale = getLocale(argv[++i]);
                    continue;
                }
                if (option.equals("i")) {
                    ifile = new File(argv[++i]);
                    if (!ifile.exists() || !ifile.isFile()) {
                        System.err.println("error: invalid input file");
                        System.exit(1);
                    }
                    continue;
                }
                if (option.equals("o")) {
                    ofile = new File(argv[++i]);
                    if (ofile.exists() && !ofile.isFile()) {
                        System.err.println("error: invalid output file");
                        System.exit(1);
                    }
                    continue;
                }
                if (option.equals("c")) {
                    classname = argv[++i];
                    continue;
                }
            }
            // unknown argument
            System.err.println("error: unknown argument ("+arg+")");
            printHelp();
        }
        if (locale == null && ifile == null) {
            System.err.println("error: must specify -l or -i");
            System.exit(1);
        }
        if (locale != null && ifile != null) {
            System.err.println("error: specify only -l or -i");
            System.exit(1);
        }
        if (basename == null && locale != null) {
            System.err.println("error: must specify -b");
            System.exit(1);
        }

        OutputStream out = new BufferedOutputStream(ofile != null ? 
            new FileOutputStream(ofile) : System.out);

            // convert resource bundle
            if (locale != null) {
                ResourceBundle bundle = ResourceBundle.getBundle(basename, locale);

                Props2Js.convert(out, bundle, classname);
            }

            // convert properties file
            else {
                InputStream in = new FileInputStream(ifile);
                Properties props = new Properties();
                props.load(in);
                in.close();

                Props2Js.convert(out, props, classname);
            }

            if (ofile != null) {
                out.close();
            }
    }

    private static DataOutputStream getOutputStream(OutputStream ostream) {
        return ostream instanceof DataOutputStream ?
            (DataOutputStream)ostream : new DataOutputStream(ostream);
    }

    private static Locale getLocale(String arg) {
        StringTokenizer tokenizer = new StringTokenizer(arg, "_");
        String language = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
        String country = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
        String variant = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
        if (language != null) {
            if (country != null) {
                if (variant != null) {
                    return new Locale(language, country, variant);
                }
                return new Locale(language, country);
            }
            return new Locale(language);
        }
        return null;
    }

    private static void printHelp() {
        System.err.println("usage: java "+Props2Js.class.getName()+" options");
        System.err.println();
        System.err.println("options:");
        System.err.println("  -b basename  Resource bundle basename");
        System.err.println("  -l locale    Resource bundle locale");
        System.err.println("  -i filename  Properties input file");
        System.err.println("  -o filename  JavaScript output file");
        System.err.println("  -c classname JavaScript class to generate");
        System.err.println();
        System.err.println("notes:");
        System.err.println("  Options -b and -l must be used together.");
        System.err.println("  Either option -l or -i must be specified.");
        System.err.println("  If option -o not specified, writes to stdout.");
        System.exit(1);
    }
} // class Props2Js
