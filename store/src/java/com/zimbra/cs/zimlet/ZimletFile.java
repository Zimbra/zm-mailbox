/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.TemplateCompiler;

/**
 * Represents Zimlet distribution file.
 * 
 * @author jylee
 *
 */
public class ZimletFile implements Comparable<ZimletFile> {

    private File mBase;
    private InputStream mBaseStream;
    private byte[] mCopy;
    private static Pattern BAD_FILE_NAME_PATTERN = Pattern.compile("(\\.\\.\\/)|(\\p{Cntrl})");
    public int compareTo(ZimletFile obj) {
        return getZimletName().compareTo(obj.getZimletName());
    }

    public static abstract class ZimletEntry {
        protected String mName;

        protected ZimletEntry(String name) throws ZimletException {
            //security checks
            Matcher nameMatcher = BAD_FILE_NAME_PATTERN.matcher(name);
            if(nameMatcher.find()) {
                throw ZimletException.INVALID_ZIMLET_ENTRY(name);
            }
            if(new File(name).isAbsolute()) {
                throw ZimletException.INVALID_ABSOLUTE_PATH(name);
            }
            mName = name;
        }
        public String getName() {
            return mName;
        }
        public abstract byte[] getContents() throws IOException;
        public abstract InputStream getContentStream() throws IOException;
    }

    public static class ZimletZipEntry extends ZimletEntry {
        private ZipFile  mContainer;
        private ZipEntry mEntry;

        public ZimletZipEntry(ZipFile f, ZipEntry e) throws ZimletException {
            super(e.getName());
            mContainer = f;
            mEntry = e;
        }
        public byte[] getContents() throws IOException {
            InputStream is = mContainer.getInputStream(mEntry);
            byte[] ret = ByteUtil.getContent(is, (int)mEntry.getSize());
            is.close();
            return ret;
        }
        public InputStream getContentStream() throws IOException {
            return mContainer.getInputStream(mEntry);
        }
    }

    public static class ZimletDirEntry extends ZimletEntry {
        private File mFile;

        public ZimletDirEntry(File f) throws ZimletException {
            super(f.getName());
            mFile = f;
        }
        public byte[] getContents() throws IOException {
            InputStream is = new FileInputStream(mFile);
            byte[] ret = ByteUtil.getContent(is, (int)mFile.length());
            is.close();
            return ret;
        }
        public InputStream getContentStream() throws IOException {
            return new FileInputStream(mFile);
        }
    }

    public static class ZimletRawEntry extends ZimletEntry {
        private byte[] mData;
        
        public ZimletRawEntry(InputStream is, String name, int size) throws IOException, ZimletException {
            super(name);
            mData = new byte[size];
            int num;
            int offset, len;
            offset = 0;
            len = size;
            while (len > 0 && (num = is.read(mData, offset, len)) != -1) {
                offset += num;
                len -= num;
            }
        }
        public byte[] getContents() throws IOException {
            return mData;
        }
        public InputStream getContentStream() throws IOException {
            return new ByteArrayInputStream(mData);
        }
    }
    
    private static final String XML_SUFFIX = ".xml";
    private static final String ZIP_SUFFIX = ".zip";
    private static final String CONFIG_TMPL = "config_template.xml";

    private String mDescFile;
    private Map<String,ZimletEntry> mEntries;

    private ZimletDescription mDesc;
    private String            mDescString;
    private ZimletConfig      mConfig;
    private String            mConfigString;

    private String mZimletName;

    public ZimletFile(String zimlet) throws IOException, ZimletException {
        mBase = new File(findZimlet(zimlet));
        initialize();
    }

    public ZimletFile(File parent, String zimlet) throws IOException, ZimletException {
        mBase = new File(parent, zimlet);
        initialize();
    }

    public ZimletFile(String name, InputStream is) throws IOException, ZimletException {
        mBaseStream = is;
        initialize(name);
    }

    private void initialize() throws IOException, ZimletException {
        String name = mBase.getName().toLowerCase();
        int index = name.lastIndexOf(File.separatorChar);
        if (index > 0) {
            name = name.substring(index + 1);
        }
        initialize(name);
    }

