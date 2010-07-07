/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.Reader;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
	 * Count the total number of bytes of the <code>InputStream</code>
	 * @param is The stream to read from.
	 * @return total number of bytes
	 * @throws IOException
	 */
	public static int countBytes(InputStream is) throws IOException {
		byte[] buf = new byte[8192];
		int count = 0;
		int num = 0;
		while ((num = is.read(buf)) != -1)
			count += num;
		return count;
	}

    /** Reads all data from the <code>InputStream</code> into a <tt>byte[]</tt>
     *  array.  Closes the stream, regardless of whether an error occurs.
     * @param is        The stream to read from.
     * @param sizeHint  A (non-binding) hint as to the size of the resulting
     *                  <tt>byte[]</tt> array, or <tt>-1</tt> for no hint. */
    public static byte[] getContent(InputStream is, int sizeHint) throws IOException {
        return getContent(is, sizeHint, -1);
    }

    /** Reads all data from the <code>InputStream</code> into a <tt>byte[]</tt>
     *  array.
     * @param is        The stream to read from.
     * @param sizeHint  A (non-binding) hint as to the size of the resulting
     *                  <tt>byte[]</tt> array, or <tt>-1</tt> for no hint.
     * @param close     <tt>true</tt> to close the stream after reading. */
    public static byte[] getContent(InputStream is, int sizeHint, boolean close) throws IOException {
        return getContent(is, -1, sizeHint, -1, close);
    }

    /** Reads all data from the <code>InputStream</code> into a <tt>byte[]</tt>
     *  array.  Closes the stream, regardless of whether an error occurs.  If
     *  a positive <code>sizeLimit</code> is specified and the stream is
     *  larger than that limit, an <code>IOException</code> is thrown.
     * @param is        The stream to read from.
     * @param sizeHint  A (non-binding) hint as to the size of the resulting
     *                  <tt>byte[]</tt> array, or <tt>-1</tt> for no hint.
     * @param sizeLimit The maximum number of bytes that can be copied from the
     *                  stream before an <code>IOException</code> is thrown,
     *                  or <tt>-1</tt> for no limit. */
    public static byte[] getContent(InputStream is, int sizeHint, long sizeLimit) throws IOException {
        return getContent(is, -1, sizeHint, sizeLimit, true);
    }

    /** Reads a certain quantity of data from the <code>InputStream</code> into
     *  a <tt>byte[]</tt> array.  Closes the stream, regardless of whether an
     *  error occurs.  If a nonnegative <code>length</code> is specified, the
     *  amount of data read into the array is capped by that value; otherwise,
     *  the method behaves exactly as {@link #getContent(InputStream, int)}.
     * @param is        The stream to read from.
     * @param length    The maximum number of bytes that will be copied from
     *                  the stream.
     * @param sizeHint  A (non-binding) hint as to the size of the resulting
     *                  <tt>byte[]</tt> array, or <tt>-1</tt> for no hint. */
    public static byte[] getPartialContent(InputStream is, int length, int sizeHint) throws IOException {
        return getContent(is, length, sizeHint, -1, true);
    }

    private static byte[] getContent(InputStream is, int length, int sizeHint, long sizeLimit, boolean close) throws IOException {
        if (length == 0)
            return new byte[0];

        try {
            BufferStream bs = sizeLimit == -1 ?  new BufferStream(sizeHint,
                Integer.MAX_VALUE, Integer.MAX_VALUE) : new BufferStream(sizeHint,
                (int)sizeLimit, sizeLimit);
            
            bs.readFrom(is, length == -1 ? Long.MAX_VALUE : length);
            if (sizeLimit > 0 && bs.size() > sizeLimit)
                throw new IOException("stream too large");
            return bs.toByteArray();
        } finally {
            if (close)
                closeStream(is);
        }
    }

    /**
     * Reads a <tt>String</tt> from the given <tt>Reader</tt>.  Reads
     * until the either end of the stream is hit or until <tt>length</tt> characters
     * are read.
     * 
     * @param reader the content source
     * @param length number of characters to read, or <tt>-1</tt> for no limit
     * @param close <tt>true</tt> to close the <tt>Reader</tt> when done
     * @return the content or an empty <tt>String</tt> if no content is available
     */
    public static String getContent(Reader reader, int length, boolean close)
    throws IOException {
        if (reader == null || length == 0) {
            return "";
        }
        if (length < 0) {
            length = Integer.MAX_VALUE;
        }
        char[] buf = new char[Math.min(1024, length)];
        int totalRead = 0;
        StringBuilder retVal = new StringBuilder(buf.length);
        
        try {
            while (true) {
                int numToRead = Math.min(buf.length, length - totalRead);
                if (numToRead <= 0) {
                    break;
                }
                int numRead = reader.read(buf);
                if (numRead < 0) {
                    break;
                }
                retVal.append(buf, 0, numRead);
                totalRead += numRead;
            }
            return retVal.toString();
        } finally {
            if (close) {
                try {
                    reader.close();
                } catch (IOException e) {
                    ZimbraLog.misc.warn("Unable to close Reader", e);
                }
            }
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
                ZimbraLog.misc.debug("ignoring exception while draining PipedInputStream", e);
            }
        }

        try {
            is.close();
        } catch (Exception e) {
            ZimbraLog.misc.debug("ignoring exception while closing input stream", e);
        }
    }

    public static void closeStream(OutputStream os) {
        if (os == null)
            return;

        try {
            os.close();
        } catch (Exception e) {
            ZimbraLog.misc.debug("ignoring exception while closing output stream", e);
        }
    }
    
    /**
     * Closes the given reader and ignores any exceptions.
     * @param r the <tt>Reader</tt>, may be <tt>null</tt>
     */
    public static void closeReader(Reader r) {
        if (r == null) {
            return;
        }
        try {
            r.close();
        } catch (IOException e) {
            ZimbraLog.misc.debug("ignoring exception while closing reader", e);
        }
    }
    
    /**
     * Closes the given writer and ignores any exceptions.
     * @param w the <tt>Writer</tt>, may be <tt>null</tt>
     */
    public static void closeWriter(Writer w) {
        if (w == null) {
            return;
        }
        try {
            w.close();
        } catch (IOException e) {
            ZimbraLog.misc.debug("ignoring exception while closing writer", e);
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
	    if (data == null || data.length < 2) {
	        return false;
	    }
	    int byte1 = data[0];
	    int byte2 = data[1] & 0xff; // Remove sign, since bytes are signed in Java.
	    return (byte1 | (byte2 << 8)) == GZIPInputStream.GZIP_MAGIC;
	}
	
	/**
	 * Determines if the data in the given stream is gzipped.
	 * Requires that the <tt>InputStream</tt> supports mark/reset.
	 */
	public static boolean isGzipped(InputStream in)
	throws IOException {
        in.mark(2);
        int header = in.read() | (in.read() << 8);
        in.reset();
        if (header == GZIPInputStream.GZIP_MAGIC) {
            return true;
        }
        return false;
	}
	
	/**
	 * Returns the length of the data returned by an <tt>InputStream</tt>
	 * Reads the stream in its entirety and closes the stream when done reading.
	 */
	public static long getDataLength(InputStream in)
	throws IOException {
	    byte[] buf = new byte[8192];
	    int dataLength = 0;
	    int bytesRead = 0;
	    try {
	        while ((bytesRead = in.read(buf)) >= 0) {
	            dataLength += bytesRead;
	        }
	    } finally {
	        closeStream(in);
	    }
	    
	    return dataLength;
	}
	
    public static String encodeFSSafeBase64(byte[] data) {
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
	 * Returns the SHA1 digest of the supplied data.
	 * @param data data to digest
	 * @param base64 if <tt>true</tt>, return as base64 String, otherwise return
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
     * Reads the given <tt>InputStream</tt> in its entirety, closes
     * the stream, and returns the SHA1 digest of the read data.
     * @param in data to digest
     * @param base64 if <tt>true</tt>, returns as base64 String, otherwise return
     *  as hex string.
     * @return
     */
	public static String getSHA1Digest(InputStream in, boolean base64)
	throws IOException {
	    try {
	        MessageDigest md = MessageDigest.getInstance("SHA1");
	        byte[] buffer = new byte[1024];
	        int numBytes;
	        while ((numBytes = in.read(buffer)) >= 0) {
	            md.update(buffer, 0, numBytes);
	        }
	        byte[] digest = md.digest();
	        if (base64)
	            return encodeFSSafeBase64(digest);
	        else
	            return new String(Hex.encodeHex(digest));
        } catch (NoSuchAlgorithmException e) {
            // this should never happen unless the JDK is foobar
            //  e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            ByteUtil.closeStream(in);
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
	 * Returns the SHA1 digest for the given data, encoded
	 * as base64.
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

    private static final int SKIP_BUFFER_SIZE = 4096;
    private static byte[] skipBuffer;

    public static long skip(InputStream is, long n) throws IOException {
        if (n <= 0)
            return 0;

        if (skipBuffer == null)
            skipBuffer = new byte[SKIP_BUFFER_SIZE];
        byte[] localSkipBuffer = skipBuffer;

        long remaining = n;
        while (remaining > 0) {
            int nr = is.read(localSkipBuffer, 0, (int) Math.min(SKIP_BUFFER_SIZE, remaining));
            if (nr < 0)
                break;
            remaining -= nr;
        }
        return n - remaining;
    }

    /**
     * Copies an input stream fully to output stream.
     * @param in the <tt>InputStream</tt>
     * @param closeIn if <tt>true</tt>, the <tt>InputStream</tt> is closed before returning, even
     *                when there is an error.
     * @param out the <tt>OutputStream</tt>
     * @param closeOut if <tt>true</tt>, the <tt>OutputStream</tt> is closed before returning, even
     *                 when there is an error.
     * @return the number of bytes copied
     * @throws IOException
     */
    public static long copy(InputStream in, boolean closeIn, OutputStream out, boolean closeOut) throws IOException {
        return copy(in, closeIn, out, closeOut, -1L);
    }

    /**
     * Copies an input stream fully to output stream.
     * @param in the <tt>InputStream</tt>
     * @param closeIn if <tt>true</tt>, the <tt>InputStream</tt> is closed before returning, even
     *                when there is an error.
     * @param out the <tt>OutputStream</tt>
     * @param closeOut if <tt>true</tt>, the <tt>OutputStream</tt> is closed before returning, even
     *                 when there is an error.
     * @param maxLength maximum number of bytes to copy
     * @return the number of bytes copied
     * @throws IOException
     */
    public static long copy(InputStream in, boolean closeIn, OutputStream out, boolean closeOut, long maxLength) throws IOException {
        try {
            long transferred = 0;
            if (maxLength != 0) {
                byte buffer[] = new byte[8192];
                int numRead;
                do {
                    int readMax = buffer.length;
                    if (maxLength > 0 && maxLength - transferred < readMax)
                        readMax = (int) (maxLength - transferred);

                    if ((numRead = in.read(buffer, 0, readMax)) < 0)
                        break;
                    out.write(buffer, 0, numRead);
                    transferred += numRead;
                } while (maxLength < 0 || transferred < maxLength);
            }
            return transferred;
        } finally {
            if (closeIn)
                closeStream(in);
            if (closeOut)
                closeStream(out);
        }
    }
    
    /**
     * Reads up to <tt>limit</tt> bytes from the <tt>InputStream</tt>.
     * @param in the data stream
     * @param sizeHint used to allocate the byte array that is returned.  Used for
     *   optimizing memory allocation.  Does not affect the behavior of this method.
     *   If unknown, set this value to <tt>0</tt>.
     * @param limit maximum number of bytes to read
     */
    public static byte[] readInput(InputStream in, int sizeHint, int limit)
    throws IOException {
        if (limit <= 0) {
            return new byte[0];
        }
        if (sizeHint > limit) {
            sizeHint = limit;
        }
        byte[] data = null;
        int bytesRead = 0; // Index of next insertion and also the length of data read
        
        if (sizeHint > 0) {
            // Size hint is specified.  Try reading directly into the byte array.
            // If the size hint is correct, we avoid the extra arraycopy calls that
            // ByteArrayOutputStream would do.
            data = new byte[sizeHint];

            // Read until we've hit the end of the stream or filled the byte array.
            while (true) {
                int numToRead = data.length - bytesRead;
                if (numToRead <= 0) {
                    break;
                }
                int numRead = in.read(data, bytesRead, numToRead);
                if (numRead < 0) {
                    break;
                }
                bytesRead += numRead;
            }
            
            if (bytesRead >= limit) {
                return data;
            }

            if (bytesRead < data.length) {
                // Size hint was too big.
                byte[] truncated = new byte[bytesRead];
                System.arraycopy(data, 0, truncated, 0, bytesRead);
                return truncated;
            }
        }
        
        // See if there's more data available.  Note that we use read() instead
        // of available() because available() will sometimes return 0 when there's
        // actually more data to read.
        int oneMoreByte = in.read();
        if (oneMoreByte < 0) {
            // No more data to read.  Return what we've already read.
            if (data == null) {
                return new byte[0];
            } else {
                return data;
            }
        } else {
            bytesRead++;
        }
        
        // Size hint was 0 or low.  Read the remaining data into a ByteArrayOutputStream.
        ByteArrayOutputStream buf = null;
        if (data != null) {
            buf = new ByteArrayOutputStream(data.length * 2);
            buf.write(data);
        } else {
            buf = new ByteArrayOutputStream(1024);
        }
        buf.write((byte) oneMoreByte);
        
        byte[] chunk = new byte[1024];
        while (true) {
            int numToRead = Math.min(limit - bytesRead, chunk.length);
            if (numToRead <= 0) {
                break;
            }
            int numRead = in.read(chunk, 0, numToRead);
            if (numRead < 0) {
                break;
            }
            buf.write(chunk, 0, numRead);
            bytesRead += numRead;
        }
        return buf.toByteArray();
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
        } else {
            out.writeInt(0);
        }
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
        private OutputStream stream1, stream2;

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

    /**
     * Calculates the number of bytes read from the wrapped stream.
     * 
     * @see PositionInputStream#getPosition()
     */
    public static class PositionInputStream extends FilterInputStream {
        private long position = 0, mark = 0;

        public PositionInputStream(InputStream is) {
            super(is);
        }

        @Override public int read() throws IOException {
            int c = super.read();
            if (c != -1)
                position++;
            return c;
        }

        @Override public int read(byte[] b, int off, int len) throws IOException {
            int count = super.read(b, off, len);
            position += count;
            return count;
        }

        @Override public synchronized void mark(int readlimit) {
            super.mark(readlimit);
            mark = position;
        }

        @Override public synchronized void reset() throws IOException {
            super.reset();
            position = mark;
        }

        @Override public long skip(long n) throws IOException {
            long delta = super.skip(n);
            position += delta;
            return delta;
        }

        /**
         * Returns the number of bytes read from the wrapped stream.
         */
        public long getPosition() {
            return position;
        }
    }

    public static class SegmentInputStream extends PositionInputStream {
        private final long mLimit;

        public static SegmentInputStream create(InputStream is, long start, long end) throws IOException {
            if (start > 0) {
                long numSkipped = is.skip(start);
                if (numSkipped != start) {
                    throw new IOException("Attempted to skip " + start + " bytes, actually skipped " + numSkipped);
                }
            }
            return new SegmentInputStream(is, Math.max(0L, end - start));
        }

        public SegmentInputStream(InputStream is, long limit) {
            super(is);  mLimit = limit;
        }

        private long actualAvailable() {
            return mLimit - getPosition();
        }

        @Override public int available() {
            return (int) Math.min(actualAvailable(), Integer.MAX_VALUE);
        }

        @Override public int read() throws IOException {
            return available() <= 0 ? -1 : super.read();
        }

        @Override public int read(byte[] b, int off, int len) throws IOException {
            return available() <= 0 ? -1 : super.read(b, off, Math.min(len, available()));
        }

        @Override public long skip(long n) throws IOException {
            return super.skip(Math.max(Math.min(n, actualAvailable()), 0L));
        }
    }
}
