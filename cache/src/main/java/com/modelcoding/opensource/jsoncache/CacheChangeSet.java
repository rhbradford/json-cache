// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import java.util.List;

/**
 * Describes changes to be made/that were made to a {@link JsonCache}, in the form of a list of "put" and "remove"
 * operations.
 */
public interface CacheChangeSet {

    List<CacheObject> getPuts();

    List<CacheLocation> getRemoves();

    CacheChangeSet changeSetFrom(List<CacheObject> puts, List<CacheLocation> removes);
}
