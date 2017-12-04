// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages;

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
    
    /**
     * {@link StartOfCacheChangeSet} are considered equal if they have the same {@link #getNumPuts()} and {@link #getNumRemoves()}, 
     * and the same {@link #getId()} and {@link #isCacheImage()}. 
     * 
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if the obj is also a {@link CacheChangeSet} with the same {@link #getNumPuts()} and {@link #getNumRemoves()}
     *          and the same {@link #getId()} and {@link #isCacheImage()}; 
     *          {@code false} otherwise.
     * @see     #hashCode()
     */
    boolean equals(Object obj);
   
   /** 
    * @return  a hash code value for this {@link StartOfCacheChangeSet} which must be based on {@link #getNumPuts()}, {@link #getNumRemoves()},
    *          {@link #getId()} and {@link #isCacheImage()}.
    * @see     #equals(java.lang.Object)
    */
   int hashCode();
}
