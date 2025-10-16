package org.dema.graphql.dgs

import graphql.schema.Coercing
import java.time.LocalDateTime

interface LocalDateTimeScalar : Coercing<LocalDateTime, String>
