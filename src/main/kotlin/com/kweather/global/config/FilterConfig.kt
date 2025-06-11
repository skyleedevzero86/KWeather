package com.kweather.global.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
class FilterConfig {

    @Bean
    @ConditionalOnProperty(
        prefix = "app.encoding-filter",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun encodingFilter(): OncePerRequestFilter {
        return object : OncePerRequestFilter() {
            override fun doFilterInternal(
                request: HttpServletRequest,
                response: HttpServletResponse,
                filterChain: FilterChain
            ) {
                if (response.isCommitted) {
                    filterChain.doFilter(request, response)
                    return
                }

                val contentType = response.contentType
                val newContentType = when {
                    contentType == null -> "application/json;charset=UTF-8"
                    contentType.contains("application/json") && !contentType.contains("charset") ->
                        "application/json;charset=UTF-8"
                    contentType.contains("text/html") && !contentType.contains("charset") ->
                        "text/html;charset=UTF-8"
                    else -> null
                }

                newContentType?.let { response.contentType = it }
                filterChain.doFilter(request, response)
            }
        }
    }
}