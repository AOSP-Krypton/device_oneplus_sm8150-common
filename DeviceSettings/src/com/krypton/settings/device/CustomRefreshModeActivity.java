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

import android.app.ActionBar;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import androidx.fragment.app.FragmentActivity;

import com.krypton.settings.device.fragments.CustomRefreshModeFragment;

public class CustomRefreshModeActivity extends FragmentActivity {

    private ActionBar actionBar;
    private CustomRefreshModeFragment mFragment;
    private SearchView mSearchView;
    private boolean mShowSystem = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_refresh_rate_settings);
        mFragment = (CustomRefreshModeFragment) getSupportFragmentManager().findFragmentById(R.id.custom_refresh_rate_fragment);
        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.custom_refresh_rate_fragment_title));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.custom_refresh_rate_menu, menu);
        MenuItem item = menu.findItem(R.id.search_apps_menu_item);
        mSearchView = (SearchView) item.getActionView();
        mSearchView.setQueryHint(getResources().getString(R.string.string_search_app));
        handleSearch();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.show_system:
                mShowSystem = !mShowSystem;
                item.setTitle(mShowSystem == true ? R.string.string_hide_system : R.string.string_show_system);
                if (mFragment != null) {
                    mFragment.updateStatus(mShowSystem);
                    if (mSearchView != null && !mSearchView.isIconified())
                        mFragment.filterApps(mSearchView.getQuery().toString());
                    else mFragment.updateView();
                }
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleSearch() {
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (mFragment != null) mFragment.filterApps(newText);
                return true;
            }
        });
    }
}
