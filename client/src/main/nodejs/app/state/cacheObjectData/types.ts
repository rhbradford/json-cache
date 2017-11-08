// Author: Richard Bradford

export interface CacheObject {
    
    readonly id: string,
    readonly type: string,
    readonly content: any
}

export interface CacheRemove {
    
    readonly id: string
}

export interface CacheChangeSet {
    
    readonly puts: Array<CacheObject>,
    readonly removes: Array<CacheRemove>
}

export interface FlattenedCacheObject {
    
    readonly id: string,
    readonly type: string,
    readonly [others: string] : any
}

enum TypeKeys {

    CHANGE_SET_RECEIVED = "CHANGE_SET_RECEIVED",
    OTHER_ACTION = "__any_other_action_type"
}

export default TypeKeys