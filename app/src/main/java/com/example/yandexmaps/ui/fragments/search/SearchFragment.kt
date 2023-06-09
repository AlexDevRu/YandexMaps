package com.example.yandexmaps.ui.fragments.search

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.widget.SearchView
import androidx.navigation.fragment.findNavController
import com.example.yandexmaps.R
import com.example.yandexmaps.databinding.FragmentSearchBinding
import com.example.yandexmaps.ui.fragments.adapters.SuggestionsAdapter
import com.example.yandexmaps.ui.fragments.base.BaseFragment
import com.example.yandexmaps.ui.fragments.main.MapsVM
import com.example.yandexmaps.ui.fragments.main.Result
import com.example.yandexmaps.ui.models.SearchResponseModel
import com.example.yandexmaps.utils.Utils
import com.example.yandexmaps.utils.hideKeyBoard
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.search.*
import com.yandex.runtime.Error
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class SearchFragment: BaseFragment<FragmentSearchBinding>(FragmentSearchBinding::inflate) {

    companion object {
        private const val TAG = "SearchFragment"
        private const val RESULT_NUMBER_LIMIT = 5
    }

    private val mapsViewModel by sharedViewModel<MapsVM>()
    private val viewModel by sharedViewModel<SearchVM>()

    private lateinit var suggestionsAdapter: SuggestionsAdapter

    private lateinit var suggestSession: SuggestSession
    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)


    private val SEARCH_OPTIONS = SuggestOptions().setSuggestTypes(
        SuggestType.BIZ.value
    )

    private val voiceSearchActivityResultLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val list = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = list?.firstOrNull()
            binding.suggestQuery.setQuery(text.orEmpty(), false)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.root.requestFocus()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        suggestSession = searchManager.createSuggestSession()

        suggestionsAdapter = SuggestionsAdapter {
            Log.w(TAG, "suggestion uri ${it.uri}")
            mapsViewModel.searchBySuggestion(it, {
                goBack()
            }, {

            }, {
                showSnackBar(it.message!!.toInt())
            })
        }

        binding.suggestResult.adapter = suggestionsAdapter

        binding.suggestQuery.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.query.value = newText
                requestSuggest(newText)
                return true
            }
        })

        binding.voiceSearchButton.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            voiceSearchActivityResultLauncher.launch(intent)
        }

        observe()
    }

    private fun observe() {
        viewModel.suggestionsList.observe(viewLifecycleOwner) {
            suggestionsAdapter.submitList(it)
        }
        viewModel.query.observe(viewLifecycleOwner) {
            Log.w(TAG, "search fragment query updated")
            binding.suggestQuery.setQuery(it, false)
        }
    }

    private fun requestSuggest(query: String) {
        val boundingBox = mapsViewModel.userLocationVM.boundingBox ?: return

        suggestSession.suggest(query, boundingBox, SEARCH_OPTIONS, object: SuggestSession.SuggestListener {
            override fun onResponse(suggests: MutableList<SuggestItem>) {
                Log.w(TAG,"suggests ${suggests}")

                /*suggests[0].properties.forEach {
                    Log.d(TAG, "key ${it.key}, value ${it.value}")
                }
                suggests[0].tags.forEach {
                    Log.d(TAG, "tag ${it}")
                }*/

                viewModel.suggestionsList.value = suggests
            }

            override fun onError(error: Error) {
                val errorMessage = Utils.getErrorMessage(error)
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun goBack() {
        binding.suggestQuery.hideKeyBoard()
        findNavController().navigateUp()
    }
}