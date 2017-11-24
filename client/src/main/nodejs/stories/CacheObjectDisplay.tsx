// Author: Richard Bradford

import * as React from "react"
import {storiesOf} from "@storybook/react"

import "../node_modules/ag-grid/dist/styles/ag-grid.css"
import "../node_modules/ag-grid/dist/styles/theme-dark.css"

import CacheObjectDisplay from "../app/views/components/CacheObjectDisplay"

const columns = ["id", "type", "price", "details"]

const rowData = [
    { id: "Object1", type: "Order", price: 23.56, details: JSON.stringify({ product: "XYZ", category: "End" }) },
    { id: "Object2", type: "Order", price: -67.998, details: { product: "ABC", category: "Start" } }
]

storiesOf("CacheObjectDisplay", module)
    .add("Basic", () => (
        <div style={{ height: "200px" }}>
            <CacheObjectDisplay columns={columns} rowData={rowData}/>
        </div>
    ))
