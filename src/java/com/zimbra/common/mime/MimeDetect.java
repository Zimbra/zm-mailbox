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
package com.zimbra.common.mime;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;

public class MimeDetect {
    private TreeMap<Glob, String> globs = new TreeMap<Glob, String>();
    private TreeMap<Magic, String> magics = new TreeMap<Magic, String>();
    private static MimeDetect mimeDetect = null;
    public static int DEFAULT_LIMIT = 8 * 1024;
    
    static {
        mimeDetect = new MimeDetect();
        try {
            mimeDetect.parse(LC.shared_mime_info_globs.value(),
                LC.shared_mime_info_magic.value());
        } catch (Exception e) {
            ZimbraLog.system.warn("shared-mime-info file error " + e);
        }
    }

    private class Glob implements Comparable<Glob> {
        private boolean literal;
        private Pattern pattern;
        private int priority;
        private String regex;

        Glob(String regex) throws IOException {
            this(regex, 50);
        }
        
        Glob(String regex, int priority) throws IOException {
            StringBuffer buf = new StringBuffer();
            char chars[] = regex.toCharArray();

            literal = true;
            this.priority = priority;
            this.regex = regex;
            for (int i = 0; i < chars.length; i++) {
                switch (chars[i]) {
                case '?':
                    buf.append('.');
                    literal = false;
                    break;
                case '*':
                    buf.append(".*");
                    literal = false;
                    break;
                case '[':
                    literal = false;
                    buf.append(chars[i++]);
                    if (i == chars.length)
                        throw new IOException("invalid glob pattern " + regex);
                    if (chars[i] == '!') {
                        buf.append("^");
                        i++;
                    }
                    while (i < chars.length && chars[i] != ']') {
                        if ("+()^$.{}[|\\&".indexOf(chars[i]) != -1)
                            buf.append('\\');
                        buf.append(chars[i++]);
                    }
                    if (i == chars.length)
                        throw new IOException("invalid glob pattern " + regex);
                    buf.append(chars[i]);
                    break;
                default:
                    if ("+()^$.{}[]|\\&".indexOf(chars[i]) != -1)
                        buf.append('\\');
                buf.append(chars[i]);
                }
            }
            buf.append('$');
            pattern = Pattern.compile(buf.toString());
        }
        
        public int compareTo(final Glob glob) {
            if (priority != glob.priority)
                return glob.priority - priority;
            else if (literal && !glob.literal)
                return -1;
            else if (!literal && glob.literal)
                return 1;
            else if (regex.length() == glob.regex.length())
                return regex.compareTo(glob.regex);
            return glob.regex.length() - regex.length();
        }
    }
    
    private class Magic implements Comparable<Magic> {
        public class Rule {
            public int indent;
            public byte mask[];
            public int offset;
            public int range;
            public byte value[];
            public int word;
            
            public Rule(int indent, int offset, byte value[], byte mask[],
                int word, int range) {
                this.indent = indent;
                this.offset = offset;
                this.value = value;
                if (mask == null) {
                    mask = new byte[value.length];
                    Arrays.fill(mask, (byte)0xff);
                }
                this.mask = mask;
                this.word = word;
                this.range = range;
            }

            public boolean detect(byte data[], int limit) {
                for (int pos = offset; pos < offset + range; pos++) {
                    if (limit < pos + value.length)
                        return false;
                    for (int i = 0; i < value.length; i++) {
                        if ((value[i] & mask[i]) != (data[pos + i] & mask[i]))
                            return false;
                    }
                }
                return true;
            }

            public boolean detect(RandomAccessFile raf) {
                byte buf[] = null;
                
                for (int pos = offset; pos < offset + range; pos++) {
                    try {
                        if (raf.length() < pos + value.length)
                            return false;
                        raf.seek(pos);
                        if (buf == null)
                            buf = new byte[value.length];
                        raf.readFully(buf);
                        for (int i = 0; i < value.length; i++) {
                            if ((value[i] & mask[i]) != (buf[i] & mask[i]))
                                return false;
                        }
                    } catch (IOException e) {
                        return false;
                    }
                }
                return true;
            }
        }
        
        private int priority;
        private ArrayList<Rule> rules = new ArrayList<Rule>();
        private String type;

