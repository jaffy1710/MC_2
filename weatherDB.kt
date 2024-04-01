// WeatherDatabaseHelper.kt
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class WeatherDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "Weather.db"

        private  var SQL_CREATE_ENTRIES =
                "CREATE TABLE ${WeatherContract.WeatherEntry.TABLE_NAME} (" +
                        "${WeatherContract.WeatherEntry._ID} INTEGER PRIMARY KEY," +
                    "${WeatherContract.WeatherEntry.COLUMN_CITY} TEXT," +
                    "${WeatherContract.WeatherEntry.COLUMN_DATE} TEXT," +
                    "${WeatherContract.WeatherEntry.COLUMN_MAX_TEMP} REAL," +
                    "${WeatherContract.WeatherEntry.COLUMN_MIN_TEMP} REAL)"

        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${WeatherContract.WeatherEntry.TABLE_NAME}"
    }
}
