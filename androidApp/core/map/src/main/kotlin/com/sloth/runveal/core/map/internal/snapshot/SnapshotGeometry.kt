package com.sloth.runveal.core.map.internal.snapshot

import com.sloth.runveal.map.snapshot.GeoBounds
import org.maplibre.android.geometry.LatLngBounds

internal fun GeoBounds.toLatLngBounds(): LatLngBounds = LatLngBounds.from(north, east, south, west)
