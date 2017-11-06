// Author: Richard Bradford

import reducer, {flattenCacheObject, initialState} from "./reducers"
import TypeKeys, {CacheRemove} from "./types"
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
                id:       "C1",
                type:     "TypeC",
                content: JSON.stringify(cacheObject_C1.content)
            })
        })
    })

    test("should return the initial state", () => {

        expect(reducer(undefined, { type: TypeKeys.OTHER_ACTION })).toEqual(initialState)
    })

    test("should add puts to initial state", () => {

        const changes = {
            
            puts: [
                cacheObject_A1,
                cacheObject_A2,
                cacheObject_B1,
                cacheObject_B2,
                cacheObject_C1,
                cacheObject_C2
            ],
            removes: [] as Array<CacheRemove>
        }
        
        const operation = operations.onChangeSetReceived(changes)
        
        let state = initialState
        
        state = reducer(state, operation)
        
        expect(state.cacheObjectTypes).toEqual(["TypeA", "TypeB", "TypeC"])
        expect(state.cacheObjectsByType).toEqual(Map({
            TypeA: [flattened_cacheObject_A1, flattened_cacheObject_A2],
            TypeB: [flattened_cacheObject_B1, flattened_cacheObject_B2],
            TypeC: [flattened_cacheObject_C1, flattened_cacheObject_C2]
        }))
        expect(state.cacheObjectData).toEqual(Map({
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
        }))
    })

    test("should remove removes from state", () => {
        
    })

    test("should handle removes that are for objects not present in state", () => {
        
    })

    test("should always return objects for each type as array sorted by object id", () => {
        
    })

    test("should return same array of types if types do not change after processing change set", () => {
        
    })

    test("should return same array of objects for a type if objects for that type are not changed after processing change set", () => {
        
    })
})
