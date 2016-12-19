import org.junit.Test;
import org.opencv.core.Point;

import static org.junit.Assert.*;

/**
 * Created by Pawel on 2016-12-19.
 */
public class FaceDetectionControllerTest {
    @Test
    public void calculateXDistanceTest() throws Exception {
        //given
        Point point1 = new Point(0,0);
        Point point2 = new Point(100,50);

        double result = Utils.calculateXDistance(point1,point2);

        //then
        assert result == 100;

    }

    @Test
    public void calculateXDistanceTestShouldGiveZero() throws Exception {
        //given
        Point point1 = new Point(0,27);
        Point point2 = new Point(0,50);

        double result = Utils.calculateXDistance(point1,point2);

        //then
        assert result == 0;

    }

    @Test
    public void calculateXDistanceTestShouldReturnZero() throws Exception {
        //given
        Point point1  = new Point();
        Point point2  = new Point();

        double result = Utils.calculateXDistance(point1,point2);

        //then
        assert result == 0;

    }
}