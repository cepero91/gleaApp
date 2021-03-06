package com.example.glea.presentation.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.example.glea.R
import com.example.glea.data.datamanager.mappers.PokemonDetailMapper
import com.example.glea.data.datamanager.network.api.PokemonDetailsApiHelperImpl
import com.example.glea.data.datamanager.network.api.PokemonListApiHelperImpl
import com.example.glea.data.datamanager.persistence.PokemonDb
import com.example.glea.domain.models.Pokemon
import com.example.glea.presentation.adapter.PokemonListAdapter
import com.example.glea.presentation.adapter.PokemonLoadStateAdapter
import com.example.glea.presentation.intent.PokemonIntent
import com.example.glea.presentation.states.PokemonListState
import com.example.glea.presentation.view_model.PokemonListViewModel
import com.example.glea.presentation.view_model.PokemonListViewModelFactory
import com.example.glea.util.Types
import kotlinx.android.synthetic.main.activity_pokemon_list.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf


@RequiresApi(Build.VERSION_CODES.M)
@ExperimentalCoroutinesApi
class PokemonListActivity : AppCompatActivity(), PokemonListAdapter.OnPokemonSelectedListener {

    private val adapter: PokemonListAdapter = PokemonListAdapter()
    private lateinit var pokemonListViewModel: PokemonListViewModel

    private val pokemonDb: PokemonDb by inject { parametersOf(this, false, this, this, this) }
    private val pokemonListApiHelperImpl: PokemonListApiHelperImpl by inject { parametersOf(this) }
    private val pokemonDetailsApiHelperImpl: PokemonDetailsApiHelperImpl by inject {
        parametersOf(
            this
        )
    }
    private val pokemonMapper: PokemonDetailMapper by inject { parametersOf(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pokemon_list)

        initAdapter()
        initViewModel()
        observePokemonListState()
        observeLoadState()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        lifecycleScope.launch {
            pokemonListViewModel.pokemonIntent.send(PokemonIntent.FetchPokemonList)
        }
    }

    private fun initAdapter() {
        recycler_pokemon.adapter = adapter.withLoadStateFooter(
            footer = PokemonLoadStateAdapter { adapter.retry() }
        )
        adapter.pokemonSelectedListener = this
    }

    private fun initViewModel() {
        pokemonListViewModel = ViewModelProvider(
            this,
            PokemonListViewModelFactory(
                pokemonListApiHelperImpl,
                pokemonDetailsApiHelperImpl,
                pokemonDb,
                pokemonMapper
            )
        ).get(PokemonListViewModel::class.java)
    }

    private fun observePokemonListState() {
        lifecycleScope.launch {
            pokemonListViewModel.state.collectLatest { pokemonListState ->
                when (pokemonListState) {
                    is PokemonListState.Loading -> {
                        initial_loading_view.visibility = View.VISIBLE
                    }
                    is PokemonListState.PokemonList -> {
                        adapter.submitData(pokemonListState.pokeList)
                        initial_loading_view.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun observeLoadState(){
        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates ->
                if(loadStates.refresh is LoadState.Error){
                    initial_loading_view.visibility = View.GONE
                }
            }
        }

    }

    override fun onPokemonSelected(pokemon: Pokemon?) {
        if (supportFragmentManager.findFragmentByTag(PokemonDetailFragment.TAG) == null) {
            val pokemonDetailFragment = PokemonDetailFragment.newInstance(pokemon)
            pokemonDetailFragment.show(supportFragmentManager, PokemonDetailFragment.TAG)
        }
    }
}

