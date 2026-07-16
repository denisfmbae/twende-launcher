package co.nedlink.twende.data.obd

/**
 * Decodes OBD-II Mode 03 trouble codes.
 *
 * Two bytes per code. The top two bits pick the system letter (P/C/B/U), the
 * next two the first digit, and the remaining twelve bits are three hex digits:
 *
 *   A = 0x01, B = 0x33  ->  P0133
 *
 * The description table covers the common generic codes. Manufacturer-specific
 * codes (P1xxx and up) run to many thousands and differ per marque, so anything
 * outside the table is labelled by family rather than guessed at — an honest
 * "powertrain fault, look up this code" beats an invented description.
 */
object DtcCatalog {

    fun decode(a: Int, b: Int): String {
        val letter = when ((a shr 6) and 0x03) {
            0 -> "P"; 1 -> "C"; 2 -> "B"; else -> "U"
        }
        val d1 = (a shr 4) and 0x03
        val d2 = a and 0x0F
        val d3 = (b shr 4) and 0x0F
        val d4 = b and 0x0F
        return "%s%d%X%X%X".format(letter, d1, d2, d3, d4)
    }

    fun describe(code: String): String = TABLE[code] ?: family(code)

    private fun family(code: String): String = when (code.firstOrNull()) {
        'P' -> "Powertrain fault (engine/transmission) — look up $code for this make"
        'C' -> "Chassis fault (ABS/brakes/suspension) — look up $code for this make"
        'B' -> "Body fault (airbag/lighting/comfort) — look up $code for this make"
        'U' -> "Network fault (a control module isn't communicating) — look up $code"
        else -> "Unknown code $code"
    }

    private val TABLE = mapOf(
        // Misfires — the most common thing you'll actually see
        "P0300" to "Random/multiple cylinder misfire",
        "P0301" to "Cylinder 1 misfire detected",
        "P0302" to "Cylinder 2 misfire detected",
        "P0303" to "Cylinder 3 misfire detected",
        "P0304" to "Cylinder 4 misfire detected",
        "P0305" to "Cylinder 5 misfire detected",
        "P0306" to "Cylinder 6 misfire detected",
        // Fuel & air metering
        "P0171" to "System too lean (Bank 1) — often a vacuum leak or dirty MAF",
        "P0172" to "System too rich (Bank 1)",
        "P0174" to "System too lean (Bank 2)",
        "P0175" to "System too rich (Bank 2)",
        "P0101" to "Mass air flow sensor range/performance",
        "P0102" to "Mass air flow sensor circuit low",
        "P0113" to "Intake air temperature sensor circuit high",
        "P0128" to "Coolant thermostat below regulating temperature",
        // Oxygen sensors / catalyst
        "P0130" to "O2 sensor circuit fault (Bank 1, Sensor 1)",
        "P0133" to "O2 sensor slow response (Bank 1, Sensor 1)",
        "P0135" to "O2 sensor heater circuit (Bank 1, Sensor 1)",
        "P0420" to "Catalytic converter efficiency below threshold (Bank 1)",
        "P0430" to "Catalytic converter efficiency below threshold (Bank 2)",
        // Evaporative emissions — very often just a loose fuel cap
        "P0440" to "Evaporative emission system fault",
        "P0442" to "Small evaporative leak — check the fuel cap first",
        "P0455" to "Large evaporative leak — check the fuel cap first",
        "P0456" to "Very small evaporative leak",
        // Ignition / sensors
        "P0335" to "Crankshaft position sensor circuit",
        "P0340" to "Camshaft position sensor circuit",
        "P0327" to "Knock sensor circuit low",
        "P0121" to "Throttle position sensor range/performance",
        "P0122" to "Throttle position sensor circuit low",
        // EGR / idle
        "P0401" to "Exhaust gas recirculation flow insufficient",
        "P0402" to "Exhaust gas recirculation flow excessive",
        "P0505" to "Idle air control system fault",
        "P0506" to "Idle speed lower than expected",
        // Charging / electrical
        "P0562" to "System voltage low — check alternator and battery",
        "P0563" to "System voltage high",
        // Transmission
        "P0700" to "Transmission control system fault",
        "P0715" to "Input/turbine speed sensor circuit",
    )
}
