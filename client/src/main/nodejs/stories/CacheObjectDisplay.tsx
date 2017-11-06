// Author: Richard Bradford

import * as React from "react"
import {storiesOf} from "@storybook/react"

import "../node_modules/ag-grid/dist/styles/ag-grid.css"
import "../node_modules/ag-grid/dist/styles/theme-dark.css"

import CacheObjectDisplay from "../app/views/components/CacheObjectDisplay"

const columnDefs = [
    { headerName: "Id", field: "id" },
    { headerName: "Type", field: "type" },
    { headerName: "Price", field: "price" }
]

const rowData = [
    { id: "Object1", type: "Order", price: 23.56 },
    { id: "Object2", type: "Order", price: -67.998 }
]

storiesOf("CacheObjectDisplay", module)
    .add("Basic", () => (
        <CacheObjectDisplay columnDefs={columnDefs} rowData={rowData}/>
    ))
