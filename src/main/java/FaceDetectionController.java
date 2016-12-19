import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



public class FaceDetectionController {
    private final int framesPerSecond = 25;
    private final int initialDelay = 0;

    @FXML
    private Button cameraButton;
    // the FXML area for showing the current rgbFrame
    @FXML
    private ImageView originalFrame;

    // a timer for acquiring the video stream
    private ScheduledExecutorService timer;
    // the OpenCV object that performs the video capture
    private VideoCapture capture;
    // a flag to change the button behavior
    private boolean cameraActive;

    // classifiers
    private CascadeClassifier faceCascade ;
    private CascadeClassifier   lefEyeClassifier;
    private CascadeClassifier   rightEyeClassifier;

    private Mat rgbFrame;
    private Mat grayFrame;

    // match value
    private int absoluteFaceSize;
    // rectangle used to extract eye region - ROI
    private Rect eyearea = new Rect();

    private Point leftIris = new Point();
    private List<Double> leftIrisXPoints = new ArrayList<Double>();
    private List<Double> leftIrisYPoints = new ArrayList<Double>();

    private Point rightIris = new Point();
    private List<Double> rightIrisXPoints = new ArrayList<Double>();
    private List<Double> rightIrisYPoints = new ArrayList<Double>();

    private HashMap<String,Gaze> calibratePoints ;
    private int calibrationNumber = 0;


    private long frameNumber = 0;

    /**
     * Init the controller, at start time
     */
    protected void init() {
        calibratePoints = new HashMap<String, Gaze>();
        this.capture = new VideoCapture();
        this.faceCascade = new CascadeClassifier("L:\\Studia\\ProgrammingProjects\\EyeTrackerTest\\src\\main\\resources\\haarcascade_frontalface_alt.xml");
        lefEyeClassifier = new CascadeClassifier("L:\\Studia\\ProgrammingProjects\\EyeTrackerTest\\src\\main\\resources\\haarcascade_lefteye_2splits.xml");
        rightEyeClassifier = new CascadeClassifier("L:\\Studia\\ProgrammingProjects\\EyeTrackerTest\\src\\main\\resources\\haarcascade_righteye_2splits.xml");
        this.absoluteFaceSize = 0;

    }

    @FXML
    protected void calibrate(){
      switch (calibrationNumber){
          case 0 :
              calibratePoints.put("leftUpperCorner",new Gaze(leftIris,rightIris));
              calibrationNumber++;
              Gaze leftUpperCorner = calibratePoints.get("leftUpperCorner");
              System.out.println("Left upper corner : " + leftUpperCorner.toString());
              break;
          case 1 :
              calibratePoints.put("leftBottomCorner",new Gaze(leftIris,rightIris));
              Gaze leftBottomCorner = calibratePoints.get("leftBottomCorner");
              System.out.println("Left bottom corner : " + leftBottomCorner.toString());
              calibrationNumber++;
              break;
          case 2 :
              calibratePoints.put("rightBottomCorner",new Gaze(leftIris,rightIris));
              Gaze rightBottomCorner = calibratePoints.get("rightBottomCorner");
              System.out.println("Right bottom corner : " + rightBottomCorner.toString());
              calibrationNumber++;
              break;
          case 3 :
              calibratePoints.put("rightUpperCorner",new Gaze(leftIris,rightIris));
              Gaze rightUpperCorner = calibratePoints.get("rightUpperCorner");
              System.out.println("Right upper corner : " + rightUpperCorner.toString());
              calibrationNumber++;
              break;
          default:
              break;
      }
    }

    private Point calculatePointOnImage(Point point){
        Point pointOnImage = new Point();

        return  pointOnImage;
    }





    @FXML
    protected void syntezise (){

    }

    @FXML
    protected void mode (){

    }

    @FXML
    protected void scale (){

    }

    @FXML
    protected void load (){

    }

    @FXML
    protected void reset (){
        calibrationNumber=0;
        calibratePoints.clear();
    }

