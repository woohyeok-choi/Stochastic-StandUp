package kaist.iclab.standup.smi.repository

import kaist.iclab.standup.smi.data.Mission
import smile.math.BFGS
import smile.math.DifferentiableMultivariateFunction
import smile.math.MathEx
import java.util.*
import java.util.stream.IntStream
import kotlin.math.exp

class StochasticIncentiveRepository : IncentiveRepository {
    override fun calculateStochasticIncentive(
        missions: List<Mission>,
        timestamp: Long,
        latitude: Double,
        longitude: Double
    ): Int? {
        return 0
    }

    class OneHotEncoder private constructor(private val categories: Array<Array<String>>, private val dropFirst: Boolean) {
        fun transform(data: Array<String>) : DoubleArray {
            require(data.size == categories.size)
            return data.mapIndexed { idx, value ->
                val nCategory = categories[idx].size
                val order = categories[idx].indexOf(value)

                DoubleArray(if(dropFirst) nCategory - 1 else nCategory) {
                    val adjustOrder = if(dropFirst) order - 1 else order
                    if (it == adjustOrder) 1.0 else 0.0
                }.toList()
            }.flatten().toDoubleArray()
        }

        companion object {
            fun fit(data: Array<Array<String>>, dropFirst: Boolean): OneHotEncoder =
                OneHotEncoder(Array(data[0].size) {
                        index -> data.map { it[index] }.distinct().toTypedArray()
                }, dropFirst)
        }
    }

    class BinaryLogisticRegression(val w: DoubleArray, val logLikelihood: Double, private val C: Double = 0.1) {
        private val p = w.size - 1
        private var learningRate = 0.1

        internal class BinaryObjectiveFunction(
            private val x: Array<DoubleArray>,
            private val y: IntArray,
            private val C: Double
        ) : DifferentiableMultivariateFunction {
            override fun f(w: DoubleArray): Double {
                val f = IntStream.range(0, x.size).parallel().mapToDouble { i: Int ->
                    val wx = dotProduct(x[i], w)
                    MathEx.log1pe(wx) - y[i] * wx
                }.sum()

                return if (C > 0.0) {
                    f + 0.5 * C * w.sumByDouble { it * it }
                } else {
                    f
                }
            }

            override fun g(w: DoubleArray, g: DoubleArray): Double {
                val p = x[0].size
                val partitionSize = Integer.valueOf(1000)
                val partitions = x.size / partitionSize + if (x.size % partitionSize == 0) 0 else 1
                val gradients = Array(partitions) { DoubleArray(p + 1) { 0.0 } }

                val f = IntStream.range(0, partitions).parallel().mapToDouble { r ->
                    val gradient = gradients[r]
                    val begin = r * partitionSize
                    val end = ((r + 1) * partitionSize).coerceAtMost(x.size)

                    IntStream.range(begin, end).sequential().mapToDouble { i ->
                        val xi = x[i]
                        val wx = dotProduct(xi, w)
                        val err = y[i] - MathEx.logistic(wx)
                        for (j in 0 until p) {
                            gradient[j] -= err * xi[j]
                        }
                        gradient[p] -= err
                        MathEx.log1pe(wx) - y[i] * wx
                    }.sum()
                }.sum()

                Arrays.fill(g, 0.0)

                for (gradient in gradients) {
                    for (i in g.indices) {
                        g[i] += gradient[i]
                    }
                }

                return if (C > 0.0) {
                    var wnorm = 0.0
                    for (i in 0 until p) {
                        wnorm += w[i] * w[i]
                        g[i] += C * w[i]
                    }
                    f + 0.5 * C * wnorm
                } else {
                    f
                }
            }
        }

        fun update(x: DoubleArray, y: Int) {
            require(x.size == p) { "Invalid input vector size: " + x.size }

            val wx = dotProduct(x, w)
            val err = y - MathEx.logistic(wx)

            w[p] += learningRate * err
            for (j in 0 until p) {
                w[j] += learningRate * err * x[j]
            }

            if (C > 0.0) {
                for (j in 0 until p) {
                    w[j] -= learningRate * C * w[j]
                }
            }
        }

        fun predict(x: DoubleArray): Int {
            val f = 1.0 / (1.0 + exp(-dotProduct(x, w)))
            return if (f < 0.5) 0 else 1
        }

        fun predictProbability(x: DoubleArray): Double {
            return 1.0 / (1.0 + exp(-dotProduct(x, w)))
        }

        companion object {
            private fun dotProduct(x: DoubleArray, w: DoubleArray): Double {
                var dot = w[x.size]
                for (i in x.indices) {
                    dot += x[i] * w[i]
                }
                return dot
            }

            fun fit(
                x: Array<DoubleArray>,
                y: IntArray,
                C: Double = 0.1,
                tol: Double = 1e-5,
                maxIter: Int = 500
            ): BinaryLogisticRegression {
                require(x.size == y.size)
                require(C >= 0.0) { "Invalid regularization factor: $C" }
                require(tol > 0.0) { "Invalid tolerance: $tol" }
                require(maxIter > 0) { "Invalid maximum number of iterations: $maxIter" }

                val p = x[0].size
                val bfgs = BFGS(tol, maxIter)
                val w = DoubleArray(p + 1)
                val logLikelihood = -bfgs.minimize(BinaryObjectiveFunction(x, y, C), 5, w)
                val model = BinaryLogisticRegression(w, logLikelihood, C)
                model.learningRate = 0.1 / x.size
                return model
            }
        }
    }
}