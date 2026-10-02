package ir.kamranvahdati.lawoffice;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** One restrained outline family for office navigation and status cards. */
final class OfficeIcon extends Drawable {
    private final String kind;
    private final Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);
    OfficeIcon(String kind,int color){this.kind=kind;pen.setColor(color);pen.setStyle(Paint.Style.STROKE);pen.setStrokeWidth(1.8f);pen.setStrokeCap(Paint.Cap.ROUND);pen.setStrokeJoin(Paint.Join.ROUND);}
    @Override public void draw(Canvas c){Rect bounds=getBounds();c.save();c.translate(bounds.left,bounds.top);c.scale(bounds.width()/24f,bounds.height()/24f);
        switch(kind){
            case "person":circle(c,12,8,3.1f);arc(c,5,19,19,19);break;
            case "case":rect(c,3,7,21,19);line(c,9,7,9,5);line(c,9,5,15,5);line(c,15,5,15,7);line(c,3,12,21,12);break;
            case "check":circle(c,12,12,8.5f);line(c,8,12,11,15);line(c,11,15,16,9);break;
            case "alert":circle(c,12,12,8.5f);line(c,12,7.5f,12,12.5f);circle(c,12,16,0.6f);break;
            case "deadline":circle(c,12,12,8.5f);line(c,12,7,12,12);line(c,12,12,15,14);break;
            case "hearing":rect(c,3,6,21,20);line(c,3,10,21,10);line(c,7,3.5f,7,8);line(c,17,3.5f,17,8);line(c,8,14,16,14);break;
            case "ریال":circle(c,12,12,8.5f);line(c,9,10,15,10);line(c,10,14,14,14);break;
            case "menu":line(c,4,6,20,6);line(c,4,12,20,12);line(c,4,18,20,18);break;
            default:rect(c,4,5,20,20);line(c,8,3,8,8);line(c,16,3,16,8);line(c,4,10,20,10);
        }c.restore();}
    private void line(Canvas c,float x,float y,float xx,float yy){c.drawLine(x,y,xx,yy,pen);}
    private void rect(Canvas c,float x,float y,float xx,float yy){c.drawRoundRect(x,y,xx,yy,2,2,pen);}
    private void circle(Canvas c,float x,float y,float r){c.drawCircle(x,y,r,pen);}
    private void arc(Canvas c,float left,float bottom,float right,float endY){Path p=new Path();p.moveTo(left,bottom);p.cubicTo(left,12,right,12,right,endY);c.drawPath(p,pen);}
    @Override public void setAlpha(int alpha){pen.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){pen.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
