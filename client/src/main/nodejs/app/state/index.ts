// Author: Richard Bradford

import {State as cacheObjectDataState} from "./cacheObjectData"
import {State as cacheObjectTypeState} from "./cacheObjectType"

export interface State {
    
    cacheObjectData: cacheObjectDataState
    cacheObjectType: cacheObjectTypeState
}