// Author: Richard Bradford

import * as React from "react"
import {Provider} from "react-redux"
import {createStore} from "redux"
import {devToolsEnhancer} from "redux-devtools-extension"
import {storiesOf} from "@storybook/react"
import {Grid, Button, Sticky, Ref, Menu} from "semantic-ui-react"
const GoldenLayout = require("golden-layout")
import * as PropTypes from 'prop-types'

import "../node_modules/ag-grid/dist/styles/ag-grid.css"
import "../node_modules/ag-grid/dist/styles/theme-dark.css"

import "../node_modules/golden-layout/src/css/goldenlayout-base.css"
import "../node_modules/golden-layout/src/css/goldenlayout-dark-theme.css"

import {State} from "../app/state"
import DataReducer, {operations as DataOps} from "../app/state/cacheObjectData"
import {CacheChangeSet} from "../app/model/types"
import CacheObjectDisplayContainer from "../app/views/containers/CacheObjectDisplayContainer"
import {CacheRemove} from "../app/model/types"

const rootReducer = (state: State, action: any): State => {
    return ({
        ...state,
        cacheObjectData: DataReducer(state.cacheObjectData, action)
    })
}

const theType = "ComplexObject"

const store = createStore(
    rootReducer,
    {
        cacheObjectData: undefined,
        cacheObjectType: {
            selectedType: theType
        }
    } as State,
    devToolsEnhancer({})
)

const tickData: CacheChangeSet = {

    puts:    [
        {
            id:      "object_1",
            type:    theType,
            content: {
                name: "Frederick",
                age:  24
            }
        },
        {
            id:      "object_2",
            type:    theType,
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
            type:    theType,
            content: {
                name: "Frederick",
                age:  32
            }
        },
        {
            id:      "object_2",
            type:    theType,
            content: {
                name: "Wilma",
                age:  33
            }
        },
        {
            id:      "object_3",
            type:    theType,
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

interface GoldenLayoutWrapperProps {
    dataProvider: React.ComponentClass<{}> | (() => React.ReactElement<{}>)
}

class GoldenLayoutWrapper extends React.Component<GoldenLayoutWrapperProps, {}> {

    layoutDiv: any
    layout: any
    
    constructor(props: GoldenLayoutWrapperProps) {
        super(props)
        this.updateSize = this.updateSize.bind(this)
    }
    
    componentDidMount() {
        // Build basic golden-layout config
        const config = {
            settings: {
                showCloseIcon: false  
            },
            content: [{
                type:    'row',
                content: [
                    {
                        type:      'react-component',
                        component: 'TestDataProviderContainer',
                        title: 'DataProvider',
                        isClosable: false
                    },
                    {
                        type:      'react-component',
                        component: 'TestComponentContainer',
                        title: 'CacheObjectDisplay',
                        isClosable: false
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

        this.layout = new GoldenLayout(config, this.layoutDiv)
        this.layout.registerComponent('TestDataProviderContainer',
            wrapComponent(this.props.dataProvider, this.context.store)
        )
        this.layout.registerComponent('TestComponentContainer',
            wrapComponent(CacheObjectDisplayContainer, this.context.store)
        )
        this.layout.init()

        window.addEventListener('resize', this.updateSize)
    }
    
    updateSize() {
        
        this.layout.updateSize()
    }
    
    componentWillUnmount() {
        
        this.layout.destroy()
        window.removeEventListener('resize', this.updateSize)
    }

    render() {
        return (
            <div className='goldenLayout' style={{position:"absolute", height:"100%", width:"100%"}} ref={e => this.layoutDiv = e}/>
        )
    }

    // ContextTypes must be defined in order to pass the redux store to exist in
    // "this.context".
    static contextTypes: React.ValidationMap<any> = {
        store: PropTypes.object
    }
}

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

    render() {
        const things = ["Cup", "Spoon", "Saucer"]

        const put = (i: number) => ({
            id:      `${i}`,
            type:    theType,
            content: {
                stuff: things[i % 3],
                nums: i
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
        return (
            <div style={{height: "100vh", width:"100vw", display:"flex", flexFlow:"column"}}>
                <div style={{flex:"0 1 0%", paddingBottom:"8px"}}>
                    <Menu>
                        <Menu.Item name="menuItem"/>
                    </Menu>
                </div>
            <div style={{flex:"1 1 0%", position:"relative"}}>    
            <GoldenLayoutWrapper dataProvider={LargeDataWrapper}/>
            </div>
            </div>
        )
    })


