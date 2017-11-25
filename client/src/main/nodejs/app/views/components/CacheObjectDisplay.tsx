// Author: Richard Bradford

import * as React from "react"
import {AgGridReact} from "ag-grid-react"
import {AgGridEvent, GridApi, ColumnApi, ColDef} from "ag-grid"
import {CacheObject, CacheObjectColumn, CacheObjectContentType} from "../../model/types"
import {ValueGetterParams} from "ag-grid/dist/lib/entities/colDef"

interface CacheObjectDisplayProps {

    readonly objectType: CacheObjectContentType,
    readonly columns: Array<CacheObjectColumn>,
    readonly rowData: Array<CacheObject>
}

interface CacheObjectDisplayState {

    columns: Array<CacheObjectColumn>,
    columnDefs: Array<any>
}

class CacheObjectDisplay extends React.Component<CacheObjectDisplayProps, CacheObjectDisplayState> {

    constructor(props: CacheObjectDisplayProps) {
        super(props)

        this.state = {

            columns:    this.props.columns,
            columnDefs: this.createColumnDefs(this.props.objectType, this.props.columns)
        }
    }

    createColumnDefs = (objectType: CacheObjectContentType, columns: Array<CacheObjectColumn>): any[] => {

        if(columns && objectType) {
            
            const columnDefs = columns.map(column => {
                
                let valueGetter: (params: ValueGetterParams) => any
                
                if(objectType == CacheObjectContentType.object)
                    valueGetter = (params: ValueGetterParams) => { return params.node.data.content[column.name] }
                else     
                    valueGetter = (params: ValueGetterParams) => { return params.node.data.content }
                    
                return {
                    headerName: column.name,
                    valueGetter: valueGetter
                } as ColDef
            })
            
            columnDefs.push({
                headerName: "*id",
                valueGetter: (params: ValueGetterParams) => { return params.node.data.id }
            })
            
            columnDefs.push({
                headerName: "*type",
                valueGetter: (params: ValueGetterParams) => { return params.node.data.type }
            })

            return columnDefs.reverse()
        }
        else {
            return []
        }
    }

    gridApi: GridApi
    columnApi: ColumnApi

    onGridReady = (params: AgGridEvent) => {

        this.gridApi = params.api
        this.columnApi = params.columnApi
    }

    sizeColumns = () => {

        if(this.columnApi && this.columnApi.getAllColumns().length > 0) {

            const allColumnIds: Array<any> = []
            this.columnApi.getAllColumns().forEach(column => {

                allColumnIds.push(column.getColId());
            })
            this.columnApi.autoSizeColumns(allColumnIds)
        }
    }

    componentDidMount() {

        this.sizeColumns()
    }

    componentWillReceiveProps(nextProps: CacheObjectDisplayProps) {

        if(this.state.columns != nextProps.columns) {
            this.setState(
                {
                    columns:    nextProps.columns,
                    columnDefs: this.createColumnDefs(nextProps.objectType, nextProps.columns)
                },
                () => {
                    this.gridApi.sizeColumnsToFit()
                    this.sizeColumns()
                }
            )
        }
    }

    render() {

        const { rowData } = this.props

        const gridStyle = {
            height: "100%",
            width:  "100%"
        }

        return (
            <div style={gridStyle} className="ag-dark">
                <AgGridReact
                    columnDefs={this.state.columnDefs}
                    rowData={rowData}
                    suppressNoRowsOverlay
                    onGridReady={this.onGridReady}
                    enableColResize
                    enableSorting
                    enableFilter
                    deltaRowDataMode
                    getRowNodeId={(row) => row.id}
                    autoSizePadding={10}
                />
            </div>
        )
    }
}

export default CacheObjectDisplay



