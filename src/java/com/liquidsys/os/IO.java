package com.zimbra.native;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class IO {

	private static final boolean mNativeIO;
	
	static {
		String os = System.getProperty("os.name");
		if (os != null && os.equalsIgnoreCase("Linux")) {
			mNativeIO = true;
		} else {
			mNativeIO = false;
		}
		
		if (mNativeIO) {
			System.loadLibrary("liquidos");
		}
	}

	public static boolean usingNativeIO() {
		return mNativeIO;
	}

	private static native void link0(byte[] oldpath, byte[] newpath) throws IOException;
	
    private static native int linkCount0(byte[] path) throws IOException;

	/**
	 * Creates a hard link "newpath" to existing "oldpath".  If
	 * we do not support hard links on current platform we just copy
	 * the file.
	 */
	public static void link(String oldpath, String newpath) throws IOException {
		if (mNativeIO) {
			link0(oldpath.getBytes(), newpath.getBytes());
		} else {
			FileInputStream in = new FileInputStream(oldpath);
			FileOutputStream out = new FileOutputStream(newpath);
			byte[] buffer = new byte[16 * 1024];
			int bytes_read;
			while ((bytes_read = in.read(buffer, 0, buffer.length)) > -1) {
				out.write(buffer, 0, bytes_read);
			}
			out.close();
			in.close();
		}
	}

    public static int linkCount(String path) throws IOException {
        if (mNativeIO) {
            // native method throws specified exception
            return linkCount0(path.getBytes());
        } else {
            return 1;
        }
    }
}
