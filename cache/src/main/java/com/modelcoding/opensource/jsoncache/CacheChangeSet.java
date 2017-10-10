// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import java.util.Set;

/**
 * A CacheChangeSet describes changes to be made/that were made to a {@link JsonCache}, in the form of "put" and "remove" 
 * operations.
 * <p>
 * A CacheChangeSet is <em>immutable</em>.    
 */
public interface CacheChangeSet {

    /**
     * @return a set of objects that are to be/were added to a {@link JsonCache}.<br>
     *         <em>The return must not expose this CacheChangeSet to mutation.</em>
     */
    Set<? extends CacheObject> getPuts();

    /**
     * @return a set of cache removal operations that are to be/were applied to a {@link JsonCache}.<br>
     *         <em>The return must not expose this CacheChangeSet to mutation.</em>
     */
    Set<? extends CacheRemove> getRemoves();
    
    /**
     * CacheChangeSet are considered equal if they have the same {@link #getPuts()} and {@link #getRemoves()}. 
     * 
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if the obj is also a CacheChangeSet with the same {@link #getPuts()} and {@link #getRemoves()}; 
     *          {@code false} otherwise.
     * @see     #hashCode()
     */
    boolean equals(Object obj);
   
   /** 
    * @return  a hash code value for this CacheChangeSet which must be based solely on {@link #getPuts()} and {@link #getRemoves()}.
    * @see     #equals(java.lang.Object)
    */
   int hashCode();
}
