// Author: Richard Bradford

import TypeKeys from "./types"
import {ActionTypes} from "./actions"

export interface State {
    
    selectedCacheObjectType: string
}

export const initialState: State = {
    
    selectedCacheObjectType: undefined
}

const typeSelectedReducer = (state: State = initialState, action: ActionTypes): State => {
 
    switch(action.type) {
        
        case TypeKeys.TYPE_SELECTED:
            return { 
                selectedCacheObjectType: action.cacheObjectType 
            }
            
        default:
            return state
    }
}

export default typeSelectedReducer