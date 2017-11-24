// Author: Richard Bradford

import TypeKeys from "./types"
import {CacheChangeSet} from "../../model/types"

export interface OtherAction {
    
    readonly type: TypeKeys.OTHER_ACTION
}

export interface ChangeSetReceivedAction {

    readonly type: TypeKeys.CHANGE_SET_RECEIVED,
    readonly changes: CacheChangeSet
}

export interface ClearDataAction {

    readonly type: TypeKeys.CLEAR_DATA
}

export type ActionTypes = 
    | ChangeSetReceivedAction
    | ClearDataAction
    | OtherAction

const onChangeSetReceived = (changes: CacheChangeSet): ChangeSetReceivedAction => ({
    
    type: TypeKeys.CHANGE_SET_RECEIVED,
    changes
})

const clearData = (): ClearDataAction => ({
    
    type: TypeKeys.CLEAR_DATA
})

export default {

    onChangeSetReceived,
    clearData
}