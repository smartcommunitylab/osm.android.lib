package eu.trentorise.smartcampus.osm.android.bonuspack.routing;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.util.Log;
import eu.trentorise.smartcampus.osm.android.bonuspack.utils.BonusPackHelper;
import eu.trentorise.smartcampus.osm.android.bonuspack.utils.HttpConnection;
import eu.trentorise.smartcampus.osm.android.bonuspack.utils.PolylineEncoder;
import eu.trentorise.smartcampus.osm.android.util.BoundingBoxE6;
import eu.trentorise.smartcampus.osm.android.util.GeoPoint;

/** class to get a route between a start and a destination point, 
 * going through a list of waypoints. 
 * 
 * It uses MapQuest open, public and free API, based on OpenStreetMap data. <br>
 * See http://open.mapquestapi.com/directions/
 * @return a "Road" object. 
 * 
 * @author M.Kergall
 */
public class MapQuestRoadManager extends RoadManager {
	private String mLocale;
	private String mRoadType;
	private Context mContext;
	static final String MAPQUEST_GUIDANCE_SERVICE_V1 = "http://open.mapquestapi.com/directions/v1/route?key=Fmjtd%7Cluub20uy2q%2Cbg%3Do5-9urlqa";

	static final String MAPQUEST_GUIDANCE_SERVICE_V0 = "http://open.mapquestapi.com/guidance/v0/route?";
	/**
	 * fastest - Quickest drive time route.
	 */
	public static final String FASTEST="&routeType=fastest";
	/**
	 * shortest - Shortest driving distance route.
	 */
	public static final String SHORTEST ="&routeType=shortest";
	/**
	 * pedestrian - Walking route; Avoids limited access roads; Ignores turn restrictions.
	 */
	public static final String PEDESTRIAN ="&routeType=pedestrian";
	/**
	 * multimodal - Combination of walking and (if available) Public Transit.
	 */
	public static final String MULTIMODAL = "&routeType=multimodal";
	/**
	 * bicycle - Bike route; Avoids limited access roads; Avoids roads where bicycle access is false; Favors bike specific paths and lower maxspeed roads.
	 */
	public static final String BYCICLE ="&routeType=bicycle";


	boolean apiv1;

	/**
	 * 
	 * @param locale
	 * for the Instructions' language
	 * @param roadType
	 * the type of the road (e.g. PEDESTRIAN, FASTEST, SHORTEST)
	 * @param context
	 * @param apiv1
	 * true if you want to use mapquest's api v1, false for api v0
	 */
	public MapQuestRoadManager(Locale locale, String roadType, Context context, boolean apiv1){
		mLocale = "&locale="+locale.getLanguage()+"_"+locale.getCountry();
		mRoadType = roadType;
		mContext = context;
		this.apiv1  = apiv1;
	}

	/**
	 * Build the URL to MapQuest service returning a route in XML format
	 * @param waypoints: array of waypoints, as [lat, lng], from start point to end point. 
	 */
	protected String getUrl(ArrayList<GeoPoint> waypoints) {
		StringBuffer urlString;
		if(apiv1){
			urlString = new StringBuffer(MAPQUEST_GUIDANCE_SERVICE_V1);
		}else{
			urlString = new StringBuffer(MAPQUEST_GUIDANCE_SERVICE_V0);
		}
		urlString.append("&callback=renderAdvancedNarrative");
		urlString.append("&outFormat=xml");
		urlString.append(mRoadType);
		urlString.append("&timeType=1");
		urlString.append("&enhancedNarrative=false"); //or "none"
		urlString.append("&shapeFormat=cmp"); //encoded polyline, much faster
		urlString.append("&generalize=0");
		urlString.append(mLocale); //add language
		urlString.append("&unit=k");

		urlString.append("&from=");
		GeoPoint p = waypoints.get(0);
		urlString.append(geoPointAsString(p));

		for (int i=1; i<waypoints.size(); i++){
			p = waypoints.get(i);
			urlString.append("&to="+geoPointAsString(p));
		}
		//urlString.append("&generalizeAfter=500" /*+&generalize=2"*/); 
		//500 points max, 2 meters tolerance

		//Warning: MapQuest Open API doc is sometimes WRONG:
		//- use unit, not units
		//- use fishbone, not enableFishbone
		//- locale (fr_FR, en_US) is supported but not documented. 
		//- generalize and generalizeAfter are not properly implemented
		return urlString.toString();
	}

