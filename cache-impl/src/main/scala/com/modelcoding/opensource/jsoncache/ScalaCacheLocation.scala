// Author: Richard Bradford

package com.modelcoding.opensource.jsoncache

case class ScalaCacheLocation(getCacheObjectId: String) extends CacheLocation {
  
  require(getCacheObjectId != null, "A CacheLocation cannot have a null cacheObjectId")
}
