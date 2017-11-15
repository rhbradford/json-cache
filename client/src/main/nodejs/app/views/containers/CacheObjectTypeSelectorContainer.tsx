// Author: Richard Bradford

import {connect} from "react-redux"
import {Dispatch} from "redux"

import {State} from "../../state"
import {operations, ActionTypes} from "../../state/cacheObjectType"
import {selectors} from "../../state/cacheObjectData"
import CacheObjectTypeSelector from "../components/CacheObjectTypeSelector"

const mapStateToProps = (state: State) => {

    return {

        types: selectors.cacheObjectTypes(state.cacheObjectData)
    }
}

const mapDispatchToProps = (dispatch: Dispatch<ActionTypes>) => {

    return {

        onSelect: (type: string) => {

            dispatch(operations.onTypeSelected(type))
        }
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(CacheObjectTypeSelector)

