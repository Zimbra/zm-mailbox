package com.zimbra.buildinfo;

public class Version {

    public static void main(String[] args) {
        Package p = Version.class.getPackage();
        System.out.println("Implementation-Title: "
                + p.getImplementationTitle() + "\nImplementation-Version: "
                + p.getImplementationVersion() + "\nImplementation-Vendor: "
                + p.getImplementationVendor() + "\n");
    }
}