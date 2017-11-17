// Author: Richard Bradford

import * as React from "react"

interface State {

    height: number,
    trackingHeight: number,
    offsetElement: Element
}

interface ExternalProps {

    offsetElement?: Element
}

interface InjectedProps {

    height?: number
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

            divElement: Element
            timer: number

            constructor(props: ResultProps) {

                super(props)

                this.state = {
                    height: undefined,
                    trackingHeight: undefined,
                    offsetElement: props.offsetElement
                }

                this.checkHeight = this.checkHeight.bind(this)
            }

            componentWillReceiveProps(nextProps: Readonly<ResultProps>) {
                
                if(nextProps.offsetElement !== this.props.offsetElement) this.setState({
                    offsetElement: nextProps.offsetElement
                })
            }

            componentDidMount() {

                const offsetHeight = this.state.offsetElement ? this.state.offsetElement.clientHeight : 0
                const trackingHeight = this.divElement.clientHeight - offsetHeight  
                const bcrect = this.divElement.getBoundingClientRect()
                const height = bcrect.top < 0 ? bcrect.height - offsetHeight : bcrect.height - bcrect.top - offsetHeight
                this.setState({ height, trackingHeight });

                this.timer = window.setInterval(this.checkHeight, trackingIntervalMs)
            }

            componentWillUnmount() {

                window.clearInterval(this.timer)
            }

            checkHeight() {

                const offsetHeight = this.state.offsetElement ? this.state.offsetElement.clientHeight : 0
                const trackingHeight = this.divElement.clientHeight - offsetHeight  
                if(this.state.trackingHeight != trackingHeight) {
                    const bcrect = this.divElement.getBoundingClientRect()
                    const height = bcrect.top < 0 ? bcrect.height - offsetHeight : bcrect.height - bcrect.top - offsetHeight
                    this.setState({
                        height,
                        trackingHeight
                    })
                }
            }

            render() {

                let component = this.state.height ? <InnerComponent {...this.props} {...this.state} /> : <div/>

                return (
                    <div>
                        <div ref={(divElement) => this.divElement = divElement}
                             style={{
                                 float:  "left",
                                 height: "100vh",
                                 width:  "0%"
                             }}/>
                        <div style={{ width: "100%" }}>{component}</div>
                        <div style={{ clear: "both" }}/>
                    </div>
                )
            }
        }
    }

export default withHeight