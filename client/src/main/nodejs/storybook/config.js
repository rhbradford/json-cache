// Author: Richard Bradford

import {configure} from '@storybook/react'

import "../app/semantic.slate.min.css"
// import "semantic-ui-css/semantic.min.css"

function loadStories() {
    require('../stories/CacheObjectTypeSelector.tsx')
    require('../stories/CacheConnector.tsx')
    require('../stories/CacheObjectFilter.tsx')
    require('../stories/CacheObjectDisplay.tsx')
    require('../stories/Header.tsx')
    // You can require as many stories as you need.
}

configure(loadStories, module)