	/**
	 * @param waypoints: list of GeoPoints. Must have at least 2 entries, start and end points. 
	 * @return the road
	 */
	@Override public Road getRoad(ArrayList<GeoPoint> waypoints) {
		String url = getUrl(waypoints);
		Log.d(BonusPackHelper.LOG_TAG, "MapQuestRoadManager.getRoute:"+url);
		Road road = null;
		HttpConnection connection = new HttpConnection();
		connection.doGet(url);
		InputStream stream = connection.getStream();
		if (stream != null)
			road = getRoadXML(stream, waypoints);
		if (road == null || road.mRouteHigh.size()==0){
			//Create default road:
			road = new Road(waypoints);
		}
		connection.close();
		Log.d(BonusPackHelper.LOG_TAG, "MapQuestRoadManager.getRoute - finished");
		//		Log.w("sixze", Integer.toString(road.mNodes.size()));
		//		Log.w("sixze", (road.mNodes.get(4).mInstructions));
		return road;
	}

	/** 
	 * XML implementation
	 * @param is: input stream to parse
	 * @return the road
	 */
	protected Road getRoadXML(InputStream is, ArrayList<GeoPoint> waypoints) {
		if(apiv1){
			XMLHandler handler = new XMLHandler();
			try {
				SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
				parser.parse(is, handler);
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Road road = handler.mRoad;
			if (road != null && road.mRouteHigh.size()>0){
				road.mNodes = finalizeNodes(road.mNodes, handler.mLinks, road.mRouteHigh);
				road.mRouteHigh = finalizeRoadShape(road, handler.mLinks);
				road.buildLegs(waypoints);
				road.mStatus = Road.STATUS_OK;
			}
			return road;
		}else {
			XMLParser handler = new XMLParser(mContext,is);
			Road road = handler.getRoad();
			return road;

		}
	}

	protected ArrayList<RoadNode> finalizeNodes(ArrayList<RoadNode> mNodes, 
			ArrayList<RoadLink> mLinks, ArrayList<GeoPoint> polyline){
		int n = mNodes.size();
		if (n == 0)
			return mNodes;
		ArrayList<RoadNode> newNodes = new ArrayList<RoadNode>(n);
		RoadNode lastNode = null;
		for (int i=1; i<n-1; i++){ //1, n-1 => first and last MapQuest nodes are irrelevant.
			RoadNode node = mNodes.get(i);
			RoadLink link = mLinks.get(node.mNextRoadLink);
			if (lastNode!=null && (node.mInstructions == null || node.mManeuverType == 0)){
				//this node is irrelevant, don't keep it, 
				//but update values of last node:
				lastNode.mLength += link.mLength;
				lastNode.mDuration += (node.mDuration + link.mDuration);
			} else {
				//TODO: check not the end node (n-2)
				node.mLength = link.mLength;
				node.mDuration += link.mDuration;
				int locationIndex = link.mShapeIndex;
				node.mLocation = polyline.get(locationIndex);
				newNodes.add(node);
				lastNode = node;
			}
		}
		//switch to the new array of nodes:
		return newNodes;
	}

	/**
	 * Clean-up 2 useless portions of MapQuest road shape: before start node, and after end node. 
	 * @return new road shape
	 */
	public ArrayList<GeoPoint> finalizeRoadShape(Road road, ArrayList<RoadLink> links){
		ArrayList<GeoPoint> newShape = new ArrayList<GeoPoint>(road.mRouteHigh.size());
		RoadNode nodeStart = road.mNodes.get(0);
		RoadNode nodeEnd = road.mNodes.get(road.mNodes.size()-1);
		int shapeIndexStart = links.get(nodeStart.mNextRoadLink).mShapeIndex;
		int shapeIndexEnd = links.get(nodeEnd.mNextRoadLink).mShapeIndex;
		for (int i=shapeIndexStart; i<=shapeIndexEnd; i++){
			newShape.add(road.mRouteHigh.get(i));
		}
		return newShape;
	}
}

/** Road Link is a portion of road between 2 "nodes" or intersections */
class RoadLink {
	/** in km/h */
	public double mSpeed; 
	/** in km */
	public double mLength; 
	/** in sec */
	public double mDuration;
	/** starting point of the link, as index in initial polyline */
	public int mShapeIndex; 
}

/** XMLHandler: class to handle XML generated by MapQuest "guidance" open API. 
 *
 */
class XMLHandler extends DefaultHandler {
	public Road mRoad;
	public ArrayList<RoadLink> mLinks;

