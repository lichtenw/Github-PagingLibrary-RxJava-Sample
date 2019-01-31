package com.lichtenw.android.paging.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.lichtenw.android.paging.R;
import com.lichtenw.android.paging.viewmodel.RepoViewModel;


/**
 * App that demos instant search and pagination using paging library, Retrofit2 and RxJava.
 */
public class MainActivity extends AppCompatActivity {

    static final String TAG = MainActivity.class.getSimpleName();

    private RepoViewModel viewModel;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle(R.string.app_name);

        RepoPagedListAdapter adapter = new RepoPagedListAdapter();

        viewModel = ViewModelProviders.of(this).get(RepoViewModel.class);
        viewModel.getRepos().observe(this, adapter::submitList);
        viewModel.getErrors().observe(this, this::showError);
        viewModel.getLoading().observe(this, this::onProgress);

        progressBar = findViewById(R.id.progress_bar);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        final EditText editText = findViewById(R.id.edit_text);
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.submitQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.clear_img).setOnClickListener((v) -> {
            editText.setText("");
            viewModel.resetQuery();
            progressBar.setVisibility(View.INVISIBLE);
        });
    }


    private void onProgress(Boolean b) {
        progressBar.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }


    private void showError(String error) {
        progressBar.setVisibility(View.INVISIBLE);
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("\n" + error + "\n")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}