        Magic(InputStream is) throws IOException {
            String line = readLine(is);
            
            if (!line.endsWith("]"))
                throw new IOException("invalid magic section");

            Rule rule;
            String tokens[] = line.substring(line.charAt(0) == '[' ? 1 : 0,
                line.length() - 1).split(":");
            
            if (tokens.length != 2)
                throw new IOException("invalid magic syntax");
            priority = Integer.parseInt(tokens[0]);
            type = tokens[1];
            while ((rule = readRule(is)) != null)
                rules.add(rule);
        }
        
        public int compareTo(final Magic magic) {
            if (priority != magic.priority)
                return magic.priority - priority;
            else if (magic.rules.size() != rules.size())
                return magic.rules.size() - rules.size();
            return type.compareTo(magic.type);
        }
        
        private Rule readRule(InputStream is) throws IOException {
            int c = is.read(), c2;
            int indent = 0;
            int length;
            byte mask[] = null;
            int offset;
            int range = 1;
            StringBuffer sb = new StringBuffer();
            byte value[];
            int word = 1;

            if (c == '[' || c == -1) {
                return null;
            } else if (c != '>') {
                do {
                    sb.append((char)c);
                } while ((c = is.read()) != '>');
                indent = Integer.parseInt(sb.toString());
                sb.setLength(0);
            }
            while ((c = is.read()) != '=' && c != -1)
                sb.append((char)c);
            offset = Integer.parseInt(sb.toString());
            sb.setLength(0);
            c = is.read();
            c2 = is.read();
            if (c < 0 || c2 < 0)
                throw new IOException("invalid magic match length");
            length = (c << 8) + c2;
            value = new byte[length];
            if (is.read(value) != length)
                throw new IOException("short magic match data");
            c = is.read();
            if (c == '&') {
                mask = new byte[length];
                if (is.read(mask) != length)
                    throw new IOException("short magic mask data");
                c = is.read();
            }
            if (c == '~') {
                while ((c = is.read()) != '+' && c != '\n' && c != -1)
                    sb.append((char)c);
                word = Integer.parseInt(sb.toString());
                sb.setLength(0);
            }
            if (c == '+') {
                while ((c = is.read()) != '\n' && c != -1)
                    sb.append((char)c);
                range = Integer.parseInt(sb.toString());
                sb.setLength(0);
            }
            return new Rule(indent, offset, value, mask, word, range);
        }
    }
    
    public MimeDetect() {}
    
    public MimeDetect(String globList, String magicList) throws IOException {
        parse(globList, magicList);
    }

    public static MimeDetect getMimeDetect() { return mimeDetect; }
    
    public String detect(String file) {
        for (Map.Entry<Glob, String> entry : globs.entrySet())
            if (entry.getKey().pattern.matcher(file).matches())
                return entry.getValue();
        return null;
    }

    public String detect(byte data[]) { return detect(data, null, data.length); }
    
    public String detect(byte data[], String file) {
        return detect(data, file, data.length);
    }
    
    public String detect(byte data[], int limit) {
        return detect(data, null, limit);
    }
    
    public String detect(byte data[], String file, int limit) {
        for (Map.Entry<Magic, String> entry : magics.entrySet()) {
            boolean found = true;
            int indent = 0;
            
            for (Magic.Rule rule : entry.getKey().rules) {
                if (rule.indent == indent) {
                    if (found = rule.detect(data, limit))
                        indent++;
                }
            }
            if (found)
                return entry.getValue();
        }
        return file == null ? null : detect(file);
    }

    public String detect(File file) throws IOException {
        return detect(file, DEFAULT_LIMIT);
    }

    public String detect(File file, int limit) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        
        if (limit == -1) {
            for (Map.Entry<Magic, String> entry : magics.entrySet()) {
                boolean found = true;
                int indent = 0;
                
                for (Magic.Rule rule : entry.getKey().rules) {
                    if (rule.indent == indent) {
                        if (found = rule.detect(raf))
                            indent++;
                    }
                }
                if (found)
                    return entry.getValue();
            }
            return detect(file.getName());
        }
        
        byte data[] = new byte[(int)raf.length() < limit ? (int)raf.length() :
            limit];

