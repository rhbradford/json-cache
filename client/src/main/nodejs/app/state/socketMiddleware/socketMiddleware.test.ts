// Author: Richard Bradford

import configureMockStore, {MockStore} from "redux-mock-store"
import actions from "./actions"
import {socketMiddleware} from "./socketMiddleware"
import {Socket, SocketProvider} from "./types"

const sockets = new Map<string, Socket>()

const socketSend: jest.Mock<void> = jest.fn()
const socketClose: jest.Mock<void> = jest.fn()

const socketProvider: SocketProvider = (params, eventhandlers) => {

    const socket = {
        send:      socketSend,
        close:     socketClose,
        onopen:    eventhandlers.onopen,
        onclose:   eventhandlers.onclose,
        onerror:   eventhandlers.onerror,
        onmessage: eventhandlers.onmessage
    }

    sockets.set(params.url, socket)

    return socket
}

const reset = () => {
    
    // middlewareSockets.clear()
    sockets.clear()
    resetMocks()
}

const resetMocks = () => {
    
    socketSend.mockClear()
    socketClose.mockClear()
}

// const middlewares = [socketMiddleware(socketProvider)]
// const mockStore = configureMockStore(middlewares)

describe('socketMiddleware', () => {

    test("should obtain a socket and emit onConnecting when asked to connect", () => {

        reset()

        const middlewares = [socketMiddleware(socketProvider)]
        const mockStore = configureMockStore(middlewares)
        const store = mockStore({})

        const action = actions.connect("url")

        store.dispatch(action)

        expect(sockets.size).toBe(1)

        const socket = sockets.get("url")

        expect(socket).toBeDefined()

        expect(store.getActions()).toEqual([
            actions.connect("url"),
            actions.onConnecting("url")
        ])
    })

    test("should emit expected actions and call socket for normal lifecycle: connect, receive messages, send messages, disconnect", () => {

        reset()

        const middlewares = [socketMiddleware(socketProvider)]
        const mockStore = configureMockStore(middlewares)
        const store = mockStore({})

        // Disconnect sent to middleware which has not yet had a connect... 
        store.dispatch(actions.disconnect("url"))

        // ... should do nothing
        expect(store.getActions()).toEqual([
            actions.disconnect("url")
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)

        store.clearActions()
        // Send message sent to middleware which has not yet had a connect...
        store.dispatch(actions.sendMessage("url", { type: "MyMsg", content: "stuff" }))

        // ... should do nothing
        expect(store.getActions()).toEqual([
            actions.sendMessage("url", { type: "MyMsg", content: "stuff" })
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)
        
        store.clearActions()
        // Request middleware to connect
        store.dispatch(actions.connect("url"))
        
        const socket = sockets.get("url")
        expect(socket).toBeDefined()
        expect(store.getActions()).toEqual([
            actions.connect("url"),
            actions.onConnecting("url"),
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)

        store.clearActions()
        // Simulate successful connection        
        socket.onopen({ type: "onopen" } as Event)

        expect(store.getActions()).toEqual([
            actions.onConnected("url")
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)
        
        store.clearActions()
        // Request middleware to send a message
        let msg = { type: "MyMsg", content: "stuff" }
        store.dispatch(actions.sendMessage("url", msg))
        
        expect(socketClose.mock.calls.length).toBe(0)
        expect(socketSend.mock.calls.length).toBe(1)
        expect(socketSend.mock.calls[0].length).toBe(1)
        expect(socketSend.mock.calls[0][0]).toEqual(JSON.stringify(msg))
        expect(store.getActions()).toEqual([
            actions.sendMessage("url", msg)
        ])
        
        store.clearActions()
        resetMocks()
        // Simulate receiving a message
        msg = { type: "MyMsgResponse", content: "stuff is working" }
        socket.onmessage({
            type: "onmessage",
            data: JSON.stringify(msg)
        } as MessageEvent)
        
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)
        expect(store.getActions()).toEqual([
            actions.onMessageReceived("url", msg)
        ])
        
        store.clearActions()
        // Request middleware to disconnect
        store.dispatch(actions.disconnect("url"))

        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(1)
        expect(store.getActions()).toEqual([
            actions.disconnect("url"),
            actions.onDisconnecting("url"),
        ])
        
        store.clearActions()
        resetMocks()
        // Simulate close of connection        
        socket.onclose({ type: "onclose", code: 1000 } as CloseEvent)

        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)
        expect(store.getActions()).toEqual([
            actions.onDisconnected("url"),
        ])

        store.clearActions()
        // Send message sent to middleware which has disconnected...
        store.dispatch(actions.sendMessage("url", { type: "MyMsg", content: "stuff" }))

        // ... should emit nothing and do nothing
        expect(store.getActions()).toEqual([
            actions.sendMessage("url", { type: "MyMsg", content: "stuff" })
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)
    })
    
    test("should close socket and emit onErrorOccurred when errorMsg occurs", () => {
        
        reset()

        const middlewares = [socketMiddleware(socketProvider)]
        const mockStore = configureMockStore(middlewares)
        let store = mockStore({})

        // Request middleware to connect
        store.dispatch(actions.connect("url"))
        
        let socket = sockets.get("url")
        expect(socket).toBeDefined()
        expect(store.getActions()).toEqual([
            actions.connect("url"),
            actions.onConnecting("url"),
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)

        store.clearActions()
        // Simulate unsuccessful connection        
        socket.onerror({ type: "onerror" } as Event)

        expect(store.getActions()).toEqual([
            actions.onDisconnecting("url")
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(1)
        
        reset()
        store = mockStore({})
        
        // Request middleware to connect
        store.dispatch(actions.connect("url"))
        
        socket = sockets.get("url")
        expect(socket).toBeDefined()
        expect(store.getActions()).toEqual([
            actions.connect("url"),
            actions.onConnecting("url"),
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)

        store.clearActions()
        // Simulate successful connection        
        socket.onopen({ type: "onopen" } as Event)

        expect(store.getActions()).toEqual([
            actions.onConnected("url")
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)
        
        store.clearActions()
        // Simulate an errorMsg
        socket.onerror({ type: "onerror" } as Event)
        socket.onclose({ type: "onclose", code: 1001, reason: "server failed"} as CloseEvent)

        expect(store.getActions()).toEqual([
            actions.onDisconnecting("url"),
            actions.onDisconnected("url"),
            actions.onErrorOccurred("url", 1001, "server failed")
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(1)
    })

    test("should work with multiple connections", () => {
        
        reset()

        const middlewares = [socketMiddleware(socketProvider)]
        const mockStore = configureMockStore(middlewares)
        const store: MockStore<{}> = mockStore({})

        // url1 - Request middleware to connect
        store.dispatch(actions.connect("url1"))
        
        const socket1 = sockets.get("url1")
        expect(socket1).toBeDefined()
        expect(store.getActions()).toEqual([
            actions.connect("url1"),
            actions.onConnecting("url1"),
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)

        store.clearActions()
        // url1 - Simulate successful connection        
        socket1.onopen({ type: "onopen" } as Event)

        expect(store.getActions()).toEqual([
            actions.onConnected("url1")
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)

        store.clearActions()
        // url2 - Request middleware to connect
        store.dispatch(actions.connect("url2"))
        
        const socket2 = sockets.get("url2")
        expect(socket2).toBeDefined()
        expect(store.getActions()).toEqual([
            actions.connect("url2"),
            actions.onConnecting("url2"),
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)
        
        store.clearActions()
        // url1 - Request middleware to send a message 
        let msg = { type: "MyMsg", content: "stuff" }
        store.dispatch(actions.sendMessage("url1", msg))
        
        expect(socketClose.mock.calls.length).toBe(0)
        expect(socketSend.mock.calls.length).toBe(1)
        expect(socketSend.mock.calls[0].length).toBe(1)
        expect(socketSend.mock.calls[0][0]).toEqual(JSON.stringify(msg))
        expect(store.getActions()).toEqual([
            actions.sendMessage("url1", msg)
        ])
        
        store.clearActions()
        resetMocks()
        // url2 - Simulate receiving a message
        msg = { type: "MyMsgResponse", content: "stuff is working" }
        socket2.onmessage({
            type: "onmessage",
            data: JSON.stringify(msg)
        } as MessageEvent)
        
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)
        expect(store.getActions()).toEqual([
            actions.onMessageReceived("url2", msg)
        ])
        
        store.clearActions()
        // url1 - Request middleware to disconnect
        store.dispatch(actions.disconnect("url1"))

        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(1)
        expect(store.getActions()).toEqual([
            actions.disconnect("url1"),
            actions.onDisconnecting("url1"),
        ])
        
        store.clearActions()
        resetMocks()
        // url1 - Simulate close of connection        
        socket1.onclose({ type: "onclose", code: 1000 } as CloseEvent)

        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)
        expect(store.getActions()).toEqual([
            actions.onDisconnected("url1"),
        ])

        store.clearActions()
        resetMocks()
        // url2 - Request middleware to send a message
        msg = { type: "MyMsgFor2", content: "stuff" }
        store.dispatch(actions.sendMessage("url2", msg))

        expect(socketClose.mock.calls.length).toBe(0)
        expect(socketSend.mock.calls.length).toBe(1)
        expect(socketSend.mock.calls[0].length).toBe(1)
        expect(socketSend.mock.calls[0][0]).toEqual(JSON.stringify(msg))
        expect(store.getActions()).toEqual([
            actions.sendMessage("url2", msg)
        ])
        
        store.clearActions()
        resetMocks()
        // url2 - Request middleware to disconnect
        store.dispatch(actions.disconnect("url2"))

        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(1)
        expect(store.getActions()).toEqual([
            actions.disconnect("url2"),
            actions.onDisconnecting("url2"),
        ])
        
        store.clearActions()
        resetMocks()
        // url1 - Simulate close of connection        
        socket2.onclose({ type: "onclose", code: 1000 } as CloseEvent)

        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)
        expect(store.getActions()).toEqual([
            actions.onDisconnected("url2"),
        ])

        store.clearActions()
        // Send to middleware which has disconnected...
        store.dispatch(actions.sendMessage("url2", { type: "MyMsg", content: "stuff" }))
        store.dispatch(actions.sendMessage("url1", { type: "MyMsg", content: "stuff" }))

        // ... should do nothing
        expect(store.getActions()).toEqual([
            actions.sendMessage("url2", { type: "MyMsg", content: "stuff" }),
            actions.sendMessage("url1", { type: "MyMsg", content: "stuff" })
        ])
        expect(socketSend.mock.calls.length).toBe(0)
        expect(socketClose.mock.calls.length).toBe(0)
    })
})