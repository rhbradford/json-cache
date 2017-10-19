// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A {@link CacheObject} represents a "JSON object" in a {@link JsonCache}.<br>
 * A "JSON object" represents an instance of an entity that has a fixed identity and type, but whose content changes 
 * over time.    
 * <p>
 * A {@link CacheObject} is <em>immutable</em> and must have:<br>
 *     <ul>
 *         <li>a unique identity in the cache: {@link #getId()}</li>
 *         <li>a type: {@link #getType()}</li>
 *         <li>some JSON content: {@link #getContent()}</li>
 *     </ul>
 */
public interface CacheObject {

    /**
     * @return the identity of this {@link CacheObject} in a {@link JsonCache}.
     */
    String getId();
    
    /**
     * @return an indication of the type of this {@link CacheObject}, which can be used to interpret the JSON from 
     *         {@link #getContent()}.
     */
    String getType();

    /**
     * @return the content of this {@link CacheObject} as JSON.<br>
     *         <em>The return must not expose this {@link CacheObject} to mutation.</em>
     */
    JsonNode getContent();

    /**
     * {@link CacheObject}s are always considered equal if they have the same {@link #getId()}. 
     * 
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if the obj is also a {@link CacheObject} with the same {@link #getId()}; 
     *          {@code false} otherwise.
     * @see     #hashCode()
     */
    boolean equals(Object obj);
   
   /** 
    * @return  a hash code value for this {@link CacheObject} which must be based solely on {@link #getId()}.
    * @see     #equals(java.lang.Object)
    */
   int hashCode();
}
