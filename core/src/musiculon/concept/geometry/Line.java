package musiculon.concept.geometry;

public class Line {

    public static float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    }

    // y = k * x
    public static float getYfromXWithK(float k, float x) {
        return k * x;
    }

}