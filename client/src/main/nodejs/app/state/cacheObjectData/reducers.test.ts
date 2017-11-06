// Author: Richard Bradford

import reducer, {flattenCacheObject, ImmutableState, initialState, State} from "./reducers"
import TypeKeys, {CacheObject, CacheRemove} from "./types"
import operations from "./operations"
import {Map} from "immutable"

const cacheObject_A1 = {
    id:      "A_1",
    type:    "TypeA",
    content: {
        price:    23.5,
        product:  "X",
        location: {
            exchange: "GR",
            country:  "DE"
        }
    }
}
const flattened_cacheObject_A1 = flattenCacheObject(cacheObject_A1)
const cacheObject_A1_update = {
    id:      "A_1",
    type:    "TypeA",
    content: {
        price:    25.3,
        product:  "X",
        location: {
            exchange: "GR",
            country:  "DE"
        }
    }
}
const flattened_cacheObject_A1_update = flattenCacheObject(cacheObject_A1_update)
const cacheObject_A2 = {
    id:      "A_2",
    type:    "TypeA",
    content: {
        price:    22.5,
        product:  "Y",
        location: {
            exchange: "GR",
            country:  "DE"
        }
    }
}
const flattened_cacheObject_A2 = flattenCacheObject(cacheObject_A2)
const cacheObject_B1 = {
    id:      "B1",
    type:    "TypeB",
    content: {
        name: "Fred",
        age:  64
    }
}
const flattened_cacheObject_B1 = flattenCacheObject(cacheObject_B1)
const cacheObject_B1_update = {
    id:      "B1",
    type:    "TypeB",
    content: {
        name: "Fred",
        age:  65
    }
}
const flattened_cacheObject_B1_update = flattenCacheObject(cacheObject_B1_update)
const cacheObject_B2 = {
    id:      "B2",
    type:    "TypeB",
    content: {
        name: "James",
        age:  22
    }
}
const flattened_cacheObject_B2 = flattenCacheObject(cacheObject_B2)
const cacheObject_C1 = {
    id:      "C1",
    type:    "TypeC",
    content: ["some", "stuff"]
}
const flattened_cacheObject_C1 = flattenCacheObject(cacheObject_C1)
const cacheObject_C1_update = {
    id:      "C1",
    type:    "TypeC",
    content: ["more", "stuff"]
}
const flattened_cacheObject_C1_update = flattenCacheObject(cacheObject_C1_update)
const cacheObject_C2 = {
    id:      "C2",
    type:    "TypeC",
    content: ["other", "things"]
}
const flattened_cacheObject_C2 = flattenCacheObject(cacheObject_C2)
const cacheObject_D1 = {
    id:      "D1",
    type:    "TypeD",
    content: "text"
}
const flattened_cacheObject_D1 = flattenCacheObject(cacheObject_D1)
const exampleState: ImmutableState = Map({
    cacheObjectTypes: ["TypeA", "TypeB", "TypeC"],    
    cacheObjectsByType: Map({
        TypeA: [flattened_cacheObject_A1, flattened_cacheObject_A2],
        TypeB: [flattened_cacheObject_B1, flattened_cacheObject_B2],
        TypeC: [flattened_cacheObject_C1, flattened_cacheObject_C2]
    }),
    cacheObjectData: Map({
        TypeA: Map({
            A_1: flattened_cacheObject_A1,
            A_2: flattened_cacheObject_A2
        }),
        TypeB: Map({
            B1: flattened_cacheObject_B1,
            B2: flattened_cacheObject_B2
        }),
        TypeC: Map({
            C1: flattened_cacheObject_C1,
            C2: flattened_cacheObject_C2
        })
    }) 
} as State)

