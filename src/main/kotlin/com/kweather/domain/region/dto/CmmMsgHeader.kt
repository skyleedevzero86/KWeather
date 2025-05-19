package com.kweather.domain.region.dto

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class CmmMsgHeader(
    @JacksonXmlProperty(localName = "errMsg")
    val errMsg: String? = null,
    @JacksonXmlProperty(localName = "returnAuthMsg")
    val returnAuthMsg: String? = null,
    @JacksonXmlProperty(localName = "returnReasonCode")
    val returnReasonCode: String? = null
)