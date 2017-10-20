// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

/**
 * A {@link CacheChangeCalculator} calculates how a {@link JsonCache} is changed when a {@link CacheChangeSet} is applied - 
 * see {@link JsonCache#applyChanges(CacheChangeCalculator)}.
 * <p>
 * A {@link JsonCache} applies the given {@link #getChangeSet()} to its current {@link Cache} by calling 
 * {@link #calculateChange(Cache)}. This provides a new {@link Cache} to replace the current one -  
 * {@link ChangeResult#getCache()}. It also provides the details of the changes actually made for subsequent publication
 * - ({@link ChangeResult#getChangeSet()}).
 * <p>
 * An implementation of a {@link JsonCache} is therefore open to extension to different strategies for applying changes.
 * <p>
 * For example, it is possible to reduce the number of "puts" published for objects that already exist as exact copies 
 * in the {@link JsonCache} (as might occur when re-connecting two caches in different processes together).<br>
 * Alternatively, a {@link JsonCache} might exist to mediate changes between many clients. In such a system, each object 
 * in the {@link JsonCache} might contain its version number, such that the only change allowed to an existing object is
 * one containing the correct, expected version number for the object.<br>
 * In addition, a {@link CacheChangeCalculator} determines whether a change set is applied atomically (all changes succeed, 
 * or no changes are made), or whether changes can be applied individually (some changes can fail, where others succeed).
 * Finally, a {@link CacheChangeCalculator} can be used to provide persistence for a {@link JsonCache}. 
 */
public interface CacheChangeCalculator {

    /**
     * The results for {@link JsonCache#applyChanges(CacheChangeCalculator)}.
     */
    interface ChangeResult {

        /**
         * @return a new {@link Cache} to replace a previous {@link Cache}.
         */
        Cache getCache();

        /**
         * @return the changes actually made to create a new {@link Cache} from the previous {@link Cache}.<br>
         *         <em>The return is never a {@link CacheImage}</em>. 
         */
        CacheChangeSet getChangeSet();
    }

    /**
     * @return the changes contained in this {@link CacheChangeCalculator} to be processed against a {@link Cache} supplied 
     *         to {@link #calculateChange(Cache)}.
     */
    CacheChangeSet getChangeSet();

    /**
     * @param cache a {@link Cache} to be processed against the changes contained in this {@link CacheChangeCalculator} 
     *              given by {@link #getChangeSet()}. 
     * @return the results of processing the changes from {@link #getChangeSet()} against the given {@code cache} 
     *         according to the strategy encapsulated by this {@link CacheChangeCalculator}.
     * @throws NullPointerException if {@code cache} is {@code null}                  
     */
    ChangeResult calculateChange(Cache cache);
}
