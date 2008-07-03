/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.service.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.tar.*;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.util.ItemData;

public class ItemDataFile {
    public static void create(String path, OutputStream os) throws IOException {
        create(path, null, "UTF-8", os);
    }

    public static void create(String path, byte[] types, String cset,
        OutputStream os) throws IOException {
        File f = new File(path);
        TarOutputStream tos = new TarOutputStream(new GZIPOutputStream(os),
            cset == null ? "UTF-8" : cset);
        
        tos.setLongFileMode(TarOutputStream.LONGFILE_GNU);
        try {
            if (f.isDirectory())
                addDir(f, f.getPath(), types, tos);
            else
                addFile(f, f.getParent(), types, tos);
        } finally {
            tos.close();
        }
    }
    
    public static void extract(InputStream is) throws IOException {
        extract(is, true, null, null, "UTF-8");
    }
    
    public static void extract(InputStream is, boolean meta, byte[] types,
        String cset, String dir) throws IOException {
        byte[] buf = new byte[TarBuffer.DEFAULT_BLKSIZE];
        TarEntry te;
        TarInputStream tis = new TarInputStream(new GZIPInputStream(is),
            cset == null ? "UTF-8" : cset);

        if (dir == null)
            dir = ".";
        try {
            while ((te = tis.getNextEntry()) != null) {
                if (skip(types, (byte)te.getMajorDeviceId()))
                    continue;

                File f = new File(dir + File.separator + te.getName());
                FileOutputStream out;
    
                if (!f.getParent().equals("."))
                    f.getParentFile().mkdir();
                if (te.getName().endsWith(".meta")) {
                    if (!meta)
                        continue;
                    out = new FileOutputStream(f);
                    ItemData id = new ItemData(getData(tis, te));
                    out.write(id.encode(2).getBytes("UTF-8"));
                } else {
                    int in;
                    
                    out = new FileOutputStream(f);
                    while ((in = tis.read(buf)) != -1)
                        out.write(buf, 0, in);
                }
                out.close();
                f.setLastModified(te.getModTime().getTime());
            }
        } finally {
            tis.close();
        }
    }

    public static void list(InputStream is, PrintStream os) throws IOException {
        list(is, null, "UTF-8", os);
    }
    
    public static void list(InputStream is, byte[] types, String cset,
        PrintStream os) throws IOException {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,
            DateFormat.SHORT);
        TarEntry te;
        TarInputStream tis = new TarInputStream(new GZIPInputStream(is),
            cset == null ? "UTF-8" : cset);

