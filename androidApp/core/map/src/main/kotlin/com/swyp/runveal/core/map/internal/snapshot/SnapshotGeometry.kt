package com.swyp.runveal.core.map.internal.snapshot

import com.swyp.runveal.map.snapshot.GeoBounds
import org.maplibre.android.geometry.LatLngBounds

internal fun GeoBounds.toLatLngBounds(): LatLngBounds = LatLngBounds.from(north, east, south, west)
