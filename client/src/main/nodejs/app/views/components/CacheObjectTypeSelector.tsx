// Author: Richard Bradford

import * as React from "react"
import {Dropdown, Button} from "semantic-ui-react"

export interface CacheObjectTypeSelectorProps {
    readonly types:    string[],
    readonly onSelect: (value: string) => void
}

const CacheObjectTypeSelector: React.SFC<CacheObjectTypeSelectorProps> = ({ types, onSelect }) => {

    const opts = types.map(type => ({ text: type, value: type }))

    if(opts.length === 0)
        return <Button fluid>No types available</Button> 
    else
        return <Dropdown fluid button options={opts} defaultValue={opts[0].value} onChange={(e, data) => onSelect(data.value as string)}/> 
}

export default CacheObjectTypeSelector
