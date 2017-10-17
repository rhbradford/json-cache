// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.controlledclient;

import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import org.reactivestreams.Publisher;

public interface ControlledCacheChangeSetSource extends Publisher<CacheChangeSet> {

    String getId();
}
