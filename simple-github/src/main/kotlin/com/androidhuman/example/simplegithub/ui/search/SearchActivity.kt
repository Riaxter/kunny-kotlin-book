package com.androidhuman.example.simplegithub.ui.search

import com.androidhuman.example.simplegithub.R
import com.androidhuman.example.simplegithub.api.GithubApi
import com.androidhuman.example.simplegithub.api.model.GithubRepo
import com.androidhuman.example.simplegithub.api.model.RepoSearchResponse
import com.androidhuman.example.simplegithub.ui.repo.RepositoryActivity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.androidhuman.example.simplegithub.api.provideGithubApi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_search.*

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : AppCompatActivity(), SearchAdapter.ItemClickListener {
    internal lateinit var menuSearch: MenuItem

    internal lateinit var searchView: SearchView

    internal val adapter by lazy {
        SearchAdapter().apply {
            setItemClickListener(this@SearchActivity)
        }
    }

    internal lateinit var api: GithubApi

    private val disposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        with(rvActivitySearchList){
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = this@SearchActivity.adapter
        }

        api = provideGithubApi(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_search, menu)
        menuSearch = menu.findItem(R.id.menu_activity_search_query)
        searchView = (menuSearch.actionView as SearchView).apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    updateTitle(query)
                    hideSoftKeyboard()
                    collapseSearchView()
                    searchRepository(query)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    return false
                }
            })
        }
        menuSearch.expandActionView()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (R.id.menu_activity_search_query == item.itemId) {
            item.expandActionView()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(repository: GithubRepo) {
        startActivity(
            Intent(this, RepositoryActivity::class.java).apply {
                putExtra(RepositoryActivity.KEY_USER_LOGIN, repository.owner.login)
                putExtra(RepositoryActivity.KEY_REPO_NAME, repository.name)
            }
        )
    }

    private fun searchRepository(query: String) {

        disposable.add(api.searchRepository(query)
            .flatMap {
                if (0 == it.totalCount)
                    Observable.error(IllegalStateException("No search result"))
                else
                    Observable.just(it.items)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                clearResults()
                hideError()
                showProgress()
            }
            .doOnTerminate {
                hideProgress()
            }
            .subscribe({ items ->
                with(adapter) {
                    setItems(items)
                    notifyDataSetChanged()
                }
            }){
                showError(it.message)
            }
        )

    }

    override fun onStop() {
        super.onStop()
        disposable.clear()
    }

    private fun updateTitle(query: String) {
        supportActionBar?.subtitle = query
    }

    private fun hideSoftKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).run {
            hideSoftInputFromWindow(searchView.windowToken, 0)
        }
    }

    private fun collapseSearchView() {
        menuSearch.collapseActionView()
    }

    private fun clearResults() {
        adapter.clearItems()
        adapter.notifyDataSetChanged()
    }

    private fun showProgress() {
        pbActivitySearch.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        pbActivitySearch.visibility = View.GONE
    }

    private fun showError(message: String?) {
        tvActivitySearchMessage.text = message ?: "Unexpected error."
        tvActivitySearchMessage.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvActivitySearchMessage.text = ""
        tvActivitySearchMessage.visibility = View.GONE
    }
}
