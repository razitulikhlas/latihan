package com.proyekakhir.latihan

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.proyekakhir.latihan.database.DatabaseHelper
import com.proyekakhir.latihan.model.Student
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var edName: EditText
    private lateinit var edAddress: EditText
    private lateinit var edPhone: EditText
    private lateinit var btnLocation: Button
    private lateinit var btnUpload: Button
    private lateinit var btnSave: Button
    private lateinit var previewImage: ImageView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var settingClient: SettingsClient
    private lateinit var locationSettingRequest: LocationSettingsRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationsa = ""

    companion object {
        const val REQUEST_CHECK_SETTINGS = 0x1
        const val LOCATION_REQUEST_INTERVAL = 3000L
        const val LOCATION_REQUEST_FASTEST_INTERVAL = 6000L
        const val LOCATION_REQUEST_SMALLEST_DISPLACEMENT = 10f
    }

    private var shortAddress = ""
    private var fullAddress = ""
    private var addresses: List<Address>? = null
    private var ivStore: Uri? = null


    private var gender = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()

    }

    private var PERMISSIONS_LOCATION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private var PERMISSIONS_CAMERA = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )


    private fun initView() {
        databaseHelper = DatabaseHelper(this)
        settingClient = SettingsClient(this)
        locationSettingRequest = LocationSettingsRequest.Builder().apply {
            addLocationRequest(LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = LOCATION_REQUEST_INTERVAL
                fastestInterval = LOCATION_REQUEST_FASTEST_INTERVAL
                smallestDisplacement = LOCATION_REQUEST_SMALLEST_DISPLACEMENT
            })
            setAlwaysShow(true)
        }.build()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        edName = findViewById(R.id.edName)
        edAddress = findViewById(R.id.edAddress)
        edPhone = findViewById(R.id.edPhone)
        btnLocation = findViewById(R.id.btnLocation)
        btnUpload = findViewById(R.id.btnUpload)
        btnSave = findViewById(R.id.btnSave)
        previewImage = findViewById(R.id.imgPreview)
        Log.e("TAG", "initView: " + databaseHelper.getListStudent().toString())

        btnSave.setOnClickListener {
            save()
        }

        btnLocation.setOnClickListener {
            if (hasPermissions(PERMISSIONS_LOCATION)) {
                getDeviceLocation()
            } else {
                requestPermissionLocation.launch(
                    PERMISSIONS_LOCATION
                )
            }
        }

        btnUpload.setOnClickListener {
            if (hasPermissions(PERMISSIONS_CAMERA)) {
                showPopupDialog()
            } else {
                requestPermission.launch(
                    PERMISSIONS_CAMERA
                )
            }
        }
    }

    private val requestPermissionLocation =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            var permissionCount = 0
            permission.entries.forEach {
                if (it.value) {
                    permissionCount++
                }
            }

            Log.e("TAG", "total $permissionCount")

            if (permissionCount == 2 || permissionCount == 3) {
                Log.e("TAG", "Permission success")
                getDeviceLocation()
            } else {
                Log.e("TAG", "Permission failed")
                finish()
                Toast.makeText(this, "anda membutuhkan permission", Toast.LENGTH_SHORT).show()
            }
        }

    private fun hasPermissions(permissions: Array<String>): Boolean =
        permissions.all {
            ActivityCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun getDeviceLocation() {
        Log.e("TAG", "getDeviceLocation: ")
        settingClient.checkLocationSettings(locationSettingRequest).apply {
            addOnSuccessListener {
                val locationRequest = LocationRequest.create()
                locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                locationRequest.interval = 20 * 1000

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult?) {
                        super.onLocationResult(locationResult)
                        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                        if (locationResult == null) return
                        for (location in locationResult.locations) {
                            if (location != null) {
                                setAddress(location.latitude, location.longitude)
                                locationsa =
                                    "latitude ${location.latitude} longititude ${location.longitude}"
                                edAddress.setText(shortAddress)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    fusedLocationProviderClient.removeLocationUpdates(
                                        locationCallback
                                    )
                                }
                            }
                        }
                    }
                }
                try {
                    fusedLocationProviderClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.myLooper()
                    )
                } catch (e: SecurityException) {
                    Log.e("TAG", "getDeviceLocation: ${e.message}")

                }
            }
            addOnFailureListener {
                when ((it as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            val re = it as ResolvableApiException
                            re.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)
                        } catch (sie: IntentSender.SendIntentException) {
                            Log.e("TAG", "Error")
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val alertDialog = AlertDialog.Builder(this@MainActivity)
                        alertDialog.setTitle("Rami mart")
                        alertDialog.setMessage("aplikasi membutuhkan gps mode high accuracy untuk menemukan lokasi anda")
                        alertDialog.setIcon(R.mipmap.ic_launcher)
                        alertDialog.setPositiveButton(
                            "YA"
                        ) { dialog, _ ->
                            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            dialog.dismiss()
                        }
                        alertDialog.setNegativeButton("BATAL") { dialog, _ ->
                            dialog.dismiss()
                        }
                        alertDialog.show()
                    }
                }
            }
        }
    }

    var items_image = arrayOf("Pilih gambar dari Gallery", "Open camera")

    private fun showPopupDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("pilih image")
            .setItems(
                items_image
            ) { _: DialogInterface?, which: Int ->
                if (which == 0) {
                    imageFromGallery()
                } else if (which == 1) {
                    imageFromCamera()
                }
            }.show()
    }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            var permissionCount = 0
            permission.entries.forEach {
                if (it.value) {
                    permissionCount++
                }
            }

            if (permissionCount == 3) {
                showPopupDialog()
            } else {
//                Log.e(TAG, "Permission failed")
                finish()
//                showToastLong(requireContext(), "anda membutuhkan permission")
            }
        }

    private fun imageFromCamera() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "imageStore")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        ivStore = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                Log.e("TAG", "openCameraDevice: 2")
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, ivStore)
                resultMediaStore.launch(takePictureIntent)
            }
        }
    }

    private var resultMediaStore =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
