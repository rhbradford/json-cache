// Author: Richard Bradford

import * as React from "react"
import {storiesOf} from "@storybook/react"
import {action} from "@storybook/addon-actions"

import CacheObjectTypeSelector from "../app/views/components/CacheObjectTypeSelector"

storiesOf("CacheObjectTypeSelector", module)
    .add("With types available", () => (
        <CacheObjectTypeSelector types={['TypeA', 'TypeB']} onSelect={action("onSelect")}/>
    ))
    .add("With no types available", () => (
        <CacheObjectTypeSelector types={[]} onSelect={action("onSelect")}/>
    ))
