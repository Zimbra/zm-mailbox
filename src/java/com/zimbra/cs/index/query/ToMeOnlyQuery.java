package com.zimbra.cs.index.query;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.DBQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.util.AccountUtil;


public class ToMeOnlyQuery extends Query {

    @Override
    void dump(StringBuilder out) {
        out.append("TO_ME_ONLY");

    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool)
            throws ServiceException {
        DBQueryOperation op = new DBQueryOperation();
        Account acct = mbox.getAccount();
        for (String addr: AccountUtil.getEmailAddresses(acct)) {
            ParsedAddress parsed = new ParsedAddress(addr).parse();
            op.addRecipient(parsed.getSortString(), evalBool(bool));
        }
        return op;
    }

    @Override
    public boolean hasTextOperation() {
        return false;
    }

}