        raf.readFully(data);
        return detect(data, file.getName());
    }
    
    public String detect(InputStream is) throws IOException {
        return detect(is, null, DEFAULT_LIMIT);
    }

    public String detect(InputStream is, String file) throws IOException {
        return detect(is, file, DEFAULT_LIMIT);
    }

    public String detect(InputStream is, int limit) throws IOException {
        return detect(is, null, limit);
    }
    
    public String detect(InputStream is, String file, int limit) throws IOException {
        return detect(ByteUtil.getPartialContent(is, limit, limit), file, limit);
        
    }
    
    public void addGlob(String type, String regex, int priority) throws
        IOException {
        globs.put(new Glob(regex, priority), type);
    }

    public void parseGlobs(String fileList) throws IOException {
        String files[] = fileList.split(":");
        
        if (fileList == null || fileList.length() == 0) {
            globs.clear();
            return;
        }
        for (String file : files) {
            BufferedInputStream is = new BufferedInputStream(
                new FileInputStream(new File(file)));
            String line;
            
            while ((line = readLine(is)) != null) {
                if (!line.startsWith("#")) {
                    String tokens[] = line.split(":");
                    
                    if (tokens.length == 2)
                        globs.put(new Glob(tokens[1]), tokens[0]);
                    else if (tokens.length == 3)
                        globs.put(new Glob(tokens[2],
                            Integer.parseInt(tokens[0])), tokens[1]);
                    else
                        throw new IOException("invalid glob syntax " + line);
                }
            }
        }
    }
    
    public void parseMagic(String fileList) throws IOException {
        String files[] = fileList.split(":");
        final String MAGIC_MAGIC = "MIME-Magic\0";

        if (fileList == null || fileList.length() == 0) {
            magics.clear();
            return;
        }
        for (String file : files) {
            InputStream is = new BufferedInputStream(new FileInputStream(new File(file)));
            String line = readLine(is);
            
            if (line == null || !line.equals(MAGIC_MAGIC))
                throw new IOException("invalid magic file ");
            if (is.read() != '[')
                throw new IOException("invalid magic section");
            while (is.available() > 0) {
                Magic magic = new Magic(is);

                magics.put(magic, magic.type);
            }
        }
    }

    public void parse(String globList, String magicList) throws IOException {
        parseGlobs(globList);
        parseMagic(magicList);
    }
    
    private String readLine(InputStream is) throws IOException {
        StringBuffer sb = new StringBuffer();
        int c;
        
        while ((c = is.read()) != -1 && c != '\n')
            sb.append((char)c);
        return sb.length() == 0 ? null : sb.toString();
    }
    
    private static void usage(Options opts) {
        new HelpFormatter().printHelp(MimeDetect.class.getSimpleName() +
            " [options] file", opts);
        System.exit(2);
    }
    
    public static void main(String[] args) {
        int limit = -1;
        MimeDetect md = new MimeDetect();
        Options opts = new Options();
        CommandLineParser parser = new GnuParser();
        int ret = 1;

        opts.addOption("g", "globs", true, "globs file");
        opts.addOption("l", "limit", true, "size limit");
        opts.addOption("m", "magic", true, "magic file");
        opts.addOption("n", "name", false, "name only");
        try {
            CommandLine cl = parser.parse(opts, args);
            String file;
            String globs = LC.shared_mime_info_globs.value();
            String magic = LC.shared_mime_info_magic.value();
            String type;

            if (cl.hasOption('g'))
                globs = cl.getOptionValue('g');
            if (cl.hasOption('l'))
                limit = Integer.parseInt(cl.getOptionValue('l'));
            if (cl.hasOption('m'))
                magic = cl.getOptionValue('m');
            if (cl.getArgs().length != 1)
                usage(opts);
            file = cl.getArgs()[0];
            md.parse(globs, magic);
            if (cl.hasOption('n')) {
                type = md.detect(file);
            } else if (file.equals("-")) {
                byte data[] = new byte[limit == -1 ? 1024 * 1024 : limit];
                int len = System.in.read(data);
                
                type = md.detect(data, len);
            } else {
                type = md.detect(new File(file), limit);
            }
            if (type == null) {
                System.out.println("unknown");
            } else {
                System.out.println(type);
                ret = 0;
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            if (e instanceof UnrecognizedOptionException)
                usage(opts);
        }
        System.exit(ret);
    }
}
