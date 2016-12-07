import org.opencv.core.Point;

import java.util.Collections;
import java.util.List;

/**
 * Created by praktykant on 2016-12-07.
 */
public class Utils {

    public static double median(List<Double> coordinatesList) {
        double median = 0;
        double avg = 0.0;

        Collections.sort(coordinatesList);

       if (coordinatesList.size() % 2 ==0){
           avg = coordinatesList.get(coordinatesList.size()/2)+coordinatesList.get((coordinatesList.size()/2)-1);

           median = avg/2.0;
       }
       else {
           median = coordinatesList.get(coordinatesList.size()/2);
       }
       return median;
    }
}
