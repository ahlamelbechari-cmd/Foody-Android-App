package com.imadev.foody.ui.user

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.imadev.foody.R
import com.imadev.foody.databinding.FragmentOrderTrackingBinding
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.utils.collectFlow
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderTrackingFragment : BaseFragment<FragmentOrderTrackingBinding, OrderTrackingViewModel>(), OnMapReadyCallback {

    override val viewModel: OrderTrackingViewModel by viewModels()
    private var mMap: GoogleMap? = null
    
    private var driverMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var polyline: Polyline? = null
    private var isFirstZoom = true

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentOrderTrackingBinding = FragmentOrderTrackingBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val orderId = arguments?.getString("orderId") ?: return
        viewModel.trackOrder(orderId)

        observeTrackingData()
    }

    private fun observeTrackingData() {
        viewModel.driverLocation.collectFlow(viewLifecycleOwner) { driverLatLng ->
            val order = viewModel.currentOrder.value
            if (driverLatLng != null && order != null) {
                updateUI(driverLatLng, order)
            }
        }
    }

    private fun updateUI(driverPos: LatLng, order: com.imadev.foody.model.Order) {
        mMap?.let { map ->
            // Update Driver Marker
            if (driverMarker == null) {
                val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_delivery_car, null)
                val bitmap = (drawable as BitmapDrawable).bitmap
                val smallMarker = Bitmap.createScaledBitmap(bitmap, 120, 120, false)
                
                driverMarker = map.addMarker(MarkerOptions()
                    .position(driverPos)
                    .title("Votre livreur")
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)))
            } else {
                driverMarker?.position = driverPos
            }

            // Update Destination Marker
            val destLat = if (order.deliveryLat != 0.0) order.deliveryLat else order.client.address?.latLng?.latitude ?: 0.0
            val destLon = if (order.deliveryLon != 0.0) order.deliveryLon else order.client.address?.latLng?.longitude ?: 0.0
            val destPos = LatLng(destLat, destLon)

            if (destLat != 0.0) {
                if (destinationMarker == null) {
                    destinationMarker = map.addMarker(MarkerOptions()
                        .position(destPos)
                        .title("Moi")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
                }
                
                // Update Polyline
                polyline?.remove()
                polyline = map.addPolyline(PolylineOptions()
                    .add(driverPos, destPos)
                    .width(10f)
                    .color(Color.parseColor("#FF5A5F"))
                    .geodesic(true))

                // Zoom to fit both
                if (isFirstZoom) {
                    val builder = LatLngBounds.Builder()
                    builder.include(driverPos)
                    builder.include(destPos)
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 200))
                    isFirstZoom = false
                }
            } else if (isFirstZoom) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(driverPos, 15f))
                isFirstZoom = false
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.uiSettings?.isZoomControlsEnabled = true
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitleString("Suivi du livreur")
    }
}