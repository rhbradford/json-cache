// Author: Richard Bradford

import TypeKeys from "./types"

export interface OtherAction {

    readonly type: TypeKeys.OTHER_ACTION
}

export interface FilterSetAction {

    readonly type: TypeKeys.FILTER_SET,
    readonly cacheObjectFilter: string
}

export type ActionTypes =
    | FilterSetAction
    | OtherAction

const onFilterSet = (cacheObjectFilter: string): FilterSetAction => ({

    type: TypeKeys.FILTER_SET,
    cacheObjectFilter
})

export default {

    onFilterSet
}