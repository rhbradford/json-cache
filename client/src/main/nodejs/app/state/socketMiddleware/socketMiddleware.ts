// Author: Richard Bradford

import TypeKeys, {SocketProvider} from "./types"
import actions, {ActionTypes} from "./actions"
import {ConnectionParams, Socket} from "./types"
import {Action, Dispatch, Middleware, MiddlewareAPI} from "redux"

export const socketMiddleware: (socketProvider: SocketProvider) => Middleware = (socketProvider: SocketProvider) => {

    // noinspection JSUnusedLocalSymbols
    const onOpen = (sockets: Map<string, Socket>, store: MiddlewareAPI<void>, params: ConnectionParams) => (evt: Event)  => {
    
        store.dispatch(actions.onConnected(params.url))
    }
    
    // noinspection JSUnusedLocalSymbols
    const onClose = (store: MiddlewareAPI<void>, params: ConnectionParams) => (evt: CloseEvent)  => {
        
        store.dispatch(actions.onDisconnected(params.url))
        if(evt.code !== 1000)
            store.dispatch(actions.onErrorOccurred(params.url, evt.code, evt.reason))
    }
    
    // noinspection JSUnusedLocalSymbols
    const onError = (sockets: Map<string, Socket>, store: MiddlewareAPI<void>, params: ConnectionParams) => (evt: Event)  => {
    
        let socket = sockets.get(params.url)
        if(socket) disconnect(sockets, store, socket, params.url)
    }
    
    const onMessage = (store: MiddlewareAPI<void>, params: ConnectionParams) => (evt: MessageEvent)  => {
        
        const msg = JSON.parse(evt.data)
        store.dispatch(actions.onMessageReceived(params.url, msg))
    }
    
    const disconnect = (sockets: Map<string, Socket>, store: MiddlewareAPI<void>, socket: Socket, url: string) => {
        
        socket.close()
        sockets.delete(url)
        store.dispatch(actions.onDisconnecting(url))
    }
    
    const sockets = new Map<string, Socket>()
    
    return <S>(store: MiddlewareAPI<S>) => (next: Dispatch<S>) => <A extends Action>(a: A): A => {

        const result = next(a)

        // noinspection JSUnusedLocalSymbols
        const isActionTypes = (a: any): a is ActionTypes => {
            return true
        }

        if(isActionTypes(a)) {

            const action = <ActionTypes>a

            switch(action.type) {

                case TypeKeys.CONNECT: {
                    let socket = sockets.get(action.url)
                    // Connecting when a socket for a URL still exists is an error.
                    // There is no good way to signal the issue to the coder, however.
                    // The following will do some clean up:
                    if(socket) disconnect(sockets, store, socket, action.url)

                    store.dispatch(actions.onConnecting(action.url))

                    socket = socketProvider(
                        action,
                        {
                            onopen:    onOpen(sockets, store, action),
                            onclose:   onClose(store, action),
                            onerror:   onError(sockets, store, action),
                            onmessage: onMessage(store, action)
                        }
                    )

                    sockets.set(action.url, socket)
                }
                    break

                case TypeKeys.DISCONNECT: {
                    let socket = sockets.get(action.url)
                    if(socket) disconnect(sockets, store, socket, action.url)
                }
                    break

                case TypeKeys.SEND_MESSAGE: {
                    let socket = sockets.get(action.url)
                    if(socket) socket.send(JSON.stringify(action.msg))
                }
                    break
            }
        }

        return result
    }
}

export const webSocketProvider: SocketProvider = (params, eventhandlers) => {
    
    const ws = new WebSocket(params.url)
    
    ws.onopen = eventhandlers.onopen
    ws.onclose = eventhandlers.onclose
    ws.onerror = eventhandlers.onerror
    ws.onmessage = eventhandlers.onmessage
    
    return ws
}

export default socketMiddleware(webSocketProvider)