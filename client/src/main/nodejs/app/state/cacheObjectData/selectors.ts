// Author: Richard Bradford

import {State} from "./reducers"
import {FlattenedCacheObject, FlattenedCacheObjectColumn} from "./types"

const cacheObjectTypes = (state: State): Array<string> => {
    
    return state.get("cacheObjectTypes")
}

const cacheObjectsForType = (state: State, type: string): Array<FlattenedCacheObject> => {
    
    return state.get("cacheObjectsByType").get(type)
}

const cacheObjectColumnsForType = (state: State, type: string): Array<FlattenedCacheObjectColumn> => {
    
    return state.get("cacheObjectColumnsByType").get(type)
}

export default {
    
    cacheObjectTypes,
    cacheObjectsForType,
    cacheObjectColumnsForType
}