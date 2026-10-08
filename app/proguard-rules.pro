# 1. Lindungi Model Entitas Room Database
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep class com.industri.fleettrack.data.local.entity.** { *; }

# 2. Lindungi Model DTO Retrofit & Gson Serialisasi
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.industri.fleettrack.data.remote.api.** { *; }
-keepclassmembers class com.industri.fleettrack.data.remote.api.** { *; }

# 3. Lindungi Timber Logger di Mode Rilis
-dontwarn timber.log.**
-keep class timber.log.** { *; }