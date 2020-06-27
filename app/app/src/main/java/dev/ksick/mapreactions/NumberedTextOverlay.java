package dev.ksick.mapreactions;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import org.apache.commons.lang3.StringUtils;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

public class NumberedTextOverlay extends Overlay {

    Paint textPaint = new Paint();
    Paint textBackgroundPaint = new Paint();
    Paint numberPaint = new Paint();
    Paint numberBackgroundPaint = new Paint();
    Paint borderPaint = new Paint();

    private int number = -1;
    private String text;
    private GeoPoint position;

    private int borderSize = 2;
    private int paddingTop = 16;
    private int paddingBottom = 16;
    private int paddingLeft = 24;
    private int paddingRight = 24;
    private int cornerRadius = 12;

    public NumberedTextOverlay(int number, String text, GeoPoint position) {
        this.number = number;
        this.text = text;
        this.position = position;

        // set default values
        setBackgroundColor(Color.BLACK);
        setTextColor(Color.WHITE);
        setFontSize(48);
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setPosition(GeoPoint position) {
        this.position = position;
    }

    public void setBorderSize(int borderSize) {
        this.borderSize = borderSize;
    }

    public void setPaddingTop(int paddingTop) {
        this.paddingTop = paddingTop;
    }

    public void setPaddingBottom(int paddingBottom) {
        this.paddingBottom = paddingBottom;
    }

    public void setPaddingLeft(int paddingLeft) {
        this.paddingLeft = paddingLeft;
    }

    public void setPaddingRight(int paddingRight) {
        this.paddingRight = paddingRight;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public void setPadding(int padding) {
        paddingLeft = paddingRight = paddingBottom = paddingTop = padding;
    }

    public void setBackgroundColor(int backgroundColor) {
        textBackgroundPaint.setColor(backgroundColor);
        numberPaint.setColor(backgroundColor);
        borderPaint.setColor(backgroundColor);
    }

    public void setTextColor(int textColor) {
        textPaint.setColor(textColor);
        numberBackgroundPaint.setColor(textColor);
    }

    public void setTypeface(Typeface typeface) {
        textPaint.setTypeface(typeface);
    }

    public void setNumberTypeface(Typeface numberTypeface) {
        numberPaint.setTypeface(numberTypeface);
    }

    public void setFontSize(int fontSize) {
        textPaint.setTextSize(fontSize);
        numberPaint.setTextSize(fontSize);
    }

    @Override
    public void draw(Canvas pCanvas, Projection pProjection) {
        if (position == null || number < 0 || StringUtils.isBlank(text)) {
            return;
        }

        Point pixelCoordinates = new Point();
        pProjection.toPixels(position, pixelCoordinates);

        Rect textRect = new Rect();
        RectF textBackgroundRect = new RectF();

        textPaint.getTextBounds(text, 0, text.length(), textRect);
        textRect.offset(pixelCoordinates.x, pixelCoordinates.y);

        textBackgroundRect.set(
                textRect.left - paddingLeft,
                textRect.top - paddingTop,
                textRect.right + paddingRight,
                textRect.bottom + paddingBottom
        );

        RectF numberRect = new RectF(
                textBackgroundRect.left - paddingRight - numberPaint.measureText(String.valueOf(number)),
                textRect.top,
                textBackgroundRect.left - paddingRight,
                textRect.bottom
        );

        RectF numberBackgroundRect = new RectF(
                numberRect.left - paddingLeft,
                textBackgroundRect.top,
                numberRect.right + paddingRight,
                textBackgroundRect.bottom
        );

        RectF borderRect = new RectF(
                numberBackgroundRect.left - borderSize,
                textBackgroundRect.top - borderSize,
                textBackgroundRect.right + borderSize,
                textBackgroundRect.bottom + borderSize
        );

        // border
        pCanvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, borderPaint);

        // text
        pCanvas.drawRoundRect(textBackgroundRect, cornerRadius, cornerRadius, textBackgroundPaint);
        pCanvas.drawRect(textBackgroundRect.left, textBackgroundRect.top, textBackgroundRect.left + cornerRadius, textBackgroundRect.bottom, textBackgroundPaint);
        pCanvas.drawText(text, textRect.left, textRect.bottom, textPaint);

        // number
        pCanvas.drawRoundRect(numberBackgroundRect, cornerRadius, cornerRadius, numberBackgroundPaint);
        pCanvas.drawRect(numberBackgroundRect.right - cornerRadius, numberBackgroundRect.top, numberBackgroundRect.right, numberBackgroundRect.bottom, numberBackgroundPaint);
        pCanvas.drawText(String.valueOf(number), numberRect.left, numberRect.bottom, numberPaint);
    }
}
