package com.zimbra.cs.mailbox;

public interface AdditionalQuotaProvider {
  long getAdditionalQuota(Mailbox mailbox);
}
