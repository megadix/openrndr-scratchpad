package megadix.lab

import org.openrndr.application
import org.openrndr.extra.midi.MidiDeviceDescription
import org.openrndr.extra.midi.MidiTransceiver
import org.openrndr.panel.ControlManager
import org.openrndr.panel.elements.dropdownButton
import org.openrndr.panel.elements.item
import org.openrndr.panel.layout


fun main(args: Array<String>) = application {

    val settings = object {
        // NOTE: these are MY default values, plu-in your own to skip configuration dialog!
        var midiDevice: MidiDeviceDescription? = MidiDeviceDescription(
            name = "MPK mini 3", vendor = "Unknown vendor",
            receive = true, transmit = true
        )
    }

    val useDefaults = args.isNotEmpty() && args.any { it.contains("(-d)|(--defaults)".toRegex()) }

    if (!useDefaults) {
        /*
         * Configuration dialog
         */
        application {
            program {
                extend(ControlManager()) {
                    layout {
                        dropdownButton {
                            label = "Choose MIDI input devices"

                            MidiDeviceDescription.list()
                                .filter { it.transmit }
                                .forEachIndexed() { i, device ->
                                    item {
                                        label = "${device.name} [${device.vendor}]"
                                        data = device
                                        events.picked.listen {
                                            settings.midiDevice = it.source.data as MidiDeviceDescription?
                                        }
                                    }
                                }

                            // pre-select default value
                            value = items().firstOrNull {
                                val midiDevice: MidiDeviceDescription = it.data as MidiDeviceDescription
                                midiDevice.vendor == settings.midiDevice!!.vendor && midiDevice.name == settings.midiDevice!!.name
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * Main application
     */

    program {
        configure {
            width = 800
            height = 600
        }

        fun checkMidiDevice(): Boolean {
            if (settings.midiDevice == null) {
                println("Input MIDI device is not configured, exiting")
                return false
            }
            return true
        }

        fun initMidiDevice(): MidiTransceiver? {
            return try {
                MidiTransceiver.fromDeviceVendor(
                    settings.midiDevice!!.name,
                    settings.midiDevice!!.vendor
                )
            } catch (e: Exception) {
                println("Error configuring MIDI device: ${e.message}")
                null
            }
        }

        if (!checkMidiDevice()) {
            application.exit()
        }

        val midiController = initMidiDevice()
        if (midiController == null) {
            application.exit()
        } else {
            midiController!!.controlChanged.listen {
                println("control change: channel: ${it.channel}, control: ${it.control}, value: ${it.value}")
            }
            midiController!!.noteOn.listen {
                println("note on: channel: ${it.channel}, key: ${it.note}, velocity: ${it.velocity}")
            }
        }
    }
}