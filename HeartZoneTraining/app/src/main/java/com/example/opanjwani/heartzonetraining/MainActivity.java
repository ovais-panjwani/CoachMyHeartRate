package com.example.opanjwani.heartzonetraining;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ua.sdk.Ua;
import com.ua.sdk.UaException;
import com.ua.sdk.UaLog;
import com.ua.sdk.user.User;


public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
                    LoginFragment.Listener, BluetoothScanFragment.Listener,
                    SetUpFragment.Listener, RecordFragment.Listener{

    private static String REDIRECT_URI = "uasdk" + BuildConfig.CLIENT_KEY + "://mmf.oauth/authorization_callback/";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private Ua ua;
    private Bundle savedInstanceStateLocal;

    private String authorizationCode;
    private String AUTHORIZATION_CODE_KEY = "HEART_RATE_ZONE_TRAINER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        savedInstanceStateLocal = savedInstanceState;

        if (savedInstanceState != null) {
            authorizationCode = savedInstanceState.getString(AUTHORIZATION_CODE_KEY);
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        UaWrapper uaWrapper = UaWrapper.getInstance();
        ua = uaWrapper.getUa();

        Intent intent = getIntent();
        Uri data = intent.getData();
        String action = intent.getAction();

        if (assertClientCredentials()) {
            if (savedInstanceStateLocal == null) {
                //First launch so initialize the state
                if (ua.isAuthenticated()) {
                    onLogin();
                } else {
                    ua.logout(new Ua.LogoutCallback() {
                        @Override
                        public void onLogout(UaException e) {
                            MainActivity.this.onLogout();
                        }
                    });
                }
            }
        }

        if (Intent.ACTION_VIEW.equalsIgnoreCase(action) && data != null) {
            UaLog.debug("Deeplink: " + data.toString());

            // Example implementation of handling OAuth2 authorization. Aside from edge case
            // handling, the key thing to note is that you must retrieve the authorization code from
            // the intent and then call ua.login() with it and a callback. Also, in this app we
            // store the authorization code to prevent reuse on, e.g., orientation changes. You may
            // choose to resolve this with a configChanges flag in your manifest.

            if (data.getScheme().equals(getString(R.string.intent_scheme))
                    && data.getAuthority().equals(getString(R.string.intent_host))) {
                String error = data.getQueryParameter("error");
                if (error != null && error.equals("access_denied")) {
                    new AlertDialog.Builder(this).setTitle("No Authorization")
                            .setMessage(getString(R.string.app_name) + " was not authorized by the user.")
                            .setPositiveButton("OK", null)
                            .show();
                    ua.logout(new Ua.LogoutCallback() {
                        @Override
                        public void onLogout(UaException e) {
                            MainActivity.this.onLogout();
                        }
                    });
                } else {
                    String intentAuthorizationCode = data.getQueryParameter("code");
                    if (intentAuthorizationCode != null) {
                        if (intentAuthorizationCode.equals(authorizationCode)) {
                            // We've already used this authorization code
                            if (ua.getAuthenticationManager().getOAuth2Credentials() != null) {
                                // We already have creds, so show the user logged-in view
                                this.onLogin();
                            } else {
                                // We've already used this authZ code but never got creds
                                UaLog.error("Previous authorization attempt failed.");
                                new AlertDialog.Builder(MainActivity.this).setTitle("Error logging in")
                                        .setMessage("Something broke while we were trying to log you in. Please try again.")
                                        .setPositiveButton("Darn!", null)
                                        .show();
                                ua.logout(new Ua.LogoutCallback() {
                                    @Override
                                    public void onLogout(UaException e) {
                                        MainActivity.this.onLogout();
                                    }
                                });
                            }
                        } else {
                            // We got a new code so we're in the middle of OAuth 2 authorization
                            UaLog.debug("Got OAuth2 authorization code: " + intentAuthorizationCode);
                            ua.login(intentAuthorizationCode, new Ua.LoginCallback() {
                                public void onLogin(User user, UaException e) {
                                    if (e != null) {
                                        UaLog.error("Unable to retrieve access token.", e);
                                        new AlertDialog.Builder(MainActivity.this).setTitle("Error logging in")
                                                .setMessage("Could not log you in via OAuth 2. Please try again.")
                                                .setPositiveButton("Darn!", null)
                                                .show();
                                    } else {
                                        MainActivity.this.onLogin();
                                    }
                                }
                            });

                            // Store this code so we don't try to reuse it later (see onSaveInstanceState)
                            authorizationCode = intentAuthorizationCode;
                        }
                    }
                }
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    private boolean assertClientCredentials() {
        // check if the developer forgot to enter their CLIENT_ID or CLIENT_SECRET
        if (BuildConfig.CLIENT_KEY == null
                || BuildConfig.CLIENT_SECRET == null
                || BuildConfig.CLIENT_KEY.equals("null")
                || BuildConfig.CLIENT_SECRET.equals("null")
                || BuildConfig.CLIENT_KEY.equals("{CLIENT_KEY}")
                || BuildConfig.CLIENT_SECRET.equals("{CLIENT_SECRET}")) {
            new AlertDialog.Builder(this).setTitle("UA SDK Not Initialized")
                    .setMessage("Missing CLIENT_KEY or CLIENT_SECRET. See the README file.")
                    .setPositiveButton("Ok close.", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
            return false;
        }
        return true;
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    private void navigateToPage(int id) {
        BaseFragment fragment = null;

        switch (id) {
            case R.id.nav_set_up:
                fragment = new SetUpFragment();
                break;
            case R.id.nav_history:
                fragment = new HistoryFragment();
                break;
            case R.id.nav_settings:
                fragment = new SettingsFragment();
                break;
        }

        if (fragment != null) {
            navigateToFragment(fragment, false);
        }
    }

    private void navigateToFragment(BaseFragment fragment, boolean addToBackStack) {
        if (fragment == null) {
            throw new RuntimeException("trying to navigate to unknown fragment");
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        if (addToBackStack) {
            ft.addToBackStack(null);
        }

        ft.replace(R.id.container, fragment).commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(AUTHORIZATION_CODE_KEY, authorizationCode);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    public void onLogin() {
        if (ua.isAuthenticated()) {
            navigateToPage(R.id.nav_set_up);
            // Set up the drawer after login
            mNavigationDrawerFragment.setUp(
                    R.id.navigation_drawer,
                    (DrawerLayout) findViewById(R.id.drawer_layout));
        }
    }

    public void onLogout() {
        String authorizationUrl = ua.getUserAuthorizationUrl(REDIRECT_URI);
        LoginFragment fragment = new LoginFragment();
        fragment.setAuthorizationUrl(authorizationUrl);
        UaLog.debug("Navigating to WebView fragment with authorizationUrl: %s", authorizationUrl);
        navigateToFragment(fragment, false);
    }

    public void onReceivedAuthorizationCode(String authorizationCode) {
        UaLog.debug("onReceivedAuthorizationCode: %s", authorizationCode);
        ua.login(authorizationCode, new Ua.LoginCallback() {
            public void onLogin(User user, UaException e) {
                if (e != null) {
                    UaLog.error("Unable to retrieve access token.", e);
                    new AlertDialog.Builder(MainActivity.this).setTitle("Error logging in")
                            .setMessage("Could not log you in via OAuth 2. Please try again.")
                            .setPositiveButton("Darn!", null)
                            .show();
                } else {
                        MainActivity.this.onLogin();
                }
            }
        });
    }

    @Override
    public void onBluetoothSelected() {
        navigateToPage(R.id.nav_set_up);

    }

    @Override
    public void onBleScan() {
        navigateToFragment(new BluetoothScanFragment(), true);
    }

    @Override
    public void onStartClicked() {
        navigateToFragment(new RecordFragment(), true);
    }

    @Override
    public void onFinishWorkout() {
        navigateToPage(R.id.nav_set_up);
    }
}
