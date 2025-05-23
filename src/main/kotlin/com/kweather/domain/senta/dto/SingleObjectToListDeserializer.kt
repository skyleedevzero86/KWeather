package com.kweather.domain.senta.dto


import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode

class SingleObjectToListDeserializer : StdDeserializer<List<SenTaIndexItem>>(List::class.java) {
    override fun deserialize(p: com.fasterxml.jackson.core.JsonParser, ctxt: DeserializationContext): List<SenTaIndexItem> {
        val node: JsonNode = p.codec.readTree(p)
        val mapper = p.codec as com.fasterxml.jackson.databind.ObjectMapper
        return if (node.isObject) {
            listOf(mapper.treeToValue(node.get("item"), SenTaIndexItem::class.java))
        } else {
            mapper.treeToValue(node.get("item"), List::class.java) as List<SenTaIndexItem>
        }
    }
}