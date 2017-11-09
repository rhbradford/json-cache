// Author: Richard Bradford

import actions from "../socketMiddleware/actions"
import urlActions from "./actions"

const connect = actions.connect
const disconnect = actions.disconnect
const sendMessage = actions.sendMessage
const setURL = urlActions.setURL

export default {
    
    connect,
    disconnect,
    sendMessage,
    setURL
}
