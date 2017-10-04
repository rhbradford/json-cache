// Author: richard
// Date:   06 Sep 2017

package com.modelcoding.opensource.jsoncache;

import com.fasterxml.jackson.databind.JsonNode;

public interface CacheObject extends CacheLocation {
    
    String getCacheObjectType();
    
    JsonNode getCacheObjectContent();
}
