/*
 * GNU GENERAL PUBLIC LICENSE
 *                 Version 3, 29 June 2007
 *
 *     Copyright (c) 2015 Joaquim Ley <me@joaquimley.com>
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.joaquimley.birdsseyeview.activities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.joaquimley.birdsseyeview.R;
import com.joaquimley.birdsseyeview.helpers.GoogleMapAnimationHelper;
import com.joaquimley.birdsseyeview.helpers.GoogleMapHelper;
import com.joaquimley.birdsseyeview.utils.TestValues;

import java.util.ArrayList;
import java.util.List;

/**
 * Launcher activity with Google Map fragment
 */

public class MapsActivity extends FragmentActivity implements GoogleMap.OnMapLoadedCallback,
        Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener, GoogleMap.OnMapClickListener, SearchView.OnClickListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private Marker mMarker;
    private AnimatorSet mAnimatorSet;
    private Menu mMenu;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    private LatLng[] mRouteExample;
    SearchView searchView;
    ImageButton ivVoice;
    EditText tvMg;
    ProgressDialog pd;
    String spokenText;
    private HeatmapTileProvider mProvider;
    private TileOverlay mOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        searchView = (SearchView) findViewById(R.id.floating_search_view);
        ivVoice = (ImageButton) findViewById(R.id.iv_voice);
        ivVoice.setOnClickListener(this);
        tvMg = (EditText) findViewById(R.id.tv_mg);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Initialize UI elements, listeners etc.
     */
    private void init() {

        mRouteExample = GoogleMapHelper.createMapRoute(new LatLng(28.482653, 77.065631),
                new LatLng(28.479390, 77.070112), new LatLng(28.477372, 77.068134), new LatLng(28.479519, 77.080222));
    }

    /**
     * Creates a map instance if there isn't one already created
     */
    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            if (mMap != null) {
                setUpMap(false, false, false);
            }
        }
    }

    /**
     * Creation and customization of the map
     *
     * @param isIndoorEnabled      self explanatory
     * @param isAllGesturesEnabled self explanatory
     * @param isZoomControlEnabled self explanatory
     */
    private void setUpMap(boolean isIndoorEnabled, boolean isAllGesturesEnabled, boolean isZoomControlEnabled) {
        mMap.setIndoorEnabled(isIndoorEnabled);

        // Disable gestures & controls since ideal results (pause Animator) is
        // not easy to show in a simplified example.
        mMap.getUiSettings().setAllGesturesEnabled(isAllGesturesEnabled);
        mMap.getUiSettings().setZoomControlsEnabled(isZoomControlEnabled);

        // Create a marker to represent the user on the route.
        mMarker = mMap.addMarker(GoogleMapHelper.createMarker(mRouteExample[0], false,
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

//        mMap.addMarker(GoogleMapHelper.createMarker(mRouteExample[0], false,
//                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        // Create a polyline for the route.
        mMap.addPolyline(GoogleMapHelper.createPolyline(mRouteExample, TestValues.POLYLINE_FINAL_COLOR,
                TestValues.POLYLINE_WIDTH));

        // Once the map is ready, zoom to the beginning of the route start the
        // animation.
        mMap.setOnMapLoadedCallback(this);

        // Move the camera over the start position.
        CameraPosition pos = GoogleMapAnimationHelper.createCameraPosition(mRouteExample,
                TestValues.CAMERA_OBLIQUE_ZOOM);

        addHeatMap();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
        mMap.setOnMapClickListener(this);
    }

    /**
     * When the map has finished loading all it's components (listener), calls the
     * GoogleMapsAnimationHelper.createRouteAnimatorSet() and starts animation (via callAnimateRoute()) method
     */
    @Override
    public void onMapLoaded() {
        // Once the camera has moved to the beginning of the route,
        // start the animation.


        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                mMap.setOnCameraChangeListener(null);
                callAnimateRoute();
            }
        });

        // Animate the camera to the beginning of the route.
        CameraPosition pos = GoogleMapAnimationHelper.createCameraPosition(mRouteExample,
                TestValues.CAMERA_OBLIQUE_ZOOM, TestValues.CAMERA_OBLIQUE_TILT);
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
    }

    /**
     * Calls the createRouteAnimatorSet, here to use the MapsActivity.this as the listener(s)
     * starts the animation.
     */
    private void callAnimateRoute() {

        mAnimatorSet = GoogleMapAnimationHelper.createRouteAnimatorSet(mRouteExample, mMap,
                TestValues.CAMERA_HEADING_CHANGE_RATE, mMarker, this, this, 0, 0);
        mAnimatorSet.start();
    }

    private void addHeatMap() {
        List<LatLng> list = null;
        list = new ArrayList<>();
        list.add(new LatLng(28.235049940269292 , 77.28803633280342));
        list.add(new LatLng(27.90998858969923 , 77.26792815094979));
        list.add(new LatLng(28.055684415380945 , 76.86118835500598));
        list.add(new LatLng(27.408396108647676 , 76.82341838301392));
        list.add(new LatLng(28.225825565077155 , 77.28755303729871));
        list.add(new LatLng(27.892805054191097 , 77.28323722304084));
        list.add(new LatLng(28.264873481579603 , 76.30748644343075));
        list.add(new LatLng(27.764742280873413 , 76.52631834283271));
        list.add(new LatLng(28.524549710672567 , 77.00301231814531));
        list.add(new LatLng(27.68088482709732 , 77.005397018282));
        list.add(new LatLng(28.54967018946187 , 76.99303691592459));
        list.add(new LatLng(27.417894184440936 , 76.99919241846828));
        list.add(new LatLng(28.00224154294479 , 77.46316191507873));
        list.add(new LatLng(27.993371067120894 , 77.09587818565477));
        list.add(new LatLng(28.003658701837733 , 76.49738883588348));
        list.add(new LatLng(27.99347900268358 , 76.64015714110597));
        list.add(new LatLng(28.358750837293808 , 77.1533149610516));
        list.add(new LatLng(27.644170351345704 , 77.16801505024014));
        list.add(new LatLng(28.57992190255971 , 76.85013689034629));
        list.add(new LatLng(27.665024170399736 , 76.92353896217878));
        list.add(new LatLng(28.082972738842237 , 77.62624535259339));
        list.add(new LatLng(27.987089205071484 , 77.54205291771892));
        list.add(new LatLng(28.140468085365608 , 76.79060083750308));
        list.add(new LatLng(27.83492564195939 , 76.44755578507142));
        list.add(new LatLng(28.40267058797446 , 77.00058532657097));
        list.add(new LatLng(27.67588578927512 , 77.00005653731895));
        list.add(new LatLng(28.068214918986733 , 76.9998387386549));
        list.add(new LatLng(27.68621886767404 , 76.99961018380479));
        list.add(new LatLng(28.000405202375244 , 77.14698831644647));
        list.add(new LatLng(27.99987749689257 , 77.1853018961347));
        list.add(new LatLng(28.000236053345102 , 76.86726515659967));
        list.add(new LatLng(27.999564363743833 , 76.85734565045497));
        list.add(new LatLng(28.02283013163098 , 77.87961011504393));
        list.add(new LatLng(27.632940585804853 , 77.67793926589516));
        list.add(new LatLng(28.120454815909298 , 76.81108802008426));
        list.add(new LatLng(27.74927204874989 , 76.13577828266716));
        list.add(new LatLng(28.254329980190285 , 77.15135243431259));
        list.add(new LatLng(27.400660859894273 , 77.31835453611573));
        list.add(new LatLng(28.512122123210425 , 76.7549430524811));
        list.add(new LatLng(27.40095235897241 , 76.78317607289297));
        list.add(new LatLng(28.179699231162655 , 77.21196525245875));
        list.add(new LatLng(27.71998703872144 , 77.16160772562661));
        list.add(new LatLng(28.335540968937035 , 76.94286023753223));
        list.add(new LatLng(27.618056580793887 , 76.9613415470634));
        list.add(new LatLng(28.156148352230446 , 77.10774145811529));
        list.add(new LatLng(27.913153372647514 , 77.60091851616019));
        list.add(new LatLng(28.127602445179715 , 76.59051748196933));
        list.add(new LatLng(27.86962545884631 , 76.60369530127605));
        list.add(new LatLng(28.209557898478945 , 77.58167697799507));
        list.add(new LatLng(27.972710601538232 , 77.13466850214914));
        list.add(new LatLng(28.244619199512034 , 76.901725250586));
        list.add(new LatLng(27.573235838135982 , 76.3242224207267));
        list.add(new LatLng(28.114172369840723 , 77.0528400947665));
        list.add(new LatLng(27.527749089996313 , 77.28215428639284));
        list.add(new LatLng(28.429992155791343 , 76.84940329793582));
        list.add(new LatLng(27.432287317768527 , 76.53867683361702));
        list.add(new LatLng(28.925436782240737 , 77.52634257582713));
        list.add(new LatLng(27.926208353311747 , 77.38071455595882));
        list.add(new LatLng(28.66557637098983 , 76.3836015102223));
        list.add(new LatLng(27.293653290054763 , 76.73121205574274));
        list.add(new LatLng(28.410111450843534 , 77.00259197679529));
        list.add(new LatLng(27.397044664233913 , 77.8263131493263));
        list.add(new LatLng(28.177435782009635 , 76.40550222280235));
        list.add(new LatLng(27.849381767300393 , 76.14152187391613));
        list.add(new LatLng(28.0012986001423 , 77.01593907299332));
        list.add(new LatLng(27.415146911683042 , 77.11929723521533));
        list.add(new LatLng(28.511173973545375 , 76.91787955604124));
        list.add(new LatLng(27.628841246296737 , 76.95701539490615));
        list.add(new LatLng(28.0656352327234 , 77.39179729668854));
        list.add(new LatLng(27.969375624174738 , 77.05353262126259));
        list.add(new LatLng(28.057730522924572 , 76.36889416145607));
        list.add(new LatLng(27.95317589563212 , 76.83122025801396));
        list.add(new LatLng(28.491386999566224 , 77.39022020158465));
        list.add(new LatLng(27.74730449897559 , 77.64227531749657));
        list.add(new LatLng(28.408026975446454 , 76.47335159003369));
        list.add(new LatLng(27.823522000359834 , 76.91334125856052));
        list.add(new LatLng(28.26552039143396 , 77.28811900957628));
        list.add(new LatLng(27.739754833844927 , 77.51013497824019));
        list.add(new LatLng(28.623190362243196 , 76.68532962469372));
        list.add(new LatLng(27.639582937003354 , 76.85142060840835));
        list.add(new LatLng(28.0046881850981 , 77.22910534381185));
        list.add(new LatLng(27.966820280680075 , 77.60155470198796));
        list.add(new LatLng(28.017064470394327 , 76.27816487053657));
        list.add(new LatLng(27.956084023632574 , 76.1735460850173));
        list.add(new LatLng(28.933313882912373 , 77.02008979381348));
        list.add(new LatLng(27.92023523547758 , 77.00108632182133));
        list.add(new LatLng(28.095019016215407 , 76.95888767940058));
        list.add(new LatLng(27.3774109621571 , 76.98350588790541));
        list.add(new LatLng(28.299234177204283 , 77.1916849766002));
        list.add(new LatLng(27.6746028361062 , 77.25776462067788));
        list.add(new LatLng(28.198491738847814 , 76.85614042503808));
        list.add(new LatLng(27.73981866019187 , 76.78210208228914));
        list.add(new LatLng(28.065954458551985 , 77.31508476587774));
        list.add(new LatLng(27.93782621687446 , 77.34153695352188));
        list.add(new LatLng(28.1198294552624 , 76.92360413327438));
        list.add(new LatLng(27.789183167998583 , 76.88359163321334));
        list.add(new LatLng(28.085776209835785 , 77.03640755351697));
        list.add(new LatLng(27.88576668648151 , 77.28350645848799));
        list.add(new LatLng(28.06598675327922 , 76.7029229125231));
        list.add(new LatLng(27.768158175620584 , 76.75651882523037));
        list.add(new LatLng(28.693221567522063 , 77.18378916867744));
        list.add(new LatLng(27.9946786944575 , 77.20327346988519));
        list.add(new LatLng(28.3920272744314 , 76.79898424452219));
        list.add(new LatLng(27.480845015421252 , 76.95593196532931));
        list.add(new LatLng(28.09881551641523 , 77.00930645665403));
        list.add(new LatLng(27.63567145992938 , 77.268603024063));
        list.add(new LatLng(28.22723650844004 , 76.69699592621083));
        list.add(new LatLng(27.40009965162315 , 76.8441072254207));
        list.add(new LatLng(28.265099516237722 , 77.30333115989495));
        list.add(new LatLng(27.985639261029345 , 77.4396324092514));
        list.add(new LatLng(28.011379462658276 , 76.92860958250964));
        list.add(new LatLng(27.810544087661537 , 76.50891966280678));
        list.add(new LatLng(28.04579138743629 , 77.43237364111918));
        list.add(new LatLng(27.970761518738158 , 77.39676008748785));
        list.add(new LatLng(28.006557898682836 , 76.62634114707295));
        list.add(new LatLng(27.98889255076337 , 76.7001992743395));
        list.add(new LatLng(28.148955471049604 , 77.01243656583813));
        list.add(new LatLng(27.590524689916126 , 77.02696853164498));
        list.add(new LatLng(28.079639097013377 , 76.98924365748447));
        list.add(new LatLng(27.800864144057083 , 76.98334489984252));
        list.add(new LatLng(28.077651655003883 , 77.06324074569798));
        list.add(new LatLng(27.654375833348446 , 77.20690473838702));
        list.add(new LatLng(28.39563889016568 , 76.75827192608585));
        list.add(new LatLng(27.87854493521954 , 76.69797046788169));
        list.add(new LatLng(28.061755609841754 , 77.29990884535376));
        list.add(new LatLng(27.767963479102686 , 77.34672185925541));
        list.add(new LatLng(28.054474421557572 , 76.55260939466224));
        list.add(new LatLng(27.850578354251358 , 76.7888174355359));
        list.add(new LatLng(28.027049119538116 , 77.01622087712032));
        list.add(new LatLng(27.9963068640847 , 77.02447584777221));
        list.add(new LatLng(28.015170410263654 , 76.90621158691776));
        list.add(new LatLng(27.98981405306237 , 76.9323848563286));
        list.add(new LatLng(28.076473073325694 , 77.02164163064455));
        list.add(new LatLng(27.910616007043167 , 77.02768553976613));
        list.add(new LatLng(28.07177859006738 , 76.95831917382448));
        list.add(new LatLng(27.997103807657172 , 76.98097081639365));
        list.add(new LatLng(28.379226188333554 , 77.1736118691936));
        list.add(new LatLng(27.315783590611268 , 77.0766602351004));
        list.add(new LatLng(28.427515487235837 , 76.82459324676721));
        list.add(new LatLng(27.726632754409955 , 76.89920285064555));
        list.add(new LatLng(28.182898526062782 , 77.14214910454785));
        list.add(new LatLng(27.92622003987489 , 77.11416853467831));
        list.add(new LatLng(28.15774976883827 , 76.56410210594274));
        list.add(new LatLng(27.9151853028187 , 76.25693985753918));
        list.add(new LatLng(28.150402547987 , 77.31220309382434));
        list.add(new LatLng(27.83649696590989 , 77.24543912496218));
        list.add(new LatLng(28.20291313193539 , 76.98080795180897));
        list.add(new LatLng(27.723034416493064 , 76.60222461737942));
        list.add(new LatLng(28.319842883905416 , 77.01984751426298));
        list.add(new LatLng(27.84381745411323 , 77.15811655385772));
        list.add(new LatLng(28.025049486026507 , 76.85040354415686));
        list.add(new LatLng(27.59131375182705 , 76.68139415868704));
        list.add(new LatLng(28.23649015960697 , 77.06137252570245));
        list.add(new LatLng(27.550930194507423 , 77.00712657517816));
        list.add(new LatLng(28.225352142549426 , 76.9358225080733));
        list.add(new LatLng(27.996972516711885 , 76.84231942599861));
        list.add(new LatLng(28.019412120789436 , 77.11456511683649));
        list.add(new LatLng(27.961211431344907 , 77.52501544362406));
        list.add(new LatLng(28.22198940918427 , 76.99427315382182));
        list.add(new LatLng(27.99307524182891 , 76.34137917935465));
        list.add(new LatLng(28.17185776847381 , 77.0867239736015));
        list.add(new LatLng(27.474418503653936 , 77.02706902568336));
        list.add(new LatLng(28.374506514786685 , 76.96415785863647));
        list.add(new LatLng(27.708433961628494 , 76.80623942983448));
        list.add(new LatLng(28.135094759055175 , 77.08964395129956));
        list.add(new LatLng(27.816257209555907 , 77.27586323554377));
        list.add(new LatLng(28.08310840692761 , 76.82216138232072));
        list.add(new LatLng(27.93065074983129 , 76.5075585882013));
        list.add(new LatLng(28.072532298533563 , 77.03845276010992));
        list.add(new LatLng(27.968180313115873 , 77.5415737091227));
        list.add(new LatLng(28.052537608327686 , 76.80394258819315));
        list.add(new LatLng(27.966337530322186 , 76.64639774240517));
        list.add(new LatLng(28.257664642524052 , 77.11072843992838));
        list.add(new LatLng(27.86040444033634 , 77.06051949830153));
        list.add(new LatLng(28.03660908476654 , 76.93127240508726));
        list.add(new LatLng(27.6247804079702 , 76.99212135600524));
        list.add(new LatLng(28.112892557392755 , 77.02642817943072));
        list.add(new LatLng(27.94936781301053 , 77.00747478473185));
        list.add(new LatLng(28.042261536415303 , 76.98407550982054));
        list.add(new LatLng(27.89838637690323 , 76.77793611674336));
        list.add(new LatLng(28.34301847551938 , 77.13291439805626));
        list.add(new LatLng(27.974429624684717 , 77.0285500985361));
        list.add(new LatLng(28.172807805208688 , 76.92613971255085));
        list.add(new LatLng(27.58154669352196 , 76.90667466418732));
        list.add(new LatLng(28.20914922936844 , 77.0199074492895));
        list.add(new LatLng(27.686050781250394 , 77.10922143198871));
        list.add(new LatLng(28.54620660442844 , 76.84187953564268));
        list.add(new LatLng(27.478191592394555 , 76.91863585135329));
        list.add(new LatLng(28.081564771100517 , 77.10012163916763));
        list.add(new LatLng(27.932741289325527 , 77.37439416858535));
        list.add(new LatLng(28.210505927616033 , 76.34685187782598));
        list.add(new LatLng(27.760807382726707 , 76.98242999782593));
        list.add(new LatLng(28.037065305347873 , 77.02675336780082));
        list.add(new LatLng(27.41647368045366 , 77.0887254766432));
        list.add(new LatLng(28.121064387576695 , 76.92364692538192));
        list.add(new LatLng(27.77101505904582 , 76.95980005801295));
        list.add(new LatLng(28.03547375951532 , 77.3254789293814));
        list.add(new LatLng(27.93209579678316 , 77.04069307886444));
        list.add(new LatLng(28.040776153207386 , 76.981064889093));
        list.add(new LatLng(27.940787289198077 , 76.42047562082175));
        list.add(new LatLng(28.124944739055934 , 77.04113923050832));
        list.add(new LatLng(27.985688983073786 , 77.27166137067701));
        list.add(new LatLng(28.550583032820608 , 76.56980473583292));
        list.add(new LatLng(27.722045846453806 , 76.46602329229341));
        list.add(new LatLng(28.411752188668537 , 77.5380902382238));
        list.add(new LatLng(27.40198199348087 , 77.50665535740904));
        list.add(new LatLng(28.201561555119795 , 76.41879321558064));
        list.add(new LatLng(27.177496890722924 , 76.51390997748553));
        list.add(new LatLng(28.263983975873305 , 77.72179134980136));
        list.add(new LatLng(27.710034557682157 , 77.6826026456604));
        list.add(new LatLng(28.261822497571657 , 76.42273759068068));
        list.add(new LatLng(27.622149244392592 , 76.34508641077063));
        list.add(new LatLng(28.591500053768105 , 77.09630836045282));
        list.add(new LatLng(27.47610129000458 , 77.14928086807839));
        list.add(new LatLng(28.668190647294974 , 76.82278766654484));
        list.add(new LatLng(27.716870732401926 , 76.96002084840057));
        list.add(new LatLng(28.12167611889208 , 77.55052761481254));
        list.add(new LatLng(27.813373235044907 , 77.31700239283198));
        list.add(new LatLng(28.143142116944606 , 76.85652995725265));
        list.add(new LatLng(27.448195190783 , 76.42320460455657));
        list.add(new LatLng(28.023893972869427 , 77.00936598433853));
        list.add(new LatLng(27.430575973595683 , 77.2020001628938));
        list.add(new LatLng(28.06643248241303 , 76.46825673850388));
        list.add(new LatLng(27.811420106873832 , 76.68034473091114));
        list.add(new LatLng(28.04831228781668 , 77.4204826054767));
        list.add(new LatLng(27.882093579548002 , 77.42069552414735));
        list.add(new LatLng(28.146886522292252 , 76.3605177146475));
        list.add(new LatLng(27.96264044616147 , 76.67939345544828));
        list.add(new LatLng(28.14073584473623 , 77.11605728207687));
        list.add(new LatLng(27.715261620723147 , 77.07488516569433));
        list.add(new LatLng(28.250755938958772 , 76.98671959236039));
        list.add(new LatLng(27.346672622350955 , 76.92342950022376));
        list.add(new LatLng(28.34568085248501 , 77.58924237234329));
        list.add(new LatLng(27.56177077125048 , 77.19331943804163));
        list.add(new LatLng(28.075252859126003 , 76.21739561746726));
        list.add(new LatLng(27.692713786080148 , 76.73438005618188));
        list.add(new LatLng(28.733435549451062 , 77.01760123590924));
        list.add(new LatLng(27.5110955176065 , 77.17606693260485));
        list.add(new LatLng(28.25884316152628 , 76.87729516394924));
        list.add(new LatLng(27.502106541826798 , 76.68251290500099));

        mProvider = new HeatmapTileProvider.Builder().data(list).build();
        mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }


    /**
     * Google Map animation listener mAnimatorSet
     */
    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        mMap.moveCamera(CameraUpdateFactory
                .newCameraPosition(CameraPosition.builder(mMap.getCameraPosition())
                        .bearing((Float) valueAnimator.getAnimatedValue())
                        .build()));
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        // Toast.makeText(getApplicationContext(), "Animation Cancel", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAnimationEnd(Animator animation) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setCancelable(true);
        builder.setTitle("Reached");
        builder.setMessage("You have reached your destination");
        final AlertDialog dialog = builder.create();
        dialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
                allDone();

            }
        }, 4000);


    }

    void allDone() {
        tvMg.setText("");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    public void onAnimationStart(Animator animation) {
        //Toast.makeText(getApplicationContext(), "Animation Start", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        mMenu = menu; // Keep the menu for later use (swapping icons).
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mMap == null) {
            return true;
        }

        switch (item.getItemId()) {

            case R.id.action_marker:
                mMarker.setVisible(!mMarker.isVisible());
                return true;

            case R.id.action_buildings:
                mMap.setBuildingsEnabled(!mMap.isBuildingsEnabled());
                return true;

            case R.id.action_animation:
                if (mAnimatorSet.isRunning()) {
                    mAnimatorSet.cancel();
                } else {
                    mAnimatorSet.start();
                }
                return true;

            case R.id.action_perspective:

                CameraPosition currentPosition = mMap.getCameraPosition();
                CameraPosition newPosition;
                if (currentPosition.zoom == TestValues.CAMERA_OBLIQUE_ZOOM
                        && currentPosition.tilt == TestValues.CAMERA_OBLIQUE_TILT) {
                    newPosition = new CameraPosition.Builder()
                            .tilt(GoogleMapAnimationHelper.getMaximumTilt(19))
                            .zoom(19)
                            .bearing(currentPosition.bearing)
                            .target(currentPosition.target).build();
                } else {
                    newPosition = new CameraPosition.Builder()
                            .tilt(TestValues.CAMERA_OBLIQUE_TILT)
                            .zoom(TestValues.CAMERA_OBLIQUE_ZOOM)
                            .bearing(currentPosition.bearing)
                            .target(currentPosition.target).build();
                }
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(newPosition));

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        GoogleMapAnimationHelper.animateLiftOff(mMap, 2);
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.iv_voice:
                displaySpeechRecognizer();
                break;

        }


    }

    private static final int SPEECH_REQUEST_CODE = 0;

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            spokenText = results.get(0);
            // Do something with spokenText
            Log.i("TAG", spokenText);
            // searchView.setQuery(spokenText,true);


            pd = new ProgressDialog(MapsActivity.this);
            pd.setTitle("Please wait");
            pd.setMessage("Finding best route for your safety ");
            pd.setCancelable(false);
            pd.show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    pd.dismiss();
                    voiceComplete();
                }
            }, 4000);


        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void voiceComplete() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mMap = null;
        mGoogleApiClient = null;
        mLocationRequest = null;
        mLastLocation = null;
        mCurrLocationMarker = null;

        if(spokenText.equals("MG Road")){
        tvMg.setText("MG Road Metro Station, Maruti Housing Colony, Sector 28, Gurugram, Haryana 122022");
            init();
        }else if(spokenText.equals("IFFCO Chowk")){
            tvMg.setText("IFFCO Chowk, Sector 29, Gurugram, Haryana");
            init1();
        }

        if (GoogleMapHelper.googleServicesAvailability(this) == null) {
            setUpMapIfNeeded();
        }
    }

    private void init1() {

        mRouteExample = GoogleMapHelper.createMapRoute(new LatLng(28.482654, 77.065628),
                new LatLng(28.479109, 77.070634), new LatLng(28.477308, 77.068287), new LatLng(28.478096, 77.071701), new LatLng(28.471682, 77.072597));
    }


    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();


        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Your Location");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));


        if (mCurrLocationMarker == null) {
            mCurrLocationMarker = mMap.addMarker(markerOptions);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(mCurrLocationMarker.getPosition()).zoom(8).build();


            mMap.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
        addHeatMap();


    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }
}