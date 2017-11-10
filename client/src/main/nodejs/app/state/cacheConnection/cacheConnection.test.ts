// Author: Richard Bradford

import reducer, {State, initialState} from "./reducers"
import operations from "./operations"
import actions from "../socketMiddleware/actions"
import TypeKeys, {ConnectionStatus, Message} from "../socketMiddleware/types"

describe("reducer", () => {
    
    test("should return the initial state", () => {

        expect(reducer(undefined, { type: TypeKeys.OTHER_ACTION })).toEqual(initialState)
    })
    
    test("should set the connection url and reset errorInfo when connect invoked", () => {
        
        const inputState = {
            ...initialState,
            errorInfo: {
                errorMsg:  "bad thing happened",
                errorCode: 1001
            }
        } as State
        const operation = operations.connect("url")
        
        const state = reducer(inputState, operation)
        
        expect(state).toEqual({
            ...inputState,
            url: "url",
            errorInfo: undefined
        } as State)
    })
    
    test("should set status when connecting", () => {
        
        const inputState = {
            ...initialState,
            url: "url"
        } as State
        const operation = actions.onConnecting("url")
        
        const state = reducer(inputState, operation)
        
        expect(state).toEqual({
            ...inputState,
            status: ConnectionStatus.CONNECTING
        } as State)
    })
    
    test("should set errorInfo and preserve status when errorMsg occurs", () => {
        
        const inputState = {
            url:      "url",
            status:   ConnectionStatus.CONNECTING,
            errorInfo: undefined
        } as State
        const operation = actions.onErrorOccurred("url", 1001, "bad thing happened")
        
        const state = reducer(inputState, operation)
        
        expect(state).toEqual({
            url: "url",
            status: ConnectionStatus.CONNECTING,
            errorInfo: {
                errorMsg:  "bad thing happened",
                errorCode: 1001
            }
        } as State)
    })
    
    test("should set status when connected", () => {
        
        const inputState = {
            url:      "url",
            status:   ConnectionStatus.CONNECTING,
            errorInfo: undefined
        } as State
        const operation = actions.onConnected("url")
        
        const state = reducer(inputState, operation)
        
        expect(state).toEqual({
            ...inputState,
            status: ConnectionStatus.CONNECTED
        } as State)
    })
    
    test("should set status when disconnecting", () => {
        
        const inputState = {
            url:      "url",
            status:   ConnectionStatus.CONNECTED,
            errorInfo: undefined
        } as State
        const operation = actions.onDisconnecting("url")
        
        const state = reducer(inputState, operation)
        
        expect(state).toEqual({
            ...inputState,
            status: ConnectionStatus.DISCONNECTING
        } as State)
    })
    
    test("should set status when disconnected", () => {
        
        const inputState = {
            url:      "url",
            status:   ConnectionStatus.DISCONNECTING,
            errorInfo: undefined
        } as State
        const operation = actions.onDisconnected("url")
        
        const state = reducer(inputState, operation)
        
        expect(state).toEqual({
            ...inputState,
            status: ConnectionStatus.DISCONNECTED
        } as State)
    })

    test("should not change state when disconnect or sendMessage invoked", () => {
        
        const inputState = {
            url:      "url",
            status:   ConnectionStatus.CONNECTED,
            errorInfo: undefined
        } as State
        
        let state = reducer(inputState, operations.disconnect("url"))
        
        expect(state).toBe(inputState)
        
        state = reducer(inputState, operations.sendMessage("url", {type: "msg", content:""} as Message))
                
        expect(state).toBe(inputState)
    })
})
