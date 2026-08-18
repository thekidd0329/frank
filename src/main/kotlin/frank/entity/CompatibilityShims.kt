package frank.entity

fun Double.coerceIn(minimumValue: Float, maximumValue: Float): Float =
    coerceIn(minimumValue.toDouble(), maximumValue.toDouble()).toFloat()

fun <T> ArrayDeque<T>.addFirst(element: T) = add(0, element)
