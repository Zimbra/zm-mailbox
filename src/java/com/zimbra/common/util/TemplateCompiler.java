/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.io.*;

import org.apache.commons.cli.*;

/**
 */
public class TemplateCompiler {

	//
	// Constants
	//

	private static final String S_PARAM = "\\$\\{(.+?)\\}";
	private static final String S_INLINE = "<\\$=(.+?)\\$>";
	private static final String S_CODE = "<\\$(.+?)\\$>";
	private static final String S_ALL = S_PARAM + "|" + S_INLINE + "|" + S_CODE;
	private static final String S_TEMPLATE = "<template(.*?)>(.*?)</template>";
	private static final String S_ATTR = "\\s*(\\S+)\\s*=\\s*('[^']*'|\"[^\"]*\")";
	private static final String S_WS_LINESEP = "\\s*\\n+\\s*";
	private static final String S_GT_LINESEP_LT = ">" + S_WS_LINESEP + "<";

	private static final Pattern RE_REPLACE = Pattern.compile(S_ALL, Pattern.DOTALL);
	private static final Pattern RE_TEMPLATE = Pattern.compile(S_TEMPLATE, Pattern.DOTALL);
	private static final Pattern RE_ATTR = Pattern.compile(S_ATTR, Pattern.DOTALL);

	private static final String A_XML_SPACE = "xml:space";
	private static final String V_XML_SPACE_PRESERVE = "preserve";


    private static final String S_PARAM_PART = "([^\\(\\.]+)(\\(.*?\\))?\\.?";
    private static final Pattern RE_PARAM_PART = Pattern.compile(S_PARAM_PART);

    //
	// Data
	//

	private static Options _mOptions = new Options();
	private String _prefix = "";
	private String _idir = ".";
	private String _odir = ".";
	private boolean _authoritative = false;
	private boolean _define = false;
	private String[] _filenames = null;
	private String _format = "js";

	
	//
	//  Main
	//

	static {
        Option option = new Option("p", "prefix", true, "");
        option.setRequired(false);
        _mOptions.addOption(option);

		option = new Option("d", "define", false, "prevent ");
		option.setRequired(false);
		_mOptions.addOption(option);

		option = new Option("a", "authoritative", false, "declare template as authoritative");
		option.setRequired(false);
		_mOptions.addOption(option);

        //TODO: do we need to collect files?  For now, just pass files in, collect with sh.
		option = new Option("i", "inputdir", true, "source directory base");
        option.setRequired(false);
        _mOptions.addOption(option);

        option = new Option("o", "outputdir", true, "name of directory for resultant files");
        option.setRequired(false);
        _mOptions.addOption(option);

		option = new Option("f", "format", true, "output format, \"js\" or \"properties\" (default: \"js\"");
		option.setRequired(false);
		_mOptions.addOption(option);
		
	}


