// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import java.util.Set;

/**
 * A {@link CacheImage} is a special form of {@link CacheChangeSet} representing the contents of a {@link JsonCache}.<br>
 * A {@link CacheImage} contains no "removes", only "puts", one for each {@link CacheObject} in the {@link JsonCache}.    
 * <p>
 * A {@link CacheImage} is <em>immutable</em>.    
 */
public interface CacheImage extends CacheChangeSet {

    /**
     * @return a set of objects that are the contents of a {@link JsonCache}.<br>
     *         <em>The return must not expose this {@link CacheImage} to mutation.</em>
     */
    Set<? extends CacheObject> getPuts();

    /**
     * @return always returns an empty set of cache removal operations.<br>
     *         <em>The return must not expose this {@link CacheImage} to mutation.</em>
     */
    Set<? extends CacheRemove> getRemoves();
    
    /**
     * {@link CacheImage}s are considered equal if they have the same {@link #getPuts()} and {@link #getRemoves()}. 
     * 
     * @param   obj   the reference object with which to compare.
     * @return  {@code true} if the obj is also a {@link CacheImage} with the same {@link #getPuts()} and {@link #getRemoves()}; 
     *          {@code false} otherwise.
     * @see     #hashCode()
     */
    boolean equals(Object obj);
}
