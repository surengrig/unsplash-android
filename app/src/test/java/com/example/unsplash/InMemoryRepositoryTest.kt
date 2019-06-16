package com.example.unsplash

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import com.example.unsplash.api.UnsplashApiService
import com.example.unsplash.libs.Response
import com.example.unsplash.model.NetworkState
import com.example.unsplash.model.UnsplashPost
import com.example.unsplash.repository.Listing
import com.example.unsplash.repository.byPage.InMemoryByPageKeyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.json.JSONArray
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

class ResponseFactory(val totalPages: Int) {
    companion object {
        fun listOfResponses(totalPages: Int, invoke: ResponseFactory.() -> Unit): List<Response> {
            val factory = ResponseFactory(totalPages)
            factory.invoke()
            return factory.responses
        }
    }

    private var index = 1
    private fun fakePostJson(id: Int) =
        "[{\"id\":\"$id\",\"created_at\":\"2019-06-14T11:19:39-04:00\",\"updated_at\":\"2019-06-15T13:34:49-04:00\",\"width\":2082,\"height\":3879,\"color\":\"#8CCEE8\",\"description\":null,\"urls\":{\"raw\":\"raw_url\",\"full\":\"full_url\",\"regular\":\"regular\",\"small\":\"small\",\"thumb\":\"thumb\"}}]"

    private val _responses = arrayListOf<Response>()
    val responses: List<Response>
        get() = _responses


    private fun addResponse(
        rawData: String,
        status: Int,
        headers: Map<String, List<String>>
    ) {
        if (status == 200) {
            index++
        }

        _responses.add(
            Response(rawData = rawData, status = status, headers = headers)
        )
    }

    fun addSuccessfulResponse() {
        addResponse(
            rawData = fakePostJson(index),
            status = 200,
            headers = mapOf("X-Total" to listOf(totalPages.toString()))
        )
    }