    @SuppressWarnings("unchecked")
    private void initialize(String name) throws IOException, ZimletException {
        if(!name.matches(ZimletUtil.ZIMLET_NAME_REGEX)) {
            //a bad zimlet name will result in an invalid or non-existent path for description file,
            //so there is no need to try and load a zimlet with a bad name 
            throw ZimletException.INVALID_ZIMLET_NAME();
        }
        if (name.endsWith(ZIP_SUFFIX)) {
            name = name.substring(0, name.length() - 4);
        }
        mDescFile = name + XML_SUFFIX;
        
        mEntries = new HashMap<String,ZimletEntry>();

        if (mBaseStream != null) {
            mCopy = ByteUtil.getContent(mBaseStream, 0);
            JarInputStream zis = new JarInputStream(new ByteArrayInputStream(mCopy));
            Manifest mf = zis.getManifest();
            if (mf != null) {
                mDescFile = mf.getMainAttributes().getValue("Zimlet-Description-File");
            }
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (entry.getSize() > 0)
                    mEntries.put(entry.getName().toLowerCase(), new ZimletRawEntry(zis, entry.getName(), (int)entry.getSize()));
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
            zis.close();
        } else if (mBase.isDirectory()) {
            addFileEntry(mBase, "");
        } else {
            JarFile jar = new JarFile(mBase);
            Manifest mf = jar.getManifest();
            if (mf != null) {
                mDescFile = mf.getMainAttributes().getValue("Zimlet-Description-File");
            }
            Enumeration entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.getSize() > 0)
                    mEntries.put(entry.getName().toLowerCase(), new ZimletZipEntry(jar, entry));
            }
        }

        initZimletDescription();
        mZimletName = mDesc.getName();
    }

    private void addFileEntry(File f, String dir) throws IOException, ZimletException {
        addFileEntry(f, dir, f);
    }
    private void addFileEntry(File f, String dir, File base) throws IOException, ZimletException {
        File[] files = f.listFiles();
        if (files == null)
            return;
        for (File file : files) {
            String name = ((dir.length() == 0) ? "" : dir + "/") + file.getName().toLowerCase();
            if (file.isDirectory()) {
                addFileEntry(file, name, base);
                continue;
            }
            if (!name.endsWith(".template.js")) {
                mEntries.put(name, new ZimletDirEntry(file));
            }
            if (name.endsWith(".template")) {
                addTemplateFileEntry(file, dir, base);
            }
        }
    }

    private void addTemplateFileEntry(File ifile, String dir, File base) throws IOException, ZimletException {
        File ofile = new File(ifile.getParentFile(), ifile.getName()+".js");
        if (!ofile.exists() || ifile.lastModified() > ofile.lastModified()) {
            String prefix = base.getName() + ".";
            String dirname = base.getAbsolutePath();
            if (!dirname.endsWith(File.separator)) dirname += File.separatorChar;
            String filename = ifile.getAbsolutePath().substring(dirname.length());
            String pkg = prefix + filename.replaceAll("\\.[^\\.]+$", "").replace(File.separatorChar, '.');
            TemplateCompiler.compile(ifile, ofile, pkg, true, true);
        }
        String name = ((dir.length() == 0) ? "" : dir + "/") + ofile.getName().toLowerCase();
        mEntries.put(name, new ZimletDirEntry(ofile));
    }

    private void initZimletDescription() throws IOException, ZimletException {
        if (mDesc == null) {
            ZimletEntry entry = (ZimletEntry)mEntries.get(mDescFile.toLowerCase());
            if (entry == null) {
                throw new FileNotFoundException("zimlet description not found: " + mDescFile);
            }
            mDescString = new String(entry.getContents(), "UTF-8");
            mDesc = new ZimletDescription(mDescString);
        }
    }

    /**
     * 
     * @return The original XML zimlet description.
     * @throws IOException
     * @throws ZimletException
     */
    public String getZimletDescString() throws IOException, ZimletException {
        initZimletDescription();
        return mDescString;
    }

    /**
     * 
     * @return The zimlet description for this instance.
     * @throws IOException
     * @throws ZimletException
     */
    public ZimletDescription getZimletDescription() throws IOException, ZimletException {
        initZimletDescription();
        return mDesc;
    }

    public boolean hasZimletConfig() {
        return mEntries.containsKey(CONFIG_TMPL);
    }

    private void initZimletConfig() throws IOException, ZimletException {
        if (mConfig == null) {
            ZimletEntry entry = (ZimletEntry)mEntries.get(CONFIG_TMPL);
            if (entry == null) {
                throw new FileNotFoundException("zimlet config not found: " + CONFIG_TMPL);
            }
            mConfigString = new String(entry.getContents());
            mConfig = new ZimletConfig(mConfigString);
        }
    }

    /**
     * 
     * @return The original XML config template.
     * @throws IOException
     * @throws ZimletException
     */
    public String getZimletConfigString() throws IOException, ZimletException {
        initZimletConfig();
        return mConfigString;
    }

    /**
     * 
     * @return The configuration section for this instance.
     * @throws IOException
     * @throws ZimletException
     */
    public ZimletConfig getZimletConfig() throws IOException, ZimletException {
        initZimletConfig();
        return mConfig;
    }

    public ZimletEntry getEntry(String name) {
        return mEntries.get(name.toLowerCase());
    }

    public Collection<ZimletEntry> getAllEntries() {
        return mEntries.values();
    }

    public String getZimletName() {
        return mZimletName;
    }

    public String getName() {
        return getZimletName() + ".zip";
    }

    public URL toURL() throws MalformedURLException {
        return mBase.toURL();
    }

    public File getFile() {
        return mBase;
    }

    private static String findZimlet(String zimlet) throws FileNotFoundException {
        File zimletFile = new File(zimlet);

        if (!zimletFile.exists()) {
            zimletFile = new File(zimlet + ZIP_SUFFIX);
            if (!zimletFile.exists()) {
                throw new FileNotFoundException("Zimlet not found: " + zimlet);
            }
        }

        if (zimletFile.isDirectory()) {
            String[] files = zimletFile.list();
            String zimletTargetName = zimletFile.getName() + ZIP_SUFFIX;
            int i;
            for (i = 0; i < files.length; i++) {
                if (files[i].equals(zimletTargetName)) {
                    return zimlet + File.separator + files[i];
                }
            }
        }

        return zimlet;
    }

    public byte[] toByteArray() {
        return mCopy;
    }
}