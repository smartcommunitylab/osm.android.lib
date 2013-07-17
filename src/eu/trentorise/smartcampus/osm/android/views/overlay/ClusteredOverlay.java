package eu.trentorise.smartcampus.osm.android.views.overlay;


import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import eu.trentorise.smartcampus.osm.android.ResourceProxy;
import eu.trentorise.smartcampus.osm.android.util.ClusteringHelper;
import eu.trentorise.smartcampus.osm.android.views.MapView;
import eu.trentorise.smartcampus.osm.android.views.overlay.ItemizedOverlayWithFocus;
import eu.trentorise.smartcampus.osm.android.views.overlay.OverlayItem;

public class  ClusteredOverlay extends ItemizedOverlayWithFocus<OverlayItem> {
	List<OverlayItem> myList = null;
	List<OverlayItem> aList = null;
	Context mContext;
	

	

	
	public ClusteredOverlay(
			Context ctx,
			List<OverlayItem> aList,
			OnItemGestureListener<OverlayItem> aOnItemTapListener) {
		super(ctx, aList, aOnItemTapListener);
		// TODO Auto-generated constructor stub
		mContext = ctx;
		this.aList = aList;
	}





	public ClusteredOverlay(Context ctx,
			List<OverlayItem> aList,
			Drawable pMarker,
			Drawable pMarkerFocused,
			int pFocusedBackgroundColor,
			OnItemGestureListener<OverlayItem> aOnItemTapListener,
			ResourceProxy pResourceProxy) {
		super(aList, pMarker, pMarkerFocused, pFocusedBackgroundColor,
				aOnItemTapListener, pResourceProxy);
		// TODO Auto-generated constructor stub
		this.aList = aList;
	}





	public ClusteredOverlay(Context ctx,
			List<OverlayItem> aList,
			OnItemGestureListener<OverlayItem> aOnItemTapListener,
			ResourceProxy pResourceProxy) {
		super(aList, aOnItemTapListener, pResourceProxy);
		this.aList = aList;
	}





	@Override
	public void draw(Canvas c, MapView osmv, boolean shadow) {
		myList = ClusteringHelper.cluster(mContext, osmv, aList);
		ClusteringHelper.render(osmv, myList);
		myList.clear();
	}


}
