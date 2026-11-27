package org.demalab.test

import java.util.UUID

fun String.withRandomSuffix(length: Int = 4): String =
    this + "_" + UUID.randomUUID().toString()
        .replace("-", "")
        .take(length)
