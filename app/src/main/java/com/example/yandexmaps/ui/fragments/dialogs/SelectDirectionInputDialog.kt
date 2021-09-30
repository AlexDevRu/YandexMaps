package com.example.yandexmaps.ui.fragments.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.example.yandexmaps.R
import com.example.yandexmaps.databinding.LayoutDirectionsOptionsBinding
import com.example.yandexmaps.ui.fragments.adapters.DirectionOptionsAdapter
import com.example.yandexmaps.ui.fragments.main.DIRECTION_ACTION
import com.example.yandexmaps.ui.fragments.main.MapsVM
import com.example.yandexmaps.ui.models.DirectionOptionUIModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class SelectDirectionInputDialog : DialogFragment() {

    private var binding: LayoutDirectionsOptionsBinding? = null

    private lateinit var adapter: DirectionOptionsAdapter

    private val mapsVM by sharedViewModel<MapsVM>()

    private val options = listOf(
        DirectionOptionUIModel(R.drawable.ic_baseline_gps_fixed_24, R.string.my_location, DIRECTION_ACTION.BIND_MY_LOCATION),
        DirectionOptionUIModel(R.drawable.ic_baseline_room_24, R.string.choose_on_map, DIRECTION_ACTION.CHOOSE_ON_MAP)
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = LayoutDirectionsOptionsBinding.inflate(LayoutInflater.from(context))

        adapter = DirectionOptionsAdapter {
            findNavController().navigateUp()
            mapsVM.applyDirectionAction(it.action)
        }

        binding?.root?.adapter = adapter
        adapter.submitList(options)

        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Choose").setView(binding!!.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}