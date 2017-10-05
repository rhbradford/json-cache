// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

/**
 * Locates/identifies a {@link CacheObject} in a {@link JsonCache}.
 */
public interface CacheLocation {

    /**
     * @return the location/identity of a {@link CacheObject} in a {@link JsonCache}.
     */
    String getCacheObjectId();

    /**
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if the obj is also a CacheLocation with the same {@link #getCacheObjectId()}; 
     *          {@code false} otherwise.
     * @see     #hashCode()
     */
    boolean equals(Object obj);
   
   /** 
    * @return  a hash code value for this CacheLocation which must be based solely on {@link #getCacheObjectId()}.
    * @see     #equals(java.lang.Object)
    */
   int hashCode();
}
