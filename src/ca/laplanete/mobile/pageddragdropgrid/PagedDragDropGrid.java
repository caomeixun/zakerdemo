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

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

public class PagedDragDropGrid extends HorizontalScrollView implements PagedContainer, OnGestureListener {

    private static final int FLING_VELOCITY = 500;
    private int activePage = 0;
    private boolean activePageRestored = false;
    
	private DragDropGrid grid;
	private PagedDragDropGridAdapter adapter;
    private OnClickListener listener;
    private GestureDetector gestureScanner;
    public static ScrollView scrollView ;//下滑主动类
    
    private OnPageChangedListener pageChangedListener;

    public PagedDragDropGrid(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initPagedScroll();
        initGrid();
    }
 
    public PagedDragDropGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPagedScroll();
        initGrid();
    }
 
    public PagedDragDropGrid(Context context) {
        super(context);
        initPagedScroll();
        initGrid();
    }
 
    public PagedDragDropGrid(Context context, AttributeSet attrs, int defStyle, PagedDragDropGridAdapter adapter) {
        super(context, attrs, defStyle);
        this.adapter = adapter;
        initPagedScroll();
        initGrid();   
    }
 
    public PagedDragDropGrid(Context context, AttributeSet attrs, PagedDragDropGridAdapter adapter) {
        super(context, attrs);
        this.adapter = adapter;
        initPagedScroll();
        initGrid();   
    }
 
    public PagedDragDropGrid(Context context, PagedDragDropGridAdapter adapter) {
        super(context);
        this.adapter = adapter;
        initPagedScroll();        
        initGrid();    	
    }

	private void initGrid() {
	
		grid = new DragDropGrid(getContext());
    	addView(grid);    
	}
	public void toAbsoultPosition(View position){
		if(grid != null){
			grid.toAbsoultPosition(position);
		}
	}
	public ScrollView getScrollView(){
		if(scrollView == null){
			scrollView  = (ScrollView) this.getParent();
		}
		return scrollView;
	}
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		if(scrollView == null){
			scrollView  = (ScrollView) this.getParent();
		}
		super.onConfigurationChanged(newConfig);
	}
	
    public void initPagedScroll(){
    	
    	setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
    	
    	if (!isInEditMode()) {
    	    gestureScanner = new GestureDetector(getContext(), this);
    	}
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
     
                boolean specialEventUsed = gestureScanner.onTouchEvent(event);
                if(!specialEventUsed && (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
                    int scrollX = getScrollX();                    
                    int onePageWidth = v.getMeasuredWidth();
                    int page = ((scrollX + (onePageWidth/2))/onePageWidth);
                    scrollHor(page);
                    return true;
                } else {                    
                    return specialEventUsed;
                }
            }
        });
    }
    
    public void setOnPageChangedListener(OnPageChangedListener listener) {
        this.pageChangedListener = listener;
    }
    
    public void setAdapter(PagedDragDropGridAdapter adapter) {
    	this.adapter = adapter;
		grid.setAdapter(adapter);
		grid.setContainer(this);
	}
    
    public void setClickListener(OnClickListener l) {
        this.listener = l;
        grid.setOnClickListener(l);
    }
    
    public boolean onLongClick(View v) {
        return grid.onLongClick(v);
    }

    public void notifyDataSetChanged() {
        removeAllViews();
        initGrid();
        grid.setAdapter(adapter);
        grid.setContainer(this);
        grid.setOnClickListener(listener);
    }

	@Override
	public void scrollToPage(int page) {
		boolean isTop = true;
		if(page>activePage)
			isTop = false;
		activePage = page;
		//end
        if(isTop)
        	getScrollView().smoothScrollTo(0, 	getScrollView().getScrollY() - DragDropGrid.pageHeight);
        else
        	getScrollView().smoothScrollTo(0, 	getScrollView().getScrollY() + DragDropGrid.pageHeight);
        if(pageChangedListener != null){
        	pageChangedListener.onPageChanged(this, page);
        }
	}
	private void scrollHor(int page){
		activePage = page;
		int onePageWidth = getMeasuredWidth();
		int scrollTo = page*onePageWidth;
        smoothScrollTo(scrollTo, 0);
        if(pageChangedListener != null){
        	pageChangedListener.onPageChanged(this, page);
        }
	}
	@Override
	public void scrollLeft() {		
		int newPage = activePage-1;
		scrollHor(newPage);
	}

	@Override
	public void scrollRight() {
		int newPage = activePage+1;
		scrollHor(newPage);
	}
	
	@Override
	public void scrollTop() {		
		int newPage = activePage-1;
		if (canScrollToPreviousPage()) {
			scrollToPage(newPage);
		}
	}
	
	@Override
	public void scrollButtom() {
		int newPage = activePage+1;
		//if (canScrollToNextPage()) {		
		scrollToPage(newPage);
		//}
	}

	@Override
	public int currentPage() {
		return activePage;
	}

	@Override
	public void enableScroll() {
		requestDisallowInterceptTouchEvent(false);
	}

	@Override
	public void disableScroll() {
		requestDisallowInterceptTouchEvent(true);
	}

	@Override
	public boolean canScrollToNextPage() {
		if(getScrollView().getScrollY() >= DragDropGrid.maxHeight -  DragDropGrid.pageHeight - DragDropGrid.biggestChildHeight*0.7)
			return false;
		else
			return true;
	}

	@Override
	public boolean canScrollToPreviousPage() {
		
		if(getScrollView().getScrollY() > DragDropGrid.biggestChildHeight*0.7)
			return true;
		else
			return false;
	}

	public void restoreCurrentPage(int currentPage) {
	    activePage = currentPage;
	    activePageRestored = true;
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
	    super.onLayout(changed, l, t, r, b);	 
	    
	    if (activePageRestored) {
	        activePageRestored = false;
	        scrollToRestoredPage();
	    }
	}

    private void scrollToRestoredPage() {        
        scrollToPage(activePage);
    }
	
    @Override
    public boolean onDown(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent evt1, MotionEvent evt2, float velocityX, float velocityY) {   
        if (velocityX < -FLING_VELOCITY) {
            scrollRight();
            return true;
        } else if (velocityX > FLING_VELOCITY) {
            scrollLeft();
            return true;
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent arg0) {
    }

    @Override
    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent arg0) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent arg0) {
        return false;
    }	
}
