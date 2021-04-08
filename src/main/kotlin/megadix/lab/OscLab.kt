package megadix.lab

import org.openrndr.PresentationMode
import org.openrndr.application
import org.openrndr.extra.osc.OSC

// Reaper DAW master volume
//val channel = "/master/volume"
val channel = "/midilistaction"

fun main() = application {
    configure {
        width = 768
        height = 576
    }

    program {
        val osc = OSC()

        println("OSC listening to: $channel")
        osc.listen(channel) {
            println("Received OSC Event: $it")
        }

        window.presentationMode = PresentationMode.MANUAL
        window.requestDraw()

    }
}
