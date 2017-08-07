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

package com.joaquimley.birdsseyeview.utils;

import android.animation.TypeEvaluator;

import com.google.android.gms.maps.model.LatLng;

/**
 * Util class to calculate the camera position
 */

public class LatLngEvaluator implements TypeEvaluator<LatLng> {

    double mLatitude;
    double mLongitude;

    public LatLngEvaluator(LatLng startValue, LatLng endValue) {
        mLatitude = endValue.latitude - startValue.latitude;
        mLongitude = endValue.longitude - startValue.longitude;
    }

    @Override
    public LatLng evaluate(float fraction, LatLng startValue, com.google.android.gms.maps.model.LatLng
            endValue) {
        double lat = mLatitude * fraction + startValue.latitude;
        double lng = mLongitude * fraction + startValue.longitude;
        return new LatLng(lat, lng);
    }
}
