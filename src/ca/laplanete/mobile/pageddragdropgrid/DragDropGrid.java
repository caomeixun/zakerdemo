/**
 * Copyright 2012
 *
 * Nicolas Desjardins
 * https://github.com/mrKlar
 *
 * Facilite solutions
 * http://www.facilitesolutions.com/
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ca.laplanete.mobile.pageddragdropgrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ListView;
import android.widget.ScrollView;

public class DragDropGrid extends ViewGroup implements OnTouchListener, OnLongClickListener {

	private static int ANIMATION_DURATION = 250;
	private static int EGDE_DETECTION_MARGIN = 35;

	private PagedDragDropGridAdapter adapter;
	private OnClickListener onClickListener = null;
	private PagedContainer container;

	private SparseIntArray newPositions = new SparseIntArray();
	private float scaleSize = 1.2f;
	protected static int gridPageWidth  = 0;
	private int gridPageHeight = 0;
	public static int pageHeight     = 0;
	public static int maxHeight     = 0;
	private int dragged = -1;
	private int columnWidthSize;
	private int rowHeightSize;
	private int biggestChildWidth;
	public static int biggestChildHeight = 0;
	private int computedColumnCount;
	private int computedRowCount;
	private int initialRawX;
	private int initialRawY;
	private int initialX;
	private int initialY;
	private boolean movingView;
	private int lastTarget = -1;
	private boolean wasOnEdgeJustNow = false;
	private Timer edgeScrollTimer;
	
	private int space;
	int topSpace = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());;
	
	final private Handler edgeTimerHandler = new Handler();
	private int lastTouchX;
	private int lastTouchY;

	private DeleteDropZoneView deleteZone;

	public DragDropGrid(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public DragDropGrid(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public DragDropGrid(Context context) {
		super(context);
		init();
	}

	public DragDropGrid(Context context, AttributeSet attrs, int defStyle, PagedDragDropGridAdapter adapter, PagedContainer container) {
		super(context, attrs, defStyle);
		this.adapter = adapter;
		this.container = container;
		init();
	}

	public DragDropGrid(Context context, AttributeSet attrs, PagedDragDropGridAdapter adapter, PagedContainer container) {
		super(context, attrs);
		this.adapter = adapter;
		this.container = container;
		init();
	}

	public DragDropGrid(Context context, PagedDragDropGridAdapter adapter, PagedContainer container) {
		super(context);
		this.adapter = adapter;
		this.container = container;
		init();
	}

	private void init() {
//		new Handler().postDelayed(new Runnable() {
//			
//			@Override
//			public void run() {
//		       	Log.e("TAG",DragDropGrid.this.getWidth()+ "down"+DragDropGrid.this.getHeight());
//		    	scrollTo(0, 200);
//				invalidate();
//			}
//		}, 2000);
			
	    if (isInEditMode() && adapter == null) {
	        useEditModeAdapter();
	    }
	    
		setOnTouchListener(this);
		setOnLongClickListener(this);
		createDeleteZone();
	}

	private void useEditModeAdapter() {
	    adapter = new PagedDragDropGridAdapter() {
            
            @Override
            public View view(int page, int index) {
                return null;
            }
            
            @Override
            public void swapItems(int pageIndex, int itemIndexA, int itemIndexB) {

            }
            
            @Override
            public int rowCount() {
                return AUTOMATIC;
            }
            
            @Override
            public int pageCount() {
                return AUTOMATIC;
            }
            
            @Override
            public void moveItemToPreviousPage(int pageIndex, int itemIndex) {

            }
            
            @Override
            public void moveItemToNextPage(int pageIndex, int itemIndex) {

            }
            
            @Override
            public int itemCountInPage(int page) {
                return 0;
            }
            
            @Override
            public void deleteItem(int pageIndex, int itemIndex) {

            }
            
            @Override
            public int columnCount() {
                return 0;
            }

            @Override
            public int deleteDropZoneLocation() {
                return PagedDragDropGridAdapter.BOTTOM;
            }

            @Override
            public Bitmap showRemoveDropZone() {
                return null;
            }

			@Override
			public boolean isLastCanMove() {
				return false;
			}

			@Override
			public int getMaxPageSize() {
				return 0;
			}
        };       
    }

    public void setAdapter(PagedDragDropGridAdapter adapter) {
		this.adapter = adapter;
		space = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, adapter.pageCount()==1?5:1, getResources().getDisplayMetrics());
		addChildViews();		
	}

	public void setOnClickListener(OnClickListener l) {
	    onClickListener = l;
	}

	private void addChildViews() {
		for (int page = 0; page < adapter.pageCount(); page++) {
			for (int item = 0; item < adapter.itemCountInPage(page); item++) {
				addView(adapter.view(page, item));
			}
		}
		deleteZone.bringToFront();
	}

	private void animateMoveAllItems() {
		Animation rotateAnimation = createFastRotateAnimation();

		for (int i=0; i < getItemViewCount(); i++) {
			View child = getChildAt(i);
			child.startAnimation(rotateAnimation);
		 }
	}

	private void cancelAnimations() {
		 for (int i=0; i < getItemViewCount()-2; i++) {
			 View child = getChildAt(i);
			 child.clearAnimation();
		 }
	}

	public boolean onInterceptTouchEvent(MotionEvent event) {
	    return onTouch(null, event);
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			touchDown(event);
			break;
		case MotionEvent.ACTION_MOVE:
			touchMove(event);
			break;
		case MotionEvent.ACTION_UP:
			touchUp(event);
			break;
		}
		if (aViewIsDragged())
			return true;
		return false;
	}

	private void touchUp(MotionEvent event) {
	    if(!aViewIsDragged()) {
	        if(onClickListener != null) {
                View clickedView = getChildAt(getTargetAtCoor((int) event.getX(), (int) event.getY()));
                if(clickedView != null)
                    onClickListener.onClick(clickedView);
            }
	    } else {
	        cancelAnimations();
    		manageChildrenReordering();
	    }
	}

	private void manageChildrenReordering() {
		boolean draggedDeleted = touchUpInDeleteZoneDrop(lastTouchX, lastTouchY);
		
		if (draggedDeleted) {
			animateDeleteDragged();
			final int delId = dragged;
			reorderChildrenWhenDraggedIsDeleted(delId);
		
		} else {
			reorderChildren();
    		recoveyZaker();
		}
	}
	/**
	 * 还原空间未初始化装填
	 */
	private void recoveyZaker() {
		hideDeleteView();
		cancelEdgeTimer();

		movingView = false;
		dragged = -1;
		lastTarget = -1;
		container.enableScroll();
	}


	private void animateDeleteDragged() {
		ScaleAnimation scale = new ScaleAnimation(scaleSize, 0f, scaleSize, 0f, biggestChildWidth / 2 , biggestChildHeight / 2);
		scale.setDuration(ANIMATION_DURATION-30);
		scale.setFillAfter(true);
		scale.setFillEnabled(true);

		getDraggedView().clearAnimation();
		getDraggedView().startAnimation(scale);
	}

	private void reorderChildrenWhenDraggedIsDeleted(final int dragId) {
		final Integer newDraggedPosition = newPositions.get(dragId,dragId);
		tellAdapterDraggedIsDeleted(newDraggedPosition);
		delAnimation(newDraggedPosition);

		new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				delLayoutAgain(newDraggedPosition);
				
			}
		}, ANIMATION_DURATION);
	}
	private void delLayoutAgain(int postition){
		List<View> children = saveChildren();
		removeItemChildren(children);
		//List<View> reorderedViews = reeorderDelView(children,postition);
		addReorderedChildrenToParent(children);
		tellAdapterDraggedIsDeleted(postition);
		removeViewAt(postition);
		recoveyZaker();
		requestFocus();
	}
	private List<Integer> lists = new ArrayList<Integer>();
	private void delAnimation(int delId){
		lists = new ArrayList<Integer>();
		SparseIntArray cache = new SparseIntArray();
		for(int i=0;i<newPositions.size();i++){
			cache.put(newPositions.keyAt(i), newPositions.valueAt(i));
		}
		nextPageAnimation(delId+1,getChildCount()-2);
		newPositions =cache;
	}
	
	private void nextPageAnimation(int stardId,int maxLocation){
		
		for(int i= stardId;i <= maxLocation;i++){
			animateGap(i);
			lastTarget = i;
		}
		
	}
	private void prePageAnimation(int stardId,int minLocation){
		
		for(int i= stardId;i >= minLocation;i--){
			animateGap(i);
			lastTarget = i;
		}
		
	}
	private void moveNextAnimation(int id){
		for(int i= id;i<getChildCount()-2;i++){
			View targetView = getChildView(i);
			
			
			Point oldXY = getCoorForIndex(i);
			Point newXY = getCoorForIndex(i+1);
			Point oldOffset = computeTranslationStartDeltaRelativeToRealViewPosition(i, i+1, oldXY);
			Point newOffset = computeTranslationEndDeltaRelativeToRealViewPosition(oldXY, newXY);
			
			animateMoveToNewPosition(targetView, oldOffset, newOffset);
		}
		
	}
	private void tellAdapterDraggedIsDeleted(Integer newDraggedPosition) {
		ItemPosition position = itemInformationAtPosition(newDraggedPosition);
		adapter.deleteItem(position.pageIndex,position.itemIndex);
	}

	private void touchDown(MotionEvent event) {
		initialRawX = (int)event.getRawX();
		initialRawY = (int)event.getRawY();
		initialX = (int)event.getX();
		initialY = (int)event.getY();

		lastTouchX = (int)event.getRawX() + (currentPage() * gridPageWidth);
		lastTouchY = (int)event.getRawY();
	}

	private void touchMove(MotionEvent event) {
		if (movingView && aViewIsDragged()) {

			
			lastTouchX = (int) event.getX();
			lastTouchY = (int) event.getY();

			ensureThereIsNoArtifact();
			
			moveDraggedView(lastTouchX, lastTouchY);
			manageSwapPosition(lastTouchX, lastTouchY);
			manageEdgeCoordinates(lastTouchX,lastTouchY);
			manageDeleteZoneHover(lastTouchX, lastTouchY);
			
			
		}
	}

    private void ensureThereIsNoArtifact() {
        invalidate();
    }

	private void manageDeleteZoneHover(int x, int y) {
		Rect zone = new Rect();
		deleteZone.getHitRect(zone);

		if (zone.intersect(x, y, x+1, y+1)) {
			
			deleteZone.highlight();
		} else {
			deleteZone.smother();
		}
	}

	private boolean touchUpInDeleteZoneDrop(int x, int y) {
		Rect zone = new Rect();
		deleteZone.getHitRect(zone);

		if (zone.intersect(x, y, x+1, y+1)) {
			deleteZone.smother();
			return true;
		}
		return false;
	}

	private void moveDraggedView(int x, int y) {
		View childAt = getDraggedView();		
		
		int width = childAt.getMeasuredWidth();
		int height = childAt.getMeasuredHeight();
		int yTiaoZheng = initialY % height;
		int xTiaoZheng = initialX- (columnWidthSize - width - space);
		if(initialX > columnWidthSize)
			xTiaoZheng = initialX -columnWidthSize - space;
		int l = x - xTiaoZheng;//- (1 * width / 2);
		int t = y - yTiaoZheng + topSpace;//- (1 * height / 2);
		priviousPageX = x;
		priviousPageY = y;
		childAt.layout(l, t, l + width, t + height);
	}

	private void manageSwapPosition(int x, int y) {
		int target = getTargetAtCoor(x, y);
		if(target == getChildCount()-2 && !adapter.isLastCanMove()){
			return ;
		}
		if (childHasMoved(target) && target != lastTarget) {
			animateGap(target);
			lastTarget = target;
		}
	}
	private int priviousPageX = 0;
	private int priviousPageY = 0;
	private void manageEdgeCoordinates(int x,int y) {
		//final boolean onRightEdge = onRightEdgeOfScreen(x);
		//final boolean onLeftEdge = onLeftEdgeOfScreen(x);
		final boolean onBottomEdge = onBottomEdgeOfScreen(y);
		final boolean onTopEdge = onTopEdgeOfScreen(y);
		if (canScrollToEitherSide(onTopEdge,onBottomEdge)) {
			if (!wasOnEdgeJustNow) {
				startEdgeDelayTimer(onTopEdge, onBottomEdge);
				wasOnEdgeJustNow = true;
			}
		} else {
			if (wasOnEdgeJustNow) {
				stopAnimateOnTheEdge();
			}
			wasOnEdgeJustNow = false;
			cancelEdgeTimer();
		}
	}

	private void stopAnimateOnTheEdge() {
			View draggedView = getDraggedView();
			draggedView.clearAnimation();
			animateDragged();
	}

	private void cancelEdgeTimer() {

		if (edgeScrollTimer != null) {
			edgeScrollTimer.cancel();
			edgeScrollTimer = null;
		}
	}

	private void startEdgeDelayTimer(final boolean onTopEdge, final boolean onBottomEdge) {
		if (canScrollToEitherSide(onTopEdge,onBottomEdge)) {
			animateOnTheEdge();
			if (edgeScrollTimer == null) {
				edgeScrollTimer = new Timer();
				scheduleScroll(onTopEdge, onBottomEdge);
			}
		}
	}

	private void scheduleScroll(final boolean onTopEdge, final boolean onBottomEdge) {
		edgeScrollTimer.schedule(new TimerTask() {
		    @Override
		    public void run() {
		    	if (wasOnEdgeJustNow) {
		    		wasOnEdgeJustNow = false;
		    		edgeTimerHandler.post(new Runnable() {
						@Override
						public void run() {
							hideDeleteView();
							scroll(onTopEdge, onBottomEdge);
							//cancelAnimations();
							//animateMoveAllItems();
							animateDragged();
							//popDeleteView();
						}
					});
		    	}
		    }
		}, 1000);
	}

