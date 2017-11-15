// Author: Richard Bradford

import TypeKeys from "./types"
import {ActionTypes} from "./actions"

export interface State {
    
    readonly selectedType: string
}

export const initialState: () => State = () => ({
    
    selectedType: undefined
})

const reducer = (state: State = initialState(), action: ActionTypes): State => {
 
    switch(action.type) {
        
        case TypeKeys.TYPE_SELECTED:
            return { 
                selectedType: action.cacheObjectType 
            }
            
        default:
            return state
    }
}

export default reducer