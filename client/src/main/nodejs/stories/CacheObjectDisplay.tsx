// Author: Richard Bradford

import * as React from "react"
import {storiesOf} from "@storybook/react"

import "../node_modules/ag-grid/dist/styles/ag-grid.css"
import "../node_modules/ag-grid/dist/styles/theme-dark.css"

import {CacheObjectContentType} from "../app/model/types"
import CacheObjectDisplay from "../app/views/components/CacheObjectDisplay"

const columns = [
    {
        name: "price",
        type: CacheObjectContentType.number
    },
    {
        name: "details",
        type: CacheObjectContentType.object
    }
]

const rowData = [
    { id: "Object1", type: "Order", content: { price: 23.56, details: { product: "XYZ", category: "End" } } },
    { id: "Object2", type: "Order", content: { price: -67.998, details: { product: "ABC", category: "Start" } } }
]

storiesOf("CacheObjectDisplay", module)
    .add("Basic", () => (
        <div style={{ height: "200px" }}>
            <CacheObjectDisplay objectType={CacheObjectContentType.object} columns={columns} rowData={rowData}/>
        </div>
    ))
