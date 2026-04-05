package com.imadev.foody.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.imadev.foody.ui.HomeActivity
import com.imadev.foody.utils.NavigationCommand

abstract class BaseFragment<B : ViewBinding, VM : BaseViewModel> : Fragment() {

    protected lateinit var binding: B
    protected abstract val viewModel: VM

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = createViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeNavigation()
    }

    private fun observeNavigation() {
        viewModel.navigation.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { command ->
                when (command) {
                    is NavigationCommand.ToDirection -> findNavController().navigate(command.directions)
                    is NavigationCommand.ToDirectionAction -> findNavController().navigate(
                        command.directions,
                        command.bundle
                    )
                    is NavigationCommand.Back -> findNavController().popBackStack()
                }
            }
        }
    }

    abstract fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): B

    abstract fun setToolbarTitle(activity: HomeActivity)

}
