package eu.trentorise.smartcampus.osm.android.api;
import eu.trentorise.smartcampus.osm.android.util.GeoPoint;

/**
 * An interface that resembles the Google Maps API GeoPoint class
 * and is implemented by the osmdroid {@link GeoPoint} class.
 *
 * @author Neil Boyd
 *
 */
public interface IGeoPoint {
	int getLatitudeE6();
	int getLongitudeE6();
}