package org.dema.common

import java.util.UUID

fun Map<String, *>.uuidOrNull(name: String): UUID? = when (val v = this[name]) {
    is UUID -> v
    is CharSequence -> runCatching { UUID.fromString(v.toString()) }.getOrNull()
    else -> null
}

fun Map<String, *>.requireUuid(name: String): UUID =
    requireNotNull(uuidOrNull(name)) { "Key: [$name] is missing or not a valid UUID" }
