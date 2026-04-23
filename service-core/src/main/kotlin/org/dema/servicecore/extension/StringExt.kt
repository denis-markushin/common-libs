package org.dema.servicecore.extension

import java.util.UUID

fun String.uuid() = UUID.fromString(this)
