package org.literacybridge.acm.store;

import org.literacybridge.acm.store.MetadataStore.Transaction;

public interface Persistable {
    <T extends Transaction> void commitTransaction(T t);
}
