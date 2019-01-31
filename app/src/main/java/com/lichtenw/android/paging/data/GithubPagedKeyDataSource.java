package com.lichtenw.android.paging.data;

import android.util.Log;

import com.lichtenw.android.paging.model.Repo;

import androidx.annotation.NonNull;
import androidx.paging.PageKeyedDataSource;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.PublishSubject;


/**
 * Network only data source for pagination.
 */
public class GithubPagedKeyDataSource extends PageKeyedDataSource<Integer, Repo> {

    static final String TAG = GithubPagedKeyDataSource.class.getSimpleName();

    private String query;
    private PublishSubject<RepoQueryData> initiator;
    private PublishProcessor<RepoQueryData> paginator;


    public GithubPagedKeyDataSource() {
    }


    public void setPublishers(PublishSubject<RepoQueryData> initiator, PublishProcessor<RepoQueryData> paginator) {
        this.initiator = initiator;
        this.paginator = paginator;
    }


    public void setQuery(String query) {
        this.query = query;
    }


    public void loadInitial(@NonNull PageKeyedDataSource.LoadInitialParams<Integer> params,
                            @NonNull LoadInitialCallback<Integer, Repo> callback) {
        Log.d(TAG, "loadInitial");
        initiator.onNext(new RepoQueryData(query, params.requestedLoadSize, 1, callback));
    }


    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params,
                           @NonNull LoadCallback<Integer, Repo> callback) {
        // ignored, since we only ever append to our initial load
    }


    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params,
                          @NonNull LoadCallback<Integer, Repo> callback) {
        Log.d(TAG, "loadAfter, page: " + params.key);
        paginator.onNext(new RepoQueryData(query, params.requestedLoadSize, params.key, callback));
    }
}
