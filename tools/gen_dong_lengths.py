"""Generate a *temporary estimate* of total road length per dong (adm_cd2 -> meters).

The real denominator (actual total road length per dong) is planned to come from a server
API that doesn't exist yet (docs/local-db/local-db-schema-design.md SS3.3). Until then, this
approximates it as polygon area x a single road-density constant, so the dong/sigungu/sido
completion-rate UI shows plausible non-zero numbers instead of a hardcoded 0%. Replace the
output file (and DongAssetRepository/TrackingService's loader) once the real server data
exists.

Usage:
  python3 gen_dong_lengths.py <korea_dong.geojson> <out.json> [--density 8000]
"""
import argparse
import json
import math

EARTH_RADIUS_M = 6371000.0


def ring_area_m2(ring, ref_lat_rad):
    """Lat/lon ring (degrees) -> local equirectangular projection (centered on ref_lat) -> shoelace."""
    meters_per_deg_lat = EARTH_RADIUS_M * math.pi / 180.0
    meters_per_deg_lon = meters_per_deg_lat * math.cos(ref_lat_rad)
    pts = [(lon * meters_per_deg_lon, lat * meters_per_deg_lat) for lon, lat in ring]
    area = 0.0
    n = len(pts)
    for i in range(n):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % n]
        area += x1 * y2 - x2 * y1
    return abs(area) / 2.0


def polygon_area_m2(polygon_coords):
    """polygon_coords = [exterior_ring, hole1, hole2, ...]; holes are subtracted."""
    if not polygon_coords:
        return 0.0
    exterior = polygon_coords[0]
    ref_lat_rad = math.radians(sum(p[1] for p in exterior) / len(exterior))
    area = ring_area_m2(exterior, ref_lat_rad)
    for hole in polygon_coords[1:]:
        area -= ring_area_m2(hole, ref_lat_rad)
    return max(area, 0.0)


def feature_area_m2(geometry):
    if geometry["type"] == "Polygon":
        return polygon_area_m2(geometry["coordinates"])
    if geometry["type"] == "MultiPolygon":
        return sum(polygon_area_m2(poly) for poly in geometry["coordinates"])
    return 0.0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("boundary_geojson")
    parser.add_argument("out_json")
    parser.add_argument("--density", type=float, default=8000.0, help="meters of road per km^2 (default: Seoul-ish urban density)")
    args = parser.parse_args()

    with open(args.boundary_geojson, encoding="utf-8") as f:
        data = json.load(f)

    result = {}
    for feature in data["features"]:
        adm_cd2 = feature["properties"]["adm_cd2"]
        area_km2 = feature_area_m2(feature["geometry"]) / 1_000_000.0
        result[adm_cd2] = round(area_km2 * args.density, 1)

    with open(args.out_json, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, separators=(",", ":"))

    lengths = sorted(result.values())
    print(f"dongs: {len(result)}")
    print(f"length range: {lengths[0]:.0f}m ~ {lengths[-1]:.0f}m, median: {lengths[len(lengths)//2]:.0f}m")


if __name__ == "__main__":
    main()
