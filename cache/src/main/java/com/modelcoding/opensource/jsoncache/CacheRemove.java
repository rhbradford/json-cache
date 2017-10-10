// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A CacheRemove represents removal of a {@link CacheObject} from a {@link JsonCache}.
 * <p>
 * A CacheRemove is <em>immutable</em> and must have some {@link #getContent()}.    
 */
public interface CacheRemove {

    /**
     * @return the identity of a CacheObject in a {@link JsonCache}.
     */
    String getId();

    /**
     * A hook to allow for cache changing strategies that compare changes against existing objects
     * - see {@link CacheChangeCalculator}. 
     * 
     * @return the details of this CacheRemove as JSON.<br>
     *         <em>The return must not expose this CacheRemove to mutation.</em>
     */
    JsonNode getContent();

    /**
     * CacheRemoves are always considered equal if they have the same {@link #getId()}. 
     * 
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if the obj is also a CacheRemove with the same {@link #getId()}; 
     *          {@code false} otherwise.
     * @see     #hashCode()
     */
    boolean equals(Object obj);
   
   /** 
    * @return  a hash code value for this CacheRemove which must be based solely on {@link #getId()}.
    * @see     #equals(java.lang.Object)
    */
   int hashCode();
}
