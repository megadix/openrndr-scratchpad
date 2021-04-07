package megadix

import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.color.ColorRGBa.Companion.RED
import org.openrndr.color.ColorRGBa.Companion.TRANSPARENT
import org.openrndr.extra.noclear.NoClear
import org.openrndr.extra.noise.perlinLinear
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Vector2
import kotlin.math.abs
import kotlin.random.Random

fun main() = application {
    val goFullscreen = false
    val randomSeed = 100

    configure {
        if (goFullscreen) {
            fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
        } else {
            width = 1080
            height = 600
            windowResizable = true
        }
    }

    val drops = mutableListOf<Vector2>()

    oliveProgram {
        drawer.clear(TRANSPARENT)

        mouse.dragged.listen {
            drops.add(it.position)
        }

        extend(NoClear())

        extend {
            val iterator = drops.listIterator()

            while (iterator.hasNext()) {
                val drop = iterator.next()

                if (drop.y > height || Random.nextDouble() < 0.001) {
                    iterator.remove()
                    continue
                }

                drawer.stroke = RED
                drawer.fill = RED

                val noiseX = perlinLinear(iterator.nextIndex(), drop.y)

                if (drop.y < height.toDouble()) {
                    drawer.circle(drop, (abs(noiseX) + 1.0) * 2.0)
                }

                iterator.set(
                    Vector2(
                        drop.x + noiseX * 3.0,
                        drop.y + 0.5
                    )
                )
            }
        }

    }
}