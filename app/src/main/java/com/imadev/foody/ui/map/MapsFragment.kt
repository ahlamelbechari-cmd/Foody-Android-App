package com.imadev.foody.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MarkerOptions
import com.imadev.foody.R
import com.imadev.foody.databinding.FragmentMapsBinding
import com.imadev.foody.model.Address
import com.imadev.foody.model.LatLng
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.Constants.PERMISSIONS_REQUEST_LOCATION
import com.imadev.foody.utils.hide
import com.imadev.foody.utils.show
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

private const val TAG = "MapsFragment"

@AndroidEntryPoint
class MapsFragment : BaseFragment<FragmentMapsBinding, MapsViewModel>(), OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener,
    ActivityCompat.OnRequestPermissionsResultCallback {


    private var locationPermissionGranted: Boolean = false
    private lateinit var map: GoogleMap

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override val viewModel: MapsViewModel by activityViewModels()

    private var defaultLocation = com.google.android.gms.maps.model.LatLng(33.966304, -6.8549541)


    companion object {
        private const val DEFAULT_ZOOM = 15

        @JvmStatic
        fun isPermissionGranted(
            grantPermissions: Array<String>, grantResults: IntArray,
            permission: String
        ): Boolean {
            for (i in grantPermissions.indices) {
                if (permission == grantPermissions[i]) {
                    return grantResults[i] == PackageManager.PERMISSION_GRANTED
                }
            }
            return false
        }
    }


    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMapsBinding = FragmentMapsBinding.inflate(layoutInflater, container, false)


    override fun onResume() {
        super.onResume()
        setToolbarTitle(activity as HomeActivity)
        (activity as HomeActivity).getToolbar().hide()
    }

    override fun onPause() {
        super.onPause()
        (activity as HomeActivity).getToolbar().show()
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitle(R.string.map)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        getLocationPermission()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
        
        binding.chooseLocation.show()
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_LOCATION)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != PERMISSIONS_REQUEST_LOCATION) return
        if (isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            enableMyLocation()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMyLocationClickListener(this)
        
        if (locationPermissionGranted) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM.toFloat()))
                    setupMap(currentLatLng)
                } else {
                    setupMap(defaultLocation)
                }
            }
        } else {
            setupMap(defaultLocation)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            locationPermissionGranted = true
            onMapReady(map)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupMap(initialLocation: com.google.android.gms.maps.model.LatLng) {
        map.clear()
        val marker = map.addMarker(MarkerOptions().position(initialLocation).title("Your Location"))
        
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 12F))
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.uiSettings.isZoomControlsEnabled = true

        map.setOnCameraMoveListener {
            marker?.position = map.cameraPosition.target
        }

        binding.chooseLocation.setOnClickListener {
            marker?.let {
                val geocoder = Geocoder(requireContext(), Locale.US)
                
                try {
                    val addresses = geocoder.getFromLocation(it.position.latitude, it.position.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val myAddress = Address(
                            city = addr.locality ?: addr.subAdminArea ?: "",
                            state = addr.adminArea ?: "",
                            country = addr.countryName ?: "",
                            address = addr.getAddressLine(0) ?: "",
                            latLng = LatLng(it.position.latitude, it.position.longitude)
                        )
                        viewModel.address.value = myAddress
                        findNavController().popBackStack()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Geocoder error: ${e.message}")
                    Toast.makeText(requireContext(), "Location service error, try again", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    override fun onMyLocationClick(p0: Location) {}
}
