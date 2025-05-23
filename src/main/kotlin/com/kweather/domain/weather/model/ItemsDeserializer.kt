package com.kweather.domain.weather.model

import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode

class ItemsDeserializer<T : Any>(vc: Class<*>? = null) : StdDeserializer<List<T>>(vc) {
    override fun deserialize(p: com.fasterxml.jackson.core.JsonParser, ctxt: DeserializationContext): List<T> {
        val node: JsonNode = p.codec.readTree(p)
        return when {
            node.isArray -> {
                val type = ctxt.typeFactory.constructType(valueType)
                node.elements().asSequence().mapNotNull {
                    p.codec.treeToValue(it, type.rawClass) as? T
                }.toList()
            }
            node.isObject -> {
                val type = ctxt.typeFactory.constructType(valueType)
                listOfNotNull(p.codec.treeToValue(node, type.rawClass) as? T)
            }
            else -> emptyList()
        }
    }
}