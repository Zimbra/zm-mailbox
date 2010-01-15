/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.zimbra.cs.util.BuildInfo;

public class FileGenUtil {
    

    private static final String BEGIN_MARKER = "BEGIN-AUTO-GEN-REPLACE";

    private static final String END_MARKER  = "END-AUTO-GEN-REPLACE";
    
    public static String wrapComments(String comments, int maxLineLength, String prefix) {
        comments = comments.trim().replaceAll("\\s+", " ");
        StringBuilder result = new StringBuilder();
        String[] words = comments.split("\\s+");
        int lineLength = 0;
        for (String word : words) {
            if (lineLength + word.length() + 1> maxLineLength) {
                result.append("\n");
                lineLength = 0;
            }
            if (lineLength == 0 && prefix != null) result.append(prefix);
            if (lineLength > 0) { result.append(' '); lineLength++; }
            result.append(word);
            lineLength += word.length();
        }
        if (result.length() == 0 && prefix != null)
            result.append(prefix);
        return result.toString();
    }

    
    public static void replaceJavaFile(String javaFile, String content) throws IOException {
       BufferedReader in = null;
       BufferedWriter out = null;

       File oldFile = new File(javaFile);
       if (!oldFile.canWrite()) {
           System.err.println("============================================");
           System.err.println("Unable to write to: "+javaFile);
           System.err.println("============================================");
           System.exit(1);
       }

       File newFile = new File(javaFile+"-autogen");

       try {
           out = new BufferedWriter(new FileWriter(newFile));
           in = new BufferedReader(new FileReader(oldFile));
           String line;
           boolean replaceMode = false;

           while((line = in.readLine()) != null) {
               if (line.indexOf(BEGIN_MARKER) != -1) {
                   out.write(line);
                   out.newLine();
                   out.newLine();
                   out.write(String.format("    /* build: %s */", BuildInfo.FULL_VERSION));
                   out.newLine();
                   out.write(content);
                   out.newLine();
                   replaceMode = true;
               } else if (line.indexOf(END_MARKER) != -1) {
                   replaceMode = false;
                   out.write(line);
                   out.newLine();
               } else if (!replaceMode){
                   out.write(line);
                   out.newLine();
               }
           }

           in.close();
           in = null;

           out.close();
           out = null;

           if (!newFile.renameTo(oldFile)) {
               System.err.println("============================================");
               System.err.format("Unable to rename(%s) to (%s)%n", newFile.getName(), oldFile);
               System.err.println("============================================");
               System.exit(1);
           }

           System.out.println("======================================");
           System.out.println("generated: "+javaFile);
           System.out.println("======================================");

       } finally {
           if (in != null) in.close();
           if (out != null) out.close();
       }
   }

}
