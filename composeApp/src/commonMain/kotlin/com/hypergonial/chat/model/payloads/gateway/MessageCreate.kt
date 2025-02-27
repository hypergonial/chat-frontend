package com.hypergonial.chat.model.payloads.gateway

import com.hypergonial.chat.model.Event
import com.hypergonial.chat.model.MessageCreateEvent
import com.hypergonial.chat.model.payloads.Message
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MESSAGE_CREATE")
class MessageCreate(@SerialName("data") val message: Message) : GatewayMessage(), EventConvertible {
    override fun toEvent(): Event {
        return MessageCreateEvent(message)
    }
}
