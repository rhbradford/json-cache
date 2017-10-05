// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a JSON "object" in a {@link JsonCache}.
 * <p>
 * A JSON "object" always has:<br>
 *     <ul>
 *         <li>a unique location/identity in the cache: {@link #getCacheObjectId()}</li>
 *         <li>a type: {@link #getCacheObjectType()}</li>
 *         <li>some JSON content: {@link #getCacheObjectContent()}</li>
 *     </ul><br>    
 * (Note that two CacheObjects are considered equal if they have the same location/identity 
 * - see {@link CacheLocation#equals(Object)}).    
 */
public interface CacheObject extends CacheLocation {

    /**
     * @return an indication of the type of this CacheObject, which can be used to interpret the JSON from 
     *         {@link #getCacheObjectContent()}.
     */
    String getCacheObjectType();

    /**
     * @return the content of this CacheObject as JSON.
     */
    JsonNode getCacheObjectContent();
}
