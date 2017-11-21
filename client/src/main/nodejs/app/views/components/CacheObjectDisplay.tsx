// Author: Richard Bradford

import * as React from "react"
import {AgGridReact} from "ag-grid-react"

interface CacheObjectDisplayProps {

    readonly columnDefs: any[],
    readonly rowData: any[],
    readonly height?: number
}

const CacheObjectDisplay: React.SFC<CacheObjectDisplayProps> = ({columnDefs, rowData, height} : CacheObjectDisplayProps) => {
    
    const gridStyle = {
        height: "100%",
        width: "100%"
    }
    
    return (
        <div style={gridStyle} className="ag-dark">
            <AgGridReact
                columnDefs={columnDefs}
                rowData={rowData}
                onGridReady={(params: any) => params.api.sizeColumnsToFit()}
                enableSorting
                enableFilter
                deltaRowDataMode
                getRowNodeId={(row) => row.id}
            />
        </div>
    )
}

export default CacheObjectDisplay



