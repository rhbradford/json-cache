// Author: Richard Bradford

import {CacheObject, CacheObjectContentType} from "./types"

const isObject = (o: any): boolean => {

    return (typeof o === 'object' && !Array.isArray(o) && o !== null)
}

const isArray = (o: any): boolean => {

    return Array.isArray(o)
}

export const cacheObjectContentType = (obj: CacheObject): CacheObjectContentType => {
    
    return cacheObjectColumnContentType(obj.content)
}

export const cacheObjectColumnContentType = (columnExample: any): CacheObjectContentType => {
    
    switch(typeof columnExample) {
      
        case "string":
            return CacheObjectContentType.string
        case "number":
            return CacheObjectContentType.number
        case "boolean":
            return CacheObjectContentType.boolean
            
        default:    
            if(isObject(columnExample))
                return CacheObjectContentType.object
            else if(isArray(columnExample))
                return CacheObjectContentType.array
            else
                return undefined
    } 
}
