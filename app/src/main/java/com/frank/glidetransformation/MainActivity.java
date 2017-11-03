package com.frank.glidetransformation;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.bumptech.glide.Glide;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private TeamListAdapter mAdapter;
    private List<TeamBean> mGameList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mAdapter = new TeamListAdapter(this, mGameList);
        mRecyclerView.setAdapter(mAdapter);
        refreshList();
    }

    private void refreshList() {
        mGameList.clear();
        for (int i = 0; i < TeamConstants.logoList.length; i++) {
            TeamBean teamBean = new TeamBean();
            teamBean.setId("ID" + i);
            teamBean.setName(TeamConstants.nameList[i]);
            teamBean.setImage(TeamConstants.logoList[i]);
            mGameList.add(teamBean);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_all_cache:
                clearAllCache();
                break;
            case R.id.clear_memory_cache:
                clearMemoryCache();
                break;
            case R.id.clear_disk_cache:
                clearDiskCache();
                break;
            default:
                refreshList();
                break;
        }
        return true;
    }

    private void clearAllCache() {
        clearMemoryCache();
        clearDiskCache();
    }

    private void clearMemoryCache() {
        Glide.get(this).clearMemory();
    }

    private void clearDiskCache() {
        new ClearDiskCacheTask(this).execute();
    }

    private static class ClearDiskCacheTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<Activity> mActivity;

        public ClearDiskCacheTask(Activity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final Activity activity = mActivity.get();
            if (activity != null) {
                Glide.get(activity).clearDiskCache();
            }
            return null;
        }
    }
}
