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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.nextgis.metroaccess.data.DownloadData;
import com.nextgis.metroaccess.data.GraphDataItem;
import com.nextgis.metroaccess.data.MAGraph;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.nextgis.metroaccess.Constants.APP_REPORTS_DIR;
import static com.nextgis.metroaccess.Constants.APP_REPORTS_SCREENSHOT;
import static com.nextgis.metroaccess.Constants.BUNDLE_CITY_CHANGED;
import static com.nextgis.metroaccess.Constants.BUNDLE_ERRORMARK_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_EVENTSRC_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_MSG_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_PAYLOAD_KEY;
import static com.nextgis.metroaccess.Constants.KEY_PREF_RECENT_ARR_STATIONS;
import static com.nextgis.metroaccess.Constants.KEY_PREF_RECENT_DEP_STATIONS;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_CAT_DATA = "data_cat";

	public static final String KEY_PREF_USER_TYPE = "user_type";
	public static final String KEY_PREF_DOWNLOAD_PATH = "download_path";
	public static final String KEY_PREF_UPDROUTEDATA = "update_route_data";
	public static final String KEY_PREF_CHANGE_CITY_BASES = "change_city_bases";
	public static final String KEY_PREF_DATA_LOCALE = "data_loc";
    public static final String KEY_PREF_LEGEND = "legend";
	public static final String KEY_PREF_CITY = "city";
	public static final String KEY_PREF_CITYLANG = "city_lang";
	public static final String KEY_PREF_MAX_ROUTE_COUNT = "max_route_count";
	public static final String KEY_PREF_GA = "ga_enabled";
	public static final String KEY_PREF_REPORT_WIFI = "reports_wifi";

	protected List<DownloadData> m_asDownloadData;
	protected static Handler m_oGetJSONHandler; 
	
	protected ListPreference m_CityLangPref;
	protected ListPreference m_CityPref;

    private Toolbar mActionBar;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        ActionBar ab = getSupportActionBar();
//        ab.setDisplayHomeAsUpEnabled(true);

        m_asDownloadData = new ArrayList<DownloadData>();
		
		m_oGetJSONHandler = new Handler() {
            public void handleMessage(Message msg) {
            	super.handleMessage(msg);
            	
            	Bundle resultData = msg.getData();
            	boolean bHaveErr = resultData.getBoolean(BUNDLE_ERRORMARK_KEY);
            	int nEventSource = resultData.getInt(BUNDLE_EVENTSRC_KEY);
            	String sPayload = resultData.getString(BUNDLE_PAYLOAD_KEY);
            	if(bHaveErr){
            		Toast.makeText(PreferencesActivity.this, resultData.getString(BUNDLE_MSG_KEY), Toast.LENGTH_LONG).show();
            	}

            	switch(nEventSource){
            	case 2:
            		OnDownloadData();
            		break;
        		default:
        			break;
            	}
            }
        };
        
        addPreferencesFromResource(R.xml.preferences);

        mActionBar.setTitle(R.string.sSettings);

        PreferenceManager preferenceManager = getPreferenceManager();

	    //add button update data
	    PreferenceCategory targetCategory = (PreferenceCategory)findPreference(KEY_CAT_DATA);
	    
	    MAGraph oGraph = MainActivity.GetGraph();

        Preference legendPref = findPreference(KEY_PREF_LEGEND);
        if(legendPref != null){
            legendPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    ((Analytics) getApplication()).addEvent(Analytics.SCREEN_PREFERENCE, Analytics.LEGEND, Analytics.PREFERENCE);
                    Intent intentView = new Intent(getApplicationContext(), StationImageView.class);
                    startActivity(intentView);
                    return true;
                }
            });
        }

	    m_CityPref = (ListPreference) findPreference(KEY_PREF_CITY);
        if(m_CityPref != null){
        	UpdateCityList();
            int index = m_CityPref.findIndexOfValue( m_CityPref.getValue() );
            if(index >= 0){
            	m_CityPref.setSummary(m_CityPref.getEntries()[index]);
            }
        }

        m_CityLangPref = (ListPreference) findPreference(KEY_PREF_CITYLANG);
        if (m_CityLangPref != null) {
            int index = m_CityLangPref.findIndexOfValue(m_CityLangPref.getValue());

            if (index >= 0) {
                m_CityLangPref.setSummary(m_CityLangPref.getEntries()[index]);
            } else {
                String currCityLang = MainActivity.GetGraph().GetLocale();
                index = m_CityLangPref.findIndexOfValue(currCityLang);
                if (index < 0) {
                    index = 0;
                }
                m_CityLangPref.setValue((String) m_CityLangPref.getEntryValues()[index]);
                m_CityLangPref.setSummary(m_CityLangPref.getEntries()[index]);
            }
        }

	    Preference checkUpd = new Preference(this);
	    checkUpd.setKey(KEY_PREF_UPDROUTEDATA);
	    checkUpd.setTitle(R.string.sPrefUpdDataTitle);