    /**
     * The action triggered by pushing the button on the GUI
     */
    @FXML
    protected void startCamera() {
        // set a fixed width for the rgbFrame
        originalFrame.setFitWidth(600);
        // preserve image ratio
        originalFrame.setPreserveRatio(true);

        if (!this.cameraActive) {

            // start the video capture
            this.capture.open(0);

            // is the video stream available?
            if (this.capture.isOpened()) {
                this.cameraActive = true;

                // grab a rgbFrame
                Runnable frameGrabber = new Runnable() {

                    public void run()
                    {
                        Image imageToShow = grabFrame();
                        originalFrame.setImage(imageToShow);
                    }
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, initialDelay, framesPerSecond, TimeUnit.MILLISECONDS);

                // update the button content
                this.cameraButton.setText("Stop Camera");
            }
            else {
                // log the error
                System.err.println("Failed to open the camera connection...");
            }
        }
        else {
            // the camera is not active at this point
            this.cameraActive = false;
            // update again the button content
            this.cameraButton.setText("Start Camera");

            // stop the timer
            try {
                this.timer.shutdown();
                this.timer.awaitTermination(framesPerSecond, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                // log the exception
                System.err.println("Exception in stopping the rgbFrame capture, trying to release the camera now... " + e);
            }

            // release the camera
            this.capture.release();
            // clean the rgbFrame
            this.originalFrame.setImage(null);
        }
    }

   /*Get rgbFrame from camera*/
    private Image grabFrame() {
        // init everything
        Image imageToShow = null;
        rgbFrame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened()) {
            try {
                // read the current rgbFrame
                this.capture.read(rgbFrame);

                // if the rgbFrame is not empty, process it
                if (!rgbFrame.empty()) {
                    // face detection
                    this.detectAndDisplay(rgbFrame);

                    // convert the Mat object (OpenCV) to Image (JavaFX)
                    imageToShow = mat2Image(rgbFrame);
                }

            }
            catch (Exception e) {
                // log the (full) error
                System.err.println("ERROR: " + e);
            }
        }

        frameNumber++;

        return imageToShow;
    }


    private void detectAndDisplay(Mat frame) {
        MatOfRect faces = new MatOfRect();
        grayFrame = new Mat();

        // convert the rgbFrame in gray scale
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        // equalize the rgbFrame histogram to improve the result
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // compute minimum face size (20% of the rgbFrame height, in our case)
        if (this.absoluteFaceSize == 0)
        {
            int height = grayFrame.rows();
            if (Math.round(height * 0.2f) > 0)
            {
                this.absoluteFaceSize = Math.round(height * 0.2f);
            }
        }

        // detect faces
        this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, Objdetect.CASCADE_SCALE_IMAGE,
                new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());


