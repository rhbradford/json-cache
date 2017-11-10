// Author: Richard Bradford

import TypeKeys from "./types"
import {ActionTypes} from "./actions"

export interface State {
    
    readonly filter: string
}

export const initialState: State = {
    
    filter: ""
}

const reducer = (state: State = initialState, action: ActionTypes): State => {
 
    switch(action.type) {
        
        case TypeKeys.FILTER_SET:
            return { 
                filter: action.cacheObjectFilter 
            }
            
        default:
            return state
    }
}

export default reducer