// Author: Richard Bradford

import {State} from "./reducers"
import {ConnectionErrorInfo, ConnectionStatus} from "../socketMiddleware/types"

const url = (state: State): string => {
    
    return state.url
}

const status = (state: State): ConnectionStatus => {
    
    return state.status
}

const errorInfo = (state: State): ConnectionErrorInfo => {
    
    return state.errorInfo
}

export default {
    
    url,
    status,
    errorInfo
}