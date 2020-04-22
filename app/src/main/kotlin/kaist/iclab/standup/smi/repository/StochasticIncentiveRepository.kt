package kaist.iclab.standup.smi.repository

import kaist.iclab.standup.smi.common.AppLog
import kaist.iclab.standup.smi.common.throwError
import kaist.iclab.standup.smi.common.toGeoHash
import kaist.iclab.standup.smi.pref.RemotePrefs
import smile.math.BFGS
import smile.math.DifferentiableMultivariateFunction
import smile.math.MathEx
import java.util.*
import java.util.stream.IntStream
import kotlin.math.abs

class StochasticIncentiveRepository : IncentiveRepository {
    override fun calculateStochasticIncentive(
        histories: List<IncentiveHistory>,
        timestamp: Long,
        latitude: Double,
        longitude: Double
    ): Int? {
        if (histories.isEmpty()) {
            AppLog.d(javaClass, "calculateStochasticIncentive(): Empty histories")
            return RemotePrefs.defaultIncentives
        }

        val curContext = (latitude to longitude).toGeoHash(7) ?: return null

        val prevHistoriesInCurContext = histories.filter {
            (it.latitude to it.longitude).toGeoHash(7) == curContext
        }

        /**
         * Case 1: There is no history corresponding to this context; return default incentives
         */
        if (prevHistoriesInCurContext.isEmpty()) {
            AppLog.d(javaClass, "calculateStochasticIncentive(): No history corresponding to $curContext ($latitude, $longitude)")
            return RemotePrefs.defaultIncentives
        }

        /**
         * Case 2: There are all successes corresponding to this context; return minimum incentives
         */
        if (prevHistoriesInCurContext.all { it.isSucceeded }) {
            AppLog.d(javaClass, "calculateStochasticIncentive(): All succeeded histories corresponding to $curContext ($latitude, $longitude)")
            return RemotePrefs.minIncentives
        }

        /**
         * Case 3: There are all failures corresponding to this context; return maximum incentives
         */
        if (prevHistoriesInCurContext.all { !it.isSucceeded }) {
            AppLog.d(javaClass, "calculateStochasticIncentive(): All failed histories corresponding to $curContext ($latitude, $longitude)")
            return RemotePrefs.maxIncentives
        }

        /**
         * Case 4: Normal; mixed with successes and failures
         */

        val trainContexts = histories.map {
            val context = (it.latitude to it.longitude).toGeoHash(7) ?: ""
            arrayOf(context)
        }.toTypedArray()

        val trainIncentives = histories.map { abs(it.incentive.toDouble()) }.toDoubleArray()

        val encoder = OneHotEncoder.fit(data = trainContexts, dropFirst = true)
        val encodedTrainContexts = trainContexts.map { encoder.transform(it) }
        val encodedCurContext = encoder.transform(arrayOf(curContext))

        if (encodedTrainContexts.size != trainIncentives.size) throwError(
            -1,
            "Context and incentive are not same shape: contexts = ${encodedTrainContexts.size} / incentives = ${trainIncentives.size}"
        )

        val trainX = Array(encodedTrainContexts.size) {
            doubleArrayOf(trainIncentives[it], *encodedTrainContexts[it])
        }

        val trainY = histories.map { if (it.isSucceeded) 1 else 0 }.toIntArray()

        val model = BinaryLogisticRegression.fit(
            x = trainX,
            y = trainY,
            C = 0.1,
            tol = 1e-4,
            maxIter = 500
        )

        val coefficients = model.coefficients
        val intercept = model.intercept

        val betaContext = coefficients.copyOfRange(1, coefficients.count())

        if (encodedCurContext.size != betaContext.size) throwError(
            -1,
            "Encoded context and coefficients are not same shape: context = $curContext " +
                    "/ encodedContext = ${encodedCurContext.joinToString(",")} " +
                    "/ beta = ${betaContext.joinToString(",")}"
        )

        val dotProduct = encodedCurContext.foldIndexed(0.0) { index, acc, d ->
            acc + betaContext[index] * d
        }

        var numerator = dotProduct + intercept
        var denominator = coefficients.first()

        val resultStr = "Context = $curContext (${encodedCurContext.joinToString(", ")}); " +
                "B_incentive. = $denominator; B_context = ${betaContext.joinToString(",")};" +
                "Numerator = $numerator (= $dotProduct + $intercept)"

        if (abs(denominator) < 1e-3) {
            denominator = 1e-3
        }

        if (abs(numerator) < 1e-3) {
            numerator = -1e-3
        }

        return if (numerator < 0 && denominator > 0) {
            val estimatedReward = - (numerator / denominator)
            val adjustedReward = (estimatedReward / RemotePrefs.unitIncentives).toInt() * RemotePrefs.unitIncentives
            AppLog.d(javaClass, "calculateStochasticIncentive(): Incentive calculated for $curContext ($latitude, $longitude);" +
                    "estimated = $estimatedReward, adjusted = $adjustedReward; $resultStr")
            adjustedReward.coerceIn(RemotePrefs.minIncentives, RemotePrefs.maxIncentives)
        } else {
            AppLog.d(javaClass, "calculateStochasticIncentive(): Incentive does not related for $curContext ($latitude, $longitude); $resultStr")
            RemotePrefs.minIncentives
        }
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
                        index -> data.map { it[index] }.sorted().distinct().toTypedArray()
                }, dropFirst)
        }
    }

    class BinaryLogisticRegression(w: DoubleArray) {
        private var learningRate = 0.1

        val intercept = w.last()
        val coefficients = w.copyOfRange(0, w.lastIndex)

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

                bfgs.minimize(BinaryObjectiveFunction(x, y, C), 5, w)

                val model = BinaryLogisticRegression(w)

                model.learningRate = 0.1 / x.size

                return model
            }
        }
    }


}