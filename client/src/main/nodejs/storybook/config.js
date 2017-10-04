// Author: richard
// Date:   28 Sep 2017

import {configure} from '@storybook/react'

import "./actions.ts"

function loadStories() {
    require('../stories/CacheObjectTypeSelector.tsx');
    // You can require as many stories as you need.
}

configure(loadStories, module)
