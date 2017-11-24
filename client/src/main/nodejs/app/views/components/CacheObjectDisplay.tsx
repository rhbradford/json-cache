// Author: Richard Bradford

import * as React from "react"
import {AgGridReact} from "ag-grid-react"
import {AgGridEvent, GridApi, ColumnApi} from "ag-grid"

interface CacheObjectDisplayProps {

    readonly columns: Array<string>,
    readonly rowData: any[]
}

interface CacheObjectDisplayState {
    
    columns: Array<string>,
    columnDefs: any[]
}

class CacheObjectDisplay extends React.Component<CacheObjectDisplayProps,CacheObjectDisplayState> {
    
    constructor(props: CacheObjectDisplayProps) {
        super(props)
        
        this.state = {
            
            columns: this.props.columns,
            columnDefs: this.createColumnDefs(this.props.columns)
        }
    }
    
    createColumnDefs = (columns: Array<String>): any[] => {

        if(columns) {
            return columns.map(column => ({
                headerName: column,
                field: column
            }))
        } else {
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
        
        if(this.columnApi) {
            
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
            this.setState({
                columns: nextProps.columns,
                columnDefs: nextProps.columns.map(column => ({
                    headerName: column,
                    field: column
                }))
            })
            this.sizeColumns()
        }
    }
    
    render() {
        
        const {rowData} = this.props
        
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
                />
            </div>
        )
    }
}

export default CacheObjectDisplay



