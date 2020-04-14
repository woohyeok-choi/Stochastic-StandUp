package kaist.iclab.standup.smi.tracker

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings


private const val HOST = "10.0.2.2:8080"

fun initializeFirestore() : FirebaseFirestore {
    val firestore = FirebaseFirestore.getInstance()

    firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
        .setHost(HOST)
        .setSslEnabled(false)
        .setPersistenceEnabled(false)
        .build()

    return firestore
}
