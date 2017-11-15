// Author: Richard Bradford

import * as React from "react"
import {storiesOf} from "@storybook/react"
import {action} from "@storybook/addon-actions"

import Header from "../app/views/components/Header"
import CacheConnector, {States} from "../app/views/components/CacheConnector"
import CacheObjectTypeSelector from "../app/views/components/CacheObjectTypeSelector"
import CacheObjectFilter from "../app/views/components/CacheObjectFilter"

storiesOf("Header", module)
    .add("Example", () => (
        <Header
            cacheConnector={
                <CacheConnector connectorUrl="" connectorState={States.Disconnected} onConnect={action("onConnect")} onDisconnect={action("onDisconnect")}/>
            }
            cacheObjectTypeSelector={
                <CacheObjectTypeSelector types={["TypeA", "TypeB"]} onSelect={action("onSelect")}/>
            }
            cacheObjectFilter={
                <CacheObjectFilter filter=".*" onFilterSet={action("onFilterSet")}/>
            }
        />
    ))
