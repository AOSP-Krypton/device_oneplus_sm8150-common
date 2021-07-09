/*
 * Copyright (C) 2021 AOSP-Krypton Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.krypton.settings.device;

import static android.provider.Settings.System.CUSTOM_REFRESH_RATE_MODE_APPS;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

public class CustomRefreshModeActivity extends Activity {
    private ExecutorService mExecutor;
    private Handler mHandler;
    private PackageManager mPM;
    private RecyclerView mRecyclerView;
    private CustomRefreshModeListAdapter mAdapter;
    private SearchView mSearchView;
    private List<AppInfo> mSortedFullAppsList;
    private boolean mShowSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_refresh_rate_settings);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mPM = getPackageManager();
        mHandler = new Handler(Looper.getMainLooper());
        mExecutor = Executors.newFixedThreadPool(2);
        mRecyclerView = (RecyclerView) findViewById(R.id.custom_refresh_rate_recyclerview);
        mAdapter = new CustomRefreshModeListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadAppsListAsync(mPM.getInstalledPackages(0));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.custom_refresh_rate_menu, menu);
        MenuItem item = menu.findItem(R.id.search_apps_menu_item);
        mSearchView = (SearchView) item.getActionView();
        mSearchView.setQueryHint(getString(R.string.string_search));
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAppsAsync(newText);
                return true;
            }
        });
        return true;
    }

    @Override
    public void onStop() {
        mExecutor.shutdownNow();
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.show_system:
                mShowSystem = !mShowSystem;
                item.setTitle(mShowSystem ?
                    R.string.string_hide_system : R.string.string_show_system);
                if (mSearchView != null && !mSearchView.isIconified()) {
                    filterAppsAsync(mSearchView.getQuery());
                } else {
                    updateListAsync();
                }
                return true;
            case R.id.reset_choices:
                Utils.putStringInSettings(this, CUSTOM_REFRESH_RATE_MODE_APPS, "");
                mAdapter.notifyDataSetChanged();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadAppsListAsync(final List<PackageInfo> list) {
        mExecutor.execute(() -> {
            list.sort((pInfo1, pInfo2) -> compare(pInfo1.applicationInfo.loadLabel(mPM),
                pInfo2.applicationInfo.loadLabel(mPM)));
            mSortedFullAppsList = new ArrayList<>(list.size());
            final ArrayList<AppInfo> listForAdapter = new ArrayList<>(list.size() / 2);
            mAdapter.updateList(listForAdapter);
            for (PackageInfo pInfo: list) {
                final AppInfo appInfo = new AppInfo(
                    pInfo.applicationInfo.loadIcon(mPM),
                    pInfo.applicationInfo.loadLabel(mPM),
                    pInfo.applicationInfo.isSystemApp(),
                    pInfo.packageName);
                mSortedFullAppsList.add(appInfo);
                if (!appInfo.isSystemApp()) {
                    listForAdapter.add(appInfo);
                    mHandler.post(() -> mAdapter.notifyDataSetChanged());
                }
            }
        });
    }

    private void updateListAsync() {
        mExecutor.execute(() -> {
            if (mShowSystem) {
                mAdapter.updateList(mSortedFullAppsList);
            } else {
                final ArrayList<AppInfo> listForAdapter = new ArrayList<>(mSortedFullAppsList.size() / 2);
                for (AppInfo appInfo: mSortedFullAppsList) {
                    if (!appInfo.isSystemApp()) {
                        listForAdapter.add(appInfo);
                    }
                }
                mAdapter.updateList(listForAdapter);
            }
            mHandler.post(() -> mAdapter.notifyDataSetChanged());
        });
    }

    private void filterAppsAsync(CharSequence query) {
        mExecutor.execute(() -> {
            final ArrayList<AppInfo> newList = new ArrayList<>();
            for (AppInfo appInfo: mSortedFullAppsList) {
                if (!appInfo.isSystemApp() || mShowSystem) {
                    if (appInfo.filter(query)) {
                        newList.add(appInfo);
                    }
                }
            }
            mAdapter.updateList(newList);
            mHandler.post(() -> mAdapter.notifyDataSetChanged());
        });
    }

    private int compare(CharSequence label1, CharSequence label2) {
        return label1.toString().compareToIgnoreCase(label2.toString());
    }
}
