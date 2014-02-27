package com.todddavies.components.progressbar;

import de.azapps.mirakel.progressbar.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

/**
 * An indicator of progress, similar to Android's ProgressBar.
 * Can be used in 'spin mode' or 'increment mode'
 * @author Todd Davies
 *
 * Licensed under the Creative Commons Attribution 3.0 license see:
 * http://creativecommons.org/licenses/by/3.0/
 */
public class ProgressWheel extends View {
	
	//Sizes (with defaults)
	private int layout_height = 0;
	private int layout_width = 0;
	private int fullRadius = 100;
	private int circleRadius = 80;
	private int barLength = 60;
	private int barWidth = 20;
	private int rimWidth = 20;
	private int textSize = 20;
	
	//Padding (with defaults)
	private int paddingTop = 5;
	private int paddingBottom = 5;
	private int paddingLeft = 5;
	private int paddingRight = 5;
	
	//Colors (with defaults)
	private int barColor = 0xAA000000;
	private int circleColor = 0x00000000;
	private int rimColor = 0xAADDDDDD;
	private int textColor = 0xFF000000;

	//Paints
	private Paint barPaint = new Paint();
	private Paint circlePaint = new Paint();
	private Paint rimPaint = new Paint();
	private Paint textPaint = new Paint();
	
	//Rectangles
	@SuppressWarnings("unused")
	private RectF rectBounds = new RectF();
	private RectF circleBounds = new RectF();
	
	//Animation
	//The amount of pixels to move the bar by on each draw
	private int spinSpeed = 2;
	//The number of milliseconds to wait inbetween each draw
	private int delayMillis = 0;
	protected Handler spinHandler = new Handler() {
		/**
		 * This is the code that will increment the progress variable
		 * and so spin the wheel
		 */
		@Override
		public void handleMessage(Message msg) {
			invalidate();
			if(ProgressWheel.this.isSpinning) {
				ProgressWheel.this.progress+=ProgressWheel.this.spinSpeed;
				if(ProgressWheel.this.progress>360) {
					ProgressWheel.this.progress = 0;
				}
				ProgressWheel.this.spinHandler.sendEmptyMessageDelayed(0, ProgressWheel.this.delayMillis);
			}
			//super.handleMessage(msg);
		}
	};
	int progress = 0;
	boolean isSpinning = false;
	
	//Other
	private String text = "";
	private String[] splitText = {};
	
	/**
	 * The constructor for the ProgressWheel
	 * @param context
	 * @param attrs
	 */
	public ProgressWheel(Context context, AttributeSet attrs) {
		super(context, attrs);

		parseAttributes(context.obtainStyledAttributes(attrs, 
				R.styleable.ProgressWheel));
	}
	
	//----------------------------------
	//Setting up stuff
	//----------------------------------
	
	/**
	 * Use onSizeChanged instead of onAttachedToWindow to get the dimensions of the view,
	 * because this method is called after measuring the dimensions of MATCH_PARENT & WRAP_CONTENT.
	 * Use this dimensions to setup the bounds and paints.
	 */
	@Override
    	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        	super.onSizeChanged(w, h, oldw, oldh);
        	
        	// Share the dimensions
	        this.layout_width = w;
        	this.layout_height = h;
                
