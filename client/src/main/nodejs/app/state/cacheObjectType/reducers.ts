// Author: Richard Bradford

import TypeKeys from "./types"
import {ActionTypes} from "./actions"

interface State {
    
    selectedCacheObjectType: string
}

const initialState: State = {
    
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