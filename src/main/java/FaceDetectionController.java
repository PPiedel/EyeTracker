import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class FaceDetectionController {
    private final int framesPerSecond = 33;
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

    // face cascade classifier
    private CascadeClassifier faceCascade ;
    // Classifiers for left-right eyes
    private CascadeClassifier   lefEyeClassifier;
    private CascadeClassifier   rightEyeClassifier;

    private Mat rgbFrame;
    private Mat grayFrame;
    private Mat mZoomWindow;

    // Helper Mat
    private Mat mResult;
    // match value
    private double match_value;
    private int absoluteFaceSize;
    // rectangle used to extract eye region - ROI
    private Rect eyearea = new Rect();
    // counter of learning frames
    private int learn_frames = 0;
    // Mat for templates
    private Mat rightEyeTemplate;
    private Mat leftEyeTemplate;
    private Mat hierarchy = new Mat();

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

                // grab a rgbFrame every 33 ms (30 frames/sec)
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

        return imageToShow;
    }

    /*Detect face, eyes*/
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

            // compute the eye area
            eyearea = new Rect(faceRect.x +faceRect.width/8,(int)(faceRect.y + (faceRect.height/4.5)),faceRect.width - 2*faceRect.width/8,(int)(faceRect.height/3.0));

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
        Point iris = new Point();

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

            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;
            Imgproc.rectangle(rgbFrame, eye_only_rectangle.tl(), eye_only_rectangle.br(), new Scalar(255, 255, 0, 255), 2);

            //find the pupil inside the eye rect
            detectPupil(eye_only_rectangle);

            return template;
        }

        return template;
    }

    private void detectPupil(Rect eyeRect) {
        hierarchy = new Mat();

        Mat img = rgbFrame.submat(eyeRect);
        Mat img_hue = new Mat();

        Mat circles = new Mat();

        // / Convert it to hue, convert to range color, and blur to remove false
        // circles
        Imgproc.cvtColor(img, img_hue, Imgproc.COLOR_RGB2HSV);// COLOR_BGR2HSV);

        Core.inRange(img_hue, new Scalar(0, 0, 0), new Scalar(255, 255, 32), img_hue);

        Imgproc.erode(img_hue, img_hue, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));

        Imgproc.dilate(img_hue, img_hue, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6)));

        Imgproc.Canny(img_hue, img_hue, 170, 220);
        Imgproc.GaussianBlur(img_hue, img_hue, new Size(9, 9), 2, 2);
        // Apply Hough Transform to find the circles
        Imgproc.HoughCircles(img_hue, circles, Imgproc.CV_HOUGH_GRADIENT, 3, img_hue.rows(), 200, 75, 10, 25);

        if (circles.cols() > 0) {
            for (int x = 0; x < circles.cols(); x++) {
                double vCircle[] = circles.get(0, x);

                if (vCircle == null)
                    break;

                Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
                int radius = (int) Math.round(vCircle[2]);

                // draw the found circle
                Imgproc.circle(img, pt, radius, new Scalar(0, 255, 0), 2);
                Imgproc.circle(img, pt, 3, new Scalar(0, 0, 255), 2);
            }
        }
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