		setupBounds();
		setupPaints();
		invalidate();
    	}
    
	/**
	 * Set the properties of the paints we're using to 
	 * draw the progress wheel
	 */
	private void setupPaints() {
		this.barPaint.setColor(this.barColor);
        this.barPaint.setAntiAlias(true);
        this.barPaint.setStyle(Style.STROKE);
        this.barPaint.setStrokeWidth(this.barWidth);
        
        this.rimPaint.setColor(this.rimColor);
        this.rimPaint.setAntiAlias(true);
        this.rimPaint.setStyle(Style.STROKE);
        this.rimPaint.setStrokeWidth(this.rimWidth);
        
        this.circlePaint.setColor(this.circleColor);
        this.circlePaint.setAntiAlias(true);
        this.circlePaint.setStyle(Style.FILL);
        
        this.textPaint.setColor(this.textColor);
        this.textPaint.setStyle(Style.FILL);
        this.textPaint.setAntiAlias(true);
        this.textPaint.setTextSize(this.textSize);
	}

	/**
	 * Set the bounds of the component
	 */
	private void setupBounds() {
		// Width should equal to Height, find the min value to steup the circle
		int minValue = Math.min(this.layout_width, this.layout_height);
		
		// Calc the Offset if needed
		int xOffset = this.layout_width - minValue;
		int yOffset = this.layout_height - minValue;
		
		// Add the offset
		this.paddingTop = this.getPaddingTop() + (yOffset / 2);
	    	this.paddingBottom = this.getPaddingBottom() + (yOffset / 2);
	    	this.paddingLeft = this.getPaddingLeft() + (xOffset / 2);
	    	this.paddingRight = this.getPaddingRight() + (xOffset / 2);
		
		this.rectBounds = new RectF(this.paddingLeft,
				this.paddingTop,
                		this.getLayoutParams().width - this.paddingRight,
                		this.getLayoutParams().height - this.paddingBottom);
		
		this.circleBounds = new RectF(this.paddingLeft + this.barWidth,
				this.paddingTop + this.barWidth,
                		this.getLayoutParams().width - this.paddingRight - this.barWidth,
                		this.getLayoutParams().height - this.paddingBottom - this.barWidth);
		
		this.fullRadius = (this.getLayoutParams().width - this.paddingRight - this.barWidth)/2;
	    	this.circleRadius = (this.fullRadius - this.barWidth) + 1;
	}

	/**
	 * Parse the attributes passed to the view from the XML
	 * @param a the attributes to parse
	 */
	private void parseAttributes(TypedArray a) {
		this.barWidth = (int) a.getDimension(R.styleable.ProgressWheel_barWidth,
			this.barWidth);
		
		this.rimWidth = (int) a.getDimension(R.styleable.ProgressWheel_rimWidth,
			this.rimWidth);
		
		this.spinSpeed = (int) a.getDimension(R.styleable.ProgressWheel_spinSpeed,
			this.spinSpeed);
		
		this.delayMillis = a.getInteger(R.styleable.ProgressWheel_delayMillis,
			this.delayMillis);
		if(this.delayMillis<0) {
			this.delayMillis = 0;
		}
	    
	    this.barColor = a.getColor(R.styleable.ProgressWheel_barColor, this.barColor);
	    
	    this.barLength = (int) a.getDimension(R.styleable.ProgressWheel_barLength,
	    	this.barLength);
	    
	    this.textSize = (int) a.getDimension(R.styleable.ProgressWheel_textSize,
	    	this.textSize);
	    
	    this.textColor = a.getColor(R.styleable.ProgressWheel_textColor,
	    	this.textColor);
	    
	    //if the text is empty , so ignore it
	    if(a.hasValue(R.styleable.ProgressWheel_text)) {
                setText(a.getString(R.styleable.ProgressWheel_text));
            }
	    
	    this.rimColor = a.getColor(R.styleable.ProgressWheel_rimColor,
	    	this.rimColor);
	    
	    this.circleColor = a.getColor(R.styleable.ProgressWheel_circleColor,
	    	this.circleColor);
	    	
	    	
		// Recycle
		a.recycle();
	}

	//----------------------------------
	//Animation stuff
	//----------------------------------
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		//Draw the rim
		canvas.drawArc(this.circleBounds, 360, 360, false, this.rimPaint);
		//Draw the bar
		if(this.isSpinning) {
			canvas.drawArc(this.circleBounds, this.progress - 90, this.barLength, false,
				this.barPaint);
		} else {
			canvas.drawArc(this.circleBounds, -90, this.progress, false, this.barPaint);
		}
		//Draw the inner circle
		canvas.drawCircle((this.circleBounds.width()/2) + this.rimWidth + this.paddingLeft, 
				(this.circleBounds.height()/2) + this.rimWidth + this.paddingTop, 
				this.circleRadius, 
				this.circlePaint);
		//Draw the text (attempts to center it horizontally and vertically)
		int offsetNum = 0;
		for(String s : this.splitText) {
			float offset = this.textPaint.measureText(s) / 2;
			canvas.drawText(s, this.getWidth() / 2 - offset, 
				this.getHeight() / 2 + (this.textSize*(offsetNum)) 
				- ((this.splitText.length-1)*(this.textSize/2)), this.textPaint);
			offsetNum++;
		}
	}

	/**
	 * Reset the count (in increment mode)
	 */
	public void resetCount() {
		this.progress = 0;
		setText("0%");
		invalidate();
	}

	/**
	 * Turn off spin mode
	 */
	public void stopSpinning() {
		this.isSpinning = false;
		this.progress = 0;
		this.spinHandler.removeMessages(0);
	}
	
	
	/**
	 * Puts the view on spin mode
	 */
	public void spin() {
		this.isSpinning = true;
		this.spinHandler.sendEmptyMessage(0);
	}
	
	/**
	 * Increment the progress by 1 (of 360)
	 */
	public void incrementProgress() {
		this.isSpinning = false;
		this.progress++;
		setText(Math.round(((float)this.progress/360)*100) + "%");
		this.spinHandler.sendEmptyMessage(0);
	}

	/**
	 * Set the progress to a specific value
	 */
	public void setProgress(int i) {
	    this.isSpinning = false;
	    this.progress=i;
	    this.spinHandler.sendEmptyMessage(0);
	}
	
	//----------------------------------
	//Getters + setters
	//----------------------------------
	
	/**
	 * Set the text in the progress bar
	 * Doesn't invalidate the view
	 * @param text the text to show ('\n' constitutes a new line)
	 */
	public void setText(String text) {
		this.text = text;
		this.splitText = this.text.split("\n");
	}
	
	public int getCircleRadius() {
		return this.circleRadius;
	}

	public void setCircleRadius(int circleRadius) {
		this.circleRadius = circleRadius;
	}

	public int getBarLength() {
		return this.barLength;
	}

	public void setBarLength(int barLength) {
		this.barLength = barLength;
	}

	public int getBarWidth() {
		return this.barWidth;
	}

	public void setBarWidth(int barWidth) {
		this.barWidth = barWidth;
	}

	public int getTextSize() {
		return this.textSize;
	}

	public void setTextSize(int textSize) {
		this.textSize = textSize;
	}

	@Override
	public int getPaddingTop() {
		return this.paddingTop;
	}

	public void setPaddingTop(int paddingTop) {
		this.paddingTop = paddingTop;
	}

	@Override
	public int getPaddingBottom() {
		return this.paddingBottom;
	}

	public void setPaddingBottom(int paddingBottom) {
		this.paddingBottom = paddingBottom;
	}

	@Override
	public int getPaddingLeft() {
		return this.paddingLeft;
	}

	public void setPaddingLeft(int paddingLeft) {
		this.paddingLeft = paddingLeft;
	}

	@Override
	public int getPaddingRight() {
		return this.paddingRight;
	}

	public void setPaddingRight(int paddingRight) {
		this.paddingRight = paddingRight;
	}

	public int getBarColor() {
		return this.barColor;
	}

	public void setBarColor(int barColor) {
		this.barColor = barColor;
	}

	public int getCircleColor() {
		return this.circleColor;
	}

	public void setCircleColor(int circleColor) {
		this.circleColor = circleColor;
	}

	public int getRimColor() {
		return this.rimColor;
	}

	public void setRimColor(int rimColor) {
		this.rimColor = rimColor;
	}
	
	
	public Shader getRimShader() {
		return this.rimPaint.getShader();
	}

	public void setRimShader(Shader shader) {
		this.rimPaint.setShader(shader);
	}

	public int getTextColor() {
		return this.textColor;
	}

	public void setTextColor(int textColor) {
		this.textColor = textColor;
	}
	
	public int getSpinSpeed() {
		return this.spinSpeed;
	}

	public void setSpinSpeed(int spinSpeed) {
		this.spinSpeed = spinSpeed;
	}
	
	public int getRimWidth() {
		return this.rimWidth;
	}

	public void setRimWidth(int rimWidth) {
		this.rimWidth = rimWidth;
	}
	
	public int getDelayMillis() {
		return this.delayMillis;
	}

	public void setDelayMillis(int delayMillis) {
		this.delayMillis = delayMillis;
	}
}