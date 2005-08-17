package com.liquidsys.coco.util;

import java.security.NoSuchAlgorithmException;
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
        SecureRandom random = null;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

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
