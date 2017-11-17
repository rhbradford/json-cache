// Author: Richard Bradford

import {connect} from "react-redux"

import {State} from "../../state"
import {selectors} from "../../state/cacheObjectData"
import CacheObjectDisplay from "../components/CacheObjectDisplay"
import withHeight from "../higherOrderComponents/WithHeight"

const mapStateToProps = (state: State) => {

    const cacheObjectData = state.cacheObjectData
    const type = state.cacheObjectType.selectedType

    return {

        columnDefs: selectors.cacheObjectColumnsForType(cacheObjectData, type),
        rowData:    selectors.cacheObjectsForType(cacheObjectData, type)
    }
}

export default connect(mapStateToProps)(withHeight()(CacheObjectDisplay))