//	private boolean canScrollToEitherSide(final boolean onRightEdge, final boolean onLeftEdge) {
//		return (onLeftEdge && container.canScrollToPreviousPage()) || (onRightEdge && container.canScrollToNextPage());
//	}
	private boolean canScrollToEitherSide(final boolean onTopEdge,final boolean onBottomEdge) {
		return (onBottomEdge && container.canScrollToNextPage() ) || ( onTopEdge&& container.canScrollToPreviousPage());
	}

	private void scroll(boolean onTopEdge, boolean onBottomEdge) {
		cancelEdgeTimer();

		if (onTopEdge && container.canScrollToPreviousPage()) {
			scrollToPreviousPage();
			popDeleteView(-DragDropGrid.pageHeight);
		} else if (onBottomEdge && container.canScrollToNextPage()) {
			scrollToNextPage();
			popDeleteView(DragDropGrid.pageHeight);
		}
		wasOnEdgeJustNow = false;
	}
	private void scrollToNextPage() {
		
		//TODO
		//tellAdapterToMoveItemToNextPage(dragged);
		//moveDraggedToNextPage();
		container.scrollButtom();
//		int currentPage = 0;
//		int lastItem = adapter.itemCountInPage(currentPage)-1;
//		dragged = positionOfItem(currentPage, lastItem);
		//stopAnimateOnTheEdge();
		int postion = priviousPageY+pageHeight;
		if(postion>maxHeight){
			postion = maxHeight - biggestChildHeight/2;
		}
		moveDraggedView(priviousPageX,postion);
//		requestLayout();
		int location = getTargetAtCoor(priviousPageX,postion);
		nextPageAnimation(lastTarget+1,location);
	}

	private void scrollToPreviousPage() {
		//tellAdapterToMoveItemToPreviousPage(dragged);
//		moveDraggedToPreviousPage();
//
		container.scrollTop();
//		int currentPage = currentPage();
//		int lastItem =  findTheIndexOfFirstElementInCurrentPage()-1;
//		dragged = positionOfItem(currentPage, lastItem);
//		bringDraggedToFront();
//		requestLayout();
//				
//		stopAnimateOnTheEdge();
		int postion = priviousPageY - pageHeight;
		if(postion < 0){
			postion = 0;
		}
		moveDraggedView(priviousPageX,postion<=0?postion-biggestChildHeight/2:postion);
//		requestLayout();
		int location = getTargetAtCoor(priviousPageX,postion);
		prePageAnimation(lastTarget+1,location);
	}
	int draggedEndPosition = newPositions.get(dragged, dragged);
		private void moveDraggedToPreviousPage() {
		List<View> children = cleanUnorderedChildren();
		newPositions.clear();
		List<View> reorderedViews = reeorderView(children);

		View draggedView = reorderedViews.get(draggedEndPosition);
		reorderedViews.remove(draggedEndPosition);

		int indexFirstElementInCurrentPage = findTheIndexOfFirstElementInCurrentPage();
		int indexOfDraggedOnNewPage = indexFirstElementInCurrentPage-1;		
		reorderAndAddViews(reorderedViews, draggedView, indexOfDraggedOnNewPage);
	}

    private int findTheIndexOfFirstElementInCurrentPage() {
    	int location = getTargetAtCoor(priviousPageX,priviousPageY);
        return location;//indexFirstElementInCurrentPage;
    }

	private void removeItemChildren(List<View> children) {
		for (View child : children) {
			removeView(child);
		}
	}

	private void moveDraggedToNextPage() {
		List<View> children = cleanUnorderedChildren();

		List<View> reorderedViews = reeorderView(children);
		int draggedEndPosition = newPositions.get(dragged, dragged);

		View draggedView = reorderedViews.get(draggedEndPosition);
		reorderedViews.remove(draggedEndPosition);

		int indexLastElementInNextPage = findTheIndexLastElementInNextPage();

		int indexOfDraggedOnNewPage = indexLastElementInNextPage-1;
		reorderAndAddViews(reorderedViews, draggedView, indexOfDraggedOnNewPage);
	}

    private int findTheIndexLastElementInNextPage() {
        int currentPage = currentPage();
		int indexLastElementInNextPage = 0;
		for (int i=0;i<=currentPage+1;i++) {
			indexLastElementInNextPage += adapter.itemCountInPage(i);
		}
        return adapter.itemCountInPage(0);//indexLastElementInNextPage;
    }

	private void reorderAndAddViews(List<View> reorderedViews, View draggedView, int indexOfDraggedOnNewPage) {

		reorderedViews.add(indexOfDraggedOnNewPage,draggedView);
		newPositions.clear();

		for (View view : reorderedViews) {
			if (view != null) {
				addView(view);
			}
		}

		deleteZone.bringToFront();
	}

	private boolean onLeftEdgeOfScreen(int x) {
		int currentPage = container.currentPage();

		int leftEdgeXCoor = currentPage*gridPageWidth;
		int distanceFromEdge = x - leftEdgeXCoor;
		return (x > 0 && distanceFromEdge <= EGDE_DETECTION_MARGIN);
	}

	private boolean onRightEdgeOfScreen(int x) {
		int currentPage = container.currentPage();

		int rightEdgeXCoor = (currentPage*gridPageWidth) + gridPageWidth;
		int distanceFromEdge = rightEdgeXCoor - x;
		return (x > (rightEdgeXCoor - EGDE_DETECTION_MARGIN)) && (distanceFromEdge < EGDE_DETECTION_MARGIN);
	}
	/**
	 * 根据底部控制
	 * @param y
	 * @return
	 */
	private boolean onBottomEdgeOfScreen(int y) {
		View view = getDraggedView();
		int[] location = new int[2];
		view.getLocationOnScreen(location);
		y = location[1]+biggestChildHeight/2;
		int rightEdgeXCoor = pageHeight;
		int distanceFromEdge = rightEdgeXCoor - y;

		return (y > (rightEdgeXCoor - EGDE_DETECTION_MARGIN)) && (distanceFromEdge < EGDE_DETECTION_MARGIN);
	}
	/**
	 * 根据底部控制
	 * @param y
	 * @return
	 */
	private boolean onTopEdgeOfScreen(int y) {
		View view = getDraggedView();
		int[] location = new int[2];
		view.getLocationOnScreen(location);
		y = location[1];
		return (y <= EGDE_DETECTION_MARGIN);
	}

	private void animateOnTheEdge() {
		View v = getDraggedView();

		ScaleAnimation scale = new ScaleAnimation(.887f, scaleSize, .887f, scaleSize, v.getMeasuredWidth() * 3 / 4, v.getMeasuredHeight() * 3 / 4);
		scale.setDuration(200);
		scale.setRepeatMode(Animation.REVERSE);
		scale.setRepeatCount(Animation.INFINITE);

		v.clearAnimation();
		v.startAnimation(scale);
	}

	private void animateGap(int targetLocationInGrid) {
		int viewAtPosition = currentViewAtPosition(targetLocationInGrid);
		if (viewAtPosition == dragged) {
			return;
		}

		View targetView = getChildView(viewAtPosition);
		lists.add(viewAtPosition);
//	      Log.e("animateGap target", ((TextView)targetView.findViewWithTag("text")).getText().toString());

		Point oldXY = getCoorForIndex(viewAtPosition);
		Point newXY = getCoorForIndex(newPositions.get(dragged, dragged));
		Point oldOffset = computeTranslationStartDeltaRelativeToRealViewPosition(targetLocationInGrid, viewAtPosition, oldXY);
		Point newOffset = computeTranslationEndDeltaRelativeToRealViewPosition(oldXY, newXY);
		animateMoveToNewPosition(targetView, oldOffset, newOffset);
		saveNewPositions(targetLocationInGrid, viewAtPosition);
	}
	private Point computeTranslationEndDeltaRelativeToRealViewPosition(Point oldXY, Point newXY) {
		return new Point(newXY.x - oldXY.x, newXY.y - oldXY.y);
	}

	private Point computeTranslationStartDeltaRelativeToRealViewPosition(int targetLocation, int viewAtPosition, Point oldXY) {
		Point oldOffset;
		if (viewWasAlreadyMoved(targetLocation, viewAtPosition)) {
			Point targetLocationPoint = getCoorForIndex(targetLocation);
			oldOffset = computeTranslationEndDeltaRelativeToRealViewPosition(oldXY, targetLocationPoint);
		} else {
			oldOffset = new Point(0,0);
		}
		return oldOffset;
	}
	/**
	 * 
	 * @param targetLocation 拖动空间的位置
	 * @param viewAtPosition 原位置交换后的位置
	 */
	private void saveNewPositions(int targetLocation, int viewAtPosition) {
		newPositions.put(viewAtPosition, newPositions.get(dragged, dragged));
		newPositions.put(dragged, targetLocation);
		tellAdapterToSwapDraggedWithTarget(newPositions.get(dragged, dragged), newPositions.get(viewAtPosition, viewAtPosition));
	}

	private boolean viewWasAlreadyMoved(int targetLocation, int viewAtPosition) {
		return viewAtPosition != targetLocation;
	}

	private void animateMoveToNewPosition(View targetView, Point oldOffset, Point newOffset) {
//		AnimationSet set = new AnimationSet(true);
//
//		Animation rotate = createFastRotateAnimation();
//		Animation translate = createTranslateAnimation(oldOffset, newOffset);
//
//		set.addAnimation(rotate);
//		set.addAnimation(translate);
//
//		targetView.clearAnimation();
		targetView.startAnimation(createTranslateAnimation(oldOffset, newOffset));
	}
	/**
	 * 定义去任意目标
	 * @param positoin
	 */
	public void toAbsoultPosition(View v){
		dragged = positionForView(v);
		Point newOffSet = new Point(0, 0);
		int[] toOff=new int[2];
		v.getLocationInWindow(toOff);
		Point toOffSet = new Point(-toOff[0],-toOff[1]);
		AnimationSet ani = new AnimationSet(false);
		ScaleAnimation scale = new ScaleAnimation(scaleSize, 0f, scaleSize, 0f, biggestChildWidth / 2 , biggestChildHeight / 2);
		scale.setDuration(ANIMATION_DURATION+100);
		ani.setFillEnabled(true);
		ani.setFillAfter(true);
		ani.addAnimation(scale);
		ani.addAnimation(createTranslateAnimation(newOffSet, toOffSet));
		v.startAnimation(ani);


		bringDraggedToFront();
		reorderChildrenWhenDraggedIsDeleted(dragged);
		
	}
	private TranslateAnimation createTranslateAnimation(Point oldOffset, Point newOffset) {
		TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, oldOffset.x,
															  Animation.ABSOLUTE, newOffset.x,
															  Animation.ABSOLUTE, oldOffset.y,
															  Animation.ABSOLUTE, newOffset.y);
		translate.setDuration(ANIMATION_DURATION);
		translate.setFillEnabled(true);
		translate.setFillAfter(true);
		translate.setInterpolator(new AccelerateDecelerateInterpolator());
		return translate;
	}

	private Animation createFastRotateAnimation() {
		Animation rotate = new RotateAnimation(-2.0f,
										  2.0f,
										  Animation.RELATIVE_TO_SELF,
										  0.5f,
										  Animation.RELATIVE_TO_SELF,
										  0.5f);

	 	rotate.setRepeatMode(Animation.REVERSE);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setDuration(60);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());

		return rotate;
	}

	private int currentViewAtPosition(int targetLocation) {
		int viewAtPosition = targetLocation;
		for (int i = 0; i < newPositions.size(); i++) {
			int value = newPositions.valueAt(i);
			if (value == targetLocation) {
				viewAtPosition = newPositions.keyAt(i);
				break;
			}
		}
		return viewAtPosition;
	}

	private Point getCoorForIndex(int index) {
		ItemPosition page = itemInformationAtPosition(index);

		int row = page.itemIndex / computedColumnCount;
		int col = page.itemIndex - (row * computedColumnCount);
		int x,y;
		if(computedColumnCount==2){
			x = (currentPage() * gridPageWidth) + (columnWidthSize * col);
			y = rowHeightSize * row;
		}else{
			x = col* biggestChildWidth + (col + 1)*(gridPageWidth-adapter.columnCount() * biggestChildWidth)/(adapter.columnCount() +1);
			y = (row * (biggestChildHeight)) ;
		}
		//傍边空余一部分内容
		if(computedColumnCount==2){
			if(x==0){
				x += columnWidthSize - biggestChildWidth -space;
			}else{
				x = columnWidthSize +space;
			}
		}
		return new Point(x, y);
	}

	private int getTargetAtCoor(int x, int y) {
		int page = currentPage();

		int col = getColumnOfCoordinate(x, page);
		int row = getRowOfCoordinate(y);
		int positionInPage = col + (row * computedColumnCount);

		return positionOfItem(page, positionInPage);
	}

	private int getColumnOfCoordinate(int x, int page) {
		int col = 0;
		int pageLeftBorder = (page) * gridPageWidth;
		for (int i = 1; i <= computedColumnCount; i++) {
			int colRightBorder = (i * columnWidthSize) + pageLeftBorder;
			if (x < colRightBorder) {
				break;
			}
			col++;
		}
		return col;
	}

	private int getRowOfCoordinate(int y) {
		int row = 0;
		for (int i = 1; i <= computedRowCount; i++) {
			if (y < i * rowHeightSize) {
				break;
			}
			row++;
		}
		return row;
	}

	private int currentPage() {
		return 0;//container.currentPage();
	}

	private void reorderChildren() {
		List<View> children = cleanUnorderedChildren();
		addReorderedChildrenToParent(children);
//		requestLayout();
	}

	private List<View> cleanUnorderedChildren() {
		List<View> children = saveChildren();
		removeItemChildren(children);
		return children;
	}

	private void addReorderedChildrenToParent(List<View> children) {
		List<View> reorderedViews = reeorderView(children);
		newPositions.clear();
		for (View view : reorderedViews) {
			if (view != null){
				addView(view);
			}
		}

		deleteZone.bringToFront();
	}
	/**
	 * 保存现有的子项
	 * @return
	 */
	private List<View> saveChildren() {
		List<View> children = new ArrayList<View>();
		for (int i = 0; i < getItemViewCount(); i++) {
			View child;
			if (i == dragged) {
				child = getDraggedView();
			} else {
				child = getChildView(i);
			}
			child.clearAnimation();
			children.add(child);
		}
		return children;
	}

	private List<View> reeorderView(List<View> children) {
		View[] views = new View[children.size()];

		for (int i = 0; i < children.size(); i++) {
			int position = newPositions.get(i, -1);
			if (childHasMoved(position)) {
				if(position<children.size())
				views[position] = children.get(i);
			} else {
				views[i] = children.get(i);
			}
		}
		return new ArrayList<View>(Arrays.asList(views));
	}

	private boolean childHasMoved(int position) {
		return position != -1;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

		Display display = wm.getDefaultDisplay();

		widthSize = acknowledgeWidthSize(widthMode, widthSize, display);
		heightSize = acknowledgeHeightSize(heightMode, heightSize, display);

		adaptChildrenMeasuresToViewSize(widthSize, heightSize);
		searchBiggestChildMeasures();
		double number = 2d;
		if (adapter.columnCount() != PagedDragDropGridAdapter.AUTOMATIC && adapter.rowCount() != PagedDragDropGridAdapter.AUTOMATIC) {
			number = adapter.columnCount();
		}
		double count =Math.ceil( getItemViewCount()/number);
		if(adapter.pageCount()>1){
			heightSize = (int) Math.round(3 * biggestChildHeight);
		}else{
			heightSize = (int) Math.round(count * biggestChildHeight);
		}
		computeGridMatrixSize(widthSize, heightSize);
		computeColumnsAndRowsSizes(widthSize, heightSize);
		heightSize += topSpace;

		heightSize = heightSize < pageHeight ? pageHeight: heightSize;
		gridPageHeight = heightSize ;
		maxHeight = gridPageHeight;
		
		measureChild(deleteZone, MeasureSpec.makeMeasureSpec(gridPageWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int)getPixelFromDip(50), MeasureSpec.EXACTLY));
		if(adapter.getMaxPageSize()%2 != 0 && adapter.getMaxPageSize() == getItemViewCount()){
			setMeasuredDimension(widthSize * adapter.pageCount(), heightSize - biggestChildHeight);
		}else{
			setMeasuredDimension(widthSize * adapter.pageCount(), heightSize);
		}
	}

	private float getPixelFromDip(int size) {
		Resources r = getResources();
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, r.getDisplayMetrics());
		return px;
	}

	private void computeColumnsAndRowsSizes(int widthSize, int heightSize) {
		columnWidthSize = widthSize / computedColumnCount;
		rowHeightSize = heightSize / computedRowCount;
	}

	private void computeGridMatrixSize(int widthSize, int heightSize) {
		if (adapter.columnCount() != -1 && adapter.rowCount() != -1) {
			computedColumnCount = adapter.columnCount();
			computedRowCount = adapter.rowCount();
		} else {
			if (biggestChildWidth > 0 && biggestChildHeight > 0) {
				computedColumnCount = widthSize / biggestChildWidth;
				computedRowCount = heightSize / biggestChildHeight;
			}
		}

		if (computedColumnCount == 0) {
			computedColumnCount = 1;
		}

		if (computedRowCount == 0) {
			computedRowCount = 1;
		}
	}
	

	private void searchBiggestChildMeasures() {
		biggestChildWidth = 0;
		biggestChildHeight = 0;
		for (int index = 0; index < getItemViewCount(); index++) {
			View child = getChildAt(index);

			if (biggestChildHeight < child.getMeasuredHeight()) {
				biggestChildHeight = child.getMeasuredHeight();
			}

			if (biggestChildWidth < child.getMeasuredWidth()) {
				biggestChildWidth = child.getMeasuredWidth();
			}
		}
	}

	private int getItemViewCount() {
		// -1 to remove the DeleteZone from the loop
		return getChildCount()-1;
	}

	private void adaptChildrenMeasuresToViewSize(int widthSize, int heightSize) {
		if (adapter.columnCount() != PagedDragDropGridAdapter.AUTOMATIC && adapter.rowCount() != PagedDragDropGridAdapter.AUTOMATIC) {
			int desiredGridItemWidth = widthSize / adapter.columnCount();
			int desiredGridItemHeight = heightSize / adapter.rowCount();
			measureChildren(MeasureSpec.makeMeasureSpec(desiredGridItemWidth, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(desiredGridItemHeight, MeasureSpec.AT_MOST));
		} else {
			measureChildren(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		}
	}

	private int acknowledgeHeightSize(int heightMode, int heightSize, Display display) {
		int titleHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, getResources().getDisplayMetrics());
		if (heightMode == MeasureSpec.UNSPECIFIED) {
			heightSize = display.getHeight()-titleHeight;
		}
		gridPageHeight = heightSize;
		if(pageHeight==0)
		pageHeight = gridPageHeight;
		return heightSize;
	}

	private int acknowledgeWidthSize(int widthMode, int widthSize, Display display) {
		if (widthMode == MeasureSpec.UNSPECIFIED) {
			widthSize = display.getWidth();
		}
		gridPageWidth = widthSize;
		return gridPageWidth;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int pageWidth  = (l + r) / adapter.pageCount();

		for (int page = 0; page < adapter.pageCount(); page++) {
			layoutPage(pageWidth, page);
		}
		
		if (weWereMovingDraggedBetweenPages()) {
		    bringDraggedToFront();
		}
	}

    private boolean weWereMovingDraggedBetweenPages() {
        return dragged != -1;
    }

	private void layoutPage(int pageWidth, int page) {
		int col = 0;
		int row = 0;
		for (int childIndex = 0; childIndex < adapter.itemCountInPage(page); childIndex++) {
			layoutAChild(pageWidth, page, col, row, childIndex);
			col++;
			if (col == computedColumnCount) {
				col = 0;
				row++;
			}
		}
	}

	private void layoutAChild(int pageWidth, int page, int col, int row, int childIndex) {
		int position = positionOfItem(page, childIndex);

		View child = getChildAt(position);

		int left = 0;
		int top = 0;
		if (position == dragged && lastTouchOnEdge()) {
			left = computePageEdgeXCoor(child);
			top = lastTouchY - (child.getMeasuredHeight() / 2);
		} else {
			if (adapter.columnCount() != PagedDragDropGridAdapter.AUTOMATIC && adapter.rowCount() != PagedDragDropGridAdapter.AUTOMATIC) {
				//卡乐付添加应用模式
				left = col* biggestChildWidth + (col + 1)*(pageWidth-adapter.columnCount() * biggestChildWidth)/(adapter.columnCount() +1);
				top = (row * (biggestChildHeight)) ;
			}else{
				//卡乐付主页模式
				if(col==0){
					left = (page * pageWidth) + columnWidthSize - child.getMeasuredWidth()-space;
				}else{
					left = (page * pageWidth) + columnWidthSize + space;
					
				}
				top = (row * (rowHeightSize)) + ((rowHeightSize - child.getMeasuredHeight()) / 2);
			}
				
		}
			top += topSpace;
			if(position == adapter.getMaxPageSize()-1){
				
				child.layout(left, top, 0, 0);
			}else{
				child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
			}
	}

	private boolean lastTouchOnEdge() {
		return onRightEdgeOfScreen(lastTouchX) || onLeftEdgeOfScreen(lastTouchX);
	}

	private int computePageEdgeXCoor(View child) {
		int left;
		left = lastTouchX - (child.getMeasuredWidth() / 2);
		if (onRightEdgeOfScreen(lastTouchX)) {
			left = left - gridPageWidth;
		} else if (onLeftEdgeOfScreen(lastTouchX)) {
			left = left + gridPageWidth;
		}
		return left;
	}

	@Override
	public boolean onLongClick(View v) {	    
	    if(positionForView(v) != -1) {
    		container.disableScroll();
    
    		movingView = true;
    		dragged = positionForView(v);
    		
    		bringDraggedToFront();
    
    		//animateMoveAllItems();取消空间抖动
    
    		animateDragged();
    		popDeleteView(0);

    		return true;
	    }
	    
	    return false;
	}

	private void bringDraggedToFront() {
	    View draggedView = getChildAt(dragged);
	    draggedView.bringToFront();	    
	    deleteZone.bringToFront();	    	    
    }

    private View getDraggedView() {
    	return getChildAt(getChildCount()-2);
 //     return getChildAt(dragged);
    }

    private void animateDragged() {

		ScaleAnimation scale = new ScaleAnimation(1f, scaleSize, 1f, scaleSize, biggestChildWidth / 2 , biggestChildHeight / 2);
		scale.setDuration(200);
		scale.setFillAfter(true);
		scale.setFillEnabled(true);

		if (aViewIsDragged()) {
			View draggedView = getDraggedView();
//			Log.e("animateDragged", ((TextView)draggedView.findViewWithTag("text")).getText().toString());
			
            draggedView.clearAnimation();
			draggedView.startAnimation(scale);
		}
	}

	private boolean aViewIsDragged() {
		return weWereMovingDraggedBetweenPages();
	}
	/**
	 * 显示删除布局
	 * @param space
	 */
	private void popDeleteView(int space) {
	    
	    if (adapter.showRemoveDropZone()!=null) {
    		showDeleteView(space);
	    }
		
	}
	/**
	 * 显示删除布局
	 */
    private void showDeleteView(int space) {
    	deleteZone.trash = adapter.showRemoveDropZone();
        deleteZone.setVisibility(View.VISIBLE);
        
        int t = computeDropZoneVerticalLocation() + space;
        //删除坐标t+ pageHeight + space
        int b = t+ pageHeight ;
        b = b> maxHeight? maxHeight:b;
        t = t< 0 ? 0 : t;
        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
        deleteZone.layout(0,  0, width, maxHeight);
    }
	
//	private int computeDropZoneVerticalBottom() {
//        int deleteDropZoneLocation = adapter.deleteDropZoneLocation();
//        if (deleteDropZoneLocation == PagedDragDropGridAdapter.TOP) {
//            return deleteZone.getMeasuredHeight();
//        } else {
//            return gridPageHeight - deleteZone.getMeasuredHeight() + gridPageHeight;
//        }
//    }

    private int computeDropZoneVerticalLocation() {        
//        int deleteDropZOneLocation = adapter.deleteDropZoneLocation();
//        if (deleteDropZOneLocation == PagedDragDropGridAdapter.TOP) {
//            return 0;
//        } else {
//            return gridPageHeight - deleteZone.getMeasuredHeight();
//        }
    	return (int) PagedDragDropGrid.scrollView.getScrollY();
    }

	private void createDeleteZone() {
		deleteZone = new DeleteDropZoneView(getContext());
		addView(deleteZone);
	}

	private void hideDeleteView() {
	    deleteZone.setVisibility(View.INVISIBLE);
	}

	private int positionForView(View v) {
		for (int index = 0; index < getItemViewCount(); index++) {
			View child = getChildView(index);
				if (isPointInsideView(initialRawX, initialRawY, child)) {
					return index;
				}
		}
		return -1;
	}

    private View getChildView(int index) {
        if (weWereMovingDraggedBetweenPages()) {
            if (index >= dragged) {
                return getChildAt(index -1);
            }
        } 

        return getChildAt(index);
        
    }

	private boolean isPointInsideView(float x, float y, View view) {
		int location[] = new int[2];
		view.getLocationOnScreen(location);
		int viewX = location[0];
		int viewY = location[1];

		if (pointIsInsideViewBounds(x, y, view, viewX, viewY)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean pointIsInsideViewBounds(float x, float y, View view, int viewX, int viewY) {
		return (x > viewX && x < (viewX + view.getWidth())) && (y > viewY && y < (viewY + view.getHeight()));
	}

	public void setContainer(PagedDragDropGrid container) {
		this.container = container;
	}

	private int positionOfItem(int pageIndex, int childIndex) {
		int currentGlobalIndex = 0;
		for (int currentPageIndex = 0; currentPageIndex < adapter.pageCount(); currentPageIndex++) {
			int itemCount = adapter.itemCountInPage(currentPageIndex);
			for (int currentItemIndex = 0; currentItemIndex < itemCount; currentItemIndex++) {
				if (pageIndex == currentPageIndex && childIndex == currentItemIndex) {
					return currentGlobalIndex;
				}
				currentGlobalIndex++;
			}
		}
		return -1;
	}

	private ItemPosition itemInformationAtPosition(int position) {
		int currentGlobalIndex = 0;
		for (int currentPageIndex = 0; currentPageIndex < adapter.pageCount(); currentPageIndex++) {
			int itemCount = adapter.itemCountInPage(currentPageIndex);
			for (int currentItemIndex = 0; currentItemIndex < itemCount; currentItemIndex++) {
				if (currentGlobalIndex == position) {
					return new ItemPosition(currentPageIndex, currentItemIndex);
				}
				currentGlobalIndex++;
			}
		}
		return null;
	}

	private void tellAdapterToSwapDraggedWithTarget(int dragged, int target) {
		ItemPosition draggedItemPositionInPage = itemInformationAtPosition(dragged);
		ItemPosition targetItemPositionInPage = itemInformationAtPosition(target);
		if (draggedItemPositionInPage != null && targetItemPositionInPage != null) {
			adapter.swapItems(draggedItemPositionInPage.pageIndex,draggedItemPositionInPage.itemIndex, targetItemPositionInPage.itemIndex);
		}
	}

//	private void tellAdapterToMoveItemToPreviousPage(int itemIndex) {
//		ItemPosition itemPosition = itemInformationAtPosition(itemIndex);
//		adapter.moveItemToPreviousPage(itemPosition.pageIndex,itemPosition.itemIndex);
//	}
//
//	private void tellAdapterToMoveItemToNextPage(int itemIndex) {
//		ItemPosition itemPosition = itemInformationAtPosition(itemIndex);
//		adapter.moveItemToNextPage(itemPosition.pageIndex,itemPosition.itemIndex);
//	}

	private class ItemPosition {
		public int pageIndex;
		public int itemIndex;

		public ItemPosition(int pageIndex, int itemIndex) {
			super();
			this.pageIndex = pageIndex;
			this.itemIndex = itemIndex;
		}
	}
}