describe("cacheObjectData reducer", () => {

    describe("flattenCacheObject function", () => {

        test("flattens cacheObject with content as object using keys from content", () => {

            expect(flattenCacheObject(cacheObject_A1)).toEqual({
                id:       "A_1",
                type:     "TypeA",
                price:    23.5,
                product:  "X",
                location: JSON.stringify(cacheObject_A1.content.location)
            })
        })

        test("flattens cacheObject with content that is not an object using JSON string for content", () => {

            expect(flattenCacheObject(cacheObject_C1)).toEqual({
                id:      "C1",
                type:    "TypeC",
                content: JSON.stringify(cacheObject_C1.content)
            })
        })
    })

    test("should return the initial state", () => {

        expect(reducer(undefined, { type: TypeKeys.OTHER_ACTION })).toEqual(initialState)
    })

    test("should add puts to initial state", () => {

        const inputState = initialState
        const changes = {

            puts:    [
                cacheObject_A1,
                cacheObject_B1,
                cacheObject_C1,
                cacheObject_A2,
                cacheObject_B2,
                cacheObject_C2
            ],
            removes: [] as Array<CacheRemove>
        }
        const operation = operations.onChangeSetReceived(changes)
        const expectedState = exampleState

        const state = reducer(inputState, operation)

        expect(state.toJS()).toEqual(expectedState.toJS())
    })

    test("should return larger array of types if puts add new types to state", () => {

        const inputState = exampleState
        const changes = {

            puts:    [
                cacheObject_D1
            ],
            removes: [] as Array<CacheRemove>
        }
        const operation = operations.onChangeSetReceived(changes)
        const expectedState = Map({
            cacheObjectTypes:   ["TypeA", "TypeB", "TypeC", "TypeD"],
            cacheObjectsByType: Map({
                TypeA: [flattened_cacheObject_A1, flattened_cacheObject_A2],
                TypeB: [flattened_cacheObject_B1, flattened_cacheObject_B2],
                TypeC: [flattened_cacheObject_C1, flattened_cacheObject_C2],
                TypeD: [flattened_cacheObject_D1]
            }),
            cacheObjectData:    Map({
                TypeA: Map({
                    A_1: flattened_cacheObject_A1,
                    A_2: flattened_cacheObject_A2
                }),
                TypeB: Map({
                    B1: flattened_cacheObject_B1,
                    B2: flattened_cacheObject_B2
                }),
                TypeC: Map({
                    C1: flattened_cacheObject_C1,
                    C2: flattened_cacheObject_C2
                }),
                TypeD: Map({
                    D1: flattened_cacheObject_D1
                })
            })
        } as State) as ImmutableState

        const state = reducer(inputState, operation)

        expect(state.toJS()).toEqual(expectedState.toJS())

        // Arrays of objects for types should be re-used as is if no changes for that type have occurred
        expect(state.get("cacheObjectsByType").get("TypeA")).toBe(inputState.get("cacheObjectsByType").get("TypeA"))
        expect(state.get("cacheObjectsByType").get("TypeB")).toBe(inputState.get("cacheObjectsByType").get("TypeB"))
        expect(state.get("cacheObjectsByType").get("TypeC")).toBe(inputState.get("cacheObjectsByType").get("TypeC"))
    })

    test("should remove removes from state", () => {

        const inputState = exampleState
        const changes = {

            puts:    [] as Array<CacheObject>,
            removes: [
                {
                    id: "A_1"
                },
                {
                    id: "C1"
                }
            ]
        }
        const operation = operations.onChangeSetReceived(changes)
        const expectedState = Map({
                    cacheObjectTypes: ["TypeA", "TypeB", "TypeC"],    
                    cacheObjectsByType: Map({
                        TypeA: [flattened_cacheObject_A2],
                        TypeB: [flattened_cacheObject_B1, flattened_cacheObject_B2],
                        TypeC: [flattened_cacheObject_C2]
                    }),
                    cacheObjectData: Map({
                        TypeA: Map({
                            A_2: flattened_cacheObject_A2
                        }),
                        TypeB: Map({
                            B1: flattened_cacheObject_B1,
                            B2: flattened_cacheObject_B2
                        }),
                        TypeC: Map({
                            C2: flattened_cacheObject_C2
                        })
                    }) 
                } as State) as ImmutableState

        const state = reducer(inputState, operation)

        expect(state.toJS()).toEqual(expectedState.toJS())

        // Array of types should be re-used as is if no changes to types present have occurred 
        expect(state.get("cacheObjectTypes")).toBe(inputState.get("cacheObjectTypes"))

        // Arrays of objects for types should be re-used as is if no changes for that type have occurred
        expect(state.get("cacheObjectsByType").get("TypeB")).toBe(inputState.get("cacheObjectsByType").get("TypeB"))
    })

    test("should handle removes that are for objects not present in state", () => {

        const inputState = exampleState
        const changes = {

            puts:    [] as Array<CacheObject>,
            removes: [
                {
                    id: "XX"
                },
                {
                    id: "YY"
                }
            ]
        }
        const operation = operations.onChangeSetReceived(changes)
        const expectedState = Map({
                    cacheObjectTypes: ["TypeA", "TypeB", "TypeC"],    
                    cacheObjectsByType: Map({
                        TypeA: [flattened_cacheObject_A1, flattened_cacheObject_A2],
                        TypeB: [flattened_cacheObject_B1, flattened_cacheObject_B2],
                        TypeC: [flattened_cacheObject_C1, flattened_cacheObject_C2]
                    }),
                    cacheObjectData: Map({
                        TypeA: Map({
                            A_1: flattened_cacheObject_A1,
                            A_2: flattened_cacheObject_A2
                        }),
                        TypeB: Map({
                            B1: flattened_cacheObject_B1,
                            B2: flattened_cacheObject_B2
                        }),
                        TypeC: Map({
                            C1: flattened_cacheObject_C1,
                            C2: flattened_cacheObject_C2
                        })
                    }) 
                } as State) as ImmutableState

        const state = reducer(inputState, operation)

        expect(state.toJS()).toEqual(expectedState.toJS())

        // Array of types should be re-used as is if no changes to types present have occurred 
        expect(state.get("cacheObjectTypes")).toBe(inputState.get("cacheObjectTypes"))

        // Arrays of objects for types should be re-used as is if no changes for that type have occurred
        expect(state.get("cacheObjectsByType").get("TypeA")).toBe(inputState.get("cacheObjectsByType").get("TypeA"))
        expect(state.get("cacheObjectsByType").get("TypeB")).toBe(inputState.get("cacheObjectsByType").get("TypeB"))
        expect(state.get("cacheObjectsByType").get("TypeC")).toBe(inputState.get("cacheObjectsByType").get("TypeC"))
    })

    test("should return smaller array of types if removes result in no objects for a previous type", () => {

        const inputState = exampleState
        const changes = {

            puts:    [] as Array<CacheObject>,
            removes: [
                {
                    id: "A_1"
                },
                {
                    id: "A_2"
                }
            ]
        }
        const operation = operations.onChangeSetReceived(changes)
        const expectedState = Map({
                    cacheObjectTypes: ["TypeB", "TypeC"],    
                    cacheObjectsByType: Map({
                        TypeB: [flattened_cacheObject_B1, flattened_cacheObject_B2],
                        TypeC: [flattened_cacheObject_C1, flattened_cacheObject_C2]
                    }).toJS(),
                    cacheObjectData: Map({
                        TypeB: Map({
                            B1: flattened_cacheObject_B1,
                            B2: flattened_cacheObject_B2
                        }),
                        TypeC: Map({
                            C1: flattened_cacheObject_C1,
                            C2: flattened_cacheObject_C2
                        })
                    }) 
                } as State) as ImmutableState

        const state = reducer(inputState, operation)

        expect(state.toJS()).toEqual(expectedState.toJS())

        // Arrays of objects for types should be re-used as is if no changes for that type have occurred
        expect(state.get("cacheObjectsByType").get("TypeB")).toBe(inputState.get("cacheObjectsByType").get("TypeB"))
        expect(state.get("cacheObjectsByType").get("TypeC")).toBe(inputState.get("cacheObjectsByType").get("TypeC"))
    })

    test("should handle full combinations of puts and removes", () => {

        const inputState = exampleState
        const changes = {

            puts:    [
                cacheObject_D1,
                cacheObject_B1_update,
                cacheObject_C1_update,
                cacheObject_A1_update
            ],
            removes: [
                {
                    id: "C2"
                }       
            ]
        }
        const operation = operations.onChangeSetReceived(changes)
        const expectedState = Map({
                    cacheObjectTypes: ["TypeA", "TypeB", "TypeC","TypeD"],    
                    cacheObjectsByType: Map({
                        TypeA: [flattened_cacheObject_A1_update, flattened_cacheObject_A2],
                        TypeB: [flattened_cacheObject_B1_update, flattened_cacheObject_B2],
                        TypeC: [flattened_cacheObject_C1_update],
                        TypeD: [flattened_cacheObject_D1]
                    }),
                    cacheObjectData: Map({
                        TypeA: Map({
                            A_1: flattened_cacheObject_A1_update,
                            A_2: flattened_cacheObject_A2
                        }),
                        TypeB: Map({
                            B1: flattened_cacheObject_B1_update,
                            B2: flattened_cacheObject_B2
                        }),
                        TypeC: Map({
                            C1: flattened_cacheObject_C1_update
                        }),
                        TypeD: Map({
                            D1: flattened_cacheObject_D1
                        })
                    }) 
                } as State) as ImmutableState

        const state = reducer(inputState, operation)

        expect(state.toJS()).toEqual(expectedState.toJS())
    })
})
