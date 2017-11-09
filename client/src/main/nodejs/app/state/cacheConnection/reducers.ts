// Author: Richard Bradford

import TypeKeys, {ConnectionStatus} from "../socketMiddleware/types"
import {ActionTypes} from "../socketMiddleware/actions"
import {ActionTypes as URLActionTypes} from "./actions"
import {TypeKeys as URLTypeKeys} from "./types"

export interface State {

    readonly url: string
    readonly status: ConnectionStatus
    readonly error: string
}

export const initialState: State = {

    url:    undefined,
    status: ConnectionStatus.DISCONNECTED,
    error:  undefined
}

const cacheConnectionReducer = (state: State = initialState, action: ActionTypes): State => {

    switch(action.type) {

        case TypeKeys.DISCONNECTED:
            return {
                url:    action.url,
                status: ConnectionStatus.DISCONNECTED,
                ...state
            }

        case TypeKeys.CONNECT:
            return {
                url: action.url,
                ...state
            }

        case TypeKeys.CONNECTING:
            return {
                url:    action.url,
                status: ConnectionStatus.CONNECTING,
                error:  undefined
            }

        case TypeKeys.CONNECTED:
            return {
                url:    action.url,
                status: ConnectionStatus.CONNECTED,
                ...state
            }

        case TypeKeys.DISCONNECTING:
            return {
                url:    action.url,
                status: ConnectionStatus.DISCONNECTING,
                ...state
            }

        case TypeKeys.ERROR_OCCURRED:
            return {
                error: action.msg,
                ...state
            }

        default:
            return state
    }
}

const urlReducer = (state: State = initialState, action: URLActionTypes): State => {
    
    switch(action.type) {
        
        case URLTypeKeys.SET_URL:
            return {
                url: action.url,
                ...state
            }            

        default:
            return state
    }
}

export default {
    
    urlReducer,
    cacheConnectionReducer
}