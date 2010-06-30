/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.cs.util.tnef.mapi;

import java.io.IOException;
import net.freeutils.tnef.RawInputStream;
import net.freeutils.tnef.TNEFUtils;

/**
 * From MS-OXCICAL on the value of the UID property :
 * 
 *    UID = EncodedGlobalId / ThirdPartyGlobalId
 *    EncodedGlobalId         = Header GlobalIdData
 *    ThirdPartyGlobalId      = 1*UTF8-octets      ; Assuming UTF-8 is the encoding
 *    Header = ByteArrayID InstanceDate CreationDateTime Padding DataSize
 *    ByteArrayID           =  "040000008200E00074C5B7101A82E008"
 *    InstanceDate          =  InstanceYear InstanceMonth InstanceDay
 *    InstanceYear          =  4*4HEXDIG     ; UInt16
 *    InstanceMonth         =  2*2HEXDIG      ; UInt8
 *    InstanceDay           =  2*2HEXDIG     ; UInt8
 *    CreationDateTime      =  FileTime
 *    FileTime              =  16*16HEXDIG      ; UInt64
 *    Padding               =  16*16HEXDIG      ; "0000000000000000" recommended
 *    DataSize              =  8*8HEXDIG      ; UInt32 little-endian
 *    GlobalIdData          =  2*HEXDIG     ; [Should this be = * (2*HEXDIG)?]
 *
 * Effectively, an "EncodedGlobalId" is a direct encoding of the PidLidGlobalObjectId
 * property to hex.  The analog PidLidGlobalObjectId property of a "ThirdPartyGlobalId"
 * contains the full "ThirdPartyGlobalId" inside it.
 * 
 * Information from MS-OXCOCAL on PidLidGlobalObjectId structure :
 *    byte[16]             ByteArrayID = 0x04, 0x00, 0x00, 0x00,
 *                                       0x82, 0x00, 0xE0, 0x00,
 *                                       0x74, 0xC5, 0xB7, 0x10,
 *                                       0x1A, 0x82, 0xE0, 0x08
 *    uint16               InstanceYear (big endian!)
 *    uint8                InstanceMonth - between 1 and 12
 *    uint8                InstanceDay
 *    FileTime (uint64)    CreationDateTime
 *    byte[8]              Reserved - should be zeros
 *    uint32               Size (little endian)
 *    byte[Size]           Data
 */

public class GlobalObjectId {

    private static final byte[] thirdPartyWaterMark = 
            // v     C     a     l     -     U     i     d
            {0x76, 0x43, 0x61, 0x6C, 0x2D, 0x55, 0x69, 0x64, 0x01, 0x00, 0x00, 0x00};
    private String icalUid;

    private int origInstanceYear;
    private int origInstanceMonth;
    private int origInstanceDay;

    /**
     * Constructs a GlobalObjectId from a raw stream
     *
     * @param ris
     * @throws IOException if the stream end is reached,
     *         or if an I/O error occurs
     */
    public GlobalObjectId(RawInputStream ris) throws IOException {
        byte[] allData = ris.toByteArray();
        RawInputStream risCopy = new RawInputStream(allData);
        risCopy.readBytes(16);   // ByteArrayID
        setOrigInstanceYear(risCopy);
        setOrigInstanceMonth(risCopy);
        setOrigInstanceDay(risCopy);
        risCopy.readBytes(8);   // CreationDateTime - a FileTime uint64
        risCopy.readBytes(8);   // Reserved - MS-OXCICAL recommends setting to zeros if creating
        long dataSize = risCopy.readU32();
        boolean isWrappedIcalUid = false;
        int cnt;
        if (dataSize > thirdPartyWaterMark.length) {
            isWrappedIcalUid = true;
            byte [] testWaterMark= risCopy.readBytes(thirdPartyWaterMark.length);
            for (cnt = 0; cnt < thirdPartyWaterMark.length; cnt++) {
                if (thirdPartyWaterMark[cnt] != testWaterMark[cnt]) {
                    isWrappedIcalUid = false;
                    break;
                }
            }
        }
        if (isWrappedIcalUid) {
            Long uidLen = new Long (dataSize - thirdPartyWaterMark.length);
            byte [] icalUidBytes = risCopy.readBytes(uidLen.intValue());
            icalUid = new String(icalUidBytes, "UTF8");
            icalUid = TNEFUtils.removeTerminatingNulls(icalUid);
        } else {
            //  Ensure that date fields are zeroed
            for (cnt = 16;cnt <=19; cnt++) {
                allData[cnt] = 0;
            }
            StringBuffer s = new StringBuffer();
            for (cnt = 0; cnt < allData.length; cnt++) {
                String b = Integer.toHexString(allData[cnt] & 0xFF).toUpperCase();
                if (b.length() == 1)
                    s.append('0');
                s.append(b);
            }
            icalUid = s.toString();
        }
    }
 
    /**
     * 
     * @return the value to use for the ICALENDAR UID property
     */
    public String getIcalUid() {
        return icalUid;
    }

    /**
     * @return the origInstanceYear
     */
    public int getOrigInstanceYear() {
        return origInstanceYear;
    }

    /**
     * @return the origInstanceMonth
     */
    public int getOrigInstanceMonth() {
        return origInstanceMonth;
    }

    /**
     * @return the origInstanceDay
     */
    public int getOrigInstanceDay() {
        return origInstanceDay;
    }

    /**
     * @param ris - the stream currently being processed
     * @throws IOException 
     */
    private void setOrigInstanceYear(RawInputStream ris) throws IOException {
        // Note: if non-zero is BIG-ENDIAN!
        int firstByte = ris.readU8();
        int secondByte = ris.readU8();
        this.origInstanceYear = (firstByte << 8) + secondByte;
    }

    /**
     * @param ris - the stream currently being processed
     * @throws IOException 
     */
    private void setOrigInstanceMonth(RawInputStream ris) throws IOException {
        this.origInstanceMonth = ris.readU8();
    }

    /**
     * @param ris - the stream currently being processed
     * @throws IOException 
     */
    private void setOrigInstanceDay(RawInputStream ris) throws IOException {
        this.origInstanceDay = ris.readU8();
    }

}