        os.format("%-13s %17s %10s %6s %s\n", "TYPE", "DATE", "SIZE", "METASZ",
        "PATH");
        try {
            TarEntry idEntry = null;
            
            while ((te = tis.getNextEntry()) != null) {
                if (te.getName().endsWith(".meta")) {
                    if (idEntry != null && !skip(types,
                        (byte)idEntry.getMajorDeviceId()))
                        os.format("%-13s %17s %10s %6d %s\n",
                            idEntry.getGroupName(),
                            df.format(idEntry.getModTime()), 0,
                            idEntry.getSize(), idEntry.getName().substring(0,
                                idEntry.getName().indexOf(".meta")));
                    idEntry = te;
                } else {
                    if (!skip(types, (byte)te.getMajorDeviceId()))
                        os.format("%-13s %17s %10s %6d %s\n", te.getGroupName(),
                            df.format(te.getModTime()), te.getSize(),
                            idEntry == null ? 0 : idEntry.getSize(), te.getName());
                    idEntry = null;
                }
            }
            if (idEntry != null && !skip(types,
                (byte)idEntry.getMajorDeviceId()))
                os.format("%-13s %17s %10s %6d %s\n", idEntry.getGroupName(),
                    df.format(idEntry.getModTime()), 0,
                    idEntry.getSize(), idEntry.getName().substring(0,
                    idEntry.getName().indexOf(".meta")));
        } finally {
            tis.close();
        }
    }

    static byte[] getData(TarInputStream tis, TarEntry te) throws IOException {
        int dsz = (int)te.getSize();
        byte[] data = new byte[dsz];
        
        if (tis.read(data, 0, dsz) != dsz)
            throw new IOException("archive read err");
        return data;
    }
    
    static boolean skip(byte[] types, byte type) {
        if (types == null)
            return false;
        for (byte t : types)
            if (t == type)
                return false;
        return true;
    }
    
    static void addDir(File f, String topdir, byte[] types, TarOutputStream tos)
        throws IOException {
        String path = f.getPath();
        String[] all = f.list();
        List<File>dirs = new ArrayList<File>();
        List<File>files = new ArrayList<File>();
    
        Arrays.sort(all);
        for (String file : all) {
            File subf = new File(path + File.separator + file);
      
            if (subf.getName().equals("Tags") && path.equals(topdir)) {
                addDir(subf, topdir, types, tos);
            } else if (subf.isDirectory()) {
                dirs.add(subf);
            } else if (subf.getName().endsWith(".meta")) {
                file = subf.getPath().substring(0,
                    subf.getPath().indexOf(".meta"));
                f = new File(file);
                if (!f.exists() || f.isDirectory())
                    files.add(f);
            } else {
                files.add(subf);
            }
        }
        for (File file : files)
            addFile(file, topdir, types, tos);
        for (File dir : dirs)
            addDir(dir, topdir, types, tos);
    }

    static void addFile(File f, String topdir, byte[] types, TarOutputStream tos)
        throws IOException {
        ItemData id = null;
        String path = f.getPath();
        File mf = new File(path + ".meta");
        TarEntry te;
    
        if (path.indexOf(topdir) == 0)
            path = path.substring(topdir.length() + 1);
        path = path.replace('\\', '/');
        if (mf.exists()) {
            byte[] meta = new byte[(int)mf.length()];
            FileInputStream fis = new FileInputStream(mf);
            
            if (fis.read(meta) != mf.length())
               throw new IOException("meta read err: " + f.getPath());
            fis.close();
            id = new ItemData(meta);
            if (skip(types, id.ud.type))
                return;
            te = new TarEntry(path + ".meta");
            te.setGroupName(MailItem.getNameForType(id.ud.type));
            te.setMajorDeviceId(id.ud.type);
            te.setMinorDeviceId(id.ud.id);
            te.setModTime(mf.lastModified());
            te.setSize(meta.length);
            tos.putNextEntry(te);
            tos.write(meta);
        }
        if (f.exists() && !f.isDirectory() && (id != null || types == null)) {
            byte[] buf = new byte[TarBuffer.DEFAULT_BLKSIZE];
            FileInputStream fis = new FileInputStream(f);
            int in;
            
            te = new TarEntry(path + ".meta");
            te.setGroupName(MailItem.getNameForType(id.ud.type));
            te.setMajorDeviceId(id.ud.type);
            te.setMinorDeviceId(id.ud.id);
            te.setModTime(mf.lastModified());
            te.setSize(f.length());
            tos.putNextEntry(te);
            if ((in = fis.read(buf)) > 0)
                tos.write(buf, 0, in);
            fis.close();
        }
        tos.closeEntry();
    }

    private static void usage(Options opts) {
        new HelpFormatter().printHelp(ItemDataFile.class.getSimpleName() +
            " [options] file", opts);
        System.exit(1);
    }

    public static void main(String[] args) {
        String cset = null;
        Options opts = new Options();
        CommandLineParser parser = new GnuParser();

        opts.addOption("a", "assemble", false, "assemble backup");
        opts.addOption("c", "charset", true, "path charset");
        opts.addOption("e", "extract", false, "extract backup");
        opts.addOption("l", "list", false, "list backup");
        opts.addOption("n", "nometa", false, "ignore metadata");
        opts.addOption("p", "path", true, "extracted backup path");
        opts.addOption("t", "types", true, "item types");
        ZimbraLog.toolSetupLog4j("ERROR", null);
        try {
            CommandLine cl = parser.parse(opts, args);
            String path = ".";
            String file = null;
            boolean meta = true;
            byte[] types = null;

            if (cl.hasOption('c'))
                cset = cl.getOptionValue('c');
            if (cl.hasOption('n'))
                meta = false;
            if (cl.hasOption('p'))
                path = cl.getOptionValue('p');
            if (cl.hasOption('t'))
                types = MailboxIndex.parseTypesString(cl.getOptionValue('t'));
            if (cl.getArgs().length != 1)
                usage(opts);
            file = cl.getArgs()[0];
            if (cl.hasOption('c'))
                create(path, types, cset, new FileOutputStream(file));
            else if (cl.hasOption('e'))
                extract(new FileInputStream(file), meta, types, cset, path);
            else if (cl.hasOption('l') && file.equals("-"))
                list(System.in, types, cset, System.out);
            else if (cl.hasOption('l'))
                list(new FileInputStream(file), types, cset, System.out);
            else
                usage(opts);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            if (e instanceof UnrecognizedOptionException)
                usage(opts);
            System.exit(1);
        }
    }
}
