// Author: Richard Bradford

import {Map as ImmutableMap} from "immutable"

import {CacheObject, CacheObjectColumn, CacheObjectContentType} from "../../model/types"
import {cacheObjectContentType, cacheObjectColumnContentType} from "../../model/utils"
import TypeKeys from "./types"
import {ActionTypes} from "./actions"

export type Type = string
export type Id = string

export interface StateData {

    readonly cacheObjectData: ImmutableMap<Type, ImmutableMap<Id, CacheObject>>,
    readonly cacheObjectTypes: Array<Type>,
    readonly cacheObjectContentTypes: ImmutableMap<Type, CacheObjectContentType>
    readonly cacheObjectContentKeysCache: ImmutableMap<Type, Set<string>>
    readonly cacheObjectsByType: ImmutableMap<Type, Array<CacheObject>>
    readonly cacheObjectColumnsByType: ImmutableMap<Type, Array<CacheObjectColumn>>
}

export interface State extends ImmutableMap<string, any> {

    get<K extends keyof StateData>(key: K): StateData[K]

    set<K extends keyof StateData, V extends StateData[K]>(key: K, value: V): State
}

export const initialState: () => State = () => ImmutableMap({
    cacheObjectData:             ImmutableMap(),
    cacheObjectTypes:            [],
    cacheObjectContentTypes:     ImmutableMap(),
    cacheObjectContentKeysCache: ImmutableMap(),
    cacheObjectsByType:          ImmutableMap(),
    cacheObjectColumnsByType:    ImmutableMap()
} as StateData)

const reducer = (state: State = initialState(), action: ActionTypes): State => {

    switch(action.type) {

        case TypeKeys.CHANGE_SET_RECEIVED:
            let nextState = state
            let objectData = state.get("cacheObjectData")
            let types = state.get("cacheObjectTypes")
            let contentTypes = state.get("cacheObjectContentTypes")
            let objectsByType = state.get("cacheObjectsByType")
            let columnsByType = state.get("cacheObjectColumnsByType")
            let keysCache = state.get("cacheObjectContentKeysCache")

            const typesTouched: Set<string> = new Set()
            const typesToBeRemoved: Set<string> = new Set()
            // noinspection JSMismatchedCollectionQueryUpdate
            const typesToBeAdded: Set<string> = new Set()
            const { puts, removes } = action.changes

            for(let put of puts) {

                const type = put.type
                const contentType = cacheObjectContentType(put)

                typesTouched.add(type)
                typesToBeRemoved.delete(type)

                let newType = false

                if(!objectData.has(type)) {

                    newType = true
                    typesToBeAdded.add(type)

                    contentTypes = contentTypes.set(type, contentType)
                    if(contentType != CacheObjectContentType.object) {

                        const column = {

                            name: "content",
                            type: contentType
                        }

                        columnsByType = columnsByType.set(type, [column])
                    }
                }

                objectData = objectData.setIn([type, put.id], put)

                if(contentType == CacheObjectContentType.object) {

                    const keys: Array<string> = Object.keys(put.content)
                    const keysAsString = keys.toString()

                    if(newType) {

                        const columns: Array<CacheObjectColumn> = keys.sort().map(key =>
                            ({
                                name: key,
                                type: cacheObjectColumnContentType(put.content[key])
                            })
                        )

                        keysCache = keysCache.set(type, new Set([keysAsString]))
                        columnsByType = columnsByType.set(type, columns)
                    }
                    else {

                        if(!keysCache.get(type).has(keysAsString)) {

                            const keysAsStringSet = keysCache.get(type)
                            keysAsStringSet.add(keysAsString)
                            keysCache = keysCache.set(type, keysAsStringSet)

                            const columns = columnsByType.get(type)
                            const currentKeys = columns.map(column => column.name)

                            const allKeys = new Set(currentKeys)
                            for(let key of keys) {
                                allKeys.add(key)
                            }

                            if(allKeys.size != currentKeys.length) {

                                const newColumns = Array.from(allKeys).sort().map(key =>
                                    ({
                                        name: key,
                                        type: cacheObjectColumnContentType(put.content[key])
                                    })
                                )

                                columnsByType = columnsByType.set(type, newColumns)
                            }
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
                contentTypes = contentTypes.remove(type)
                keysCache = keysCache.remove(type)
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
            nextState = nextState.set("cacheObjectContentTypes", contentTypes)
            nextState = nextState.set("cacheObjectContentKeysCache", keysCache)

            return nextState

        case TypeKeys.CLEAR_DATA:
            return initialState()

        default:
            return state
    }
}

export default reducer