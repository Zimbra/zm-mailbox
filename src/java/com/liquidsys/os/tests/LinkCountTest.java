package com.zimbra.native.tests;

import java.io.IOException;

import com.zimbra.native.IO;

public class LinkCountTest {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Error: no arguments specified");
            return;
        }
        for (int i = 0; i < args.length; i++) {
            try {
                System.out.println(args[i] + ": " + IO.linkCount(args[i]));
            } catch (IOException ioe) {
                System.out.println(args[i] + ": " + ioe);
            }
        }
    }
}
