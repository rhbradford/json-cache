// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A CacheObject represents a JSON "object" in a {@link JsonCache}.
 * <p>
 * A CacheObject is <em>immutable</em> and must have:<br>
 *     <ul>
 *         <li>a unique location/identity in the cache: {@link #getId()}</li>
 *         <li>a type: {@link #getType()}</li>
 *         <li>some JSON content: {@link #getContent()}</li>
 *     </ul><br>    
 */
public interface CacheObject {

    /**
     * @return the location/identity of a CacheObject in a {@link JsonCache}.
     */
    String getId();
    
    /**
     * @return an indication of the type of this CacheObject, which can be used to interpret the JSON from 
     *         {@link #getContent()}.
     */
    String getType();

    /**
     * @return the content of this CacheObject as JSON.<br>
     *         <em>The return must not expose this CacheObject to mutation.</em>
     */
    JsonNode getContent();

    /**
     * CacheObjects are always considered equal if they have the same {@link #getId()}. 
     * 
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if the obj is also a CacheObject with the same {@link #getId()}; 
     *          {@code false} otherwise.
     * @see     #hashCode()
     */
    boolean equals(Object obj);
   
   /** 
    * @return  a hash code value for this CacheObject which must be based solely on {@link #getId()}.
    * @see     #equals(java.lang.Object)
    */
   int hashCode();
}