package org.dema.common

import java.util.UUID

fun String.uuid() = UUID.fromString(this)