//	    checkUpd.setSummary(R.string.sPrefUpdDataSummary);
	    checkUpd.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
                builder.setMessage(R.string.sAreYouSure)
                        .setTitle(R.string.sQuestion)
                        .setPositiveButton(R.string.sYes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this);
                                String sUrl = sharedPref.getString(KEY_PREF_DOWNLOAD_PATH, MainActivity.GetDownloadURL());

                                m_asDownloadData.clear();

                                MAGraph oGraph = MainActivity.GetGraph();

                                for (GraphDataItem oItem : oGraph.GetRouteMetadata().values()) {
                                    m_asDownloadData.add(new DownloadData(PreferencesActivity.this, oItem, sUrl + oItem.GetPath() + ".zip", m_oGetJSONHandler));
                                }

                                OnDownloadData();
                            }
                        })
                        .setNegativeButton(R.string.sNo, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });

                builder.create();
                builder.show();

                return true;
            }
        });

	    Preference changeCityBases = new Preference(this);
	    changeCityBases.setKey(KEY_PREF_CHANGE_CITY_BASES);
	    changeCityBases.setTitle(R.string.sPrefChangeCityBasesTitle);
//	    changeCityBases.setSummary(R.string.sPrefChangeCityBasesSummary);
	    changeCityBases.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        	public boolean onPreferenceClick(Preference preference) {
        		//Add and remove bases

        		MAGraph oGraph = MainActivity.GetGraph();

        		File oDataFolder = new File(getExternalFilesDir(""), MainActivity.GetRemoteMetaFile());
    			String sJSON = MainActivity.readFromFile(oDataFolder);
        		oGraph.OnUpdateMeta(sJSON, false);

        		final List<GraphDataItem> new_items = oGraph.HasChanges();
                Collections.sort(new_items);
        		final List<GraphDataItem> exist_items = new ArrayList<GraphDataItem>(oGraph.GetRouteMetadata().values());
                Collections.sort(exist_items);

        	    int count = new_items.size() + exist_items.size();
        	    if(count == 0)
        	    	return false;

        	    final boolean[] checkedItems = new boolean[count];
        	    final CharSequence[] checkedItemStrings = new CharSequence[count];

                for(int i = 0; i < exist_items.size(); i++){
                    checkedItems[i] = true;
                }

                for(int i = 0; i < exist_items.size(); i++){
                    checkedItemStrings[i] = exist_items.get(i).GetLocaleName();
                }

        	    for(int i = 0; i < new_items.size(); i++){
        	    	checkedItems[i + exist_items.size()] = false;
        	    }

        	    for(int i = 0; i < new_items.size(); i++){
        	    	checkedItemStrings[i + exist_items.size()] = new_items.get(i).GetFullName();
        	    }


        	    AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
        		builder.setTitle(R.string.sPrefChangeCityBasesTitle)
        			   .setMultiChoiceItems(checkedItemStrings, checkedItems,
        						new DialogInterface.OnMultiChoiceClickListener() {
        							@Override
        							public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        								checkedItems[which] = isChecked;
        							}
        						})
        				.setPositiveButton(R.string.sPrefChangeCityBasesBtn,
        						new DialogInterface.OnClickListener() {
        							@Override
        							public void onClick(DialogInterface dialog, int id) {

        								m_asDownloadData.clear();

        								for (int i = 0; i < checkedItems.length; i++) {
        									//check if no change
        									//1. item is unchecked and was unchecked
                                            //if(!checkedItems[i] && i < new_items.size()){
                                            if(!checkedItems[i] && i >= exist_items.size()){
        										continue;
        									}
                                            //else if(i < new_items.size()){
        									else if(i >= exist_items.size()){  // add
                                                //m_asDownloadData.add(new DownloadData(PreferencesActivity.this, new_items.get(i), MainActivity.GetDownloadURL() + new_items.get(i).GetPath() + ".zip", m_oGetJSONHandler));
        										m_asDownloadData.add(new DownloadData(PreferencesActivity.this, new_items.get(i - exist_items.size()), MainActivity.GetDownloadURL() + new_items.get(i - exist_items.size()).GetPath() + ".zip", m_oGetJSONHandler));
        									}
        									//2. item is checked and was checked
                                            //else if (checkedItems[i] && i >= new_items.size()){
                                            else if (checkedItems[i] && i < exist_items.size()){
        										continue;
        									}
        									else{//delete
        										//File oDataFolder = new File(getExternalFilesDir(MainActivity.GetRouteDataDir()), exist_items.get(i - new_items.size()).GetPath());
                                                File oDataFolder = new File(getExternalFilesDir(MainActivity.GetRouteDataDir()), exist_items.get(i).GetPath());
        										DeleteRecursive(oDataFolder);
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
				return true;
        	}
        });
	    
	    targetCategory.addPreference(changeCityBases);
        targetCategory.addPreference(checkUpd);

        Preference gaPreference = findPreference(KEY_PREF_GA);
        gaPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                final boolean isGAEnabled = (Boolean)o;

                if (isGAEnabled)
                    ((Analytics) getApplication()).addEvent(Analytics.SCREEN_PREFERENCE, "Enable GA", Analytics.PREFERENCE);

                ((Analytics) getApplication()).reload(isGAEnabled);

                return true;
            }
        });

        int reportsCount = 0;
        File file = new File(getExternalFilesDir(null), APP_REPORTS_DIR);
        if (file.exists() && file.isDirectory())
            for (File report : file.listFiles())
                if (report.isFile() && !report.getName().equals(APP_REPORTS_SCREENSHOT))
                    reportsCount++;

        if (reportsCount > 0)
            findPreference(KEY_PREF_REPORT_WIFI).setSummary(reportsCount + " " + getString(R.string.sReportWaiting));
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.activity_preferences, new LinearLayout(this), false);

        mActionBar = (Toolbar) contentView.findViewById(R.id.action_bar);
        mActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ViewGroup contentWrapper = (ViewGroup) contentView.findViewById(R.id.content_wrapper);
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);

        getWindow().setContentView(contentView);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    } 
    
	@Override
	protected void onPause() {	
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);		
		sharedPref.unregisterOnSharedPreferenceChangeListener(this);
		
		super.onPause();
	}
	
    @Override
	public void onResume() {
        super.onResume();

 		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
 		sharedPref.registerOnSharedPreferenceChangeListener(this);
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		CharSequence newVal = "";
		Preference Pref = findPreference(key);

        if(key.equals(KEY_PREF_CITY)){
			newVal = sharedPreferences.getString(key, "msk");
			int nIndex = m_CityPref.findIndexOfValue((String) newVal);
            if(nIndex >= 0){
            	m_CityPref.setSummary(m_CityPref.getEntries()[nIndex]);
            }
            MainActivity.GetGraph().SetCurrentCity((String) newVal);    // FIXME wait until selected city is not loaded
            setResult(RESULT_OK, new Intent().putExtra(BUNDLE_CITY_CHANGED, true));
            return;
		}
		else if(key.equals(KEY_PREF_CITYLANG)){
			newVal = sharedPreferences.getString(key, "en");
			int nIndex = m_CityLangPref.findIndexOfValue((String) newVal);
            if(nIndex >= 0){
            	m_CityLangPref.setSummary((String) m_CityLangPref.getEntries()[nIndex]);
            }
            MainActivity.GetGraph().SetLocale((String) newVal);
            return;
			
		}
		/*else if(key.equals(KEY_PREF_USER_TYPE))
		{
			newVal = sharedPreferences.getString(key, "1");
        	String toIntStr = (String) newVal;
    		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    		int index = Integer.parseInt(toIntStr);
    		editor.putInt(key + "_int", index);
    		editor.commit();
    		
    		index--;           
            if(index >= 0){
            	mlsNaviType.setSummary((String) mlsNaviType.getEntries()[index]);
            }
        }*/
		else if(key.equals(KEY_PREF_DOWNLOAD_PATH)){
			String sUrl = sharedPreferences.getString(key, MainActivity.GetDownloadURL());			
    		if(sUrl.length() > 0){
            	Pref.setSummary(sUrl);	
            	MainActivity.SetDownloadURL(sUrl);
    		}
		}
	}

	public static void DeleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            DeleteRecursive(child);

	    fileOrDirectory.delete();
	}
	
	protected void OnDownloadData(){
		if(m_asDownloadData.isEmpty()){
			MAGraph oGraph = MainActivity.GetGraph();
			oGraph.FillRouteMetadata();
			UpdateCityList();

			return;
		}
		DownloadData data = m_asDownloadData.get(0);
		m_asDownloadData.remove(0);
		
		data.OnDownload();
	}
	
	protected void UpdateCityList(){
		MAGraph oGraph = MainActivity.GetGraph();
		Map<String, GraphDataItem> oRouteMetadata = MAGraph.sortByLocalNames(oGraph.GetRouteMetadata());
    	if(oRouteMetadata.size() > 0){
    		CharSequence[] ent = new CharSequence[oRouteMetadata.size()];
    		CharSequence[] ent_val = new CharSequence[oRouteMetadata.size()];
    		int nCounter = 0;
    		for (Map.Entry<String, GraphDataItem> entry : oRouteMetadata.entrySet()) {
				ent[nCounter] = entry.getValue().GetLocaleName();
				ent_val[nCounter] = entry.getKey();
				nCounter++;
			}

    		m_CityPref.setEntries(ent);
    		m_CityPref.setEntryValues(ent_val);

            int index = m_CityPref.findIndexOfValue(m_CityPref.getValue());
            if (index < 0)
                m_CityPref.setValue(oGraph.GetCurrentCity());

    		m_CityPref.setEnabled(true);
    	} else {
    		m_CityPref.setEnabled(false);
    	}
	}

    public static void clearRecent(SharedPreferences prefs) {
        prefs.edit().remove(KEY_PREF_RECENT_DEP_STATIONS).remove(KEY_PREF_RECENT_ARR_STATIONS).apply();
    }
}