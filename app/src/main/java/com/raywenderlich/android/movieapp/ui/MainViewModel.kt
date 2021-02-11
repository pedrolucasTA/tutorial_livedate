/*
 * Copyright (c) 2020 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.movieapp.ui

import androidx.lifecycle.*
import com.raywenderlich.android.movieapp.framework.network.MovieRepository
import com.raywenderlich.android.movieapp.framework.network.model.Movie
import com.raywenderlich.android.movieapp.ui.movies.MovieLoadingState
import kotlinx.coroutines.*
import javax.inject.Inject

class MainViewModel @Inject constructor(private val repository: MovieRepository) :
    ViewModel() {


  private val _popularMoviesLiveData = MutableLiveData<List<Movie>>()
  val moviesMediatorData = MediatorLiveData<List<Movie>>()
  private var debouncePeriod: Long = 500
  private var searchJob: Job? = null
  private var _searchMoviesLiveData: LiveData<List<Movie>>

  val movieLoadingStateLiveData = MutableLiveData<MovieLoadingState>()
  private val _searchFieldTextLiveData = MutableLiveData<String>()

  private val _navigateToDetails = MutableLiveData<Event<String>>()
  val navigateToDetails: LiveData<Event<String>>
    get() = _navigateToDetails


  init {
    _searchMoviesLiveData = Transformations.switchMap(_searchFieldTextLiveData) {
      fetchMovieByQuery(it)
    }

    moviesMediatorData.addSource(_popularMoviesLiveData) {
      moviesMediatorData.value = it
    }

    moviesMediatorData.addSource(_searchMoviesLiveData) {
      moviesMediatorData.value = it
    }
  }

  fun onFragmentReady() {
    if (_popularMoviesLiveData.value.isNullOrEmpty()) {
      fetchPopularMovies()
    }
  }


  fun onSearchQuery(query: String) {
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      delay(debouncePeriod)
      if (query.length > 2) {
        //4
        _searchFieldTextLiveData.value = query

      }
    }
  }

  private fun fetchPopularMovies() {
    //1
    movieLoadingStateLiveData.value = MovieLoadingState.LOADING
    viewModelScope.launch(Dispatchers.IO) {
      try {
        //2
        val movies = repository.fetchPopularMovies()
        _popularMoviesLiveData.postValue(movies)

        //3
        movieLoadingStateLiveData.postValue(MovieLoadingState.LOADED)
      } catch (e: Exception) {
        //4
        movieLoadingStateLiveData.postValue(MovieLoadingState.INVALID_API_KEY)
      }
    }
  }


  private fun fetchMovieByQuery(query: String): LiveData<List<Movie>> {
    val liveData = MutableLiveData<List<Movie>>()
    viewModelScope.launch(Dispatchers.IO) {
      try {
        //1
        withContext(Dispatchers.Main) {
          movieLoadingStateLiveData.value = MovieLoadingState.LOADING
        }

        val movies = repository.fetchMovieByQuery(query)
        liveData.postValue(movies)

        //2
        movieLoadingStateLiveData.postValue(MovieLoadingState.LOADED)
      } catch (e: Exception) {
        //3
        movieLoadingStateLiveData.postValue(MovieLoadingState.INVALID_API_KEY)
      }
    }
    return liveData
  }

  fun onMovieClicked(movie: Movie) {
    movie.title?.let {
      _navigateToDetails.value = Event(it)
    }
  }

  override fun onCleared() {
    super.onCleared()
    searchJob?.cancel()
  }
}