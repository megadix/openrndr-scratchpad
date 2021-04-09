package megadix

import mu.KotlinLogging
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorRGBa.Companion.BLACK
import org.openrndr.color.ColorRGBa.Companion.WHITE
import org.openrndr.color.ColorXSVa
import org.openrndr.draw.loadFont
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.Vector2.Companion.ZERO
import org.openrndr.math.map
import kotlin.math.pow
import kotlin.math.round
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

val G = 6.674.times(10.0.pow(-11))
val SOLARMASS = 1.98892 * 10.0.pow(30)

const val numInitBodies = 500
const val randomSeed = 3

val minMass = SOLARMASS / 100.0
val maxMass = SOLARMASS * 10.0.pow(3)

val blackHoleMass = maxMass * 10.0.pow(3)

// max distance from the center of the universe at the start of simulation
val maxInitDistance = 10.0.pow(9)
val screenZoom = maxInitDistance * 3.0
val tooFar = maxInitDistance * 3.0

const val centralBlackHole = true
//const val centralBlackHole = false

// best with centralBlackHole = true
val maxInitVelocity = 3.0 * 10.0.pow(4)
// best with centralBlackHole = false
//val maxInitVelocity = 0.8 * 10.0.pow(4)

val random = Random(randomSeed)

data class Body(
    var mass: Double = 0.0,
    var position: Vector2 = ZERO,
    var velocity: Vector2 = ZERO,
    var force: Vector2 = ZERO,
    var color: ColorRGBa = WHITE
) {
    fun update(dt: Double) {
        velocity = Vector2(
            velocity.x + dt * force.x / mass,
            velocity.y + dt * force.y / mass
        )
        position = Vector2(
            position.x + dt * velocity.x,
            position.y + dt * velocity.y
        )
    }
}

fun main(args: Array<String>) = application {
    val gui = GUI()

    val settings = object {
        @DoubleParameter("Grav. atten. factor", 5.0, 12.0)
        var softenFactor = 9.0

        @DoubleParameter("dt factor", 1.0, 5.0)
        var dtFactor = 2.5
    }

    val useDefaults = args.isNotEmpty() && args.any { it.contains("(-d)|(--defaults)".toRegex()) }

    fun newBody(): Body {
        val mass = random.nextDouble(minMass, maxMass)

        val position = Polar(
            random.nextDouble(0.0, 360.0),
            random.nextDouble(maxInitDistance),
        )
        val positionCartesian = position.cartesian

        val velocity = Polar(
            position.theta + 90.0,
            map(0.0, maxInitDistance, 0.0, maxInitVelocity, position.radius, true)
        ).cartesian

        return Body(
            mass,
            positionCartesian,
            velocity,
            color = ColorXSVa(
                random.nextDouble(0.0, 360.0),
                map(minMass, maxMass, 0.0, 1.0, mass, true),
                map(minMass, maxMass, 0.2, 1.0, mass, true)
            ).toRGBa()
        )
    }

    fun initBodies(): MutableList<Body> {
        return (1..numInitBodies).map { newBody() }.toMutableList()
    }

    fun addGravitationalForce(body1: Body, body2: Body, soften: Double) {
        val r = body1.position.distanceTo(body2.position)
        val r2 = r.pow(2)
        val scalarForce = G * body1.mass * body2.mass / (r2 * soften)
        body1.force += (body2.position - body1.position).normalized * scalarForce
    }

    fun applyAllGravitationalForces(body1: Body, bodies: MutableList<Body>, soften: Double) {
        body1.force = ZERO

        for (body2 in bodies) {
            if (body1 !== body2) {
                addGravitationalForce(body1, body2, soften)
            }
        }
    }

    configure {
        width = 1080
        height = 800
    }

    program {
        if (!useDefaults) {
            gui.add(settings, "Settings")
            extend(gui)
            gui.visible = false
        }

        val font = loadFont("data/fonts/default.otf", 18.0)
        drawer.fontMap = font

        val bodies = initBodies()

        val blackHole = if (centralBlackHole) Body(
            blackHoleMass,
            color = BLACK, // OF COURSE
        ) else null

        var lastFramerate: Double

        extend {
            val soften = 10.0.pow(settings.softenFactor)
            val dt = 10.0.pow(settings.dtFactor)

            drawer.clear(BLACK)

            val widthDouble = width.toDouble()
            val heightDouble = height.toDouble()

            val iter = bodies.listIterator()

            if (logger.isDebugEnabled && frameCount % 50 == 0 && seconds > 0.5) {
                lastFramerate = round(1.0 / deltaTime)
                logger.debug { "Frame rate: $lastFramerate" }
            }

            while (iter.hasNext()) {
                val it = iter.next()

                if (it.position.length > tooFar) {
                    logger.debug { "Removed body too far" }
                    iter.remove()
                    iter.add(newBody())
                }

                // draw current state of the system

                drawer.fill = it.color

                val screenPosition = Vector2(
                    map(-screenZoom, screenZoom, 0.0, widthDouble, it.position.x),
                    map(-screenZoom, screenZoom, 0.0, heightDouble, it.position.y)
                )

                drawer.point(screenPosition)

                // update gravitational forces actiong on this body
                applyAllGravitationalForces(it, bodies, soften)
                // apply also black hole gravitation
                if (centralBlackHole) {
                    addGravitationalForce(it, blackHole!!, soften)
                }

                // update position and velocity for next draw
                it.update(dt)
            }
        }
    }

}