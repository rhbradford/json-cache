// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

/**
 * A {@link Cache} is contained inside a {@link JsonCache}.
 * <p>
 * A {@link Cache} is an <em>immutable</em> set of {@link CacheObject}s.
 */
public interface Cache {

    /**
     * @param cacheObjectId the identity of a {@link CacheObject} that may be contained in this {@link Cache}.
     * @return {@code true} if a {@link CacheObject} with the given identity exists in this {@link Cache};
     *         {@code false} otherwise.
     * @throws NullPointerException if {@code cacheObjectId} is {@code null}                  
     */
    boolean containsCacheObject(String cacheObjectId);

    /**
     * @param cacheObjectId the identity of a {@link CacheObject} that may be contained in this {@link Cache}.
     * @return the contained {@link CacheObject} if one is present in this {@link Cache} with the given {@code cacheObjectId},<br>
     *         otherwise an exception is thrown.
     * @throws IllegalArgumentException if no {@link CacheObject} is present in this {@link Cache} with the given {@code cacheObjectId}.   
     * @throws NullPointerException if {@code cacheObjectId} is {@code null}                  
     * @see #containsCacheObject(String)
     */
    CacheObject getCacheObject(String cacheObjectId);

    /**
     * @param cacheObject a new {@link CacheObject} to be contained in a {@link Cache}.
     * @return a new {@link Cache} with the same content as this {@link Cache} except it is guaranteed to contain the given {@code cacheObject}.<br>
     *         If an object with the same identity is already in this {@link Cache}, then the object is replaced with the 
     *         given {@code cacheObject} in the new {@link Cache} that is returned.
     * @throws NullPointerException if {@code cacheObject} is {@code null}                  
     */
    Cache put(CacheObject cacheObject);

    /**
     * @param cacheRemove provides the identity of a {@link CacheObject} that may be contained in this {@link Cache}.
     * @return a new Cache with the same content as this {@link Cache} except it is guaranteed to contain no {@link CacheObject} 
     *         with the identity given by {@code cacheRemove}.<br>
     *         If no object with the identity given by {@code cacheRemove} is in this {@link Cache}, then this {@link Cache} is 
     *         simply returned as is.
     * @throws NullPointerException if {@code cacheRemove} is {@code null}                  
     */
    Cache remove(CacheRemove cacheRemove);

    /**
     * @return the contents of this {@link Cache} as a {@link CacheChangeSet} with each object in the {@link Cache} as a "put",
     *         and where {@link CacheChangeSet#isCacheImage()} is {@code true}.<br>
     *         <em>The return must not expose this {@link Cache} to mutation.</em>
     */
    CacheChangeSet getImage();
}
