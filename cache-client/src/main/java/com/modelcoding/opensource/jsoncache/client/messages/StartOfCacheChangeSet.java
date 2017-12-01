// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.client.messages;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.modelcoding.opensource.jsoncache.CacheChangeSet;
import com.modelcoding.opensource.jsoncache.CacheMessage;

public interface StartOfCacheChangeSet extends CacheMessage {

    /**
     * @return id obtained from {@link CacheChangeSet#getId()}
     */
    String getId();

    /**
     * 
     * @return indicator obtained from {@link CacheChangeSet#isCacheImage()}
     */
    boolean isCacheImage();

    /**
     * @return size of {@link CacheChangeSet#getPuts()}
     */
    int getNumPuts();

    /**
     * @return size of {@link CacheChangeSet#getRemoves()}
     */
    int getNumRemoves();
    
    /**
     * @return JSON of the form:
     * <pre>
     * {<br>
     *     "frame": "start",
     *     "id": {@link #getId()} 
     *     "isCacheImage" : {@link #isCacheImage()},<br>
     *     "numPuts" : size of {@link #getNumPuts()},<br>
     *     "numRemoves" : size of {@link #getNumRemoves()}<br>
     * }    
     * </pre>    
     */
    @Override
    ObjectNode asJsonNode();
}
