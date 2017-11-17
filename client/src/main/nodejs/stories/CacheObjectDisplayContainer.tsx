// Author: Richard Bradford

import * as React from "react"
import {Provider} from "react-redux"
import {createStore} from "redux"
import {devToolsEnhancer} from "redux-devtools-extension"
import {storiesOf} from "@storybook/react"
import {Grid, Button, Sticky, Ref} from "semantic-ui-react"

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

const DataChangerWrapper = () => {

    return (
        <div>
            <Grid columns={16}>
                <Grid.Column width={4}>
                    <Button.Group fluid>
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
                <Grid.Column stretched width={12}>
                    <CacheObjectDisplayContainer/>
                </Grid.Column>
            </Grid>
            <Button fluid content="Other Space"/>
        </div>
    )
}

class LargeDataWrapper extends React.Component<{},{}> {

    offsetElement: Element
    
    render() {
        const things = ["Cup", "Spoon", "Saucer"]

        const put = (id: number) => ({
            id:      `${id}`,
            type:    "X",
            content: {
                stuff: things[id % 3]
            }
        })

        const puts = []
        for(let i = 0; i < 20; i++) {

            puts.push(put(i))
        }

        const data: CacheChangeSet = {
            puts:    puts,
            removes: [] as Array<CacheRemove>
        }

        return (
            <div>
                <Sticky>
                    <Button.Group fluid>
                        <Button content="Load" onClick={(e, d) => {
                            e.preventDefault()
                            store.dispatch(DataOps.onChangeSetReceived(data))
                        }}/>
                    </Button.Group>
                </Sticky>
                <CacheObjectDisplayContainer offsetElement={this.offsetElement}/>
                <Ref innerRef={(e) => {this.offsetElement = e}}>
                <Button fluid content="Other Space"/>
                </Ref>
            </div>
        )
    }
}

storiesOf("CacheObjectDisplayContainer", module)
    .addDecorator(story =>
        <Provider store={store}>
            {story()}
        </Provider>
    )
    .add("Ticking", () => {
        store.dispatch(DataOps.clearData())
        return (<DataChangerWrapper/>)
    })
    .add("Large number of rows", () => {
        store.dispatch(DataOps.clearData())
        return (<LargeDataWrapper/>)
    })

