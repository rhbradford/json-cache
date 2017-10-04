// Author: richard
// Date:   06 Sep 2017

package com.modelcoding.opensource.jsoncache;

public interface CacheChanger {
    
    interface Reduction {
        
        Cache getCache();
        
        CacheChangeSet getChangeSet();
    }
    
    CacheChangeSet getChangeSet();
    
    Reduction reduce( Cache cache );
}
