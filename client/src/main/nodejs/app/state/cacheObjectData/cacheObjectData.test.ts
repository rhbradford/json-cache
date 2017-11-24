// Author: Richard Bradford

import reducer, {State, initialState, StateData} from "./reducers"
import TypeKeys from "./types"
import {CacheObject, CacheRemove, CacheObjectContentType} from "../../model/types"
import operations from "./operations"
import {Map} from "immutable"

const object_A_Columns = [
    {
        name: "location",
        type: CacheObjectContentType.object
    },
    {
        name: "price",
        type: CacheObjectContentType.number
    },
    {
        name: "product",
        type: CacheObjectContentType.string
    }
]
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

const object_B_Columns = [
    {
        name: "age",
        type: CacheObjectContentType.number
    },
    {
        name: "name",
        type: CacheObjectContentType.string
    }
]
const cacheObject_B1 = {
    id:      "B1",
    type:    "TypeB",
    content: {
        name: "Fred",
        age:  64
    }
}
const cacheObject_B1_update = {
    id:      "B1",
    type:    "TypeB",
    content: {
        name: "Fred",
        age:  65
    }
}
const cacheObject_B2 = {
    id:      "B2",
    type:    "TypeB",
    content: {
        name: "James",
        age:  22
    }
}

const object_C_Columns = [
    {
        name: "content",
        type: CacheObjectContentType.array
    }
]
const cacheObject_C1 = {
    id:      "C1",
    type:    "TypeC",
    content: ["some", "stuff"]
}
const cacheObject_C1_update = {
    id:      "C1",
    type:    "TypeC",
    content: ["more", "stuff"]
}
const cacheObject_C2 = {
    id:      "C2",
    type:    "TypeC",
    content: ["other", "things"]
}

const object_D_Columns = [
    {
        name: "content",
        type: CacheObjectContentType.string
    }
]
const cacheObject_D1 = {
    id:      "D1",
    type:    "TypeD",
    content: "text"
}

const cacheObject_E1 = {
    id:      "E1",
    type:    "TypeE",
    content: {
        name: "Fred"
    }
}
const cacheObject_E2 = {
    id:      "E2",
    type:    "TypeE",
    content: {
        name: "Wilma",
        age:  72
    }
}
const cacheObject_E3 = {
    id:      "E3",
    type:    "TypeE",
    content: {
        age: 18
    }
}
const object_E_Columns = [
    {
        name: "age",
        type: CacheObjectContentType.number
    },
    {
        name: "name",
        type: CacheObjectContentType.string
    }
]

const exampleState: State = Map({
    cacheObjectTypes:         ["TypeA", "TypeB", "TypeC"],
    cacheObjectContentTypes: Map({
        TypeA: CacheObjectContentType.object,
        TypeB: CacheObjectContentType.object,
        TypeC: CacheObjectContentType.array
    }),
    cacheObjectsByType:       Map({
        TypeA: [cacheObject_A1, cacheObject_A2],
        TypeB: [cacheObject_B1, cacheObject_B2],
        TypeC: [cacheObject_C1, cacheObject_C2]
    }),
    cacheObjectColumnsByType: Map({
        TypeA: object_A_Columns,
        TypeB: object_B_Columns,
        TypeC: object_C_Columns
    }),
    cacheObjectData:          Map({
        TypeA: Map({
            A_1: cacheObject_A1,
            A_2: cacheObject_A2
        }),
        TypeB: Map({
            B1: cacheObject_B1,
            B2: cacheObject_B2
        }),
        TypeC: Map({
            C1: cacheObject_C1,
            C2: cacheObject_C2
        })
    })
} as StateData)

const fullExampleState = (): State => {
    
    const inputState = initialState()
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

    return reducer(inputState, operation)
}

const removeKeysCache = (state: State): State => {
    
    return state.set("cacheObjectContentKeysCache", undefined)
}

