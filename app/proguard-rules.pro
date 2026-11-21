# R8 / ProGuard rules for Miner IA (com.minerrelay.app)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# OkHttp optional platform warnings
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Room entities and DAOs
-keepclassmembers class * extends androidx.room.RoomDatabase {
    void clearAllTables();
}
-keep class com.minerrelay.app.data.local.** { *; }

# Keep ForegroundService entry point
-keep class com.minerrelay.app.service.MiningRelayService { *; }
