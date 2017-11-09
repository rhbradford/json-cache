// Author: Richard Bradford

import {State} from "./reducers"

const selectedType = (state: State): string => {
    
    return state.selectedType
}

export default {
    
    selectedType
}