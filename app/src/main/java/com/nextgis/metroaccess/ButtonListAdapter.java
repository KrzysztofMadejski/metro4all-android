/******************************************************************************
 * Project:  Metro4All
 * Purpose:  Routing in subway.
 * Authors:  Dmitry Baryshnikov (polimax@mail.ru), Stanislav Petriakov
 ******************************************************************************
 *   Copyright (C) 2014,2015 NextGIS
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.nextgis.metroaccess.data.PortalItem;
import com.nextgis.metroaccess.data.StationItem;

import java.io.File;

import static com.nextgis.metroaccess.Constants.ARRIVAL_RESULT;
import static com.nextgis.metroaccess.Constants.BUNDLE_PORTALID_KEY;
import static com.nextgis.metroaccess.Constants.BUNDLE_STATIONID_KEY;
import static com.nextgis.metroaccess.Constants.DEPARTURE_RESULT;
import static com.nextgis.metroaccess.Constants.PARAM_PORTAL_DIRECTION;
import static com.nextgis.metroaccess.Constants.PARAM_ROOT_ACTIVITY;
import static com.nextgis.metroaccess.Constants.PARAM_SCHEME_PATH;
import static com.nextgis.metroaccess.Constants.SUBSCREEN_PORTAL_RESULT;
import static com.nextgis.metroaccess.MainActivity.getBitmapFromSVG;

public class ButtonListAdapter extends BaseAdapter {

    protected Context m_oContext;
    protected LayoutInflater m_oInfalInflater;
    protected StationItem nullStation, fromStation, toStation;
    protected PortalItem fromPortal, toPortal;

    public ButtonListAdapter(Context c) {
        this.m_oContext = c;
        this.m_oInfalInflater = (LayoutInflater) m_oContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        fromStation = toStation = nullStation = new StationItem(-1, m_oContext.getString(R.string.sStationName) + ": " + m_oContext.getString(R.string.sNotSet), -1, -1, -1, -1, -1, -1);
        nullPortals(true, true);
    }

    @Override
    public int getCount() {
        return 2;//TODO: add "set my conditions" and "set from map"
    }

    @Override
    public Object getItem(int arg0) {
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        switch (position) {
            case 0://create from pane
                return CreatePane(convertView, parent, true, fromStation, fromPortal);
            case 1://create to pane
                return CreatePane(convertView, parent, false, toStation, toPortal);
            case 2://create from map pane
//                return CreateAdds(convertView, (String) m_oContext.getResources().getText(R.string.sLimits));
            case 3://create conditions pane
                break;
        }
        return null;
    }

    private View CreatePane(View convertView, final ViewGroup parent, final boolean isFromPane, final StationItem station, final PortalItem portal) {
        if (convertView == null) {
            convertView = m_oInfalInflater.inflate(R.layout.fromto_layout, parent, false);
        }

        final int paneTitle, requestCode;
        final String gaPane;

        if (isFromPane) {
            paneTitle = R.string.sFromStation;
            gaPane = Analytics.FROM;
//            requestCode = PORTAL_MAP_MAIN_FROM_RESULT;
            requestCode = DEPARTURE_RESULT;
        } else {
            paneTitle = R.string.sToStation;
            gaPane = Analytics.TO;
//            requestCode = PORTAL_MAP_MAIN_TO_RESULT;
            requestCode = ARRIVAL_RESULT;
        }

        // set map button
        final ImageView ibtnMenu = (ImageView) convertView.findViewById(R.id.ibtnMenu);
        ImageView ivMetroIconLeft = (ImageView) convertView.findViewById(R.id.ivMetroIconLeft);
        ImageView ivMetroIconRight = (ImageView) convertView.findViewById(R.id.ivMetroIconRight);
        ImageView ivSmallIcon = (ImageView) convertView.findViewById(R.id.ivSmallIcon);

        File schemaFile = new File(MainActivity.GetGraph().GetCurrentRouteDataPath() + "/schemes", "" + station.GetNode() + ".png");
        final Bundle bundle = new Bundle();
        bundle.putInt(BUNDLE_STATIONID_KEY, station.GetId());
        bundle.putInt(BUNDLE_PORTALID_KEY, portal.GetId());
        bundle.putBoolean(PARAM_PORTAL_DIRECTION, isFromPane);
        bundle.putBoolean(PARAM_ROOT_ACTIVITY, true);
        bundle.putString(PARAM_SCHEME_PATH, schemaFile.getPath());

        if (station != nullStation) {
            ibtnMenu.setVisibility(View.VISIBLE);
            ibtnMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LayoutInflater mInflater = (LayoutInflater) m_oContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View layout = mInflater.inflate(R.layout.stationlist_popup_menu, parent, false);

                    final TextView itemMap = (TextView) layout.findViewById(R.id.tvMap);
                    final TextView itemLayout = (TextView) layout.findViewById(R.id.tvLayout);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    // fix for items full-width background fill
                    if (itemMap.getLayoutParams().width > itemLayout.getLayoutParams().width)
                        itemLayout.setLayoutParams(lp);
                    else
                        itemMap.setLayoutParams(lp);

                    layout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

                    final PopupWindow mDropdown = new PopupWindow(layout, itemLayout.getMeasuredWidth(),
                            itemMap.getMeasuredHeight() + itemLayout.getMeasuredHeight(), true);
                    mDropdown.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    Drawable background = m_oContext.getResources().getDrawable(R.drawable.abc_popup_background_mtrl_mult);
                    mDropdown.setBackgroundDrawable(background);
                    mDropdown.setClippingEnabled(false);

                    itemMap.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((Analytics) ((Activity) m_oContext).getApplication()).addEvent(Analytics.SCREEN_MAIN, Analytics.BTN_MAP, gaPane + " " + Analytics.PANE);

                            mDropdown.dismiss();
                            Intent intent = new Intent(m_oContext, StationMapActivity.class);
                            intent.putExtras(bundle);

                            Activity parent = (Activity) m_oContext;
                            parent.startActivityForResult(intent, requestCode);
                        }
                    });

                    itemLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((Analytics) ((Activity) m_oContext).getApplication()).addEvent(Analytics.SCREEN_MAIN, Analytics.BTN_LAYOUT, gaPane + " " + Analytics.PANE);

                            mDropdown.dismiss();
                            Intent intent = new Intent(m_oContext, StationImageView.class);
                            intent.putExtras(bundle);

                            Activity parent = (Activity) m_oContext;
                            parent.startActivityForResult(intent, requestCode);
                        }
                    });

                    mDropdown.showAsDropDown(ibtnMenu, (int) (-itemLayout.getMeasuredWidth() * 0.75), -30);
                }
            });

            // set selected line icon, entrance metro icon and arrow icon
            Bitmap metroIcon = getBitmapFromSVG(MainActivity.GetGraph().GetCurrentRouteDataPath() + "/icons/metro.svg");
            Bitmap arrowIcon = getBitmapFromSVG(m_oContext, R.raw.arrow, m_oContext.getResources().getColor(R.color.grey_dark));
            String color = MainActivity.GetGraph().GetLineColor(station.GetLine());
            Bitmap lineIcon = getBitmapFromSVG(m_oContext, R.raw._0, color);

            if (isFromPane) {   // from pane > rotate arrow 180 degree
                Matrix matrix = new Matrix();
                matrix.postRotate(180);
                arrowIcon = Bitmap.createBitmap(arrowIcon, 0, 0, arrowIcon.getWidth(), arrowIcon.getHeight(), matrix, true);
            }

            setImageToImageView(ivMetroIconLeft, metroIcon);
            setImageToImageView(ivMetroIconRight, arrowIcon);
            setImageToImageView(ivSmallIcon, lineIcon);
        } else {    // hide all icons if statiton is not selected
            ibtnMenu.setVisibility(View.GONE);
            ivMetroIconLeft.setVisibility(View.GONE);
            ivMetroIconRight.setVisibility(View.GONE);
            ivSmallIcon.setVisibility(View.GONE);
        }

        // set texts
        TextView tvPaneName = (TextView) convertView.findViewById(R.id.tvPaneName);
        tvPaneName.setText(paneTitle);

        String sStationName = station.GetName();
        TextView tvStationName = (TextView) convertView.findViewById(R.id.tvStationName);
        tvStationName.setText(sStationName);

        String sEntranceName = portal.GetReadableMeetCode().equals("") ? portal.GetName() : portal.GetReadableMeetCode() + ": " + portal.GetName();
        TextView tvEntranceName = (TextView) convertView.findViewById(R.id.tvEntranceName);
        tvEntranceName.setText(sEntranceName);

        return convertView;
    }

    private void setImageToImageView(ImageView view, Bitmap bitmap) {
        if (bitmap != null) {
            view.setImageBitmap(bitmap);
            view.setVisibility(View.VISIBLE);
        } else
            view.setVisibility(View.GONE);
    }

    public void setFromStation(StationItem fromStation) {
        if (fromStation != null)
            this.fromStation = fromStation;
        else
            this.fromStation = nullStation;
    }

    public void setToStation(StationItem toStation) {
        if (toStation != null)
            this.toStation = toStation;
        else
            this.toStation = nullStation;
    }

    public void setFromPortal(int portalId) {
        if (fromStation != nullStation) {
            PortalItem newPortal = fromStation.GetPortal(portalId);

            if (newPortal != null)
                fromPortal = newPortal;
        }
        else
            nullPortals(true, false);
    }

    public void setToPortal(int portalId) {
        if (toStation != nullStation) {
            PortalItem newPortal = toStation.GetPortal(portalId);

            if (newPortal != null)
                toPortal = newPortal;
        }
        else
            nullPortals(false, true);
    }

    public void clear() {
        fromStation = toStation = nullStation;
        nullPortals(true, true);

        notifyDataSetChanged();
    }

    private void nullPortals(boolean from, boolean to) {
        if (from)
            fromPortal = new PortalItem(-1, m_oContext.getString(R.string.sEntranceName) + ": " + m_oContext.getString(R.string.sNotSet), -1, -1, null, -1, -1, -1);

        if (to)
            toPortal = new PortalItem(-1, m_oContext.getString(R.string.sExitName) + ": " + m_oContext.getString(R.string.sNotSet), -1, -1, null, -1, -1, -1);
    }
}
