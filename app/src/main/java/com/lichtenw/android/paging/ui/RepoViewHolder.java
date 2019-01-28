package com.lichtenw.android.paging.ui;

import android.view.View;
import android.widget.TextView;

import com.lichtenw.android.paging.R;
import com.lichtenw.android.paging.model.Repo;

import androidx.recyclerview.widget.RecyclerView;


/**
 * Simple view holder with a text view to show the repo name.
 */
public class RepoViewHolder extends RecyclerView.ViewHolder {

    private TextView contentTextView;

    public RepoViewHolder(View itemView) {
        super(itemView);
        contentTextView = itemView.findViewById(R.id.content);
    }

    void bindTo(Repo repo) {
        contentTextView.setText(repo == null ? null : repo.name);
    }
}

