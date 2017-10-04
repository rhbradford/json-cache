import * as React from "react"
import * as ReactDOM from "react-dom"

import CacheObjectTypeSelector from "./views/components/CacheObjectTypeSelector"

// import "../semantic/dist/semantic.slate.min.css"
import "./semantic.slate.min.css"

ReactDOM.render(
    <CacheObjectTypeSelector types={[]} onSelect={(value) => console.log(value)}/>,
    document.getElementById("root")
)