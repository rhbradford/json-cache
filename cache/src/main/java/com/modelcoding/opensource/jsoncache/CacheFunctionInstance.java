// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

/**
 * A {@link CacheFunctionInstance} contains an instance of a {@link CacheFunction} and associates the function with
 * an id.
 * <p>
 * When the {@link #getCode()} from a {@link CacheFunctionInstance} is executed against a {@link Cache} the 
 * {@link CacheChangeSet#getId()} of the changes produced matches the function instance {@link #getId()}.<br>
 * This makes it possible to trace a flow of changes.    
 */
public interface CacheFunctionInstance {

    /**
     * @return a unique id for this {@link CacheFunctionInstance} that will then be assigned to the {@link CacheChangeSet}
     *         produced when the {@link #getCode()} is executed.
     */
    String getId();

    /**
     * @return the instructions for producing a new {@link Cache} contained within this {@link CacheFunctionInstance}
     */
    CacheFunction getCode();
}
