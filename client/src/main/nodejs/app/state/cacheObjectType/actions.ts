// Author: Richard Bradford

import TypeKeys from "./types"

export interface OtherAction {
    
    readonly type: TypeKeys.OTHER_ACTION
}

export interface TypeSelectedAction {

    readonly type: TypeKeys.TYPE_SELECTED,
    readonly cacheObjectType: string
}

export type ActionTypes = 
    | TypeSelectedAction
    | OtherAction

const onTypeSelected = (cacheObjectType: string): TypeSelectedAction => ({
    
    type: TypeKeys.TYPE_SELECTED,
    cacheObjectType
})

export default {

    onTypeSelected
}