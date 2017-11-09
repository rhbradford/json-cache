// Author: Richard Bradford

import TypeKeys from "./types"

export interface OtherAction {
    
    readonly type: TypeKeys.OTHER_ACTION
}

export interface SetURLAction {

    readonly type: TypeKeys.SET_URL,
    readonly url: string
}

export type ActionTypes = 
    | SetURLAction
    | OtherAction

const setURL = (url: string): SetURLAction => ({
  
    type: TypeKeys.SET_URL,
    url
})

export default {
    
    setURL
}