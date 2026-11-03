package com.swyp.runveal.core.map.internal.maplibre

import com.swyp.runveal.map.LatLon
import org.maplibre.android.geometry.LatLng

internal fun LatLon.toMapLibreLatLng(): LatLng = LatLng(lat, lon)
