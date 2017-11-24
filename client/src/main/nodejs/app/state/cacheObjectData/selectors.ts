// Author: Richard Bradford

import {State} from "./reducers"
import {CacheObject, CacheObjectColumn, CacheObjectContentType} from "../../model/types"

const cacheObjectTypes = (state: State): Array<string> => {
    
    return state.get("cacheObjectTypes")
}

const cacheObjectContentType = (state: State, type: string): CacheObjectContentType => {
    
    return state.get("cacheObjectContentTypes").get(type)
}

const cacheObjects = (state: State, type: string): Array<CacheObject> => {
    
    return state.get("cacheObjectsByType").get(type)
}

const cacheObjectColumns = (state: State, type: string): Array<CacheObjectColumn> => {
    
    return state.get("cacheObjectColumnsByType").get(type)
}

export default {
    
    cacheObjectTypes,
    cacheObjectContentType,
    cacheObjects,
    cacheObjectColumns
}