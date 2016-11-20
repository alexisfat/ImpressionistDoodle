package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;


import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;
    private VelocityTracker velocity = null;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private final int MIN_RADIUS = 25;
    private float _minBrushRadius = MIN_RADIUS;


    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public Bitmap getOffScreenBitmap(){
        return _offScreenBitmap;
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }


    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        if(_offScreenCanvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
            invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }
    /* This function gets the color from a pixel of the image, however if the pixel is out of bounds
    * from the image, the color becomes gray.
     */
    private void getColor(MotionEvent motionEvent){
        /* get the current x, y points */
        float currX = motionEvent.getX();
        float currY = motionEvent.getY();
        /* get the bitmap from the image view*/
        Bitmap imageViewBitmap = _imageView.getDrawingCache();
        /*make a rectangle in the bounds*/
        Rect r = getBitmapPositionInsideImageView(_imageView);
        /*check if the x,y is in the bounds*/
        if (!r.contains((int) currX, (int) currY)){
            /*set the ARGB value since we are out of bounds*/
            _paint.setARGB(_alpha, 240, 240, 240);
        } else {
            /*otherwise grab the pixel and get the color*/
            int pixel = imageViewBitmap.getPixel((int) currX, (int) currY);
            int redValue = Color.red(pixel);
            int greenValue = Color.green(pixel);
            int blueValue = Color.blue(pixel);
            //Bitmap imageViewBitmap = _imageView.getDrawingCache();
            //int colorAtTouchPixelInImage = imageViewBitmap.getPixel(curPoint.x, curPoint.y);
            // int colorAtTouchPixelInImage = imageViewBitmap.getPixel((int)currX, (int)currY);

            _paint.setARGB(_alpha, redValue, greenValue, blueValue);
        }
    }
    /*This function checks which brush the user is using*/
    private void checkBrush(int touchX, int touchY, float xVelocity, float yVelocity){
       /*if we use a circle brush, then we are going to use a velocity tracker*/
        if (_brushType == BrushType.Circle) {
            _useMotionSpeedForBrushStrokeSize = true;
            //_offScreenCanvas.drawCircle(currX, currY, 30.0f, _paint);
        }
        /*check that we are using motion*/
        if (_useMotionSpeedForBrushStrokeSize) {
            /*make the min brush radius equal to the min of either the x or y velocities*/
            _minBrushRadius = Math.min(xVelocity, yVelocity);
            /*ensure that the min brush radius isn't smaller that our min*/
            if(_minBrushRadius < MIN_RADIUS){
                _minBrushRadius = MIN_RADIUS;
            }
            /*if(_minBrushRadius > MAX_RADIUS){
                _minBrushRadius = _defaultRadius;
            }*/
            /*draw the circle, and reset the flag to false*/
            _offScreenCanvas.drawCircle(touchX, touchY, _minBrushRadius, _paint);
            _useMotionSpeedForBrushStrokeSize = false;
        }
        /*if the brush is a rectangle, draw a rectangle*/
        if (_brushType == BrushType.Rectangle) {
            _offScreenCanvas.drawRect(touchX + 100f, touchY + 100f , touchX + 300f , touchY + 300f, _paint);
        }
        /*if the brush is a square, draw a square*/
        if (_brushType == BrushType.Square) {
            _offScreenCanvas.drawRect(touchX, touchY, touchX + 40f, touchY + 40f, _paint);
        }
        /*if the brush is a line, draw a line*/
        if (_brushType == BrushType.Line) {
            _offScreenCanvas.drawLine(touchX, touchY, touchX + 80f, touchY + 80f, _paint);
        }
        /*if the brush is a popcorn, draw random circles!!*/
        if (_brushType == BrushType.Popcorn) {
            //_offScreenCanvas.drawCircle(touchX, touchY, 70f, _paint);
            Random rand = new Random();
            _minBrushRadius = 100;
            int width = getWidth();
            int height = getHeight();
            int randX = rand.nextInt(width);
            int randY = rand.nextInt(height);
            int randR = (int)_minBrushRadius + rand.nextInt(100);
            _offScreenCanvas.drawCircle(randX, randY, randR, _paint);


        }
        /*if the brush is circle splatter, draw 10 random guassian circles*/
        if (_brushType == BrushType.CircleSplatter) {
            //_offScreenCanvas.drawCircle(touchX, touchY, 70f, _paint);
            Random rand = new Random();
            for (int i = 0; i < 10; i++){
                float d =(float) rand.nextGaussian();
                float newX = touchX*d;
                float newY = touchY*d;
                _offScreenCanvas.drawCircle(newX, newY, _minBrushRadius,_paint);
            }


        }
          /*if the brush is a line splatter, draw random lines*/
        if (_brushType == BrushType.LineSplatter) {
            Random rand = new Random();
            _minBrushRadius = 100;
            int width = getWidth();
            int height = getHeight();
            int randX = rand.nextInt(width);
            int randY = rand.nextInt(height);
            int randR = (int)_minBrushRadius + rand.nextInt(100);
            _offScreenCanvas.drawLine(randX, randY, randX+randR, randY+randR, _paint);
        }

    }


    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        //TODO
        //Basically, the way this works is to list for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location

        int index = motionEvent.getActionIndex();
        int pointerID = motionEvent.getPointerId(index);
        /*call the get Color method, so we know what color make the circles*/
        getColor(motionEvent);

        switch (motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                /*check to see if we have a velocity*/
                if(velocity == null) {
                    velocity = VelocityTracker.obtain();
                } else {
                    velocity.clear();
                }
                velocity.addMovement(motionEvent);
                break;
                /*get the history of motion*/
            case MotionEvent.ACTION_MOVE:
                int historySize = motionEvent.getHistorySize();
                /*add the event to the velocity*/
                velocity.addMovement(motionEvent);
                velocity.computeCurrentVelocity(200,200);
                /*get the x and y velocities*/
                float xVelocity = VelocityTrackerCompat.getXVelocity(velocity,pointerID);
                float yVelocity = VelocityTrackerCompat.getYVelocity(velocity,pointerID);
                /*draw for as long as the history is*/
                for(int i = 0; i < historySize; i++) {
                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);
                    /*check the brush*/
                    checkBrush((int)touchX, (int)touchY, xVelocity, yVelocity);

                }

                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        invalidate();
        return true;
    }




    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

