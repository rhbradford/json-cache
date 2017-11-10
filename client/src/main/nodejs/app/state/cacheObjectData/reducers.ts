// Author: Richard Bradford

import {Map} from "immutable"

import TypeKeys, {CacheObject, FlattenedCacheObject} from "./types"
import {ActionTypes} from "./actions"

export type Type = string
export type Id = string

export interface State {
    
    readonly cacheObjectTypes: Array<Type>,
    readonly cacheObjectData: Map<Type,Map<Id,FlattenedCacheObject>>,
    readonly cacheObjectsByType: Map<Type,Array<FlattenedCacheObject>>
}

export interface ImmutableState extends Map<string, any> {
    
    get<K extends keyof State>(key: K): State[K]
    set<K extends keyof State, V extends State[K]>(key: K, value: V): ImmutableState
}

export const initialState: ImmutableState = Map({
    cacheObjectTypes:   [],
    cacheObjectData:    Map(),
    cacheObjectsByType: Map()
} as State)

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
        
    } else {
        
        keys["content"] = JSON.stringify(cacheObject.content)
    }
    
    return {
        id: cacheObject.id,
        type: cacheObject.type,
        ...keys
    }
}
 

const reducer = (state: ImmutableState = initialState, action: ActionTypes): ImmutableState => {
 
    switch(action.type) {
        
        case TypeKeys.CHANGE_SET_RECEIVED:
            let nextState = state
            let objectsByType = state.get("cacheObjectsByType")
            let types = state.get("cacheObjectTypes")
            let objectData = state.get("cacheObjectData")

            const typesTouched: Set<string> = new Set()
            const typesToBeRemoved: Set<string> = new Set()
            // noinspection JSMismatchedCollectionQueryUpdate
            const typesToBeAdded: Set<string> = new Set()
            const { puts, removes } = action.changes

            for(let put of puts) {
                
                const type = put.type
                typesTouched.add(type)
                typesToBeRemoved.delete(type)
                
                if(!objectData.has(type))
                    typesToBeAdded.add(type)
                
                objectData = objectData.setIn([type, put.id], flattenCacheObject(put))
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
            }
            
            for(let type of typesTouched) {
                
                const sortedIds = objectData.get(type).keySeq().toArray().sort() 
                const objects = sortedIds.map(id => { return objectData.getIn([type, id]) })
                
                objectsByType = objectsByType.set(type, objects)
            }
            
            nextState = nextState.set("cacheObjectData", objectData)
            nextState = nextState.set("cacheObjectsByType", objectsByType)
            nextState = nextState.set("cacheObjectTypes", types)
            
            return nextState            
        default:
            return state
    }
}

export default reducer