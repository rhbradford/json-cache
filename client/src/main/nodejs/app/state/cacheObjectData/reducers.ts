// Author: Richard Bradford

import {Map} from "immutable"
import * as _ from "lodash"

import TypeKeys, {CacheObject, FlattenedCacheObject} from "./types"
import {ActionTypes} from "./actions"

export type Type = string
export type Id = string

export interface State {
    
    cacheObjectTypes: Array<Type>,
    cacheObjectData: Map<Type,Map<Id,FlattenedCacheObject>>,
    cacheObjectsByType: Map<Type,Array<FlattenedCacheObject>>
}

export const initialState: State = {
    
    cacheObjectTypes:   [],
    cacheObjectData:    Map(),
    cacheObjectsByType: Map()
}

export const flattenCacheObject = (cacheObject: CacheObject): FlattenedCacheObject => {

    const isObject = (o: any): boolean => {
        
        return (typeof o === 'object' && !Array.isArray(o) && o !== null)
    }
    
    const isArray = (o: any): boolean => {
        
        return Array.isArray(o)
    }
    
    const fco: FlattenedCacheObject = {
        id: cacheObject.id,
        type: cacheObject.type
    }
    
    if(isObject(cacheObject.content)) {
        
        for(let key in cacheObject.content) {
            
            if(cacheObject.content.hasOwnProperty(key)) {
                
                let value = cacheObject.content[key]
                if(isArray(value) || isObject(value))
                    fco[key] = JSON.stringify(value)
                else
                    fco[key] = value
            }
        }
        
    } else {
        
        fco["content"] = JSON.stringify(cacheObject.content)
    }
    
    return fco
}
 

const changeSetReceivedReducer = (state: State = initialState, action: ActionTypes): State => {
 
    switch(action.type) {
        
        case TypeKeys.CHANGE_SET_RECEIVED:
            let objectsByType = state.cacheObjectsByType
            let types = state.cacheObjectTypes
            let objectData = state.cacheObjectData

            const typesSeen: Set<string> = new Set()
            const { puts, removes } = action.changes

            for(let put of puts) {
                
                const type = put.type
                typesSeen.add(type)
                objectData = objectData.setIn([type, put.id], flattenCacheObject(put))
            }
            
            for(let remove of removes) {
                
                for(let type in objectData.keys()) {
                    
                    if(objectData.hasIn([type, remove.id])) {
                        
                        typesSeen.add(type)
                        objectData = objectData.removeIn([type, remove.id])
                    }
                }
            }
            
            if(!_.isEqual(new Set(state.cacheObjectTypes), typesSeen)) {

                types = Array.from(typesSeen).sort()
            }

            for(let type of typesSeen) {
                
                const sortedIds = objectData.get(type).keySeq().toArray().sort() 
                const objects = sortedIds.map(id => { return objectData.getIn([type, id]) })
                objectsByType = objectsByType.set(type, objects)
            }
            
            return { 
                cacheObjectData: objectData,
                cacheObjectsByType: objectsByType,
                cacheObjectTypes: types
            }
            
        default:
            return state
    }
}

export default changeSetReceivedReducer