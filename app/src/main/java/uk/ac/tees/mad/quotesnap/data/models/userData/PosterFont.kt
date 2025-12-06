//package uk.ac.tees.mad.quotesnap.data.models.userData
//
//import android.graphics.Typeface
//
//enum class PosterFont(val displayName: String, val typefaceStyle: Int) {
//    SERIF("Serif", Typeface.SERIF.style),
//    SANS_SERIF("Sans Serif", Typeface.SANS_SERIF.style),
//    MONOSPACE("Monospace", Typeface.MONOSPACE.style);
//
//    fun toTypeface(): Typeface {
//        return when (this) {
//            SERIF -> Typeface.SERIF
//            SANS_SERIF -> Typeface.SANS_SERIF
//            MONOSPACE -> Typeface.MONOSPACE
//        }
//    }
//
//    companion object {
//        fun fromString(value: String): PosterFont {
//            return values().firstOrNull {
//                it.name.equals(value, true) || it.displayName.equals(value, true)
//            } ?: SANS_SERIF
////            return try {
////                valueOf(value.uppercase())
////            } catch (e: Exception) {
////                SANS_SERIF // Default fallback
////            }
//        }
//    }
//}

package uk.ac.tees.mad.quotesnap.data.models.userData

import android.graphics.Typeface

enum class PosterFont(val displayName: String) {
    CLASSIC("Classic"),       // Serif + Normal
    MODERN("Modern"),         // Sans Serif + Bold
    TYPEWRITER("Typewriter"); // Monospace + Normal

    fun toTypeface(): Typeface {
        return when (this) {
            CLASSIC -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            MODERN -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            TYPEWRITER -> Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
    }

    companion object {
        fun fromString(value: String): PosterFont {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                MODERN // Default to Modern
            }
        }
    }
}