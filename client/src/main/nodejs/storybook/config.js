// Author: Richard Bradford

import {configure} from '@storybook/react'

function loadStories() {
    require('../stories/CacheObjectTypeSelector.tsx')
    require('../stories/CacheConnector.tsx')
    require('../stories/CacheObjectDisplay.tsx')
    // You can require as many stories as you need.
}

configure(loadStories, module)
