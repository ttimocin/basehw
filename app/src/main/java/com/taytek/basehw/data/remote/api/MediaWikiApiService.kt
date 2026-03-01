package com.taytek.basehw.data.remote.api

import com.taytek.basehw.data.remote.dto.FandomQueryResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * MediaWiki API interface for Hot Wheels and Matchbox Fandom wikis.
 */
interface MediaWikiApiService {

    /**
     * Generic query — full URL is supplied at runtime so we can
     * switch between hotwheels / matchbox / mini-gt wikis.
     */
    @GET
    suspend fun getCategoryMembers(
        @Url url: String,
        @Query("action") action: String = "query",
        @Query("list") list: String = "categorymembers",
        @Query("cmtitle") categoryTitle: String,
        @Query("cmlimit") limit: Int = 500,
        @Query("cmprop") cmProp: String = "title|pageid",
        @Query("cmcontinue") continueToken: String? = null,
        @Query("format") format: String = "json"
    ): FandomQueryResponse

    /**
     * Fetch page images for a batch of page IDs (pipe-separated).
     */
    @GET
    suspend fun getPageImages(
        @Url url: String,
        @Query("action") action: String = "query",
        @Query("prop") prop: String = "pageimages",
        @Query("pageids") pageIds: String,
        @Query("pithumbsize") thumbSize: Int = 500,
        @Query("format") format: String = "json"
    ): FandomQueryResponse
}
