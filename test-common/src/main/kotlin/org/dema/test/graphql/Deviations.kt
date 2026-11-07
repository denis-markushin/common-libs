package org.dema.test.graphql

import graphql.language.DescribedNode
import graphql.language.EnumTypeDefinition
import graphql.language.FieldDefinition
import graphql.language.ImplementingTypeDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.TypeDefinition
import graphql.parser.Parser
import graphql.parser.ParserEnvironment
import graphql.parser.ParserOptions
import graphql.schema.idl.TypeUtil

/**
 * Deviations of a GraphQL schema from the conventions, one line each: a bare id in an output type
 * (`id Zorg.vendorId`), a plain list in a query namespace instead of a Relay connection
 * (`list ZorgQueries.all`) and a missing description (`description Zorg.name`).
 */
object Deviations {
    fun of(sdl: String): Set<String> {
        val environment = ParserEnvironment.newParserEnvironment()
            .document(sdl)
            .parserOptions(ParserOptions.getDefaultSdlParserOptions())
            .build()
        return Parser.parse(environment).definitions
            .filterIsInstance<TypeDefinition<*>>()
            .flatMap { ids(it) + lists(it) + descriptions(it) }
            .toSet()
    }

    private fun ids(type: TypeDefinition<*>): List<String> =
        fields(type)
            .filterNot { type.name.endsWith("Queries") || type.name.endsWith("Mutations") }
            .map { "${type.name}.${it.name}" }
            .filter { it.endsWith("Id") || it.endsWith("Ids") }
            .filterNot { it.endsWith(".recordId") || it == "RefusalError.entityId" }
            .map { "id $it" }

    private fun lists(type: TypeDefinition<*>): List<String> =
        fields(type)
            .filter { type.name.endsWith("Queries") && TypeUtil.simplePrint(it.type).startsWith("[") }
            .map { "list ${type.name}.${it.name}" }

    private fun descriptions(type: TypeDefinition<*>): List<String> =
        (own(type) + members(type))
            .filter { (_, node) -> node.description == null }
            .map { (name, _) -> "description $name" }

    private fun own(type: TypeDefinition<*>): List<Pair<String, DescribedNode<*>>> =
        if (type.name in setOf("Query", "Mutation") || type.javaClass.simpleName.endsWith("ExtensionDefinition")) {
            emptyList()
        } else {
            listOf(type.name to type as DescribedNode<*>)
        }

    private fun members(type: TypeDefinition<*>): List<Pair<String, DescribedNode<*>>> =
        when (type) {
            is EnumTypeDefinition -> type.enumValueDefinitions.map { "${type.name}.${it.name}" to it }
            is InputObjectTypeDefinition -> type.inputValueDefinitions.map { "${type.name}.${it.name}" to it }
            else -> fields(type).flatMap { field -> outputs(type.name, field) }
        }

    private fun outputs(owner: String, field: FieldDefinition): List<Pair<String, DescribedNode<*>>> {
        val name = "$owner.${field.name}"
        val contract = setOf("recordId", "record", "error", "pageInfo", "edges", "cursor", "node")
        val own = if (field.name in contract) emptyList() else listOf(name to field)
        return own + field.inputValueDefinitions.map { "$name(${it.name})" to it }
    }

    private fun fields(type: TypeDefinition<*>): List<FieldDefinition> =
        (type as? ImplementingTypeDefinition<*>)?.fieldDefinitions.orEmpty()
}
