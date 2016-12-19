import org.opencv.core.Point;

/**
 * Created by Pawel on 2016-12-18.
 */
public class Gaze {
    private Point leftIris;
    private Point rightIris;


    public Gaze(Point leftIris, Point rightIris) {
        this.leftIris = leftIris;
        this.rightIris = rightIris;
    }

    public Point getLeftIris() {
        return leftIris;
    }

    public void setLeftIris(Point leftIris) {
        this.leftIris = leftIris;
    }

    public Point getRightIris() {
        return rightIris;
    }

    public void setRightIris(Point rightIris) {
        this.rightIris = rightIris;
    }
}
