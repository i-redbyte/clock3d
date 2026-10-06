package ru.redbyte.clock3d.gl

/** Encoded in mesh UV.x for per-fragment coloring. */
internal enum class ClockMaterial(val uvX: Float) {
    Face(0f),
    HourHand(1f),
    MinuteHand(2f),
    SecondHand(3f),
    Digit(4f),
    Rim(5f),
    Accent(6f),
    Backdrop(7f),
    Mist(8f),
}
