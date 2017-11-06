// Author: Richard Bradford

import * as React from "react"
import {storiesOf} from "@storybook/react"
import {action} from "@storybook/addon-actions"

import Header from "../app/views/components/Header"
import {States} from "../app/views/components/CacheConnector"

storiesOf("Header", module)
    .add("Example", () => (
        <Header 
            cacheConnectorProps={ 
                {
                    connectorUrl: "",
                    connectorState: States.Disconnected,
                    onConnect: action("onConnect"),
                    onDisconnect: action("onDisconnect")
                }
            }
            cacheObjectTypeSelectorProps={
                {
                    types: ["TypeA", "TypeB"],
                    onSelect: action("onCacheObjectTypeSelect")
                }
            }
        />
    ))
