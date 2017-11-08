// Author: Richard Bradford

import configureMockStore from "redux-mock-store"
import actions from "./actions"
import {socketMiddleware} from "./socketMiddleware"
import {Socket, SocketProvider} from "./types"
import Mock = jest.Mock

const sockets = new Map<string, Socket>()
const socketProvider: SocketProvider = (params, eventhandlers) => {

    const socket = {
        send:      (data: any) => {

        },
        close:     (code?: number, reason?: string) => {

        },
        onopen:    eventhandlers.onopen,
        onclose:   eventhandlers.onclose,
        onerror:   eventhandlers.onerror,
        onmessage: eventhandlers.onmessage
    }
    
    sockets.set(params.url, socket)
    
    return socket
}

const middlewares = [socketMiddleware(socketProvider)]
const mockStore = configureMockStore(middlewares)

describe('socketMiddleware', () => {

    test("should obtain a socket and emit onConnecting when asked to connect", () => {

        sockets.clear()
        
        const store = mockStore({})

        const action = actions.connect("url")
        
        store.dispatch(action)
  
        expect(sockets.size).toBe(1)
        
        const socket = sockets.get("url")
        
        expect(socket).toBeDefined()
        
        expect(store.getActions()).toEqual([
            actions.onConnecting("url")
        ])
    })
})