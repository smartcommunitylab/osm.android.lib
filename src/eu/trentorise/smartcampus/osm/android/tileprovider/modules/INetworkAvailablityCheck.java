package eu.trentorise.smartcampus.osm.android.tileprovider.modules;

public interface INetworkAvailablityCheck {

	boolean getNetworkAvailable();

	boolean getWiFiNetworkAvailable();

	boolean getCellularDataNetworkAvailable();

	boolean getRouteToPathExists(int hostAddress);
}