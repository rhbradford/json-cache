// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import java.util.Map;

public interface Cache {

    Map<CacheLocation, CacheObject> asMap();

    boolean containsCacheObjectAt(CacheLocation cacheLocation);

    CacheObject getCacheObjectAt(CacheLocation cacheLocation);

    Cache put(CacheObject cacheObject);

    Cache remove(CacheLocation cacheLocation);
}
