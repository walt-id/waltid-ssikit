package id.walt.cli

import id.walt.servicematrix.ServiceMatrix
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.StringSpec


class VcTemplatesCommandTest : StringSpec({

    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")

    "vc templates list" {
        VcTemplatesListCommand().parse(listOf())
    }

    "vc templates export" {
        // TODO WALT0508
        //VcTemplatesExportCommand().parse(listOf("Europass"))
    }
})
