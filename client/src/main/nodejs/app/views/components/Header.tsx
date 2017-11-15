// Author: Richard Bradford

import * as React from "react"
import {Sticky, Grid} from "semantic-ui-react"
import {ReactElement} from "react"

export interface HeaderProps {

    readonly cacheConnector: ReactElement<any>,
    readonly cacheObjectTypeSelector: ReactElement<any>
    readonly cacheObjectFilter: ReactElement<any>
}

const Header: React.SFC<HeaderProps> = ({ cacheConnector, cacheObjectTypeSelector, cacheObjectFilter }: HeaderProps) => {
    
    return <Sticky>
        <Grid>
            <Grid.Row stretched stackable>
                <Grid.Column width={8}>{cacheConnector}</Grid.Column>
                <Grid.Column width={3}>{cacheObjectTypeSelector}</Grid.Column>
                <Grid.Column width={5}>{cacheObjectFilter}</Grid.Column>
            </Grid.Row>
        </Grid>
    </Sticky>
}

export default Header