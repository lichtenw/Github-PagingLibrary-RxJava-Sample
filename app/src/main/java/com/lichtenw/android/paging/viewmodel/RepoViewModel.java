package com.lichtenw.android.paging.viewmodel;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lichtenw.android.paging.api.GithubAPI;
import com.lichtenw.android.paging.api.RepoSearchResponse;
import com.lichtenw.android.paging.data.GithubDataSourceFactory;
import com.lichtenw.android.paging.data.RepoQueryData;
import com.lichtenw.android.paging.model.Repo;

import java.util.concurrent.TimeUnit;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
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
 * View model
 */
public class RepoViewModel extends ViewModel {

    static final String TAG = RepoViewModel.class.getSimpleName();

    private LiveData<PagedList<Repo>> repoList;
    private GithubDataSourceFactory dataSourceFactory;
    private PublishSubject<RepoQueryData> initiator = PublishSubject.create();
    private PublishProcessor<RepoQueryData> paginator = PublishProcessor.create();
    private MutableLiveData<String> errors = new MutableLiveData<>();
    private MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Integer lastPage;


    private GithubAPI githubAPI;


    public RepoViewModel() {

        initGithubApi();

        subscribeForData();

        dataSourceFactory = new GithubDataSourceFactory(initiator, paginator);

        PagedList.Config config = new PagedList.Config.Builder()
                .setPageSize(40)
                .setEnablePlaceholders(true)
                .setInitialLoadSizeHint(60)
                .setPrefetchDistance(20)
                .setMaxSize(1000)
                .build();
        repoList = new LivePagedListBuilder<>(dataSourceFactory, config).build();
    }


    private void subscribeForData() {

        // Using debounce() to reduce requests frequency on key input
        // Using filter() to reduce requests until there is more than 2 characters
        // Using distinctUtilChanged() to ignore making same requests
        // Using switchMap() to cancel/discard previous requests and return only the latest response
        // Relevant article here...
        // https://blog.mindorks.com/implement-search-using-rxjava-operators-c8882b64fe1d
        Disposable d1 = initiator
                .debounce(300, TimeUnit.MILLISECONDS)
                .filter(qd -> qd.query.length() > 2)
                .distinctUntilChanged()
                .observeOn(Schedulers.io())
                .switchMap(qd -> Observable.just(fetchRepos(qd)) )
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(qd -> {
                    loading.postValue(false);
                    qd.initialCallback.onResult(qd.response.items, null,qd.pageNum+1);
                }, error -> {
                    errors.postValue(error.getMessage());
                });

        // Using backpressure to reduce requests frequency
        // Using concatMap() to concatenate the output of multiple observables to act like a single observable
        Disposable d2 = paginator
                .onBackpressureDrop()
                .observeOn(Schedulers.io())
                .concatMap(qd -> Flowable.just(fetchRepos(qd)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(qd -> {
                    loading.postValue(false);
                    Integer nextPage = qd.pageNum+1 == lastPage ? null : qd.pageNum+1;
                    qd.loadCallback.onResult(qd.response.items, nextPage);
                }, error -> {
                    errors.postValue(error.getMessage());
                });

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
        lastPage = null;
        submitQuery("");
    }


    public void submitQuery(String query) {
        dataSourceFactory.setQuery(query);
        // causes DataSource create to be invoked again...
        repoList.getValue().getDataSource().invalidate();
    }


    private RepoQueryData fetchRepos(RepoQueryData qd) throws Exception {

        Log.d(TAG, "Execute Query: " + qd.query + ", for page: " + qd.pageNum);
        loading.postValue(true);
        Call<RepoSearchResponse> call = githubAPI.getRepos(qd.query, qd.loadSize, qd.pageNum);
        Response<RepoSearchResponse> response = call.execute();
        if (response.code() == 200) {
            RepoSearchResponse repoSearchResponse = response.body();
            Log.d(TAG, "# TOTAL ITEMS: " + repoSearchResponse.total_count);
            String pageLinks = response.headers().get("Link");
            if (repoSearchResponse.total_count == 0 || pageLinks == null) {
                throw new Exception("Github Query Request Failed\n\nNo Items For Query '" + qd.query + "'");
            }
            // Find the last page number...
            Log.d(TAG, "PAGE LINKS: " + pageLinks);
            int idx = pageLinks.lastIndexOf("rel=\"last\"");
            if (idx != -1) {
                idx = pageLinks.lastIndexOf("page=", idx);
                try {
                    String num = pageLinks.substring(idx+5,pageLinks.indexOf(">",idx+5));
                    lastPage = Integer.parseInt(num);
                    Log.d(TAG, "LAST PAGE: " + lastPage);
                } catch (Exception ex) {
                    lastPage = null;
                }
            }
            qd.response = repoSearchResponse;
            return qd;
        } else if (response.code() == 403) {
            lastPage = null;
            String limit = response.headers().get("X-RateLimit-Remaining");
            if (Integer.parseInt(limit) == 0) {
                throw new Exception("Github Query Request Failed\n\nToo Many Requests Per Minute");
            }
            throw new Exception("Http Error " + response.code());
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
                .baseUrl(GithubAPI.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        githubAPI = retrofit.create(GithubAPI.class);
    }
}
