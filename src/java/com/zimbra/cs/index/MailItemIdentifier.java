package com.zimbra.cs.index;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeName;

import com.zimbra.cs.mailbox.MailItem;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@class")
@JsonTypeName("MailItemIdentifier")
public class MailItemIdentifier {
    private final MailItem.Type type;
    private final int id;
    private final boolean inDumpster;

    public MailItemIdentifier(int id) {
        this.id = id;
        this.type = MailItem.Type.UNKNOWN;
        this.inDumpster = false;
    }

    @JsonCreator
    public MailItemIdentifier(@JsonProperty("id") int id, @JsonProperty("type") MailItem.Type type, @JsonProperty("inDumpster") boolean inDumpster) {
        this.id = id;
        this.type = type;
        this.inDumpster = inDumpster;
    }

    public MailItemIdentifier(MailItem item) {
        this(item.getId(), item.getType(), item.inDumpster());
    }

    public int getId() {
        return id;
    }

    public MailItem.Type getType() {
        return type;
    }

    public boolean isInDumpster() {
        return inDumpster;
    }
}
