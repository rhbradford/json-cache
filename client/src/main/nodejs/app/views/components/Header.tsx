// Author: Richard Bradford

import * as React from "react"
import {Sticky, Grid} from "semantic-ui-react"

import {default as CacheConnector, CacheConnectorProps} from "./CacheConnector"
import {default as CacheObjectTypeSelector, CacheObjectTypeSelectorProps} from "./CacheObjectTypeSelector"

export interface HeaderProps {
    
    readonly cacheConnectorProps: CacheConnectorProps,
    readonly cacheObjectTypeSelectorProps: CacheObjectTypeSelectorProps
}

const Header = (headerProps: HeaderProps) => {
    
    return <Sticky>
        <Grid stretched>
            <Grid.Column width={13}><CacheConnector {...headerProps.cacheConnectorProps}/></Grid.Column>
            <Grid.Column width={3}><CacheObjectTypeSelector {...headerProps.cacheObjectTypeSelectorProps}/></Grid.Column>
        </Grid>
    </Sticky>
}

export default Header