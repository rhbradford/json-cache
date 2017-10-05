// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import com.fasterxml.jackson.databind.JsonNode;

public interface CacheObject extends CacheLocation {

    String getCacheObjectType();

    JsonNode getCacheObjectContent();
}
