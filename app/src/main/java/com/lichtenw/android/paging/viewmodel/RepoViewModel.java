package com.lichtenw.android.paging.viewmodel;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lichtenw.android.paging.api.GithubServiceAPI;
import com.lichtenw.android.paging.api.RepoSearchResponse;
import com.lichtenw.android.paging.data.GithubDataSourceFactory;
import com.lichtenw.android.paging.data.RepoQueryData;
import com.lichtenw.android.paging.model.Repo;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Repo View Model manages tasks related to network communication, data manipulation, live data
 * updates back to the UI.
 */
public class RepoViewModel extends ViewModel {

    static final String TAG = RepoViewModel.class.getSimpleName();

    private final static int PAGE_SIZE = 30;

    private LiveData<PagedList<Repo>> repoList;
    private GithubDataSourceFactory dataSourceFactory;
    private PublishSubject<RepoQueryData> initiator = PublishSubject.create();
    private PublishProcessor<RepoQueryData> paginator = PublishProcessor.create();
    private MutableLiveData<String> errors = new MutableLiveData<>();
    private MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();


    private GithubServiceAPI githubServiceAPI;


    public RepoViewModel() {

        initGithubApi();

        subscribeForData();

        dataSourceFactory = new GithubDataSourceFactory(initiator, paginator);

        PagedList.Config config = new PagedList.Config.Builder()
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(true)
                .setInitialLoadSizeHint(PAGE_SIZE*2)
                .setPrefetchDistance(PAGE_SIZE/3)
                .setMaxSize(PAGE_SIZE*10)
                .build();
        repoList = new LivePagedListBuilder<>(dataSourceFactory, config)
                .setFetchExecutor(Executors.newFixedThreadPool(3))
                .build();
    }


    private void subscribeForData() {

        // Using debounce() to reduce requests frequency on key input
        // Using filter() to reduce requests until there are more than 2 characters
        // Using distinctUtilChanged() to ignore making same requests
        // Using switchMap() to discard ongoing requests and return only the latest response
        Disposable d1 = initiator
                .debounce(400, TimeUnit.MILLISECONDS)
                .filter(qd -> qd.query != null && qd.query.length() > 2)
                .distinctUntilChanged()
                .observeOn(Schedulers.io())
                .switchMap(qd -> Observable.just(fetchRepos(qd)) )
                .subscribe(qd -> onResult(qd), error -> onError(error));

        // Using backPressureDrop() to drop requests if it can't handle more than it's capacity of 128 requests
        // Using concatMap() to concatenate multiple requests to act like a single request.
        Disposable d2 = paginator
                .onBackpressureDrop()
                .observeOn(Schedulers.io())
                .concatMap(qd -> Flowable.just(fetchRepos(qd)))
                .subscribe(qd -> onResult(qd), error -> onError(error));

        compositeDisposable.add(d1);
        compositeDisposable.add(d2);
    }


    public LiveData<PagedList<Repo>> getRepos() {
        return repoList;
    }


    public LiveData<String> getErrors() {
        return errors;
    }


    public LiveData<Boolean> getLoading() {
        return loading;
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        if (compositeDisposable != null) {
            compositeDisposable.clear();
            compositeDisposable = null;
        }
    }


    public void resetQuery() {
        submitQuery("");
    }


    public void submitQuery(String query) {
        dataSourceFactory.setQuery(query);
        // causes DataSource create to be invoked again...
        repoList.getValue().getDataSource().invalidate();
    }


    private void onResult(RepoQueryData qd) {
        loading.postValue(false);
        if (qd.response == null || qd.response.items == null) {
            return;
        }
        Log.d(TAG, "onResult(). Total count: " + qd.response.total_count);
        if (qd.initialCallback != null) {
            qd.initialCallback.onResult(qd.response.items, null,qd.pageNum+1);
        } else {
            qd.pagingCallback.onResult(qd.response.items, qd.pageNum+1);
        }
    }


    private void onError(Throwable error) {
        Log.e(TAG, "Error", error);
        loading.postValue(false);
        errors.postValue(error.getMessage());
    }


    private RepoQueryData fetchRepos(RepoQueryData qd) throws Exception {

        Log.d(TAG, "Execute Query: " + qd.query + ", for page: " + qd.pageNum);
        loading.postValue(true);
        Call<RepoSearchResponse> call = githubServiceAPI.getRepos(qd.query, qd.loadSize, qd.pageNum);
        Response<RepoSearchResponse> response = call.execute();
        if (response.code() == 200) {
            qd.response = response.body();
            return qd;
        } else {
            String msg = "\nGithub Query Request Failed\n\n";
            if (response.code() == 403) {
                if (response.headers().get("X-RateLimit-Remaining").equals("0")) {
                    msg += "Too Many Requests Per Minute\n";
                }
            }
            onError(new Exception(msg));
        }
        return qd;
    }


    private void initGithubApi() {
        Gson gson = new GsonBuilder().setLenient().create();

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        //interceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        //interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(24, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GithubServiceAPI.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        githubServiceAPI = retrofit.create(GithubServiceAPI.class);
    }
}