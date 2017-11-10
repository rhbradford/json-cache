// Author: Richard Bradford

import * as React from "react"
import {Form, Icon, Button, Input, InputOnChangeData} from "semantic-ui-react"
import {SyntheticEvent} from "react"

export enum States { Set, Editing }

export interface CacheObjectFilterProps {
    readonly filter: string
    readonly onFilterSet: (filter: string) => void
}

interface CacheObjectFilterState {
    filter: string
    editingFilter: string
    state: States
}

class CacheObjectFilter extends React.Component<CacheObjectFilterProps, CacheObjectFilterState> {

    inputRef: any
    
    constructor(props: CacheObjectFilterProps) {
        super(props)

        this.state = {
            filter:        this.props.filter,
            editingFilter: this.props.filter,
            state:         States.Set
        }

        this.handleFilterChange = this.handleFilterChange.bind(this)
        this.handleEdit = this.handleEdit.bind(this)
        this.handleSet = this.handleSet.bind(this)
        this.handleCancel = this.handleCancel.bind(this)
    }

    // noinspection JSUnusedLocalSymbols
    handleFilterChange(event: SyntheticEvent<HTMLInputElement>, data: InputOnChangeData) {
        this.setState({
            editingFilter: data.value
        })
    }

    handleSet(f: string, onFilterSet: (filter: string) => void) {
        this.setState({
            state: States.Set
        })
        onFilterSet(f)
    }

    handleCancel() {
        this.setState((prevState, props) => ({
            ...prevState,
            editingFilter: prevState.filter,
            state:         States.Set
        }))
    }

    handleEdit() {
        this.setState({
            state: States.Editing
        })
    }
    
    handleRef = (c: any) => {
        this.inputRef = c
    }
    
    focus = () => {
        this.inputRef.focus()
    }

    render() {
        const { onFilterSet } = this.props

        const state = this.state.state
        const filter = this.state.editingFilter

        const editingButtons = (
            <Button.Group>
                <Button icon={<Icon name="checkmark"/>} type="submit"/>
                <Button icon={<Icon name="remove"/>} onClick={(e, d) => {
                    e.preventDefault()
                    this.handleCancel()
                }}/>
            </Button.Group>
        )

        const setButtons = (
            <Button icon={<Icon name="edit"/>} onClick={(e, d) => {
                e.preventDefault()
                this.handleEdit()
                this.focus()
            }}/>
        )

        switch(state) {
            case States.Editing:
                return (
                    <Form onSubmit={(e) => this.handleSet(filter, onFilterSet)}>
                        <Form.Field inline>
                            <Input
                                fluid
                                ref={this.handleRef}
                                focus
                                label="Filter:"
                                labelPosition="left"
                                value={filter}
                                placeholder="Filter"
                                action={editingButtons}
                                onChange={this.handleFilterChange}
                       />
                        </Form.Field>
                    </Form>
                )
            case States.Set:
                return (
                    <Form>
                        <Form.Field inline>
                            <Input
                                fluid
                                label="Filter:"
                                labelPosition="left"
                                value={filter}
                                placeholder="Filter"
                                action={setButtons}
                            />
                        </Form.Field>
                    </Form>
                )
        }
    }
}

export default CacheObjectFilter

