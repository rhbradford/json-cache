// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client.messages;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import com.modelcoding.opensource.jsoncache.CacheMessage;

public interface EndOfCacheChangeSet extends CacheMessage {

    /**
     * @return id obtained from {@link CacheChangeSet#getId()}
     */
    String getId();

    /**
     * @return JSON of the form:
     * <pre>
     * {<br>
     *     "frame": "end",
     *     "id": {@link #getId()} 
     * }    
     * </pre>    
     */
    @Override
    ObjectNode asJsonNode();
}
