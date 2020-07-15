package musiculon.concept.data;

//Game entity that has a shape of circle.
public class CircleEntity {
    public float x, y, radius, rotation, speed;

    public CircleEntity(float x, float y, float radius, float rotation, float speed) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.rotation = rotation;
        this.speed = speed;
    }
}