package com.ruzhan.movie.home.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ruzhan.lion.database.CommonModel
import com.ruzhan.lion.model.Movie
import com.ruzhan.lion.util.LionUtils
import com.ruzhan.movie.network.MovieRepository
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


class MovieListViewModel(app: Application) : AndroidViewModel(app) {

    companion object {

        private const val REFRESH = "REFRESH"
        private const val LOAD_MORE = "LOAD_MORE"

        private const val DEBOUNCE_TIME = 1000L
        private const val LION_MOVIE_LIST = 3000
        private const val START_PAGE = 1
    }

    val loadStatusLiveData = MutableLiveData<Boolean>()
    val refreshLiveData = MutableLiveData<List<Movie>>()
    val loadMoreLiveData = MutableLiveData<List<Movie>>()

    private var isLoading = false
    private var loadPage = START_PAGE
    private var disposable: Disposable? = null
    private var refreshDisposable: Disposable? = null
    private var localDisposable: Disposable? = null
    private var localFlowAble: Flowable<Any>? = null

    private val mainGSon = Gson()
    private val compositeDisposable = CompositeDisposable()

    init {
        localFlowAble = Flowable.create<Any>({ e ->
            handleInsertCommonModel()
            e.onComplete()
        }, BackpressureStrategy.LATEST)
    }

    private fun handleInsertCommonModel() {
        val movieList = refreshLiveData.value
        if (movieList != null) {
            val jsonStr = mainGSon.toJson(movieList)
            val commonModel = CommonModel(LION_MOVIE_LIST, jsonStr)
            MovieRepository.get().insertCommonModel(commonModel)
        }
    }

    fun getLocalMovieList() {
        disposable = MovieRepository.get().getCommonModel(LION_MOVIE_LIST)
                .map { commonModel -> commonModelToMovieList(commonModel) }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { throwable -> throwable.printStackTrace() }
                .doOnSubscribe { }
                .doFinally { }
                .doOnNext { movieList -> handleLocalMovieList(movieList) }
                .subscribe({ }, { })
        compositeDisposable.add(disposable!!)
    }

    private fun commonModelToMovieList(commonModel: CommonModel): ArrayList<Movie> {
        var movieList = ArrayList<Movie>()
        val content = commonModel.content
        if (content.isNotBlank()) {
            movieList = mainGSon.fromJson(content, object : TypeToken<List<Movie>>() {}.type)
        }
        return movieList
    }

    private fun handleLocalMovieList(movieList: List<Movie>?) {
        if (refreshLiveData.value == null) {
            refreshLiveData.value = movieList
        }
        disposable?.dispose()
    }

    fun getRefreshMovieList() {
        getMovieList(REFRESH)
    }

    fun getLoadMoreMovieList() {
        getMovieList(LOAD_MORE)
    }

    private fun getMovieList(status: String) {
        if (isLoading) {
            return
        }
        isLoading = true
        when (status) {
            REFRESH -> loadPage = START_PAGE
            LOAD_MORE -> loadPage = ++loadPage
            else -> {
                // do nothing
            }
        }
        val pageFileName = loadPage.toString().plus(LionUtils.JSON_FILE)
        refreshDisposable = MovieRepository.get().getMovieList(pageFileName)
                .map { result -> result.data }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError {}
                .doOnSubscribe {}
                .doFinally {
                    loadStatusLiveData.value = false
                    isLoading = false
                    refreshDisposable?.dispose()
                }
                .doOnSuccess { movieList -> handleMovieList(status, movieList) }
                .subscribe({ }, { })
        compositeDisposable.add(refreshDisposable!!)
    }

    private fun handleMovieList(status: String, movieList: List<Movie>?) {
        when (status) {
            REFRESH -> {
                refreshLiveData.value = movieList
                insertLocalMovieList()
            }
            LOAD_MORE -> {
                loadMoreLiveData.value = movieList
            }
            else -> {
                // do nothing
            }
        }
    }

    private fun insertLocalMovieList() {
        val localFlowAble = localFlowAble
        if (localFlowAble != null) {
            localDisposable = localFlowAble.debounce(DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError { throwable -> throwable.printStackTrace() }
                    .doOnComplete { localDisposable?.dispose() }
                    .subscribe({ }, { })
            compositeDisposable.add(localDisposable!!)
        }
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }
}