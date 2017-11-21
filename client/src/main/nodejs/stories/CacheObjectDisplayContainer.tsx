// Author: Richard Bradford

import * as React from "react"
import {Provider} from "react-redux"
import {createStore} from "redux"
import {devToolsEnhancer} from "redux-devtools-extension"
import {storiesOf} from "@storybook/react"
import {Grid, Button, Sticky, Ref} from "semantic-ui-react"
const GoldenLayout = require("golden-layout")
import * as PropTypes from 'prop-types'

import "../node_modules/ag-grid/dist/styles/ag-grid.css"
import "../node_modules/ag-grid/dist/styles/theme-dark.css"

import "../node_modules/golden-layout/src/css/goldenlayout-base.css"
import "../node_modules/golden-layout/src/css/goldenlayout-dark-theme.css"

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

interface GoldenLayoutContext {
    store: any
}

class GoldenLayoutWrapper extends React.Component<{dataProvider: React.ComponentClass<{}> | (() => React.ReactElement<{}>)}, {}> {

    layout: any
    
    componentDidMount() {
        // Build basic golden-layout config
        const config = {
            content: [{
                type:    'row',
                content: [
                    {
                        type:      'react-component',
                        component: 'TestDataProviderContainer'
                    },
                    {
                        type:      'react-component',
                        component: 'TestComponentContainer'
                    }
                ]
            }]
        }

        function wrapComponent(Component: any, store: any): any {
            class Wrapped extends React.Component {
                render() {
                    return (
                        <Provider store={store}>
                            <Component {...this.props}/>
                        </Provider>
                    );
                }
            }

            return Wrapped
        }

        let layout = new GoldenLayout(config, this.layout)
        layout.registerComponent('TestDataProviderContainer',
            wrapComponent(this.props.dataProvider, this.context.store)
        )
        layout.registerComponent('TestComponentContainer',
            wrapComponent(CacheObjectDisplayContainer, this.context.store)
        )
        layout.init()

        window.addEventListener('resize', () => {
            layout.updateSize()
        })
    }

    render() {
        return (
            <div className='goldenLayout' style={{height: "100vh"}} ref={input => this.layout = input}/>
        )
    }

    static contextTypes: React.ValidationMap<any> = {
        store: PropTypes.object
    }
}

// ContextTypes must be defined in order to pass the redux store to exist in
// "this.context". The redux store is given to GoldenLayoutWrapper from its
// surrounding <Provider> in index.jsx.
// GoldenLayoutWrapper.contextTypes = {
//     store: React.PropTypes.object.isRequired
// }

const DataChangerWrapper = () => {

    return (
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
    )
}

class LargeDataWrapper extends React.Component<{}, {}> {

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
            <Button.Group fluid>
                <Button content="Load" onClick={(e, d) => {
                    e.preventDefault()
                    store.dispatch(DataOps.onChangeSetReceived(data))
                }}/>
            </Button.Group>
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
        return (
            <div style={{height: "100vh"}}>
            <GoldenLayoutWrapper dataProvider={DataChangerWrapper}/>
            </div>
        )
    })
    .add("Large number of rows", () => {
        store.dispatch(DataOps.clearData())
        return (<LargeDataWrapper/>)
    })
    .add("Reflex test", () => {
        store.dispatch(DataOps.clearData())
        return (
            <div/>
        )
    })


