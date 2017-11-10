// Author: Richard Bradford

import TypeKeys, {ConnectionErrorInfo, ConnectionStatus} from "../socketMiddleware/types"
import {ActionTypes} from "../socketMiddleware/actions"

export interface State {

    readonly url: string
    readonly status: ConnectionStatus
    readonly errorInfo: ConnectionErrorInfo
}

export const initialState: State = {

    url:       "",
    status:    ConnectionStatus.DISCONNECTED,
    errorInfo: undefined
}

const cacheConnectionReducer = (state: State = initialState, action: ActionTypes): State => {

    switch(action.type) {

        case TypeKeys.DISCONNECTED:
            return {
                ...state,
                url:    action.url,
                status: ConnectionStatus.DISCONNECTED
            }

        case TypeKeys.CONNECT:
            return {
                ...state,
                url:       action.url,
                errorInfo: undefined
            }

        case TypeKeys.CONNECTING:
            return {
                ...state,
                url:    action.url,
                status: ConnectionStatus.CONNECTING
            }

        case TypeKeys.CONNECTED:
            return {
                ...state,
                url:    action.url,
                status: ConnectionStatus.CONNECTED
            }

        case TypeKeys.DISCONNECTING:
            return {
                ...state,
                url:    action.url,
                status: ConnectionStatus.DISCONNECTING
            }

        case TypeKeys.ERROR_OCCURRED:
            return {
                ...state,
                errorInfo: {
                    errorMsg: action.errorMsg,
                    errorCode: action.errorCode
                }
            }

        default:
            return state
    }
}

export default cacheConnectionReducer