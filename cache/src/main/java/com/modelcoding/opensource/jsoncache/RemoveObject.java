// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A RemoveObject represents removal of a {@link CacheObject} from a {@link JsonCache}.
 * <p>
 * A RemoveObject is <em>immutable</em> and must have some {@link #getContent()}.    
 */
public interface RemoveObject {

    /**
     * @return the location/identity of a CacheObject in a {@link JsonCache}.
     */
    String getId();

    /**
     * A hook to allow for cache changing strategies that compare changes against existing objects
     * - see {@link CacheChanger}. 
     * 
     * @return the details of this RemoveObject as JSON.<br>
     *         <em>The return must not expose this RemoveObject to mutation.</em>
     */
    JsonNode getContent();

    /**
     * RemoveObjects are always considered equal if they have the same {@link #getId()}. 
     * 
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if the obj is also a RemoveObject with the same {@link #getId()}; 
     *          {@code false} otherwise.
     * @see     #hashCode()
     */
    boolean equals(Object obj);
   
   /** 
    * @return  a hash code value for this RemoveObject which must be based solely on {@link #getId()}.
    * @see     #equals(java.lang.Object)
    */
   int hashCode();
}
