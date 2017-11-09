// Author: Richard Bradford

import {State} from "./reducers"
import {FlattenedCacheObject} from "./types"

const cacheObjectTypes = (state: State): Array<string> => {
    
    return state.cacheObjectTypes
}

const cacheObjectsForType = (state: State, type: string): Array<FlattenedCacheObject> => {
    
    return state.cacheObjectsByType.get(type)
}

export default {
    
    cacheObjectTypes,
    cacheObjectsForType
}