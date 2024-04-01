// WeatherContract.kt
import android.provider.BaseColumns

object WeatherContract {
    // Table contents
    class WeatherEntry : BaseColumns {
        companion object {
            const val TABLE_NAME = "weather"
            const val COLUMN_CITY = "city"
            const val COLUMN_DATE = "date"
            const val COLUMN_MAX_TEMP = "max_temp"
            const val COLUMN_MIN_TEMP = "min_temp"
            const val _ID = BaseColumns._ID
        }
    }
}
