// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import java.util.Set;

/**
 * A Cache is contained inside a {@link JsonCache}. It is an <em>immutable</em> set of {@link CacheObject}s.
 */
public interface Cache {

    /**
     * @param cacheObjectId the identity of a {@link CacheObject} that may be contained in this Cache.
     * @return {@code true} if a {@link CacheObject} with the given identity exists in this Cache, {@code false} otherwise.
     */
    boolean containsCacheObject(String cacheObjectId);

    /**
     * @param cacheObjectId the identity of a {@link CacheObject} that may be contained in this Cache.
     * @return the contained {@link CacheObject} if one is present in this Cache with the given {@code cacheObjectId},<br>
     *         otherwise an exception is thrown.
     * @throws IllegalArgumentException if no {@link CacheObject} is present in this Cache with the given {@code cacheObjectId}.   
     * @see #containsCacheObject(String)
     */
    CacheObject getCacheObject(String cacheObjectId);

    /**
     * @param put a new {@link CacheObject} to be contained in a Cache.
     * @return a new Cache with the same content as this Cache except it is guaranteed to contain the given {@code put}.<br>
     *         If an object with the same identity is already in this Cache, then the object is replaced with the 
     *         given {@code put} in the new Cache that is returned.
     */
    Cache put(PutObject put);

    /**
     * @param remove provides the identity of a {@link CacheObject} that may be contained in this Cache.
     * @return a new Cache with the same content as this Cache except it is guaranteed to contain no {@link CacheObject} 
     *         with the identity given by {@code remove}.<br>
     *         If no object with the identity given by {@code remove} is in this Cache, then this Cache is simply returned as is.
     */
    Cache remove(RemoveObject remove);

    /**
     * @return the contents of this Cache.<br>
     *         <em>The return must not expose this Cache to mutation.</em>
     */
    Set<? extends CacheObject> getObjects();
}
