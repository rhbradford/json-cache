// Author: Richard Bradford

import {Map as ImmutableMap} from "immutable"

import TypeKeys, {CacheObject, FlattenedCacheObject, FlattenedCacheObjectColumn} from "./types"
import {ActionTypes} from "./actions"

export type Type = string
export type Id = string

export interface StateData {

    readonly cacheObjectTypes: Array<Type>,
    readonly cacheObjectData: ImmutableMap<Type, ImmutableMap<Id, FlattenedCacheObject>>,
    readonly cacheObjectsByType: ImmutableMap<Type, Array<FlattenedCacheObject>>
    readonly cacheObjectColumnsByType: ImmutableMap<Type, Array<FlattenedCacheObjectColumn>>
}

export interface State extends ImmutableMap<string, any> {

    get<K extends keyof StateData>(key: K): StateData[K]

    set<K extends keyof StateData, V extends StateData[K]>(key: K, value: V): State
}

export const initialState: () => State = () => ImmutableMap({
    cacheObjectTypes:         [],
    cacheObjectData:          ImmutableMap(),
    cacheObjectsByType:       ImmutableMap(),
    cacheObjectColumnsByType: ImmutableMap()
} as StateData)

const isObject = (o: any): boolean => {

    return (typeof o === 'object' && !Array.isArray(o) && o !== null)
}

const isArray = (o: any): boolean => {

    return Array.isArray(o)
}

export const flattenCacheObject = (cacheObject: CacheObject): FlattenedCacheObject => {

    // noinspection JSMismatchedCollectionQueryUpdate
    const keys: { [others: string]: any } = {}

    if(isObject(cacheObject.content)) {

        for(let key in cacheObject.content) {

            if(cacheObject.content.hasOwnProperty(key)) {

                let value = cacheObject.content[key]
                if(isArray(value) || isObject(value))
                    keys[key] = JSON.stringify(value)
                else
                    keys[key] = value
            }
        }

    }
    else {

        keys["content"] = JSON.stringify(cacheObject.content)
    }

    return {
        id:   cacheObject.id,
        type: cacheObject.type,
        ...keys
    }
}

const reducer = (state: State = initialState(), action: ActionTypes): State => {

    switch(action.type) {

        case TypeKeys.CHANGE_SET_RECEIVED:
            let nextState = state
            let objectsByType = state.get("cacheObjectsByType")
            let types = state.get("cacheObjectTypes")
            let objectData = state.get("cacheObjectData")
            let columnsByType = state.get("cacheObjectColumnsByType")

            const typesTouched: Set<string> = new Set()
            const typesToBeRemoved: Set<string> = new Set()
            // noinspection JSMismatchedCollectionQueryUpdate
            const typesToBeAdded: Set<string> = new Set()
            const { puts, removes } = action.changes

            const keysCache = new Map<string, Set<string>>()

            for(let put of puts) {

                const type = put.type
                typesTouched.add(type)
                typesToBeRemoved.delete(type)

                let newType = false

                if(!objectData.has(type)) {

                    newType = true
                    typesToBeAdded.add(type)
                }

                objectData = objectData.setIn([type, put.id], flattenCacheObject(put))

                const fco = objectData.getIn([type, put.id])
                const fcoKeys: Array<string> = Object.keys(fco)
                const fcoKeysAsString = fcoKeys.toString()

                if(newType) {

                    const columnDefs: Array<FlattenedCacheObjectColumn> = fcoKeys.sort().map(key => {
                        return {
                            headerName: key,
                            field:      key
                        }
                    })

                    keysCache.set(type, new Set([fcoKeysAsString]))
                    columnsByType = columnsByType.set(type, columnDefs)
                }
                else {

                    if(!keysCache.has(type) || !keysCache.get(type).has(fcoKeysAsString)) {

                        const columnDefs = columnsByType.get(type)
                        const currentKeys = columnDefs.map(columnDef => columnDef.field)

                        const allKeys = new Set(currentKeys)
                        for(let key of fcoKeys) {
                            allKeys.add(key)
                        }

                        if(allKeys.size != currentKeys.length) {

                            const newColumnDefs = Array.from(allKeys).sort().map(key => ({
                                headerName: key,
                                field:      key
                            }))

                            columnsByType = columnsByType.set(type, newColumnDefs)

                            if(keysCache.has(type))
                                keysCache.get(type).add(fcoKeysAsString)
                            else
                                keysCache.set(type, new Set([fcoKeysAsString]))
                        }
                    }
                }
            }

            for(let remove of removes) {

                objectData.keySeq().forEach(type => {

                    if(objectData.hasIn([type, remove.id])) {

                        typesTouched.add(type)

                        objectData = objectData.removeIn([type, remove.id])

                        if(objectData.get(type).isEmpty()) {

                            typesTouched.delete(type)
                            typesToBeRemoved.add(type)
                            typesToBeAdded.delete(type)

                            objectData = objectData.remove(type)
                        }
                    }
                })
            }

            if(typesToBeAdded.size > 0 || typesToBeRemoved.size > 0)
                types = objectData.keySeq().toArray().sort();

            for(let type of typesToBeRemoved) {

                objectsByType = objectsByType.remove(type)
                columnsByType = columnsByType.remove(type)
            }

            for(let type of typesTouched) {

                const sortedIds = objectData.get(type).keySeq().toArray().sort()
                const objects = sortedIds.map(id => {
                    return objectData.getIn([type, id])
                })

                objectsByType = objectsByType.set(type, objects)
            }

            nextState = nextState.set("cacheObjectData", objectData)
            nextState = nextState.set("cacheObjectTypes", types)
            nextState = nextState.set("cacheObjectColumnsByType", columnsByType)
            nextState = nextState.set("cacheObjectsByType", objectsByType)

            return nextState

        case TypeKeys.CLEAR_DATA:
            return initialState()

        default:
            return state
    }
}

export default reducer