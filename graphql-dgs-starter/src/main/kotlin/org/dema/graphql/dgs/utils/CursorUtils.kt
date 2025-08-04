package org.dema.graphql.dgs.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.charset.StandardCharsets
import java.util.Base64

private val objectMapper: ObjectMapper =
    jacksonObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

/**
 * Utility object for encoding and decoding relay cursors to and from Base64.
 */
object CursorUtils {
    /**
     * Decodes this Base64 encoded cursor string into an array of values.
     *
     * @receiver the Base64 encoded cursor or `null`
     * @return decoded values or an empty array if the receiver is `null`
     */
    fun String?.decode(): Array<String> {
        val base64EncodedString = this ?: return emptyArray()
        return base64EncodedString.decodeBase64().let { objectMapper.readValue<Array<String>>(it) }
    }

    /**
     * Encodes this array of values into a Base64 string representation.
     */
    fun Array<out Any?>.encodeToBase64(): String = objectMapper.writeValueAsString(this).encodeToBase64()

    private fun String.decodeBase64(): String = String(Base64.getDecoder().decode(this))

    private fun String.encodeToBase64(): String = Base64.getEncoder().encodeToString(this.toByteArray(StandardCharsets.UTF_8))
}