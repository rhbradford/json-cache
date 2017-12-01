// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface CacheMessage {

    ObjectNode asJsonNode();
}