    fun addErrorResponse() {
        addResponse(rawData = "", status = 401, headers = mapOf())
    }
}

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class InMemoryRepositoryTest {
    @Suppress("unused")
    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()

    lateinit var repositorySubject: InMemoryByPageKeyRepository

    @Mock
    lateinit var mockUnsplashApi: UnsplashApiService


    private fun <T> PagedList<T>.loadAllData() {
        if (size == 0) return
        do {
            val oldSize = this.loadedCount
            this.loadAround(this.size - 1)
        } while (this.size != oldSize)
    }

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val scope = TestCoroutineScope()
        repositorySubject = InMemoryByPageKeyRepository(
            mockUnsplashApi, scope
        )
    }

    /**
     * asserts that empty list works fine
     */
    @Test
    fun emptyList() {
        val listing = repositorySubject.getPosts(1)
        val pagedList = getPagedList(listing)
        assertThat(pagedList.size, `is`(0))
    }

    /**
     * asserts loading a full list in multiple pages
     */
    @Test
    fun verifyCompleteList() {
        val responses = ResponseFactory.listOfResponses(2) {
            addSuccessfulResponse()
            addSuccessfulResponse()
        }
        `GIVEN responses are`(responses)
        `WHEN loads posts`()
            .`WHEN loads all posts`()
            .`THEN loaded posts are from`(responses)
    }

    /**
     * asserts the failure message when the initial load fails
     */
    @Test
    fun failToLoadInitial() {
        `GIVEN responses are`(ResponseFactory.listOfResponses(1) { addErrorResponse() })
        `WHEN loads posts`()
            .`THEN has network error`()
    }

    /**
     * asserts the failure message when the load fails after successful initial load
     */
    @Test
    fun failToLoadAfterInitial() {
        `GIVEN responses are`(ResponseFactory.listOfResponses(2) {
            addSuccessfulResponse()
            addErrorResponse()
        })
        `WHEN loads posts`()
            .`THEN networks state is`(NetworkState.LOADED)
            .`WHEN loads all posts`()
            .`THEN has network error`()
    }

    /**
     * asserts the retry logic when initial load request fails
     */
    @Test
    fun retryWhenInitialFails() {
        val responsesWithError = ResponseFactory.listOfResponses(3) {
            addErrorResponse()
            addSuccessfulResponse()
            addSuccessfulResponse()
        }
        val responses = ResponseFactory.listOfResponses(3) {
            addSuccessfulResponse()
            addSuccessfulResponse()
            addSuccessfulResponse()
        }

        `GIVEN responses are`(responsesWithError)
        `WHEN loads posts`()
            .`THEN has network error`()
            .`GIVEN responses are`(responsesWithError)
            .`WHEN loads all posts`()
            .`THEN has network error`()
            .`GIVEN responses are`(responses)
            .`WHEN retry`()
            .`THEN loaded posts are from`(responses)
    }

    /**
     * asserts the retry logic when load fails after initial load but subsequent loads succeed
     */
    @Test
    fun retryAfterInitialFails() {
        val responsesWithError = ResponseFactory.listOfResponses(3) {
            addSuccessfulResponse()
            addErrorResponse()
            addSuccessfulResponse()
        }
        val responses = ResponseFactory.listOfResponses(3) {
            addSuccessfulResponse()
            addSuccessfulResponse()
            addSuccessfulResponse()
        }

        `GIVEN responses are`(responsesWithError)
        `WHEN loads posts`()
            .`THEN loaded posts are from`(listOf(responses[0]))
            .`THEN networks state is`(NetworkState.LOADED)

            .`GIVEN responses are`(responsesWithError).`WHEN loads all posts`()
            .`THEN has network error`()
            .`GIVEN responses are`(responses)
            .`WHEN retry`()
            .`THEN loaded posts are from`(responses)
    }

    /**
     * Asserts the refresh logic when initial load fails, but subsequent loads succeed
     */
    @Test
    fun refreshAfterFailure() {
        val responses = ResponseFactory.listOfResponses(3) {
            addSuccessfulResponse()
            addSuccessfulResponse()
            addSuccessfulResponse()
        }
        val errorResponse = ResponseFactory.listOfResponses(1) {
            addErrorResponse()
        }
        `GIVEN responses are`(errorResponse)
        `WHEN loads posts`()
            .`THEN has network error`()
            .`GIVEN responses are`(responses)
            .`WHEN refresh`()
            .`THEN loaded posts are from`(responses)

    }

    private fun `GIVEN responses are`(response: List<Response?>) = givenResponseAre(response)

    private fun Listing<UnsplashPost>.`GIVEN responses are`(response: List<Response?>): Listing<UnsplashPost> {
        givenResponseAre(response)
        return this
    }

    private fun givenResponseAre(response: List<Response?>) {
        response.forEachIndexed { index, item ->
            Mockito.`when`(mockUnsplashApi.getPage(index + 1))
                .thenReturn(item)
        }
    }

    /**
     * Load single page
     */
    private fun `WHEN loads posts`(pageSize: Int = 2): Listing<UnsplashPost> {
        val listing = repositorySubject.getPosts(pageSize = pageSize)
        getPagedList(listing)
        return listing
    }

    /**
     * Load all pages
     */
    private fun Listing<UnsplashPost>.`WHEN loads all posts`(pageSize: Int = 2): Listing<UnsplashPost> {
        getPagedList(this).loadAllData()
        return this
    }

    /**
     * Retry and load all pages
     */
    private fun Listing<UnsplashPost>.`WHEN retry`(): Listing<UnsplashPost> {
        this.retry()
        this.pagedList.value?.loadAllData()
        return this
    }

    /**
     * Refresh and load all pages
     */
    private fun Listing<UnsplashPost>.`WHEN refresh`(): Listing<UnsplashPost> {
        this.refresh()
        this.pagedList.value?.loadAllData()
        return this
    }

    /**
     * Asserts all the posts are loaded from [responses]
     */
    private fun Listing<UnsplashPost>.`THEN loaded posts are from`(responses: List<Response>): Listing<UnsplashPost> {
        val posts = responses.map {
            UnsplashPost.listFromJson(JSONArray(it.rawData))
        }.reduce { acc, list ->
            val newList = mutableListOf<UnsplashPost>()
            newList.addAll(acc)
            newList.addAll(list)
            newList

        }
        assertThat(this.pagedList.value?.snapshot(), `is`(posts))
        return this
    }

    private fun Listing<UnsplashPost>.`THEN has network error`(): Listing<UnsplashPost> {
        assertThat(getNetworkState(this), `is`(NetworkState.error("network error")))
        return this
    }

    private fun Listing<UnsplashPost>.`THEN networks state is`(networkState: NetworkState): Listing<UnsplashPost> {
        assertThat(getNetworkState(this), `is`(networkState))
        return this
    }


    /**
     * extract the latest paged list from the listing
     */
    private fun getPagedList(listing: Listing<UnsplashPost>): PagedList<UnsplashPost> {
        val observer = LoggingObserver<PagedList<UnsplashPost>>()
        listing.pagedList.observeForever(observer)
        assertThat(observer.value, `is`(notNullValue()))
        return observer.value!!
    }

    /**
     * extract the latest network state from the listing
     */
    private fun getNetworkState(listing: Listing<UnsplashPost>): NetworkState? {
        val networkObserver = LoggingObserver<NetworkState>()
        listing.networkState.observeForever(networkObserver)
        return networkObserver.value
    }

    /**
     * simple observer that logs the latest value it receives
     */
    private class LoggingObserver<T> : Observer<T> {
        var value: T? = null
        override fun onChanged(t: T?) {
            this.value = t
        }
    }
}

