package com.proyekakhir.latihan.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.proyekakhir.latihan.model.Student

class DatabaseHelper(
    context: Context?,
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "db_student"
        const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "student"
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_PHONE = "phone"
        private const val KEY_LOCATION = "location"
        private const val KEY_AGE = "age"
        private const val KEY_GENDER = "gender"
        private const val KEY_IMAGE = "image"
        private const val KEY_ADDRESS = "address"

        const val CREATE_TABLE =
            "CREATE TABLE $TABLE_NAME ($KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$KEY_NAME TEXT,$KEY_ADDRESS TEXT,$KEY_PHONE TEXT,$KEY_LOCATION TEXT,$KEY_AGE INTEGER,$KEY_GENDER TEXT," +
                    "$KEY_IMAGE TEXT);"
        const val TABLE_EXIST = "DROP TABLE IF EXISTS \"$TABLE_NAME\""

    }

    override fun onCreate(p0: SQLiteDatabase?) {
        p0?.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        p0?.execSQL(TABLE_EXIST)
        onCreate(p0)
    }

    fun addStudents(student: Student): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_NAME, student.name)
        values.put(KEY_ADDRESS, student.address)
        values.put(KEY_AGE, student.age)
        values.put(KEY_GENDER, student.gender)
        values.put(KEY_IMAGE, student.image)
        values.put(KEY_LOCATION, student.location)
        values.put(KEY_PHONE, student.phone)
        val result =db.insert(TABLE_NAME, null, values)
        Log.e("TAG", "addStudents: $result")
        return result
    }

    fun getListStudent(): ArrayList<Student> {
        val listStudent: ArrayList<Student> = ArrayList()
        val query = "SELECT * FROM $TABLE_NAME"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val data = Student(
                    cursor.getInt(cursor.getColumnIndex(KEY_ID)),
                    cursor.getString(cursor.getColumnIndex(KEY_NAME)),
                    cursor.getInt(cursor.getColumnIndex(KEY_AGE)),
                    cursor.getString(cursor.getColumnIndex(KEY_ADDRESS)),
                    cursor.getString(cursor.getColumnIndex(KEY_GENDER)),
                    cursor.getString(cursor.getColumnIndex(KEY_PHONE)),
                    cursor.getString(cursor.getColumnIndex(KEY_LOCATION)),
                    cursor.getString(cursor.getColumnIndex(KEY_IMAGE)),
                )
                listStudent.add(data)
            } while (cursor.moveToNext())
        }
        return listStudent
    }

    fun updateStudent(siswa:Student){
        val db = this.writableDatabase
        val query = "UPDATE $TABLE_NAME SET $KEY_NAME = \'${siswa.name}\'," +
                "$KEY_IMAGE = \'${siswa.image}, $KEY_GENDER=\'${siswa.gender}\'," +
                "$KEY_AGE = \'${siswa.age}\', $KEY_LOCATION=\'${siswa.location}\'," +
                "$KEY_PHONE=\'${siswa.phone}\', $KEY_ADDRESS=\'${siswa.address}\'"
        db.execSQL(query)
        db.close()
    }

    fun deleteStudent(id:Int){
        val db = this.writableDatabase
        val query = "DELETE FROM $TABLE_NAME WHERE $KEY_ID = \"$id\""
        db.execSQL(query)
        db.close()
    }

}