package kaist.iclab.standup.smi.repository

import android.location.Location
import android.view.animation.OvershootInterpolator
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.model.LatLng
import kaist.iclab.standup.smi.common.DataField
import kaist.iclab.standup.smi.common.asSuspend
import kaist.iclab.standup.smi.common.throwError
import kaist.iclab.standup.smi.common.toGeoHash
import kaist.iclab.standup.smi.data.OverallStat
import kaist.iclab.standup.smi.data.OverallStats
import kaist.iclab.standup.smi.data.PlaceStat
import kaist.iclab.standup.smi.data.PlaceStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

class StatRepository(
    private val geoApiContext: GeoApiContext? = null,
    private val placesClient: PlacesClient? = null,
    private val rootReference: () -> DocumentReference?,
    private val docReference: () -> CollectionReference?
) {
    suspend fun getPlaceStat(
        latitude: Double,
        longitude: Double
    ): PlaceStat? = (latitude to longitude).toGeoHash()?.let { getPlaceStat(it) }
    
    suspend fun getPlaceStat(
        id: String
    ) = docReference.invoke()?.let { reference -> PlaceStat.get(reference, id) }

    suspend fun getOverallStat(): OverallStat? = rootReference.invoke()?.let { reference ->
        OverallStat.getSelf(reference)
    }

    suspend fun updatePlaceName(latitude: Double, longitude: Double, name: String) = docReference.invoke()?.let { reference ->
        val geoHash = (latitude to longitude).toGeoHash() ?: return@let
        PlaceStat.update(ref = reference, id = geoHash) {
            this.name = name
        }
    }

    suspend fun updateVisitEvent(latitude: Double, longitude: Double, timestamp: Long) {
        val exists = createOrUpdatePlace(
            latitude,
            longitude,
            PlaceStats.numVisit to FieldValue.increment(1L),
            PlaceStats.lastVisitTime to timestamp
        )

        rootReference.invoke()?.let { reference ->
            OverallStat.updateSelf(
                reference,
                OverallStats.numVisit to FieldValue.increment(1L),
                OverallStats.numPlaces to FieldValue.increment(if (exists) 0L else 1L)
            )
        }
    }

    suspend fun updateMissionResult(
        latitude: Double,
        longitude: Double,
        isSucceeded: Boolean,
        incentives: Int
    ) {
        val exists = createOrUpdatePlace(
            latitude,
            longitude,
            PlaceStats.numMission to FieldValue.increment(1L),
            PlaceStats.numSuccess to FieldValue.increment(if (isSucceeded) 1L else 0L),
            PlaceStats.incentive to FieldValue.increment(
                if (isSucceeded && incentives >= 0 || !isSucceeded && incentives < 0) incentives.toLong() else 0L
            )
        )

        rootReference.invoke()?.let { reference ->
            val lastMissionDay = OverallStat.getSelf(reference)?.lastMissionDay ?: 0
            val curMissionDayStart = DateTime(System.currentTimeMillis(), DateTimeZone.getDefault()).withTimeAtStartOfDay().millis

            OverallStat.updateSelf(
                reference,
                OverallStats.numMission to FieldValue.increment(1L),
                OverallStats.numSuccess to FieldValue.increment(if (isSucceeded) 1L else 0L),
                OverallStats.incentive to FieldValue.increment(
                    if (isSucceeded && incentives >= 0 || !isSucceeded && incentives < 0) incentives.toLong() else 0L
                ),
                OverallStats.numPlaces to FieldValue.increment(if (exists) 0L else 1L),
                OverallStats.lastMissionDay to curMissionDayStart,
                OverallStats.numDaysMissions to FieldValue.increment(
                    if (lastMissionDay < curMissionDayStart) 1L else 0L
                )
            )
        }
    }

    private suspend fun createOrUpdatePlace(
        latitude: Double,
        longitude: Double,
        vararg params: Pair<DataField<*>, Any>
    ) = docReference.invoke()?.let { reference ->
        val geoHash = (latitude to longitude).toGeoHash() ?: return@let null
        val placeStat = PlaceStat.get(reference, geoHash)
        val numVisitField = if (placeStat == null) {
            arrayOf(
                PlaceStats.numVisit to FieldValue.increment(1)
            )
        } else {
            arrayOf()
        }

        val nameAndAddressField: Array<Pair<DataField<*>, Any>> = if (placeStat?.name.isNullOrBlank() || placeStat?.address.isNullOrBlank()) {
            val (name, address) = getPlaceNameAndAddress(latitude, longitude)
            arrayOf(
                PlaceStats.name to name,
                PlaceStats.address to address
            )
        } else {
            arrayOf()
        }

        PlaceStat.update(reference, geoHash, *params, *numVisitField, *nameAndAddressField)
        return@let placeStat != null
    } ?: true

    private suspend fun getPlaceNameAndAddress(
        latitude: Double,
        longitude: Double
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        if (geoApiContext == null || placesClient == null) return@withContext "" to ""

        try {
            val results = GeocodingApi.reverseGeocode(geoApiContext, LatLng(latitude, longitude)).await()
            val nearestPlaceId = results.minBy { result ->
                distance(
                    lat1 = latitude,
                    lon1 = longitude,
                    lat2 = result.geometry.location.lat,
                    lon2 = result.geometry.location.lng
                )
            }?.placeId ?: throwError(-1)

            val placeRequest = FetchPlaceRequest.newInstance(
                nearestPlaceId,
                listOf(Place.Field.NAME, Place.Field.ADDRESS)
            )
            val place = placesClient.fetchPlace(placeRequest).asSuspend()?.place ?: throwError(-1)
            (place.name ?: "") to (place.address ?: "")
        } catch (e: Exception) {
            "" to ""
        }
    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val result = floatArrayOf(0F)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result.first()
    }
}