/******************************************************************************
 * Project:  Metro4All
 * Purpose:  Routing in subway.
 * Authors:  Dmitry Baryshnikov (polimax@mail.ru), Stanislav Petriakov
 ******************************************************************************
*   Copyright (C) 2013-2015 NextGIS
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.metroaccess;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.nextgis.metroaccess.data.DownloadData;
import com.nextgis.metroaccess.data.GraphDataItem;
import com.nextgis.metroaccess.data.MAGraph;
import com.nextgis.metroaccess.data.PortalItem;
import com.nextgis.metroaccess.data.RouteItem;
import com.nextgis.metroaccess.data.StationItem;

import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.asu.emit.qyan.alg.model.Path;
import edu.asu.emit.qyan.alg.model.abstracts.BaseVertex;

import static com.nextgis.metroaccess.Constants.ARRIVAL_RESULT;
import static com.nextgis.metroaccess.Constants.BUNDLE_CITY_CHANGED;
import static com.nextgis.metroaccess.Constants.BUNDLE_ENTRANCE_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_ERRORMARK_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_EVENTSRC_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_MSG_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_PATHCOUNT_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_PATH_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_PAYLOAD_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_PORTALID_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_STATIONID_KEY;
import static com.nextgis.metroaccess.Constants.DEPARTURE_RESULT;
import static com.nextgis.metroaccess.Constants.ICONS_RAW;
import static com.nextgis.metroaccess.Constants.LOCATING_TIMEOUT;
import static com.nextgis.metroaccess.Constants.META;
import static com.nextgis.metroaccess.Constants.PREF_RESULT;
import static com.nextgis.metroaccess.Constants.REMOTE_METAFILE;
import static com.nextgis.metroaccess.Constants.ROUTE_DATA_DIR;
import static com.nextgis.metroaccess.Constants.STATUS_FINISH_LOCATING;
import static com.nextgis.metroaccess.Constants.STATUS_INTERRUPT_LOCATING;
import static com.nextgis.metroaccess.Constants.TAG;
import static com.nextgis.metroaccess.PreferencesActivity.clearRecent;

//https://code.google.com/p/k-shortest-paths/

public class MainActivity extends ActionBarActivity {

	protected boolean m_bInterfaceLoaded;

	protected static Handler m_oGetJSONHandler;

	protected Button m_oSearchButton;
	protected MenuItem m_oSearchMenuItem;

	protected int m_nDepartureStationId, m_nArrivalStationId;
	protected int m_nDeparturePortalId, m_nArrivalPortalId;

	protected List<DownloadData> m_asDownloadData;

	public static String m_sUrl = "http://metro4all.org/data/v2.7/";
	public static MAGraph m_oGraph;

	protected ListView m_lvListButtons;
    protected ButtonListAdapter m_laListButtons;

    GpsMyLocationProvider gpsMyLocationProvider;

    Menu menu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        gpsMyLocationProvider = new GpsMyLocationProvider(this);
		setContentView(R.layout.empty_activity_main);

		m_bInterfaceLoaded = false;

        // initialize the default settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        m_nDepartureStationId = prefs.getInt("dep_"+BUNDLE_STATIONID_KEY, -1);
        m_nArrivalStationId = prefs.getInt("arr_"+BUNDLE_STATIONID_KEY, -1);
        m_nDeparturePortalId = prefs.getInt("dep_"+BUNDLE_PORTALID_KEY, -1);
        m_nArrivalPortalId = prefs.getInt("arr_"+BUNDLE_PORTALID_KEY, -1);

        m_sUrl = prefs.getString(PreferencesActivity.KEY_PREF_DOWNLOAD_PATH, m_sUrl);

        m_oGraph = Analytics.getGraph();

		//create downloading queue empty initially
		m_asDownloadData = new ArrayList<>();

		CreateHandler();

		//check for data exist
		if(IsRoutingDataExist()){
			//else check for updates
			CheckForUpdates();
		}
		else{
			//ask to download data
			GetRoutingData();
		}

        boolean disableGA = prefs.getBoolean(PreferencesActivity.KEY_PREF_GA, true);
        ((Analytics) getApplication()).reload(disableGA);
        GoogleAnalytics.getInstance(this).setDryRun(true);
	}

    protected void CreateHandler(){

		m_oGetJSONHandler = new Handler() {
            public void handleMessage(Message msg) {
            	super.handleMessage(msg);

            	Bundle resultData = msg.getData();
            	boolean bHaveErr = resultData.getBoolean(BUNDLE_ERRORMARK_KEY);
            	int nEventSource = resultData.getInt(BUNDLE_EVENTSRC_KEY);
            	String sPayload = resultData.getString(BUNDLE_PAYLOAD_KEY);

            	if(bHaveErr){
            		MainActivity.this.ErrMessage(resultData.getString(BUNDLE_MSG_KEY));
	            	switch(nEventSource){
	            	case 1://get remote meta
	            		File file = new File(getExternalFilesDir(null), REMOTE_METAFILE);
	            		sPayload = readFromFile(file);
	            		break;
	            	case 2:
	            		if(IsRoutingDataExist())
	                    	LoadInterface();
	            		break;
            		default:
            			return;
	            	}
            	}

            	switch(nEventSource){
            	case 1://get remote meta
            		if(IsRoutingDataExist()){
            			//check if updates available
            			CheckUpdatesAvailable(sPayload);
            		}
            		else{
            			AskForDownloadData(sPayload);
            		}
            		break;
            	case 2:
            		if(m_asDownloadData.isEmpty()){
            			m_oGraph.FillRouteMetadata();
            			LoadInterface();
            		}
            		else{
            			OnDownloadData();
            		}
            		break;
            	}
            }
        };
	}

	protected void LoadInterface(){

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String sCurrentCity = prefs.getString(PreferencesActivity.KEY_PREF_CITY, m_oGraph.GetCurrentCity());

        if (sCurrentCity == null)
            return;

        if(sCurrentCity.length() < 2){
        	//find first city and load it
        	m_oGraph.SetFirstCityAsCurrent();
        }
        else{
        	m_oGraph.SetCurrentCity( sCurrentCity );
        }

        if(!m_oGraph.IsValid())
        	return;

		m_bInterfaceLoaded = true;
		setContentView(R.layout.activity_main);

        View view = findViewById(R.id.ivLimitations);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((Analytics) getApplication()).addEvent(Analytics.SCREEN_MAIN, Analytics.LIMITATIONS, Analytics.SCREEN_MAIN);
                onSettings(true);
            }
        });

        setLimitationsColor(getLimitationsColor());

		m_lvListButtons = (ListView)findViewById(R.id.lvButtList);
		m_laListButtons = new ButtonListAdapter(this);
		// set adapter to list view
        m_lvListButtons.addFooterView(new View(this), null, true);
		m_lvListButtons.setAdapter(m_laListButtons);
		m_lvListButtons.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	        	switch(position){
	        	case 0: //from
                    ((Analytics) getApplication()).addEvent(Analytics.SCREEN_MAIN, Analytics.FROM, Analytics.PANE);
	        		onSelectDepatrure();
	        		break;
	        	case 1: //to
                    ((Analytics) getApplication()).addEvent(Analytics.SCREEN_MAIN, Analytics.TO, Analytics.PANE);
	        		onSelectArrival();
	        		break;
	        	}
	        }
	    });


		m_oSearchButton = (Button) findViewById(R.id.btSearch);
		m_oSearchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((Analytics) getApplication()).addEvent(Analytics.SCREEN_MAIN, "Search route", Analytics.SCREEN_MAIN);
            	onSearch();
             }
        });
		m_oSearchButton.setEnabled(false);

    	if(m_oSearchButton != null)
    		m_oSearchButton.setEnabled(false);
    	if(m_oSearchMenuItem != null)
    		m_oSearchMenuItem.setEnabled(false);

    	if(!m_oGraph.IsValid()){
    		MainActivity.this.ErrMessage( m_oGraph.GetLastError());
    	}
    	else{
    		UpdateUI();
    	}

	}

    private int getLimitationsColor() {
        return LimitationsActivity.hasLimitations(this) ? getResources().getColor(android.R.color.white) : getResources().getColor(R.color.metro_material_dark);
    }

    private void setLimitationsColor(int color) {
        if (LimitationsActivity.hasLimitations(this))
            findViewById(R.id.ivLimitations).setBackgroundResource(R.drawable.btn_selector);
        else
            findViewById(R.id.ivLimitations).setBackgroundResource(R.drawable.btn_limitations_off_selector);

        ImageView iv = (ImageView) findViewById(R.id.ivLimitations);
        Bitmap bitmap = getBitmapFromSVG(this, R.raw.wheelchair_icon, color);
        iv.setImageBitmap(bitmap);
    }

    protected void onSettings(boolean isLimitations) {
        if (isLimitations)
            startActivityForResult(new Intent(this, LimitationsActivity.class), PREF_RESULT);
        else {
            startActivityForResult(new Intent(this, PreferencesActivity.class), PREF_RESULT);
        }

        //intentSet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);        
        //Bundle bundle = new Bundle();
        //bundle.putParcelable(BUNDLE_METAMAP_KEY, m_oGraph);
        //intentSet.putExtras(bundle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;

        getMenuInflater().inflate(R.menu.menu_main, menu);
        tintIcons(menu, this);
		return true;
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in action bar clicked; go home
            return false;
        case R.id.btn_settings:
            ((Analytics) getApplication()).addEvent(Analytics.SCREEN_MAIN, Analytics.MENU_SETTINGS, Analytics.MENU);
            onSettings(false);
            return true;
        case R.id.btn_limitations:
            ((Analytics) getApplication()).addEvent(Analytics.SCREEN_MAIN, Analytics.LIMITATIONS, Analytics.MENU);
            onSettings(true);
            return true;
        case R.id.btn_report:
            Intent intentReport = new Intent(this, ReportActivity.class);
            intentReport.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentReport);
            return true;
        case R.id.btn_about:
            ((Analytics) getApplication()).addEvent(Analytics.SCREEN_MAIN, Analytics.MENU_ABOUT, Analytics.MENU);
            Intent intentAbout = new Intent(this, AboutActivity.class);
            intentAbout.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentAbout);
            return true;
        case R.id.btn_locate:
            if (!item.isEnabled()) return true;

            if (!m_bInterfaceLoaded) {
                Toast.makeText(this, R.string.sLocationNoCitySelected, Toast.LENGTH_SHORT).show();
                return true;
            }

            ((Analytics) getApplication()).addEvent(Analytics.SCREEN_MAIN, "Locate closest entrance", Analytics.ACTION_BAR);

            final Context context = this;
            if (isProviderDisabled(context, false)) {
                showLocationInfoDialog(context, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (isProviderDisabled(context, true))
                            Toast.makeText(context, R.string.sLocationFail, Toast.LENGTH_LONG).show();
                        else
                            locateClosestEntrance();
                    }
                });
            } else
                locateClosestEntrance();
            break;
        }
		return super.onOptionsItemSelected(item);
	}

    public static void showLocationInfoDialog(final Context context, DialogInterface.OnClickListener onNegativeButtonClicked) {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        final boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        final boolean isLocationDisabled = isProviderDisabled(context, true);

        if (isProviderDisabled(context, false)) {   // at least one provider is turned off
            String network, gps, info;
            network = gps = "";

            if (!isNetworkEnabled)
                network = "\r\n- " + context.getString(R.string.sLocationNetwork);

            if(!isGPSEnabled)
                gps = "\r\n- " + context.getString(R.string.sLocationGPS);

            if (isLocationDisabled)
                info = context.getString(R.string.sLocationDisabledMsg);
            else
                info = context.getString(R.string.sLocationInaccuracy) + network + gps;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.sLocationAccuracy).setMessage(info)
                    .setPositiveButton(R.string.sSettings,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                }
                            })
                    .setNegativeButton(R.string.sCancel, onNegativeButtonClicked);
            builder.create();
            builder.show();
        }
    }

    public static boolean isProviderDisabled(Context context, boolean both) {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        final boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        return both ? !isGPSEnabled && !isNetworkEnabled : !isGPSEnabled || !isNetworkEnabled;
    }

    private void locateClosestEntrance() {
        menu.findItem(R.id.btn_locate).setEnabled(false);
        Toast.makeText(this, R.string.sLocationStart, Toast.LENGTH_SHORT).show();

        final Handler h = new Handler(){
            private boolean isLocationFound = false;

            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case STATUS_INTERRUPT_LOCATING:
                        if(!isLocationFound)
                            Toast.makeText(getApplicationContext(), R.string.sLocationFail, Toast.LENGTH_LONG).show();
                    case STATUS_FINISH_LOCATING:
                        gpsMyLocationProvider.stopLocationProvider();
                        isLocationFound = true;
                        menu.findItem(R.id.btn_locate).setEnabled(true);
                        break;
                }
            }
        };

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                h.sendEmptyMessage(STATUS_INTERRUPT_LOCATING);
            }
        }, LOCATING_TIMEOUT);

        gpsMyLocationProvider.startLocationProvider(new IMyLocationConsumer() {
            StationItem stationClosest = null;
            PortalItem portalClosest = null;

            @Override
            public void onLocationChanged(Location location, IMyLocationProvider iMyLocationProvider) {
                double currentLat = location.getLatitude();
                double currentLon = location.getLongitude();

                float shortest = Float.MAX_VALUE;
                float distance[] = new float[1];
                List<StationItem> stations = new ArrayList<>(m_oGraph.GetStations().values());

                for (int i = 0; i < stations.size(); i++) {  // find closest station first
                    Location.distanceBetween(currentLat, currentLon, stations.get(i).GetLatitude(), stations.get(i).GetLongitude(), distance);

                    if (distance[0] < shortest) {
                        shortest = distance[0];
                        stationClosest = stations.get(i);
                    }
                }

                if (stationClosest != null) {  // and then closest station's portal
                    shortest = Float.MAX_VALUE;
                    List<PortalItem> portals = stationClosest.GetPortals(true);

                    for (int i = 0; i < portals.size(); i++) {
                        Location.distanceBetween(currentLat, currentLon, portals.get(i).GetLatitude(), portals.get(i).GetLongitude(), distance);

                        if (distance[0] < shortest) {
                            shortest = distance[0];
                            portalClosest = portals.get(i);
                        }
                    }

                    Intent intent = new Intent();
                    intent.putExtra(BUNDLE_STATIONID_KEY, stationClosest.GetId());
                    intent.putExtra(BUNDLE_PORTALID_KEY, portalClosest.GetId());
                    onActivityResult(DEPARTURE_RESULT, RESULT_OK, intent);
                }

                h.sendEmptyMessage(STATUS_FINISH_LOCATING);

                if(stationClosest != null && portalClosest != null) {
                    String portalName = portalClosest.GetReadableMeetCode();
                    portalName = portalName.equals("") ? ": " + portalClosest.GetName() : " " + portalName + ": " + portalClosest.GetName();

                    Toast.makeText(getApplicationContext(), String.format(getString(R.string.sStationPortalName), stationClosest.GetName(),
                            getString(R.string.sEntranceName), portalName), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

	protected void CheckForUpdates(){
		final MetaDownloader uploader = new MetaDownloader(MainActivity.this, getResources().getString(R.string.sDownLoading), m_oGetJSONHandler, true);
		uploader.execute(GetDownloadURL() + META);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                uploader.Abort();
                LoadInterface();
            }
        }, 3000);
	}

	protected void GetRoutingData(){
		MetaDownloader loader = new MetaDownloader(MainActivity.this, getResources().getString(R.string.sDownLoading), m_oGetJSONHandler, true);
		loader.execute(GetDownloadURL() + META);
	}

	//check if data for routing is downloaded
	protected boolean IsRoutingDataExist(){
		return m_oGraph.IsRoutingDataExist();
	}

	protected void CheckUpdatesAvailable(String sJSON){

		m_oGraph.OnUpdateMeta(sJSON, true);
		final List<GraphDataItem> items = m_oGraph.HasChanges();
        Collections.sort(items);

		int count = items.size();
		if(count < 1){
			LoadInterface();
			return;
		}
		final boolean[] checkedItems = new boolean[count];
	    for(int i = 0; i < count; i++){
	    	checkedItems[i] = true;
	    }

	    final CharSequence[] checkedItemStrings = new CharSequence[count];
	    for(int i = 0; i < count; i++){
	    	checkedItemStrings[i] = items.get(i).GetFullName();
	    }

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.sUpdateAvaliable)
		.setMultiChoiceItems(checkedItemStrings, checkedItems,
				new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				checkedItems[which] = isChecked;
			}
		})
		.setPositiveButton(R.string.sDownload,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {

				m_asDownloadData.clear();

				for (int i = 0; i < checkedItems.length; i++) {
					if (checkedItems[i]){
						m_asDownloadData.add(new DownloadData(MainActivity.this, items.get(i), GetDownloadURL() + items.get(i).GetPath() + ".zip", m_oGetJSONHandler));
					}
				}

				OnDownloadData();

			}
		})

		.setNegativeButton(R.string.sCancel,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				LoadInterface();
				dialog.cancel();
			}
		});
		builder.create();
		builder.show();
    }

	protected void AskForDownloadData(String sJSON){
		//ask user for download
		m_oGraph.OnUpdateMeta(sJSON, false);
		final List<GraphDataItem> items = m_oGraph.HasChanges();
        Collections.sort(items);

	    int count = items.size();
	    if(count == 0)
	    	return;

	    final boolean[] checkedItems = new boolean[count];
	    for(int i = 0; i < count; i++){
	    	checkedItems[i] = false;
	    }

	    final CharSequence[] checkedItemStrings = new CharSequence[count];
	    for(int i = 0; i < count; i++){
	    	checkedItemStrings[i] = items.get(i).GetFullName();
	    }

	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.sSelectDataToDownload)
			   .setMultiChoiceItems(checkedItemStrings, checkedItems,
						new DialogInterface.OnMultiChoiceClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which, boolean isChecked) {
								checkedItems[which] = isChecked;
							}
						})
				.setPositiveButton(R.string.sDownload,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {

								m_asDownloadData.clear();

								for (int i = 0; i < checkedItems.length; i++) {
									if (checkedItems[i]){
										m_asDownloadData.add(new DownloadData(MainActivity.this, items.get(i), GetDownloadURL() + items.get(i).GetPath() + ".zip", m_oGetJSONHandler));
									}
								}
								OnDownloadData();
							}
						})

				.setNegativeButton(R.string.sCancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();

							}
						});
		builder.create();
		builder.show();
	}

	protected void OnDownloadData(){
		if(m_asDownloadData.isEmpty())
			return;
		DownloadData data = m_asDownloadData.get(0);
		m_asDownloadData.remove(0);

		data.OnDownload();
	}

	public static boolean writeToFile(File filePath, String sData){
		try{
			FileOutputStream os = new FileOutputStream(filePath, false);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(os);
	        outputStreamWriter.write(sData);
	        outputStreamWriter.close();
	        return true;
		}
		catch(IOException e){
			return false;
		}
	}

	public static String readFromFile(File filePath) {

	    String ret = "";

	    try {
	    	FileInputStream inputStream = new FileInputStream(filePath);

	        if ( inputStream != null ) {
	            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
	            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
	            String receiveString = "";
	            StringBuilder stringBuilder = new StringBuilder();

	            while ( (receiveString = bufferedReader.readLine()) != null ) {
	                stringBuilder.append(receiveString);
	            }

	            inputStream.close();
	            ret = stringBuilder.toString();
	        }
	    }
	    catch (FileNotFoundException e) {
	    	e.printStackTrace();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }

	    return ret;
	}

	@Override
	protected void onPause() {
		final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();

		//store departure and arrival

		edit.putInt("dep_" + BUNDLE_STATIONID_KEY, m_nDepartureStationId);
		edit.putInt("arr_" + BUNDLE_STATIONID_KEY, m_nArrivalStationId);
		edit.putInt("dep_" + BUNDLE_PORTALID_KEY, m_nDeparturePortalId);
		edit.putInt("arr_" + BUNDLE_PORTALID_KEY, m_nArrivalPortalId);

		edit.apply();

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

	    m_nDepartureStationId = prefs.getInt("dep_"+BUNDLE_STATIONID_KEY, -1);
	    m_nArrivalStationId = prefs.getInt("arr_"+BUNDLE_STATIONID_KEY, -1);
	    m_nDeparturePortalId = prefs.getInt("dep_"+BUNDLE_PORTALID_KEY, -1);
	    m_nArrivalPortalId = prefs.getInt("arr_"+BUNDLE_PORTALID_KEY, -1);

		//check if routing data changed
		m_oGraph.FillRouteMetadata();

		if(m_bInterfaceLoaded){
			if(m_oGraph.IsEmpty()){
				if(IsRoutingDataExist()){
					LoadInterface();
				}
			}
			UpdateUI();
		}
    }

	protected void 	onSelectDepatrure(){
	    Intent intent = new Intent(this, SelectStationActivity.class);
	    Bundle bundle = new Bundle();
	    bundle.putInt(BUNDLE_EVENTSRC_KEY, DEPARTURE_RESULT);
        //bundle.putSerializable(BUNDLE_STATIONMAP_KEY, (Serializable) mmoStations);
        bundle.putBoolean(BUNDLE_ENTRANCE_KEY, true);
        bundle.putInt(BUNDLE_STATIONID_KEY, m_nDepartureStationId);
        bundle.putInt(BUNDLE_PORTALID_KEY, m_nDeparturePortalId);
	    intent.putExtras(bundle);
	    startActivityForResult(intent, DEPARTURE_RESULT);
	}

	protected void 	onSelectArrival(){
	    Intent intent = new Intent(this, SelectStationActivity.class);
	    Bundle bundle = new Bundle();
	    bundle.putInt(BUNDLE_EVENTSRC_KEY, ARRIVAL_RESULT);
        //bundle.putSerializable(BUNDLE_STATIONMAP_KEY, (Serializable) mmoStations);
        bundle.putBoolean(BUNDLE_ENTRANCE_KEY, false);
        bundle.putInt(BUNDLE_STATIONID_KEY, m_nArrivalStationId);
        bundle.putInt(BUNDLE_PORTALID_KEY, m_nArrivalPortalId);
	    intent.putExtras(bundle);
	    startActivityForResult(intent, ARRIVAL_RESULT);
	}

	@Override
	  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    /*if (resultCode != RESULT_OK) {
	    	return;
	    }*/

    	int nStationId = -1;
    	int nPortalId = -1;
        boolean isCityChanged = false;

    	if(data != null) {
            nStationId = data.getIntExtra(BUNDLE_STATIONID_KEY, -1);
            nPortalId = data.getIntExtra(BUNDLE_PORTALID_KEY, -1);
            isCityChanged = data.getBooleanExtra(BUNDLE_CITY_CHANGED, false);
        } else {
            switch (requestCode) {
                case DEPARTURE_RESULT:
                    nStationId = m_nDepartureStationId;
                    nPortalId = m_nDeparturePortalId;
                    break;
                case ARRIVAL_RESULT:
                    nStationId = m_nArrivalStationId;
                    nPortalId = m_nArrivalPortalId;
                    break;
            }
        }

        final SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();

	    switch(requestCode) {
	    case DEPARTURE_RESULT:
	       	m_nDepartureStationId = nStationId;
	    	m_nDeparturePortalId = nPortalId;

            if (isCityChanged && nStationId != -1)
                m_nArrivalPortalId = m_nArrivalStationId = -1;

                    break;
	    case ARRIVAL_RESULT:
	    	m_nArrivalStationId = nStationId;
	    	m_nArrivalPortalId = nPortalId;

            if (isCityChanged && nStationId != -1)
                m_nDeparturePortalId = m_nDepartureStationId = -1;

            break;
	    case PREF_RESULT:
	    	break;
    	default:
    		break;
	    }

        if (isCityChanged) {
            if (nStationId == -1)
                m_nArrivalPortalId = m_nDeparturePortalId = m_nDepartureStationId = m_nArrivalStationId = -1;

            clearRecent(PreferenceManager.getDefaultSharedPreferences(this));
        }

        edit.putInt("dep_"+BUNDLE_STATIONID_KEY, m_nDepartureStationId);
        edit.putInt("dep_"+BUNDLE_PORTALID_KEY, m_nDeparturePortalId);
        edit.putInt("arr_"+BUNDLE_STATIONID_KEY, m_nArrivalStationId);
        edit.putInt("arr_"+BUNDLE_PORTALID_KEY, m_nArrivalPortalId);

	    edit.apply();

	    if (m_bInterfaceLoaded)
	    	UpdateUI();
        else
	    	LoadInterface();
	}

	protected void UpdateUI(){
		if(m_oGraph.HasStations()){
	    	StationItem dep_sit = m_oGraph.GetStation(m_nDepartureStationId);

	    	if(dep_sit != null && m_laListButtons != null){
                m_laListButtons.setFromStation(dep_sit);
                m_laListButtons.setFromPortal(m_nDeparturePortalId);

	    		PortalItem pit = dep_sit.GetPortal(m_nDeparturePortalId);

	    		if(pit == null)
                    m_nDeparturePortalId = -1;
	    	} else {
                m_laListButtons.setFromStation(null);
                m_laListButtons.setFromPortal(0);
	    		m_nDepartureStationId = -1;
	    	}

	    	StationItem arr_sit = m_oGraph.GetStation(m_nArrivalStationId);

	    	if(arr_sit != null && m_laListButtons != null){
                m_laListButtons.setToStation(arr_sit);
                m_laListButtons.setToPortal(m_nArrivalPortalId);

	    		PortalItem pit = arr_sit.GetPortal(m_nArrivalPortalId);

	    		if(pit == null)
                    m_nArrivalPortalId = -1;
	    	} else {
                m_laListButtons.setToStation(null);
                m_laListButtons.setToPortal(0);
	    		m_nArrivalStationId = -1;
	    	}
		}

	    if(m_nDepartureStationId != m_nArrivalStationId && m_nDepartureStationId != -1 && m_nArrivalStationId != -1 && m_nDeparturePortalId != -1 && m_nArrivalPortalId != -1){
	    	if(m_oSearchButton != null)
	    		m_oSearchButton.setEnabled(true);
    		if(m_oSearchMenuItem != null)
	    		m_oSearchMenuItem.setEnabled(true);
	    }
	    else{
	    	if(m_oSearchButton != null)
	    		m_oSearchButton.setEnabled(false);
	    	if(m_oSearchMenuItem != null)
	    		m_oSearchMenuItem.setEnabled(false);
	    }

	    if(m_laListButtons != null)
	    	m_laListButtons.notifyDataSetChanged();

        setLimitationsColor(getLimitationsColor());
	}

	protected void onSearch(){

		final ProgressDialog progressDialog = new ProgressDialog(this);
		progressDialog.setCancelable(false);
		progressDialog.setIndeterminate(true);
		progressDialog.setMessage(getString(R.string.sSearching));
		progressDialog.show();

		new Thread() {

			public void run() {

				//BellmanFordShortestPath
				/*List<DefaultWeightedEdge> path = BellmanFordShortestPath.findPathBetween(mGraph, stFrom.getId(), stTo.getId());
				if(path != null){
					for(DefaultWeightedEdge edge : path) {
		                	Log.d("Route", mmoStations.get(mGraph.getEdgeSource(edge)) + " - " + mmoStations.get(mGraph.getEdgeTarget(edge)) + " " + edge);
		                }
				}*/
				//DijkstraShortestPath
				/*List<DefaultWeightedEdge> path = DijkstraShortestPath.findPathBetween(mGraph, stFrom.getId(), stTo.getId());
				if(path != null){
					for(DefaultWeightedEdge edge : path) {
		                	Log.d("Route", mmoStations.get(mGraph.getEdgeSource(edge)) + " - " + mmoStations.get(mGraph.getEdgeTarget(edge)) + " " + edge);
		                }
				}*/
		        //KShortestPaths
				/*
				KShortestPaths<Integer, DefaultWeightedEdge> kPaths = new KShortestPaths<Integer, DefaultWeightedEdge>(mGraph, stFrom.getId(), 2);
		        List<GraphPath<Integer, DefaultWeightedEdge>> paths = null;
		        try {
		            paths = kPaths.getPaths(stTo.getId());
		            for (GraphPath<Integer, DefaultWeightedEdge> path : paths) {
		                for (DefaultWeightedEdge edge : path.getEdgeList()) {
		                	Log.d("Route", mmoStations.get(mGraph.getEdgeSource(edge)) + " - " + mmoStations.get(mGraph.getEdgeTarget(edge)) + " " + edge);
		                }
		                Log.d("Route", "Weight: " + path.getWeight());
		            }
		        } catch (IllegalArgumentException e) {
		        	e.printStackTrace();
		        }*/

				//YenTopKShortestPaths

			    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			    int nMaxRouteCount = prefs.getInt(PreferencesActivity.KEY_PREF_MAX_ROUTE_COUNT, 3);

				List<Path> shortest_paths_list = m_oGraph.GetShortestPaths(m_nDepartureStationId, m_nArrivalStationId, nMaxRouteCount);

				if(shortest_paths_list.size() == 0){
					//MainActivity.this.ErrMessage(R.string.sCannotGetPath);
					//Toast.makeText(MainActivity.this, R.string.sCannotGetPath, Toast.LENGTH_SHORT).show();
					Log.d(TAG, MainActivity.this.getString(R.string.sCannotGetPath));
				}
				else {
			        Intent intentView = new Intent(MainActivity.this, com.nextgis.metroaccess.StationListView.class);
			        //intentView.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

			        int nCounter = 0;
			        Bundle bundle = new Bundle();
			        bundle.putInt("dep_" + BUNDLE_PORTALID_KEY, m_nDeparturePortalId);
			        bundle.putInt("arr_" + BUNDLE_PORTALID_KEY, m_nArrivalPortalId);

			        for (Path path : shortest_paths_list) {
						ArrayList<Integer> IndexPath = new  ArrayList<>();
						Log.d(TAG, "Route# " + nCounter);
			            for (BaseVertex v : path.get_vertices()) {
			            	IndexPath.add(v.get_id());
			            	Log.d(TAG, "<" + m_oGraph.GetStation(v.get_id()));
			            }
			            intentView.putIntegerArrayListExtra(BUNDLE_PATH_KEY + nCounter, IndexPath);
			            nCounter++;
			        }

			        bundle.putInt(BUNDLE_PATHCOUNT_KEY, nCounter);
			        //bundle.putSerializable(BUNDLE_STATIONMAP_KEY, (Serializable) mmoStations);
			        //bundle.putSerializable(BUNDLE_CROSSESMAP_KEY, (Serializable) mmoCrosses);

					intentView.putExtras(bundle);

			        MainActivity.this.startActivity(intentView);

				}

				progressDialog.dismiss();

			}

		}.start();

	}

	public static String GetDownloadURL(){
		return m_sUrl;
	}

	public static String GetRouteDataDir(){
		return ROUTE_DATA_DIR;
	}

	public static String GetMetaFileName(){
		return META;
	}

	public static String GetRemoteMetaFile(){
		return REMOTE_METAFILE;
	}

	public static MAGraph GetGraph(){
        if (m_oGraph == null)
            m_oGraph = Analytics.getGraph();

		return m_oGraph;
	}

	public static void SetDownloadURL(String sURL){
		m_sUrl = sURL;
	}

	public void ErrMessage(String sErrMsg){
		Toast.makeText(this, sErrMsg, Toast.LENGTH_SHORT).show();
	}

	public void ErrMessage(int nErrMsg){
		Toast.makeText(this, getString(nErrMsg), Toast.LENGTH_SHORT).show();
	}

    /**
     * Get bitmap from SVG file
     *
     * @param path  Path to SVG file
     * @return      Bitmap
     */
    public static Bitmap getBitmapFromSVG(String path) {
        File svgFile = new File(path);
        SVG svg = null;

        try {
            FileInputStream is = new FileInputStream(svgFile);
            svg = SVG.getFromInputStream(is);
        } catch (FileNotFoundException | SVGParseException e) {
            e.printStackTrace();
        }

        return getBitmapFromSVG(svg, Color.TRANSPARENT);
    }

    /**
     * Get bitmap from SVG resource file
     *
     * @param context   Current context
     * @param id        SVG resource id
     * @return          Bitmap
     */
    public static Bitmap getBitmapFromSVG(Context context, int id) {
        return getBitmapFromSVG(context, id, Color.TRANSPARENT);
    }

    /**
     * Get bitmap from SVG resource file with proper station icon and color overlay
     *
     * @param context   Current context
     * @param id        SVG resource id
     * @param color     String color to overlay
     * @return          Bitmap
     */
    public static Bitmap getBitmapFromSVG(Context context, int id, String color) {
        Bitmap bitmap = null;

        if (color != null) {
            int c = Color.parseColor(color);
            bitmap = getBitmapFromSVG(context, id, c);
        }

        return bitmap;
    }

    /**
     * Get bitmap from SVG resource file with color overlay
     *
     * @param context   Current context
     * @param id        SVG resource id
     * @param color     Color to overlay
     * @return          Bitmap
     */
    public static Bitmap getBitmapFromSVG(Context context, int id, int color) {
        SVG svg = null;

        try {
            svg = SVG.getFromResource(context, id);
        } catch (SVGParseException e) {
            e.printStackTrace();
        }

        return getBitmapFromSVG(svg, color);
    }

    /**
     * Get bitmap from SVG with color overlay
     *
     * @param svg       SVG object
     * @param color     Color to overlay. Color.TRANSPARENT = no overlay
     * @return          Bitmap
     */
    public static Bitmap getBitmapFromSVG(SVG svg, int color) {
        Bitmap bitmap = null;

        if (svg != null && svg.getDocumentWidth() != -1) {
            PictureDrawable pd = new PictureDrawable(svg.renderToPicture());
            bitmap = Bitmap.createBitmap(pd.getIntrinsicWidth(), pd.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawPicture(pd.getPicture());

            if (color != Color.TRANSPARENT) {   // overlay color
                Paint p = new Paint();
                ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                p.setColorFilter(filter);

                canvas = new Canvas(bitmap);
                canvas.drawBitmap(bitmap, 0, 0, p);
            }
        }

        return bitmap;
    }

    /**
     * Get bitmap from SVG resource file with proper route item icon and color overlay
     *
     * @param context   Current context
     * @param entry     RouteItem to get it's color and icon type
     * @param subItem   SubItem = _8.svg / x8.png
     * @return          Bitmap
     */
    public static Bitmap getBitmapFromSVG(Context context, RouteItem entry, boolean subItem) {
        String color = MainActivity.GetGraph().GetLineColor(entry.GetLine());
        Bitmap bitmap = null;
        int type = subItem ? 8 : entry.GetType();

        if (color != null) {
            int c = Color.parseColor(color);
            bitmap = getBitmapFromSVG(context, ICONS_RAW[type], c);
        }

        if (type == 6 || type == 7)
            bitmap = getBitmapFromSVG(MainActivity.GetGraph().GetCurrentRouteDataPath() + "/icons/metro.svg");

        return bitmap;
    }

    public static void tintIcons(Menu menu, Context context) {
        MenuItem item;
        Drawable tintIcon;
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
//        int color = typedValue.data;
        int color = Color.WHITE;
        int colorDark = Color.BLACK;

        for(int i = 0; i < menu.size(); i++) {
            item = menu.getItem(i);
            tintIcon = item.getIcon();

            if (tintIcon != null) {
                Bitmap bitmap = Bitmap.createBitmap(tintIcon.getIntrinsicWidth(), tintIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);

                tintIcon.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                tintIcon.draw(canvas);

                Paint p = new Paint();
                ColorFilter filter = new PorterDuffColorFilter(colorDark, PorterDuff.Mode.SRC_ATOP);
                p.setColorFilter(filter);
                canvas.drawBitmap(bitmap, 0, 0, p);
                canvas.drawBitmap(bitmap, 0, 0, p);
//                canvas.drawBitmap(bitmap, 0, 0, p);
                filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                p.setColorFilter(filter);
                canvas.drawBitmap(bitmap, 0, 0, p);

//                tintIcon.mutate().setColorFilter(colorDark, PorterDuff.Mode.SRC_ATOP);
                item.setIcon(new BitmapDrawable(context.getResources(), bitmap));
            }
        }
    }
}
