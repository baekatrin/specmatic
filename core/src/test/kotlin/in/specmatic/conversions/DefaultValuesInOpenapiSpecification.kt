package `in`.specmatic.conversions

import `in`.specmatic.core.Flags
import `in`.specmatic.core.GenerativeTestsEnabled
import `in`.specmatic.core.HttpRequest
import `in`.specmatic.core.HttpResponse
import `in`.specmatic.core.pattern.JSONObjectPattern
import `in`.specmatic.core.pattern.parsedJSONObject
import `in`.specmatic.core.value.*
import `in`.specmatic.test.TestExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class DefaultValuesInOpenapiSpecification {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            System.setProperty("SCHEMA_EXAMPLE_DEFAULT", "true")
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            System.setProperty("SCHEMA_EXAMPLE_DEFAULT", "false")
        }
    }

    @Test
    fun `schema examples should be used as default values`() {
        try {
            System.setProperty(Flags.schemaExampleDefault, "true")

            val specification = OpenApiSpecification.fromYAML(
                """
            openapi: 3.0.1
            info:
              title: Employee API
              version: 1.0.0
            paths:
              /employees:
                post:
                  requestBody:
                    content:
                      application/json:
                        schema:
                          ${'$'}ref: '#/components/schemas/Employee'
                  responses:
                    '200':
                      description: OK
                      content:
                        text/plain:
                          schema:
                            type: string
            components:
              schemas:
                Employee:
                  type: object
                  properties:
                    name:
                      type: string
                      example: 'Jane Doe'
                    age:
                      type: integer
                      format: int32
                      example: 35
                    salary:
                      type: number
                      format: double
                      nullable: true
                      example: 50000
                    salary_history:
                      type: array
                      items:
                        type: number
                        example: 1000
                    years_employed:
                      type: array
                      items:
                        type: number
                      example:
                        - 2021
                        - 2022
                        - 2023
                  required:
                    - name
                    - age
                    - salary
                    - years_employed
            """.trimIndent(), ""
            ).toFeature()

            val withGenerativeTestsEnabled = specification.copy(
                generativeTestingEnabled = true,
                resolverStrategies = specification.resolverStrategies.copy(generation = GenerativeTestsEnabled())
            )

            val testTypes = mutableListOf<String>()

            val results = withGenerativeTestsEnabled.executeTests(object : TestExecutor {
                override fun execute(request: HttpRequest): HttpResponse {
                    val body = request.body as JSONObjectValue

                    println(body.toStringLiteral())

                    if ("salary" in body.jsonObject && body.jsonObject["salary"] !is NumberValue) {
                        testTypes.add("salary mutated to ${body.jsonObject["salary"]!!.displayableType()}")
                        return HttpResponse.ERROR_400
                    }

                    if (body.jsonObject["name"] !is StringValue) {
                        testTypes.add("name mutated to ${body.jsonObject["name"]!!.displayableType()}")
                        return HttpResponse.ERROR_400
                    }

                    if (body.jsonObject["age"] !is NumberValue) {
                        testTypes.add("age mutated to ${body.jsonObject["age"]!!.displayableType()}")
                        return HttpResponse.ERROR_400
                    }

                    if("salary_history" in body.jsonObject && body.jsonObject["salary_history"] !is JSONArrayValue) {
                        testTypes.add("salary_history mutated to ${body.jsonObject["salary_history"]!!.displayableType()}")
                        return HttpResponse.ERROR_400
                    }

                    if("salary_history" in body.jsonObject && body.jsonObject["salary_history"] is JSONArrayValue && (body.jsonObject["salary_history"]!! as JSONArrayValue).list.any { it !is NumberValue }) {
                        val item = (body.jsonObject["salary_history"]!! as JSONArrayValue).list.first { it !is NumberValue }
                        testTypes.add("salary_history[item] mutated to ${item.displayableType()}")
                        return HttpResponse.ERROR_400
                    }

                    if("years_employed" in body.jsonObject && body.jsonObject["years_employed"] !is JSONArrayValue) {
                        testTypes.add("years_employed mutated to ${body.jsonObject["years_employed"]!!.displayableType()}")
                        return HttpResponse.ERROR_400
                    }

                    if("years_employed" in body.jsonObject && body.jsonObject["years_employed"] is JSONArrayValue && (body.jsonObject["years_employed"]!! as JSONArrayValue).list.any { it !is NumberValue }) {
                        val item = (body.jsonObject["years_employed"]!! as JSONArrayValue).list.first { it !is NumberValue }
                        testTypes.add("years_employed[item] mutated to ${item.displayableType()}")
                        return HttpResponse.ERROR_400
                    }

                    assertThat(body.jsonObject["name"]).isEqualTo(StringValue("Jane Doe"))
                    assertThat(body.jsonObject["age"]).isEqualTo(NumberValue(35))

                    if ("salary" in body.jsonObject) {
                        testTypes.add("salary is present")
                    } else {
                        testTypes.add("salary is absent")
                    }

                    if ("salary" in body.jsonObject) {
                        assertThat(body.jsonObject["salary"]).isEqualTo(NumberValue(50000))
                    }

                    if("salary_history" in body.jsonObject) {
                        assertThat((body.jsonObject["salary_history"] as JSONArrayValue).list).containsOnly(NumberValue(1000))
                    }

                    if("years_employed" in body.jsonObject) {
                        assertThat((body.jsonObject["years_employed"] as JSONArrayValue).list).contains(NumberValue(2021), NumberValue(2022), NumberValue(2023))
                    }

                    return HttpResponse.OK
                }

                override fun setServerState(serverState: Map<String, Value>) {

                }
            })

            assertThat(testTypes).containsExactlyInAnyOrder(
                "name mutated to null",
                "name mutated to number",
                "name mutated to boolean",
                "age mutated to null",
                "age mutated to boolean",
                "age mutated to string",
                "salary mutated to boolean",
                "salary mutated to string",
                "salary_history mutated to null",
                "years_employed mutated to null",
                "name mutated to null",
                "name mutated to number",
                "name mutated to boolean",
                "age mutated to null",
                "age mutated to boolean",
                "age mutated to string",
                "salary mutated to boolean",
                "salary mutated to string",
                "years_employed mutated to null",
                "salary is present",
                "salary is present"
            )
            assertThat(results.results).hasSize(testTypes.size)
        } finally {
            System.clearProperty(Flags.schemaExampleDefault)
        }
    }

    @Test
    fun `named examples should be given preference over schema examples`() {
        try {
            System.setProperty(Flags.schemaExampleDefault, "true")

            val specification = OpenApiSpecification.fromYAML(
                """
            openapi: 3.0.1
            info:
              title: Employee API
              version: 1.0.0
            paths:
              /employees:
                post:
                  requestBody:
                    content:
                      application/json:
                        schema:
                          ${'$'}ref: '#/components/schemas/Employee'
                        examples:
                          SUCCESS:
                            value:
                              name: 'John Doe'
                              age: 30
                  responses:
                    '200':
                      description: OK
                      content:
                        text/plain:
                          schema:
                            type: string
                          examples:
                            SUCCESS:
                              value: 'success'
                    '400':
                        description: Bad Request
                        content:
                          text/plain:
                            schema:
                              type: string
            components:
              schemas:
                Employee:
                  type: object
                  properties:
                    name:
                      type: string
                      example: 'Jane Doe'
                    age:
                      type: integer
                      format: int32
                      example: 35
                    salary:
                      type: number
                      format: double
                      nullable: true
                      example: 50000
                  required:
                    - name
                    - age
            """.trimIndent(), ""
            ).toFeature()

            val withGenerativeTestsEnabled = specification.copy(
                generativeTestingEnabled = true,
                resolverStrategies = specification.resolverStrategies.copy(generation = GenerativeTestsEnabled())
            )

            val testTypes = mutableListOf<String>()

            val results = withGenerativeTestsEnabled.executeTests(object : TestExecutor {
                override fun execute(request: HttpRequest): HttpResponse {
                    val body = request.body as JSONObjectValue

                    if ("salary" in body.jsonObject && body.jsonObject["salary"] !is NumberValue) {
                        testTypes.add("salary mutated to ${body.jsonObject["salary"]!!.displayableType()}")
                        return HttpResponse.ERROR_400
                    }

                    if (body.jsonObject["name"] !is StringValue) {
                        testTypes.add("name mutated to ${body.jsonObject["name"]!!.displayableType()}")
                        return HttpResponse.ERROR_400
                    }

                    if (body.jsonObject["age"] !is NumberValue) {
                        testTypes.add("age mutated to ${body.jsonObject["age"]!!.displayableType()}")
                        return HttpResponse.ERROR_400
                    }

                    assertThat(body.jsonObject["name"]).isEqualTo(StringValue("John Doe"))
                    assertThat(body.jsonObject["age"]).isEqualTo(NumberValue(30))

                    if ("salary" in body.jsonObject) {
                        testTypes.add("salary is present")
                    } else {
                        testTypes.add("salary is absent")
                    }

                    if ("salary" in body.jsonObject) {
                        assertThat(body.jsonObject["salary"]).isEqualTo(NumberValue(50000))
                    }

                    return HttpResponse.OK
                }

                override fun setServerState(serverState: Map<String, Value>) {

                }
            })

            assertThat(testTypes).containsExactlyInAnyOrder(
                "salary is present",
                "salary is absent",
                "name mutated to null",
                "name mutated to number",
                "name mutated to boolean",
                "age mutated to null",
                "age mutated to boolean",
                "age mutated to string",
                "salary mutated to boolean",
                "salary mutated to string",
                "name mutated to null",
                "name mutated to number",
                "name mutated to boolean",
                "age mutated to null",
                "age mutated to boolean",
                "age mutated to string"
            )
            assertThat(results.results).hasSize(testTypes.size)
        } finally {
            System.clearProperty(Flags.schemaExampleDefault)
        }
    }
}