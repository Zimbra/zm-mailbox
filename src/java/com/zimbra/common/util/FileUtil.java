/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 6. 17.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * @author jhahm
 */
public class FileUtil {

	private static final int COPYBUFLEN = 16 * 1024;

	public static void copy(String src, String dest) throws IOException {
		copy(new File(src), new File(dest));
	}

    public static void copy(File from, File to) throws IOException {
    	FileInputStream fin = null;
    	FileOutputStream fout = null;
    	try {
    		fin = new FileInputStream(from);
    		fout = new FileOutputStream(to);
    		FileChannel cin = fin.getChannel();
    		FileChannel cout = fout.getChannel();
    		long target = cin.size();
    		long transferred = cin.transferTo(0, target, cout);
    		if (target != transferred) {
    			throw new IOException("FileUtil.copy(" + from + "," + to + "): incomplete transfer target=" + target + " transferred=" + transferred); 
    		}
    	} finally {
    		if (fin != null) {
    			try {
    				fin.close();
    			} catch (IOException ioe) {
    				ZimbraLog.misc.warn("FileUtil.copy(" + from + "," + to + "): ignoring exception while closing input channel", ioe);
    			}
    		}
    		if (fout != null) {
    			try {
    				fout.close();
    			} catch (IOException ioe) {
    				ZimbraLog.misc.warn("FileUtil.copy(" + from + "," + to + "): ignoring exception while closing output channel", ioe);
    			}
    		}
    	}
    }

    /**
     * Copy an input stream to file.
     * @param in
     * @param closeIn If true, input stream is closed before returning, even
     *                when there is an error
     * @param dest
     * @throws IOException
     */
    public static void copy(InputStream in, boolean closeIn, File dest) throws IOException {
        try {
            FileOutputStream fos = new FileOutputStream(dest);
            byte[] buf = new byte[COPYBUFLEN];
            try {
                int byteRead;
                while ((byteRead = in.read(buf)) != -1) {
                    fos.write(buf, 0, byteRead);
                }
            } finally {
                fos.close();
            }
        } finally {
            if (closeIn) {
                try {
                    in.close();
                } catch (IOException e) {
                    ZimbraLog.misc.warn("Ignoring exception while closing input stream", e);
                }
            }
        }
    }

    public static void ensureDirExists(File dir) throws IOException {
        if (!mkdirs(dir))
            throw new IOException("Unable to create directory " + dir.getPath());
    }
    
	public static void ensureDirExists(String directory) throws IOException {
		File d = new File(directory);
		ensureDirExists(d);
    }

	public static String getTodayDir() {
		DateFormat fmt = new SimpleDateFormat("yyyy.MM.dd");
		return fmt.format(new Date());
	}

	private static class MTimeComparator implements Comparator {
        private boolean mReverse;
        
        private MTimeComparator(boolean reverse) {
            mReverse = reverse;
        }
		public int compare(Object o1, Object o2) {
			File f1 = (File) o1;
			File f2 = (File) o2;
			long diff = mReverse ? 
                    f2.lastModified() - f1.lastModified() : f1.lastModified() - f2.lastModified();
			if (diff < 0)
				return -1;
			else if (diff == 0)
				return 0;
			else
				return 1;
		}
	}

	public static void sortFilesByModifiedTime(File[] files) {
        sortFilesByModifiedTime(files, false);
	}
	
    public static void sortFilesByModifiedTime(File[] files, boolean reverse) {
        MTimeComparator comp = new MTimeComparator(reverse);
        Arrays.sort(files, comp);
    }

    /**
	 * Deletes a directory hierarchy and all files under it.
	 * 
	 * @param directory the directory
	 * @throws IOException if deletion fails for any file or directory in the hierarchy.
	 */
	public static void deleteDir(File directory) throws IOException {
        if (!directory.exists())
            return;
	    File[] files = directory.listFiles();
	    if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDir(files[i]);
                } else {
                    if (!files[i].delete()) {
                        throw new IOException("Cannot remove "
                                + files[i].getPath());
                    }
                }
            }
        }
	    if (directory.exists() && !directory.delete()) {
	        throw new IOException("Cannot remove " + directory.getPath());
	    }
	}

	/**
	 * File.mkdirs() method doesn't work when multiple threads are
	 * creating paths with a common parent directory simultaneously.
	 * Use this method instead.
	 * @return
	 */
	public static boolean mkdirs(File path) {
		if (path.exists()) {
			return true;
		}
		// mkdir() may return false if another thread creates the directory
		// after the above exists() check but before the mkdir() call.
		// As far as this thread is concerned it's a successful mkdirs().
		if (path.mkdir() || path.exists()) {
			return true;
		}
		File canonFile = null;
		try {
			canonFile = path.getCanonicalFile();
		} catch (IOException e) {
			return false;
		}
		String parent = canonFile.getParent();
		if (parent != null) {
			File pf = new File(parent);
			return mkdirs(pf) && (canonFile.mkdir() || canonFile.exists());
		} else
			return false;
	}

    /**
     * Returns the filename without path/scheme.  (substring trailing the
     * last occurrence of '/', '\', or ':' delimiter; null returned if
     * delimiter is the last character of input string)
     * @param filename
     * @return
     */
    public static String trimFilename(String filename) {
        final char[] delimiter = { '/', '\\', ':' };
    
        if (filename == null || filename.equals(""))
            return null;
        for (int i = 0; i < delimiter.length; i++) {
            int index = filename.lastIndexOf(delimiter[i]);
            if (index == filename.length() - 1)
                return null;
            if (index != -1)
                filename = filename.substring(index + 1);
        }
        return filename;
    }
}
