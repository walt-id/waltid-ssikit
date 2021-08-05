package id.walt.cli

import id.walt.servicematrix.ServiceMatrix
import io.kotest.core.spec.style.StringSpec
import id.walt.cli.*


class VcTemplatesCommandTest : StringSpec({

    ServiceMatrix("service-matrix.properties")

    "vc templates list" {
        VcTemplatesListCommand().parse(listOf())
    }

    "vc templates export" {
        // TODO WALT0508
        //VcTemplatesExportCommand().parse(listOf("Europass"))
    }
})
