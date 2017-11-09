// Author: Richard Bradford

export interface SocketEventHandling {
    
    onclose: (ev: CloseEvent) => any
    onerror: (ev: Event) => any
    onmessage: (ev: MessageEvent) => any
    onopen: (ev: Event) => any
}

export interface Socket extends SocketEventHandling {

    close(code?: number, reason?: string): void
    send(data: any): void
}

export interface WithURL {
    
    readonly url: string
}

export interface ConnectionParams extends WithURL {
    
    readonly [others: string]: any
}

/**
 * Called by middleware to obtain a WebSocket.
 * 
 * The given eventhandlers should be attached to the WebSocket. 
 * Note that there is an opportunity to wrap the handlers and extend the handling provided by the middleware.
 * For example, the params might provide an authentication token to be sent as the first message.
 * 
 * @param {ConnectionParams} params provides the address for the WebSocket to connect with, and other data as required
 * @param {SocketEventHandling} eventhandlers that should be attached to the WebSocket created
 * @return {Socket} a WebSocket that will connect to the given url and call the given eventhandlers
 */
export interface SocketProvider {

    (params: ConnectionParams, eventhandlers: SocketEventHandling): Socket 
}

export interface Message {
    
    readonly type: string,
    readonly content: any
}

export enum ConnectionStatus {
    
    CONNECTING = "CONNECTING",
    CONNECTED = "CONNECTED",
    DISCONNECTING = "DISCONNECTING",
    DISCONNECTED = "DISCONNECTED"
}

enum TypeKeys {

    CONNECT = "CONNECT",
    CONNECTING = "CONNECTING",
    CONNECTED = "CONNECTED",
    DISCONNECT = "DISCONNECT",
    DISCONNECTING = "DISCONNECTING",
    DISCONNECTED = "DISCONNECTED",
    
    ERROR_OCCURRED = "ERROR_OCCURRED",
    
    MESSAGE_RECEIVED = "MESSAGE_RECEIVED",
    SEND_MESSAGE = "SEND_MESSAGE",
    
    OTHER_ACTION = "__any_other_action_type"
}

export default TypeKeys
