// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

/**
 * A Cache is contained inside a {@link JsonCache}.
 * <p>
 * A Cache is an <em>immutable</em> set of {@link CacheObject}s.
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
     * @param cacheObject a new {@link CacheObject} to be contained in a Cache.
     * @return a new Cache with the same content as this Cache except it is guaranteed to contain the given {@code cacheObject}.<br>
     *         If an object with the same identity is already in this Cache, then the object is replaced with the 
     *         given {@code cacheObject} in the new Cache that is returned.
     */
    Cache put(CacheObject cacheObject);

    /**
     * @param cacheRemove provides the identity of a {@link CacheObject} that may be contained in this Cache.
     * @return a new Cache with the same content as this Cache except it is guaranteed to contain no {@link CacheObject} 
     *         with the identity given by {@code cacheRemove}.<br>
     *         If no object with the identity given by {@code cacheRemove} is in this Cache, then this Cache is 
     *         simply returned as is.
     */
    Cache remove(CacheRemove cacheRemove);

    /**
     * @return the contents of this Cache as a {@link CacheChangeSet} with each object in the Cache as a "put"<br>
     *         ({@link CacheChangeSet#isCacheImage()} is {@code true} for the returned {@link CacheChangeSet}).<br>
     *         <em>The return must not expose this Cache to mutation.</em>
     */
    CacheChangeSet asChangeSet();
}
