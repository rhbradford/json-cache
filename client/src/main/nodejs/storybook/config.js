// Author: Richard Bradford

import {configure} from '@storybook/react'

function loadStories() {
    require('../stories/CacheObjectTypeSelector.tsx');
    // You can require as many stories as you need.
}

configure(loadStories, module)
