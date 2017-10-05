// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public interface JsonCache extends Publisher<CacheChangeSet> {

    String getCacheId();

    int getPublisherBacklogLimit();

    void applyChanges(CacheChanger cacheChanger);

    @Override
    void subscribe(Subscriber<? super CacheChangeSet> s);
}
