// Author: Richard Bradford

import reducer, {State, initialState} from "./reducers"
import TypeKeys from "./types"
import operations from "./operations"

describe("reducer", () => {

    test("should return the initial state", () => {

        expect(reducer(undefined, { type: TypeKeys.OTHER_ACTION })).toEqual(initialState)
    })
    
    test("should set the selected type", () =>{
        
        const inputState = {
            selectedType: "TypeA"
        } as State
        const operation = operations.onTypeSelected("TypeB")
        const expectedState = {
            selectedType: "TypeB"
        } as State
        
        const state = reducer(inputState, operation)
        
        expect(state).toEqual(expectedState)
    })    
})