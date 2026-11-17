package com.sloth.runveal.core.map.internal.maplibre

import com.sloth.runveal.map.LatLon
import org.maplibre.android.geometry.LatLng

internal fun LatLon.toMapLibreLatLng(): LatLng = LatLng(lat, lon)
