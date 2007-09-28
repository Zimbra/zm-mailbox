/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Apr 18, 2004
 */
package com.zimbra.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * @author schemers
 */
public class ByteUtil {
	
	/**
	 * write the data to the specified path.
	 * @param path
	 * @param data
	 * @throws IOException
	 */
    public static void putContent(String path, byte[] data) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(path));
            fos.write(data);
        } finally {
            if (fos != null)
                fos.close();
        }
    }
    
	/**
	 * read all the content in the specified file and
	 * return as byte array.
	 * @param file file to read
	 * @return content of the file
	 * @throws IOException
	 */
	public static byte[] getContent(File file) throws IOException {
		byte[] buffer = new byte[(int) file.length()];
		
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			int total_read = 0, num_read;
			
			int num_left = buffer.length;
			
			while (num_left > 0 && (num_read = is.read(buffer, total_read, num_left)) != -1) {
				total_read += num_read;
				num_left -= num_read;
			}
		} finally {
            closeStream(is);
		}
		return buffer;
	}

	/**
	 * Get the content of the specified part using getRawInputStream.
	 * @param part
	 * @return raw content for part
	 * @throws MessagingException
	 * @throws IOException
	 */
    public static byte[] getRawContent(MimeBodyPart part) throws MessagingException, IOException {
    	return getContent(part.getRawInputStream(), part.getSize());
    }

    public static byte[] getContent(InputStream is, int sizeHint) throws IOException {
        return getContent(is, sizeHint, -1);
    }

    /**
     * read all data from specified InputStream. InputStream
     * is closed.
     * @param is
     * @param sizeHint estimated size of content.
     * @return content from stream
     * @throws MessagingException
     * @throws IOException
     */
    public static byte[] getContent(InputStream is, int sizeHint, long sizeLimit) throws IOException {
        ByteArrayOutputStream baos = null;
    	try {
    		if (sizeHint < 0)
    			sizeHint = 0; 
    		baos = new ByteArrayOutputStream(sizeHint);
    		byte[] buffer = new byte[8192];
    		int num;
    		while ((num = is.read(buffer)) != -1) {
    			baos.write(buffer, 0, num);
                if (sizeLimit > 0 && baos.size() > sizeLimit)
                    throw new IOException("stream too large");
    		}
    		return baos.toByteArray();
    	} finally {
    		closeStream(is);
    	    if (baos != null)
                baos.close();
    	}
    }

    // When this method is called from SendMsg SOAP command path
    // the getDataSource().getInputStream() call descends into
    // Java Activation Framework to set up the input stream as
    // a PipedInputStream that is fed from a PipedOutputStream
    // using a new thread named "DataHandler.getInputStream".
    // This thread lives on until the PipedInputStream is drained.
    // (See javax.activation.DataHandler.getInputStream(),
    // line 242 in JAF 1.0.2 DataHandler.java)
    //
    // A problem occurs when the above try block throws an
    // exception, such as when the transformation server is down.
    // If we don't drain the PipedInputStream, the getInputStream
    // thread will spin forever waiting for the PipedInputStream's
    // internal circular buffer to free up some space, which it
    // won't after filling up initially because no one is reading
    // from the input stream.  The input stream won't get garbage
    // collected because the getInputStream thread has a reference
    // to it.
    //
    // When the transformation server remains down, more and more
    // getInputStream threads will pile up, and with each thread
    // grabbing memory for stack, the JVM process will grow and
    // eventually will start throwing OutOfMemoryError.
    //
    // To avoid this mess, we must drain the PipedInputStream if
    // the try block doesn't complete successfully.
    //
    // If this method is called from LMTP path the input stream
    // returned is a FileInputStream, and no special clean up is
    // necessary.
    public static void closeStream(InputStream is) {
        if (is == null)
            return;

        if (is instanceof PipedInputStream) {
            try {
                while (is.read() != -1);
            } catch (Exception e) {
                ZimbraLog.misc.warn("exception while draining PipedInputStream", e);
            }
        }

        try {
            is.close();
        } catch (Exception e) {
            ZimbraLog.misc.warn("ignoring exception while closing input stream", e);
        }
    }

    public static void closeStream(OutputStream os) {
        if (os == null)
            return;

        try {
            os.close();
        } catch (Exception e) {
            ZimbraLog.misc.warn("ignoring exception while closing output stream", e);
        }
    }

    /**
     * find the index of "target" within "source".
     * @param source the array being searched
     * @param offset where to start within that array
     * @param target the array we are searching for
     * @return index of target within source, or -1 if not found.
     */
    public static int indexOf(byte[] source, int offset, byte[] target) {
    	int i = offset;
    	int slen = source.length;
    	int tlen = target.length;
    	int max = offset + (slen - tlen);
    	byte first = target[0];
    	
    	while (i <= max) {
    		if (source[i] == first) {
    			boolean match = true;
    			// look at rest
    			for (int j=1; match && j < tlen; j++) {
    				match = source[i+j] == target[j];
    			}
    			if (match)
    				return i;
    		}
    		i++;
    	}
    	return -1;
    }

    public static boolean isASCII(byte[] data) {
        if (data == null)
            return false;
        int i;
        for (i = 0; i < data.length; i++) {
            byte c = data[i];
            // invalid control characters, DEL, and the high-order bit
            if ((c < 0x20 && c != 0x09 && c != 0x0A && c != 0x0D) || c >= 0x7F)
                return false;
        }
        return true;
    }

    /**
     * compress the supplied data using GZIPOutputStream
     * and return the compressed data.
	 * @param data data to compress
	 * @return compressesd data
	 */
	public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = null;
        GZIPOutputStream gos = null;
        try {
            baos = new ByteArrayOutputStream(data.length); //data.length overkill
		    gos = new GZIPOutputStream(baos);
		    gos.write(data);
		    gos.finish();            
		    return baos.toByteArray();
        } finally {
        	    if (gos != null) {
                gos.close();
            } else if (baos != null)
                baos.close();
        }
	}

    /**
     * uncompress the supplied data using GZIPInputStream
     * and return the uncompressed data.
	 * @param data data to uncompress
	 * @return uncompressesd data
	 */
	public static byte[] uncompress(byte[] data) throws IOException {
	    // TODO: optimize, this makes my head hurt
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        GZIPInputStream gis = null;
        try {
            int estimatedResultSize = data.length * 3;
    		baos = new ByteArrayOutputStream(estimatedResultSize);
    		bais = new ByteArrayInputStream(data);
    		byte[] buffer = new byte[8192];
    		gis = new GZIPInputStream(bais);

    		int numRead;
    		while ((numRead = gis.read(buffer, 0, buffer.length)) != -1) {
    		    baos.write(buffer, 0, numRead);
    		}
    		return baos.toByteArray();
        } finally {
        	if (gis != null)
                gis.close();
            else if (bais != null)
                bais.close();
            if (baos != null)
                baos.close();
        }
	}

	/**
	 * Determines if the data contained in the buffer is gzipped
	 * by matching the first 2 bytes with GZIP magic GZIP_MAGIC (0x8b1f).
	 * @param data
	 * @return
	 */
	public static boolean isGzipped(byte[] data) {
		return data != null && data.length > 2 &&
			((data[0] | (data[1] << 8)) == GZIPInputStream.GZIP_MAGIC);
	}

    private static String encodeFSSafeBase64(byte[] data) {
        byte[] encoded = Base64.encodeBase64(data);
        // Replace '/' with ',' to make the digest filesystem-safe.
        for (int i = 0; i < encoded.length; i++) {
            if (encoded[i] == (byte) '/')
                encoded[i] = (byte) ',';
        }
        return new String(encoded);
    }

    private static byte[] decodeFSSafeBase64(String str) {
        byte[] bytes = str.getBytes();
        // Undo the mapping done in encodeFSSafeBase64().
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == (byte) ',')
                bytes[i] = (byte) '/';
        }
        return Base64.decodeBase64(bytes);
    }

	/**
	 * return the SHA1 digest of the supplied data.
	 * @param data data to digest
	 * @param base64 if true, return as base64 String, otherwise return
	 *  as hex string.
	 * @return
	 */
	public static String getSHA1Digest(byte[] data, boolean base64) {
	    try {
	        MessageDigest md = MessageDigest.getInstance("SHA1");
	        byte[] digest = md.digest(data);
	        if (base64)
                return encodeFSSafeBase64(digest);
	        else 
	            return new String(Hex.encodeHex(digest));
	    } catch (NoSuchAlgorithmException e) {
	        // this should never happen unless the JDK is foobar
	        //	e.printStackTrace();
	        throw new RuntimeException(e);
	    }
	}

	/**
	 * return the MD5 digest of the supplied data.
	 * @param data data to digest
	 * @param base64 if true, return as base64 String, otherwise return
	 *  as hex string.
	 * @return
	 */
	public static String getMD5Digest(byte[] data, boolean base64) {
	    try {
	        MessageDigest md = MessageDigest.getInstance("MD5");
	        byte[] digest = md.digest(data);
	        if (base64)
                return encodeFSSafeBase64(digest);
	        else 
	            return new String(Hex.encodeHex(digest));
	    } catch (NoSuchAlgorithmException e) {
	        // this should never happen unless the JDK is foobar
	        //	e.printStackTrace();
	        throw new RuntimeException(e);
	    }
	}

	/**
	 * Returns the digest using the default algorithm.
	 * @param data
	 * @return
	 */
	public static String getDigest(byte[] data) {
		return getSHA1Digest(data, true);
	}

	/**
	 * Returns byte array containing binary version of digest.
	 * @param digest
	 * @return
	 */
	public static byte[] getBinaryDigest(String digest) {
        return decodeFSSafeBase64(digest);
	}

    public static boolean isValidDigest(String digest) {
        if (digest != null) {
            byte[] bin = decodeFSSafeBase64(digest);
            if (bin != null) {
                String str = encodeFSSafeBase64(bin);
                return digest.equals(str);
            }
        }
        return false;
    }

    /**
     * Copy an input stream fully to output stream.
     * @param in
     * @param closeIn If true, input stream is closed before returning, even
     *                when there is an error.
     * @param out
     * @param closeOut If true, output stream is closed before returning, even
     *                 when there is an error.
     * @return
     * @throws IOException
     */
    public static int copy(InputStream in, boolean closeIn, OutputStream out, boolean closeOut) throws IOException {
        return copy(in, closeIn, out, closeOut, -1L);
    }

    public static int copy(InputStream in, boolean closeIn, OutputStream out, boolean closeOut, long maxLength) throws IOException {
        try {
            byte buffer[] = new byte[8192];
            int numRead;
            int transferred = 0;
            while ((numRead = in.read(buffer)) >= 0) {
                out.write(buffer, 0, numRead);
                transferred += numRead;

                if (maxLength >= 0 && transferred > maxLength)
                    throw new IOException("stream exceeded allowable length: " + transferred);
            }
            return transferred;
        } finally {
            if (closeIn)
                closeStream(in);
            if (closeOut)
                closeStream(out);
        }
    }

    // Custom read/writeUTF8 methods to replace DataInputStream.readUTF() and 
    // DataOutputStream.writeUTF() which have 64KB limit

    private static final int MAX_STRING_LEN = 32 * 1024 * 1024;     // 32MB

    public static void writeUTF8(DataOutput out, String str) throws IOException {
        // Special case: Null string is serialized as length of -1.
        if (str == null) {
            out.writeInt(-1);
            return;
        }

        int len = str.length();
        if (len > MAX_STRING_LEN)
            throw new IOException("String length " + len + " is too long in ByteUtil.writeUTF8(); max=" + MAX_STRING_LEN);
        if (len > 0) {
            byte[] buf = str.getBytes("UTF-8");
            out.writeInt(buf.length);
            out.write(buf);
        } else
            out.writeInt(0);
    }

    public static String readUTF8(DataInput in) throws IOException {
        int len = in.readInt();
        if (len > MAX_STRING_LEN) {
            throw new IOException("String length " + len + " is too long in ByteUtil.writeUTF8(); max=" + MAX_STRING_LEN);
        } else if (len > 0) {
            byte[] buf = new byte[len];
            in.readFully(buf, 0, len);
            return new String(buf, "UTF-8");
        } else if (len == 0) {
            return "";
        } else if (len == -1) {
            return null;
        } else {
            throw new IOException("Invalid length " + len + " in ByteUtil.readUTF8()");
        }
    }

    public static class TeeOutputStream extends OutputStream {
        OutputStream stream1, stream2;

        public TeeOutputStream(OutputStream one, OutputStream two) {
            if (one == two)
                two = null;
            stream1 = one;  stream2 = two;
        }

        @Override public void write(int b) throws IOException {
            if (stream1 != null)  stream1.write(b);
            if (stream2 != null)  stream2.write(b);
        }

        @Override public void flush() throws IOException {
            if (stream1 != null)  stream1.flush();
            if (stream2 != null)  stream2.flush();
        }

        @Override public void write(byte b[], int off, int len) throws IOException {
            if (stream1 != null)  stream1.write(b, off, len);
            if (stream2 != null)  stream2.write(b, off, len);
        }
    }
}
