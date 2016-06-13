/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import java.util.HashMap;

public class LmcGetInfoResponse extends LmcSoapResponse {

    private String mAcctName;
    private String mLifetime;
    private HashMap mPrefMap;

    public HashMap getPrefMap() { return mPrefMap; }
    public String getLifetime() { return mLifetime; }
    public String getAcctName() { return mAcctName; }

    public void setPrefMap(HashMap p) { mPrefMap = p; }
    public void setLifetime(String l) { mLifetime = l; }
    public void setAcctName(String a) { mAcctName = a; }
}
