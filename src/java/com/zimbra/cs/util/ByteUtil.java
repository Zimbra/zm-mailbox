/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on Apr 18, 2004
 */
package com.zimbra.cs.util;

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
			
			while ((num_left > 0) && ((num_read = is.read(buffer, total_read, num_left)) != -1)) {
				total_read += num_read;
				num_left -= num_read;
			}
		} finally {
			if (is != null) 
				is.close();
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
    
    /**
     * read all data from specified InputStream. InputStream
     * is closed.
     * @param is
     * @param sizeHint estimated size of content.
     * @return content from stream
     * @throws MessagingException
     * @throws IOException
     */
    public static byte[] getContent(InputStream is, int sizeHint) throws IOException {
        ByteArrayOutputStream baos = null;
    	try {
    		if (sizeHint < 0)
    			sizeHint = 0; 
    		baos = new ByteArrayOutputStream(sizeHint);
    		byte[] buffer = new byte[8192];
    		int num;
    		while ((num = is.read(buffer)) != -1) {
    			baos.write(buffer, 0, num);
    		}
    		return baos.toByteArray();
    	} finally {
    		is.close();
            if (baos != null)
                baos.close();
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
	 * by matching the first 2 bytes with GZIP magic number 0x1f, 0x8b.
	 * @param data
	 * @return
	 */
	public static boolean isGzipped(byte[] data) {
		if (data != null && data.length >= 2) {
			// convert unsigned bytes to shorts
			int first = data[0] & 0x00ff;
			int second = data[1] & 0x00ff;
			return first == 0x1f && second == 0x8b;
		}
		return false;
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
	        if (base64) {
	            byte[] encoded = Base64.encodeBase64(digest);
	            // Replace '/' with ',' to make the digest filesystem-safe.
	            for (int i = 0; i < encoded.length; i++) {
	            	if (encoded[i] == (byte) '/')
	            		encoded[i] = (byte) ',';
	            }
	            return new String(encoded);
	        } else 
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
	        if (base64) {
	            byte[] encoded = Base64.encodeBase64(digest);
	            // Replace '/' with ',' to make the digest filesystem-safe.
	            for (int i = 0; i < encoded.length; i++) {
	            	if (encoded[i] == (byte) '/')
	            		encoded[i] = (byte) ',';
	            }
	            return new String(encoded);
	        } else 
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
		byte[] bytes = digest.getBytes();
		// Undo the mapping done in getSHA1Digest/getMD5Digest
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] == (byte) ',')
				bytes[i] = (byte) '/';
		}
		return Base64.decodeBase64(bytes);
	}

	public static int copy(InputStream is, OutputStream os) throws IOException {
		byte buffer[] = new byte[8192];
		int numRead;
        int transferred = 0;
		while ((numRead = is.read(buffer)) >= 0) {
			os.write(buffer, 0, numRead);
            transferred += numRead;
		}
        return transferred;
	}

    // Custom read/writeUTF8 methods to replace DataInputStream.readUTF() and 
    // DataOutputStream.writeUTF() which have 64KB limit

    private static final int MAX_STRING_LEN = 32 * 1024 * 1024;     // 32MB

    public static void writeUTF8(DataOutput out, String str) throws IOException {
        int len = str.length();
        if (len > MAX_STRING_LEN)
            throw new IOException("String too long in RedoableOp.writeUTF8(); max=" + MAX_STRING_LEN);
        out.writeInt(len);
        if (len > 0) {
            byte[] buf = str.getBytes("UTF-8");
            out.write(buf);
        }
    }

    public static String readUTF8(DataInput in) throws IOException {
        int len = in.readInt();
        if (len > MAX_STRING_LEN)
            throw new IOException("String too long in RedoableOp.readUTF8(); max=" + MAX_STRING_LEN);
        if (len > 0) {
            byte[] buf = new byte[len];
            in.readFully(buf, 0, len);
            return new String(buf, "UTF-8");
        } else
            return "";
    }

    public static class TeeOutputStream extends OutputStream {
        OutputStream stream1, stream2;

        public TeeOutputStream(OutputStream one, OutputStream two) {
            if (one == two)
                two = null;
            stream1 = one;  stream2 = two;
        }

        public void write(int b) throws IOException {
            if (stream1 != null)  stream1.write(b);
            if (stream2 != null)  stream2.write(b);
        }

        public void flush() throws IOException {
            if (stream1 != null)  stream1.flush();
            if (stream2 != null)  stream2.flush();
        }

        public void write(byte b[], int off, int len) throws IOException {
            if (stream1 != null)  stream1.write(b, off, len);
            if (stream2 != null)  stream2.write(b, off, len);
        }
    }
}
