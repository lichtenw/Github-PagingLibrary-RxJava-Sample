package com.lichtenw.android.paging.data;

import android.util.Log;
import com.lichtenw.android.paging.model.Repo;
import androidx.paging.DataSource;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.PublishSubject;


public class GithubDataSourceFactory extends DataSource.Factory<Integer, Repo> {

    private static final String TAG = GithubDataSourceFactory.class.getSimpleName();

    private String query;
    private PublishSubject<RepoQueryData> initiator;
    private PublishProcessor<RepoQueryData> paginator;


    public GithubDataSourceFactory(PublishSubject<RepoQueryData> initiator, PublishProcessor<RepoQueryData> paginator) {
        this.initiator = initiator;
        this.paginator = paginator;
    }


    public void setQuery(String query) {
        this.query = query;
    }


    @Override
    public DataSource<Integer, Repo> create() {
        Log.d(TAG, "Create DataSource " + query);
        GithubDataSource source = new GithubDataSource();
        source.setPublishers(initiator, paginator);
        source.setQuery(query);
        return source;
    }
}