        Rect[] facesArray = faces.toArray();
        for (Rect faceRect : facesArray) {
            // draw rectangle around each face
            Imgproc.rectangle(frame, faceRect.tl(), faceRect.br(), new Scalar(0, 255, 0), 3);

            // split it
            Rect eyearea_right = new Rect(faceRect.x +faceRect.width/16,(int)(faceRect.y + (faceRect.height/4.5)),(faceRect.width - 2*faceRect.width/16)/2,(int)( faceRect.height/3.0));
            Rect eyearea_left = new Rect(faceRect.x +faceRect.width/16 +(faceRect.width - 2*faceRect.width/16)/2,(int)(faceRect.y + (faceRect.height/4.5)),(faceRect.width - 2*faceRect.width/16)/2,(int)( faceRect.height/3.0));

            //draw splitted eye area
            Imgproc.rectangle(frame,eyearea_left.tl(),eyearea_left.br() , new Scalar(255,0, 0, 255), 2);
            Imgproc.rectangle(frame,eyearea_right.tl(),eyearea_right.br() , new Scalar(255, 0, 0, 255), 2);

            detectLeftEye(lefEyeClassifier,eyearea_left,100);
            detectRightEye(rightEyeClassifier,eyearea_right,100);
        }

    }

    private Mat detectLeftEye(CascadeClassifier clasificator, Rect area, int size) {
        Mat template = new Mat();
        Mat mROI = grayFrame.submat(area);
        MatOfRect eyes = new MatOfRect();

        //isolate the eyes first
        clasificator.detectMultiScale(mROI, eyes, 1.15, 2, Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30), new Size());

        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length;) {
            Rect eye = eyesArray[i];
            eye.x = area.x + eye.x;
            eye.y = area.y + eye.y;
            Rect eye_only_rectangle = new Rect((int) eye.tl().x, (int) (eye.tl().y + eye.height * 0.4), (int) eye.width,
                    (int) (eye.height * 0.6));

            //draw rectangle around each eye
            Imgproc.rectangle(rgbFrame, eye_only_rectangle.tl(), eye_only_rectangle.br(), new Scalar(255, 255, 0, 255), 2);

            //find the pupil inside the eye rect
            detectAndDisplayLeftPupil(eye_only_rectangle);

            return template;
        }

        return template;
    }

    private Mat detectRightEye(CascadeClassifier clasificator, Rect area, int size) {
        Mat template = new Mat();
        Mat mROI = grayFrame.submat(area);
        MatOfRect eyes = new MatOfRect();

        //isolate the eyes first
        clasificator.detectMultiScale(mROI, eyes, 1.15, 2, Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30), new Size());

        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length;) {
            Rect eye = eyesArray[i];
            eye.x = area.x + eye.x;
            eye.y = area.y + eye.y;
            Rect eye_only_rectangle = new Rect((int) eye.tl().x, (int) (eye.tl().y + eye.height * 0.4), (int) eye.width,
                    (int) (eye.height * 0.6));

            //draw rectangle around each eye
            Imgproc.rectangle(rgbFrame, eye_only_rectangle.tl(), eye_only_rectangle.br(), new Scalar(255, 255, 0, 255), 2);

            //find the pupil inside the eye rect
            detectAndDisplayRightPupil(eye_only_rectangle);

            return template;
        }

        return template;
    }

    //find rith pupil - the darkness point vresion
    private void detectAndDisplayLeftPupil(Rect eyeRect){
        Mat grayEyeMat = grayFrame.submat(eyeRect);
        Mat rgbEyeMat = rgbFrame.submat(eyeRect);


        Imgproc.GaussianBlur(grayEyeMat, grayEyeMat, new Size(9, 9), 2, 2);
        Core.addWeighted(grayEyeMat,1.5,grayEyeMat,-0.5,0,grayEyeMat);


        // find the darkness point
        Core.MinMaxLocResult mmG = Core.minMaxLoc(grayEyeMat);


        leftIrisXPoints.add(mmG.minLoc.x + eyeRect.x);
        leftIrisYPoints.add(mmG.minLoc.y + eyeRect.y);

        if (frameNumber%5==0){

            //assign median of leftIris coordinates from last 5 frames
            leftIris.x = Utils.median(leftIrisXPoints);
            leftIris.y = Utils.median(leftIrisYPoints);

            leftIrisXPoints.clear();
            leftIrisYPoints.clear();
        }

        // draw point to visualise pupil
        Imgproc.circle(rgbEyeMat, mmG.minLoc,2, new Scalar(255, 255, 255, 255),2);

        //for debuging only
       // System.out.print("Left leftIris x :"+ leftIris.x);
       //System.out.print("Left leftIris y : "+ leftIris.y);
      // System.out.println("");
    }

    private void detectAndDisplayRightPupil(Rect eyeRect){
        Mat grayEyeMat = grayFrame.submat(eyeRect);
        Mat rgbEyeMat = rgbFrame.submat(eyeRect);


        Imgproc.GaussianBlur(grayEyeMat, grayEyeMat, new Size(9, 9), 2, 2);
        Core.addWeighted(grayEyeMat,1.5,grayEyeMat,-0.5,0,grayEyeMat);


        // find the darkness point
        Core.MinMaxLocResult mmG = Core.minMaxLoc(grayEyeMat);


        rightIrisXPoints.add(mmG.minLoc.x + eyeRect.x);
        rightIrisYPoints.add(mmG.minLoc.y + eyeRect.y);

        if (frameNumber%5==0){

            //assign median of leftIris coordinates from last 5 frames
            rightIris.x = Utils.median(rightIrisXPoints);
            rightIris.y = Utils.median(rightIrisYPoints);



            rightIrisXPoints.clear();
            rightIrisYPoints.clear();
        }

        // draw point to visualise pupil
        Imgproc.circle(rgbEyeMat, mmG.minLoc,2, new Scalar(255, 255, 255, 255),2);

        //for debuging only
      //  System.out.print("Right iris x :"+ rightIris.x);
      //  System.out.print("Right iris y : "+ rightIris.y);
      //  System.out.println("");
    }

    //Convert a Mat object (OpenCV) in the corresponding image(to show)
    private Image mat2Image(Mat frame) {
        int ch = frame.channels();
        double[] data = frame.get((int) leftIris.x,(int) rightIris.y);
        for (int i=0; i<ch;i++){
            data[i] = data[i] * 2;
        }
        frame.put((int) leftIris.x,(int) rightIris.y,data);

        // create a temporary buffer
        MatOfByte buffer = new MatOfByte();
        // encode the rgbFrame in the buffer, according to the PNG format
        Imgcodecs.imencode(".png", frame, buffer);
        // build and return an Image created from the image encoded in the
        // buffer
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }


}