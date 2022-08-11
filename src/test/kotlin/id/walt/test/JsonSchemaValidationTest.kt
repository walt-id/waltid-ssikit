package id.walt.test

import id.walt.common.readWhenContent
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import net.pwall.json.schema.JSONSchema

class JsonSchemaValidationTest: StringSpec({

    "schema validation should return expected".config(enabled = false) {
        forAll(
            row(
                "src/main/resources/velocity/schemas/organization-registration-responseSchema.json",
                "src/test/resources/velocity/organization-registration-response.json",
                true
            ),
            row(
                "src/main/resources/velocity/schemas/organization-registration-responseSchema.json",
                "src/test/resources/velocity/organization-registration-error.json",
                false
            ),
            row(
                "src/main/resources/velocity/schemas/organization-registration-requestSchema.json",
                "src/test/resources/velocity/organization-registration-request.json",
                true
            ),
            row(
                "src/main/resources/velocity/schemas/organization-registration-requestSchema.json",
                "src/test/resources/velocity/organization-registration-error.json",
                false
            ),
        ) { schemaPath, filepath, expected ->

            val schema = JSONSchema.parseFile(schemaPath)
            val response = readWhenContent(java.io.File(filepath))

            val validation = schema.validateBasic(response)

            validation.valid shouldBe expected
        }
    }
})