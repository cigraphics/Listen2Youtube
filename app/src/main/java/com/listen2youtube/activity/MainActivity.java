package com.listen2youtube.activity;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.listen2youtube.R;
import com.listen2youtube.Utils;
import com.listen2youtube.fragment.BaseFragment;
import com.listen2youtube.fragment.LocalFileFragment;
import com.listen2youtube.fragment.PlaylistFragment;
import com.listen2youtube.fragment.SearchOnlineFragment;
import com.listen2youtube.service.MusicService;

import java.io.File;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SearchView.OnQueryTextListener, ServiceConnection, BaseFragment.OnPlaySong {
    private static final String TAG = "MainActivity";
    private static final String SEARCH_ONLINE = "SEARCH_ONLINE", LOCAL_FILE = "LOCAL_FILE", LOCAL_PLAYLIST = "LOCAL_PLAYLIST";

    private DrawerLayout drawer;
    private SearchView searchView;
    private Toolbar toolbar;
    private MenuItem menuSearch;


    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 160;

    SearchOnlineFragment searchOnlineFragment;
    LocalFileFragment localFileFragment;
    PlaylistFragment playlistFragment;

    String currentFragment;

    MusicService musicService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawer != null) {
            drawer.addDrawerListener(toggle);
        }
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        if (savedInstanceState != null){
            searchOnlineFragment = (SearchOnlineFragment) getSupportFragmentManager().getFragment(savedInstanceState, SEARCH_ONLINE);
            localFileFragment = (LocalFileFragment) getSupportFragmentManager().getFragment(savedInstanceState, LOCAL_FILE);
            playlistFragment = (PlaylistFragment) getSupportFragmentManager().getFragment(savedInstanceState, LOCAL_PLAYLIST);
            currentFragment = savedInstanceState.getString("currentFragment");
            setTitle(savedInstanceState.getString("title"));
        } else
            setTitle("Search online");

        if (searchOnlineFragment == null)
            searchOnlineFragment = new SearchOnlineFragment();
        if (localFileFragment == null)
            localFileFragment = new LocalFileFragment();
        if (playlistFragment == null)
            playlistFragment = new PlaylistFragment();

        if (currentFragment == null || currentFragment.equals(SEARCH_ONLINE))
            switchFragment(searchOnlineFragment, SEARCH_ONLINE);
        else if (currentFragment.equals(LOCAL_FILE))
            switchFragment(localFileFragment, LOCAL_FILE);
        else switchFragment(playlistFragment, LOCAL_PLAYLIST);

//        Fragment fragment = getSupportFragmentManager().findFragmentByTag(SEARCH_ONLINE);
//        if (fragment != null)
//            searchOnlineFragment = (SearchOnlineFragment) fragment;
//        else
//            searchOnlineFragment = new SearchOnlineFragment();
//
//        fragment = getSupportFragmentManager().
//        if (fragment != null)
//            localFileFragment = (LocalFileFragment) fragment;
//        else
//            localFileFragment = new LocalFileFragment();

        ////Test
//        ExoPlayer exoPlayer = ExoPlayer.Factory.newInstance(1);
//        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
//        file = new File(file, "hello.mp3");
//        ExtractorSampleSource sampleSource = new ExtractorSampleSource(
//                Uri.parse("https://murmuring-brushlands-18762.herokuapp.com/?id=09R8_2nJtjg"),
//                new DefaultUriDataSource(this, "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:45.0) Gecko/20100101 Firefox/45.0"),
//                new DefaultAllocator(BUFFER_SEGMENT_SIZE), BUFFER_SEGMENT_SIZE * BUFFER_SEGMENT_COUNT,
//                new WebmExtractor());
//        TrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource, MediaCodecSelector.DEFAULT);
//        exoPlayer.prepare(audioRenderer);
//        exoPlayer.setPlayWhenReady(true);
        Intent service = new Intent(this, MusicService.class);
        //service.setPackage(getPackageName());
        startService(service);
        bindService(service, this, BIND_AUTO_CREATE);

        localFileFragment.setOnPlaySong(this);
    }

    private void switchFragment(Fragment fragment, String tag) {
        currentFragment = tag;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment, tag)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        menuSearch = menu.findItem(R.id.search);

        searchView = (SearchView) menuSearch.getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Intent intent;
        switch (id) {
            case R.id.search_item:
                if (currentFragment.equals(SEARCH_ONLINE))
                    break;
                toolbar.setTitle("Search online");
                switchFragment(searchOnlineFragment, SEARCH_ONLINE);
                break;
            case R.id.download_list:
                if (currentFragment.equals(LOCAL_FILE))
                    break;
                toolbar.setTitle("All local file");
                switchFragment(localFileFragment, LOCAL_FILE);
                break;
            case R.id.playlist:
                if (currentFragment.equals(LOCAL_PLAYLIST))
                    break;
                toolbar.setTitle("Playlist");
                switchFragment(playlistFragment, LOCAL_PLAYLIST);
                break;
            case R.id.settings:
                intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                break;
            case R.id.about:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.clearFocus();
        menuSearch.collapseActionView();
        if (query != null && query.length() > 0) {
            toolbar.setTitle("Search: " + query);
        } else
            return false;
        if (currentFragment.equals(LOCAL_FILE)) {
            localFileFragment.dataSet.doFilter(query);
            localFileFragment.onChanged();
        } else if (currentFragment.equals(SEARCH_ONLINE)){
            searchOnlineFragment.dataSet.searchQueryAsync(query, true);
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (searchView != null && !searchView.isIconified()) {
            searchView.clearFocus();
            menuSearch.collapseActionView();
        } else if (currentFragment.equals(SEARCH_ONLINE) && !toolbar.getTitle().equals("All local file")) {
            toolbar.setTitle("All local file");
            localFileFragment.dataSet.doFilter(null);
            localFileFragment.onChanged();
        } else if (currentFragment.equals(LOCAL_PLAYLIST) && !toolbar.getTitle().equals("Playlist")) {
            toolbar.setTitle("Playlist");
            playlistFragment.mDisplayingPlaylistId = -1;
            playlistFragment.onChanged();
        } else
            super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy - line 119: Clean up tmp dir");
        File tmpFolder = getDir("tmp", Context.MODE_PRIVATE);
        Utils.deleteRecursive(tmpFolder);
        localFileFragment.removeListener();
        musicService = null;
        unbindService(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (searchOnlineFragment.isAdded())
            getSupportFragmentManager().putFragment(outState, SEARCH_ONLINE, searchOnlineFragment);
        if (localFileFragment.isAdded())
            getSupportFragmentManager().putFragment(outState, LOCAL_FILE, localFileFragment);
        if (playlistFragment.isAdded())
            getSupportFragmentManager().putFragment(outState, LOCAL_PLAYLIST, playlistFragment);
        if (currentFragment != null)
            outState.putString("currentFragment", currentFragment);
        outState.putString("title", getTitle().toString());
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Toast.makeText(MainActivity.this, "onServiceConnected", Toast.LENGTH_SHORT).show();
        musicService = ((MusicService.LocalBinder) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
    }

    @Override
    public void playASong(BaseFragment fragment, int position, String tag) {
        Log.e(TAG, "playASong - line 268: " + tag + " " + position);
        if (musicService != null) {
            if (!tag.equals(musicService.getPlayListTag())) {
                musicService.setNewSongList(fragment.getSongList(tag), tag);
            }
            musicService.playSong(position);
        }
    }
}
