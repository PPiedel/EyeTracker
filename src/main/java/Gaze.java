import org.opencv.core.Point;

/**
 * Created by Pawel on 2016-12-18.
 */
public class Gaze {
    private Point leftPupil;
    private Point rightPupil;


    public Gaze(Point leftIris, Point rightPupil) {
        this.leftPupil = leftIris;
        this.rightPupil = rightPupil;
    }

    public Point getLeftPupil() {
        return leftPupil;
    }

    public void setLeftPupil(Point leftPupil) {
        this.leftPupil = leftPupil;
    }

    public Point getRightPupil() {
        return rightPupil;
    }

    public void setRightPupil(Point rightPupil) {
        this.rightPupil = rightPupil;
    }

    @Override
    public String toString() {
        return "Gaze : " + "leftPupil = ( " + leftPupil.x + ","+ leftPupil.y+" )" + "rightPupil = (" + rightPupil.x + "," + rightPupil.y+")" ;
    }
}
