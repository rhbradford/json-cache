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

export enum CacheObjectContentType {
    
    string = "string",
    number = "number",
    boolean = "boolean",
    array = "array",
    object = "object"
}

export interface CacheObjectColumn {
    
    readonly name: string,
    readonly type: CacheObjectContentType
}
