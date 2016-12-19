import org.junit.Test;
import org.opencv.core.Point;

/**
 * Created by Pawel on 2016-12-19.
 */
public class FaceDetectionControllerTest {

    @Test
        public void calculateXLeftIrisRangeTest() throws Exception {
            FaceDetectionController faceDetectionController = new FaceDetectionController();
            Gaze leftPosition = new Gaze(new Point(100,150),new Point(150,150));
            Gaze rightPosition = new Gaze(new Point(150,150),new Point(220,150));

            double xIrisRange = faceDetectionController.calculateXPupilRange(leftPosition,rightPosition,Pupils.LEFT_PUPIL);

        assert xIrisRange == 50;
    }

    @Test
    public void calculateXLeftIrisRangeTestShouldReturnZero() throws Exception {
        FaceDetectionController faceDetectionController = new FaceDetectionController();
        Gaze leftPosition = new Gaze(new Point(100,150),new Point(150,150));
        Gaze rightPosition = new Gaze(new Point(100,150),new Point(220,150));

        double xIrisRange = faceDetectionController.calculateXPupilRange(leftPosition,rightPosition,Pupils.LEFT_PUPIL);

        assert xIrisRange == 0;
    }

    @Test
    public void calculateXRightIrisRangeTestShouldReturnZero() throws Exception {
        FaceDetectionController faceDetectionController = new FaceDetectionController();
        Gaze leftPosition = new Gaze(new Point(100,150),new Point(150,150));
        Gaze rightPosition = new Gaze(new Point(200,150),new Point(150,150));

        double xIrisRange = faceDetectionController.calculateXPupilRange(leftPosition,rightPosition,Pupils.RIGHT_PUPIL);

        assert xIrisRange == 0;
    }

    @Test
    public void calculateXRightIrisRangeTest() throws Exception {
        FaceDetectionController faceDetectionController = new FaceDetectionController();
        Gaze leftPosition = new Gaze(new Point(100,150),new Point(150,150));
        Gaze rightPosition = new Gaze(new Point(150,150),new Point(220,150));

        double xIrisRange = faceDetectionController.calculateXPupilRange(leftPosition,rightPosition,Pupils.RIGHT_PUPIL);

        assert xIrisRange == 70;
    }
}