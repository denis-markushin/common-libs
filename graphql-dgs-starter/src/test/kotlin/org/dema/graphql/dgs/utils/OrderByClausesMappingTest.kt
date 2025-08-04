package org.dema.graphql.dgs.utils

import assertk.assertAll
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.messageContains
import org.jooq.impl.DSL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

enum class TestSortKey {
    ID_ASC,
    NAME_ASC,
}

class OrderByClausesMappingTest {
    private val idField = DSL.field("id")
    private val nameField = DSL.field("name")

    @BeforeEach
    fun setup() {
        OrderByClausesMapping.clear()
    }

    @Test
    fun `register and retrieve fields`() {
        OrderByClausesMapping.register {
            key(TestSortKey.ID_ASC).fields(idField.asc())
            key(TestSortKey.NAME_ASC).fields(nameField.asc())
        }

        val idFields = OrderByClausesMapping.getFields(TestSortKey.ID_ASC)
        val nameFields = OrderByClausesMapping.getFields(TestSortKey.NAME_ASC)

        assertAll {
            assertThat(idFields).containsExactly(idField.asc())
            assertThat(nameFields).containsExactly(nameField.asc())
        }
    }

    @Test
    fun `duplicate key registration throws`() {
        OrderByClausesMapping.register {
            key(TestSortKey.ID_ASC).fields(nameField.asc())
        }

        assertFailure {
            OrderByClausesMapping.register {
                key(TestSortKey.ID_ASC).fields(nameField.asc())
            }
        }.messageContains("Mapping for key 'TestSortKey.ID_ASC' already exists")
    }

    @Test
    fun `getFields throws for missing key`() {
        assertFailure {
            OrderByClausesMapping.getFields(TestSortKey.NAME_ASC)
        }.messageContains("No mapping found for key: 'TestSortKey.NAME_ASC'")
    }

    @Test
    fun `clear removes all mappings`() {
        val keyToRegister = TestSortKey.ID_ASC

        OrderByClausesMapping.register {
            key(keyToRegister).fields(idField.asc())
        }
        OrderByClausesMapping.clear()

        assertFailure {
            OrderByClausesMapping.getFields(keyToRegister)
        }.hasMessage("No mapping found for key: 'TestSortKey.ID_ASC'")
    }
}