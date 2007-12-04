/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;

import junit.framework.TestCase;

public class TestFileUtil extends TestCase {

    private File genFile(String path, int numBytes) throws Exception {
        File file = new File(path);
        FileOutputStream fos = new FileOutputStream(file);
        
        byte[] bytes = new byte[numBytes];
        int offset = 0;
        for (int i=0; i<numBytes; i++) {
            byte[] src = (String.valueOf(i) + "\n").getBytes();
            if (offset+src.length < numBytes) {
                System.arraycopy(src, 0, bytes, offset, src.length);
                offset += src.length;
            } else {
                // pad remaining bytes with *
                while (offset < numBytes)
                    bytes[offset++] = '*';
                break;
            }
        }  
        
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ByteUtil.copy(bais, true, fos, true);
        return file;      
    }
    
    private void printFile(File file) throws Exception {
        InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
        BufferedReader reader = new BufferedReader(isr);
        String line;
        while ((line = reader.readLine())!=null ) {
            System.out.println(line);    
        }
    }
    
    private boolean isGzip(File file) throws Exception {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        int header = is.read() | (is.read() << 8);
        is.close();
        if (header == GZIPInputStream.GZIP_MAGIC)
            return true;
        else
            return false;
    }

    
    public void testCompress() throws Exception {
        File orig = genFile("/tmp/junk.txt", 1024);
        // printFile(orig);
        
        // compress it
        File compressed = new File("/tmp/junk.compressed");
        FileUtil.compress(orig, compressed, true);
        byte[] compressedBytes = ByteUtil.getContent(compressed);
        assertTrue(isGzip(compressed));
        
        // uncompress it
        File uncompressed = new File("/tmp/junk.uncompressed");
        FileUtil.uncompress(compressed, uncompressed, true);
        
        // uncompressed file shpuld be identical to the original file
        byte[] origBytes = ByteUtil.getContent(orig);
        byte[] uncompressedBytes = ByteUtil.getContent(uncompressed);
        assertTrue(Arrays.equals(origBytes, uncompressedBytes));
    }
}