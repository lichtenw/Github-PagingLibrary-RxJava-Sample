package com.lichtenw.android.paging.data;


import com.lichtenw.android.paging.api.RepoSearchResponse;
import com.lichtenw.android.paging.model.Repo;

import androidx.paging.PageKeyedDataSource;


/**
 * Query data holds the query, callbacks and response.
 */
public class RepoQueryData {

    public final String query;
    public final int loadSize, pageNum;
    public PageKeyedDataSource.LoadInitialCallback<Integer,Repo> initialCallback;
    public PageKeyedDataSource.LoadCallback<Integer,Repo> loadCallback;
    public RepoSearchResponse response;


    private RepoQueryData(String query, int loadSize, int pageNum) {
        this.query = query;
        this.loadSize = loadSize;
        this.pageNum = pageNum;
    }

    public RepoQueryData(String query, int loadSize, int pageNum, PageKeyedDataSource.LoadInitialCallback callback) {
        this(query, loadSize, pageNum);
        this.initialCallback = callback;
    }


    public RepoQueryData(String query, int loadSize, int pageNum, PageKeyedDataSource.LoadCallback callback) {
        this(query, loadSize, pageNum);
        this.loadCallback = callback;
    }
}
