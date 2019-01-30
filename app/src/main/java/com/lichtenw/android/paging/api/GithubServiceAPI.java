package com.lichtenw.android.paging.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

//
// API Info:
// https://developer.github.com/v3/search/#search-repositories
//
// API Example:
// https://api.github.com/search/repositories?q=tetris+language:assembly&sort=stars&order=desc
//
public interface GithubServiceAPI {

    String BASE_URL = "https://api.github.com/";

    @GET("search/repositories")
    Call<RepoSearchResponse> getRepos(@Query("q") String q, @Query("per_page") int perPage, @Query("page") int page);
}
