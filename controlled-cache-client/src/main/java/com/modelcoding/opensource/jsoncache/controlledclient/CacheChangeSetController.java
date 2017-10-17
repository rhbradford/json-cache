// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.controlledclient;

import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import org.reactivestreams.Publisher;

public interface CacheChangeSetController extends Publisher<CacheChangeSet> {

    void connect(CacheChangeSetSource source, Publisher<CacheChangeSet> input);
}
