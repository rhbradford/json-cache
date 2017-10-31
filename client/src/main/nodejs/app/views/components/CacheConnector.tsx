// Author: Richard Bradford

import * as React from "react"
import {Form, Button, Input, InputOnChangeData} from "semantic-ui-react"
import {SyntheticEvent} from "react"

export enum States { Disconnected, Connecting, Connected, Disconnecting }

interface CacheConnectorProps {
    connectorUrl: string,
    connectorState: States,
    onConnect: (url: string) => void
    onDisconnect: (url: string) => void
}

interface CacheConnectorState {
    url: string
}

class CacheConnector extends React.Component<CacheConnectorProps, CacheConnectorState> {
    
    constructor(props: CacheConnectorProps) {
        super(props)
        
        this.state = {
            url: this.props.connectorUrl
        }
        
        this.handleURLChange = this.handleURLChange.bind(this)
    }
    
    // noinspection JSUnusedLocalSymbols
    handleURLChange(event: SyntheticEvent<HTMLInputElement>, data: InputOnChangeData) {
        this.setState({
            url: data.value    
        })
    }
    
    render() {
        const {connectorState, onConnect, onDisconnect} = this.props
        
        const url = this.state.url
        
        switch(connectorState) {
            case States.Disconnected:
                return (
                    <Form onSubmit={(e) => onConnect(url)}>
                        <Form.Field inline>
                        <Input    
                            fluid
                            label="URL:"
                            labelPosition="left"
                            action={<Button type="submit">Connect</Button>} 
                            value={url} 
                            placeholder="Cache URL" 
                            onChange={this.handleURLChange}
                        />
                        </Form.Field> 
                    </Form>
                )
            case States.Connecting:
                return (
                    <Form>
                        <Form.Field inline>
                        <Input    
                            fluid
                            label="URL:"
                            labelPosition="left"
                            action="Connecting..." 
                            value={url} 
                            placeholder="Cache URL" 
                        />
                        </Form.Field> 
                    </Form>
                )
            case States.Connected:
                return (
                    <Form onSubmit={(e) => onDisconnect(url)}>
                        <Form.Field inline>
                        <Input    
                            fluid
                            label="URL:"
                            labelPosition="left"
                            action={<Button type="submit">Disconnect</Button>} 
                            value={url} 
                            placeholder="Cache URL" 
                        />
                        </Form.Field> 
                    </Form>
                )
            case States.Disconnecting:
                return (
                    <Form>
                        <Form.Field inline>
                        <Input    
                            fluid
                            label="URL:"
                            labelPosition="left"
                            action="Disconnecting..." 
                            value={url} 
                            placeholder="Cache URL" 
                        />
                        </Form.Field> 
                    </Form>
                )
        }
    }
}

export default CacheConnector

