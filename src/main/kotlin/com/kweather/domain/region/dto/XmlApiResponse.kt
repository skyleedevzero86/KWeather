package com.kweather.domain.region.dto

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "OpenAPI_ServiceResponse")
data class XmlApiResponse(
    @JacksonXmlProperty(localName = "cmmMsgHeader")
    val cmmMsgHeader: CmmMsgHeader? = null
)