// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache.messages;

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
    
    /**
     * {@link EndOfCacheChangeSet} are considered equal if they have the same {@link #getId()}. 
     * 
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if the obj is also a {@link EndOfCacheChangeSet} with the same {@link #getId()}; 
     *          {@code false} otherwise.
     * @see     #hashCode()
     */
    boolean equals(Object obj);
   
   /** 
    * @return  a hash code value for this {@link StartOfCacheChangeSet} which must be based on {@link #getId()}.
    * @see     #equals(java.lang.Object)
    */
   int hashCode();
}
