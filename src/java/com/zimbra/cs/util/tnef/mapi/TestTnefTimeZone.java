package com.zimbra.cs.util.tnef.mapi;

import java.io.IOException;
import java.util.TimeZone;

import net.freeutils.tnef.RawInputStream;

import org.junit.Assert;
import org.junit.Test;

public class TestTnefTimeZone {

    @Test
    public void testTnefTimeZoneFromIntIndex() throws IOException {
        TimeZone tz = null;
        
        tz = TnefTimeZone.getTimeZone(13, true, null);
        Assert.assertFalse(tz == null);
        Assert.assertTrue(tz.getRawOffset() == -28800000);
        Assert.assertTrue(tz.getDSTSavings() == 3600000);
        
        tz = TnefTimeZone.getTimeZone(31, true, null);
        Assert.assertFalse(tz == null);
        Assert.assertTrue(tz.getRawOffset() == 0);
        Assert.assertTrue(tz.getDSTSavings() == 0);
        
        tz = TnefTimeZone.getTimeZone(60, true, null); //invalid index
        Assert.assertTrue(tz == null);
        
        tz = TnefTimeZone.getTimeZone(24, false, null);
        Assert.assertFalse(tz == null);
        Assert.assertTrue(tz.getID().equals("Asia/Dubai"));
    }
    

    @Test
    public void testLittleEndianByteArrayToIntConversions() {
        int value = 40067898;
        byte[] leByteArray = intToleByteArray(value);
        Assert.assertTrue(leByteArrayToInt(leByteArray) == value);
    }
    
    @Test
    public void testTnefTimeZoneFromInputStream() throws IOException {
        TimeZone tz = null;
        RawInputStream ris = null;
        
        ris = new RawInputStream(intToleByteArray(13), 0, 4);
        tz = TnefTimeZone.getTimeZone(ris);
        Assert.assertFalse(tz == null);
        Assert.assertTrue(tz.getRawOffset() == -28800000);
        Assert.assertTrue(tz.getDSTSavings() == 3600000);
        
        // don't observe daylight saving bit is set!!
        ris = new RawInputStream(new byte[]{13, 0, 0, (byte)128}, 0, 4);
        tz = TnefTimeZone.getTimeZone(ris);
        Assert.assertFalse(tz == null);
        Assert.assertTrue(tz.getRawOffset() == -28800000);
        Assert.assertTrue(tz.getID().equals("Etc/GMT+8"));
    }
    
    private static final byte[] intToleByteArray(int value) {
        return new byte[] {
                (byte)value,
                (byte)(value >>> 8),
                (byte)(value >>> 16),
                (byte)(value >>> 24)};
    }
    
    private static final int leByteArrayToInt(byte[] data) {
        return (data[3] << 24) | (data[2] << 16) | (data[1] << 8) | data[0];
    }
    
    public static void main(String args[]) {
        for (int i=0; i < 60; i++)
            try {
                System.out.println("Index " + i + ": " + TnefTimeZone.getTimeZone(i, true, null));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

}