//                try {
//                    FileUtilImage.from(requireActivity(), ivStore!!).also {
//                        Log.e(
//                            TAG,
//                            "prepareFilePart: camera size  ${getReadableFileSize(it.length())}"
//                        )
//                    }
//                } catch (e: IOException) {
//                    Log.e(TAG, "Failed to read picture data!")
//                    e.printStackTrace()
//                }
                previewImage.setImageURI(ivStore)
            }
        }

    private fun imageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultGallery.launch(intent)
    }

    private var resultGallery =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                ivStore = result.data?.data
                previewImage.setImageURI(ivStore)
//
//
//                Glide.with(this)
//                    .load(ivStore)
//                    .into(binding.backgroundStore)
            }
        }

    private fun save() {
        val name = edName.text.toString()
        val address = edAddress.text.toString()
        val phone = edPhone.text.toString()
        val student = Student(0, name, 20, address, gender, phone, locationsa, "image")
        databaseHelper.addStudents(student)
    }


    fun onRadioButtonClicked(view: View) {
        if (view is RadioButton) {
            val checked = view.isChecked
            when (view.id) {
                R.id.rbMan -> {
                    if (checked) {
                        gender = "Pria"
                    }
                }
                R.id.rbWoman -> {
                    if (checked) {
                        gender = "Wanita"
                    }
                }
            }
        }
    }

    private fun setAddress(
        latitude: Double,
        longitude: Double
    ) {
        val geoCoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geoCoder.getFromLocation(latitude, longitude, 1)
            this.addresses = addresses
            return if (addresses != null && addresses.size != 0) {
                fullAddress = addresses[0].getAddressLine(
                    0
                ) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

                shortAddress = generateFinalAddress(fullAddress).trim()
            } else {
                shortAddress = ""
                fullAddress = ""
            }
        } catch (e: Exception) {
            //Time Out in getting address
            shortAddress = ""
            fullAddress = ""
            addresses = null
        }
    }

    private fun generateFinalAddress(
        address: String
    ): String {
        val s = address.split(",")
        return if (s.size >= 3) s[1] + "," + s[2] else if (s.size == 2) s[1] else s[0]
    }
}