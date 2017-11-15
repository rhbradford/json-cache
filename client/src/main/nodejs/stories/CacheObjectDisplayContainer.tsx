// Author: Richard Bradford

import * as React from "react"
import {Provider} from "react-redux"
import {combineReducers, createStore} from "redux"
import {devToolsEnhancer} from "redux-devtools-extension"
import {storiesOf} from "@storybook/react"
import {Sticky, Grid, Button} from "semantic-ui-react"

import "../node_modules/ag-grid/dist/styles/ag-grid.css"
import "../node_modules/ag-grid/dist/styles/theme-dark.css"

import {State} from "../app/state"
import DataReducer, {operations as DataOps} from "../app/state/cacheObjectData"
import {CacheChangeSet, CacheRemove} from "../app/state/cacheObjectData/types"
import CacheObjectDisplayContainer from "../app/views/containers/CacheObjectDisplayContainer"

const rootReducer = (state: State, action: any): State => {
    return ({
        ...state,
        cacheObjectData: DataReducer(state.cacheObjectData, action)
    })
}

const store = createStore(
    rootReducer,
    {
        cacheObjectData: undefined,
        cacheObjectType: {
            selectedType: "X"
        }
    } as State,
    devToolsEnhancer({})
)

const tickData: CacheChangeSet = {

    puts:    [
        {
            id:      "object_1",
            type:    "X",
            content: {
                name: "Fred",
                age:  24
            }
        },
        {
            id:      "object_2",
            type:    "X",
            content: {
                name: "Wilma",
                age:  25
            }
        }
    ],
    removes: [
        {
            id: "object_3"
        }
    ]
}

const tockData: CacheChangeSet = {

    puts:    [
        {
            id:      "object_1",
            type:    "X",
            content: {
                name: "Fred",
                age:  32
            }
        },
        {
            id:      "object_2",
            type:    "X",
            content: {
                name: "Wilma",
                age:  33
            }
        },
        {
            id:      "object_3",
            type:    "X",
            content: {
                name: "Baby",
                age:  1
            }
        }
    ],
    removes: [] as Array<CacheRemove>
}

const Wrapper = () => {
//   return <CacheObjectDisplayContainer/> 
//     return (
//         <Sticky>
//             <Grid>
//                 <Grid.Row stretched>
//                     <Button.Group>
//                         <Button content="Tick" onClick={(e, d) => {
//                             e.preventDefault()
//                             store.dispatch(DataOps.onChangeSetReceived(tickData))
//                         }}/>
//                         <Button content="Tock" onClick={(e, d) => {
//                             e.preventDefault()
//                             store.dispatch(DataOps.onChangeSetReceived(tockData))
//                         }}/>
//                     </Button.Group>
//                 </Grid.Row>
//             </Grid>
//         </Sticky>
//     )
    return (
        <Sticky>
            <Grid columns={16}>
                <Grid.Column width={4}>
                    <Button.Group>
                        <Button content="Tick" onClick={(e, d) => {
                            e.preventDefault()
                            store.dispatch(DataOps.onChangeSetReceived(tickData))
                        }}/>
                        <Button content="Tock" onClick={(e, d) => {
                            e.preventDefault()
                            store.dispatch(DataOps.onChangeSetReceived(tockData))
                        }}/>
                    </Button.Group>
                </Grid.Column>
                <Grid.Column width={12}>
                    <CacheObjectDisplayContainer/>
                </Grid.Column>
            </Grid>
        </Sticky>
    )
}

storiesOf("CacheObjectDisplayContainer", module)
    .addDecorator(story =>
        <Provider store={store}>
            {story()}
        </Provider>    
    )
    .add("Basic", () => (<Wrapper/>))