	public static void main(String[] args)
	throws Exception {
        try {
            TemplateCompiler compiler = new TemplateCompiler();
            compiler.compile(args);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
	}


	//
	//  Public API
	//

	public static void compile(File idir, File odir, String prefix,
	                                String[] filenames,
	                                boolean authoritative, boolean define)
	throws IOException {
		compile(idir, odir, prefix, filenames, "js", authoritative, define);
	}

	public static void compile(File idir, File odir, String prefix,
	                                String[] filenames, String format,
	                                boolean authoritative, boolean define)
	throws IOException {
		for (String filename : filenames) {
			String path = stripExt(filename);
			String pkg = prefix + path2package(path);
			File ifile = new File(idir, filename);
			File ofile = new File(odir, filename+"."+format);
			if (upToDate(ifile, ofile)) {
                System.out.println(ifile + " is up to date");
                continue;
			}
			System.out.println("Compiling "+ifile);
			if (odir != idir) {
				File pdir = ofile.getParentFile();
				pdir.mkdirs();
			}
			try {
				compile(ifile, ofile, format, pkg, authoritative, define);
			}
			catch (IOException e) {
				System.err.println("error: "+e.getMessage());
			}
		}
	}

	public static void compile(File ifile, File ofile, String pkg,
	                           boolean authoritative, boolean define)
	throws IOException {
		compile(ifile, ofile, "js", pkg, authoritative, define);
	}

	public static void compile(File ifile, File ofile, String format, String pkg,
	                           boolean authoritative, boolean define)
	throws IOException {
	    BufferedReader in = null;
	    PrintWriter out = null;
	    try {
		    boolean isProperties = format.equals("properties");
	        in = new BufferedReader(new FileReader(ifile));
	        out = new PrintWriter(new FileWriter(ofile));

	        String lines = readLines(in);
	        Matcher matcher = RE_TEMPLATE.matcher(lines);
	        if (matcher.find()) {
	            boolean first = true;
	            do {
	                Map<String,String> attrs = parseAttrs(matcher.group(1));
	                String body = matcher.group(2);
	                String stripWsAttr = attrs.get(A_XML_SPACE);
	                String packageId = pkg;
	                String templateId = attrs.get("id");
	                // NOTE: Template ids can be specified absolutely (i.e.
	                //       overriding the default package) if the id starts
	                //       with a forward slash (/), or if the id contains
	                //       a hash mark (#). This allows a template file to
	                //       override both types of template files (i.e. a
	                //       single template per file or multiple templates
	                //       per file).
	                if (templateId != null && (templateId.indexOf('#') != -1 || templateId.startsWith("/"))) {
	                    if (templateId.indexOf('#') == -1) templateId += "#";
	                    packageId = templateId.replaceAll("#.*$", "").replaceAll("^/","").replace('/','.');
	                    templateId = templateId.replaceAll("^.*#", "");
	                }
	                String id = templateId != null && !templateId.equals("") ? packageId+"#"+templateId : packageId;
		            if (isProperties) {
			            // TODO: convert to properties
			            printEscaped(out, id);
			            if (body.indexOf('\n') == -1) {
				            out.print(" = ");
				            printEscaped(out, body);
			            }
			            else {
				            out.print(" =");
				            String[] bodylines = body.split("\n");
				            for (String bodyline : bodylines) {
				                out.print("\\\n\t");
					            printEscaped(out, bodyline);
				            }
			            }
			            out.println();
			            continue;
		            }
		            if (stripWsAttr == null || !stripWsAttr.equals(V_XML_SPACE_PRESERVE)) {
		                body = body.replaceAll(S_GT_LINESEP_LT, "><").trim();
		            }
	                convertLines(out, id, body, attrs, authoritative);
		            if (first && define) {
		                out.print("AjxPackage.define(\"");
		                out.print(packageId);
		                out.println("\");");
		            }
	                if (first) {
	                    first = false;
	                    out.print("AjxTemplate.register(\"");
	                    out.print(packageId);
	                    out.print("\", ");
	                    out.print("AjxTemplate.getTemplate(\"");
	                    out.print(id);
	                    out.print("\"), ");
	                    out.print("AjxTemplate.getParams(\"");
	                    out.print(id);

	                    out.println("\"));");
	                }
		            out.println();
	            } while (matcher.find());
	        }
	        else {
	            convertLines(out, pkg, lines, null, authoritative);
	        }
	    }
	    finally {
	        if (in != null) {
	            try {
	                in.close();
	            }
	            catch (Exception e) {
	                // ignore
	            }
	        }
	        if (out != null) {
	            out.close();
	        }
	    }
	}


	//
	//  Private helpers
	//

	private void compile(String[] args) throws IOException {
		parseArgs(args);

		File idir = new File(_idir);
		File odir = new File(_odir);
        compile(idir, odir, _prefix, _filenames, _format, _authoritative, _define);
	}

	private void parseArgs(String[] args) {
		CommandLineParser parser = new GnuParser();
		CommandLine cl = null;
		try {
		    cl = parser.parse(_mOptions, args);
		} catch (Exception e) {
		    System.out.println(e);
		    System.exit(10);
		}

		if (cl == null) {
			System.out.println("Nothing to do!");
			System.exit(1);
		}

		if (cl.hasOption("p")) {
			_prefix = cl.getOptionValue("p");
		}
		if (cl.hasOption("d")) {
			_define = true;
		}
		if (cl.hasOption("a")) {
			_authoritative = true;
		}
		if (cl.hasOption("i")) {
			_odir = cl.getOptionValue("i");
		}
		if (cl.hasOption("o")) {
			_odir = cl.getOptionValue("o");
		}
		if (cl.hasOption("f")) {
			_format = cl.getOptionValue("f");
		}

		_filenames = cl.getArgs();
		if (_filenames.length == 0) {
			System.out.println("No files to convert!");
			System.exit(1);
		}
	}

	private static void convertLines(PrintWriter out, String pkg, String lines,
	                                 Map<String,String> attrs, boolean authoritative) {
	    out.print("AjxTemplate.register(\"");
	    out.print(pkg);
	    out.println("\", ");
	    out.println("function(name, params, data, buffer) {");
	    out.println("\tvar _hasBuffer = Boolean(buffer);");
	    out.println("\tdata = (typeof data == \"string\" ? { id: data } : data) || {};");
	    out.println("\tbuffer = buffer || [];");
	    out.println("\tvar _i = buffer.length;");
	    out.println();

	    Matcher matcher = RE_REPLACE.matcher(lines);
	    if (matcher.find()) {
	        int offset = 0;
	        do {
	            int index = matcher.start();
	            if (offset < index) {
	                printStringLines(out, lines.substring(offset, index));
	            }
	            String param = matcher.group(1);
	            String inline = matcher.group(2);
	            if (param != null) {
	                printDataLine(out, param);
	            }
	            else if (inline != null) {
	                printBufferLine(out, inline);
	            }
	            else {
	                printLine(out, "\t", matcher.group(3).replaceAll("\n", "\n\t"), "\n");
	            }
	            offset = matcher.end();
	        } while (matcher.find());
	        if (offset < lines.length()) {
	            printStringLines(out, lines.substring(offset));
	        }
	    }
	    else {
	        printStringLines(out, lines);
	    }
	    out.println();

	    out.println("\treturn _hasBuffer ? buffer.length : buffer.join(\"\");");
	    out.println("},");
	    if (attrs != null && attrs.size() > 0) {
	        out.println("{");
	        Iterator<String> iter = attrs.keySet().iterator();
	        while (iter.hasNext()) {
	            String aname = iter.next();
	            String avalue = attrs.get(aname);
	            out.print("\t\"");
	            printEscaped(out, aname);
	            out.print("\": \"");
	            printEscaped(out, avalue);
	            out.print("\"");
	            if (iter.hasNext()) {
	                out.print(",");
	            }
	            out.println();
	        }
	        out.print("}");
	    }
	    else {
	        out.print("null");
	    }
	    out.print(", ");
	    out.print(authoritative);
	    out.println(");");
	}

	//
	// Private functions
	//

	private static Map<String,String> parseAttrs(String s) {
	    Map<String,String> attrs = new HashMap<String,String>();
	    Matcher matcher = RE_ATTR.matcher(s);
	    while (matcher.find()) {
	        String aname = matcher.group(1);
	        String avalue = matcher.group(2).replaceAll("^['\"]|['\"]$", "");
	        attrs.put(aname, avalue);
	    }
	    return attrs;
	}

	private static String readLines(BufferedReader in) throws IOException {
	    StringBuilder str = new StringBuilder();
	    String line;
	    while ((line = in.readLine()) != null) {
	        str.append(line);
	        str.append('\n');
	    }
	    return str.toString();
	}

	private static void printLine(PrintWriter out, String... ss) {
	    for (String s : ss) {
	        out.print(s);
	    }
	}

	private static void printStringLines(PrintWriter out, String... ss) {
	    for (String s : ss) {
	        String[] lines = s.split("\n");
	        for (int i = 0; i < lines.length; i++) {
	            String line = lines[i];
	            printStringLine(out, line, i < lines.length - 1 ? "\n" : "");
	        }
	    }
	}
	private static void printStringLine(PrintWriter out, String... ss) {
	    out.print("\tbuffer[_i++] = \"");
	    for (String s : ss) {
	        printEscaped(out, s);
	    }
	    out.println("\";");
	}

	private static void printDataLine(PrintWriter out, String s) {
	    out.print("\tbuffer[_i++] = data");
        Matcher part = RE_PARAM_PART.matcher(s);
        while (part.find()) {
            String name = part.group(1);
            String args = part.group(2);

            out.print("[\"");
            out.print(name);
            out.print("\"]");
            if (args != null) {
                out.print(args);
            }
        }
        out.println(";");
    }

    private static void printBufferLine(PrintWriter out, String... ss) {
        out.print("\tbuffer[_i++] = ");
        for (String s : ss) {
            out.print(s);
        }
        out.println(";");
    }

    private static String stripExt(String s) {
        return s.replaceAll("\\.[^\\.]+$", "");
    }

    private static String path2package(String s) {
        return s.replace(File.separatorChar, '.');
    }

    private static void printEscaped(PrintWriter out, String s) {
        int length = s.length();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c == '"') {
                out.print('\\');
            }
            else if (c == '\n') {
                out.print("\\n");
                continue;
            }
            else if (c == '\r') {
	            out.print("\\r");
	            continue;
            }
            else if (c == '\t') {
	            out.print("\\t");
	            continue;
            }
	        else if (c == '\\') {
	            out.print("\\\\");
	            continue;
            }
            out.print(c);
        }
    }

    private static boolean upToDate(File ifile, File ofile) {
        if (ifile.exists() && ofile.exists()) {
            return ifile.lastModified() < ofile.lastModified();
        }
        return false;
    }

}

