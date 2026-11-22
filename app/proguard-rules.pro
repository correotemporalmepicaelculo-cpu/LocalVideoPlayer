# ProGuard rules for LocalVideoPlayer

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends dagger.hilt.android.HiltApplication

# Keep Room entities and DAOs
-keep class com.lucas.videoplayer.data.local.entity.** { *; }
-keep class com.lucas.videoplayer.data.local.dao.** { *; }
-keep class com.lucas.videoplayer.data.local.AppDatabase { *; }

# Keep ExoPlayer classes
-keep class com.google.android.exoplayer2.** { *; }
-keep interface com.google.android.exoplayer2.** { *; }

# Keep Glide
-keep public class com.bumptech.glide.Glide
-keep public class com.bumptech.glide.GlideBuilder
-keep public class com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * implements com.bumptech.glide.module.LibraryGlideModule
-keep public class * extends com.bumptech.glide.manager.RequestManagerRetriever
-keep public class * extends com.bumptech.glide.manager.RequestManagerRetriever$RequestManagerFactory

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable { *; }

# Keep Serializable implementations
-keep class * implements java.io.Serializable { *; }

# Keep ViewBinding generated classes
-keep class com.lucas.videoplayer.databinding.** { *; }

# Keep Hilt ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModelProvider.Factory { *; }

# Keep MediaSession
-keep class androidx.media3.** { *; }

# Keep Material Components
-keep class com.google.android.material.** { *; }