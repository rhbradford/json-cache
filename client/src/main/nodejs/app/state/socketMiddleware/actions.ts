// Author: Richard Bradford

import TypeKeys, {ConnectionParams, Message, WithURL} from "./types"
import {Action} from "redux"

export interface OtherAction extends Action {
    
    readonly type: TypeKeys.OTHER_ACTION
}

export interface ConnectAction extends ConnectionParams, Action {

    readonly type: TypeKeys.CONNECT
}

export interface ConnectingAction extends WithURL, Action {

    readonly type: TypeKeys.CONNECTING
}

export interface ConnectedAction extends WithURL, Action {

    readonly type: TypeKeys.CONNECTED
}

export interface DisconnectAction extends WithURL, Action {

    readonly type: TypeKeys.DISCONNECT
}

export interface DisconnectingAction extends WithURL, Action {

    readonly type: TypeKeys.DISCONNECTING
}

export interface DisconnectedAction extends WithURL, Action {

    readonly type: TypeKeys.DISCONNECTED
}

export interface MessageReceivedAction extends WithURL, Action {

    readonly type: TypeKeys.MESSAGE_RECEIVED
    readonly msg: Message
}

export interface SendMessageAction extends WithURL, Action {

    readonly type: TypeKeys.SEND_MESSAGE
    readonly msg: Message
}

export type ActionTypes = 
    | ConnectAction
    | ConnectingAction
    | ConnectedAction
    | DisconnectAction
    | DisconnectingAction
    | DisconnectedAction
    | SendMessageAction
    | MessageReceivedAction
    | OtherAction

const connect = (url: string): ConnectAction => ({
    
    type: TypeKeys.CONNECT,
    url
})

const onConnecting = (url: string): ConnectingAction => ({
    
    type: TypeKeys.CONNECTING,
    url
})

const onConnected = (url: string): ConnectedAction => ({
    
    type: TypeKeys.CONNECTED,
    url
})

const disconnect = (url: string): DisconnectAction => ({
    
    type: TypeKeys.DISCONNECT,
    url
})

const onDisconnecting = (url: string): DisconnectingAction => ({
    
    type: TypeKeys.DISCONNECTING,
    url
})

const onDisconnected = (url: string): DisconnectedAction => ({
    
    type: TypeKeys.DISCONNECTED,
    url
})

const onMessageReceived = (url: string, msg: Message): MessageReceivedAction => ({
    
    type: TypeKeys.MESSAGE_RECEIVED,
    url,
    msg
})

const sendMessage = (url: string, msg: Message): SendMessageAction => ({
    
    type: TypeKeys.SEND_MESSAGE,
    url,
    msg
})

export default {

    connect,
    onConnecting,
    onConnected,
    disconnect,
    onDisconnecting,
    onDisconnected,
    onMessageReceived,
    sendMessage
}