	boolean isBB;
	boolean isGuidanceNodeCollection;
	private String mString;
	double mLat, mLng;
	double mNorth, mWest, mSouth, mEast;
	RoadLink mLink;
	RoadNode mNode;

	public XMLHandler() {
		isBB = isGuidanceNodeCollection = false;
		mRoad = new Road();
		mLinks = new ArrayList<RoadLink>();
	}

	@Override public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {		
		if (localName.equals("boundingBox"))
			isBB = true;
		else if (localName.equals("link"))
			mLink = new RoadLink();
		else if (localName.equals("node"))
			mNode = new RoadNode();
		else if (localName.equals("GuidanceNodeCollection"))
			isGuidanceNodeCollection = true;
		mString = new String();
	}

	/**
	 * Overrides org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override public void characters(char[] ch, int start, int length)
			throws SAXException {
		String chars = new String(ch, start, length);
		mString = mString.concat(chars);
	}

	@Override public void endElement(String uri, String localName, String name)
			throws SAXException {
		if (localName.equals("lat")) {
			mLat = Double.parseDouble(mString);
		} else if (localName.equals("lng")) {
			mLng = Double.parseDouble(mString);
		} else if (localName.equals("shapePoints")) {
			mRoad.mRouteHigh = PolylineEncoder.decode(mString, 10);
			//Log.d("DD", "High="+mRoad.mRouteHigh.size());
		} else if (localName.equals("generalizedShape")) {
			mRoad.setRouteLow(PolylineEncoder.decode(mString, 10));
			//Log.d("DD", "Low="+mRoad.mRouteLow.size());
		} else if (localName.equals("length")) {
			mLink.mLength = Double.parseDouble(mString);
		} else if (localName.equals("speed")) {
			mLink.mSpeed = Double.parseDouble(mString);
		} else if (localName.equals("shapeIndex")){
			mLink.mShapeIndex = Integer.parseInt(mString);
		} else if (localName.equals("link")) {
			//End of a link: update road attributes:
			//GuidanceLinkCollection could in theory contain additional unused links, 
			//but normally not with fishbone set to false. 
			mLink.mDuration = mLink.mLength / mLink.mSpeed * 3600.0;
			mLinks.add(mLink);
			mRoad.mLength += mLink.mLength;
			mRoad.mDuration += mLink.mDuration;
			mLink = null;
		} else if (localName.equals("turnCost")){
			int turnCost = Integer.parseInt(mString);
			mNode.mDuration += turnCost;
			mRoad.mDuration += turnCost;
		} else if (localName.equals("maneuverType")){
			mNode.mManeuverType = Integer.parseInt(mString);
		} else if (localName.equals("info")){
			if (isGuidanceNodeCollection){
				if (mNode.mInstructions == null) 
					//this is first "info" value for this node, keep it:
					mNode.mInstructions = mString;
			}
		} else if (localName.equals("linkId")){
			if (isGuidanceNodeCollection)
				mNode.mNextRoadLink = Integer.parseInt(mString);
		} else if (localName.equals("node")){
			mRoad.mNodes.add(mNode);
			mNode = null;
		} else if (localName.equals("GuidanceNodeCollection")){
			isGuidanceNodeCollection = false;
		} else if (localName.equals("ul")){
			if (isBB){
				mNorth = mLat;
				mWest = mLng;
			}
		} else if (localName.equals("lr")){
			if (isBB){
				mSouth = mLat;
				mEast = mLng;
			}
		} else if (localName.equals("boundingBox")){
			mRoad.mBoundingBox = new BoundingBoxE6(mNorth, mEast, mSouth, mWest);
			isBB = false;
		}
	}
}