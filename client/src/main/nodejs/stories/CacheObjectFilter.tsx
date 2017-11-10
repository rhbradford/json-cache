// Author: Richard Bradford

import * as React from "react"
import {storiesOf} from "@storybook/react"
import {action} from "@storybook/addon-actions"

import CacheObjectFilter from "../app/views/components/CacheObjectFilter"

storiesOf("CacheObjectFilter", module)
    .add("Set (no content)", () => (
        <CacheObjectFilter filter="" onFilterSet={action("onFilterSet")}/>
    ))
    .add("Set (with content)", () => (
        <CacheObjectFilter filter="filter" onFilterSet={action("onFilterSet")}/>
    ))
