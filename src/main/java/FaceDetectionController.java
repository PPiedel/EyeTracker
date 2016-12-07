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

    Point iris = new Point();
    List<Double> irisXPoints = new ArrayList<Double>();
    List<Double> irisYPoints = new ArrayList<Double>();

    private long frameNumber = 0;

    /**
     * Init the controller, at start time
     */
    protected void init() {
        this.capture = new VideoCapture();
        this.faceCascade = new CascadeClassifier("C:\\Users\\praktykant\\IdeaProjects\\Test\\src\\main\\resources\\haarcascade_frontalface_alt.xml");
        lefEyeClassifier = new CascadeClassifier("C:\\Users\\praktykant\\IdeaProjects\\Test\\src\\main\\resources\\haarcascade_lefteye_2splits.xml");
        rightEyeClassifier = new CascadeClassifier("C:\\Users\\praktykant\\IdeaProjects\\Test\\src\\main\\resources\\haarcascade_righteye_2splits.xml");
        this.absoluteFaceSize = 0;

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

            detectEye(lefEyeClassifier,eyearea_left,100);
            detectEye(lefEyeClassifier,eyearea_right,100);
        }

    }

    private Mat detectEye(CascadeClassifier clasificator, Rect area, int size) {
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
            detectAndDisplayPupil(eye_only_rectangle);

            return template;
        }

        return template;
    }

    //find pupils - the darkness point vresion
    private void detectAndDisplayPupil(Rect eyeRect){
        Mat grayEyeMat = grayFrame.submat(eyeRect);
        Mat rgbEyeMat = rgbFrame.submat(eyeRect);


        Imgproc.GaussianBlur(grayEyeMat, grayEyeMat, new Size(9, 9), 2, 2);
        Core.addWeighted(grayEyeMat,1.5,grayEyeMat,-0.5,0,grayEyeMat);


        // find the darkness point
        Core.MinMaxLocResult mmG = Core.minMaxLoc(grayEyeMat);


        irisXPoints.add(mmG.minLoc.x + eyeRect.x);
        irisYPoints.add(mmG.minLoc.y + eyeRect.y);

        if (frameNumber%5==0){

            //assign median of iris coordinates from last 5 frames
            iris.x = Utils.median(irisXPoints);
            iris.y = Utils.median(irisYPoints);

            irisXPoints.clear();
            irisYPoints.clear();
        }

        // draw point to visualise pupil
        Imgproc.circle(rgbEyeMat, mmG.minLoc,2, new Scalar(255, 255, 255, 255),2);

        //for debuging only
        System.out.print("Iris x :"+iris.x);
       System.out.print("Iris y : "+iris.y);
       System.out.println("");
    }

    //Convert a Mat object (OpenCV) in the corresponding image(to show)
    private Image mat2Image(Mat frame) {
        // create a temporary buffer
        MatOfByte buffer = new MatOfByte();
        // encode the rgbFrame in the buffer, according to the PNG format
        Imgcodecs.imencode(".png", frame, buffer);
        // build and return an Image created from the image encoded in the
        // buffer
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }


}