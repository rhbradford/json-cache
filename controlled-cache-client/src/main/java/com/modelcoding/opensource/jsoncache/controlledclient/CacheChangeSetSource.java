// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.controlledclient;

import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import org.reactivestreams.Publisher;

public interface CacheChangeSetSource {
    
    interface Factory {
        
        Publisher<CacheChangeSet> getSource();
        
        CacheChangeSetSource getCacheChangeSetSource(RestartableSubscriber<? super CacheChangeSet> restartableSubscriber);
    }
    
    Publisher<CacheChangeSet> getSource();
    
    void restart();
}
