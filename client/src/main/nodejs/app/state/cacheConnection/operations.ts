// Author: Richard Bradford

import actions from "../socketMiddleware/actions"

const connect = actions.connect
const disconnect = actions.disconnect
const sendMessage = actions.sendMessage

export default {
    
    connect,
    disconnect,
    sendMessage
}
