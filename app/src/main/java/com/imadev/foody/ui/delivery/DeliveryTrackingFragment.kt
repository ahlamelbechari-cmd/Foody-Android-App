package com.imadev.foody.ui.delivery

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.imadev.foody.R
import com.imadev.foody.databinding.FragmentDeliveryTrackingBinding
import com.imadev.foody.model.Order
import com.imadev.foody.model.OrderStatus
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.Resource
import com.imadev.foody.utils.collectFlow
import com.imadev.foody.utils.hide
import com.imadev.foody.utils.show
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeliveryTrackingFragment : BaseFragment<FragmentDeliveryTrackingBinding, DeliveryViewModel>(), OnMapReadyCallback {

    override val viewModel: DeliveryViewModel by activityViewModels()
    
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    private var driverMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var polyline: Polyline? = null
    private var isFirstZoom = true

    override fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentDeliveryTrackingBinding = 
        FragmentDeliveryTrackingBinding.inflate(inflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    viewModel.updateLocation(location.latitude, location.longitude)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        
        binding.btnCenterOnMe.setOnClickListener {
            val driver = (viewModel.currentDriver.value as? Resource.Success)?.data
            driver?.let {
                val pos = LatLng(it.latitude, it.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
            }
        }

        observeDeliveryState()
    }

    private fun observeDeliveryState() {
        viewModel.myOrders.collectFlow(viewLifecycleOwner) { orders ->
            val activeOrder = orders.firstOrNull { it.status == OrderStatus.IN_DELIVERY }
            if (activeOrder != null) {
                binding.trackingCard.show()
                setupTracking(activeOrder)
            } else {
                binding.trackingCard.hide()
                map.clear()
                driverMarker = null
                destinationMarker = null
                polyline = null
                isFirstZoom = true
            }
        }
    }

    private fun setupTracking(order: Order) {
        binding.tvCustomerAddress.text = order.client.address?.address ?: "Destination"
        
        // Coordonnées avec fallback (ordre récent vs ordre ancien)
        val lat = if (order.deliveryLat != 0.0) order.deliveryLat else (order.client.address?.latLng?.latitude ?: 0.0)
        val lon = if (order.deliveryLon != 0.0) order.deliveryLon else (order.client.address?.latLng?.longitude ?: 0.0)
        val destLatLng = LatLng(lat, lon)
        
        Log.d("Tracking", "Destination detectée : $lat, $lon")

        if (lat != 0.0) {
            if (destinationMarker == null) {
                destinationMarker = map.addMarker(MarkerOptions()
                    .position(destLatLng)
                    .title("Client")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
            } else {
                destinationMarker?.position = destLatLng
            }
        }

        viewModel.currentDriver.collectFlow(viewLifecycleOwner) { resource ->
            resource.data?.let { driver ->
                val driverPos = LatLng(driver.latitude, driver.longitude)
                updateDriverMarker(driverPos)
                
                if (lat != 0.0) {
                    updatePolyline(driverPos, destLatLng)
                    if (isFirstZoom) {
                        zoomToFit(driverPos, destLatLng)
                        isFirstZoom = false
                    }
                } else if (isFirstZoom) {
                    // Si pas de destination valide, on zoom au moins sur le livreur
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(driverPos, 15f))
                    isFirstZoom = false
                }
            }
        }

        binding.btnNavigate.setOnClickListener {
            if (lat != 0.0) {
                val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lon")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            } else {
                Toast.makeText(context, "Coordonnées indisponibles pour ce client", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnCompleteOrder.setOnClickListener {
             viewModel.completeDelivery(order)
             Toast.makeText(requireContext(), "Livraison terminée !", Toast.LENGTH_SHORT).show()
             // Navigation forcée vers le Dashboard Livreur
             findNavController().navigate(R.id.deliveryDashboardFragment)
        }
    }

    private fun updatePolyline(start: LatLng, end: LatLng) {
        polyline?.remove()
        polyline = map.addPolyline(PolylineOptions()
            .add(start, end)
            .width(10f)
            .color(Color.parseColor("#FF5A5F")) // Orange Foody
            .geodesic(true))
    }

    private fun zoomToFit(driverPos: LatLng, clientPos: LatLng) {
        try {
            val builder = LatLngBounds.Builder()
            builder.include(driverPos)
            builder.include(clientPos)
            val bounds = builder.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))
        } catch (e: Exception) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(driverPos, 15f))
        }
    }

    private fun updateDriverMarker(pos: LatLng) {
        if (driverMarker == null) {
            val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_delivery_car, null)
            val bitmap = (drawable as BitmapDrawable).bitmap
            val smallMarker = Bitmap.createScaledBitmap(bitmap, 120, 120, false)

            driverMarker = map.addMarker(MarkerOptions()
                .position(pos)
                .title("Moi")
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)))
        } else {
            driverMarker?.position = pos
        }
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitleString("Suivi de livraison")
    }
}