// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import java.util.List;

public interface CacheChangeSet {

    List<CacheObject> getPuts();

    List<CacheLocation> getRemoves();

    CacheChangeSet changeSetFrom(List<CacheObject> puts, List<CacheLocation> removes);
}
