// Author: Richard Bradford

import * as React from "react"
import {isUndefined} from "util"

interface InjectedProps {

    height?: number
}

interface State extends InjectedProps {

    trackingHeight: number,
    hiddenElementHeightSetting: string,
    offsetElement: Element
    willHaveOffset: boolean
}

interface ExternalProps {

    offsetElement?: Element
    willHaveOffset?: boolean
}

interface Options {

    trackingIntervalMs: number
}

/**
 * Wraps a Component - calculates the height of the area given to the Component and passes this height to the Component.
 * Tracks the height of the area every 'trackingIntervalMs' (defaults to every 50ms).
 */
const withHeight = ({ trackingIntervalMs }: Options = { trackingIntervalMs: 50 }) =>
    <TOriginalProps extends {}>(InnerComponent: (React.ComponentClass<TOriginalProps & InjectedProps> | React.StatelessComponent<TOriginalProps & InjectedProps>)) => {

        type ResultProps = TOriginalProps & ExternalProps

        return class WithHeight extends React.Component<ResultProps, State> {

            static displayName = `WithHeight(${InnerComponent.displayName})`

            hiddenElement: Element
            componentElement: Element
            timer: number

            constructor(props: ResultProps) {

                super(props)

                this.state = {
                    height: undefined,
                    trackingHeight: undefined,
                    hiddenElementHeightSetting: "100vh",
                    offsetElement: props.offsetElement,
                    willHaveOffset: (!isUndefined(props.willHaveOffset)) ? props.willHaveOffset : false
                }

                // this.checkHeight = this.checkHeight.bind(this)
                // this.setHeight = this.setHeight.bind(this)
            }

            // setHeight(element: Element) {
            //    
            //     const offsetHeight = this.state.offsetElement ? this.state.offsetElement.clientHeight : 0
            //     const trackingHeight = element.clientHeight
            //     if(trackingHeight != this.state.trackingHeight) {
            //         const bcrect = element.getBoundingClientRect()
            //         const height = bcrect.top < 0 ? bcrect.height - offsetHeight : bcrect.height - bcrect.top - offsetHeight
            //         this.setState({
            //             height,
            //             trackingHeight: height,
            //             hiddenElementHeightSetting: "100%"
            //         })
            //     }
            // }
            //
            // componentWillReceiveProps(nextProps: Readonly<ResultProps>) {
            //    
            //     if(nextProps.offsetElement !== this.props.offsetElement) this.setState({
            //         offsetElement: nextProps.offsetElement
            //     })
            // }

            componentDidMount() {

                this.setState({
                    height: this.componentElement.clientHeight
                })
                
                // if(!this.state.willHaveOffset || (this.state.willHaveOffset && this.state.offsetElement)) 
                //     this.setHeight(this.hiddenElement)
                //
                // this.timer = window.setInterval(this.checkHeight, trackingIntervalMs)
            }

            // componentWillUnmount() {
            //
            //     window.clearInterval(this.timer)
            // }
            //
            // checkHeight() {
            //
            //     if(!this.state.willHaveOffset || !isUndefined(this.state.height)) 
            //         this.setHeight(this.componentElement)
            //     else if(this.state.willHaveOffset && this.state.offsetElement) 
            //         this.setHeight(this.hiddenElement)
            // }

            render() {

                let component = this.state.height ? <InnerComponent {...this.props} {...this.state} /> : <div/>

                return (
                    <div style={{
                        display: "flex", 
                        flex: "1 1 0%", 
                        height: "100%", 
                        width: "100%",
                        minHeight: "100%",
                        flexDirection: "column",
                        position: "relative",
                        outline: "none",
                        top: 0, 
                        bottom: 0
                    }}>
                        {/*<div ref={(e) => this.hiddenElement = e}*/}
                             {/*style={{*/}
                                 {/*float:  "left",*/}
                                 {/*height: this.state.hiddenElementHeightSetting,*/}
                                 {/*width:  "0%"*/}
                             {/*}}/>*/}
                        <div ref={(e) => this.componentElement = e} 
                             style={{
                                 position: "relative",
                                 flex: "1 1 0%",
                                 width: "100%" 
                             }}>
                            {/*{component}*/}
                        </div>
                        {/*<div style={{ clear: "both" }}/>*/}
                    </div>
                )
            }
        }
    }

export default withHeight