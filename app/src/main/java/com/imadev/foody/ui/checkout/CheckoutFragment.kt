package com.imadev.foody.ui.checkout

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.firebase.auth.FirebaseAuth
import com.imadev.foody.R
import com.imadev.foody.databinding.FragmentCheckoutBinding
import com.imadev.foody.model.Address
import com.imadev.foody.model.Client
import com.imadev.foody.model.Order
import com.imadev.foody.model.PaymentMethod
import com.imadev.foody.model.OrderStatus
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.ui.common.BaseFragment
import com.imadev.foody.ui.map.MapsViewModel
import com.imadev.foody.utils.Constants.STRING_LENGTH
import com.imadev.foody.utils.Resource
import com.imadev.foody.utils.collectFlow
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

private const val TAG = "CheckoutFragment"

@AndroidEntryPoint
class CheckoutFragment : BaseFragment<FragmentCheckoutBinding, CheckoutViewModel>(),
    View.OnClickListener {

    override val viewModel: CheckoutViewModel by activityViewModels()
    private val mapsViewModel: MapsViewModel by activityViewModels()

    private var mClient = Client()
    private var mPaymentMethod: PaymentMethod? = null
    private var uid: String = ""
    private var newAddress = Address()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAuth.getInstance().uid?.let {
            uid = it
            viewModel.getClient(uid)
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCheckoutBinding = FragmentCheckoutBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.proceedPaymentBtn.setOnClickListener(this)
        binding.changeAdress.setOnClickListener(this)
        binding.changePhone.setOnClickListener(this)

        viewModel.client.collectFlow(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    if (result.data != null) mClient = result.data!!
                    with(binding) {
                        clientName.text = mClient.username
                        if (mClient.address?.address?.isNotEmpty() == true) {
                            address.text = mClient.address?.address
                            mClient.address?.let { newAddress = it }
                        }
                        if(!mClient.phone.isNullOrEmpty()) {
                            phone.setText(mClient.phone)
                        }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.totalPrice.collectFlow(viewLifecycleOwner) { price ->
            binding.total.text = requireContext().resources.getString(R.string.price, price)
        }

        viewModel.computeTotal()

        binding.cashOnDeliveryRadio.setOnCheckedChangeListener { _, b ->
            if (b) {
                binding.creditCardRadio.isChecked = !b
                mPaymentMethod = PaymentMethod.CASH
            }
        }

        binding.creditCardRadio.setOnCheckedChangeListener { _, b ->
            if (b) {
                binding.cashOnDeliveryRadio.isChecked = !b
                mPaymentMethod = PaymentMethod.CARD
            }
        }

        mapsViewModel.address.observe(viewLifecycleOwner) {
            binding.address.text = it?.address
            newAddress = it
        }
    }

    private fun proceedToPayment() {
        if (binding.address.text.isEmpty() || binding.address.text == getString(R.string.no_address_selected)) {
            Toast.makeText(requireContext(), getString(R.string.please_add_your_address), Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.phone.text?.isEmpty() == true) {
            binding.phone.error = getString(R.string.please_add_your_phone_number)
            return
        }
        if (mPaymentMethod == null) {
            Toast.makeText(requireContext(), resources.getString(R.string.please_select_a_payment_method), Toast.LENGTH_SHORT).show()
            return
        }

        if (newAddress.toString() != mClient.address.toString()) {
            newAddress.let { viewModel.updateAddress(uid, it) }
        }

        createOrder()
    }

    private fun createOrder() {
        // On s'assure que le client a bien l'adresse sélectionnée avec ses coordonnées
        mClient.address = newAddress

        val order = mPaymentMethod?.let { payment ->
            Order(
                orderNumber = getRandomString(),
                date = Date().time,
                meals = viewModel.cartList,
                client = mClient,
                paymentMethod = payment,
                status = OrderStatus.PENDING,
                uid = uid,
                deliveryLat = newAddress.latLng.latitude,
                deliveryLon = newAddress.latLng.longitude,
                to = null // No driver assigned yet
            )
        }

        order?.let {
            viewModel.sendOrder(it)
            Toast.makeText(requireContext(), "Order placed successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetList()
            viewModel.navigate(R.id.action_checkoutFragment_to_homeFragment)
        }
    }

    override fun setToolbarTitle(activity: HomeActivity) {
        activity.setToolbarTitle(R.string.checkout)
    }

    private fun getRandomString(): String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..STRING_LENGTH).map { charset.random() }.joinToString("")
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.proceedPaymentBtn.id -> proceedToPayment()
            binding.changeAdress.id -> viewModel.navigate(R.id.mapsFragment)
            binding.changePhone.id -> binding.phone.requestFocus()
        }
    }
}