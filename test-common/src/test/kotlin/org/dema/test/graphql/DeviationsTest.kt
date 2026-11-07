package org.dema.test.graphql

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import org.junit.jupiter.api.Test

class DeviationsTest {

    @Test
    fun `bare id in an output type is a deviation`() {
        assertThat(Deviations.of("type Zorg { vendorId: UUID! }"), "bare id slipped through").contains("id Zorg.vendorId")
    }

    @Test
    fun `bare id list in an output type is a deviation`() {
        assertThat(Deviations.of("type Zorg { vendorIds: [UUID!]! }"), "list of bare ids slipped through").contains("id Zorg.vendorIds")
    }

    @Test
    fun `bare id in an interface is a deviation`() {
        assertThat(Deviations.of("interface Zorg { vendorId: UUID! }"), "bare id of an interface slipped through").contains("id Zorg.vendorId")
    }

    @Test
    fun `bare id in an input type is no deviation`() {
        assertThat(Deviations.of("input Zorg { vendorId: UUID! }"), "input taken for a bare id").doesNotContain("id Zorg.vendorId")
    }

    @Test
    fun `record id of a result is no deviation`() {
        assertThat(Deviations.of("type Zorg { recordId: UUID }"), "recordId taken for a bare id").doesNotContain("id Zorg.recordId")
    }

    @Test
    fun `entity id of a refusal is no deviation`() {
        assertThat(Deviations.of("type RefusalError { entityId: UUID }"), "error contract taken for a bare id")
            .doesNotContain("id RefusalError.entityId")
    }

    @Test
    fun `entity id outside a refusal is a deviation`() {
        assertThat(Deviations.of("type Zorg { entityId: UUID }"), "bare entityId slipped through").contains("id Zorg.entityId")
    }

    @Test
    fun `lookup by id in a namespace is no deviation`() {
        assertThat(Deviations.of("type ZorgQueries { byId(id: UUID!): Zorg }"), "operation taken for a bare id")
            .doesNotContain("id ZorgQueries.byId")
    }

    @Test
    fun `list in a namespace is a deviation`() {
        assertThat(Deviations.of("type ZorgQueries { all: [Zorg!]! }"), "root list slipped through").contains("list ZorgQueries.all")
    }

    @Test
    fun `nullable list in a namespace is a deviation`() {
        assertThat(Deviations.of("type ZorgQueries { all: [Zorg] }"), "nullable root list slipped through").contains("list ZorgQueries.all")
    }

    @Test
    fun `connection in a namespace is no deviation`() {
        assertThat(Deviations.of("type ZorgQueries { all: ZorgConnection! }"), "connection taken for a list")
            .doesNotContain("list ZorgQueries.all")
    }

    @Test
    fun `list inside a type is no deviation`() {
        assertThat(Deviations.of("type Zorg { parts: [Part!]! }"), "nested list taken for a root list")
            .doesNotContain("list Zorg.parts")
    }

    @Test
    fun `undescribed type is a deviation`() {
        assertThat(Deviations.of("type Zorg { name: String }"), "type without a description slipped through").contains("description Zorg")
    }

    @Test
    fun `undescribed field is a deviation`() {
        assertThat(Deviations.of("type Zorg { name: String }"), "field without a description slipped through").contains("description Zorg.name")
    }

    @Test
    fun `undescribed input field is a deviation`() {
        assertThat(Deviations.of("input Zorg { name: String }"), "input field without a description slipped through")
            .contains("description Zorg.name")
    }

    @Test
    fun `undescribed argument is a deviation`() {
        assertThat(Deviations.of("type Zorg { name(tone: Int): String }"), "argument without a description slipped through")
            .contains("description Zorg.name(tone)")
    }

    @Test
    fun `undescribed enum value is a deviation`() {
        assertThat(Deviations.of("enum Zorg { AMBER }"), "enum value without a description slipped through").contains("description Zorg.AMBER")
    }

    @Test
    fun `contract fields need no description`() {
        val sdl = """
            "Outcome"
            type Zorg { recordId: UUID record: Zorg error: Fault pageInfo: Page edges: [Edge] cursor: String node: Zorg }
        """
        assertThat(Deviations.of(sdl), "contract fields demand a description").isEmpty()
    }

    @Test
    fun `contract name in an input needs a description`() {
        assertThat(Deviations.of("input Zorg { record: String }"), "input field hid behind a contract name")
            .contains("description Zorg.record")
    }

    @Test
    fun `type extension needs no description of its own`() {
        val sdl = """
            extend type Query {
                "Zorgs"
                zorg: ZorgQueries!
            }
        """
        assertThat(Deviations.of(sdl), "type extension demands a description").isEmpty()
    }

    @Test
    fun `root anchors need no description`() {
        assertThat(Deviations.of("type Query type Mutation"), "empty schema roots demand a description").isEmpty()
    }
}
