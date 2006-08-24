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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.util;

import java.security.SecureRandom;

public class RandomPassword {
    
    /**
     * 64 entry alphabet gives a 6 bits of entropy per character in the
     * password.
     *
     * http://world.std.com/~reinhold/dicewarefaq.html#calculatingentropy
     *
     * If the passphrase is made out of M symbols, each chosen at
     * random from a universe of N possibilities, each equally likely,
     * the entropy is M*log2(N).
     *
     */
    private static final String ALPHABET = 
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_.";
    
    private static final int DEFAULT_MIN_LENGTH = 24;
    
    private static final int DEFAULT_MAX_LENGTH = 32;
    
    /** 
     * http://world.std.com/~reinhold/passgen.html
     *
     * When using an 8-bit value to select a character from an
     * alphabet of length k, there is a risk of bias if k does not
     * evenly divide 256. To eliminate this, candidate cipher output
     * bytes are discarded if they are greater than or equal to the
     * largest multiple of k less than 256.
     */
    private static int byteLimit(int alphabetLength) {
        if (alphabetLength > 256) {
            // ie, some of the alphabet will never show up!
            throw new IllegalStateException
                ("alphabet length " + alphabetLength + " has risk of bias");
        }
        return 256 - (256 % alphabetLength);
    }
    
    /**
     * Generate a random password of random length.
     */
    public static String generate(int minLength, int maxLength) {
        SecureRandom random = new SecureRandom();

        // Calculate the desired length of the password
        int length;
        if (minLength > maxLength) {
            throw new IllegalArgumentException("minLength=" + minLength + 
                                               " > maxLength=" + maxLength);
        } else if (minLength < maxLength) {
            length = minLength + random.nextInt(1 + maxLength - minLength);
        } else {
            length = maxLength;
        }
        
        int alphabetLength = ALPHABET.length();
        int limit = byteLimit(alphabetLength);
        
        StringBuffer password = new StringBuffer(length);
        byte[] randomByte = new byte[1];
        
        while (password.length() < length) {
            random.nextBytes(randomByte);
            int i = randomByte[0] + 128;
            if (i < limit) {
                password.append(ALPHABET.charAt(i % alphabetLength));
            }
        }
        
        return password.toString();            
    }
    
    public static String generate() {
        return generate(DEFAULT_MIN_LENGTH, DEFAULT_MAX_LENGTH);
    }
    
    public static void main(String args[]) {
        int minLength = DEFAULT_MIN_LENGTH;
        int maxLength = DEFAULT_MAX_LENGTH;

        if (args.length != 0) {
            if (args.length != 2) {
                System.err.println("Usage: java " + RandomPassword.class.getName() + 
                        " <minLength> <maxLength>");
                System.exit(1);
            }
            try {
                minLength = Integer.valueOf(args[0]).intValue();
                maxLength = Integer.valueOf(args[1]).intValue();
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace();
            }
        }
        
        System.out.println(generate(minLength, maxLength));
    }
}
