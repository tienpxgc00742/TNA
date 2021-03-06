package com.tnaapp.tnalayout.activity;

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.FrameLayout;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.tienpx.videodrag.DraggableListener;
import com.tienpx.videodrag.DraggableView;
import com.google.android.libraries.mediaframework.exoplayerextensions.Video;
import com.google.android.libraries.mediaframework.layeredvideo.PlaybackControlLayer;
import com.tnaapp.tnalayout.R;
import com.tnaapp.tnalayout.player.VideoPlayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements FragmentDrawer.FragmentDrawerListener, PlaybackControlLayer.FullscreenCallback {
    private static String TAG = MainActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private FragmentDrawer drawerFragment;
    private HomeFragment homeFragment;
    private AboutFragment aboutFragment;
    public static AccessToken accessToken;
    CallbackManager callbackManager;
    private LoginButton loginButton;
    //Video
    private VideoPlayer videoPlayer;
    private FrameLayout videoPlayerContainer;
    DraggableView draggableView;
    FrameLayout bottom_layout;
    DrawerLayout drawerLayout;
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        try {
            Log.d("", "Start");
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.tnaapp.tnalayout",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("YourKeyHash :", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("", "Error");
        } catch (NoSuchAlgorithmException e) {
            Log.d("", "Error");
        }

        callbackManager = CallbackManager.Factory.create();
        setContentView(R.layout.activity_main);
        ///////////////////////
        draggableView = (DraggableView) findViewById(R.id.draggable_view);
        videoPlayerContainer = (FrameLayout) findViewById(R.id.video_frame);
        bottom_layout = (FrameLayout) findViewById(R.id.bottom_fl);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        hookDraggableViewListener();
        initializeVideoView();
        ///////////////////////
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions(Arrays.asList("public_profile, email, user_birthday"));

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("Token: ", loginResult.getAccessToken().getToken());
                FragmentDrawer.mUserName.setText(getResources().getString(R.string.loading));
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                try {
                                    FragmentDrawer.mUserName.setText(object.getString("name"));
                                    Profile profile = Profile.getCurrentProfile();
                                    if (profile != null) {
                                        new DownloadImageTask(FragmentDrawer.mUserImage).execute(Profile.getCurrentProfile().getProfilePictureUri(200, 200).toString());
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        });
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                Log.d("Login: ", "Login attempt canceled.");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.d("Login: ", "Login attempt failed.");
            }
        });
        reloadCurrentLoggedInSession();

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        drawerFragment = (FragmentDrawer)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, drawerLayout, mToolbar);
        drawerFragment.setDrawerListener(this);
        homeFragment = new HomeFragment();
        aboutFragment = new AboutFragment();
        displayView(0);
    }

    public void homeButton(View v) {
        createImaPlayer("http://rmcdn.2mdn.net/MotifFiles/html/1248596/android_1330378998288.mp4");
        draggableView.setVisibility(View.VISIBLE);
        draggableView.maximize();
    }

    public static void reloadCurrentLoggedInSession() {
        // If the access token is available already assign it.
        FragmentDrawer.mUserImage.setClickable(true);
        accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null) {
            FragmentDrawer.mUserName.setText(FragmentDrawer.mUserName.getContext().getResources().getString(R.string.loading));
            GraphRequest request = GraphRequest.newMeRequest(
                    accessToken,
                    new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject object, GraphResponse response) {
                            try {
                                FragmentDrawer.mUserName.setText(object.getString("name"));
                                Profile profile = Profile.getCurrentProfile();
                                if (profile != null) {
                                    new DownloadImageTask(FragmentDrawer.mUserImage).execute(Profile.getCurrentProfile().getProfilePictureUri(200, 200).toString());
                                }
                            } catch (JSONException | NullPointerException e) {
                                FragmentDrawer.mUserName.setText(FragmentDrawer.mUserName.getContext().getString(R.string.warning_no_network));
                                //không cho nhấn user image
                                FragmentDrawer.mUserImage.setClickable(false);
                                return;
                            }
                        }
                    });
            request.executeAsync();
        } else {
            FragmentDrawer.mUserName.setText(FragmentDrawer.mUserName.getContext().getString(R.string.login));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_search) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDrawerItemSelected(View view, int position) {
        displayView(position);
    }

    private void displayView(int position) {
        Fragment fragment = null;
        String title = getString(R.string.app_name);
        switch (position) {
            case 0:
                fragment = homeFragment;
                title = getString(R.string.title_home);
                break;
            case 1:
                fragment = aboutFragment;
                title = getString(R.string.title_about);
                break;
            default:
                break;
        }

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.container_body, fragment);
            fragmentTransaction.commit();

            // set the toolbar title
            getSupportActionBar().setTitle(title);
        }
    }
    //////////////

    /**
     * Hook DraggableListener to draggableView to pause or resume VideoView.
     */
    private void hookDraggableViewListener() {
        draggableView.setDraggableListener(new DraggableListener() {
            @Override
            public void onMaximized() {

                videoPlayer.play();
            }

            //Empty
            @Override
            public void onMinimized() {
                //Empty
                videoPlayer.pause();
                //videoPlayer.hide();
            }

            @Override
            public void onClosedToLeft() {
                videoPlayer.release();
            }

            @Override
            public void onClosedToRight() {
                videoPlayer.release();
            }
        });
    }

    private void initializeVideoView() {
        draggableView.setVisibility(View.GONE);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    public void createImaPlayer(String url) {
        if (videoPlayer != null) {
            videoPlayer.release();
        }
        videoPlayerContainer.removeAllViews();

        String adTagUrl = null;
        String videoTitle = "  FUCK";

        videoPlayer = new VideoPlayer(this, videoPlayerContainer,
                new Video(url, Video.VideoType.MP4),
                videoTitle,
                adTagUrl);
        videoPlayer.setFullscreenCallback(this);
        //Set color
        videoPlayer.setSeekBarColor(Color.RED);
        Resources res = getResources();
        Drawable logo = res.getDrawable(R.drawable.gmf_icon);
        videoPlayer.setLogoImage(logo);
        videoPlayer.play();
    }

    @Override
    protected void onDestroy() {
        if (videoPlayer != null) {
            videoPlayer.release();
        }
        super.onDestroy();
    }

    @Override
    public void onGoToFullscreen() {
        videoPlayerContainer.requestFocus();
        Log.d("", "Hidden");
    }

    @Override
    public void onReturnFromFullscreen() {;
        Log.d("", "Visible");
    }

}
