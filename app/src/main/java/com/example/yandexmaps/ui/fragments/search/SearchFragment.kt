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
import com.example.yandexmaps.ui.fragments.main.MapsFragment
import com.example.yandexmaps.ui.fragments.main.MapsVM
import com.example.yandexmaps.ui.models.SearchResponseModel
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.search.*
import com.yandex.runtime.Error
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class SearchFragment: BaseFragment<FragmentSearchBinding>(FragmentSearchBinding::inflate) {

    companion object {
        private const val TAG = "SearchFragment"
        private const val RESULT_NUMBER_LIMIT = 5
    }

    private val viewModel by sharedViewModel<MapsVM>()

    private lateinit var suggestionsAdapter: SuggestionsAdapter

    private lateinit var suggestSession: SuggestSession
    private val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)


    private val SEARCH_OPTIONS = SuggestOptions().setSuggestTypes(
        SuggestType.GEO.value or
                SuggestType.BIZ.value or
                SuggestType.TRANSIT.value
    )


    private lateinit var searchSession: Session


    private val voiceSearchActivityResultLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val list = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = list?.firstOrNull()
            binding.suggestQuery.setQuery(text.orEmpty(), false)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        suggestSession = searchManager.createSuggestSession()

        suggestionsAdapter = SuggestionsAdapter {
            searchByQuery(it)
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
        suggestSession.suggest(query, viewModel.boundingBox, SEARCH_OPTIONS, object: SuggestSession.SuggestListener {
            override fun onResponse(suggests: MutableList<SuggestItem>) {
                Log.w(TAG,"suggests ${suggests}")

                suggests[0].properties.forEach {
                    Log.d(TAG, "key ${it.key}, value ${it.value}")
                }
                suggests[0].tags.forEach {
                    Log.d(TAG, "tag ${it}")
                }

                viewModel.suggestionsList.value = suggests.map { it.displayText }
            }

            override fun onError(error: Error) {
                val errorMessage = getString(when(error) {
                    is RemoteError -> R.string.remote_error_message
                    is NetworkError -> R.string.network_error_message
                    else -> R.string.unknown_error_message
                })

                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchByQuery(query: String) {
        searchSession = searchManager.submit(
            query,
            Geometry.fromBoundingBox(viewModel.boundingBox),
            SearchOptions(),
            object: Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    val obj = response.collection.children.first().obj
                    val metadata = obj?.metadataContainer?.getItem(BusinessObjectMetadata::class.java)
                    Log.e(TAG, "address ${metadata?.address?.formattedAddress}")
                    Log.e(TAG, "working hours ${metadata?.workingHours?.availabilities?.firstOrNull()?.days} ${metadata?.workingHours?.availabilities?.firstOrNull()?.timeRanges?.firstOrNull()?.from}")

                    viewModel.searchResponse.value = SearchResponseModel(response, 1)
                    //val action = SearchFragmentDirections.actionSearchFragmentToMapsFragment()
                    //findNavController().navigate(action)

                    findNavController().navigateUp()
                }

                override fun onSearchError(error: Error) {
                    showSnackBar(error.toString())
                }
            }
        )
    }
}