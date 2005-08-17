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
