// Author: Richard Bradford

import {connect} from "react-redux"

import {State} from "../../state"
import {selectors} from "../../state/cacheObjectData"
import CacheObjectDisplay from "../components/CacheObjectDisplay"

const mapStateToProps = (state: State) => {

    const cacheObjectData = state.cacheObjectData
    const type = state.cacheObjectType.selectedType

    return {

        objectType: selectors.cacheObjectContentType(cacheObjectData, type),
        columns:    selectors.cacheObjectColumns(cacheObjectData, type),
        rowData:    selectors.cacheObjects(cacheObjectData, type)
    }
}

export default connect(mapStateToProps)(CacheObjectDisplay)

