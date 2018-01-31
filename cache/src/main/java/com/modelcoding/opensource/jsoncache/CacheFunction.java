// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

/**
 * A {@link CacheFunction} defines a manipulation of a {@link Cache} contained in a {@link JsonCache} - 
 * see {@link JsonCache#onNext(CacheFunctionInstance)}.
 * <p>
 * A {@link JsonCache} applies a {@link CacheFunction} to its current {@link Cache} by calling 
 * {@link #execute(Cache)} and receives a {@link Result}.<br>
 * The {@link Result} may provide a new {@link Cache} and a {@link CacheChangeSet} describing the difference between 
 * the {@link Cache}s.
 */
@FunctionalInterface
public interface CacheFunction {

    /**
     * Results of {@link CacheFunction#execute(Cache)} against the given {@link Cache}.
     */
    interface Result {

        /**
         * @return a new {@link Cache} (that can replace a previous {@link Cache}).
         */
        Cache getCache();

        /**
         * The {@link CacheChangeSet#getId()} of the returned changes must match {@link CacheFunctionInstance#getId()}
         * from the {@link CacheFunctionInstance} containing the {@link CacheFunction}.<br>
         * This makes it possible to track a flow of changes.
         * 
         * @return the changes actually made to create a new {@link Cache} from the previous {@link Cache}.<br>
         *         <em>The return should never have {@link CacheChangeSet#isCacheImage()} as {@code true}</em>. 
         */
        CacheChangeSet getChangeSet();
    }

    /**
     * @param cache a {@link Cache} to be processed using this {@link CacheFunction}. 
     * @return the results of processing this {@link CacheFunction} against the given {@code cache}.
     * @throws NullPointerException if {@code cache} is {@code null}                  
     */
    Result execute(Cache cache);
}
