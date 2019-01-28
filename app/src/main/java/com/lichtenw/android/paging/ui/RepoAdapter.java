package com.lichtenw.android.paging.ui;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lichtenw.android.paging.R;
import com.lichtenw.android.paging.model.Repo;

import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;


/**
 * Paging library paged list adapter.
 */
public class RepoAdapter extends PagedListAdapter<Repo, RepoViewHolder> {


    public RepoAdapter() {
        super(DIFF_CALLBACK);
    }

    @Override
    public RepoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.github_item, parent, false);
        return new RepoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RepoViewHolder holder, int position) {
        holder.bindTo(getItem(position));
    }


    static DiffUtil.ItemCallback<Repo> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Repo>() {
                // The ID property identifies when items are the same.
                @Override
                public boolean areItemsTheSame(Repo oldItem, Repo newItem) {
                    return oldItem == newItem;
                }

                // Use Object.equals() to know when an item's content changes.
                // Implement equals(), or write custom data comparison logic here.
                @Override
                public boolean areContentsTheSame(Repo oldItem, Repo newItem) {
                    return oldItem.id == newItem.id;
                }
            };

}
