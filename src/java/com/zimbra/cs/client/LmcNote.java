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

package com.zimbra.cs.client;

public class LmcNote {
    private String id;
    private String tags;
    private String date;
    private String folder;
    private String position;
    private String color;
    private String content;    

    public void setID(String i) { id = i; }
    public void setTags(String t) { tags = t; }
    public void setDate(String d) { date = d; }
    public void setFolder(String f) { folder = f; }
    public void setPosition(String p) { position = p; }
    public void setColor(String c) { color = c; }
    public void setContent(String c) { content = c; }

    public String getID() { return id; }
    public String getTags() { return tags; }
    public String getDate() { return date; }
    public String getFolder() { return folder; }
    public String getPosition() { return position; }
    public String getColor() { return color; }
    public String getContent() { return content; }
    
    public String toString() {
    	return "Note ID=" + id + " date=" + date + " tags=" + tags +
            " folder=" + folder + " position=" + position + 
            " color=" + color + " content=" + content;
    }
}
