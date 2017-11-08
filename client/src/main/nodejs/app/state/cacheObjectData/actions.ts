// Author: Richard Bradford

import TypeKeys, {CacheChangeSet} from "./types"

export interface OtherAction {
    
    readonly type: TypeKeys.OTHER_ACTION
}

export interface ChangeSetReceivedAction {

    readonly type: TypeKeys.CHANGE_SET_RECEIVED,
    readonly changes: CacheChangeSet
}

export type ActionTypes = 
    | ChangeSetReceivedAction
    | OtherAction

const onChangeSetReceived = (changes: CacheChangeSet): ChangeSetReceivedAction => ({
    
    type: TypeKeys.CHANGE_SET_RECEIVED,
    changes
})

export default {

    onChangeSetReceived
}