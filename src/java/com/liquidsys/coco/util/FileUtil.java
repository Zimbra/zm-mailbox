/*
 * Created on 2004. 6. 17.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.liquidsys.coco.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FileUtil {

	private static final int COPYBUFLEN = 16 * 1024;

	public static void copy(String src, String dest) throws IOException {
		copy(new File(src), new File(dest));
	}

	public static void copy(File src, File dest) throws IOException {
		FileInputStream in = new FileInputStream(src);
        copy(in, dest);
		in.close();
	}

    public static void copy(InputStream in, File dest) throws IOException {
        FileOutputStream fos = new FileOutputStream(dest);
        byte[] buf = new byte[COPYBUFLEN];
        BufferedOutputStream out = new BufferedOutputStream(fos);
        try {
            int byteRead;
            while ((byteRead = in.read(buf)) != -1) {
                out.write(buf, 0, byteRead);
            }
        } finally {
            out.close();
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
}
