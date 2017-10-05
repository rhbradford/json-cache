// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

public interface CacheChanger {

    interface Reduction {

        Cache getCache();

        CacheChangeSet getChangeSet();
    }

    CacheChangeSet getChangeSet();

    Reduction reduce(Cache cache);
}
