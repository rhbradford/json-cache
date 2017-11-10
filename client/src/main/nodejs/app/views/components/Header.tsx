// Author: Richard Bradford

import * as React from "react"
import {Sticky, Grid} from "semantic-ui-react"

import CacheConnector, {CacheConnectorProps} from "./CacheConnector"
import CacheObjectTypeSelector, {CacheObjectTypeSelectorProps} from "./CacheObjectTypeSelector"
import CacheObjectFilter, {CacheObjectFilterProps} from "./CacheObjectFilter"

export interface HeaderProps {

    readonly cacheConnectorProps: CacheConnectorProps,
    readonly cacheObjectTypeSelectorProps: CacheObjectTypeSelectorProps
    readonly cacheObjectFilterProps: CacheObjectFilterProps
}

const Header = (headerProps: HeaderProps) => {

    return <Sticky>
        <Grid>
            <Grid.Row stretched stackable>
                <Grid.Column width={8}><CacheConnector {...headerProps.cacheConnectorProps}/></Grid.Column>
                <Grid.Column width={3}><CacheObjectTypeSelector {...headerProps.cacheObjectTypeSelectorProps}/></Grid.Column>
                <Grid.Column width={5}><CacheObjectFilter {...headerProps.cacheObjectFilterProps}/></Grid.Column>
            </Grid.Row>
        </Grid>
    </Sticky>
}

export default Header