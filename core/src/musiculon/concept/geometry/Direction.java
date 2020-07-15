package musiculon.concept.geometry;

//This class transforms x,y coordinates into direction.
//History: it was designed to simplify using gamepad stick.
public enum Direction {

    //   NW  N  NE
    //   W   *  E
    //   SW  S  SE
    N(-Direction.TAN_60, Direction.TAN_60, 90), NE(Direction.TAN_60, Direction.TAN_30, 45),
    E(Direction.TAN_30, -Direction.TAN_30, 0), SE(-Direction.TAN_30, -Direction.TAN_60, 315),
    S(-Direction.TAN_60, Direction.TAN_60, 270), SW(Direction.TAN_60, Direction.TAN_30, 225),
    W(Direction.TAN_30, -Direction.TAN_30, 180), NW(-Direction.TAN_30, -Direction.TAN_60, 135);

    public static final float TAN_30 = 0.57735026919f;
    public static final float TAN_60 = 1.73205080757f;

    public final float leftBorderTan, rightBorderTan;
    public final int degreeAngle;

    Direction(float leftBorderTan, float rightBorderTan, int degreeAngle) {
        this.leftBorderTan = leftBorderTan;
        this.rightBorderTan = rightBorderTan;
        this.degreeAngle = degreeAngle;
    }



    public static Direction getDirection(float x, float y) {
        if (x == 0 && y == 0) {
            throw new RuntimeException("Can't call getDirection method with ZERO only arguments");
        }

        Direction result = null;
        for(Direction direction: Direction.values()) {
            float left = Line.getYfromXWithK(direction.leftBorderTan, x);
            float right = Line.getYfromXWithK(direction.rightBorderTan, x);

            boolean isValid = false;
            switch (direction) {
                case N:  isValid = y >= left && y > right; break;
                case NE:
                case E:
                case SE: isValid = y <= left && y > right; break;
                case S:  isValid = y <= left && y < right; break;
                case SW:
                case W:
                case NW: isValid = y <= right && y > left; break;
            }

            if (isValid) {
                result = direction;
                break;
            }
        }

        return result;
    }

}