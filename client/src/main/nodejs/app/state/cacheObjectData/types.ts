// Author: Richard Bradford

export interface CacheObject {
    
    id: string,
    type: string,
    content: any
}

export interface CacheRemove {
    
    id: string
}

export interface CacheChangeSet {
    
    puts: Array<CacheObject>,
    removes: Array<CacheRemove>
}

export interface FlattenedCacheObject {
    
    id: string,
    type: string,
    [others: string] : any
}

enum TypeKeys {

    CHANGE_SET_RECEIVED = "CHANGE_SET_RECEIVED",
    OTHER_ACTION = "__any_other_action_type"
}

export default TypeKeys