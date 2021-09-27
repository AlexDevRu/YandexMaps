package com.example.yandexmaps.ui.fragments.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.snackbar.Snackbar


typealias Inflate<T> = (LayoutInflater, ViewGroup?, Boolean) -> T

abstract class BaseFragment<TBinding: ViewBinding>(
    private val inflate: Inflate<TBinding>
): Fragment() {

    protected lateinit var binding: TBinding

    //protected lateinit var internetObserver: InternetUtil

    /*protected val globalVM: GlobalVM
        get() = (requireActivity() as MapsActivity).globalVM*/

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = inflate.invoke(inflater, container, false)
        //internetObserver = InternetUtil(requireContext())
        return binding.root
    }

    protected fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        //compositeDisposable.clear()
    }
}