describe("reducer", () => {

    test("should return the initial state", () => {

        expect(reducer(undefined, { type: TypeKeys.OTHER_ACTION })).toEqual(initialState())
    })

    test("should add puts to initial state", () => {

        expect(removeKeysCache(fullExampleState()).toJS()).toEqual(exampleState.toJS())
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
            cacheObjectTypes:         ["TypeA", "TypeB", "TypeC", "TypeD"],
            cacheObjectContentTypes: Map({
                TypeA: CacheObjectContentType.object,
                TypeB: CacheObjectContentType.object,
                TypeC: CacheObjectContentType.array,
                TypeD: CacheObjectContentType.string
            }),
            cacheObjectsByType:       Map({
                TypeA: [cacheObject_A1, cacheObject_A2],
                TypeB: [cacheObject_B1, cacheObject_B2],
                TypeC: [cacheObject_C1, cacheObject_C2],
                TypeD: [cacheObject_D1]
            }),
            cacheObjectColumnsByType: Map({
                TypeA: object_A_Columns,
                TypeB: object_B_Columns,
                TypeC: object_C_Columns,
                TypeD: object_D_Columns
            }),
            cacheObjectData:          Map({
                TypeA: Map({
                    A_1: cacheObject_A1,
                    A_2: cacheObject_A2
                }),
                TypeB: Map({
                    B1: cacheObject_B1,
                    B2: cacheObject_B2
                }),
                TypeC: Map({
                    C1: cacheObject_C1,
                    C2: cacheObject_C2
                }),
                TypeD: Map({
                    D1: cacheObject_D1
                })
            })
        } as StateData) as State

        const state = reducer(inputState, operation)

        expect(removeKeysCache(state).toJS()).toEqual(expectedState.toJS())

        // Arrays of objects for types should be re-used as is if no changes for that type have occurred
        expect(state.get("cacheObjectsByType").get("TypeA")).toBe(inputState.get("cacheObjectsByType").get("TypeA"))
        expect(state.get("cacheObjectsByType").get("TypeB")).toBe(inputState.get("cacheObjectsByType").get("TypeB"))
        expect(state.get("cacheObjectsByType").get("TypeC")).toBe(inputState.get("cacheObjectsByType").get("TypeC"))

        // Arrays of object columns for types should be re-used as is if no changes for that type have occurred
        expect(state.get("cacheObjectColumnsByType").get("TypeA")).toBe(inputState.get("cacheObjectColumnsByType").get("TypeA"))
        expect(state.get("cacheObjectColumnsByType").get("TypeB")).toBe(inputState.get("cacheObjectColumnsByType").get("TypeB"))
        expect(state.get("cacheObjectColumnsByType").get("TypeC")).toBe(inputState.get("cacheObjectColumnsByType").get("TypeC"))
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
            cacheObjectTypes:         ["TypeA", "TypeB", "TypeC"],
            cacheObjectContentTypes: Map({
                TypeA: CacheObjectContentType.object,
                TypeB: CacheObjectContentType.object,
                TypeC: CacheObjectContentType.array
            }),
            cacheObjectsByType:       Map({
                TypeA: [cacheObject_A2],
                TypeB: [cacheObject_B1, cacheObject_B2],
                TypeC: [cacheObject_C2]
            }),
            cacheObjectColumnsByType: Map({
                TypeA: object_A_Columns,
                TypeB: object_B_Columns,
                TypeC: object_C_Columns
            }),
            cacheObjectData:          Map({
                TypeA: Map({
                    A_2: cacheObject_A2
                }),
                TypeB: Map({
                    B1: cacheObject_B1,
                    B2: cacheObject_B2
                }),
                TypeC: Map({
                    C2: cacheObject_C2
                })
            })
        } as StateData) as State

        const state = reducer(inputState, operation)

        expect(removeKeysCache(state).toJS()).toEqual(expectedState.toJS())

        // Array of types should be re-used as is if no changes to types present have occurred 
        expect(state.get("cacheObjectTypes")).toBe(inputState.get("cacheObjectTypes"))

        // Arrays of objects for types should be re-used as is if no changes for that type have occurred
        expect(state.get("cacheObjectsByType").get("TypeB")).toBe(inputState.get("cacheObjectsByType").get("TypeB"))

        // Arrays of object columns for types should be re-used as is if no changes for that type have occurred
        expect(state.get("cacheObjectColumnsByType").get("TypeA")).toBe(inputState.get("cacheObjectColumnsByType").get("TypeA"))
        expect(state.get("cacheObjectColumnsByType").get("TypeB")).toBe(inputState.get("cacheObjectColumnsByType").get("TypeB"))
        expect(state.get("cacheObjectColumnsByType").get("TypeC")).toBe(inputState.get("cacheObjectColumnsByType").get("TypeC"))
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
            cacheObjectTypes:         ["TypeA", "TypeB", "TypeC"],
            cacheObjectContentTypes: Map({
                TypeA: CacheObjectContentType.object,
                TypeB: CacheObjectContentType.object,
                TypeC: CacheObjectContentType.array
            }),
            cacheObjectsByType:       Map({
                TypeA: [cacheObject_A1, cacheObject_A2],
                TypeB: [cacheObject_B1, cacheObject_B2],
                TypeC: [cacheObject_C1, cacheObject_C2]
            }),
            cacheObjectColumnsByType: Map({
                TypeA: object_A_Columns,
                TypeB: object_B_Columns,
                TypeC: object_C_Columns
            }),
            cacheObjectData:          Map({
                TypeA: Map({
                    A_1: cacheObject_A1,
                    A_2: cacheObject_A2
                }),
                TypeB: Map({
                    B1: cacheObject_B1,
                    B2: cacheObject_B2
                }),
                TypeC: Map({
                    C1: cacheObject_C1,
                    C2: cacheObject_C2
                })
            })
        } as StateData) as State

        const state = reducer(inputState, operation)

        expect(removeKeysCache(state).toJS()).toEqual(expectedState.toJS())

        // Array of types should be re-used as is if no changes to types present have occurred 
        expect(state.get("cacheObjectTypes")).toBe(inputState.get("cacheObjectTypes"))

        // Arrays of objects for types should be re-used as is if no changes for that type have occurred
        expect(state.get("cacheObjectsByType").get("TypeA")).toBe(inputState.get("cacheObjectsByType").get("TypeA"))
        expect(state.get("cacheObjectsByType").get("TypeB")).toBe(inputState.get("cacheObjectsByType").get("TypeB"))
        expect(state.get("cacheObjectsByType").get("TypeC")).toBe(inputState.get("cacheObjectsByType").get("TypeC"))

        // Arrays of object columns for types should be re-used as is if no changes for that type have occurred
        expect(state.get("cacheObjectColumnsByType").get("TypeA")).toBe(inputState.get("cacheObjectColumnsByType").get("TypeA"))
        expect(state.get("cacheObjectColumnsByType").get("TypeB")).toBe(inputState.get("cacheObjectColumnsByType").get("TypeB"))
        expect(state.get("cacheObjectColumnsByType").get("TypeC")).toBe(inputState.get("cacheObjectColumnsByType").get("TypeC"))
    })

    test("should return smaller array of types if removes result in no objects for a previous type", () => {

        const inputState = fullExampleState()
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
            cacheObjectTypes:         ["TypeB", "TypeC"],
            cacheObjectContentTypes: Map({
                TypeB: CacheObjectContentType.object,
                TypeC: CacheObjectContentType.array
            }),
            cacheObjectsByType:       Map({
                TypeB: [cacheObject_B1, cacheObject_B2],
                TypeC: [cacheObject_C1, cacheObject_C2]
            }).toJS(),
            cacheObjectColumnsByType: Map({
                TypeB: object_B_Columns,
                TypeC: object_C_Columns
            }),
            cacheObjectData:          Map({
                TypeB: Map({
                    B1: cacheObject_B1,
                    B2: cacheObject_B2
                }),
                TypeC: Map({
                    C1: cacheObject_C1,
                    C2: cacheObject_C2
                })
            })
        } as StateData) as State

        const state = reducer(inputState, operation)

        expect(removeKeysCache(state).toJS()).toEqual(expectedState.toJS())

        // Arrays of objects for types should be re-used as is if no changes for that type have occurred
        expect(state.get("cacheObjectsByType").get("TypeB")).toBe(inputState.get("cacheObjectsByType").get("TypeB"))
        expect(state.get("cacheObjectsByType").get("TypeC")).toBe(inputState.get("cacheObjectsByType").get("TypeC"))

        // Arrays of object columns for types should be re-used as is if no changes for that type have occurred
        expect(state.get("cacheObjectColumnsByType").get("TypeB")).toBe(inputState.get("cacheObjectColumnsByType").get("TypeB"))
        expect(state.get("cacheObjectColumnsByType").get("TypeC")).toBe(inputState.get("cacheObjectColumnsByType").get("TypeC"))
    })

    test("should handle full combinations of puts and removes", () => {

        const inputState = fullExampleState()
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
            cacheObjectTypes:         ["TypeA", "TypeB", "TypeC", "TypeD"],
            cacheObjectContentTypes: Map({
                TypeA: CacheObjectContentType.object,
                TypeB: CacheObjectContentType.object,
                TypeC: CacheObjectContentType.array,
                TypeD: CacheObjectContentType.string
            }),
            cacheObjectsByType:       Map({
                TypeA: [cacheObject_A1_update, cacheObject_A2],
                TypeB: [cacheObject_B1_update, cacheObject_B2],
                TypeC: [cacheObject_C1_update],
                TypeD: [cacheObject_D1]
            }),
            cacheObjectColumnsByType: Map({
                TypeA: object_A_Columns,
                TypeB: object_B_Columns,
                TypeC: object_C_Columns,
                TypeD: object_D_Columns
            }),
            cacheObjectData:          Map({
                TypeA: Map({
                    A_1: cacheObject_A1_update,
                    A_2: cacheObject_A2
                }),
                TypeB: Map({
                    B1: cacheObject_B1_update,
                    B2: cacheObject_B2
                }),
                TypeC: Map({
                    C1: cacheObject_C1_update
                }),
                TypeD: Map({
                    D1: cacheObject_D1
                })
            })
        } as StateData) as State

        const state = reducer(inputState, operation)

        expect(removeKeysCache(state).toJS()).toEqual(expectedState.toJS())
    })

    test("should accumulate columns as content for a type alters over successive puts", () => {

        const inputState = fullExampleState()
        const change1 = {

            puts:    [
                cacheObject_E1
            ],
            removes: [] as Array<CacheRemove>
        }
        const change2 = {

            puts:    [
                cacheObject_E2
            ],
            removes: [] as Array<CacheRemove>
        }
        const change3 = {

            puts:    [
                cacheObject_E3
            ],
            removes: [] as Array<CacheRemove>
        }
        const operation1 = operations.onChangeSetReceived(change1)
        const operation2 = operations.onChangeSetReceived(change2)
        const operation3 = operations.onChangeSetReceived(change3)
        const expectedState = Map({
            cacheObjectTypes:         ["TypeA", "TypeB", "TypeC", "TypeE"],
            cacheObjectContentTypes: Map({
                TypeA: CacheObjectContentType.object,
                TypeB: CacheObjectContentType.object,
                TypeC: CacheObjectContentType.array,
                TypeE: CacheObjectContentType.object
            }),
            cacheObjectsByType:       Map({
                TypeA: [cacheObject_A1, cacheObject_A2],
                TypeB: [cacheObject_B1, cacheObject_B2],
                TypeC: [cacheObject_C1, cacheObject_C2],
                TypeE: [cacheObject_E1, cacheObject_E2, cacheObject_E3]
            }),
            cacheObjectColumnsByType: Map({
                TypeA: object_A_Columns,
                TypeB: object_B_Columns,
                TypeC: object_C_Columns,
                TypeE: object_E_Columns
            }),
            cacheObjectData:          Map({
                TypeA: Map({
                    A_1: cacheObject_A1,
                    A_2: cacheObject_A2
                }),
                TypeB: Map({
                    B1: cacheObject_B1,
                    B2: cacheObject_B2
                }),
                TypeC: Map({
                    C1: cacheObject_C1,
                    C2: cacheObject_C2
                }),
                TypeE: Map({
                    E1: cacheObject_E1,
                    E2: cacheObject_E2,
                    E3: cacheObject_E3
                })
            })
        } as StateData) as State

        const state1 = reducer(inputState, operation1)
        const state2 = reducer(state1, operation2)
        const state3 = reducer(state2, operation3)

        expect(removeKeysCache(state3).toJS()).toEqual(expectedState.toJS())

        // If no new columns seen, object columns should be returned as is
        expect(state3.get("cacheObjectColumnsByType").get("TypeE")).toBe(state2.get("cacheObjectColumnsByType").get("TypeE"))
    })

    test("should clear data when requested", () => {

        const inputState = fullExampleState()
        const operation = operations.clearData()
        const expectedState = initialState()
        const state = reducer(inputState, operation)

        expect(state.toJS()).toEqual(expectedState.toJS())
    })
})
