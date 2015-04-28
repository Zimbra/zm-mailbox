package com.zimbra.cs.index;

import java.util.List;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeName;

import com.google.common.collect.Lists;


/**
 * Describes a queued "delete from index" task
 * @author Greg Solovyev
 *
 */
@JsonTypeName("DeleteFromIndexTaskLocator")
public class DeleteFromIndexTaskLocator extends AbstractIndexingTasksLocator {
    private final List<Integer> itemIds;
    
    public DeleteFromIndexTaskLocator(Integer itemId, String accountID, int mailboxID, int mailboxSchemaGroupID) {
        super(mailboxID, mailboxSchemaGroupID, accountID);
        itemIds = Lists.newArrayList();
        itemIds.add(itemId);
    }
    
    @JsonCreator
    public DeleteFromIndexTaskLocator(@JsonProperty("itemIds") List<Integer> itemIds, 
            @JsonProperty("accountID") String accountID, @JsonProperty("mailboxID") int mailboxID, 
            @JsonProperty("mailboxSchemaGroupID") int mailboxSchemaGroupID) {
        super(mailboxID, mailboxSchemaGroupID, accountID);
        this.itemIds = Lists.newArrayList();
        this.itemIds.addAll(itemIds);
    }
    
    public List<Integer> getItemIds() {
        return itemIds;
    }
}
