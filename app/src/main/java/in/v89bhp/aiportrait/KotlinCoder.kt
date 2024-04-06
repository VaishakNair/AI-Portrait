package `in`.v89bhp.aiportrait

fun main() {
    println("Hello, world!")

    for (i in 1..3) {
        println(i)
    }
//    print("Result: ${solution(intArrayOf())}")

//    whenExpression()

    val shape: Shape = Rectangle()

    shape.printVertexCount()
}

fun solution(A: IntArray): Int {
    // Implement your solution here
    val inputSorted = A.toSet().sorted()
    for (minValue in inputSorted.withIndex()) {
        if (minValue.value <= 0) return if(1 !in inputSorted) 1 else continue
        else if (minValue.index < inputSorted.size - 1) {
            if (minValue.value + 1 != inputSorted[minValue.index + 1]) {
                return minValue.value + 1
            } else {
                continue
            }
        } else {
            return minValue.value + 1
        }
    }
    return 1
}

fun whenExpression() {
    when(1) {
        1 -> println(fruitsList)
        1 -> println("sdfdf")
        else -> println("Less than 2")
    }
}

val fruitsList = listOf("Apple", "Orange", "Banana")

open class Shape {
    open val vertexCount = 0

    fun printVertexCount() {
        println("Vertex count: $vertexCount")
    }
}

class Rectangle : Shape() {
    override val vertexCount = 4
}

open class Size {
    open val height: Int = 0
}
// Only one class may appear in a supertype list:
//class Thing : Shape(), Size()
