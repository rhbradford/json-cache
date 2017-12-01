// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client.messages;

import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import com.modelcoding.opensource.jsoncache.CacheMessage;
import com.modelcoding.opensource.jsoncache.CacheObject;
import com.modelcoding.opensource.jsoncache.CacheRemove;

import java.util.List;

public interface CacheChangeSetFrame {

    CacheChangeSet getCacheChangeSet();
    
    /**
     * @return a sequence of {@link CacheMessage}s representing {@link #getCacheChangeSet()} as follows:
     * <ul>
     *     <li>a {@link StartOfCacheChangeSet}</li>
     *     <li>one or more {@link CacheObject}s for the puts</li>
     *     <li>one or more {@link CacheRemove}s for the removes</li>
     *     <li>an {@link EndOfCacheChangeSet}</li>
     * </ul>    
     * 
     */
    List<CacheMessage> getMessages();
}
