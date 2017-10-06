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
    Set<? extends PutObject> getPuts();

    /**
     * @return a set of objects that are to be/were removed from a {@link JsonCache}.<br>
     *         <em>The return must not expose this CacheChangeSet to mutation.</em>
     */
    Set<? extends RemoveObject> getRemoves();
}
