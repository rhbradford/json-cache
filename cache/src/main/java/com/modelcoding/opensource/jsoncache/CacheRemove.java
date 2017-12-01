// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A {@link CacheRemove} represents removal of a {@link CacheObject} from a {@link JsonCache}.
 * <p>
 * A {@link CacheRemove} is <em>immutable</em>.    
 */
public interface CacheRemove extends CacheMessage {

    /**
     * @return the identity of a {@link CacheRemove} in a {@link JsonCache}.
     */
    String getId();

    /**
     * @return this {@link CacheRemove} as JSON of the form:
     * <pre>
     * {<br>
     *     "id" : {@link #getId()}<br>
     * }    
     * </pre>    
     */
    ObjectNode asJsonNode();

    /**
     * {@link CacheRemove}s are always considered equal if they have the same {@link #getId()}. 
     * 
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if the obj is also a {@link CacheRemove} with the same {@link #getId()}; 
     *          {@code false} otherwise.
     * @see     #hashCode()
     */
    boolean equals(Object obj);
   
   /** 
    * @return  a hash code value for this {@link CacheRemove} which must be based solely on {@link #getId()}.
    * @see     #equals(java.lang.Object)
    */
   int hashCode();
}
