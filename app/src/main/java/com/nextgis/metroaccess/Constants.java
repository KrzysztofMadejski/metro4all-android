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


public interface Constants {
//    public final static int[] ICONS_RAW = { R.raw._0, R.raw._1, R.raw._2, R.raw._3, R.raw._4, R.raw._5, R.raw._6, R.raw._7, R.raw._8, R.raw._9 };
    public final static int[] ICONS_RAW = { R.raw._0, R.raw._5, R.raw._5, R.raw._3, R.raw._4, R.raw._5, R.raw._6, R.raw._7, R.raw._8, R.raw._9 };

    public final static String TAG = "metro4all";

    public final static String META = "meta.json";
    final static String REMOTE_METAFILE = "remotemeta_v2.3.json";
    final static String ROUTE_DATA_DIR = "rdata_v2.3";
    final static String APP_VERSION = "app_version";
    final static String APP_REPORTS_DIR = "reports";
    final static String APP_REPORTS_PHOTOS_DIR = "Metro4All";
    final static String APP_REPORTS_SCREENSHOT = "screenshot.jpg";

    public final static String CSV_CHAR = ";";

    final static String BUNDLE_MSG_KEY = "msg";
    final static String BUNDLE_PAYLOAD_KEY = "json";
    final static String BUNDLE_ERRORMARK_KEY = "error";
    final static String BUNDLE_EVENTSRC_KEY = "eventsrc";
    final static String BUNDLE_ENTRANCE_KEY = "in";
    final static String BUNDLE_PATHCOUNT_KEY = "pathcount";
    final static String BUNDLE_PATH_KEY = "path_";
    final static String BUNDLE_STATIONMAP_KEY = "stationmap";
    final static String BUNDLE_CROSSESMAP_KEY = "crossmap";
    final static String BUNDLE_STATIONID_KEY = "stationid";
    final static String BUNDLE_PORTALID_KEY = "portalid";
    final static String BUNDLE_METAMAP_KEY = "metamap";
    final static String BUNDLE_CITY_CHANGED = "city_changed";
    final static String BUNDLE_IMG_X = "coord_x";
    final static String BUNDLE_IMG_Y = "coord_y";

    public final static int DEPARTURE_RESULT = 1;
    public final static int ARRIVAL_RESULT = 2;
    public final static int PREF_RESULT = 3;
    public final static int SUBSCREEN_PORTAL_RESULT = 4;
    public final static int DEFINE_AREA_RESULT = 5;
    public final static int CAMERA_REQUEST = 6;
    public final static int PICK_REQUEST = 7;
    public final static int MAX_RECENT_ITEMS = 10;

    public final static String PARAM_PORTAL_DIRECTION = "PORTAL_DIRECTION";
    public final static String PARAM_SCHEME_PATH = "image_path";
    public final static String PARAM_ROOT_ACTIVITY = "root_activity";
    public final static String PARAM_ACTIVITY_FOR_RESULT = "NEED_RESULT";
    public final static String PARAM_DEFINE_AREA = "define_area";

    public static final String KEY_PREF_RECENT_DEP_STATIONS = "recent_dep_stations";
    public static final String KEY_PREF_RECENT_ARR_STATIONS = "recent_arr_stations";
    public static final String KEY_PREF_TOOLTIPS = "tooltips_showed";

    public final int STATUS_INTERRUPT_LOCATING = 0;
    public final int STATUS_FINISH_LOCATING = 1;
    public final int LOCATING_TIMEOUT = 15000;
}
