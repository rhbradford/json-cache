// Author: Richard Bradford

import * as React from "react"
import {Dropdown, Button} from "semantic-ui-react"

interface CacheObjectTypeSelectorProps {
    types:    string[],
    onSelect: (value: string) => void
}

const CacheObjectTypeSelector = ({ types, onSelect }: CacheObjectTypeSelectorProps) => {

    const opts = types.map(type => ({ text: type, value: type }))

    if(opts.length === 0)
        return <Button>No types available</Button>
    else
        return <Dropdown button options={opts} defaultValue={opts[0].value} onChange={(e, data) => onSelect(data.value as string)}/>
}

export default CacheObjectTypeSelector
