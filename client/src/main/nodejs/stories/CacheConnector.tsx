// Author: Richard Bradford

import * as React from "react"
import {storiesOf} from "@storybook/react"
import {action} from "@storybook/addon-actions"

import "../app/semantic.slate.min.css"

import {States} from "../app/views/components/CacheConnector"
import CacheConnector from "../app/views/components/CacheConnector"

storiesOf("CacheConnector", module)
    .add("Disconnected (no URL)", () => (
        <CacheConnector connectorUrl="" connectorState={States.Disconnected} onConnect={action("onConnect")} onDisconnect={action("onDisconnect")}/>
    ))
    .add("Disconnected (with URL)", () => (
        <CacheConnector connectorUrl="ws://localhost:8080/cache" connectorState={States.Disconnected} onConnect={action("onConnect")} onDisconnect={action("onDisconnect")}/>
    ))
    .add("Connecting", () => (
        <CacheConnector connectorUrl="ws://localhost:8080/cache" connectorState={States.Connecting} onConnect={action("onConnect")} onDisconnect={action("onDisconnect")}/>
    ))
    .add("Connected", () => (
        <CacheConnector connectorUrl="ws://localhost:8080/cache" connectorState={States.Connected} onConnect={action("onConnect")} onDisconnect={action("onDisconnect")}/>
    ))
    .add("Disconnecting", () => (
        <CacheConnector connectorUrl="ws://localhost:8080/cache" connectorState={States.Disconnecting} onConnect={action("onConnect")} onDisconnect={action("onDisconnect")}/>
    ))
